package de.fraunhofer.aisec.tpm2j.tpmu;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;

public abstract class TPMU_KDF_SCHEME extends StandardTPMStruct {

	/*
	 * TPMU_KDF_SCHEME Union
	 * typedef union {
	 *     TPMS_SCHEME_MGF1           mgf1;
	 *     TPMS_SCHEME_KDF1_SP800_56a kdf1_SP800_56a;
	 *     TPMS_SCHEME_KDF2           kdf2;
	 *     TPMS_SCHEME_KDF1_SP800_108 kdf1_sp800_108;
	 * } TPMU_KDF_SCHEME;
	 */
	
	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset) throws Exception;
	
	@Override
    public abstract String toString();	
}
