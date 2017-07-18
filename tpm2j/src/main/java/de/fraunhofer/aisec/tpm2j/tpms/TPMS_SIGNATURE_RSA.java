package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID.ALG_ID;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC_KEY_RSA;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SIGNATURE;

public class TPMS_SIGNATURE_RSA extends TPMU_SIGNATURE {
	
	/* from:
	 * TPMS_SIGNATURE_RSASSA Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH        hash;
	 *     TPM2B_PUBLIC_KEY_RSA sig;
	 * } TPMS_SIGNATURE_RSASSA;
	 */
	
	private TPMI_ALG_HASH hashAlg;
	private TPM2B_PUBLIC_KEY_RSA sig;

	public void setSig(TPM2B_PUBLIC_KEY_RSA sig) {
		this.sig = sig;
	}

	@Override
	public byte[] getSig() {
		return this.sig.getBuffer();
	}

	public TPM_ALG_ID getHash() {
		return hashAlg.getHashId();
	}

	public void setHash(TPMI_ALG_HASH hash) {
		this.hashAlg = hash;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashAlg, sig);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.hashAlg = new TPMI_ALG_HASH();
        brw.readStruct(this.hashAlg);
        this.sig = new TPM2B_PUBLIC_KEY_RSA();
        brw.readStruct(this.sig);
	}

	@Override
	public String toString() {
		return "TPMS_SIGNATURE_RSA:[hashAlg=" + this.hashAlg.toString() 
			+ ", sig=" + this.sig.toString() + "]";
	}

	@Override
	public ALG_ID getHashAlg() {
		return this.hashAlg.getHashId().getAlgId();
	}
}
