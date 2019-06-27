package de.fhg.aisec.tpm2j.tpm;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
/**
 * TPM_ECC_CURVE defines an enum ECC_CURVE for standard ECC
 * curves used in conjunction with TPM 2.0.
 * 
 * @author georgraess
 * @version 1.0.3
 * @since 01.12.2016
 */
public class TPM_ECC_CURVE extends StandardTPMStruct {
	
	public enum ECC_CURVE {
		TPM_ECC_NONE  			((short)0x00, "NONE"),
		TPM_ECC_NIST_P192		((short)0x01, "NIST_P192"),
		TPM_ECC_NIST_P224		((short)0x02, "NIST_P224"),
		TPM_ECC_NIST_P256		((short)0x03, "NIST_P256"),
		TPM_ECC_NIST_P384		((short)0x04, "NIST_P384"),
		TPM_ECC_NIST_P521		((short)0x05, "NIST_P521"),
		TPM_ECC_BN_P256			((short)0x10, "BN_P256"),
		TPM_ECC_BN_P638			((short)0x11, "BN_P638"),
		TPM_ECC_SM2_P256		((short)0x20, "SM2_P256");
		
		private final short id;   			// UNIT16
	    private final String description;	// String representation
	    
	    ECC_CURVE(short id, String description) {
	        this.id = id;
	        this.description = description;
	    }
	    
	    private short id() { 
	    	return id; 
	    }
	    
	    private String description() { 
	    	return description; 
	    }
	    
	    public static ECC_CURVE byID(short id) {
	        for (ECC_CURVE i : ECC_CURVE.values()) {
	            if (i.id() == id) {
	                return i;
	            }
	        }
	        return TPM_ECC_NONE;
	    }
	}

	// default to none
	private ECC_CURVE algId = ECC_CURVE.TPM_ECC_NONE;
	
	public TPM_ECC_CURVE() {
		this.algId = ECC_CURVE.TPM_ECC_NONE;
	}

	public ECC_CURVE getAlgId() {
		return algId;
	}
	
	public byte[] Id() {
		return this.toBytes();
	}	

	public void setAlgId(ECC_CURVE algId) {
		this.algId = algId;
	}
	
	public String toString() {
		return "(description=\"" + this.getAlgId().description() + "\", id=" + this.getAlgId().id() + ")";
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(algId.id());
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.algId = ECC_CURVE.byID(brw.readShort());
	}
}
