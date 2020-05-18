package de.fhg.aisec.ids.idscp2.idscp_core.rat_registry;


import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A Rat Verifier Driver Registry
 * The User can register Driver implementation instances and its configurations to the registry
 *
 * The Idscpv2 protocol will select during the idscp handshake a Rat Verifier mechanism and will
 * check for this RatVerifier in this registry
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class RatVerifierDriverRegistry {
    private static RatVerifierDriverRegistry instance;
    private static final ConcurrentHashMap<String, DriverWrapper> drivers
        = new ConcurrentHashMap<>();

    private RatVerifierDriverRegistry(){}

    public static RatVerifierDriverRegistry getInstance(){
        if (instance == null){
            instance = new RatVerifierDriverRegistry();
        }
        return instance;
    }

    /*
     * To start a Rat Verifier from the finite state machine
     *
     * First we check if the registry contains the RatVerifier instance, then we create a new
     * RatVerifierDriver from the driver wrapper that holds the corresponding
     * RatVerifierDriver class.
     *
     * The finite state machine is registered as the communication partner for the RatVerifier.
     * The RatVerifier will be initialized with a configuration, if present. Then it is started.
     */
    public static RatVerifierDriver startRatVerifierDriver(String mechanism, FsmListener listener){
        DriverWrapper driverWrapper = drivers.get(mechanism);

        try {
            RatVerifierDriver driver = driverWrapper.driverClass.getDeclaredConstructor().newInstance();
            driver.setListener(listener);
            if (driverWrapper.driverConfig != null) {
                driver.setConfig(driverWrapper.driverConfig);
            }
            driver.start();
            return driver;

        } catch (Exception e) {
            return null;
        }
    }

    /*
     * Register Rat Verifier driver and an optional configuration in the registry
     */
    public void registerDriver(
        String mechanism,
        Class<? extends RatVerifierDriver> driverClass,
        Object driverConfig
    ){
        drivers.put(mechanism, new DriverWrapper(driverClass, driverConfig));
    }

    /*
     * Unregister the driver from the registry
     */
    public void unregisterDriver(String instance){
        drivers.remove(instance);
    }

    /*
     * An inner static wrapper class, that wraps driver config and driver class
     */
    private static class DriverWrapper {
        private final Class<? extends RatVerifierDriver> driverClass;
        private final Object driverConfig;

        private DriverWrapper(
            Class<? extends RatVerifierDriver> driver,
            Object driverConfig
        ) {
            this.driverClass = driver;
            this.driverConfig = driverConfig;
        }
    }
}
