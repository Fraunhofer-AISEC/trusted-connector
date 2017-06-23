package de.fhg.aisec.ids.api.conm;

/**
 * Bean representing an "IDSCP Connection" .
 * 
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
public class IDSCPConnection {
	private String endpointIdentifier;
	private String lastProtocolState;
	private boolean attestationResult;
	private String remoteAuthentication;
	private String remoteIdentity;
	
	public IDSCPConnection() {	
	}
	
	// TODO JS: Never used. Remove?
	public IDSCPConnection(String endpointIdentifier, String lastProtocolState) {
		this.endpointIdentifier = endpointIdentifier;
		this.lastProtocolState = lastProtocolState;
	}
	
	public String getEndpointIdentifier() {
		return endpointIdentifier;
	}
	public void setEndpointIdentifier(String endpointIdentifier) {
		this.endpointIdentifier = endpointIdentifier;
	}
	public boolean getAttestationResult() {
		return attestationResult;
	}
	public void setAttestationResult(boolean result) {
		this.attestationResult = result;
	}
	public String getLastProtocolState() {
		return lastProtocolState;
	}
	public void setLastProtocolState(String lastProtocolState) {
		this.lastProtocolState = lastProtocolState;
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
	
	@Override
	public String toString() {
		return "IDSCPConnection [endpoint_identifier=" + endpointIdentifier 
				+ ", lastProtocolState=" + lastProtocolState
				+ ", attestationResult=" + attestationResult 
				+ ", remoteAuthentication=" + remoteAuthentication
				+ ", remoteIdentity=" + remoteIdentity+ "]";
	}
}
