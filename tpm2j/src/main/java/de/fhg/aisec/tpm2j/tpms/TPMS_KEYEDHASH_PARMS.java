package de.fhg.aisec.tpm2j.tpms;

import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmt.TPMT_KEYEDHASH_SCHEME;
import de.fhg.aisec.tpm2j.tpmu.TPMU_PUBLIC_PARMS;

public class TPMS_KEYEDHASH_PARMS extends TPMU_PUBLIC_PARMS {
	
	/*
	 * TPMS_KEYEDHASH_PARMS Structure
	 * typedef struct {
	 *     TPMT_KEYEDHASH_SCHEME scheme;
	 * } TPMS_KEYEDHASH_PARMS;
	 */

	private TPMT_KEYEDHASH_SCHEME scheme;
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(scheme);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.scheme = new TPMT_KEYEDHASH_SCHEME();
        brw.readStruct(this.scheme);
	}

	@Override
	public String toString() {
		return "TPMS_KEYEDHASH_PARMS:[scheme=" + this.scheme.toString() + "]";
	}

}
