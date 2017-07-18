package de.fraunhofer.aisec.tpm2j.tpmi;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;

public abstract class TPMI_RSA_KEY_BITS extends StandardTPMStruct {

	/*
	 * TPMI_RSA_KEY_BITS Type
	 * typedef TPM_KEY_BITS TPMI_RSA_KEY_BITS;
	 */
	
	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset);
	
	@Override
    public abstract String toString();
}
