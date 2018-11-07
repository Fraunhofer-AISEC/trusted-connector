/*-
 * ========================LICENSE_START=================================
 * ids-comm
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.ids.comm.ws.protocol;

import com.google.protobuf.MessageLite;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.ids.comm.client.ClientConfiguration;
import de.fhg.ids.comm.server.ServerConfiguration;
import de.fhg.ids.comm.ws.protocol.error.ErrorHandler;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.fsm.Transition;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataConsumerHandler;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataProviderHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationConsumerHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationProviderHandler;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.asynchttpclient.ws.WebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class generates the Finite State Machine (FSM) for the IDS protocol.
 *
 * @author Julian Schütte
 * @author Georg Räß
 * @author Gerd Brost
 * @author Michael Lux
 */
public class ProtocolMachine {
  private static final Logger LOG = LoggerFactory.getLogger(ProtocolMachine.class);

  /** The session to send and receive messages */
  private Session serverSession;

  private String socket = "/var/run/tpm2d/control.sock";
  private WebSocket clientSocket;

  protected Transition makeConsumerErrorTransition(ProtocolState state, ErrorHandler errorHandler) {
    return new Transition(
        ConnectorMessage.Type.ERROR,
        state,
        ProtocolState.IDSCP_END,
        e -> errorHandler.handleError(e, state, true));
  }

