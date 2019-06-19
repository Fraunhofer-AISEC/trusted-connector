package de.fhg.aisec.tpm2j.tpms;

import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpm.TPM_ALG_ID;
import de.fhg.aisec.tpm2j.tpm2b.TPM2B_DIGEST;
import de.fhg.aisec.tpm2j.tpmu.TPMU_ATTEST;

public class TPMS_COMMAND_AUDIT_INFO extends TPMU_ATTEST {
	
	/*
	 * TPMS_COMMAND_AUDIT_INFO Structure
	 * typedef struct {
	 *     UINT64       auditCounter;
	 *     TPM_ALG_ID   digestAlg;
	 *     TPM2B_DIGEST auditDigest;
	 *     TPM2B_DIGEST commandDigest;
	 * } TPMS_COMMAND_AUDIT_INFO;
	 */
	
	long auditCounter;
	TPM_ALG_ID digestAlg;
	TPM2B_DIGEST auditDigest;
	TPM2B_DIGEST commandDigest;
	
	public long getAuditCounter() {
		return auditCounter;
	}

	public void setAuditCounter(long auditCounter) {
		this.auditCounter = auditCounter;
	}

	public TPM_ALG_ID getDigestAlg() {
		return digestAlg;
	}

	public void setDigestAlg(TPM_ALG_ID digestAlg) {
		this.digestAlg = digestAlg;
	}

	public TPM2B_DIGEST getAuditDigest() {
		return auditDigest;
	}

	public void setAuditDigest(TPM2B_DIGEST auditDigest) {
		this.auditDigest = auditDigest;
	}

	public TPM2B_DIGEST getCommandDigest() {
		return commandDigest;
	}

	public void setCommandDigest(TPM2B_DIGEST commandDigest) {
		this.commandDigest = commandDigest;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(auditCounter, digestAlg, auditDigest, commandDigest);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.auditCounter = brw.readLong();
        this.digestAlg = new TPM_ALG_ID();
        brw.readStruct(this.digestAlg);
        this.auditDigest = new TPM2B_DIGEST();
        brw.readStruct(this.auditDigest);
        this.commandDigest = new TPM2B_DIGEST();
        brw.readStruct(this.commandDigest);
	}

	@Override
	public String toString() {
		return "TPMS_COMMAND_AUDIT_INFO:[auditCounter="+this.auditCounter
				+ ", digestAlg=" + this.digestAlg.toString()
				+ ", auditDigest=" + this.auditDigest.toString()
				+ ", commandDigest=" + this.commandDigest.toString() + "]";
	}

}
