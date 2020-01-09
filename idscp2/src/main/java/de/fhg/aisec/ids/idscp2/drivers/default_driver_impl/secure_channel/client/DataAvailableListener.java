package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

/**
 * An interface for DataAvailableListeners, who will be notified when new data were received
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface DataAvailableListener {
    void onMessage(byte[] data);
}
