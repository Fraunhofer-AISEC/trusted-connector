package de.fhg.aisec.ids.idscp2.idscp_core.rat_registry;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Rat Prover Driver Registry
 * The User can register Driver implementation instances and its configurations to the registry
 * <p>
 * The Idscpv2 protocol will select during the idscp handshake a Rat Prover mechanism and will
 * check for this RatProver in this registry
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class RatProverDriverRegistry {
    private static RatProverDriverRegistry instance;
    private static final ConcurrentHashMap<String, DriverWrapper> drivers = new ConcurrentHashMap<>();

    private RatProverDriverRegistry() {
    }

    public static RatProverDriverRegistry getInstance() {
        if (instance == null) {
            instance = new RatProverDriverRegistry();
        }

        return instance;
    }

    /*
     * To start a Rat Prover from the finite state machine
     *
     * First we check if the registry contains the RatProver instance, then we create a new
     * RatProverDriver from the driver wrapper that holds the corresponding RatProverDriver class.
     *
     * The finite state machine is registered as the communication partner for the RatProver.
     * The RatProver will be initialized with a configuration, if present. Then it is started.
     */
    public static RatProverDriver startRatProverDriver(String instance, FsmListener listener) {
        DriverWrapper driverWrapper = drivers.get(instance);
        if (driverWrapper == null) {
            return null;
        }

        try {
            RatProverDriver driver = driverWrapper.driverClass.getDeclaredConstructor().newInstance();
            driver.setListener(listener);
            if (driverWrapper.driverConfig != null) {
                driver.setConfig(driverWrapper.driverConfig);
            }
            driver.start();
            return driver;

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            return null;
        }
    }


    /*
     * Register Rat Prover driver and an optional configuration in the registry
     */
    public void registerDriver(
            String instance,
            Class<? extends RatProverDriver> driverClass,
            Object driverConfig
    ) {
        drivers.put(instance, new DriverWrapper(driverClass, driverConfig));
    }

    /*
     * Unregister the driver from the registry
     */
    public void unregisterDriver(String instance) {
        drivers.remove(instance);
    }

    /*
     * An inner static wrapper class, that wraps driver config and driver class
     */
    private static class DriverWrapper {
        private final Class<? extends RatProverDriver> driverClass;
        private final Object driverConfig;

        private DriverWrapper(
                Class<? extends RatProverDriver> driverClass,
                Object driverConfig
        ) {
            this.driverClass = driverClass;
            this.driverConfig = driverConfig;
        }
    }

}
