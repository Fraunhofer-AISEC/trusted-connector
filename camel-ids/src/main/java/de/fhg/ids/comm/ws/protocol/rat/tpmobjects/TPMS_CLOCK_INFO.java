package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

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
	private byte[] safe = new byte[0];	// TPMI_YES_NO is just a single byte
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(clock, resetCount, restartCount, safe);
	}
	
	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.clock = brw.readLong();
        this.resetCount = brw.readInt32();
        this.restartCount = brw.readInt32();
        this.safe = brw.readBytes(1);
		
	}
	
    public String toString() {
        return "TPMS_CLOCK_INFO: \n" 
        		+ "clock = " + this.clock + "\n"
        		+ "resetCount = " + this.resetCount + "\n"
        		+ "restartCount = " + this.restartCount + "\n"
        		+ "safe = " + this.safe + "\n";
    }

}
