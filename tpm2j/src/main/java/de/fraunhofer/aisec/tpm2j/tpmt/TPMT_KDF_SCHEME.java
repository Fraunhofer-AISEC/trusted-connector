package de.fraunhofer.aisec.tpm2j.tpmt;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_KDF;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_SCHEME_KDF1_SP800_108;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_SCHEME_KDF1_SP800_56a;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_SCHEME_KDF2;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_SCHEME_MGF1;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_KDF_SCHEME;

public class TPMT_KDF_SCHEME extends StandardTPMStruct {

	/*
	 * TPMT_KDF_SCHEME Structure
	 * typedef struct {
	 *     TPMI_ALG_KDF    scheme;
	 *     TPMU_KDF_SCHEME details;
	 * } TPMT_KDF_SCHEME;
	 */
	
	TPMI_ALG_KDF scheme;
	TPMU_KDF_SCHEME details;
	
	public TPMI_ALG_KDF getScheme() {
		return scheme;
	}
	
	public void setScheme(TPMI_ALG_KDF scheme) {
		this.scheme = scheme;
	}
	
	public TPMU_KDF_SCHEME getDetails() {
		return details;
	}
	
	public void setDetails(TPMU_KDF_SCHEME details) {
		this.details = details;
	}
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(scheme, details);
	}
	
	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.scheme = new TPMI_ALG_KDF();
        brw.readStruct(this.scheme);
        switch(this.scheme.getAlgId().getAlgId()) {
	        case TPM_ALG_MGF1:
	        	this.details = new TPMS_SCHEME_MGF1();
	        	brw.readStruct(this.details);
	        	break;
	        case TPM_ALG_KDF1_SP800_56a:
	        	this.details = new TPMS_SCHEME_KDF1_SP800_56a();
	        	brw.readStruct(this.details);
	        	break;	  
	        case TPM_ALG_KDF2:
	        	this.details = new TPMS_SCHEME_KDF2();
	        	brw.readStruct(this.details);
	        	break;	  
	        case TPM_ALG_KDF1_SP800_108:
	        	this.details = new TPMS_SCHEME_KDF1_SP800_108();
	        	brw.readStruct(this.details);
	        	break;	        	
        	default:
        		throw new Exception("TPMT_KDF_SCHEME KDF algorithm not found.");
        }
	}
	
	@Override
	public String toString() {
		return "TPMT_KDF_SCHEME:[scheme=" + this.scheme.toString()
			+ ", details=" + this.details.toString() + "]";
	}
}
