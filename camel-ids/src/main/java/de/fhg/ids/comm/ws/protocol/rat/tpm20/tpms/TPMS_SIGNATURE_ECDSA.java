package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID.ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b.TPM2B_ECC_PARAMETER;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_HASH;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_SIGNATURE;

public class TPMS_SIGNATURE_ECDSA extends TPMU_SIGNATURE {
	
	/*
	 * TPMS_SIGNATURE_ECDSA Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH       hash;
	 *     TPM2B_ECC_PARAMETER signatureR;
	 *     TPM2B_ECC_PARAMETER signatureS;
	 * } TPMS_SIGNATURE_ECDSA;
	 */
	
	private TPMI_ALG_HASH hash;
	private TPM2B_ECC_PARAMETER signatureR;
	private TPM2B_ECC_PARAMETER signatureS;
	
	public TPMI_ALG_HASH getHash() {
		return hash;
	}

	public void setHash(TPMI_ALG_HASH hash) {
		this.hash = hash;
	}

	public TPM2B_ECC_PARAMETER getSignatureR() {
		return signatureR;
	}

	public void setSignatureR(TPM2B_ECC_PARAMETER signatureR) {
		this.signatureR = signatureR;
	}

	public TPM2B_ECC_PARAMETER getSignatureS() {
		return signatureS;
	}

	public void setSignatureS(TPM2B_ECC_PARAMETER signatureS) {
		this.signatureS = signatureS;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hash, signatureR, signatureS);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.hash = new TPMI_ALG_HASH();
        brw.readStruct(this.hash);
        this.signatureR = new TPM2B_ECC_PARAMETER();
        brw.readStruct(this.signatureR);
        this.signatureS = new TPM2B_ECC_PARAMETER();
        brw.readStruct(this.signatureS);
	}

	@Override
	public String toString() {
		return "TPMS_SIGNATURE_ECDSA:[hash = " + this.hash.toString()  
			+ ", signatureR = " + this.signatureR.toString()
			+ ", signatureS = " + this.signatureS.toString()+ "]";
	}

	@Override
	public byte[] getSig() {
		return ByteArrayUtil.buildBuf(hash, signatureR, signatureS);
	}

	@Override
	public ALG_ID getHashAlg() {
		return this.hash.getHashId().getAlgId();
	}

}
