package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPM2B_DIGEST extends StandardTPMStruct {
	
	/*
	 * TPM2B_DIGEST Structure
	 * typedef struct 
	 *     UINT16 size;
	 *     BYTE   buffer[sizeof(TPMU_HA)];
	 * } TPM2B_DIGEST;
	 */

	private short size = 0;
	private byte[] buffer = new byte[0];
	
	public short getSize() {
		return size;
	}

	public void setSize(short size) {
		this.size = size;
	}

	public TPM2B_DIGEST() {
	}
	
	public TPM2B_DIGEST(byte[] buffer) {
		this.setBuffer(buffer);
	}
	
	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(this.getSize(), this.buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.size = brw.readShort();
        this.setBuffer(brw.readBytes(this.size));
	}

	@Override
	public String toString() {
		return "TPM2B_DIGEST (" + this.getSize() + " bytes): "
	            + ByteArrayUtil.toPrintableHexString(this.buffer);
	}
}
