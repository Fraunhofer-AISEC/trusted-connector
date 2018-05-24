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
package de.fhg.ids.comm.client;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import java.util.concurrent.ExecutionException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import de.fhg.aisec.ids.api.conm.AttestationResult;

public class IdscpClient {

	private ClientConfiguration config = new ClientConfiguration();
	private AttestationResult attestationResult = null;
	
	public WebSocket connect(String host, int port) throws InterruptedException, ExecutionException {
		AsyncHttpClient c = asyncHttpClient();

		// Connect to web socket
		IdspClientSocket wsListener = new IdspClientSocket(this.config);
		WebSocket ws = c.prepareGet("ws://"+host+":"+port+"/")
				.execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(wsListener).build()).get();
		
		// Block until ISCP has finished
		wsListener.semaphore().lockInterruptibly();
        try {
        	while (!wsListener.isTerminated()) {
        		wsListener.idscpInProgressCondition().await();
        	}
        } finally {
        	this.attestationResult = wsListener.getAttestationResult();
        	wsListener.semaphore().unlock();
        }		
        return ws;
	}
	
	public IdscpClient config(ClientConfiguration config) {
		this.config = config;
		return this;
	}

	/**
	 * Returns null if attestation has not yet finished, or status code of remote attestation otherwise.
	 */
	public AttestationResult getAttestationResult() {
		return this.attestationResult;
	}
}
