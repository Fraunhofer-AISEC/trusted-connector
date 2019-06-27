package de.fhg.aisec.tpm2j.tpms;

import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fhg.aisec.tpm2j.tpmu.TPMU_KDF_SCHEME;

public class TPMS_SCHEME_MGF1 extends TPMU_KDF_SCHEME {
	
	/*
	 * TPMS_SCHEME_MGF1 Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH hashAlg;
	 * } TPMS_SCHEME_MGF1;
	 */
	
	TPMI_ALG_HASH hashAlg;

	public TPMI_ALG_HASH getHashAlg() {
		return hashAlg;
	}

	public void setHashAlg(TPMI_ALG_HASH hashAlg) {
		this.hashAlg = hashAlg;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashAlg);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.hashAlg = new TPMI_ALG_HASH();
        brw.readStruct(this.hashAlg);
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_MGF1:[hashAlg=" +this.hashAlg.toString()+"]";
	}

}
