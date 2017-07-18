package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ASYM_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SIG_SCHEME;

public class TPMS_SCHEME_HMAC implements TPMU_SIG_SCHEME, TPMU_ASYM_SCHEME {
	/*
	 * HMAC_SIG_SCHEME Types
	 * typedef TPMS_SCHEME_SIGHASH TPMS_SCHEME_HMAC;
	 */
	
	TPMS_SCHEME_SIGHASH hashId;

	public TPMS_SCHEME_SIGHASH getHashId() {
		return hashId;
	}

	public void setHashId(TPMS_SCHEME_SIGHASH hashId) {
		this.hashId = hashId;
	}

	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashId);
	}

	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.hashId = new TPMS_SCHEME_SIGHASH();
        brw.readStruct(this.hashId);
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_HMAC:[hashId="+this.hashId.toString()+"]";
	}
}
