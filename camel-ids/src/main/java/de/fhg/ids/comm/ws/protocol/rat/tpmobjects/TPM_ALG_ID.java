package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPM_ALG_ID extends StandardTPMStruct {
	
	public enum ALG_ID {
		TPM_ALG_ERROR			((short)0x00, "ERROR"),
		TPM_ALG_RSA				((short)0x01, "RSA"),			// ?? according to tpm20.h this is FIRST not RSA
		TPM_ALG_SHA1			((short)0x04, "SHA1"),
		TPM_ALG_AES				((short)0x06, "AES"),
		TPM_ALG_KEYEDHASH		((short)0x08, "KEYEDHASH"),
		TPM_ALG_SHA256			((short)0x0B, "SHA256"),
		TPM_ALG_SHA384			((short)0x0C, "SHA384"),
		TPM_ALG_SHA512			((short)0x0D, "SHA512"),
		TPM_ALG_NULL			((short)0x10, "NULL"),
		TPM_ALG_SM3_256			((short)0x12, "SM3_256"),
		TPM_ALG_SM4				((short)0x13, "SM4"),
		TPM_ALG_RSASSA			((short)0x14, "RSASSA"),
		TPM_ALG_RSAES			((short)0x15, "RSAES"),
		TPM_ALG_RSAPSS			((short)0x16, "RSAPSS"),
		TPM_ALG_OAEP			((short)0x17, "OAEP"),
		TPM_ALG_ECDSA			((short)0x18, "ECDSA"),
		TPM_ALG_ECDH			((short)0x19, "ECDH"),
		TPM_ALG_ECDAA			((short)0x1A, "ECDAA"),
		TPM_ALG_SM2				((short)0x1B, "SM2"),
		TPM_ALG_ECSCHNORR		((short)0x1C, "ECSCHNORR"),
		TPM_ALG_ECMQV			((short)0x1D, "ECMQV"),
		TPM_ALG_KDF1_SP800_56a	((short)0x20, "KDF1_SP800_56a"),
		TPM_ALG_KDF2			((short)0x21, "KDF2"),
		TPM_ALG_KDF1_SP800_108	((short)0x22, "KDF1_SP800_108"),
		TPM_ALG_ECC				((short)0x23, "ECC"),
		TPM_ALG_SYMCIPHER		((short)0x25, "SYMCIPHER"),
		TPM_ALG_CTR				((short)0x40, "CTR"),
		TPM_ALG_OFB				((short)0x41, "OFB"),
		TPM_ALG_CBC				((short)0x42, "CBC"),
		TPM_ALG_CFB				((short)0x43, "CFB"),
		TPM_ALG_ECB				((short)0x44, "ECB"),
		TPM_ALG_LAST			((short)0x44, "LAST");
		
		private final short id;   			// UNIT16
	    private final String description;	// String representation
	    
	    ALG_ID(short id, String description) {
	        this.id = id;
	        this.description = description;
	    }
	    
	    private short id() { 
	    	return id; 
	    }
	    
	    private String description() { 
	    	return description; 
	    }
	    
	    public static ALG_ID byID(short id) {
	        for (ALG_ID i : ALG_ID.values()) {
	            if (i.id() == id) {
	                return i;
	            }
	        }
	        return TPM_ALG_ERROR;
	    }
	}

	// default to error
	private ALG_ID algId = ALG_ID.TPM_ALG_ERROR;
	
	public TPM_ALG_ID() {
		this.algId = ALG_ID.TPM_ALG_ERROR;
	}

	public ALG_ID getAlgId() {
		return algId;
	}

	public void setAlgId(ALG_ID algId) {
		this.algId = algId;
	}
	
	public String toString() {
		return "id(" + this.getAlgId().id() + "),description(" + this.getAlgId().description() + ")";
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(algId.id());
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.algId = ALG_ID.byID(brw.readShort());
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	}
}
