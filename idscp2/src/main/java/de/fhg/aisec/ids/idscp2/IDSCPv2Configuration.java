package de.fhg.aisec.ids.idscp2;

public abstract class IDSCPv2Configuration {
    public static final int DEFAULT_SERVER_PORT = 8080;

    private int serverPort = DEFAULT_SERVER_PORT;

    private AttestationConfig supportedAttestation = null;
    private AttestationConfig expectedAttestation = null;



    public AttestationConfig getExpectedAttestation() {
        return expectedAttestation;
    }

    public AttestationConfig getSupportedAttestation() {
        return supportedAttestation;
    }

    public int getServerPort() {
        return serverPort;
    }
}
