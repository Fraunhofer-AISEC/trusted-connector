package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ASYM_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SIG_SCHEME;

public class TPMS_SCHEME_RSAPSS implements TPMU_ASYM_SCHEME, TPMU_SIG_SCHEME {
	
	private TPMS_SCHEME_SIGHASH hashAlg;

	public TPMS_SCHEME_SIGHASH getScheme() {
		return hashAlg;
	}

	public void setScheme(TPMS_SCHEME_SIGHASH hashAlg) {
		this.hashAlg = hashAlg;
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
		return "TPMS_SCHEME_RSAPSS:[hashAlg=" + this.hashAlg.toString() + "]";
	}
}
