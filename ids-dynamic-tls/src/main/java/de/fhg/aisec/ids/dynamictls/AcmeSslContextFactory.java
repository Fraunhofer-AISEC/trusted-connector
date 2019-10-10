/*-
 * ========================LICENSE_START=================================
 * ids-dynamic-tls
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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

import de.fhg.aisec.ids.api.acme.SslContextFactoryReloadable;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This SslContextFactory registers started instances to an OSGi service that allows reloading of
 * all active SslContextFactory instances.
 *
 * @author Michael Lux
 */
public class AcmeSslContextFactory extends SslContextFactory.Server
    implements SslContextFactoryReloadable {
  private static final Logger LOG = LoggerFactory.getLogger(AcmeSslContextFactory.class);
  private ServiceRegistration<SslContextFactoryReloadable> serviceRegistration;

  @Override
  protected void doStart() throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Starting {}", this);
    }
    BundleContext bundleContext =
        FrameworkUtil.getBundle(AcmeSslContextFactory.class).getBundleContext();
    serviceRegistration =
        bundleContext.registerService(SslContextFactoryReloadable.class, this, null);
    super.doStart();
  }

  @Override
  protected void doStop() throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Stopping {}", this);
    }
    serviceRegistration.unregister();
    super.doStop();
  }

  @Override
  public void reload(String newKeyStorePath) {
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("Reloading {}", this);
      }
      this.reload(f -> f.setKeyStorePath(newKeyStorePath));
    } catch (Exception e) {
      LOG.error("Error whilst reloading SslContextFactory: " + this.toString(), e);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%x (%s)", this.getClass().getSimpleName(), this.hashCode(), this.getKeyStorePath());
  }
}
