package de.fhg.aisec.ids.idscp2.idscp_core.rat_registry;


import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

import java.util.concurrent.ConcurrentHashMap;

public class RatVerifierDriverRegistry {
    private static RatVerifierDriverRegistry instance;
    private static ConcurrentHashMap<String, DriverWrapper> drivers
        = new ConcurrentHashMap<>();

    private RatVerifierDriverRegistry(){}

    public static RatVerifierDriverRegistry getInstance(){
        if (instance == null){
            instance = new RatVerifierDriverRegistry();
        }
        return instance;
    }

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

    public void registerDriver(
        String mechanism,
        Class<? extends RatVerifierDriver> driverClass,
        Object driverConfig
    ){
        drivers.put(mechanism, new DriverWrapper(driverClass, driverConfig));
    }

    public void unregisterDriver(String instance){
        drivers.remove(instance);
    }

    private static class DriverWrapper {
        private Class<? extends RatVerifierDriver> driverClass;
        private Object driverConfig;

        private DriverWrapper(
            Class<? extends RatVerifierDriver> driver,
            Object driverConfig
        ) {
            this.driverClass = driver;
            this.driverConfig = driverConfig;
        }
    }
}
