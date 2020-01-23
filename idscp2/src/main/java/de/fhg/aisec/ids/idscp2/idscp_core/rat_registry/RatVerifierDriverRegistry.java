package de.fhg.aisec.ids.idscp2.idscp_core.rat_registry;


import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

import java.util.concurrent.ConcurrentHashMap;

public class RatVerifierDriverRegistry {
    private static RatVerifierDriverRegistry instance;
    private static ConcurrentHashMap<String, Class<? extends RatVerifierDriver>> drivers = new ConcurrentHashMap<>();

    private RatVerifierDriverRegistry(){}

    public static RatVerifierDriverRegistry getInstance(){
        if (instance == null){
            instance = new RatVerifierDriverRegistry();
        }

        return instance;
    }

    public static RatVerifierDriver startRatVerifierDriver(String mechanism, FsmListener listener){
        Class<? extends RatVerifierDriver> driverClass = drivers.get(mechanism);

        try {
            RatVerifierDriver driver = driverClass.getDeclaredConstructor().newInstance();
            driver.setListener(listener);
            driver.start();
            return driver;

        } catch (Exception e) {
            return null;
        }
    }


    public void registerDriver(String mechanism, Class<? extends RatVerifierDriver> driverClass){
        drivers.put(mechanism, driverClass);
    }

    public void unregisterDriver(String instance){
        drivers.remove(instance);
    }

}
