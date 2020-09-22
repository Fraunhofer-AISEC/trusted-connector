package de.fhg.aisec.ids.idscp2.idscp_core.rat_registry

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FsmListener
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

    /**
     * An inner static wrapper class, that wraps driver config and driver class
     */
    private class DriverWrapper(
            val driverClass: Class<out RatVerifierDriver>,
            val driverConfig: Any?
    )
    private val drivers = ConcurrentHashMap<String, DriverWrapper>()

    /**
     * Register Rat Verifier driver and an optional configuration in the registry
     */
    fun registerDriver(
            mechanism: String,
            driverClass: Class<out RatVerifierDriver>,
            driverConfig: Any?
    ) {
        drivers[mechanism] = DriverWrapper(driverClass, driverConfig)
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
    fun startRatVerifierDriver(mechanism: String?, listener: FsmListener): RatVerifierDriver? {
        val driverWrapper = drivers[mechanism]
        return try {
            val driver = driverWrapper!!.driverClass.getDeclaredConstructor().newInstance(listener)
            if (driverWrapper.driverConfig != null) {
                driver.setConfig(driverWrapper.driverConfig)
            }
            driver.start()
            driver
        } catch (e: Exception) {
            null
        }
    }
}