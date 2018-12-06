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
package de.fhg.ids.comm.client;

import java.net.URI;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.CertificatePair;
import de.fhg.ids.comm.IdscpConfiguration;

/**
 * Configuration of a client-side (Consumer) IDSC endpoint.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
public class ClientConfiguration implements IdscpConfiguration {
  @NonNull
  private IdsAttestationType attestationType = IdsAttestationType.BASIC;
  private int attestationMask = 0;
  @NonNull
  private CertificatePair certificatePair = new CertificatePair();
  @NonNull
  private String endpoint = "";
  private boolean disableServerVerification = false;
  @Nullable
  protected URI ttpUri;
  
  public static class Builder {
	@NonNull
    private ClientConfiguration config = new ClientConfiguration();

    public Builder attestationMask(int attestationMask) {
      config.attestationMask = attestationMask;
      return this;
    }

    @NonNull
    public Builder attestationType(@Nullable IdsAttestationType attestationType) {
      if (attestationType != null) {
    	config.attestationType = attestationType;
      }
      return this;
    }

    @NonNull
    public Builder certificatePair(@Nullable CertificatePair certificatePair) {
      if (certificatePair != null) {
    	config.certificatePair = certificatePair;
      }
      return this;
    }

    @NonNull
    public Builder ttpUrl(@Nullable URI ttpUri) {
    	if (ttpUri != null) {
    	  config.ttpUri = ttpUri;
    	}
    	return this;
    }

    @NonNull
    public Builder endpoint(@Nullable String endpoint) {
    	if (endpoint != null) {
    		config.endpoint = endpoint;
    	}
    	return this;
    }
    
    public Builder setDisableServerVerification(boolean disableServerVerification) {
    	config.disableServerVerification = disableServerVerification;
    	return this;
    }
    
    @NonNull
    public ClientConfiguration build() {
      return config;
    }
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

  @NonNull
  public String getEndpoint() {
	  return endpoint;
  }
  
  public boolean isDisableServerVerification() {
	  return disableServerVerification;
  }
}
