package de.fraunhofer.aisec.tpm2j.tpmu;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;

public abstract class TPMU_PUBLIC_ID extends StandardTPMStruct {
	
	/*
	 * TPMU_PUBLIC_ID Union
	 * typedef union {
	 *     TPM2B_DIGEST         keyedHash;
	 *     TPM2B_DIGEST         sym;
	 *     TPM2B_PUBLIC_KEY_RSA rsa;
	 *     TPMS_ECC_POINT       ecc;
	 * } TPMU_PUBLIC_ID;
	 */
	
	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset) throws Exception;
	
	@Override
    public abstract String toString();
}
