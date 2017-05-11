package de.fhg.aisec.ids.api.conm;

/**
 * Bean representing an "IDSCP Connection" .
 * 
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
public class IDSCPConnection {
	String string_id;
	String attestationResult;
	
	public IDSCPConnection(String string_id, String attestationResult) {
		this.string_id = string_id;
		this.attestationResult = attestationResult;
	}
	
	public String getString_id() {
		return string_id;
	}
	public void setString_id(String string_id) {
		this.string_id = string_id;
	}
	public String getAttestationResult() {
		return attestationResult;
	}
	public void setAttestationResult(String attestationResult) {
		this.attestationResult = attestationResult;
	}
	
	@Override
	public String toString() {
		return "IDSCPConnection [id=" + string_id + ", attestationResult=" + attestationResult + "]";
	}
	
	
}
