/*-
 * ========================LICENSE_START=================================
 * ids-dynamic-endpoint-config
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
package de.fhg.aisec.ids.dynamicendpointconfig;

import de.fhg.aisec.ids.api.dynamicEndpointConfig.DynamicEndpointConfigManager;
import de.fhg.aisec.ids.api.dynamicEndpointConfig.EndpointConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages EndpointConfigListeners and provide service for notifying all EndpointConfigListeners, that some endpoint
 * configuration has changed
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
@Component(immediate = true, name = "ids-dynamic-endpoint-config")
public class DynamicEndpointConfigService implements DynamicEndpointConfigManager {
  private static final Logger LOG = LoggerFactory.getLogger(DynamicEndpointConfigService.class);

  private Set<EndpointConfigListener> endpointListeners =
          Collections.synchronizedSet(new HashSet<>());

  /*
   * The following block subscribes this component to any EndpointConfigListeners.
   * A EndpointConfigListener is expected to refresh dynamicAttributeToken validation of client with new endpointConfig.
   */

  @Reference(
          name = "dynamic-endpoint-config",
          service = EndpointConfigListener.class,
          cardinality = ReferenceCardinality.MULTIPLE,
          unbind = "unbindEndpointConfigListener")
  private void bindEndpointConfigListener(EndpointConfigListener endpointListener) {
    LOG.warn("Bound EndpointConfigListener in DynamicEndpointConfigService");
    this.endpointListeners.add(endpointListener);
  }

  @SuppressWarnings("unused")
  private void unbindEndpointConfigListener(EndpointConfigListener endpointListener) {
    this.endpointListeners.remove(endpointListener);
  }


  @Activate
  public void run() {
    LOG.info("Dynamic Endpoint Configuration Service has been triggered.");
  }

  @Override
  public void notifyAll(String endpointConfig) {
    //each endpoint config listener should be notified that an endpoint config has been changed
    for (EndpointConfigListener endpointListener : this.endpointListeners) {
      endpointListener.updateTokenValidation(endpointConfig);
    }
  }
}
