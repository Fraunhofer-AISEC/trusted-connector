package de.fraunhofer.aisec.tpm2j.tpmu;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;

public abstract class TPMU_SCHEME_KEYEDHASH extends StandardTPMStruct {
	
	/*
	 * TPMU_SCHEME_KEYEDHASH Union
	 * typedef union {
	 *     TPMS_SCHEME_HMAC hmac;
	 *     TPMS_SCHEME_XOR  xor;
	 * } TPMU_SCHEME_KEYEDHASH;
	 */
	
	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset);
	
	@Override
    public abstract String toString();
	
	public byte[] getBuffer() {
		return this.toBytes();
	}
}
