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
package de.fhg.ids.comm.ws.protocol.fsm;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;

/**
 * An FSM event which may trigger a transition.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class Event {
	private Object key;
	private String payload;
	private ConnectorMessage msg;
	
	public Event(Object key, String payload, ConnectorMessage msg) {
		super();
		this.setKey(key);
		this.setPayload(payload);
		this.setMessage(msg);
	}
	public Object getKey() {
		return key;
	}
	public void setKey(Object key) {
		this.key = key;
	}
	public ConnectorMessage getMessage() {
		return this.msg;
	}
	public void setMessage(ConnectorMessage evt) {
		this.msg = evt;
	}
	public String getPayload() {
		return this.payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}	
}
