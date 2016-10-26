package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMU_PUBLIC_PARMS extends StandardTPMStruct {

	
	/*
	 * TPMU_PUBLIC_PARMS Union
	 * typedef union {
	 *     TPMS_KEYEDHASH_PARMS keyedHashDetail;
	 *     TPMT_SYM_DEF_OBJECT  symDetail;
	 *     TPMS_RSA_PARMS       rsaDetail;
	 *     TPMS_ECC_PARMS       eccDetail;
	 *     TPMS_ASYM_PARMS      asymDetail;
	 * } TPMU_PUBLIC_PARMS;
	 */
	
	// TODO: implement this

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
