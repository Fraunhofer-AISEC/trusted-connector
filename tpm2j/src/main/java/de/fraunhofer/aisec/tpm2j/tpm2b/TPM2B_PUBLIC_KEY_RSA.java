package de.fraunhofer.aisec.tpm2j.tpm2b;

import de.fraunhofer.aisec.tpm2j.TPM2Constants;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_PUBLIC_ID;

public class TPM2B_PUBLIC_KEY_RSA extends TPMU_PUBLIC_ID {
	
	/*
	 * TPM2B_PUBLIC_KEY_RSA Structure
	 * typedef struct {
	 *     UINT16 size;
	 *     BYTE   buffer[MAX_RSA_KEY_BYTES];
	 * } TPM2B_PUBLIC_KEY_RSA;
	 */
	
	private byte[] buffer = new byte[0];
	private short size = 0;

	
	public short getSize() {
		return size;
	}

	public void setSize(short size) {
		this.size = size;
	}

	public byte[] getBuffer() {
		return buffer;
	}
	
	public byte[] getKey() {
		return this.getBuffer();
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}
	
	public int getBufferLength() {
		return buffer.length;
	}	

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(size, buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.size = brw.readShort();
        if(this.size <= TPM2Constants.MAX_RSA_KEY_BYTES) {
            this.buffer = new byte[this.getSize()];
            this.buffer = brw.readBytes(this.getSize());        	
        }
        else {
        	throw new Exception("TPM2B_PUBLIC_KEY_RSA buffer size is bigger than the allowed maximum of "+TPM2Constants.MAX_RSA_KEY_BYTES+" byte.");
        }
	}

	@Override
    public String toString() {
		String key = ByteArrayUtil.toPrintableHexString(this.buffer).replace("\n", "");
		return "TPM2B_PUBLIC_KEY_RSA:[(" + this.getSize() + " byte): "+ key + "]";
    }
}
