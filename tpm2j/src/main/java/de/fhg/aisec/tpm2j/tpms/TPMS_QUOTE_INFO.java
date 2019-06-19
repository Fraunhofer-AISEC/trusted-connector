package de.fhg.aisec.tpm2j.tpms;

import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpm2b.TPM2B_DIGEST;
import de.fhg.aisec.tpm2j.tpml.TPML_PCR_SELECTION;
import de.fhg.aisec.tpm2j.tpmu.TPMU_ATTEST;

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
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.pcrSelect = new TPML_PCR_SELECTION();
        brw.readStruct(this.pcrSelect);
        this.pcrDigest = new TPM2B_DIGEST();
        brw.readStruct(this.pcrDigest);        
	}

	@Override
	public String toString() {
		return "TPMS_QUOTE_INFO:[pcrSelect="+ this.pcrDigest.toString() 
			+ ", pcrDigest=" + this.pcrSelect.toString() + "]";
	}
	
	public TPM2B_DIGEST getPcrDigest() {
		return this.pcrDigest;
	}

	public TPML_PCR_SELECTION getPcrSelect() {
		return this.pcrSelect;
	}
}
