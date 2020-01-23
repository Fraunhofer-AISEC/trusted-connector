package de.fhg.aisec.ids.idscp2.idscp_core.rat_registry;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

import java.util.concurrent.ConcurrentHashMap;

public class RatProverDriverRegistry {
    //toDO  Reflections library
    private static RatProverDriverRegistry instance;
    private static ConcurrentHashMap<String, Class<? extends RatProverDriver>> drivers = new ConcurrentHashMap<>();

    private RatProverDriverRegistry(){}

    public static RatProverDriverRegistry getInstance(){
        if (instance == null){
           instance = new RatProverDriverRegistry();
        }

        return instance;
    }

    public static RatProverDriver startRatProverDriver(String instance, FsmListener listener){
        Class<? extends RatProverDriver> driverClass = drivers.get(instance);

        try {
            RatProverDriver driver = driverClass.getDeclaredConstructor().newInstance();
            driver.setListener(listener);
            driver.start();
            return driver;

        } catch (Exception e) {
            return null;
        }
    }


    public void registerDriver(String instance, Class<? extends RatProverDriver> driverClass){
        drivers.put(instance, driverClass);
    }

    public void unregisterDriver(String instance){
        drivers.remove(instance);
    }

}
