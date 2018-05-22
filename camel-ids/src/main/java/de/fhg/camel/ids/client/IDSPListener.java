/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
package de.fhg.camel.ids.client;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.api.conm.AttestationResult;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.ProtocolMachine;
import de.fhg.ids.comm.ws.protocol.ProtocolState;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles messages for the IDS protocol.
 *
 * <p>Messages from and to the web socket are connected to the FSM implementing the actual protocol.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
public class IDSPListener implements WebSocketListener {
  private Logger LOG = LoggerFactory.getLogger(IDSPListener.class);
  private FSM fsm;
  private int attestationType = 0;
  private int attestationMask = 0;
  private ProtocolMachine machine;
  private boolean ratSuccess = false;
  private SSLContextParameters params;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition isFinishedCond = lock.newCondition();
  private final ConnectorMessage startMsg =
      Idscp.ConnectorMessage.newBuilder()
          .setType(ConnectorMessage.Type.RAT_START)
          .setId(new java.util.Random().nextLong())
          .build();

  public IDSPListener(int attestationType, int attestationMask, SSLContextParameters params) {
    this.attestationType = attestationType;
    this.attestationMask = attestationMask;
    this.params = params;
  }

  public void onOpen(WebSocket websocket) {
    LOG.debug("Websocket opened");
    IdsAttestationType type;
    switch (this.attestationType) {
      case 0:
        type = IdsAttestationType.BASIC;
        break;
      case 1:
        type = IdsAttestationType.ALL;
        break;
      case 2:
        type = IdsAttestationType.ADVANCED;
        break;
      case 3:
        type = IdsAttestationType.ZERO;
        break;
      default:
        type = IdsAttestationType.BASIC;
        break;
    }
    // create Finite State Machine for IDS protocol
    machine = new ProtocolMachine();
    fsm = machine.initIDSConsumerProtocol(websocket, type, this.attestationMask, this.params);
    // start the protocol with the first message
    fsm.feedEvent(new Event(startMsg.getType(), startMsg.toString(), startMsg));
  }

  public void onClose(WebSocket websocket, int code, String reason) {
    LOG.debug("websocket closed - reconnecting");
    fsm.reset();
  }

  public void onError(Throwable t) {
    LOG.debug("websocket on error", t);
    if (fsm != null) {
      fsm.reset();
    }
  }

  @Override
  public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
    try {
      lock.lockInterruptibly();
      try {
        ConnectorMessage msg = ConnectorMessage.parseFrom(payload);
        fsm.feedEvent(new Event(msg.getType(), new String(payload), msg));
      } catch (InvalidProtocolBufferException e) {
        LOG.error(e.getMessage(), e);
      }
      if (fsm.getState().equals(ProtocolState.IDSCP_END.id())) {
        isFinishedCond.signalAll();
      }
    } catch (InterruptedException e) {
      LOG.warn(e.getMessage());
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void onTextFrame(String payload, boolean finalFragment, int rsv) {
    onBinaryFrame(payload.getBytes(), finalFragment, rsv);
  }

  public ReentrantLock semaphore() {
    return lock;
  }

  public Condition isFinished() {
    return isFinishedCond;
  }

  // get the result of the remote attestation
  public boolean isAttestationSuccessful() {
    return machine.getIDSCPConsumerSuccess();
  }

  // get the result of the remote attestation
  public AttestationResult getAttestationResult() {
    if (machine.getAttestationType() == IdsAttestationType.ZERO) {
      return AttestationResult.SKIPPED;
    } else {
      if (machine.getIDSCPConsumerSuccess()) {
        return AttestationResult.SUCCESS;
      } else {
        return AttestationResult.FAILED;
      }
    }
  }
}
