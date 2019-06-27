package de.fhg.aisec.tpm2j.tpms;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmu.TPMU_ASYM_SCHEME;
import de.fhg.aisec.tpm2j.tpmu.TPMU_SIG_SCHEME;

public class TPMS_SCHEME_RSASSA extends StandardTPMStruct implements TPMU_SIG_SCHEME, TPMU_ASYM_SCHEME {
	
	private TPMS_SCHEME_SIGHASH hashAlg;

	public TPMS_SCHEME_SIGHASH getHashAlg() {
		return hashAlg;
	}

	public void setHashAlg(TPMS_SCHEME_SIGHASH hashAlg) {
		this.hashAlg = hashAlg;
	} 

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashAlg);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
		this.hashAlg = new TPMS_SCHEME_SIGHASH();
        brw.readStruct(this.hashAlg);
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_RSASSA:[hashAlg=" + this.hashAlg.toString() + "]";
	}
}
