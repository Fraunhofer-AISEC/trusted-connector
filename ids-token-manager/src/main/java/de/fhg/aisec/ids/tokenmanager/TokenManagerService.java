/*-
 * ========================LICENSE_START=================================
 * ids-token-manager
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
package de.fhg.aisec.ids.tokenmanager;

import de.fhg.aisec.ids.api.settings.ConnectionSettings;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;
import de.fhg.aisec.ids.api.tokenm.TokenManager;
import de.fhg.aisec.ids.idscp2.default_drivers.daps.DefaultDapsDriver;
import de.fhg.aisec.ids.idscp2.default_drivers.daps.DefaultDapsDriverConfig;
import de.fhg.aisec.ids.idscp2.default_drivers.daps.SecurityRequirements;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;


/**
 * Manages Dynamic Attribute Tokens.
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
@Component(immediate = true, name = "ids-tokenmanager")
public class TokenManagerService implements TokenManager {
  private static final Logger LOG = LoggerFactory.getLogger(TokenManagerService.class);

  @SuppressWarnings("FieldMayBeFinal")
  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  private Settings settings = null;
  private DefaultDapsDriver driver;

  /**
   * Method to acquire a Dynamic Attribute Token (DAT) from a Dynamic Attribute Provisioning Service
   * (DAPS)
   * @param dapsUrl            The token aquiry URL (e.g., http://daps.aisec.fraunhofer.de
   * @param keyStorePath       Name of the keystore file (e.g., server-keystore.p12)
   * @param keyStorePassword   Password of keystore
   * @param keystoreAliasName  Alias of the connector's key entry.
   *                           For default keystores with only one entry, this is '1'
   * @param trustStorePath     Name of the truststore file
   * @param trustStorePassword Password of truststore
   */
  @Override
  public void acquireToken(
          String dapsUrl,
          Path keyStorePath,
          char[] keyStorePassword,
          String keystoreAliasName,
          char[] keyPassword,
          Path trustStorePath,
          char[] trustStorePassword) {

    final var dapsConfig = new DefaultDapsDriverConfig.Builder()
            .setDapsUrl(dapsUrl)
            .setKeyStorePath(keyStorePath)
            .setKeyStorePassword(keyStorePassword)
            .setKeyAlias(keystoreAliasName)
            .setKeyPassword(keyPassword)
            .setTrustStorePath(trustStorePath)
            .setTrustStorePassword(trustStorePassword)
            .build();
    this.driver = new DefaultDapsDriver(dapsConfig);
    settings.setDynamicAttributeToken(new String(driver.getToken(), StandardCharsets.UTF_8));
  }

  @Override
  public void verifyJWT(
      String dynamicAttributeToken,
      ConnectionSettings connectionSettings) {
    if (connectionSettings == null) {
      this.driver.verifyTokenSecurityAttributes(dynamicAttributeToken.getBytes(StandardCharsets.UTF_8), null);
    } else {
      SecurityRequirements securityRequirements = new SecurityRequirements.Builder()
              .setRequiredSecurityLevel(connectionSettings.getRequiredSecurityProfile())
              .build();
      this.driver.verifyTokenSecurityAttributes(dynamicAttributeToken.getBytes(StandardCharsets.UTF_8), securityRequirements);
    }
  }

  @Activate
  public void run() {
    LOG.info("Token renewal triggered.");
    try {
      ConnectorConfig config = settings.getConnectorConfig();
      var ksRoot = FileSystems.getDefault().getPath("etc");
      char[] ksPassword = config.getKeystorePassword().toCharArray();
      acquireToken(
              config.getDapsUrl(),
              ksRoot.resolve(config.getKeystoreName()),
              ksPassword,
              config.getKeystoreAliasName(),
              ksPassword,
              ksRoot.resolve(config.getTruststoreName()),
              ksPassword);

    } catch (Exception e) {
      LOG.error("Token renewal failed", e);
    }
  }
}
