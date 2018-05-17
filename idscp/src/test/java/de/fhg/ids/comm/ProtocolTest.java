package de.fhg.ids.comm;

import java.util.concurrent.ExecutionException;

import org.asynchttpclient.ws.WebSocket;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import de.fhg.ids.comm.client.IdscpClient;
import de.fhg.ids.comm.server.Configuration;
import de.fhg.ids.comm.server.IdscpServer;

public class ProtocolTest {

	@Test
	public void test() throws InterruptedException, ExecutionException {

		// Start Server
		IdscpServer server = new IdscpServer().config(new Configuration().port(8081)).start();

		Thread.sleep(100);
		
		// Start Client
		WebSocket wsClient = new IdscpClient().connect("localhost", 8081);
		
		// Send some payload
		wsClient.sendMessage("Hello");
		
		server.getServer().join();
	}
}
