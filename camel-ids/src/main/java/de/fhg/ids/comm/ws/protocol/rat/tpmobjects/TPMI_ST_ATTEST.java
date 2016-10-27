package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public enum TPMI_ST_ATTEST implements TPM_ST {
	
	TPM_ST_RSP_COMMAND			((short)0x00C4, "RSP_COMMAND"),
	TPM_ST_NULL                 ((short)0X8000, "NULL"),
	TPM_ST_NO_SESSIONS          ((short)0x8001, "NO_SESSIONS"),
	TPM_ST_SESSIONS             ((short)0x8002, "SESSIONS "),
	TPM_ST_ATTEST_NV            ((short)0x8014, "ATTEST_NV"),
	TPM_ST_ATTEST_COMMAND_AUDIT ((short)0x8015, "ATTEST_COMMAND_AUDIT"),
	TPM_ST_ATTEST_SESSION_AUDIT ((short)0x8016, "ATTEST_SESSION_AUDIT"),
	TPM_ST_ATTEST_CERTIFY       ((short)0x8017, "ATTEST_CERTIFY"),
	TPM_ST_ATTEST_QUOTE         ((short)0x8018, "ATTEST_QUOTE"),
	TPM_ST_ATTEST_TIME          ((short)0x8019, "ATTEST_TIME"),
	TPM_ST_ATTEST_CREATION      ((short)0x801A, "ATTEST_CREATION"),
	TPM_ST_CREATION             ((short)0x8021, "CREATION"),
	TPM_ST_VERIFIED             ((short)0x8022, "VERIFIED"),
	TPM_ST_AUTH_SECRET          ((short)0x8023, "AUTH_SECRET"),
	TPM_ST_HASHCHECK            ((short)0x8024, "HASHCHECK"),
	TPM_ST_AUTH_SIGNED          ((short)0x8025, "AUTH_SIGNED"),
	TPM_ST_FU_MANIFEST          ((short)0x8029, "FU_MANIFEST");
	
	private final short id;   			// UNIT16
    private final String description;	// String representation
    
    TPMI_ST_ATTEST(short id, String description) {
        this.id = id;
        this.description = description;
    }

	@Override
	public short id() {
		return id;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public TPMI_ST_ATTEST byID() {
		for (TPMI_ST_ATTEST i : TPMI_ST_ATTEST.values()) {
            if (i.id() == id) {
                return i;
            }
        }
        return TPM_ST_NULL;
	}

}
