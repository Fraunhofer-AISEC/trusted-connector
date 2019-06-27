package de.fhg.aisec.tpm2j.tpm2b;

import de.fhg.aisec.tpm2j.TPM2Constants;
import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;

public class TPM2B_DIGEST extends StandardTPMStruct {
	
	/*
	 * TPM2B_DIGEST Structure
	 * typedef struct {
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
	
	public TPM2B_DIGEST(byte[] buffer) throws Exception {
		this.fromBytes(buffer, 0);
	}
	
	public byte[] getBuffer() {
		return this.buffer;
	}
	
	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(this.size, this.buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.size = brw.readShort();
        if(this.size <= TPM2Constants.SHA512_DIGEST_SIZE) {
            this.buffer = new byte[this.getSize()];
            this.buffer = brw.readBytes(this.getSize());        	
        }
        else {
        	throw new Exception("TPM2B_DIGEST buffer size is bigger than the allowed maximum of "+TPM2Constants.SHA512_DIGEST_SIZE+" byte.");
        }
	}

	@Override
	public String toString() {
		return "TPM2B_DIGEST:[(" + this.getSize() + " byte): " + ByteArrayUtil.toPrintableHexString(this.buffer).replace("\n", "") + "]";
	}
}
