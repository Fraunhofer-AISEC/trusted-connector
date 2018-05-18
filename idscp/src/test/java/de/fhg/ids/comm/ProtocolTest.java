package de.fhg.ids.comm;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.ws.WebSocket;
import org.junit.Test;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.client.ClientConfiguration;
import de.fhg.ids.comm.client.IdscpClient;
import de.fhg.ids.comm.server.ServerConfiguration;
import de.fhg.ids.comm.server.IdscpServer;

public class ProtocolTest {

	@Test
	public void test() throws InterruptedException, ExecutionException {

		// Configure and start Server in one fluent call chain
		IdscpServer server = new IdscpServer()
								.config(new ServerConfiguration()
										.port(8081)
										.tpmdSocket(new File("tpmd.test.sock"))
										.attestationMask(0)
										.attestationType(IdsAttestationType.ZERO)
								).start();
		
		// Configure and start client
		WebSocket wsClient = new IdscpClient()
								.config(new ClientConfiguration()
										.port(8080))
								.connect("localhost", 8081);
		
		// --- IDSC protocol will run automatically now ---
		
		// Send some payload from client to server
		wsClient.sendMessage("Hello");
		
		// This is how to let the server run forever:
		//server.getServer().join();
	}
}
