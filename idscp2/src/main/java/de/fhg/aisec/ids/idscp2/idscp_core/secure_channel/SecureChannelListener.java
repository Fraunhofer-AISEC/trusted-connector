package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;

/**
 * An interface for a secure channel listener, implemented by the secure channel
 */
public interface SecureChannelListener {

    /*
     * Delegate data from secure channel endpoint to the secure channel
     */
    void onMessage(byte[] data);

    /*
     * Delegate an error from an secure channel endpoint to the secure channel
     */
    void onError(Throwable t);

    /*
     * Notify secure channel that secure channel endpoint has been closed
     */
    void onClose();
}
