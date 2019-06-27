package de.fhg.aisec.tpm2j.tpmu;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;

public abstract class TPMU_NAME extends StandardTPMStruct {

	/*
	 * TPMU_SIGNATURE Union
	 * typedef union {
	 *     TPMT_HA    digest;
	 *     TPM_HANDLE handle;
	 * } TPMU_NAME;
	 */
	
	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset) throws Exception;
	
	@Override
    public abstract String toString();
}
