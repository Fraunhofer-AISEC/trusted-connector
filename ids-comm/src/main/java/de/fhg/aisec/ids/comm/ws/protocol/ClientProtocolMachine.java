/*-
 * ========================LICENSE_START=================================
 * ids-comm
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.comm.ws.protocol;

import com.google.protobuf.MessageLite;
import de.fhg.aisec.ids.comm.client.ClientConfiguration;
import de.fhg.aisec.ids.comm.ws.protocol.error.ErrorHandler;
import de.fhg.aisec.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.aisec.ids.comm.ws.protocol.fsm.Transition;
import de.fhg.aisec.ids.comm.ws.protocol.metadata.MetadataConsumerHandler;
import de.fhg.aisec.ids.comm.ws.protocol.rat.RemoteAttestationClientHandler;
import de.fhg.aisec.ids.comm.ws.protocol.rat.RemoteAttestationHandler;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import java.net.URI;
import java.util.Arrays;
import org.asynchttpclient.ws.WebSocket;
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
public class ClientProtocolMachine extends FSM {
  private static final Logger LOG = LoggerFactory.getLogger(ClientProtocolMachine.class);

  private WebSocket clientSocket;

  /**
   * Constructor of a finite state machine (FSM) implementing the IDSP protocol.
   *
   * <p>The FSM will be in its initial state and ready to accept messages via <code>FSM.feedEvent()
   * </code>. It will send responses over the session according to its FSM definition.
   */
  public ClientProtocolMachine(WebSocket ws, ClientConfiguration clientConfiguration) {
    this.clientSocket = ws;

    // set trusted third party URL
    URI ttp = clientConfiguration.getTrustedThirdPartyURI();
    // all handler
    RemoteAttestationClientHandler ratConsumerHandler =
        new RemoteAttestationClientHandler(
            clientConfiguration, ttp, RemoteAttestationHandler.CONTROL_SOCKET);
    ErrorHandler errorHandler = new ErrorHandler();
    MetadataConsumerHandler metaHandler =
        new MetadataConsumerHandler(clientConfiguration.getRDFDescription());

    // Standard protocol states
    this.addState(ProtocolState.IDSCP_START);
    this.addState(ProtocolState.IDSCP_ERROR);
    this.addState(ProtocolState.IDSCP_END);

    // Remote attestation states
    this.addState(ProtocolState.IDSCP_RAT_AWAIT_REQUEST);
    this.addState(ProtocolState.IDSCP_RAT_AWAIT_RESPONSE);
    this.addState(ProtocolState.IDSCP_RAT_AWAIT_RESULT);
    this.addState(ProtocolState.IDSCP_RAT_AWAIT_LEAVE);

    // Meta data exchange states
    this.addState(ProtocolState.IDSCP_META_REQUEST);
    this.addState(ProtocolState.IDSCP_META_RESPONSE);

    // Remote Attestation Protocol
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_START,
            ProtocolState.IDSCP_START,
            ProtocolState.IDSCP_RAT_AWAIT_REQUEST,
            e -> replyProto(ratConsumerHandler.enterRatRequest(e))));
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_REQUEST,
            ProtocolState.IDSCP_RAT_AWAIT_REQUEST,
            ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
            e -> replyProto(ratConsumerHandler.sendTPM2Ddata(e))));
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_RESPONSE,
            ProtocolState.IDSCP_RAT_AWAIT_RESPONSE,
            ProtocolState.IDSCP_RAT_AWAIT_RESULT,
            e -> replyProto(ratConsumerHandler.sendResult(e))));
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_RESULT,
            ProtocolState.IDSCP_RAT_AWAIT_RESULT,
            ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
            e -> {
              MessageLite message = ratConsumerHandler.leaveRatRequest(e);
              this.handleRatResult(
                  ratConsumerHandler.handleAttestationResult(
                      e.getMessage().getAttestationResult()));
              return replyProto(message);
            }));

    // Metadata Exchange Protocol
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.RAT_LEAVE,
            ProtocolState.IDSCP_RAT_AWAIT_LEAVE,
            ProtocolState.IDSCP_META_REQUEST,
            e -> replyProto(metaHandler.request(e))));
    this.addTransition(
        new Transition(
            ConnectorMessage.Type.META_RESPONSE,
            ProtocolState.IDSCP_META_REQUEST,
            ProtocolState.IDSCP_END,
            e -> {
              this.setMetaData(e.getMessage().getMetadataExchange().getRdfdescription());
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
        .forEach(state -> this.addTransition(makeConsumerErrorTransition(state, errorHandler)));

    // Add listener to log state transitions
    this.addSuccessChangeListener(
        (f, e) ->
            LOG.debug(String.format("Consumer State change: %s -> %s", e.getKey(), f.getState())));

    //        String graph = this.toDot();
    //        System.out.println(graph);

    /* Run the FSM */
    this.setInitialState(ProtocolState.IDSCP_START);
  }

  protected Transition makeConsumerErrorTransition(ProtocolState state, ErrorHandler errorHandler) {
    return new Transition(
        ConnectorMessage.Type.ERROR,
        state,
        ProtocolState.IDSCP_END,
        e -> errorHandler.handleError(e, state, true));
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
    this.clientSocket.sendBinaryFrame(text);
    return true;
  }
}
