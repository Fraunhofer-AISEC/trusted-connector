package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;

public class TPM2B_DATA extends StandardTPMStruct {
	
	/*
	 * TPM2B_DATA Structure
	 * typedef struct {
	 *     UINT16 size;
	 *     BYTE   buffer[sizeof(TPMT_HA)];
	 * } TPM2B_DATA;
	 */
	
	private short size = 0;
	private byte[] buffer = new byte[0];
	
	public short getSize() {
		return size;
	}

	public void setSize(short size) {
		this.size = size;
	}
	
	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public int getBufferLength() {
		return this.getSize();
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(size, buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.size = brw.readShort();
        this.setBuffer(brw.readBytes(this.size));
	}

	@Override
	public String toString() {
		return "TPM2B_DATA:[(" + this.getBufferLength() + " bytes): " + ByteArrayUtil.toPrintableHexString(this.buffer) + "]";
	}

}
