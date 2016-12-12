package de.fhg.aisec.ids.attestation;

import java.util.Comparator;

public class PcrValue implements Comparable<PcrValue> {
	private int order;
	private String value;
	
	public PcrValue(int i, String s) {
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

	@Override
	public int compareTo(PcrValue o) {
		return this.order - o.order;
	}
	
	public static Comparator<PcrValue> orderComparator = new Comparator<PcrValue>() {
		public int compare(PcrValue value1, PcrValue value2) {
			//ascending order
			return value1.compareTo(value2);
		}
	};
}