  /**
   * Returns a finite state machine (FSM) implementing the IDSP protocol.
   *
   * <p>The FSM will be in its initial state and ready to accept messages via <code>FSM.feedEvent()
   * </code>. It will send responses over the session according to its FSM definition.
   *
   * @return a FSM implementing the IDSP protocol.
   */
  public FSM initIDSConsumerProtocol(WebSocket ws, ClientConfiguration clientConfiguration) {
    this.clientSocket = ws;
    FSM fsm = new FSM();

    // set trusted third party URL
    URI ttp = clientConfiguration.getTrustedThirdPartyURI();
    // all handler
    RemoteAttestationConsumerHandler ratConsumerHandler =
        new RemoteAttestationConsumerHandler(clientConfiguration, ttp, socket);
    ErrorHandler errorHandler = new ErrorHandler();
    MetadataConsumerHandler metaHandler = new MetadataConsumerHandler();

    // Standard protocol states
    fsm.addState(ProtocolState.IDSCP_START);
    fsm.addState(ProtocolState.IDSCP_ERROR);
    fsm.addState(ProtocolState.IDSCP_END);

    // Remote attestation states
    fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_REQUEST);
    fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESPONSE);
    fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESULT);
    fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_LEAVE);

    // Meta data exchange states
    fsm.addState(ProtocolState.IDSCP_META_REQUEST);
    fsm.addState(ProtocolState.IDSCP_META_RESPONSE);

    // Remote Attestation Protocol
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_START,
            ProtocolState.IDSCP_START,
            ProtocolState.IDSCP_RAT_AWAIT_REQUEST,
            e -> replyProto(ratConsumerHandler.enterRatRequest(e))));
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_REQUEST,
            ProtocolState.IDSCP_RAT_AWAIT_REQUEST,
            ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
            e -> replyProto(ratConsumerHandler.sendTPM2Ddata(e))));
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_RESPONSE,
            ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
            ProtocolState.IDSCP_RAT_AWAIT_RESULT,
            e -> replyProto(ratConsumerHandler.sendResult(e))));
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_RESULT,
            ProtocolState.IDSCP_RAT_AWAIT_RESULT,
            ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
            e -> {
              // leaveRatRequest() MUST be called FIRST!
              MessageLite message = ratConsumerHandler.leaveRatRequest(e);
              fsm.setRatResult(ratConsumerHandler.getAttestationResult());
              return replyProto(message);
            }));

    // Metadata Exchange Protocol
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_LEAVE,
            ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
            ProtocolState.IDSCP_META_REQUEST,
            e -> replyProto(metaHandler.request(e))));
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.META_RESPONSE,
            ProtocolState.IDSCP_META_REQUEST,
            ProtocolState.IDSCP_END,
            e -> {
              fsm.setMetaData(e.getMessage().getMetadataExchange().getRdfdescription());
              return true;
            }));

    // Error transitions
    // in case of error, either fast forward to meta exchange (if in rat) or go to END
    ProtocolState[] errorStartStates =
        new ProtocolState[] {
          ProtocolState.IDSCP_START,
          ProtocolState.IDSCP_RAT_AWAIT_REQUEST,
          ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
          ProtocolState.IDSCP_RAT_AWAIT_RESULT,
          ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
          ProtocolState.IDSCP_META_REQUEST,
          ProtocolState.IDSCP_META_RESPONSE
        };
    Arrays.stream(errorStartStates)
        .forEach(state -> fsm.addTransition(makeConsumerErrorTransition(state, errorHandler)));

    // Add listener to log state transitions
    fsm.addSuccessChangeListener(
        (f, e) -> LOG.debug(
            String.format("Consumer State change: %s -> %s", e.getKey(), f.getState())));

    //        String graph = fsm.toDot();
    //        System.out.println(graph);

    /* Run the FSM */
    fsm.setInitialState(ProtocolState.IDSCP_START);

    return fsm;
  }

  protected Transition makeProviderErrorTransition(ProtocolState state, ErrorHandler errorHandler) {
    return new Transition(
        ConnectorMessage.Type.ERROR,
        state,
        ProtocolState.IDSCP_END,
        e -> {
          errorHandler.handleError(e, state, false);
          return replyAbort();
        });
  }

  public FSM initIDSProviderProtocol(
      Session sess, ServerConfiguration serverConfiguration) {
    this.serverSession = sess;
    FSM fsm = new FSM();

    // set trusted third party URL
    URI ttp = serverConfiguration.getTrustedThirdPartyURI();

    // all handler
    RemoteAttestationProviderHandler ratProviderHandler =
        new RemoteAttestationProviderHandler(serverConfiguration, ttp, socket);
    ErrorHandler errorHandler = new ErrorHandler();
    MetadataProviderHandler metaHandler = new MetadataProviderHandler();

    // Standard protocol states
    fsm.addState(ProtocolState.IDSCP_START);
    fsm.addState(ProtocolState.IDSCP_ERROR);
    fsm.addState(ProtocolState.IDSCP_END);

    // Remote Attestation states
    fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_REQUEST);
    fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESPONSE);
    fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESULT);
    fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_LEAVE);

    // Metadata exchange states
    fsm.addState(ProtocolState.IDSCP_META_REQUEST);
    fsm.addState(ProtocolState.IDSCP_META_RESPONSE);

    // Remote Attestation Protocol
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_REQUEST,
            ProtocolState.IDSCP_START,
            ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
            e -> replyProto(ratProviderHandler.enterRatRequest(e))));
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_RESPONSE,
            ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
            ProtocolState.IDSCP_RAT_AWAIT_RESULT,
            e -> replyProto(ratProviderHandler.sendTPM2Ddata(e))));
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_RESULT,
            ProtocolState.IDSCP_RAT_AWAIT_RESULT,
            ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
            e -> replyProto(ratProviderHandler.sendResult(e))));
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_LEAVE,
            ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
            ProtocolState.IDSCP_META_REQUEST,
            e -> {
              // leaveRatRequest() MUST be called FIRST!
              MessageLite message = ratProviderHandler.leaveRatRequest(e);
              fsm.setRatResult(ratProviderHandler.getAttestationResult());
              return replyProto(message);
            }));

    // Metadata Exchange Protocol
    fsm.addTransition(
        new Transition(
            ConnectorMessage.Type.META_REQUEST,
            ProtocolState.IDSCP_META_REQUEST,
            ProtocolState.IDSCP_END,
            e -> {
              System.out.println(
                  "SETTING META: " + e.getMessage().getMetadataExchange().getRdfdescription());
              fsm.setMetaData(e.getMessage().getMetadataExchange().getRdfdescription());
              return replyProto(metaHandler.response(e));
            }));

    // Error transitions
    // in case of error, either fast forward to meta exchange (if in rat) or go to END
    ProtocolState[] errorStartStates =
        new ProtocolState[] {
          ProtocolState.IDSCP_START,
          ProtocolState.IDSCP_RAT_AWAIT_REQUEST,
          ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
          ProtocolState.IDSCP_RAT_AWAIT_RESULT,
          ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
          ProtocolState.IDSCP_META_REQUEST,
          ProtocolState.IDSCP_META_RESPONSE
        };
    Arrays.stream(errorStartStates)
        .forEach(state -> fsm.addTransition(makeProviderErrorTransition(state, errorHandler)));

    /* Add listener to log state transitions */
    fsm.addSuccessChangeListener(
        (f, e) -> LOG.debug(
            String.format("Provider State change: %s -> %s", e.getKey(), f.getState())));

    //        String graph = fsm.toDot();
    //        System.out.println(graph);

    /* Run the FSM */
    fsm.setInitialState(ProtocolState.IDSCP_START);

    return fsm;
  }

  private boolean replyProto(MessageLite message) {
    return reply(message.toByteArray());
  }

  /**
   * Sends a response over the websocket session.
   *
   * @param text Bytes to send to client
   * @return true if successful, false if not.
   */
  private boolean reply(byte[] text) {
    if (this.serverSession != null) {
      try {
        ByteBuffer bb = ByteBuffer.wrap(text);
        LOG.trace("Sending out ByteBuffer with {} bytes", bb.array().length);
        serverSession.getRemote().sendBytes(bb);
        serverSession.getRemote().flush();
      } catch (IOException e) {
        LOG.error(e.getMessage(), e);
        return false;
      }
    } else if (this.clientSocket != null) {
      this.clientSocket.sendBinaryFrame(text);
    }
    return true;
  }

  private boolean replyAbort() {
    if (this.serverSession != null) {
      LOG.debug("Client sending abort");
    } else {
      LOG.debug("Server sending abort");
    }
    MessageLite abortMessage =
        ConnectorMessage.newBuilder()
            .setId(0)
            .setType(ConnectorMessage.Type.ERROR)
            .setError(Error.newBuilder().setErrorCode("").setErrorMessage("Abort").build())
            .build();
    return reply(abortMessage.toByteArray());
  }
}
