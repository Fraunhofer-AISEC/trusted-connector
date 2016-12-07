package de.fhg.aisec.ids.attestation;

public class Pcr {
	private int order;
	private String value;
	
	public Pcr(int i, String s) {
		this.order = i;
		this.value = s;
	}
	
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String toString() {
		return "[" + this.order + ":" + this.value + "]";
	}
}
