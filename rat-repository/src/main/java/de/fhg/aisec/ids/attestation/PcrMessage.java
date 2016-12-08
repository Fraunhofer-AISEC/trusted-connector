package de.fhg.aisec.ids.attestation;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;

public class PcrMessage {
	
	private String nonce;
	private boolean success = false;
	private String signature = "";
	private ConnectorMessage msg;
	
	public PcrMessage(ConnectorMessage msg) {
		this.msg = msg;
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

	public ConnectorMessage getMsg() {
		return msg;
	}

	public void setMsg(ConnectorMessage msg) {
		this.msg = msg;
	}
	
	public String toString() {
		String ret = "";
		if(msg != null) {
			ret += "\n*************************************************************************"
					+ "\nPCR Values :\n";
			for(int i = 0; i < msg.getAttestationResponse().getPcrValuesCount(); i++) {
				ret += "\t" + i + " \t" + msg.getAttestationResponse().getPcrValuesList().get(i) + "\n";
			}
			ret += "\n************************************************************************\n";
			
		}
		return ret;
	}
	
}
