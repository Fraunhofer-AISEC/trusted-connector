package de.fhg.camel.ids;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public class MyWebsocketCreator implements WebSocketCreator {

	private WebsocketConsumer consumer;
	private WebsocketStore store;

	public MyWebsocketCreator() { }
	
	public MyWebsocketCreator(WebsocketStore store, WebsocketConsumer consumer) {
		this.store = store;
		this.consumer = consumer;
	}

	@Override
	public Object createWebSocket(ServletUpgradeRequest arg0, ServletUpgradeResponse arg1) {
		return new DefaultWebsocket(store, consumer);
	}

}
