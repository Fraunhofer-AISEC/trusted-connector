package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmt;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID.ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_RSA_SCHEME;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SCHEME_ECDAA;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SCHEME_ECDSA;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SCHEME_ECSCHNORR;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SCHEME_OAEP;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SCHEME_RSAPSS;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SCHEME_RSASSA;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SCHEME_SIGHASH;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SCHEME_SM2;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_ASYM_SCHEME;

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
        TPMS_SCHEME_SIGHASH anySig = new TPMS_SCHEME_SIGHASH();
        switch(this.scheme.getAlgId().getAlgId()) {
        	case TPM_ALG_RSASSA:
        		this.details = new TPMS_SCHEME_RSASSA();
        		brw.readStruct(this.details);
                brw.readStruct(anySig);
        		this.details.setAnySig(anySig);
        		break;
        	case TPM_ALG_RSAPSS:
        		this.details = new TPMS_SCHEME_RSAPSS();
        		brw.readStruct(this.details);
                brw.readStruct(anySig);
        		this.details.setAnySig(anySig);
        		break;
        	case TPM_ALG_OAEP:
        		this.details = new TPMS_SCHEME_OAEP();
        		brw.readStruct(this.details);
                brw.readStruct(anySig);
        		this.details.setAnySig(anySig);
        		break;
        	case TPM_ALG_ECDSA:
        		this.details = new TPMS_SCHEME_ECDSA();
        		brw.readStruct(this.details);
                brw.readStruct(anySig);
        		this.details.setAnySig(anySig);
        		break;  
        	case TPM_ALG_SM2:
        		this.details = new TPMS_SCHEME_SM2();
        		brw.readStruct(this.details);
                brw.readStruct(anySig);
        		this.details.setAnySig(anySig);
        		break;  
        	case TPM_ALG_ECDAA:
        		this.details = new TPMS_SCHEME_ECDAA();
        		brw.readStruct(this.details);
                brw.readStruct(anySig);
        		this.details.setAnySig(anySig);
        		break; 
        	case TPM_ALG_ECSCHNORR:
        		this.details = new TPMS_SCHEME_ECSCHNORR();
                brw.readStruct(anySig);
        		this.details.setAnySig(anySig);
        		brw.readStruct(this.details);
        		break;
        	case TPM_ALG_NULL:
        		break;         		
        	default:
        		LOG.debug("error: no available TPMI_ALG_RSA_SCHEME given.");
        		break;
        }
	}

	@Override
    public String toString() {
		if(this.scheme.getAlgId().getAlgId().equals(TPM_ALG_ID.ALG_ID.TPM_ALG_NULL)) {
			return "TPMT_RSA_SCHEME:[scheme = " + this.scheme.toString() + "]";
		}
		else {
			return "TPMT_RSA_SCHEME:[scheme = " + this.scheme.toString() + ", details = " + this.details.toString() + "]";
		}
    }
}
