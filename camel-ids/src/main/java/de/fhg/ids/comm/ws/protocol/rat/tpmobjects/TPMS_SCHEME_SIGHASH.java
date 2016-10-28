package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMS_SCHEME_SIGHASH extends TPMU_ASYM_SCHEME {
	/*
	 * TPMI_ALG_ASYM_SCHEME Type
	 * typedef TPM_ALG_ID TPMI_ALG_ASYM_SCHEME;
	 */

	private TPMI_ALG_HASH hashId;
	
	public TPMS_SCHEME_SIGHASH() {
	}
	
	public TPMS_SCHEME_SIGHASH(TPMI_ALG_HASH id) {
		this.hashId = id;
	}	

	public TPMS_SCHEME_SIGHASH(byte[] buffer) {
		this.fromBytes(buffer, 0);
	}

	public TPMI_ALG_HASH getAlgId() {
		return hashId;
	}

	public void setAlgId(TPMI_ALG_HASH hashId) {
		this.hashId = hashId;
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashId);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.hashId = new TPMI_ALG_HASH();
        brw.readStruct(this.hashId);
	}
	
	@Override
    public String toString() {
		return "TPMS_SCHEME_SIGHASH:[hashId = " + this.hashId.toString() + "]";
    }	
}