package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b.TPM2B_DIGEST;

public abstract class TPMU_ATTEST extends StandardTPMStruct {
	
	/*
	 * TPMU_ATTEST Union
	 * typedef union {
	 *     TPMS_CERTIFY_INFO       certify;
	 *     TPMS_CREATION_INFO      creation;
	 *     TPMS_QUOTE_INFO         quote;
	 *     TPMS_COMMAND_AUDIT_INFO commandAudit;
	 *     TPMS_SESSION_AUDIT_INFO sessionAudit;
	 *     TPMS_TIME_ATTEST_INFO   time;
	 *     TPMS_NV_CERTIFY_INFO    nv;
	 * } TPMU_ATTEST;
	 */

	@Override
	public abstract byte[] toBytes();

	@Override
	public abstract void fromBytes(byte[] source, int offset);
	
	@Override
    public abstract String toString();
	
	public abstract TPM2B_DIGEST getDigest();
}
