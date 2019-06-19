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
package de.fhg.aisec.ids.camel.ids.protocol;

import de.fhg.aisec.ids.comm.CertificatePair;
import de.fhg.aisec.ids.comm.client.ClientConfiguration;
import de.fhg.aisec.ids.comm.server.ServerConfiguration;
import de.fhg.aisec.ids.comm.ws.protocol.fsm.Event;
import de.fhg.aisec.ids.comm.ws.protocol.rat.RemoteAttestationClientHandler;
import de.fhg.aisec.ids.comm.ws.protocol.rat.RemoteAttestationServerHandler;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.tpm2j.tpm.TPM_ALG_ID;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
// ADVANCED test with PCRS 0-19 i.e. bitmask is 20
public class ADVANCEDAttestationIT {

  private static final String TPMD_SOCKET = "socket/control.sock";
  private static RemoteAttestationClientHandler consumer;
  private static RemoteAttestationServerHandler provider;
  private static Logger LOG = LoggerFactory.getLogger(ADVANCEDAttestationIT.class);
  private long id = 87654321;
  private static IdsAttestationType aType = IdsAttestationType.ADVANCED;
  private static int bitmask = 20;

  private TPM_ALG_ID.ALG_ID hAlg = TPM_ALG_ID.ALG_ID.TPM_ALG_SHA256;

  private ConnectorMessage msg0 =
      ConnectorMessage.newBuilder()
          .setType(ConnectorMessage.Type.RAT_START)
          .setId(id)
          .build();

  private static ConnectorMessage msg1;
  private static ConnectorMessage msg2;
  private static ConnectorMessage msg3;
  private static ConnectorMessage msg4;
  private static ConnectorMessage msg5;
  private static ConnectorMessage msg6;
  private static ConnectorMessage msg7;
  private static ConnectorMessage msg8;

  @BeforeClass
  public static void initHandlers() throws URISyntaxException, CertificateEncodingException {
    // Certificate mocks for server and client
    Certificate clientDummyCert = mock(Certificate.class);
    when(clientDummyCert.getEncoded()).thenReturn(new byte[] {0x0});
    Certificate serverDummyCert = mock(Certificate.class);
    when(serverDummyCert.getEncoded()).thenReturn(new byte[] {0x1});
    // Client IDSCP configuration
    CertificatePair clientPair = new CertificatePair();
    clientPair.setLocalCertificate(clientDummyCert);
    clientPair.setRemoteCertificate(serverDummyCert);
    ClientConfiguration clientConfiguration = new ClientConfiguration.Builder()
        .attestationType(aType)
        .certificatePair(clientPair)
        .build();
    // Server IDSCP configuration
    CertificatePair serverPair = new CertificatePair();
    serverPair.setLocalCertificate(serverDummyCert);
    serverPair.setRemoteCertificate(clientDummyCert);
    ServerConfiguration serverConfiguration = new ServerConfiguration.Builder()
        .attestationType(aType)
        .certificatePair(serverPair)
        .attestationMask(bitmask)
        .build();
    final String ratRepoUri = "https://127.0.0.1:31337/configurations/check";
    consumer =
        new RemoteAttestationClientHandler(clientConfiguration, new URI(ratRepoUri), TPMD_SOCKET);
    provider =
        new RemoteAttestationServerHandler(serverConfiguration, new URI(ratRepoUri), TPMD_SOCKET);
  }

  @Test
  public void test1() throws Exception {
    msg1 =
        ConnectorMessage.parseFrom(
            consumer
                .enterRatRequest(new Event(msg0.getType(), msg0.toString(), msg0))
                .toByteString());
    LOG.debug(msg1.toString());
    assertTrue(msg1.getId() == id + 1);
    assertTrue(msg1.getType().equals(ConnectorMessage.Type.RAT_REQUEST));
  }

