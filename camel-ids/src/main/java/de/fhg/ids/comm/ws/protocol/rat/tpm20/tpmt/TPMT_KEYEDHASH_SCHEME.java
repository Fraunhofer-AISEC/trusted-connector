package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmt;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_KEYEDHASH_SCHEME;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_SCHEME_KEYEDHASH;

public class TPMT_KEYEDHASH_SCHEME extends StandardTPMStruct {

	/*
	 * TPMT_KEYEDHASH_SCHEME Structure
	 * typedef struct {
	 *     TPMI_ALG_KEYEDHASH_SCHEME scheme;
	 *     TPMU_SCHEME_KEYEDHASH     details;
	 * } TPMT_KEYEDHASH_SCHEME;
	 */
	
	private TPMI_ALG_KEYEDHASH_SCHEME scheme;
	private TPMU_SCHEME_KEYEDHASH details;

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(scheme, details);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.scheme = new TPMI_ALG_KEYEDHASH_SCHEME();
        brw.readStruct(this.scheme);
        switch(this.scheme.getAlgId().getAlgId()) {
        	// TODO 
        }
        //this.details = new TPMU_SCHEME_KEYEDHASH();
        //brw.readStruct(this.details);		
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}
}
