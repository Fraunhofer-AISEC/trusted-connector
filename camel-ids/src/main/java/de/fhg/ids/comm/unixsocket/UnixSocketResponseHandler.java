/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnixSocketResponseHandler {
	private Logger LOG = LoggerFactory.getLogger(UnixSocketResponseHandler.class);
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp;
		this.notify();
		return true;
	}
	
	public synchronized byte[] waitForResponse() {
		while(this.rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
		//int length = new BigInteger(pop(this.rsp, 4, true)).intValue();
		//System.out.println("UnixSocketResponsHandler recieved : " + this.rsp.length + " bytes");
		return slice(this.rsp, this.rsp.length, false);
	}
	
	static byte[] slice(byte[] original, int length, boolean fromLeft) {
		if(fromLeft) {
			return Arrays.copyOfRange(original, length, original.length);
		}
		else {
			return Arrays.copyOfRange(original, original.length - length, original.length);
		}
	}
	
	static byte[] pop(byte[] original, int length, boolean fromLeft) {
		if(fromLeft) {
			return Arrays.copyOfRange(original, 0, length);
		}
		else {
			return Arrays.copyOfRange(original, original.length - length, length);
		}
	}
}

