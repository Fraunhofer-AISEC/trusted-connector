package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;

public class TPML_PCR_SELECTION extends StandardTPMStruct {

	/*
	 * TPML_PCR_SELECTION Structure
	 * typedef struct {
	 *     UINT32             count;
	 *     TPMS_PCR_SELECTION pcrSelections[HASH_COUNT];
	 * } TPML_PCR_SELECTION;
	 */
	
	private int count;
	private TPMS_PCR_SELECTION pcrSelections;

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(count, pcrSelections);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.count = brw.readInt32();
        this.pcrSelections = new TPMS_PCR_SELECTION();
        brw.readStruct(this.pcrSelections);  
	}

	@Override
	public String toString() {
		return "TPML_PCR_SELECTION:[count := "+this.count+", pcrSelections := "+this.pcrSelections.toString()+"]";
	}
	
	public byte[] getBuffer() {
		return this.pcrSelections.getBuffer();
	}

}
