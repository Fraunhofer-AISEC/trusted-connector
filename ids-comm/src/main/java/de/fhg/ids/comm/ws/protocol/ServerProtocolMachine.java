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
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.ids.comm.server.ServerConfiguration;
import de.fhg.ids.comm.ws.protocol.error.ErrorHandler;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.fsm.Transition;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataProviderHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationServerHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This class generates the Finite State Machine (FSM) for the IDS protocol.
 *
 * @author Julian Schütte
 * @author Georg Räß
 * @author Gerd Brost
 * @author Michael Lux
 */
public class ServerProtocolMachine extends FSM {
  private static final Logger LOG = LoggerFactory.getLogger(ServerProtocolMachine.class);

  private Session serverSession;
  private AttestationResult attestationResult;

  public ServerProtocolMachine(
      Session sess, ServerConfiguration serverConfiguration) {
    this.serverSession = sess;

    // set trusted third party URL
    URI ttp = serverConfiguration.getTrustedThirdPartyURI();

    // all handler
    RemoteAttestationServerHandler ratProviderHandler =
        new RemoteAttestationServerHandler(serverConfiguration, ttp,
            RemoteAttestationHandler.CONTROL_SOCKET);
    ErrorHandler errorHandler = new ErrorHandler();
    MetadataProviderHandler metaHandler = new MetadataProviderHandler(serverConfiguration.getRDFDescription());

    // Standard protocol states
    this.addState(ProtocolState.IDSCP_START);
    this.addState(ProtocolState.IDSCP_ERROR);
    this.addState(ProtocolState.IDSCP_END);

    // Remote Attestation states
    this.addState(ProtocolState.IDSCP_RAT_AWAIT_REQUEST);
    this.addState(ProtocolState.IDSCP_RAT_AWAIT_RESPONSE);
    this.addState(ProtocolState.IDSCP_RAT_AWAIT_RESULT);
    this.addState(ProtocolState.IDSCP_RAT_AWAIT_LEAVE);

    // Metadata exchange states
    this.addState(ProtocolState.IDSCP_META_REQUEST);
    this.addState(ProtocolState.IDSCP_META_RESPONSE);

    // Remote Attestation Protocol
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_REQUEST,
            ProtocolState.IDSCP_START,
            ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
            e -> replyProto(ratProviderHandler.enterRatRequest(e))));
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_RESPONSE,
            ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
            ProtocolState.IDSCP_RAT_AWAIT_RESULT,
            e -> replyProto(ratProviderHandler.sendTPM2Ddata(e))));
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_RESULT,
            ProtocolState.IDSCP_RAT_AWAIT_RESULT,
            ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
            e -> {
              attestationResult = e.getMessage().getAttestationResult();
              return replyProto(ratProviderHandler.sendResult(e));
            }));
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_LEAVE,
            ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
            ProtocolState.IDSCP_META_REQUEST,
            e -> {
              MessageLite message = ratProviderHandler.leaveRatRequest(e);
              this.handleRatResult(ratProviderHandler.handleAttestationResult(attestationResult));
              return replyProto(message);
            }));

    // Metadata Exchange Protocol
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.META_REQUEST,
            ProtocolState.IDSCP_META_REQUEST,
            ProtocolState.IDSCP_END,
            e -> {
              this.setMetaData(e.getMessage().getMetadataExchange().getRdfdescription());
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
        .forEach(state -> this.addTransition(makeProviderErrorTransition(state, errorHandler)));

    /* Add listener to log state transitions */
    this.addSuccessChangeListener(
        (f, e) -> LOG.debug(
            String.format("Provider State change: %s -> %s", e.getKey(), f.getState())));

    //        String graph = this.toDot();
    //        System.out.println(graph);

    /* Run the FSM */
    this.setInitialState(ProtocolState.IDSCP_START);
  }

  private Transition makeProviderErrorTransition(ProtocolState state, ErrorHandler errorHandler) {
    return new Transition(
        ConnectorMessage.Type.ERROR,
        state,
        ProtocolState.IDSCP_END,
        e -> {
          errorHandler.handleError(e, state, false);
          LOG.debug("Client sending abort");
          MessageLite abortMessage =
              ConnectorMessage.newBuilder()
                  .setId(0)
                  .setType(ConnectorMessage.Type.ERROR)
                  .setError(Error.newBuilder().setErrorCode("").setErrorMessage("Abort").build())
                  .build();
          return reply(abortMessage.toByteArray());
        });
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
    try {
      ByteBuffer bb = ByteBuffer.wrap(text);
      LOG.trace("Sending out ByteBuffer with {} bytes", bb.array().length);
      serverSession.getRemote().sendBytes(bb);
      serverSession.getRemote().flush();
      return true;
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }

}
