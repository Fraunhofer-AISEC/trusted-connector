package de.fhg.aisec.ids.api.conm;

/**
 * Bean representing an "IDSCP Connection" .
 * 
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
public class IDSCPOutgoingConnection {
	private String endpoint_identifier;
	private String lastProtocolState;
	private String remoteAuthentication;
	private String remoteIdentity;
	
	
	public IDSCPOutgoingConnection(String endpoint_identifier, String lastProtocolState) {
		this.endpoint_identifier = endpoint_identifier;
		this.lastProtocolState = lastProtocolState;
	}
	
	public IDSCPOutgoingConnection() {
		// TODO Auto-generated constructor stub
	}
	
	public String getRemoteAuthentication() {
		return remoteAuthentication;
	}
	public void setRemoteAuthentication(String state) {
		this.remoteAuthentication = state;
	}
	public String getRemoteIdentity() {
		return remoteIdentity;
	}
	public void setRemoteIdentity(String hostname) {
		this.remoteIdentity = hostname;
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
	public String getLastProtocolState() {
		return lastProtocolState;
	}
	public void setLastProtocolState(String lastProtocolState) {
		this.lastProtocolState = lastProtocolState;
	}	
	@Override
	public String toString() {
		return "IDSCPOutgoingConnection [endpoint_identifier=" + endpoint_identifier + ", lastProtocolState=" + lastProtocolState + "]";
	}
	
	
}
