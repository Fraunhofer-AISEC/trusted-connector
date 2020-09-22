package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client

/**
 * An interface for an InputListenerThread, that allows DataAvailableListener registration and
 * safe stop
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface InputListener {
    /*
     * Register a listener for providing data to
     */
    fun register(listener: DataAvailableListener)

    /*
     * Terminate InputListener
     */
    fun safeStop()
}