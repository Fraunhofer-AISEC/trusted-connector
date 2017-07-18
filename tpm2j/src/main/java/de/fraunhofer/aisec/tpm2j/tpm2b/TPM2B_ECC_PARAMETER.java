package de.fraunhofer.aisec.tpm2j.tpm2b;

import de.fraunhofer.aisec.tpm2j.TPM2Constants;
import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;

public class TPM2B_ECC_PARAMETER extends StandardTPMStruct {

	/*
	 * TPM2B_ECC_PARAMETER Structure
	 * typedef struct {
	 *     UINT16 size;
	 *     BYTE   buffer[MAX_ECC_KEY_BYTES];
	 * } TPM2B_ECC_PARAMETER;
	 */
	
	private short size = 0;
	private byte[] buffer = new byte[0];
	
	public short getSize() {
		return size;
	}

	public void setSize(short size) {
		this.size = size;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(size, buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
		this.size = brw.readShort();
		if(this.size <= TPM2Constants.MAX_ECC_KEY_BYTES) {
			this.buffer = new byte[this.size];
			this.buffer = brw.readBytes(this.size);
		}
		else {
			throw new Exception("TPM2B_ECC_PARAMETER buffer size is bigger than the allowed maximum of "+TPM2Constants.MAX_ECC_KEY_BYTES+" byte.");
		}
	}

	@Override
	public String toString() {
		return "TPM2B_ECC_PARAMETER:[(" + this.size + " byte): " + ByteArrayUtil.toPrintableHexString(this.buffer) + "]";
	}

}
