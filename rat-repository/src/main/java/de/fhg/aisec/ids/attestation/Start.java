package de.fhg.aisec.ids.attestation;

public class Start {
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