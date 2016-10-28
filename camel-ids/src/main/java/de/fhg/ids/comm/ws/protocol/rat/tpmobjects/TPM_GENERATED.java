package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPM_GENERATED extends StandardTPMStruct {
	
	/*
	 * TPM_GENERATED Constants
	 * typedef UINT32 TPM_GENERATED;
	 * #define TPM_GENERATED_VALUE (TPM_GENERATED)(0xff544347)
	 */
	
	private int TPM_GENERATED = (short)0xff544347;

	public int getTPM_GENERATED() {
		return this.TPM_GENERATED;
	}

	public void setTPM_GENERATED(int tPM_GENERATED) {
		this.TPM_GENERATED = tPM_GENERATED;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(this.TPM_GENERATED);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.TPM_GENERATED = brw.readInt32();
		
	}

	@Override
	public String toString() {
		return "TPM_GENERATED:[" + this.TPM_GENERATED + "]";
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
}
