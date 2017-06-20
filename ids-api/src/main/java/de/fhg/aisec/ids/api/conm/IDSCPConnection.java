package de.fhg.aisec.ids.api.conm;

/**
 * Bean representing an "IDSCP Connection".
 * 
 * IDSCP is the IDS Communication Protocol, a TLS+WebSocket-based protocol including remote attestation for secure data transfers. 
 * 
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
public class IDSCPConnection {
	private String endpointIdentifier;
	private String lastProtocolState;
	
	public IDSCPConnection() {
		/* Bean std c'tor */
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
	public String getAttestationResult() {
		return lastProtocolState;
	}
	public void setAttestationResult(String lastProtocolState) {
		this.lastProtocolState = lastProtocolState;
	}
	
	@Override
	public String toString() {
		return "IDSCPConnection [endpoint_identifier=" + endpointIdentifier + ", lastProtocolState=" + lastProtocolState + "]";
	}
}
