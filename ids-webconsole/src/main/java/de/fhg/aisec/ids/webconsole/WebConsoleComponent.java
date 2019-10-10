/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
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
package de.fhg.aisec.ids.webconsole;

import de.fhg.aisec.ids.api.acme.AcmeClient;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.settings.Settings;
import de.fhg.aisec.ids.api.tokenm.TokenManager;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.osgi.service.component.annotations.*;

/**
 * IDS management console, reachable at http://localhost:8181.
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
@Component(name = "ids-webconsole")
public class WebConsoleComponent {
  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private TokenManager tokenManager = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private Settings settings = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private ContainerManager cml = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private AcmeClient acmeClient = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private RouteManager rm = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private ConnectionManager connectionManager = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private PAP pap = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private InfoModel im = null;

  private static WebConsoleComponent instance;

  @Activate
  @SuppressWarnings("squid:S2696")
  protected void activate() {
    instance = this;
  }

  @Deactivate
  @SuppressWarnings("squid:S2696")
  protected void deactivate() {
    instance = null;
  }

  @Nullable
  public static ContainerManager getContainerManager() {
    WebConsoleComponent in = instance;
    if (in != null) {
      return in.cml;
    }
    return null;
  }

  @Nullable
  public static AcmeClient getAcmeClient() {
    WebConsoleComponent in = instance;
    if (in != null) {
      return in.acmeClient;
    }
    return null;
  }

  @Nullable
  public static ConnectionManager getConnectionManager() {
    WebConsoleComponent in = instance;
    if (in != null) {
      return in.connectionManager;
    }
    return null;
  }

  @Nullable
  public static TokenManager getTokenManager() {
    WebConsoleComponent in = instance;
    if (in != null) {
      return in.tokenManager;
    }
    return null;
  }

  @Nullable
  public static Settings getSettings() {
    WebConsoleComponent in = instance;
    if (in != null) {
      return in.settings;
    }
    return null;
  }

  @Nullable
  public static RouteManager getRouteManager() {
    WebConsoleComponent in = instance;
    if (in != null) {
      return in.rm;
    }
    return null;
  }

  @Nullable
  public static PAP getPolicyAdministrationPoint() {
    WebConsoleComponent in = instance;
    if (in != null) {
      return in.pap;
    }
    return null;
  }

  @Nullable
  public static InfoModel getInfoModelManager() {
    WebConsoleComponent in = instance;
    if (in != null) {
      return in.im;
    }
    return null;
  }
}
