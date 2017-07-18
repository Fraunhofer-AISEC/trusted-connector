package de.fraunhofer.aisec.tpm2j.tpm;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_RSA_KEY_BITS;

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
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.bits = brw.readShort();
	}

	@Override
	public String toString() {
        return "TPM_KEY_BITS:(" + this.bits + " bit)";
    }
}
