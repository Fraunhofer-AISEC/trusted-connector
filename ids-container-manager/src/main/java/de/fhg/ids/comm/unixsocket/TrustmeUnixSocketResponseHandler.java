/*-
 * ========================LICENSE_START=================================
 * IDS Container Manager
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
package de.fhg.ids.comm.unixsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustmeUnixSocketResponseHandler {
	private Logger LOG = LoggerFactory.getLogger(TrustmeUnixSocketResponseHandler.class);
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp.clone();
		this.notify();
		return true;
	}
	
	public synchronized byte[] waitForResponse() {
		while(this.rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				LOG.error(e.getMessage(),e);
			}
		}
		byte[] result = rsp;
		LOG.debug("received response byte length: {}", result.length);
		this.rsp = null;
		return result;
	}
}

