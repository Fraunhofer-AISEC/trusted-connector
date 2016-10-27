package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public abstract class TPMU_ASYM_SCHEME extends StandardTPMStruct {

	/*
	 * TPMU_ASYM_SCHEME Union
	 * typedef union {
	 *     TPMS_SCHEME_RSASSA    rsassa;
	 *     TPMS_SCHEME_RSAPSS    rsapss;
	 *     TPMS_SCHEME_OAEP      oaep;
	 *     TPMS_SCHEME_ECDSA     ecdsa;
	 *     TPMS_SCHEME_ECDAA     ecdaa;
	 *     TPMS_SCHEME_ECSCHNORR ecSchnorr;
	 *     TPMS_SCHEME_SIGHASH   anySig;
	 * } TPMU_ASYM_SCHEME;
	 */
	
	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset);
	
	@Override
    public abstract String toString();	
}
