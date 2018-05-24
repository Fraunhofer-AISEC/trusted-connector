/*-
 * ========================LICENSE_START=================================
 * IDS Communication Protocol
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
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

import java.io.File;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;

/**
 * Configuration of the server-side (Provider) part of the IDSC protocol.
 * 
 * @author julian
 *
 */
public class ServerConfiguration {
	protected int port = 8080;
	protected String basePath = "/";
	protected IdsAttestationType attestationType = IdsAttestationType.BASIC;
	protected File tpmdSocket;
	protected int attestationMask;
	
	public ServerConfiguration port(int port) {
		this.port = port;
		return this;
	}
	
	public ServerConfiguration basePath(String basePath) {
		this.basePath = basePath;
		return this;
	}
	
	public ServerConfiguration tpmdSocket(File socket) {
		this.tpmdSocket = socket;
		return this;
	}
	
	public ServerConfiguration attestationMask(int mask) {
		this.attestationMask = mask;
		return this;
	}
	
	public ServerConfiguration attestationType(IdsAttestationType type) {
		this.attestationType = type;
		return this;
	}

}
