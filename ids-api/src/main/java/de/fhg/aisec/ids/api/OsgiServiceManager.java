/* ========================LICENSE_START=================================
 * IDS Core Platform API
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================*/

package de.fhg.aisec.ids.api;


import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.osgi.framework.Constants.OBJECTCLASS;

public class OsgiServiceManager {

    private final BundleContext bundleContext;
    private Map<String, List<ServiceBinding>> bindings = Collections.synchronizedMap(new HashMap<>());

    final class ServiceBinding<T> {
        private Consumer<T> bindFunction;
        private Runnable unbindFunction;
        private Class<T> serviceClass;
        private ServiceReference<T> serviceReference = null;

        public ServiceBinding(Consumer<T> bindFunction, Runnable unbindFunction, Class<T> serviceClass) {
            this.bindFunction = bindFunction;
            this.unbindFunction = unbindFunction;
            this.serviceClass = serviceClass;
        }

        public void bindService() {
            if (this.serviceReference == null) {
                this.serviceReference = bundleContext.getServiceReference(serviceClass);
                if (this.serviceReference != null) {
                    this.bindFunction.accept(bundleContext.getService(serviceReference));
                }
            }
        }

        public void unbindService() {
            if (this.serviceReference != null) {
                this.unbindFunction.run();
                bundleContext.ungetService(serviceReference);
                serviceReference = null;
            }
        }
    }

    public <T> void bindService(Class<T> serviceClass, Consumer<T> bindFunction, Runnable unbindFunction) {
        String serviceClassName = serviceClass.getName();
        if (!bindings.containsKey(serviceClassName)) {
            bindings.put(serviceClassName, new LinkedList<>());
        }
        ServiceBinding<T> binding = new ServiceBinding<>(bindFunction, unbindFunction, serviceClass);
        bindings.get(serviceClassName).add(binding);
        // If a service is available, bind immediately
        binding.bindService();
    }

    public OsgiServiceManager(Class<?> bindingClass) {
        bundleContext = FrameworkUtil.getBundle(bindingClass).getBundleContext();
        bundleContext.addServiceListener(serviceEvent -> {
            int eventType = serviceEvent.getType();
            if (eventType == ServiceEvent.REGISTERED || eventType == ServiceEvent.UNREGISTERING) {
                ServiceReference<?> serviceReference = serviceEvent.getServiceReference();
                // Get the service classes supported by this service reference
                Stream<String> objectClasses = Arrays.stream((String[]) serviceReference.getProperty(OBJECTCLASS));
                // Map the classes to the Lists of ServiceBindings
                Stream<List<ServiceBinding>> bindingLists = objectClasses.map(bindings::get).filter(Objects::nonNull);
                if (eventType == ServiceEvent.REGISTERED) {
                    bindingLists.forEach(list -> list.forEach(ServiceBinding::bindService));
                } else {
                    bindingLists.forEach(list -> list.forEach(ServiceBinding::unbindService));
                }
            }
        });
    }

}
