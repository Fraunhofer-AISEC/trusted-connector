package de.fhg.ids.comm.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class IdscpServer {
	private Configuration config = new Configuration();
	private Server server;
	
	
	public IdscpServer config(Configuration config) {
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
		ServletHolder holderEvents = new ServletHolder("ws-events", ServerSocketServlet.class);
		context.addServlet(holderEvents, "/");

		try {
			server.start();
			//server.dump(System.err);
			//server.join();
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
