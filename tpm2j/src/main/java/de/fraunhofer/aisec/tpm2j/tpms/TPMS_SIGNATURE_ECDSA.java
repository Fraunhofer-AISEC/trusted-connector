package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID.ALG_ID;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_ECC_PARAMETER;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SIGNATURE;

public class TPMS_SIGNATURE_ECDSA extends TPMU_SIGNATURE {
	
	/*
	 * TPMS_SIGNATURE_ECDSA Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH       hash;
	 *     TPM2B_ECC_PARAMETER signatureR;
	 *     TPM2B_ECC_PARAMETER signatureS;
	 * } TPMS_SIGNATURE_ECDSA;
	 */
	
	private TPMI_ALG_HASH hashAlg;
	private TPM2B_ECC_PARAMETER signatureR;
	private TPM2B_ECC_PARAMETER signatureS;
	
	public TPMI_ALG_HASH getHash() {
		return hashAlg;
	}

	public void setHash(TPMI_ALG_HASH hash) {
		this.hashAlg = hash;
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
		return ByteArrayUtil.buildBuf(hashAlg, signatureR, signatureS);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.hashAlg = new TPMI_ALG_HASH();
        brw.readStruct(this.hashAlg);
        this.signatureR = new TPM2B_ECC_PARAMETER();
        brw.readStruct(this.signatureR);
        this.signatureS = new TPM2B_ECC_PARAMETER();
        brw.readStruct(this.signatureS);
	}

	@Override
	public String toString() {
		return "TPMS_SIGNATURE_ECDSA:[hashAlg=" + this.hashAlg.toString()  
			+ ", signatureR=" + this.signatureR.toString()
			+ ", signatureS=" + this.signatureS.toString()+ "]";
	}

	@Override
	public byte[] getSig() {
		return ByteArrayUtil.buildBuf(hashAlg, signatureR, signatureS);
	}

	@Override
	public ALG_ID getHashAlg() {
		return this.hashAlg.getHashId().getAlgId();
	}

}
