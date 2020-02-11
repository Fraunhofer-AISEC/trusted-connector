/*-
 * ========================LICENSE_START=================================
 * ids-api
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
package de.fhg.aisec.ids.api.tokenm;

import de.fhg.aisec.ids.api.settings.ConnectionSettings;

import java.nio.file.Path;
import java.util.Map;

/**
 * Interface of the Token Manager.
 *
 * <p>TThe Token Manager takes care of identity tokens (such as the Dynamic Attribute Token,
 * acquired by the Dynamic Attribute Provisioning Service.
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
public interface TokenManager {

  Map<String, Object> acquireToken(
      Path targetDirectory,
      String dapsUrl,
      String keyStoreName,
      String keyStorePassword,
      String keystoreAliasName,
      String trustStoreName,
      String connectorUUID);

  Map<String, Object> verifyJWT(
      String dynamicAttributeToken,
      String targetAudience,
      String dapsUrl) throws Exception;

  void validateDATSecurityAttributes(
      Map<String, Object> claims,
          ConnectionSettings connectionSettings) throws DatException;

}
