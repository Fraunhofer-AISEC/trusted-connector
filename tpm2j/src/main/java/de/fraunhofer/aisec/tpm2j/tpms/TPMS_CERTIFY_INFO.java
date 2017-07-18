package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_NAME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ATTEST;

public class TPMS_CERTIFY_INFO extends TPMU_ATTEST {
	
	/*
	 * TPMS_CERTIFY_INFO Structure
	 * typedef struct {
	 *     TPM2B_NAME name;
	 *     TPM2B_NAME qualifiedName;
	 * } TPMS_CERTIFY_INFO;
	 */
	
	TPM2B_NAME name;
	TPM2B_NAME qualifiedName;

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(name, qualifiedName);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.name = new TPM2B_NAME();
        brw.readStruct(this.name);
        this.qualifiedName = new TPM2B_NAME();
        brw.readStruct(this.qualifiedName);        
	}

	@Override
	public String toString() {
		return "TPMS_CERTIFY_INFO:[name="+this.name.toString()
			+ ", qualifiedName="+this.qualifiedName.toString()+"]";
	}
}
