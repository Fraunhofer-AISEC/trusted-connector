package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

public interface TPM_ST {
	
	/*
	 * TPM_ST Constants
	 * typedef UINT16 TPM_ST;
	 * #define TPM_ST_RSP_COMMAND          (TPM_ST)(0x00C4)
	 * #define TPM_ST_NULL                 (TPM_ST)(0X8000)
	 * #define TPM_ST_NO_SESSIONS          (TPM_ST)(0x8001)
	 * #define TPM_ST_SESSIONS             (TPM_ST)(0x8002)
	 * #define TPM_ST_ATTEST_NV            (TPM_ST)(0x8014)
	 * #define TPM_ST_ATTEST_COMMAND_AUDIT (TPM_ST)(0x8015)
	 * #define TPM_ST_ATTEST_SESSION_AUDIT (TPM_ST)(0x8016)
	 * #define TPM_ST_ATTEST_CERTIFY       (TPM_ST)(0x8017)
	 * #define TPM_ST_ATTEST_QUOTE         (TPM_ST)(0x8018)
	 * #define TPM_ST_ATTEST_TIME          (TPM_ST)(0x8019)
	 * #define TPM_ST_ATTEST_CREATION      (TPM_ST)(0x801A)
	 * #define TPM_ST_CREATION             (TPM_ST)(0x8021)
	 * #define TPM_ST_VERIFIED             (TPM_ST)(0x8022)
	 * #define TPM_ST_AUTH_SECRET          (TPM_ST)(0x8023)
	 * #define TPM_ST_HASHCHECK            (TPM_ST)(0x8024)
	 * #define TPM_ST_AUTH_SIGNED          (TPM_ST)(0x8025)
	 * #define TPM_ST_FU_MANIFEST          (TPM_ST)(0x8029)
	 */
	
	public short id();
	public String description();
	public TPM_ST byID();
}
