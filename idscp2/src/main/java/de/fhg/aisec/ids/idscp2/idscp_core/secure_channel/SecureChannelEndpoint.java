package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;

/**
 * An interface for a secureChannelEndpoint that implements functions to delegate idscp functions to the secure channel
 *
 * Developer API
 *
 * Methods:
 * void close()                         disconnect
 * void onMessage(int len, byte[])      receive new raw data from connected endpoint
 * void send(byte[])                    send byte array to connected endpoint
 * boolean isConnected()                check if endpoint is connected
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface SecureChannelEndpoint {
    void close();
    void onMessage(byte[] bytes);
    void send(byte[] bytes);
    boolean isConnected();
}
