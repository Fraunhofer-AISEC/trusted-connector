package de.fraunhofer.aisec.tpm2j.tpmt;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_KEYEDHASH_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_SCHEME_KEYEDHASH;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
	public void fromBytes(byte[] source, int offset) throws Exception {
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
		throw new NotImplementedException();
	}
}
