package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_KDF_SCHEME;

public class TPMS_SCHEME_KDF1_SP800_56a extends TPMU_KDF_SCHEME {
	
	/*
	 * TPMS_SCHEME_KDF1_SP800_56a Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH hashAlg;
	 * } TPMS_SCHEME_KDF1_SP800_56a;
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
		return "TPMS_SCHEME_KDF1_SP800_56a:[hashAlg="+this.hashAlg.toString()+"]";
	}

}
