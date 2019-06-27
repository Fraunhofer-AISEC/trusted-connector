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
package de.fhg.aisec.ids.comm;

import de.fhg.aisec.ids.api.conm.RatResult;
import de.fhg.aisec.ids.comm.client.ClientConfiguration;
import de.fhg.aisec.ids.comm.client.IdscpClient;
import de.fhg.aisec.ids.comm.server.IdscpServer;
import de.fhg.aisec.ids.comm.server.ServerConfiguration;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import org.asynchttpclient.ws.WebSocket;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class ProtocolTest {

    @Test
    public void testFailureHandling() throws InterruptedException, ExecutionException,
            URISyntaxException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
            IOException, KeyManagementException {
        final MySocketListener listener = new MySocketListener();
        final Path jssePath = FileSystems.getDefault().getPath("src/test/resources/jsse");

        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(Files.newInputStream(jssePath.resolve("server-keystore.jks")),
                "password".toCharArray());
        // Configure and start Server in one fluent call chain and use NON-EXISTING TPM SOCKET.
        @SuppressWarnings("unused")
        IdscpServer server =
                new IdscpServer()
                        .config(
                                new ServerConfiguration.Builder()
                                        .port(8081)
                                        .attestationType(IdsAttestationType.BASIC)
                                        .setKeyStore(ks)
                                        .ttpUrl(new URI("https://localhost/nonexistingdummy_ttp"))
                                        .build())
                        .setSocketListener(listener)
                        .start();

        // Configure and start client (blocks until IDSCP has finished)
        IdscpClient client = new IdscpClient().config(
                new ClientConfiguration.Builder()
                        .setSha256CertificateHashes(Collections.singletonList(
                                DatatypeConverter.parseHexBinary(
                                        "4439DA49F320E3786319A5CF8D69F3A0831C4801B5CE3A14570EA84E0ECD82B0")))
                        .build());
        WebSocket wsClient = client.connect("localhost", 8081);

        // --- IDSC protocol will run automatically now ---

        // Client web socket is now expected to be open
        assertTrue(wsClient.isOpen());

        // Attestation result is expected to be not null and FAIL (because we did not connect to proper
        // TPM above)
        RatResult attestationResult = client.getAttestationResult();
        assertNotNull(attestationResult);
        assertEquals(RatResult.Status.FAILED, attestationResult.getStatus());

        // TODO Make server-side attestation result accessible
        // AttestationResult serverAttestationRes = server.handleAttestationResult();

        // Send some payload from client to server
        wsClient.sendTextFrame("Hello");

        // Expect server to receive our payload
        String serverReceived = listener.getLastMsg();
        assertNotNull(serverReceived);
        assertEquals("Hello", serverReceived);

        try {
            wsClient.sendCloseFrame(200, "Shutdown");
        } catch (Exception e) {
            //ignore
        }
        try {
            server.getServer().stop();
        } catch (Exception e) {
            //ignore
        }

        // This is how to let the server run forever:
        // server.getServer().join();
    }

}
