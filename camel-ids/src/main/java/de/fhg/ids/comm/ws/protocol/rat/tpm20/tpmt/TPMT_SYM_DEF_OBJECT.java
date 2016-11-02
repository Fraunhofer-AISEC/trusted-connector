package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmt;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_KEY_BITS;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID.ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_AES_KEY_BITS;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_SYM_OBJECT;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_SM4_KEY_BITS;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_PUBLIC_PARMS;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_SYM_KEY_BITS;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_SYM_MODE;

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
	
	public TPMT_SYM_DEF_OBJECT(byte[] buffer) {
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
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.algorithm = new TPMI_ALG_SYM_OBJECT();
        brw.readStruct(this.algorithm);
        TPM_KEY_BITS bits = new TPM_KEY_BITS();
        switch(this.algorithm.getAlgId().getAlgId()) {
        	case TPM_ALG_AES:
        		this.keyBits = new TPMI_AES_KEY_BITS();
        		brw.readStruct(this.keyBits);
        		brw.readStruct(bits);
        		this.keyBits.setSym(bits);
        		break;
           	case TPM_ALG_SM4:
           		this.keyBits = new TPMI_SM4_KEY_BITS();
           		brw.readStruct(this.keyBits);
           		brw.readStruct(bits);
           		this.keyBits.setSym(bits);
        		break;      
           	case TPM_ALG_NULL:
        		this.keyBits = null;
        		break;
        	default:
        		LOG.debug("error: no TPMI_ALG_SYM_OBJECT algorithm given !");
        		break;
        }
        
	}

	@Override
	public String toString() {
		if(this.algorithm.getAlgId().getAlgId().equals(TPM_ALG_ID.ALG_ID.TPM_ALG_NULL)) {
	        return "TPMT_SYM_DEF_OBJECT:[TPM_ALG_NULL]";
		}
		else {
	        return "TPMT_SYM_DEF_OBJECT: \n" 
	        		+ "algorithm = " + this.algorithm.toString() + "\n"
	        		+ "keyBits = " + this.keyBits.toString() + "\n"
	        		+ "mode = " + this.mode.toString() + "\n";
			
		}
    }

}
