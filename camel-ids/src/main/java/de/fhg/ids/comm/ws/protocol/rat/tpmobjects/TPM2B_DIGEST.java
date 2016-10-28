package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ids.comm.ws.protocol.ProtocolMachine;

public class TPM2B_DIGEST extends StandardTPMStruct {
	
	/*
	 * TPM2B_DIGEST Structure
	 * typedef struct 
	 *     UINT16 size;
	 *     BYTE   buffer[sizeof(TPMU_HA)];
	 * } TPM2B_DIGEST;
	 */

	private short size = 0;
	private byte[] buffer = null;
	
	public short getSize() {
		return size;
	}

	public void setSize(short size) {
		this.size = size;
	}

	public TPM2B_DIGEST() {
	}
	
	public TPM2B_DIGEST(byte[] buffer) {
		this.fromBytes(buffer, 0);
	}
	
	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buf) {
		this.buffer = buf;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(this.size, this.buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
		this.size = brw.readShort();
		this.buffer = new byte[this.size];
		byte[] buf = brw.readBytes(this.size);
        this.setBuffer(buf);
	}

	@Override
	public String toString() {
		return "TPM2B_DIGEST:[(" + this.getSize() + " bytes): " + ByteArrayUtil.toPrintableHexString(this.buffer) + "]";
	}
}
