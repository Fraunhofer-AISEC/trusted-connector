package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;

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
