package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.client

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