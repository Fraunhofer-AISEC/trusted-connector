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
package de.fhg.aisec.ids.comm.client;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.api.conm.RatResult;
import de.fhg.aisec.ids.comm.DatVerifier;
import de.fhg.aisec.ids.comm.ws.protocol.ClientProtocolMachine;
import de.fhg.aisec.ids.comm.ws.protocol.ProtocolState;
import de.fhg.aisec.ids.comm.ws.protocol.fsm.Event;
import de.fhg.aisec.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdspClientSocket implements WebSocketListener {
  private static final Logger LOG = LoggerFactory.getLogger(IdspClientSocket.class);
  private FSM fsm;
  @NonNull private final ReentrantLock lock = new ReentrantLock();
  private final Condition idscpInProgress = lock.newCondition();
  private final ConnectorMessage startMsg =
      Idscp.ConnectorMessage.newBuilder()
          .setType(ConnectorMessage.Type.RAT_START)
          .setId(new java.util.Random().nextLong())
          .build();
  private ClientConfiguration config;
  private boolean isTerminated = false;
  private DatVerifier datVerifier = dat -> {
    if (LOG.isInfoEnabled()) {
      LOG.info("Received DAT token: {}", dat);
    }
  };

  public IdspClientSocket(ClientConfiguration config) {
    this.config = config;
  }

  public void setDatVerifier(DatVerifier datVerifier) {
    this.datVerifier = datVerifier;
  }

  @Override
  public void onOpen(WebSocket websocket) {
    LOG.debug("Websocket opened");

    // create Finite State Machine for IDS protocol
    this.fsm = new ClientProtocolMachine(websocket, this.config, this.datVerifier);
    // start the protocol with the first message
    this.fsm.feedEvent(new Event(startMsg.getType(), startMsg.toString(), startMsg));
  }

  @Override
  public void onClose(WebSocket websocket, int code, String status) {
    LOG.debug("websocket closed - reconnecting");
    fsm.reset();
  }

  @Override
  public void onError(Throwable t) {
    LOG.debug("websocket on error", t);
    if (fsm != null) {
      fsm.reset();
    }
  }

  @Override
  public void onBinaryFrame(byte[] message, boolean finalFragment, int rsv) {
    LOG.debug("Client websocket received binary message {}", new String(message));
    try {
      lock.lockInterruptibly();
      try {
        ConnectorMessage msg = ConnectorMessage.parseFrom(message);
        LOG.debug("Received in state " + fsm.getState() + ": " + new String(message));
        fsm.feedEvent(new Event(msg.getType(), new String(message), msg));
      } catch (InvalidProtocolBufferException e) {
        LOG.error(e.getMessage(), e);
      }
      if (fsm.getState().equals(ProtocolState.IDSCP_END.id())) {
        LOG.debug("Client is now terminating IDSCP");
        this.isTerminated = true;
        idscpInProgress.signalAll();
      }
    } catch (InterruptedException e) {
      LOG.warn(e.getMessage());
      Thread.currentThread().interrupt();
    } finally {
      lock.unlock();
      this.isTerminated = true;
    }
  }

  @Override
  public void onTextFrame(String message, boolean finalFragment, int rsv) {
    LOG.debug("Client websocket received text message {}", message);
    onBinaryFrame(message.getBytes(), finalFragment, rsv);
  }

  @NonNull
  public ReentrantLock semaphore() {
    return lock;
  }

  @NonNull
  public Condition idscpInProgressCondition() {
    return idscpInProgress;
  }

  public boolean isTerminated() {
    return this.isTerminated;
  }

  // get the result of the remote attestation
  @NonNull
  public RatResult getAttestationResult() {
    return fsm.getRatResult();
  }

  @NonNull
  public String getMetaResult() {
    return fsm.getMetaData();
  }
}
