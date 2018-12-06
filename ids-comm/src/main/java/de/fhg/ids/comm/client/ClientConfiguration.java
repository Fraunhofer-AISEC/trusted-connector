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

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.CertificatePair;
import de.fhg.ids.comm.IdscpConfiguration;

/**
 * Configuration of a client-side (Consumer) IDSC endpoint.
 *
 * @author julian
 */
public class ClientConfiguration implements IdscpConfiguration {
  private IdsAttestationType attestationType = IdsAttestationType.BASIC;
  private int attestationMask = 0;
  private CertificatePair certificatePair = new CertificatePair();
  private String endpoint = "";
  private boolean disableServerVerification = false;
  protected URI ttpUri;
  
  public static class Builder {
    private ClientConfiguration config = new ClientConfiguration();

    public Builder attestationMask(int attestationMask) {
      config.attestationMask = attestationMask;
      return this;
    }

    public Builder attestationType(IdsAttestationType attestationType) {
      config.attestationType = attestationType;
      return this;
    }

    public Builder certificatePair(CertificatePair certificatePair) {
      config.certificatePair = certificatePair;
      return this;
    }

    public Builder ttpUrl(URI ttpUri) {
    	config.ttpUri = ttpUri;
    	return this;
    }

    public Builder endpoint(String endpoint) {
    	if (endpoint != null) {
    		config.endpoint = endpoint;
    	}
    	return this;
    }
    
    public Builder setDisableServerVerification(boolean disableServerVerification) {
    	config.disableServerVerification = disableServerVerification;
    	return this;
    }
    
    public ClientConfiguration build() {
      return config;
    }
  }

  public IdsAttestationType getAttestationType() {
    return attestationType;
  }

  public int getAttestationMask() {
    return attestationMask;
  }

  public CertificatePair getCertificatePair() {
    return certificatePair;
  }

  public URI getTrustedThirdPartyURI() {
	  return ttpUri;
  }

  public String getEndpoint() {
	  return endpoint;
  }
  
  public boolean isDisableServerVerification() {
	  return disableServerVerification;
  }
}
