/*-
 * ========================LICENSE_START=================================
 * Dynamic TLS keystore extension for Pax Web
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
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.dynamictls;

import de.fhg.aisec.ids.api.acme.SslContextFactoryReloader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * This OSGi service encapsulates a HashSet of active SslContextFactory instances
 * for reloading them when required.
 *
 * @author Michael Lux
 */
public class JettySslContextFactoryReloader implements SslContextFactoryReloader {
    private static final Logger LOG = LoggerFactory.getLogger(JettySslContextFactoryReloader.class);
    private static final Set<SslContextFactory> factories = new HashSet<>();

    public static void addFactory(SslContextFactory factory) {
        synchronized (factories) {
            factories.add(factory);
        }
    }

    public static void removeFactory(SslContextFactory factory) {
        synchronized (factories) {
            factories.remove(factory);
        }
    }

    @Override
    public void reloadAll() {
        synchronized (factories) {
            LOG.info("Reloading " + factories.size() + " SslContentFactory instance(s) of Jetty");
            factories.forEach(factory -> {
                try {
                    factory.reload(f -> {});
                } catch (Exception e) {
                    LOG.error("Error whilst reloading a SslContextFactory", e);
                }
            });
        }
    }
}
