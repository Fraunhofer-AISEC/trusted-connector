package de.fhg.aisec.ids.attestation;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAttestationServer {
	private Server server;
	private Database database;
	private int PORT = 0;
	private String host;
	private String path;
	private Logger LOG = LoggerFactory.getLogger(Database.class);

	public RemoteAttestationServer(String host, String path, int port) {
		this.database = new Database();
		this.host = host;
		this.path = path;
		this.PORT = port;
	}

	public void start() {
	    server = new Server(this.PORT);
	    ServletContextHandler handler = new ServletContextHandler();
	    handler.setContextPath("");
	    // adds Jersey Servlet with a customized ResourceConfig
	    handler.addServlet(new ServletHolder(new ServletContainer(resourceConfig())), "/*");
	    server.setHandler(handler); 
	    try {
	        server.start();
	    } catch (Exception e) {
	        throw new RuntimeException("Could not start the server", e);
	    }
	}

	private ResourceConfig resourceConfig() {
	    return new ResourceConfig().register(new REST(this.database));
	}

	public void stop() {
	    try {
	        server.stop();
	    } catch (Exception e) {
	        throw new RuntimeException("Could not stop the server", e);
	    }
	}
	
	public void join() {
	    try {
	        server.join();
	    } catch (InterruptedException e) {
	        throw new RuntimeException("Could not join the thread", e);
	    }
	}
	
	public URI getURI() {
		return this.server.getURI();
	}
	
	public void destroy() {
		server.destroy();
	}
}
