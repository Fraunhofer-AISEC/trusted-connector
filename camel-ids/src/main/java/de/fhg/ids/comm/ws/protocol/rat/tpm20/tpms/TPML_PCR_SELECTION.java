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
	private TPMS_PCR_SELECTION[] pcrSelections = new TPMS_PCR_SELECTION[0];
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public TPMS_PCR_SELECTION getPcrSelections(int i) {
		return pcrSelections[i];
	}

	public void setPcrSelections(TPMS_PCR_SELECTION pcrSelections, int i) {
		this.pcrSelections[i] = pcrSelections;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(count, pcrSelections);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.count = brw.readInt32();
        this.pcrSelections = new TPMS_PCR_SELECTION[this.count];
        for(int i = 0; i < this.count; i++) {
        	this.pcrSelections[i] = new TPMS_PCR_SELECTION();
            brw.readStruct(this.pcrSelections[i]);
        }
	}

	@Override
	public String toString() {
		String selections = "";
		for(int i = 0; i < this.count; i++) {
			selections += i + ":" + this.pcrSelections.toString() + ", ";
        }
		return "TPML_PCR_SELECTION:[count := "+this.count+", pcrSelections := "+selections.substring(-2)+"]";
	}
	
	public byte[] getBuffer(int i) {
		return this.pcrSelections[i].getBuffer();
	}

}
