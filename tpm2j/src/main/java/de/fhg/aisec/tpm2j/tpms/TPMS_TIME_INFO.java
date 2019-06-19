package de.fhg.aisec.tpm2j.tpms;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;

public class TPMS_TIME_INFO extends StandardTPMStruct {

	/*
	 * TPMS_TIME_INFO Structure
	 * typedef struct {
	 *     UINT64          time;
	 *     TPMS_CLOCK_INFO clockInfo;
	 * } TPMS_TIME_INFO;
	 */
	
	long time;
	TPMS_CLOCK_INFO clockInfo;
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(time, clockInfo);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.time = brw.readLong();
        this.clockInfo = new TPMS_CLOCK_INFO();
        brw.readStruct(this.clockInfo);
	}

	@Override
	public String toString() {
		return "TPMS_TIME_INFO:[time="+this.time+", clockInfo="+this.clockInfo.toString()+"]";
	}


}
