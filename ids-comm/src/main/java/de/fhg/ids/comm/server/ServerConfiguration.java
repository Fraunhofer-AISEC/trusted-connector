/*-
 * ========================LICENSE_START=================================
 * ids-comm
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
package de.fhg.ids.comm.server;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.CertificatePair;
import de.fhg.ids.comm.IdscpConfiguration;
import java.net.URI;
import java.security.KeyStore;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Configuration of the server-side (Provider) part of the IDSC protocol.
 *
 * @author Julian Schuette
 */
public class ServerConfiguration implements IdscpConfiguration {
  public static final int DEFAULT_PORT = 8080;

  private int port = DEFAULT_PORT;
  @NonNull
  private IdsAttestationType attestationType = IdsAttestationType.BASIC;
  private int attestationMask;
  @NonNull
  private CertificatePair certificatePair = new CertificatePair();
  @Nullable
  private KeyStore keyStore = null;
  @Nullable
  private URI ttpUri = null;
  @NonNull
  private String keyStorePassword = "password";
  @NonNull
  private String keyManagerPassword = "password";
  @NonNull
  private String certAlias = "1";

  public static class Builder {
	  @NonNull
    private ServerConfiguration config = new ServerConfiguration();

	  @NonNull
    public Builder port(int port) {
      config.port = port;
      return this;
    }

    @NonNull
    public Builder attestationType(@NonNull IdsAttestationType attestationType) {
      config.attestationType = attestationType;
      return this;
    }

    @NonNull
    public Builder attestationMask(int attestationMask) {
      config.attestationMask = attestationMask;
      return this;
    }

    @NonNull
    public Builder certificatePair(@NonNull CertificatePair certificatePair) {
      config.certificatePair = certificatePair;
      return this;
    }

    @NonNull
    public Builder ttpUrl(@NonNull URI ttpUri) {
    	config.ttpUri = ttpUri;
    	return this;
    }

    @NonNull
    public Builder setKeyStore(@NonNull KeyStore keyStore) {
    	config.keyStore = keyStore;
    	return this;
    }
    
    @NonNull
    public Builder setKeyStorePassword(@NonNull String keyStorePassword) {
      config.keyStorePassword = keyStorePassword;
      return this;
    }
    
    @NonNull
    public Builder setKeyPassword(@NonNull String keyPassword) {
      config.keyManagerPassword = keyPassword;
      return this;
    }
    
    @NonNull
    public Builder setCertAlias(@NonNull String certAlias) {
      config.certAlias = certAlias;
      return this;
    }
    
    @NonNull
    public ServerConfiguration build() {
      return config;
    }
  }

  public int getPort() {
    return port;
  }

  @NonNull
  public IdsAttestationType getAttestationType() {
    return attestationType;
  }

  public int getAttestationMask() {
    return attestationMask;
  }

  @NonNull
  public CertificatePair getCertificatePair() {
    return certificatePair;
  }
  
  @Nullable
  public URI getTrustedThirdPartyURI() {
	  return ttpUri;
  }

  @Nullable
  public KeyStore getKeyStore() {
	  return keyStore;
  }

  public String getKeyStorePassword() {
    return keyStorePassword ;
  }

  public String getKeyManagerPassword() {
	    return keyManagerPassword ;
 }

  public String getCertAlias() {
    return certAlias ;
  }
}
