package de.fraunhofer.aisec.tpm2j.tpmi;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID;

public class TPMI_ALG_ASYM_SCHEME extends StandardTPMStruct {
	/*
	 * TPMI_ALG_ASYM_SCHEME Type
	 * typedef TPM_ALG_ID TPMI_ALG_ASYM_SCHEME;
	 */

	private TPM_ALG_ID algId;
	
	public TPMI_ALG_ASYM_SCHEME() {
	}
	
	public TPMI_ALG_ASYM_SCHEME(TPM_ALG_ID id) {
		this.algId = id;
	}	

	public TPMI_ALG_ASYM_SCHEME(byte[] buffer) throws Exception {
		this.fromBytes(buffer, 0);
	}

	public TPM_ALG_ID getAlgId() {
		return algId;
	}

	public void setAlgId(TPM_ALG_ID hashId) {
		this.algId = hashId;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(algId);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.algId = new TPM_ALG_ID();
        brw.readStruct(this.algId);
	}
	
	@Override
    public String toString() {
		return "TPMI_ALG_ASYM_SCHEME:[algId=" + this.algId.toString() + "]";
    }

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 	
}
