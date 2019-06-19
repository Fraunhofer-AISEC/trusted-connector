package de.fhg.aisec.tpm2j.tpm;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;

import java.nio.ByteBuffer;

/**
 * Magic number TPM_GENERATED: Prevents an attacker from signing
 * arbitrary data with a restricted signing key and claiming later that
 * it was a TPM quote. 
 * 
 * @author georgraess
 * @version 1.0.3
 * @since 01.12.2016
 */
public class TPM_GENERATED extends StandardTPMStruct {
	
	/*
	 * TPM_GENERATED Constants
	 * typedef UINT32 TPM_GENERATED;
	 * #define TPM_GENERATED_VALUE (TPM_GENERATED)(0xff544347)
	 */
	
	private int TPM_GENERATED = (int)0xff544347;

	public int getTPM_GENERATED() {
		return this.TPM_GENERATED;
	}

	public void setTPM_GENERATED(int generated) {
		this.TPM_GENERATED = generated;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(this.TPM_GENERATED);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.TPM_GENERATED = brw.readInt32();
		
	}

	@Override
	public String toString() {
		return "(TPM_GENERATED=0x" + ByteArrayUtil.toPrintableHexString(ByteBuffer.allocate(4).putInt(TPM_GENERATED).array()).replaceAll(" ", "").toLowerCase() + ")";
	}
}