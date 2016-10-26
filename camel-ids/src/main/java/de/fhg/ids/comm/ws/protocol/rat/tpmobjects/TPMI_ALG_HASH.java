package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMI_ALG_HASH extends StandardTPMStruct {

	private TPM_ALG_ID hashId;

	public TPM_ALG_ID getHashId() {
		return hashId;
	}

	public void setHashId(TPM_ALG_ID hashId) {
		this.hashId = hashId;
	}
	
	public String toString() {
		return "TPMI_ALG_HASH : " + this.hashId.toString();
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
}
