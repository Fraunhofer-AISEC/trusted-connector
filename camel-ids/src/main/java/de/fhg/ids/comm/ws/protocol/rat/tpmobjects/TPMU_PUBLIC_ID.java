package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMU_PUBLIC_ID extends StandardTPMStruct {
	
	/*
	 * TPMU_PUBLIC_ID Union
	 * typedef union {
	 *     TPM2B_DIGEST         keyedHash;
	 *     TPM2B_DIGEST         sym;
	 *     TPM2B_PUBLIC_KEY_RSA rsa;
	 *     TPMS_ECC_POINT       ecc;
	 * } TPMU_PUBLIC_ID;
	 */
	
	// TODO : implement this

	@Override
	public byte[] toBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		// TODO Auto-generated method stub
		
	}

}
