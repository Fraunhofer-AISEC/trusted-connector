package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;

public class TPM_ST extends StandardTPMStruct {
	
	public enum ST {
		TPM_ST_RSP_COMMAND			((short)0x00C4, "RSP_COMMAND"),
		TPM_ST_NULL 				((short)0X8000, "NULL"),			// ?? according to tpm20.h this is FIRST not RSA
		TPM_ST_NO_SESSIONS			((short)0X8001, "NO_SESSIONS"),
		TPM_ST_SESSIONS 			((short)0X8002, "SESSIONS"),
		TPM_ST_ATTEST_NV 			((short)0x8014, "ATTEST_NV"),
		TPM_ST_ATTEST_COMMAND_AUDIT	((short)0x8015, "ATTEST_COMMAND_AUDIT"),
		TPM_ST_ATTEST_SESSION_AUDIT	((short)0x8016, "ATTEST_SESSION_AUDIT"),
		TPM_ST_ATTEST_CERTIFY 		((short)0x8017, "ATTEST_CERTIFY"),
		TPM_ST_ATTEST_QUOTE			((short)0x8018, "ATTEST_QUOTE"),
		TPM_ST_ATTEST_TIME			((short)0x8019, "ATTEST_TIME"),
		TPM_ST_ATTEST_CREATION		((short)0x801A, "ATTEST_CREATION"),
		TPM_ST_CREATION				((short)0x8021, "CREATION"),
		TPM_ST_VERIFIED				((short)0x8022, "VERIFIED"),
		TPM_ST_AUTH_SECRET			((short)0x8023, "AUTH_SECRET"),
		TPM_ST_HASHCHECK			((short)0x8024, "HASHCHECK"),
		TPM_ST_AUTH_SIGNED			((short)0x8025, "AUTH_SIGNED"),
		TPM_ST_FU_MANIFEST			((short)0x8029, "FU_MANIFEST");

		private final short id;   			// UNIT16
	    private final String description;	// String representation
	    
	    ST(short id, String description) {
	        this.id = id;
	        this.description = description;
	    }
	    
	    private short id() { 
	    	return id; 
	    }
	    
	    private String description() { 
	    	return description; 
	    }
	    
	    public static ST byID(short id) {
	        for (ST i : ST.values()) {
	            if (i.id() == id) {
	                return i;
	            }
	        }
	        return TPM_ST_NULL;
	    }
	}

	// default to error
	private ST stId = ST.TPM_ST_NULL;
	
	public TPM_ST() {
		this.stId = ST.TPM_ST_NULL;
	}

	public ST getStId() {
		return stId;
	}
	
	public byte[] Id() {
		return this.toBytes();
	}	

	public void setStId(ST stId) {
		this.stId = stId;
	}
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(stId.id());
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.stId = ST.byID(brw.readShort());
	}

	@Override
	public String toString() {
		return "id(" + this.getStId().id() + "),description(" + this.getStId().description() + ")";
	}

}
