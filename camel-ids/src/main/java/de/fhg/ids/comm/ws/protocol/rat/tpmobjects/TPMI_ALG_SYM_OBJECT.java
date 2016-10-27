package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMI_ALG_SYM_OBJECT extends StandardTPMStruct {
	
	/* 
	 * TPMI_ALG_SYM_OBJECT Type
	 * typedef TPM_ALG_ID TPMI_ALG_SYM_OBJECT;
	 */
	
	private TPM_ALG_ID algId;
	
	public TPMI_ALG_SYM_OBJECT() {
		// nothing
	}
	
	public TPMI_ALG_SYM_OBJECT(byte[] buffer) {
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
	
	public String toString() {
		return "TPMI_ALG_PUBLIC : " + this.algId.toString();
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(algId);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.algId = new TPM_ALG_ID();
        brw.readStruct(this.algId);
	}
	
}
