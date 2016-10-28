package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

abstract class TPMI_RSA_KEY_BITS extends StandardTPMStruct {

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
