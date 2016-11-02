package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.TPM2Constants;

public class TPM2B_DIGEST extends StandardTPMStruct {
	
	/*
	 * TPM2B_DIGEST Structure
	 * typedef struct {
	 *     UINT16 size;
	 *     BYTE   buffer[sizeof(TPMU_HA)];
	 * } TPM2B_DIGEST;
	 */

	private short size = 0;
	private byte[] buffer = new byte[TPM2Constants.SHA512_DIGEST_SIZE];
	
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
		return this.buffer;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(this.size, this.buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.size = brw.readShort();
        if(this.size <= TPM2Constants.SHA512_DIGEST_SIZE) {
            this.buffer = new byte[this.getSize()];
            this.buffer = brw.readBytes(this.getSize());        	
        }
        else {
        	LOG.debug("error: buffer of TPM2B_DIGEST is larger then SHA512_DIGEST_SIZE");
        }
	}

	@Override
	public String toString() {
		return "TPM2B_DIGEST:[(" + this.getSize() + " bytes): " + ByteArrayUtil.toPrintableHexString(this.buffer).replace("\n", "") + "]";
	}
}
