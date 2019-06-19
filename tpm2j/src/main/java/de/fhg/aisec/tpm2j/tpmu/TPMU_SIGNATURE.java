package de.fhg.aisec.tpm2j.tpmu;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tpm.TPM_ALG_ID.ALG_ID;

public abstract class TPMU_SIGNATURE extends StandardTPMStruct {

	/*
	 * TPMU_SIGNATURE Union
	 * typedef union {
	 *     TPMS_SIGNATURE_RSASSA rsassa;
	 *     TPMS_SIGNATURE_RSAPSS rsapss;
	 *     TPMS_SIGNATURE_ECDSA  ecdsa;
	 *     TPMS_SIGNATURE_ECDSA  sm2;
	 *     TPMS_SIGNATURE_ECDSA  ecdaa;
	 *     TPMS_SIGNATURE_ECDSA  ecschnorr;
	 *     TPMT_HA               hmac;
	 *     TPMS_SCHEME_SIGHASH   any;
	 * } TPMU_SIGNATURE;
	 */
	
	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset) throws Exception;
	
	@Override
    public abstract String toString();

	public abstract byte[] getSig();
	
	public abstract ALG_ID getHashAlg();
}
