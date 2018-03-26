package de.fraunhofer.aisec.tpm2j.tpms;

import java.math.BigInteger;

import de.fraunhofer.aisec.tpm2j.TPM2Constants;
import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID.ALG_ID;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_HASH;

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
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.hash = new TPMI_ALG_HASH();
        brw.readStruct(this.hash);
        this.sizeofSelect = brw.readByte();
        if(this.sizeofSelect <= TPM2Constants.PCR_SELECT_MAX) {
        	this.pcrSelect = new byte[(int) this.sizeofSelect];
        	this.pcrSelect = brw.readBytes(this.sizeofSelect);
        }
        else {
        	throw new Exception("TPMS_PCR_SELECTION sizeofSelect is bigger than the allowed PCR_SELECT_MAX.");
        }
	}

	@Override
	public String toString() {
		return "TPMS_PCR_SELECTION:[hash="+this.hash.toString()
				+ ", sizeofSelect=" + this.sizeofSelect
				+ ", pcrSelect="+ByteArrayUtil.toPrintableHexString(pcrSelect)+"]";
	}
	
	public byte[] getBuffer() {
		return this.pcrSelect;
	}
	
	public ALG_ID getHashId() {
		return this.hash.getHashId().getAlgId();
	}
	
	public int getSize() {
		return (int) this.sizeofSelect;
	}
	
	public int getNumOfRegisters() {
		return (int) this.sizeofSelect * 8;
	}
	
	public int getNumOfSetRegisters() {
		int ret = 0;
		for(int i = 0; i < this.getNumOfRegisters(); i++) {
			if(this.isPcrSelected(i)) {
				ret++;
			}
		}
		return ret;
	}
	
	// check if a single bit at position bit is set inside arr
	public static boolean isSet(byte arr, int bit) {
	    return BigInteger.valueOf(arr).testBit(bit);
	}
	
	// #sizeofSelect times octet (8)
	public static int getOctet(int numRegister) {
		return numRegister / 8;
	}
	
	// check if pcr @numRegister is set or not
	public boolean isPcrSelected(int numRegister) {
		byte mask = this.pcrSelect[TPMS_PCR_SELECTION.getOctet(numRegister)];
		return TPMS_PCR_SELECTION.isSet(mask, numRegister % 8);
	}

}
