/*-
 * ========================LICENSE_START=================================
 * camel-ids
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
package de.fhg.aisec.ids.camel.ids;

import de.fhg.aisec.ids.api.endpointconfig.EndpointConfigManager;
import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fhg.aisec.ids.api.settings.Settings;
import de.fhg.aisec.ids.api.tokenm.TokenManager;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.osgi.service.component.annotations.*;

@Component
public class CamelComponent {

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private Settings settings = null;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  private InfoModel infoModelManager = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private TokenManager tokenManager = null;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private EndpointConfigManager endpointConfigManager = null;

  private static CamelComponent instance;

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
  public static Settings getSettings() {
    CamelComponent in = instance;
    if (in != null) {
      return in.settings;
    }
    return null;
  }

  @Nullable
  public static InfoModel getInfoModelManager() {
    CamelComponent in = instance;
    if (in != null) {
      return in.infoModelManager;
    }
    return null;
  }

  @Nullable
  public static TokenManager getTokenManager() {
    CamelComponent in = instance;
    if (in != null) {
      return in.tokenManager;
    }
    return null;
  }

  @Nullable
  public static EndpointConfigManager getEndpointConfigManager() {
    CamelComponent in = instance;
    if (in != null) {
      return in.endpointConfigManager;
    }
    return null;
  }
}
