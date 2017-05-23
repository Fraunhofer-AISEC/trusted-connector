package de.fhg.aisec.ids.api.conm;

/**
 * Bean representing an "IDSCP Connection" .
 * 
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
public class IDSCPConnection {
	String endpoint_identifier;
	String lastProtocolState;
	
	public IDSCPConnection(String endpoint_identifier, String lastProtocolState) {
		this.endpoint_identifier = endpoint_identifier;
		this.lastProtocolState = lastProtocolState;
	}
	
	public IDSCPConnection() {
		// TODO Auto-generated constructor stub
	}

	public String getEndpointIdentifier() {
		return endpoint_identifier;
	}
	public void setEndpointIdentifier(String endpoint_identifier) {
		this.endpoint_identifier = endpoint_identifier;
	}
	public String getAttestationResult() {
		return lastProtocolState;
	}
	public void setAttestationResult(String lastProtocolState) {
		this.lastProtocolState = lastProtocolState;
	}
	
	@Override
	public String toString() {
		return "IDSCPConnection [endpoint_identifier=" + endpoint_identifier + ", lastProtocolState=" + lastProtocolState + "]";
	}
	
	
}
