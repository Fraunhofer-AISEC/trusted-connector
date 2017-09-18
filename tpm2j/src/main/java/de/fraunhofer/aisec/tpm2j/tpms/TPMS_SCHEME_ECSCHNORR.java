package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ASYM_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SIG_SCHEME;

public class TPMS_SCHEME_ECSCHNORR implements TPMU_ASYM_SCHEME, TPMU_SIG_SCHEME {
	
	// typedef TPMS_SCHEME_SIGHASH TPMS_SCHEME_ECSCHNORR;
	
	private TPMS_SCHEME_SIGHASH hashAlg;

	public TPMS_SCHEME_SIGHASH getHashId() {
		return hashAlg;
	}

	public void setHashId(TPMS_SCHEME_SIGHASH hashId) {
		this.hashAlg = hashId;
	}
	
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashAlg);
	}

	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.hashAlg = new TPMS_SCHEME_SIGHASH();
        brw.readStruct(this.hashAlg);
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_ECSCHNORR:[hashAlg=" + this.hashAlg.toString() + "]";
	}
}
