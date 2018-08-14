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

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import java.io.File;

/**
 * Configuration of a client-side (Consumer) IDSC endpoint.
 *
 * @author julian
 */
public class ClientConfiguration {
  protected int port = 8080;
  protected File tpmdSocket;
  protected IdsAttestationType attestationType = IdsAttestationType.BASIC;
  protected int attestationMask = 0;

  public ClientConfiguration port(int port) {
    this.port = port;
    return this;
  }

  public ClientConfiguration tpmdSocket(File socket) {
    this.tpmdSocket = socket;
    return this;
  }

  public ClientConfiguration attestationMask(int attestationMask) {
    this.attestationMask = attestationMask;
    return this;
  }

  public ClientConfiguration attestationType(IdsAttestationType attestationType) {
    this.attestationType = attestationType;
    return this;
  }
}