  @Test
  public void test2() throws Exception {
    msg2 =
        ConnectorMessage.parseFrom(
            provider
                .enterRatRequest(new Event(msg1.getType(), msg1.toString(), msg1))
                .toByteString());
    LOG.debug(msg2.toString());
    assertTrue(msg2.getId() == id + 2);
    assertTrue(msg2.getType().equals(ConnectorMessage.Type.RAT_REQUEST));
  }

  @Test
  public void test3() throws Exception {
    assumeTrue("tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    msg3 =
        ConnectorMessage.parseFrom(
            consumer
                .sendTPM2Ddata(new Event(msg2.getType(), msg2.toString(), msg2))
                .toByteString());
    LOG.debug(msg3.toString());
    assertTrue(msg3.getId() == id + 3);
    assertTrue(msg3.getType().equals(ConnectorMessage.Type.RAT_RESPONSE));
    assertTrue(msg3.getAttestationResponse().getAtype().equals(aType));
    assertTrue(msg3.getAttestationResponse().getHalg().equals(hAlg.name()));
    assertTrue(msg3.getAttestationResponse().getPcrValuesCount() == bitmask);
  }

  @Test
  public void test4() throws Exception {
    assumeTrue("tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    msg4 =
        ConnectorMessage.parseFrom(
            provider
                .sendTPM2Ddata(new Event(msg3.getType(), msg3.toString(), msg3))
                .toByteString());
    LOG.debug(msg4.toString());
    assertTrue(msg4.getId() == id + 4);
    assertTrue(msg4.getType().equals(ConnectorMessage.Type.RAT_RESPONSE));
    assertTrue(msg4.getAttestationResponse().getAtype().equals(aType));
    assertTrue(msg4.getAttestationResponse().getHalg().equals(hAlg.name()));
    assertTrue(msg4.getAttestationResponse().getPcrValuesCount() == bitmask);
  }

  @Test
  public void test5() throws Exception {
    assumeTrue("tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    msg5 =
        ConnectorMessage.parseFrom(
            consumer.sendResult(new Event(msg4.getType(), msg4.toString(), msg4)).toByteString());
    LOG.debug(msg5.toString());
    assertTrue(msg5.getId() == id + 5);
    assertTrue(msg5.getType().equals(ConnectorMessage.Type.RAT_RESULT));
    assertTrue(msg5.getAttestationResult().getResult());
    assertTrue(msg5.getAttestationResult().getAtype().equals(aType));
  }

  @Test
  public void test6() throws Exception {
    assumeTrue("tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    msg6 =
        ConnectorMessage.parseFrom(
            provider.sendResult(new Event(msg5.getType(), msg5.toString(), msg5)).toByteString());
    LOG.debug(msg6.toString());
    assertTrue(msg6.getId() == id + 6);
    assertTrue(msg6.getType().equals(ConnectorMessage.Type.RAT_RESULT));
    assertTrue(msg6.getAttestationResult().getResult());
    assertTrue(msg6.getAttestationResult().getAtype().equals(aType));
  }

  @Test
  public void test7() throws Exception {
    assumeTrue("tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    msg7 =
        ConnectorMessage.parseFrom(
            consumer
                .leaveRatRequest(new Event(msg6.getType(), msg6.toString(), msg6))
                .toByteString());
    LOG.debug(msg7.toString());
    assertTrue(msg7.getId() == id + 7);
    assertTrue(msg7.getType().equals(ConnectorMessage.Type.RAT_LEAVE));
    assertTrue(msg7.getAttestationLeave().getAtype().equals(aType));
  }

  @Test
  public void test8() throws Exception {
    assumeTrue("tpmd socket not available. Skipping integration test", new File(TPMD_SOCKET).canWrite());

    msg8 =
        ConnectorMessage.parseFrom(
            provider
                .leaveRatRequest(new Event(msg7.getType(), msg7.toString(), msg7))
                .toByteString());
    LOG.debug(msg8.toString());
    assertTrue(msg8.getId() == id + 8);
    assertTrue(msg8.getType().equals(ConnectorMessage.Type.RAT_LEAVE));
    assertTrue(msg8.getAttestationLeave().getAtype().equals(aType));
  }
}
