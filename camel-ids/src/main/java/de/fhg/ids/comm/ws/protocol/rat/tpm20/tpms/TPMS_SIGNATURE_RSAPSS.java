package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID.ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b.TPM2B_PUBLIC_KEY_RSA;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_HASH;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_SIGNATURE;

public class TPMS_SIGNATURE_RSAPSS extends TPMU_SIGNATURE {
	
	/*
	 * TPMS_SIGNATURE_RSAPSS Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH        hash;
	 *     TPM2B_PUBLIC_KEY_RSA sig;
	 * } TPMS_SIGNATURE_RSAPSS;
	 */
	
	private TPMI_ALG_HASH hash;
	private TPM2B_PUBLIC_KEY_RSA sig;

	public TPMI_ALG_HASH getHash() {
		return hash;
	}

	public void setHash(TPMI_ALG_HASH hash) {
		this.hash = hash;
	}

	public void setSig(TPM2B_PUBLIC_KEY_RSA sig) {
		this.sig = sig;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hash, sig);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.hash = new TPMI_ALG_HASH();
        brw.readStruct(this.hash);
        this.sig = new TPM2B_PUBLIC_KEY_RSA();
        brw.readStruct(this.sig);
	}

	@Override
	public String toString() {
		return "TPMS_SIGNATURE_RSAPSS:[hash = " + this.hash.toString() + ", sig = " + this.sig.toString() + "]";
	}

	@Override
	public byte[] getSig() {
		return this.sig.getBuffer();
	}

	@Override
	public ALG_ID getHashAlg() {
		return this.hash.getHashId().getAlgId();
	}
}
