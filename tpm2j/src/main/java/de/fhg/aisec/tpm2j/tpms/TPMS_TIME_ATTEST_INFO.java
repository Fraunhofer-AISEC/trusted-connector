package de.fhg.aisec.tpm2j.tpms;

import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmu.TPMU_ATTEST;

public class TPMS_TIME_ATTEST_INFO extends TPMU_ATTEST {

	/*
	 * TPMS_TIME_ATTEST_INFO Structure
	 * typedef struct {
	 *     TPMS_TIME_INFO time;
	 *     UINT64         firmwareVersion;
	 * } TPMS_TIME_ATTEST_INFO;
	 */
	
	TPMS_TIME_INFO time;
	long firmwareVersion;
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(time, firmwareVersion);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.time = new TPMS_TIME_INFO();
        brw.readStruct(this.time);
        this.firmwareVersion = brw.readLong();
    }

	@Override
	public String toString() {
		return "TPMS_TIME_ATTEST_INFO:[time=" + this.time.toString()
			+ ", firmwareVersion=" + this.firmwareVersion + "]";
	}

}
