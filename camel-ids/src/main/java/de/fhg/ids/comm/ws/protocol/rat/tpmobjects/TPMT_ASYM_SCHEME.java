package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMT_ASYM_SCHEME extends StandardTPMStruct {

	/*
	 * TPMT_ASYM_SCHEME Structure
	 * typedef struct {
	 *     TPMI_ALG_ASYM_SCHEME scheme;
	 *     TPMU_ASYM_SCHEME     details;
	 * } TPMT_ASYM_SCHEME;
	 */
	
	//private TPMI_ALG_ASYM_SCHEME scheme;
	private TPMU_ASYM_SCHEME details;

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
        return "TPMT_ASYM_SCHEME:[\n" 
               // + "size := " + this.scheme.toString() + "\n" 
                + "publicArea := " + this.details.toString() + "\n]\n";
	}
	
}
