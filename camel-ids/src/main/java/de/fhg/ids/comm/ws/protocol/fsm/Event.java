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
