package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

/**
 * An interface for DataAvailableListeners, that will be notified when new data has been received
 * at the sslSocket
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface DataAvailableListener {

    /*
     * Provide incoming data to listener
     */
    void onMessage(byte[] data);

    /*
     * Notify listener that an error has occurred
     */
    void onError();

    /*
     * Notify listener that the socket has been closed
     */
    void onClose();
}
