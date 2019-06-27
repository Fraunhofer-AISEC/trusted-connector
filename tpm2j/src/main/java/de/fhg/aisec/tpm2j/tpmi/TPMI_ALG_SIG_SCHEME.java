package de.fhg.aisec.tpm2j.tpmi;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpm.TPM_ALG_ID;

public class TPMI_ALG_SIG_SCHEME extends StandardTPMStruct {

	/*
	 * TPMI_ALG_SIG_SCHEME Type
	 * typedef TPM_ALG_ID TPMI_ALG_SIG_SCHEME;
	 */
	
	private TPM_ALG_ID algId;

	public TPM_ALG_ID getAlgId() {
		return algId;
	}

	public void setAlgId(TPM_ALG_ID algId) {
		this.algId = algId;
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
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
        return "TPMI_ALG_SIG_SCHEME:[algId=" + this.algId.toString() + "]";
    }

}
