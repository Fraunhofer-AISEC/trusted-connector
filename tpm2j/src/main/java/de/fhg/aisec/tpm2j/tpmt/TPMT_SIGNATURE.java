package de.fhg.aisec.tpm2j.tpmt;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmi.TPMI_ALG_SIG_SCHEME;
import de.fhg.aisec.tpm2j.tpms.TPMS_SIGNATURE_ECDSA;
import de.fhg.aisec.tpm2j.tpms.TPMS_SIGNATURE_RSA;
import de.fhg.aisec.tpm2j.tpmu.TPMU_SIGNATURE;

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

	public TPMT_SIGNATURE(byte[] byteArray) throws Exception {
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
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.sigAlg = new TPMI_ALG_SIG_SCHEME();
        brw.readStruct(this.sigAlg);
        switch(this.sigAlg.getAlgId().getAlgId()) {
        	case TPM_ALG_RSASSA:
        	case TPM_ALG_RSAPSS:
        		this.signature = new TPMS_SIGNATURE_RSA();
        		brw.readStruct(this.signature);
        		break;
        	case TPM_ALG_ECDSA:
        	case TPM_ALG_SM2:
        	case TPM_ALG_ECDAA:
        	case TPM_ALG_ECSCHNORR:
        		this.signature = new TPMS_SIGNATURE_ECDSA();
        		brw.readStruct(this.signature);
        		break;	
        	default:
        		throw new Exception("TPMT_SIGNATURE algorithm not found.");
        }
	}

	@Override
	public String toString() {
		return "TPMT_SIGNATURE:[sigAlg=" + this.sigAlg.toString() + ", signature=" + this.signature.toString() + "]";
	}
}
