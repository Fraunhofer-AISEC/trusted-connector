package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayable;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ASYM_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SIG_SCHEME;

public class TPMS_SCHEME_SIGHASH implements TPMU_ASYM_SCHEME, TPMU_SIG_SCHEME {
	/*
	 * TPMI_ALG_ASYM_SCHEME Type
	 * typedef TPM_ALG_ID TPMI_ALG_ASYM_SCHEME;
	 */

	private TPMI_ALG_HASH hashAlg;
	
	public TPMS_SCHEME_SIGHASH() {
	}
	
	public TPMS_SCHEME_SIGHASH(TPMI_ALG_HASH id) {
		this.hashAlg = id;
	}	

	public TPMS_SCHEME_SIGHASH(byte[] buffer) throws Exception {
		this.fromBytes(buffer, 0);
	}

	public TPMI_ALG_HASH getAlgId() {
		return hashAlg;
	}

	public void setAlgId(TPMI_ALG_HASH hashId) {
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
		return "TPMS_SCHEME_SIGHASH:[hashAlg=" + this.hashAlg.toString() + "]";
    }	
}