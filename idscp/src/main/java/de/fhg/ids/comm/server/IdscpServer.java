package de.fhg.ids.comm.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class IdscpServer {
	private ServerConfiguration config = new ServerConfiguration();
	private Server server;
	
	
	public IdscpServer config(ServerConfiguration config) {
		this.config = config;
		return this;
	}
	
	public IdscpServer start() {
		Server server = new Server(this.config.port);

		// Setup the basic application "context" for this application at "/"
		// This is also known as the handler tree (in jetty speak)
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		// Add a websocket to a specific path spec
		ServletHolder holderEvents = new ServletHolder("ids", new ServerSocketServlet(config));
		context.addServlet(holderEvents, "/");

		try {
			server.start();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
		this.server = server;
		return this;
	}
	
	public Server getServer() {
		return this.server;
	}
	
	public boolean isRunning() {
		Server s = this.server;
		return s != null && s.isRunning();
	}

}
