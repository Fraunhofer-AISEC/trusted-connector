package de.fhg.ids.comm.ws.protocol.fsm;

public class Event {
	private Object key;
	private String payload;
	
	public Event(Object key, String payload) {
		super();
		this.key = key;
		this.payload = payload;
	}
	public Object getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getPayload() {
		return payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}

}
