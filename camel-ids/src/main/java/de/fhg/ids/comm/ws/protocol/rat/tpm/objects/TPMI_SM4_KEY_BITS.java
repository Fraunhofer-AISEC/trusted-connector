package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

import de.fhg.ids.comm.ws.protocol.rat.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.ByteArrayUtil;

public class TPMI_SM4_KEY_BITS extends TPMU_SYM_KEY_BITS {
	
	private TPM_KEY_BITS bits;

	public TPM_KEY_BITS getBits() {
		return bits;
	}

	public void setBits(TPM_KEY_BITS bits) {
		this.bits = bits;
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(bits);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.bits = new TPM_KEY_BITS();
        brw.readStruct(this.bits);
	}

	@Override
	public String toString() {
        return "TPMI_SM4_KEY_BITS:[" + this.bits.toString() + "]";
	}

}
