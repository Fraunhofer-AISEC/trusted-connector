package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPM2B_NAME extends StandardTPMStruct {
	
	/*
	 *  TPM2B_NAME Structure
	 *  typedef struct {
	 *      UINT16 size;
	 *      BYTE   name[sizeof(TPMU_NAME)];
	 *  } TPM2B_NAME;
	 */
	
	private byte[] name = new byte[0];
	
	public byte[] getName() {
		return name;
	}
	
	public void setName(byte[] name) {
		this.name = name;
	}

	public int getNameLength() {
		return this.name.length;
	}
	
	@Override
	public byte[] toBytes() {
		int bufferLength = this.getNameLength();
		return ByteArrayUtil.buildBuf(bufferLength, name);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        short keyLength = brw.readShort();
        this.setName(brw.readBytes(keyLength));
	}

	@Override
	public String toString() {
		return "TPM2B_NAME (" + this.getNameLength() + " bytes): "
	            + ByteArrayUtil.toPrintableHexString(this.name);
	}
}
