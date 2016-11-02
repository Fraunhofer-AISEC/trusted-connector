package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID.ALG_ID;

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
	public abstract void fromBytes(byte[] source, int offset);
	
	@Override
    public abstract String toString();

	public abstract byte[] getSig();
	
	public abstract ALG_ID getHashAlg();
}
