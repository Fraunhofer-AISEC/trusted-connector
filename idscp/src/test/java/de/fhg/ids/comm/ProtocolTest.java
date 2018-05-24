/*-
 * ========================LICENSE_START=================================
 * IDS Communication Protocol
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
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
package de.fhg.ids.comm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.ws.WebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;

import de.fhg.aisec.ids.api.conm.AttestationResult;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.client.ClientConfiguration;
import de.fhg.ids.comm.client.IdscpClient;
import de.fhg.ids.comm.server.IdscpServer;
import de.fhg.ids.comm.server.ServerConfiguration;
import de.fhg.ids.comm.server.SocketListener;

public class ProtocolTest {

	@Test
	public void testFailureHandling() throws InterruptedException, ExecutionException {

		MySocketListener listener = new MySocketListener();
		
		// Configure and start Server in one fluent call chain and use NON-EXISTING TPM SOCKET.
		IdscpServer server = new IdscpServer()
								.config(new ServerConfiguration()
										.port(8081)
										.tpmdSocket(new File("non-existing-tpmd-socket"))
										.attestationMask(0)
										.attestationType(IdsAttestationType.BASIC)
								)
								.setSocketListener(listener)
								.start();
		
		// Configure and start client (blocks until IDSCP has finished)
		IdscpClient client = new IdscpClient();
		WebSocket wsClient = client
								.config(new ClientConfiguration()
										.port(8080))
								.connect("localhost", 8081);
		
		
		// --- IDSC protocol will run automatically now ---
		
		// Client web socket is now expected to be open
		assertTrue(wsClient.isOpen());
		
		// Attestation result is expected to be not null and FAIL (because we did not connect to proper TPM above)
		AttestationResult attestationResult = client.getAttestationResult();
		assertEquals(AttestationResult.FAILED, attestationResult);
		
		// TODO Make server-side attestation result accessible
		//AttestationResult serverAttestationRes = server.getAttestationResult();
		
		// Send some payload from client to server
		wsClient.sendMessage("Hello");

		// Not very elegant here but sending is asynchronous and we need to 
		// wait a little until message has received by server.
		Thread.sleep(20);
		
		// Expect server to receive our payload
		String serverReceived = listener.getLastMsg();
		assertNotNull(serverReceived);
		assertEquals("Hello", serverReceived);
		
		// This is how to let the server run forever:
		//server.getServer().join();
	}
}


class MySocketListener implements SocketListener {
	public String lastMsgReceived = null;
	private String lastMsg = null;
	
	@Override
	public void onMessage(Session session, byte[] msg) {
    	this.lastMsg = new String(msg);
	}
	
	public String getLastMsg() {
		return this.lastMsg;
	}
}