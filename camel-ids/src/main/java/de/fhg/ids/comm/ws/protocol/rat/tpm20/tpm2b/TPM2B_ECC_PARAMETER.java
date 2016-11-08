package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.TPM2Constants;

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
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
		this.size = brw.readShort();
		if(this.size <= TPM2Constants.MAX_ECC_KEY_BYTES) {
			this.buffer = new byte[this.size];
			this.buffer = brw.readBytes(this.size);
		}
		else {
			LOG.debug("error: buffer size is bigger then MAX_ECC_KEY_BYTES");
		}
	}

	@Override
	public String toString() {
		return "TPM2B_ECC_PARAMETER:[(" + this.size + " bytes): " + ByteArrayUtil.toPrintableHexString(this.buffer) + "]";
	}

}
