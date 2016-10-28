package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMI_ALG_HASH extends StandardTPMStruct {
	/*
	 * TPMI_ALG_HASH Type
	 * typedef TPM_ALG_ID TPMI_ALG_HASH;
	 */

	private TPM_ALG_ID hashId;
	
	public TPMI_ALG_HASH() {
	}
	
	public TPMI_ALG_HASH(TPM_ALG_ID id) {
		this.hashId = id;
	}	

	public TPMI_ALG_HASH(byte[] buffer) {
		this.fromBytes(buffer, 0);
	}

	public TPM_ALG_ID getHashId() {
		return hashId;
	}

	public void setHashId(TPM_ALG_ID hashId) {
		this.hashId = hashId;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashId);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.hashId = new TPM_ALG_ID();
        brw.readStruct(this.hashId);
	}
	
	@Override
    public String toString() {
		return "TPMI_ALG_HASH:[hashId = " + this.hashId.toString() + "]";
    }	

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
}
