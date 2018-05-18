package de.fhg.ids.comm.server;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class ServerSocketServlet extends WebSocketServlet {
	private static final long serialVersionUID = -3504454673920877370L;
	private final ServerConfiguration config;

	public ServerSocketServlet(ServerConfiguration config) {
		this.config = config;

	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.setCreator(new IdscpWebSocketCreator(this.config));
	}
}