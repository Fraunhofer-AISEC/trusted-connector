package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;

public interface SecureChannelListener {
    void onMessage(byte[] data);
    void onError();
    void onClose();
}
