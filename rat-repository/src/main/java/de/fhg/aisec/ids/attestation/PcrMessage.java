package de.fhg.aisec.ids.attestation;

import java.util.Arrays;

public class PcrMessage {
	
	private String nonce;
	private boolean success = false;
	private String signature = "";
	private PcrValue[] values;
	
	public PcrMessage(PcrValue[] values) {
		this.values = values;
	}

	public PcrMessage(PcrValue[] values, String freshNonce) {
		this.values = values;
		this.nonce = freshNonce;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public PcrValue[] getValues() {
		return values;
	}
	
	public PcrValue getValue(int i) {
		return values[i];
	}	

	public void setValues(PcrValue[] values) {
		this.values = values;
	}
	
	public String toString() {
		Arrays.sort(values, PcrValue.orderComparator);
		String ret = "";
		if(values != null) {
			ret += "\n*************************************************************************"
					+ "\nPCR Values :\n";
			for(int i = 0; i < values.length; i++) {
				ret += "\t" + i + " \t" + values[i].getValue() + "\n";
			}
			ret += "\n************************************************************************\n";
			
		}
		return ret;
	}


	
}
