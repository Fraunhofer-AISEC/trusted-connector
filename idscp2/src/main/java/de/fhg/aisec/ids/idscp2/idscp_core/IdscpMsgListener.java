package de.fhg.aisec.ids.idscp2.idscp_core;

/**
 * An interface for an IDSCP message listener
 */
public interface IdscpMsgListener {

    /*
     * notify the listener about new data via idscp
     */
    void onMessage(byte[] data);

    /*
     * notify the listener that the idscp connection was closed
     */
    void onClose();
}
