package de.fraunhofer.aisec.tpm2j.tpmi;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_KEY_BITS;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SYM_KEY_BITS;

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
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.bits = new TPM_KEY_BITS();
        brw.readStruct(this.bits);
	}

	@Override
	public String toString() {
        return "TPMI_SM4_KEY_BITS:[bits=" + this.bits.toString() + "]";
	}

}
