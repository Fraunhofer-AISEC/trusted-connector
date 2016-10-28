package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMS_ECC_PARMS extends TPMU_PUBLIC_PARMS {

	/*
	 * TPMS_ECC_PARMS Structure
	 * typedef struct {
	 *     TPMT_SYM_DEF_OBJECT symmetric;
	 *     TPMT_ECC_SCHEME     scheme;
	 *     TPMI_ECC_CURVE      curveID;
	 *     TPMT_KDF_SCHEME     kdf;
	 * } TPMS_ECC_PARMS;
	 */

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
	
	@Override
	public byte[] toBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
