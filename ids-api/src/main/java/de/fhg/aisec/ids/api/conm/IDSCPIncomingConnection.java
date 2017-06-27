package de.fhg.aisec.ids.api.conm;

/**
 * Bean representing an "IDSCP Connection" .
 * 
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
	
public class IDSCPIncomingConnection {
	private String endpointIdentifier;

	private String attestationResult;

	
	public IDSCPIncomingConnection() {
		// TODO Auto-generated constructor stub
	}
	
	public String getEndpointIdentifier() {
		return endpointIdentifier;
	}
	public void setEndpointIdentifier(String endpointIdentifier) {
		this.endpointIdentifier = endpointIdentifier;
	}
	public String getAttestationResult() {
		return attestationResult;
	}
	public void setAttestationResult(String result) {
		this.attestationResult = result;
	}	
	
	@Override
	public String toString() {
		return "IDSCPConnection [endpoint_identifier=" + endpointIdentifier 
				+ ", attestationResult=" + attestationResult + "]";
	}
}
