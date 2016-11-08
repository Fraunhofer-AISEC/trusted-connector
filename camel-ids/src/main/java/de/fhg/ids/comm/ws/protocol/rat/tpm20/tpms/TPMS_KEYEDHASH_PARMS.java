package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmt.TPMT_KEYEDHASH_SCHEME;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_PUBLIC_PARMS;

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
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.scheme = new TPMT_KEYEDHASH_SCHEME();
        brw.readStruct(this.scheme);
	}

	@Override
	public String toString() {
		return "TPMS_KEYEDHASH_PARMS:[scheme = " + this.scheme.toString() + "]";
	}

}
