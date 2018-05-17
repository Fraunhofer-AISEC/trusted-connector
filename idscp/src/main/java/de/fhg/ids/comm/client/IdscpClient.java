package de.fhg.ids.comm.client;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import java.util.concurrent.ExecutionException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ws.DefaultWebSocketListener;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

public class IdscpClient {

	public WebSocket connect(String host, int port) throws InterruptedException, ExecutionException {
		AsyncHttpClient c = asyncHttpClient();

		// Connect to web socket
		DefaultWebSocketListener wsListener = new IdspClientSocket(0, 0, null);
		WebSocket ws = c.prepareGet("ws://"+host+":"+port+"/")
				.execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(wsListener).build()).get();
		
        return ws;
		
	}
}
