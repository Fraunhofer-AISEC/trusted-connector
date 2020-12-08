package de.fhg.aisec.ids.idscp2.idscp_core.rat_registry

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.RatVerifierDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.fsmListeners.RatVerifierFsmListener
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * A Rat Verifier Driver Registry
 * The User can register Driver implementation instances and its configurations to the registry
 *
 *
 * The Idscpv2 protocol will select during the idscp handshake a Rat Verifier mechanism and will
 * check for this RatVerifier in this registry
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
object RatVerifierDriverRegistry {
    private val LOG by lazy { LoggerFactory.getLogger(RatVerifierDriverRegistry::class.java) }

    /**
     * An inner static wrapper class, that wraps driver config and driver class
     */
    private class DriverWrapper<VC>(
            val driverFactory: (RatVerifierFsmListener) -> RatVerifierDriver<VC>,
            val driverConfig: VC?
    ) {
        fun getInstance(listener: RatVerifierFsmListener) = driverFactory.invoke(listener).also {d ->
            driverConfig?.let { d.setConfig(it) }
        }
    }
    private val drivers = ConcurrentHashMap<String, DriverWrapper<*>>()

    /**
     * Register Rat Verifier driver and an optional configuration in the registry
     */
    fun <VC> registerDriver(
            mechanism: String,
            driverFactory: (RatVerifierFsmListener) -> RatVerifierDriver<VC>,
            driverConfig: VC?
    ) {
        drivers[mechanism] = DriverWrapper(driverFactory, driverConfig)
    }

    /**
     * Unregister the driver from the registry
     */
    fun unregisterDriver(instance: String) {
        drivers.remove(instance)
    }

    /**
     * To start a Rat Verifier from the finite state machine
     *
     * First we check if the registry contains the RatVerifier instance, then we create a new
     * RatVerifierDriver from the driver wrapper that holds the corresponding
     * RatVerifierDriver class.
     *
     * The finite state machine is registered as the communication partner for the RatVerifier.
     * The RatVerifier will be initialized with a configuration, if present. Then it is started.
     */
    fun startRatVerifierDriver(mechanism: String?, listener: RatVerifierFsmListener): RatVerifierDriver<*>? {
        val driverWrapper = drivers[mechanism]
        return try {
            driverWrapper!!.getInstance(listener).also { it.start() }
        } catch (e: Exception) {
            LOG.error("Error during RAT verifier start", e)
            null
        }
    }
}