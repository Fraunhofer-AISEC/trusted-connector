package de.fhg.aisec.tpm2j.tpmt;

import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpm.TPM_ALG_ID;
import de.fhg.aisec.tpm2j.tpmi.TPMI_AES_KEY_BITS;
import de.fhg.aisec.tpm2j.tpmi.TPMI_ALG_SYM_OBJECT;
import de.fhg.aisec.tpm2j.tpmi.TPMI_SM4_KEY_BITS;
import de.fhg.aisec.tpm2j.tpmu.TPMU_PUBLIC_PARMS;
import de.fhg.aisec.tpm2j.tpmu.TPMU_SYM_KEY_BITS;
import de.fhg.aisec.tpm2j.tpmu.TPMU_SYM_MODE;

public class TPMT_SYM_DEF_OBJECT extends TPMU_PUBLIC_PARMS {
	
	/*
	 * TPMT_SYM_DEF_OBJECT Structure
	 * typedef struct {
	 *     TPMI_ALG_SYM_OBJECT algorithm;
	 *     TPMU_SYM_KEY_BITS   keyBits;
	 *     TPMU_SYM_MODE       mode;
	 * } TPMT_SYM_DEF_OBJECT;
	 */
	
	private TPMI_ALG_SYM_OBJECT algorithm;
	private TPMU_SYM_KEY_BITS keyBits;
	private TPMU_SYM_MODE mode;
	
	public TPMT_SYM_DEF_OBJECT() {
	}
	
	public TPMT_SYM_DEF_OBJECT(byte[] buffer) throws Exception {
		this.fromBytes(buffer, 0);
	}
	
	public TPMT_SYM_DEF_OBJECT(TPMI_ALG_SYM_OBJECT algorithm, TPMU_SYM_KEY_BITS keyBits, TPMU_SYM_MODE mode) {
		this.algorithm = algorithm;
		this.keyBits = keyBits;
		this.mode = mode;
	}

    public TPMI_ALG_SYM_OBJECT getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(TPMI_ALG_SYM_OBJECT algorithm) {
		this.algorithm = algorithm;
	}

	public TPMU_SYM_KEY_BITS getKeyBits() {
		return keyBits;
	}

	public void setKeyBits(TPMU_SYM_KEY_BITS keyBits) {
		this.keyBits = keyBits;
	}

	public TPMU_SYM_MODE getMode() {
		return mode;
	}

	public void setMode(TPMU_SYM_MODE mode) {
		this.mode = mode;
	}
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(algorithm, keyBits, mode);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.algorithm = new TPMI_ALG_SYM_OBJECT();
        brw.readStruct(this.algorithm);
        switch(this.algorithm.getAlgId().getAlgId()) {
        	case TPM_ALG_AES:
        		this.keyBits = new TPMI_AES_KEY_BITS();
        		brw.readStruct(this.keyBits);
        		break;
           	case TPM_ALG_SM4:
           		this.keyBits = new TPMI_SM4_KEY_BITS();
           		brw.readStruct(this.keyBits);
        		break;    
           	case TPM_ALG_XOR:
           		throw new Exception("TPMT_SYM_DEF_OBJECT: TPM_ALG_XOR not implemented yet.");  
           	case TPM_ALG_NULL:
        		this.keyBits = null;
        		break;
        	default:
        		throw new Exception("TPMT_SYM_DEF_OBJECT no algorithm found.");
        }
        
	}

	@Override
	public String toString() {
		if(this.algorithm.getAlgId().getAlgId().equals(TPM_ALG_ID.ALG_ID.TPM_ALG_NULL)) {
	        return "TPMT_SYM_DEF_OBJECT:[algorithm="+this.algorithm.toString()+"]";
		}
		else {
	        return "TPMT_SYM_DEF_OBJECT:[" 
	        		+ "algorithm=" + this.algorithm.toString()
	        		+ "keyBits=" + this.keyBits.toString()
	        		+ "mode=" + this.mode.toString() + "]";
			
		}
    }

}
