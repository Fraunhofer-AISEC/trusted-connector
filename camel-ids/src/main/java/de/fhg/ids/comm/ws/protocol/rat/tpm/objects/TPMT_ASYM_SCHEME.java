package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

import de.fhg.ids.comm.ws.protocol.rat.tpm.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm.tools.ByteArrayUtil;

public class TPMT_ASYM_SCHEME extends StandardTPMStruct {

	/*
	 * TPMT_ASYM_SCHEME Structure
	 * typedef struct {
	 *     TPMI_ALG_ASYM_SCHEME scheme;
	 *     TPMU_ASYM_SCHEME     details;
	 * } TPMT_ASYM_SCHEME;
	 */
	
	private TPMI_ALG_ASYM_SCHEME scheme;
	private TPMU_ASYM_SCHEME details;
	
	public TPMI_ALG_ASYM_SCHEME getScheme() {
		return scheme;
	}

	public void setScheme(TPMI_ALG_ASYM_SCHEME scheme) {
		this.scheme = scheme;
	}

	public TPMU_ASYM_SCHEME getDetails() {
		return details;
	}

	public void setDetails(TPMU_ASYM_SCHEME details) {
		this.details = details;
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(scheme, details);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.scheme = new TPMI_ALG_ASYM_SCHEME();
        brw.readStruct(this.scheme);
        switch(this.scheme.getAlgId().getAlgId()) {
        	case TPM_ALG_RSASSA:
        		this.details = new TPMS_SCHEME_RSASSA();
        		break;
        	case TPM_ALG_RSAPSS:
        		this.details = new TPMS_SCHEME_RSAPSS();
        		break;   
        	case TPM_ALG_OAEP:
        		this.details = new TPMS_SCHEME_OAEP();
        		break;   
        	case TPM_ALG_ECDSA:
        		this.details = new TPMS_SCHEME_ECDSA();
        		break;   
        	case TPM_ALG_ECDAA:
        		this.details = new TPMS_SCHEME_ECDAA();
        		break;   
        	case TPM_ALG_ECSCHNORR:
        		this.details = new TPMS_SCHEME_ECSCHNORR();
        		break;          		
        	default:
        		LOG.debug("error: algorithm not found !");
        		break;
        }
        brw.readStruct(this.details);
        this.details.setAnySig(new TPMS_SCHEME_SIGHASH());
        brw.readStruct(this.details.getAnySig());
	}

	@Override
	public String toString() {
        return "TPMT_ASYM_SCHEME:[\n" 
                + "scheme := " + this.scheme.toString() + "\n" 
                + "details := " + this.details.toString() + "\n]\n";
	}
	
}
