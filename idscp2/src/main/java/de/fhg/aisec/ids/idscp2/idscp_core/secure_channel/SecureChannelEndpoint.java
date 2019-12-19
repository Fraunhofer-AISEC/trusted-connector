package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;


public interface SecureChannelEndpoint {
    void close();
    void onMessage(int len, byte[] bytes);
    void send(byte[] bytes);
    boolean isConnected();
    void setConnectionId(String id);
}
