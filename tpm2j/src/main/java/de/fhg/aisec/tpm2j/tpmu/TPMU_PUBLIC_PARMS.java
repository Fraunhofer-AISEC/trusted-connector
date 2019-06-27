package de.fhg.aisec.tpm2j.tpmu;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;

public abstract class TPMU_PUBLIC_PARMS extends StandardTPMStruct {

	/*
	 * TPMU_PUBLIC_PARMS Union
	 * typedef union {
	 *     TPMS_KEYEDHASH_PARMS keyedHashDetail;
	 *     TPMT_SYM_DEF_OBJECT  symDetail;
	 *     TPMS_RSA_PARMS       rsaDetail;
	 *     TPMS_ECC_PARMS       eccDetail;
	 *     TPMS_ASYM_PARMS      asymDetail;
	 * } TPMU_PUBLIC_PARMS;
	 */

	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset) throws Exception;
	
	@Override
    public abstract String toString();
}
