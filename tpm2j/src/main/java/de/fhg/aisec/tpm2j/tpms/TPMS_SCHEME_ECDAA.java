package de.fhg.aisec.tpm2j.tpms;

import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fhg.aisec.tpm2j.tpmu.TPMU_ASYM_SCHEME;
import de.fhg.aisec.tpm2j.tpmu.TPMU_SIG_SCHEME;

public class TPMS_SCHEME_ECDAA implements TPMU_ASYM_SCHEME, TPMU_SIG_SCHEME {
	
	/*
	 * TPMS_SCHEME_ECDAA Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH hashAlg;
	 *     UINT16        count;
	 * } TPMS_SCHEME_ECDAA;
	 */
	
	private TPMI_ALG_HASH hashAlg;
	private short count;

	public TPMI_ALG_HASH getHashId() {
		return hashAlg;
	}

	public void setHashId(TPMI_ALG_HASH hashAlg) {
		this.hashAlg = hashAlg;
	}

	public short getCount() {
		return count;
	}

	public void setCount(short count) {
		this.count = count;
	}

	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashAlg, count);
	}

	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.hashAlg = new TPMI_ALG_HASH();
        brw.readStruct(this.hashAlg);
        this.count = brw.readShort();
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_ECDAA:[hashAlg=" + this.hashAlg.toString() 
				+ ", count=" + this.count + "]";
	}
}
