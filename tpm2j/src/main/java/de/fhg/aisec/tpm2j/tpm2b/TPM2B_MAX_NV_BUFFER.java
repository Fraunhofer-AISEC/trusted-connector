package de.fhg.aisec.tpm2j.tpm2b;

import de.fhg.aisec.tpm2j.TPM2Constants;
import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;

public class TPM2B_MAX_NV_BUFFER extends StandardTPMStruct {

	/*
	 * TPM2B_MAX_NV_BUFFER Structure
	 * typedef struct {
	 *     UINT16 size;
	 *     BYTE   buffer[MAX_NV_INDEX_SIZE];
	 * } TPM2B_MAX_NV_BUFFER;
	 */
	
	private short size;
	private byte[] buffer = new byte[0];
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(size, buffer);
	}
	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.size = brw.readShort();
        if(this.size <= TPM2Constants.MAX_NV_INDEX_SIZE) {
        	this.buffer = new byte[this.size];
        	this.buffer = brw.readBytes(this.size);
        }
        else {
        	throw new Exception("TPM2B_MAX_NV_BUFFER is bigger than allowed maximum of "+TPM2Constants.MAX_NV_INDEX_SIZE+" bytes.");
        }
        
	}
	@Override
	public String toString() {
		return "TPM2B_MAX_NV_BUFFER:[("+this.size+" byte): " + ByteArrayUtil.toPrintableHexString(buffer).replaceAll("\n", "") + "]";
	}
}
