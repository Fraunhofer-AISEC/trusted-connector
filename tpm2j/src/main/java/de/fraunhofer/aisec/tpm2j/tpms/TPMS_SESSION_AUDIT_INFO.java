package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_DIGEST;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_NAME;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_YES_NO;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ATTEST;

public class TPMS_SESSION_AUDIT_INFO extends TPMU_ATTEST {
	/*
	 * TPMS_SESSION_AUDIT_INFO Structure
	 * typedef struct {
	 *     TPMI_YES_NO  exclusiveSession;
	 *     TPM2B_DIGEST sessionDigest;
	 * } TPMS_SESSION_AUDIT_INFO;
	 */
	
	TPMI_YES_NO exclusiveSession;
	TPM2B_DIGEST sessionDigest;

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(exclusiveSession, sessionDigest);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.exclusiveSession = new TPMI_YES_NO();
        brw.readStruct(this.exclusiveSession);
        this.sessionDigest = new TPM2B_DIGEST();
        brw.readStruct(this.sessionDigest);        
	}

	@Override
	public String toString() {
		return "TPMS_SESSION_AUDIT_INFO:[exclusiveSession=" + this.exclusiveSession.toString()
			+ ", sessionDigest=" + this.sessionDigest.toString() + "]";
	}

}
