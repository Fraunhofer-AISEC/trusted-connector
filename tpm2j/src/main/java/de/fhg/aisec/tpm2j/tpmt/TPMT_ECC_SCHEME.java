package de.fhg.aisec.tpm2j.tpmt;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmi.TPMI_ALG_ECC_SCHEME;
import de.fhg.aisec.tpm2j.tpms.*;
import de.fhg.aisec.tpm2j.tpmu.TPMU_SIG_SCHEME;

public class TPMT_ECC_SCHEME extends StandardTPMStruct {

	/*
	 * TPMT_ECC_SCHEME Structure
	 * typedef struct {
	 *     TPMI_ALG_ECC_SCHEME scheme;
	 *     TPMU_SIG_SCHEME     details;
	 * } TPMT_ECC_SCHEME;
	 */
	
	TPMI_ALG_ECC_SCHEME scheme;
	TPMU_SIG_SCHEME details;
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(scheme, details);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.scheme = new TPMI_ALG_ECC_SCHEME();
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
	        case TPM_ALG_ECDSA:
	        	this.details = new TPMS_SCHEME_ECDSA();
	        	brw.readStruct(this.details);
	        	break;
	        case TPM_ALG_SM2:
	        	this.details = new TPMS_SCHEME_SM2();
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
	        case TPM_ALG_HMAC:
	        	this.details = new TPMS_SCHEME_HMAC();
	        	brw.readStruct(this.details);
	        	break;
	       	default:
        		throw new Exception("TPMT_ECC_SCHEME signature scheme not found");
        		
        }
	}

	@Override
	public String toString() {
		return "TPMT_ECC_SCHEME:[scheme=" + this.scheme.toString()
			+ ", details=" + this.details.toString() + "]";
	}

}
