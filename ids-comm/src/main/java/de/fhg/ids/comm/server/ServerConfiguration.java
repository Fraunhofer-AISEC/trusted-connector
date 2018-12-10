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

import java.net.URI;
import java.security.KeyStore;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.CertificatePair;
import de.fhg.ids.comm.IdscpConfiguration;

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
  private CertificatePair certificatePair = new CertificatePair();
  private boolean disableClientCertificateValidation = false;
  private KeyStore keystore = null;
  private URI ttpUri = null;

  public static class Builder {
	@NonNull
    private ServerConfiguration config = new ServerConfiguration();

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
    public Builder ttpUrl(@Nullable URI ttpUri) {
    	if (ttpUri != null) {
    		config.ttpUri = ttpUri;
    	}
    	return this;
    }
    
    public Builder setDisableClientCertificateValidation(boolean disable) {
    	config.disableClientCertificateValidation = disable;
    	return this;
    }
    
    public Builder setKeyStore(KeyStore keystore) {
    	config.keystore = keystore;
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

  @Nullable
  public CertificatePair getCertificatePair() {
    return certificatePair;
  }
  
  @Nullable
  public URI getTrustedThirdPartyURI() {
	  return ttpUri;
  }
  
  public boolean isDisableClientCertificateValidation() {
	  return disableClientCertificateValidation;
  }

  @Nullable
  public KeyStore getKeyStore(){
	  return keystore;
  }
}
