package de.fhg.ids.comm.ws.protocol.fsm;

public class Event {
	private String key;
	private String payload;
	
	public Event(String key, String payload) {
		super();
		this.key = key;
		this.payload = payload;
	}
	public String getKey() {
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
