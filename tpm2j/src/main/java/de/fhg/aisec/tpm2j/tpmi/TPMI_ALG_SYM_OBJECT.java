package de.fhg.aisec.tpm2j.tpmi;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpm.TPM_ALG_ID;

public class TPMI_ALG_SYM_OBJECT extends StandardTPMStruct {
	
	/* 
	 * TPMI_ALG_SYM_OBJECT Type
	 * typedef TPM_ALG_ID TPMI_ALG_SYM_OBJECT;
	 */
	
	private TPM_ALG_ID algId;
	
	public TPMI_ALG_SYM_OBJECT() {
	}
	
	public TPMI_ALG_SYM_OBJECT(byte[] buffer) throws Exception {
		this.fromBytes(buffer, 0);
	}	
	
	public TPMI_ALG_SYM_OBJECT(TPM_ALG_ID algId) {
		this.algId = algId;
	}
	
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
		return "TPMI_ALG_PUBLIC:[algId=" + this.algId.toString() + "]";
	}
}
