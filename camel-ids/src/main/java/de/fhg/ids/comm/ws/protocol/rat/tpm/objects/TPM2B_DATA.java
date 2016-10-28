package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

import de.fhg.ids.comm.ws.protocol.rat.tpm.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm.tools.ByteArrayUtil;

public class TPM2B_DATA extends StandardTPMStruct {
	
	/*
	 * TPM2B_DATA Structure
	 * typedef struct {
	 *     UINT16 size;
	 *     BYTE   buffer[sizeof(TPMT_HA)];
	 * } TPM2B_DATA;
	 */
	
	private byte[] buffer = new byte[0];
	
	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public int getBufferLength() {
		return this.buffer.length;
	}

	@Override
	public byte[] toBytes() {
		int bufferLength = this.getBufferLength();
		return ByteArrayUtil.buildBuf(bufferLength, buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        short keyLength = brw.readShort();
        this.setBuffer(brw.readBytes(keyLength));
	}

	@Override
	public String toString() {
		return "TPM2B_DATA:[(" + this.getBufferLength() + " bytes): " + ByteArrayUtil.toPrintableHexString(this.buffer) + "]";
	}

}
