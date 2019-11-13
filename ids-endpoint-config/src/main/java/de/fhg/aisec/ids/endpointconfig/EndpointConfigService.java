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
package de.fhg.aisec.ids.endpointconfig;

import de.fhg.aisec.ids.api.endpointconfig.EndpointConfigListener;
import de.fhg.aisec.ids.api.endpointconfig.EndpointConfigManager;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages EndpointConfigListeners and provide service for notifying all EndpointConfigListeners, that some endpoint
 * configuration has changed
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
@Component(immediate = true, name = "ids-dynamic-endpoint-config")
public class EndpointConfigService implements EndpointConfigManager {
  private static final Logger LOG = LoggerFactory.getLogger(EndpointConfigService.class);

  private Map<String, EndpointConfigListener> endpointListeners = new ConcurrentHashMap<>();

  /*
   * The following block subscribes this component to any EndpointConfigListeners.
   * A EndpointConfigListener is expected to refresh dynamicAttributeToken validation of client with new endpointConfig.
   */

  @Override
  public void addEndpointConfigListener(String identifier, EndpointConfigListener endpointListener) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Register EndpointConfigListener: {}", identifier);
    }
    this.endpointListeners.put(identifier, endpointListener);
  }

  @Override
  public void removeEndpointConfigListener(String identifier) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Unregister EndpointConfigListener: {}", identifier);
    }
    this.endpointListeners.remove(identifier);
  }

  @Override
  public void notify(String identifier) {
    //each endpoint config listener should be notified that an endpoint config has been changed
    EndpointConfigListener listener = endpointListeners.get(identifier);
    if (listener != null) {
      listener.updateTokenValidation();
    } else {
      LOG.warn("Identifier {} has no known listener, no token validation updated.", identifier);
    }
  }
}
