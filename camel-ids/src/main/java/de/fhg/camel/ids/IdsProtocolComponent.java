/*-
 * ========================LICENSE_START=================================
 * camel-ids
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.camel.ids;

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import java.util.Optional;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component binding dynamically to the OSGi preferences service.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public class IdsProtocolComponent {
  private static final Logger LOG = LoggerFactory.getLogger(IdsProtocolComponent.class);
  private static Optional<PreferencesService> prefService = Optional.empty();
  private static Optional<ConnectionManager> connectionManager = Optional.empty();

  @Reference(
    name = "camel-ids.config.service",
    service = PreferencesService.class,
    cardinality = ReferenceCardinality.OPTIONAL,
    policy = ReferencePolicy.DYNAMIC,
    unbind = "unbindConfigurationService"
  )
  public void bindConfigurationService(PreferencesService conf) {
    LOG.info("Bound to configuration service");
    IdsProtocolComponent.prefService = Optional.of(conf);
  }

  public void unbindConfigurationService(PreferencesService conf) {
    IdsProtocolComponent.prefService = Optional.empty();
  }

  public static Optional<PreferencesService> getPreferencesService() {
    return prefService;
  }

  @Reference(
    name = "connections.service",
    service = ConnectionManager.class,
    cardinality = ReferenceCardinality.OPTIONAL,
    policy = ReferencePolicy.DYNAMIC,
    unbind = "unbindConnectionManager"
  )
  public void bindConnectionManager(ConnectionManager conn) {
    LOG.info("Bound to connection manager");
    IdsProtocolComponent.connectionManager = Optional.of(conn);
  }

  public void unbindConnectionManager(ConnectionManager conn) {
    IdsProtocolComponent.connectionManager = Optional.empty();
  }

  public static Optional<ConnectionManager> getConnectionManager() {
    return IdsProtocolComponent.connectionManager;
  }
}
