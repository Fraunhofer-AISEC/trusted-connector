package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMT_RSA_SCHEME extends StandardTPMStruct {

	/*
	 * TPMT_RSA_SCHEME Structure
	 * typedef struct {
	 *     TPMI_ALG_RSA_SCHEME scheme;
	 *     TPMU_ASYM_SCHEME    details;
	 * } TPMT_RSA_SCHEME;
	 */
	
	private TPMI_ALG_RSA_SCHEME scheme;
	private TPMU_ASYM_SCHEME details;
	
	public TPMI_ALG_RSA_SCHEME getScheme() {
		return scheme;
	}

	public void setScheme(TPMI_ALG_RSA_SCHEME scheme) {
		this.scheme = scheme;
	}

	public TPMU_ASYM_SCHEME getDetails() {
		return details;
	}

	public void setDetails(TPMU_ASYM_SCHEME details) {
		this.details = details;
	}
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(scheme, details);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.scheme = new TPMI_ALG_RSA_SCHEME();
        brw.readStruct(this.scheme);
        // todo reconstruct details from byte       
	}

	@Override
    public String toString() {
        return "TPMT_RSA_SCHEME:[\n" 
        		+ "type = " + this.scheme.toString() + "\n"
        		+ "nameAlg = " + this.details.toString() + "\n]\n";
    }
}
