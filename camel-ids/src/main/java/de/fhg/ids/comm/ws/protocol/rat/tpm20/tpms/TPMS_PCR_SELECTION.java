package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import java.math.BigInteger;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.TPM2Constants;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID.ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_HASH;

public class TPMS_PCR_SELECTION extends StandardTPMStruct {
	
	/*
	 * TPMS_PCR_SELECTION Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH hash;
	 *     UINT8         sizeofSelect;
	 *     BYTE          pcrSelect[PCR_SELECT_MAX];
	 * } TPMS_PCR_SELECTION;
	 */
	
	private TPMI_ALG_HASH hash;
	private byte sizeofSelect;
	private byte[] pcrSelect = new byte[0];

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hash, sizeofSelect, pcrSelect);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.hash = new TPMI_ALG_HASH();
        brw.readStruct(this.hash);
        this.sizeofSelect = brw.readByte();
        if(this.sizeofSelect <= TPM2Constants.PCR_SELECT_MAX) {
        	this.pcrSelect = new byte[(int) this.sizeofSelect];
        	this.pcrSelect = brw.readBytes(this.sizeofSelect);
        }
        else {
        	LOG.debug("error: sizeofSelect >= pcrSelect[PCR_SELECT_MAX]");
        }
	}

	@Override
	public String toString() {
		return "TPMS_PCR_SELECTION:[hash := "+this.hash.toString()
				+ ", sizeofSelect := " + this.sizeofSelect
				+ ", pcrSelect := "+ByteArrayUtil.toPrintableHexString(pcrSelect)+"]";
	}
	
	public byte[] getBuffer() {
		return this.pcrSelect;
	}
	
	public ALG_ID getHashId() {
		return this.hash.getHashId().getAlgId();
	}
	
	// check if a single bit at position bit is set inside arr
	public boolean isSet(byte arr, int bit) {
	    return BigInteger.valueOf(arr).testBit(bit);
	}
	
	// #sizeofSelect times octet (8)
	public int getNumPcrs() {
		return this.sizeofSelect * 8;
	}
	
	// #sizeofSelect times octet (8)
	public double getOctet(int numRegister) {
		return Math.floor(numRegister / 8);
	}
	
	// check if pcr @numRegister is set or not
	public boolean isPcrSelected(int numRegister) {
		byte mask = this.pcrSelect[(int) this.getOctet(numRegister)];
		return this.isSet(mask, numRegister % 8);
	}

}
