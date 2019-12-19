package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

/**
 * An interface for an InputListenerThread, that allows DataAvailableListener registration and safe stop
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface InputListener {

    void register(DataAvailableListener listener);
    void safeStop();
}
