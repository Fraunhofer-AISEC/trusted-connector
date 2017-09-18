package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_YES_NO;

public class TPMS_CLOCK_INFO extends StandardTPMStruct {
	
	/*
	 * TPMS_CLOCK_INFO Structure
	 * typedef struct {
	 *     UINT64      clock;
	 *     UINT32      resetCount;
	 *     UINT32      restartCount;
	 *     TPMI_YES_NO safe;
	 * } TPMS_CLOCK_INFO;
	 */
	
	private long clock;
	private int resetCount;
	private int restartCount;
	private TPMI_YES_NO safe;

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(clock, resetCount, restartCount, safe);
	}
	
	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.clock = brw.readLong();
        this.resetCount = brw.readInt32();
        this.restartCount = brw.readInt32();
        this.safe = new TPMI_YES_NO();
        brw.readStruct(this.safe);
		
	}
	
    public String toString() {
        return "TPMS_CLOCK_INFO:[clock=" + this.clock 
        		+ ", resetCount=" + this.resetCount 
        		+ ", restartCount=" + this.restartCount 
        		+ ", safe=" + this.safe.toString() + "]";
    }

}
