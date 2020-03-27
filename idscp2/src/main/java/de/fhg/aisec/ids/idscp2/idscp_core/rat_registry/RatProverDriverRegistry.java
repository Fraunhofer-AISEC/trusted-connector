package de.fhg.aisec.ids.idscp2.idscp_core.rat_registry;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

import java.util.concurrent.ConcurrentHashMap;

public class RatProverDriverRegistry {
    //toDo  Reflections library
    private static RatProverDriverRegistry instance;
    private static ConcurrentHashMap<String, DriverWrapper> drivers = new ConcurrentHashMap<>();

    private RatProverDriverRegistry(){}

    public static RatProverDriverRegistry getInstance(){
        if (instance == null){
           instance = new RatProverDriverRegistry();
        }

        return instance;
    }

    public static RatProverDriver startRatProverDriver(String instance, FsmListener listener){
        DriverWrapper driverWrapper = drivers.get(instance);

        try {
            RatProverDriver driver = driverWrapper.driverClass.getDeclaredConstructor().newInstance();
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
        String instance,
        Class<? extends RatProverDriver> driverClass,
        Object driverConfig
    ){
        drivers.put(instance, new DriverWrapper(driverClass, driverConfig));
    }

    public void unregisterDriver(String instance){
        drivers.remove(instance);
    }

    private static class DriverWrapper {
        private Class<? extends RatProverDriver> driverClass;
        private Object driverConfig;

        private DriverWrapper(
            Class<? extends RatProverDriver> driverClass,
            Object driverConfig
        ) {
            this.driverClass = driverClass;
            this.driverConfig = driverConfig;
        }
    }

}
