package de.fhg.aisec.ids.idscp2;

/**
 * Global IDSCPv2 protocol constants
 *
 * @author Leon Beckmann(leon.beckmann@aisec.fraunhofer.de)
 */

public final class Constants {

    //IDSCPv2 message types
    public static final int ATTESTATION_REQUEST_MSG = 0;
    public static final int ATTESTATION_RESPONSE_MSG = 1;
    public static final int METADATA_REQUEST_MSG = 2;
    public static final int METADATA_RESPONSE_MSG = 3;
    public static final int PAYLOAD_MSG = 4;

    //socket instances
    public static final String TLS_INSTANCE = "TLSv1.2";

    //Standard Messages
    public static final String CLIENT_GOODBYE = "Client is disconnecting";
    public static final String SERVER_GOODBYE = "Server is disconnecting";
    public static final String END_OF_STREAM = "End of stream";


    private Constants(){};
}
