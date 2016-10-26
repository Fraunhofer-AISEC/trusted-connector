package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPM2B_DIGEST extends StandardTPMStruct {

	
	/*
	 * TPM2B_DIGEST Structure
	 * typedef struct 
	 *     UINT16 size;
	 *     BYTE   buffer[sizeof(TPMU_HA)];
	 * } TPM2B_DIGEST;
	 */
	
	private byte[] bufferBytes = null;

	public byte[] getBufferBytes() {
		return bufferBytes;
	}

	public void setBufferBytes(byte[] bufferBytes) {
		this.bufferBytes = bufferBytes;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(this.getBufferSize(), this.bufferBytes);
	}

	private Object getBufferSize() {
		if (this.bufferBytes == null) {
            return 0;
        }
        else {
            return this.bufferBytes.length;
        }
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        int parmSize = brw.readInt32();
        this.setBufferBytes(brw.readBytes(parmSize));
	}

}
