package de.fhg.aisec.tpm2j.tpmu;

import de.fhg.aisec.tpm2j.tools.ByteArrayable;

public interface TPMU_ASYM_SCHEME extends ByteArrayable {

	/*
	 * TPMU_ASYM_SCHEME Union
	 * typedef union {
	 *     TPMS_SCHEME_RSASSA    rsassa;
	 *     TPMS_SCHEME_RSAPSS    rsapss;
	 *     TPMS_SCHEME_OAEP      oaep;
	 *     TPMS_SCHEME_ECDSA     ecdsa;
	 *     TPMS_SCHEME_SM2       sm2;
	 *     TPMS_SCHEME_ECDAA     ecdaa;
	 *     TPMS_SCHEME_ECSCHNORR ecSchnorr;
	 *     TPMS_SCHEME_SIGHASH   anySig;
	 * } TPMU_ASYM_SCHEME;
	 */
	
	public abstract byte[] toBytes();

	public abstract void fromBytes(byte[] source, int offset) throws Exception;
	
    public abstract String toString();

}
