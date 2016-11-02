package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b.TPM2B_DIGEST;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_ATTEST;

public class TPMS_QUOTE_INFO extends TPMU_ATTEST {
	
	/*
	 * TPMS_QUOTE_INFO Structure
	 * typedef struct {
	 *     TPML_PCR_SELECTION pcrSelect;
	 *     TPM2B_DIGEST       pcrDigest;
	 * } TPMS_QUOTE_INFO;
	 */
	
	private TPML_PCR_SELECTION pcrSelect;
	private TPM2B_DIGEST pcrDigest;

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(pcrSelect, pcrDigest);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.pcrSelect = new TPML_PCR_SELECTION();
        brw.readStruct(this.pcrSelect);
        this.pcrDigest = new TPM2B_DIGEST();
        brw.readStruct(this.pcrDigest);        
	}

	@Override
	public String toString() {
		return "TPMS_QUOTE_INFO:[pcrSelect := "+ this.pcrDigest.toString() 
			+ ", pcrDigest := " + this.pcrSelect.toString() + "]";
	}
	
	@Override
	public TPM2B_DIGEST getDigest() {
		return this.pcrDigest;
	}
}
