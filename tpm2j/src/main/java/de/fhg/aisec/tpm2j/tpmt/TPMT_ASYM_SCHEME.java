package de.fhg.aisec.tpm2j.tpmt;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmi.TPMI_ALG_ASYM_SCHEME;
import de.fhg.aisec.tpm2j.tpms.*;
import de.fhg.aisec.tpm2j.tpmu.TPMU_ASYM_SCHEME;

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
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(scheme, details);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.scheme = new TPMI_ALG_ASYM_SCHEME();
        brw.readStruct(this.scheme);
        switch(this.scheme.getAlgId().getAlgId()) {
        	case TPM_ALG_RSASSA:
        		this.details = new TPMS_SCHEME_RSASSA();
        		brw.readStruct(this.details);
        		break;
        	case TPM_ALG_RSAPSS:
        		this.details = new TPMS_SCHEME_RSAPSS();
        		brw.readStruct(this.details);
        		break;   
        	case TPM_ALG_OAEP:
        		this.details = new TPMS_SCHEME_OAEP();
        		brw.readStruct(this.details);
        		break;   
        	case TPM_ALG_ECDSA:
        		this.details = new TPMS_SCHEME_ECDSA();
        		brw.readStruct(this.details);
        		break;   
        	case TPM_ALG_ECDAA:
        		this.details = new TPMS_SCHEME_ECDAA();
        		brw.readStruct(this.details);
        		break;   
        	case TPM_ALG_ECSCHNORR:
        		this.details = new TPMS_SCHEME_ECSCHNORR();
        		brw.readStruct(this.details);
        		break; 
        	default:
        		this.details = new TPMS_SCHEME_SIGHASH();
        		brw.readStruct(this.details);
        }
	}

	@Override
	public String toString() {
        return "TPMT_ASYM_SCHEME:[" 
                + "scheme=" + this.scheme.toString() 
                + "details=" + this.details.toString() + "]";
	}
	
}
