package de.fhg.aisec.ids.attestation;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Repository {
    private static RemoteAttestationServer ratServer;

	public static void main(String[] args) throws Exception {
		ratServer = new RemoteAttestationServer("127.0.0.1" , "check", 31337);
        try {
        	ratServer.start();
        	ratServer.join();
        } finally {
        	ratServer.stop();
        }
    }
}