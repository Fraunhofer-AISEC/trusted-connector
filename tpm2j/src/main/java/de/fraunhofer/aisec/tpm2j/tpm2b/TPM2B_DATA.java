package de.fraunhofer.aisec.tpm2j.tpm2b;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
/**
 * Magic number TPM_GENERATED: Prevents an attacker from signing
 * arbitrary data with a restricted signing key and claiming later that
 * it was a TPM quote. 
 * 
 * @author georgraess
 * @version 1.0.3
 * @since 01.12.2016
 */
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
		return "TPM2B_DATA:[(" + this.getBufferLength() + " byte): " + ByteArrayUtil.toPrintableHexString(this.buffer) + "]";
	}

}
