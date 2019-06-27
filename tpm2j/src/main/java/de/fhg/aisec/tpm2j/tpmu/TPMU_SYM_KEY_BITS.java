package de.fhg.aisec.tpm2j.tpmu;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;

public abstract class TPMU_SYM_KEY_BITS extends StandardTPMStruct {
	/*
	 * TPMU_SYM_KEY_BITS Union
	 * typedef union {
	 *     TPMI_AES_KEY_BITS aes;
	 *     TPMI_SM4_KEY_BITS SM4;
	 *     TPM_KEY_BITS      sym;
	 *     TPMI_ALG_HASH     xor;
	 * } TPMU_SYM_KEY_BITS;
	 */
	
	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset) throws Exception;
	
	@Override
    public abstract String toString();
}
