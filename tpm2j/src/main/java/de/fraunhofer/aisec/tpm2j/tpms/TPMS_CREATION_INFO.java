package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_GENERATED;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_DIGEST;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_NAME;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ATTEST;

public class TPMS_CREATION_INFO extends TPMU_ATTEST {
	/*
	 * TPMS_CREATION_INFO Structure
	 * typedef struct {
	 *     TPM2B_NAME   objectName;
	 *     TPM2B_DIGEST creationHash;
	 * } TPMS_CREATION_INFO;
	 */
	
	TPM2B_NAME objectName;
	TPM2B_DIGEST creationHash;

	public TPM2B_NAME getObjectName() {
		return objectName;
	}

	public void setObjectName(TPM2B_NAME objectName) {
		this.objectName = objectName;
	}

	public TPM2B_DIGEST getCreationHash() {
		return creationHash;
	}

	public void setCreationHash(TPM2B_DIGEST creationHash) {
		this.creationHash = creationHash;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(objectName, creationHash);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.objectName = new TPM2B_NAME();
        brw.readStruct(this.objectName);
        this.creationHash = new TPM2B_DIGEST();
        brw.readStruct(this.creationHash);        
	}

	@Override
	public String toString() {
		return "TPMS_CREATION_INFO:[objectName=" + this.objectName.toString()
			+ ", creationHash=" + this.creationHash.toString() + "]";
	}

}
