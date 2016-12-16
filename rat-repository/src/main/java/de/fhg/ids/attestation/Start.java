package de.fhg.ids.attestation;

public class Start {
    private static RemoteAttestationServer ratServer;

	public static void main(String[] args) throws Exception {
		ratServer = new RemoteAttestationServer("127.0.0.1" , "configurations/check", 31337);
        try {
        	ratServer.start();
        	ratServer.join();
        } finally {
        	ratServer.stop();
        }
    }
}