package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ASYM_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SIG_SCHEME;

public class TPMS_SCHEME_OAEP implements TPMU_ASYM_SCHEME, TPMU_SIG_SCHEME {
	
	private TPMI_ALG_HASH hashAlg;

	public TPMI_ALG_HASH getHashId() {
		return hashAlg;
	}

	public void setHashId(TPMI_ALG_HASH hashId) {
		this.hashAlg = hashId;
	}

	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashAlg);
	}

	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.hashAlg = new TPMI_ALG_HASH();
        brw.readStruct(this.hashAlg);
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_OAEP:[hashAlg=" + this.hashAlg.toString() + "]";
	}
}
