package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmt;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_SIG_SCHEME;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SIGNATURE_ECDSA;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_SIGNATURE_RSAPSS;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_SIGNATURE;

public class TPMT_SIGNATURE extends StandardTPMStruct {
	
	/*
	 * TPMT_SIGNATURE Structure
	 * typedef struct {
	 *     TPMI_ALG_SIG_SCHEME sigAlg;
	 *     TPMU_SIGNATURE      signature;
	 * } TPMT_SIGNATURE;
	 */
	
	private TPMI_ALG_SIG_SCHEME sigAlg;
	private TPMU_SIGNATURE signature;

	public TPMT_SIGNATURE(byte[] byteArray) {
		this.fromBytes(byteArray, 0);
	}

	public TPMI_ALG_SIG_SCHEME getSigAlg() {
		return sigAlg;
	}

	public void setSigAlg(TPMI_ALG_SIG_SCHEME sigAlg) {
		this.sigAlg = sigAlg;
	}

	public TPMU_SIGNATURE getSignature() {
		return signature;
	}
	
	public void setSignature(TPMU_SIGNATURE signature) {
		this.signature = signature;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(sigAlg, signature);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.sigAlg = new TPMI_ALG_SIG_SCHEME();
        brw.readStruct(this.sigAlg);
        LOG.debug("sig scheme:" + ByteArrayUtil.toHexString(this.sigAlg.getAlgId().Id()));
        switch(this.sigAlg.getAlgId().getAlgId()) {
        	case TPM_ALG_RSASSA:
        		// TODO !
        		break;
        	case TPM_ALG_RSAPSS:        		
        		this.signature = new TPMS_SIGNATURE_RSAPSS();
        		brw.readStruct(this.signature);
        		break;         	
        	case TPM_ALG_ECDSA:
        	case TPM_ALG_SM2:
        	case TPM_ALG_ECDAA:
        		// TODO !
        		break;
        	case TPM_ALG_ECSCHNORR:
            	this.signature = new TPMS_SIGNATURE_ECDSA();
            	brw.readStruct(this.signature);
            	break;		
        	default:
        		LOG.debug("error: \""+ this.sigAlg.getAlgId().getAlgId().toString() +"\" (name:\"" + this.sigAlg.getAlgId().getAlgId().name() + "\") signature algorithm found (not yet all implemented)");
        		break;
        }
	}

	@Override
	public String toString() {
		return "TPMT_SIGNATURE:[sigAlg = " + this.sigAlg.toString() + ", signature = " + this.signature.toString() + "]";
	}
}
