package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPM_KEY_BITS extends TPMI_RSA_KEY_BITS {
	
	private short bits = 0;

	public short getBits() {
		return bits;
	}

	public void setBits(short bits) {
		this.bits = bits;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(bits);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.bits = brw.readShort();
	}

	@Override
	public String toString() {
        return "TPM_KEY_BITS:[\n" 
        		+ "bits = " + this.bits + "\n]\n";
    }
}
