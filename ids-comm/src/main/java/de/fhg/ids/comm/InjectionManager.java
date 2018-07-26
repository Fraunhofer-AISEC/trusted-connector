package de.fhg.ids.comm;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.fhg.aisec.ids.api.settings.Settings;
import org.ops4j.peaberry.Peaberry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class InjectionManager {

    private static Injector injector;

    public static void setInjector(Injector injector) {
        InjectionManager.injector = injector;
    }

    public static Injector getInjector() {
        if (injector != null) {
            return injector;
        } else {
            throw new IllegalStateException("Injector has not been set!");
        }
    }

    @Activate
    public void start(BundleContext context) {
        setInjector(Guice.createInjector(Peaberry.osgiModule(context), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Settings.class).toProvider(Peaberry.service(Settings.class).single());
            }
        }));
    }

}