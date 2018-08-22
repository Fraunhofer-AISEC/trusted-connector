/*-
 * ========================LICENSE_START=================================
 * camel-ids
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
package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import com.google.protobuf.ByteString;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.ids.comm.Converter;
import de.fhg.ids.comm.unixsocket.UnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.rat.NonceGenerator;
import java.io.File;
import java.io.IOException;
import org.junit.Test;

/**
 * Integration tests against a TPM 2.0 daemon (tpmd) over a UNIX domain socket at
 * ./socket/control.sock.
 *
 * <p>These tests will only run if the respective UNIX domain socket is available. Otherwise they
 * will be ignored.
 */
public class UnixSocketIT {
  private static final String TPMD_SOCKET = "socket/control.sock";

  @Test
  public void testBASIC() throws Exception {
    assumeTrue(
        "tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    UnixSocketThread client;
    Thread thread;
    UnixSocketResponseHandler handler;
    byte[] quoted = NonceGenerator.generate(20);
    IdsAttestationType type = IdsAttestationType.BASIC;
    try {
      // client will be used to send messages
      client = new UnixSocketThread(TPMD_SOCKET);
      thread = new Thread(client);
      thread.setDaemon(true);
      thread.start();
      // responseHandler will be used to wait for messages
      handler = new UnixSocketResponseHandler();

      // construct protobuf message to send to local tpm2d via unix socket
      ControllerToTpm msg =
          ControllerToTpm.newBuilder()
              .setAtype(type)
              .setQualifyingData(Converter.bytesToHex(quoted))
              .setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
              .build();
      client.send(msg.toByteArray(), handler, true);
      System.out.println("waiting for socket response ....");
      byte[] tpmData = handler.waitForResponse();
      System.out.println("tpmData length : " + tpmData.length);
      // and wait for response
      TpmToController response = TpmToController.parseFrom(tpmData);
      System.out.println(response.toString());
      assertEquals(TpmToController.Code.INTERNAL_ATTESTATION_RES, response.getCode());
      assertEquals(type, response.getAtype());
    } catch (IOException e) {
      System.out.println("could not write to/read from " + TPMD_SOCKET);
      e.printStackTrace();
    }
  }

  @Test
  public void testALL() throws Exception {
    assumeTrue(
        "tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    UnixSocketThread client;
    Thread thread;
    String socket = "socket/control.sock";
    UnixSocketResponseHandler handler;
    byte[] quoted = NonceGenerator.generate(20);
    IdsAttestationType type = IdsAttestationType.ALL;

    // client will be used to send messages
    client = new UnixSocketThread(socket);
    thread = new Thread(client);
    thread.setDaemon(true);
    thread.start();
    // responseHandler will be used to wait for messages
    handler = new UnixSocketResponseHandler();

    // construct protobuf message to send to local tpm2d via unix socket
    ControllerToTpm msg =
        ControllerToTpm.newBuilder()
            .setAtype(type)
            .setQualifyingData(Converter.bytesToHex(quoted))
            .setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
            .build();
    client.send(msg.toByteArray(), handler, true);
    System.out.println("waiting for socket response ....");
    byte[] tpmData = handler.waitForResponse();
    System.out.println("tpmData length : " + tpmData.length);
    // and wait for response
    TpmToController response = TpmToController.parseFrom(tpmData);
    System.out.println(response.toString());
    assertEquals(TpmToController.Code.INTERNAL_ATTESTATION_RES, response.getCode());
    assertEquals(type, response.getAtype());
  }

  @Test
  public void testADVANCED() throws Exception {
    assumeTrue(
        "tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    UnixSocketThread client;
    Thread thread;
    UnixSocketResponseHandler handler;
    byte[] quoted = NonceGenerator.generate(20);
    IdsAttestationType type = IdsAttestationType.ADVANCED;

    // client will be used to send messages
    client = new UnixSocketThread(TPMD_SOCKET);
    thread = new Thread(client);
    thread.setDaemon(true);
    thread.start();
    // responseHandler will be used to wait for messages
    handler = new UnixSocketResponseHandler();

    // construct protobuf message to send to local tpm2d via unix socket
    ControllerToTpm msg =
        ControllerToTpm.newBuilder()
            .setAtype(type)
            .setQualifyingData(Converter.bytesToHex(quoted))
            .setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
            .setPcrs(24)
            .build();
    client.send(msg.toByteArray(), handler, true);
    System.out.println("waiting for socket response ....");
    byte[] tpmData = handler.waitForResponse();
    System.out.println("tpmData length : " + tpmData.length);
    // and wait for response
    TpmToController response = TpmToController.parseFrom(tpmData);
    System.out.println(response.toString());
    assertEquals(TpmToController.Code.INTERNAL_ATTESTATION_RES, response.getCode());
    assertEquals(type, response.getAtype());
  }
}
