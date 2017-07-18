package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_GENERATED;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_MAX_NV_BUFFER;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_NAME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ATTEST;

public class TPMS_NV_CERTIFY_INFO extends TPMU_ATTEST {
	
	/*
	 * TPMS_NV_CERTIFY_INFO Structure
	 * typedef struct {
	 *     TPM2B_NAME          indexName;
	 *     UINT16              offset;
	 *     TPM2B_MAX_NV_BUFFER nvContents;
	 * } TPMS_NV_CERTIFY_INFO;
	 */
	
	private TPM2B_NAME indexName;
	private short offset;
	private TPM2B_MAX_NV_BUFFER nvContents;

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(indexName, offset, nvContents);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.indexName = new TPM2B_NAME();
        brw.readStruct(this.indexName);
        this.offset = brw.readShort();
        this.nvContents = new TPM2B_MAX_NV_BUFFER();
        brw.readStruct(this.nvContents);
	}

	@Override
	public String toString() {
		return "TPMS_NV_CERTIFY_INFO:[indexName=" + this.indexName.toString()
			+ ", offset=" + this.offset 
			+ ", nvContents=" + this.nvContents.toString() + "]";
	}

}
