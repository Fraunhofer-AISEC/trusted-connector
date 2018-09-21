/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
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
package de.fhg.aisec.ids.webconsole;

import javax.ws.rs.ServiceUnavailableException;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import de.fhg.aisec.ids.api.acme.AcmeClient;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.settings.Settings;

/**
 * IDS management console, reachable at http://localhost:8181/ids/ids.html.
 *
 * <p>This OSGi component registers a web application, as soon as it is bound to an <code>
 * HttpService</code> interface. The web application will allow to control and configure the IDS
 * Core Platform and the connected services.
 *
 * <p>Note that the web console does not enforce any authorization or encryption and provides
 * everyone with the ability to control highly critical settings of the core platform. It must only
 * be used for demonstration purposes and must be deactivated and/or removed in productive use.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
public class WebConsoleComponent {

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private static Settings settings = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private static ContainerManager cml = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private static AcmeClient acmeClient = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private static RouteManager rm = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private static ConnectionManager connectionManager = null;

  private static PAP pap = null;
  public static ContainerManager getContainerManagerOrThrowSUE() {
    if (cml != null) {
      return cml;
    } else {
      throw new ServiceUnavailableException("ConnectionManager is currently not available");
    }
  }

  public static AcmeClient getAcmeClient() {
    if (acmeClient != null) {
      return acmeClient;
    } else {
      throw new ServiceUnavailableException("ACME client is not available");
    }
  }

  public static ConnectionManager getConnectionManagerOrThrowSUE() {
    if (connectionManager != null) {
      return connectionManager;
    } else {
      throw new ServiceUnavailableException("ConnectionManager is currently not available");
    }
  }

  public static Settings getSettingsOrThrowSUE() {
    if (settings != null) {
      return settings;
    } else {
      throw new ServiceUnavailableException("Settings are currently not available");
    }
  }

  public static RouteManager getRouteManagerOrThrowSUE() {
    if (rm != null) {
      return rm;
    } else {
      throw new ServiceUnavailableException("RouteManager is currently not available");
    }
  }

  public static PAP getPolicyAdministrationPointOrThrowSUE() {
    if (pap != null) {
      return pap;
    } else {
      throw new ServiceUnavailableException("PAP is currently not available");
    }
  }

  private WebConsoleComponent() {}
}
