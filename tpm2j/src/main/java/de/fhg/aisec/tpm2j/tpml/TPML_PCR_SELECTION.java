package de.fhg.aisec.tpm2j.tpml;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpms.TPMS_PCR_SELECTION;

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
	public void fromBytes(byte[] source, int offset) throws Exception {
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
		if(this.count > 0) {
			StringBuilder selections = new StringBuilder();
			for(int i = 0; i < this.count; i++) {
				selections.append("[").append(i).append(":").append(this.pcrSelections[i].toString()).append("], ");
	        }
			return "TPML_PCR_SELECTION:[count="+this.count+", pcrSelections="
					+ selections.substring(0, selections.length() - 2) + "]";
		}
		else {
			return "TPML_PCR_SELECTION:[count=0, pcrSelections=]";
		}
	}
	
	public byte[] getBuffer(int i) {
		return this.pcrSelections[i].getBuffer();
	}

}
