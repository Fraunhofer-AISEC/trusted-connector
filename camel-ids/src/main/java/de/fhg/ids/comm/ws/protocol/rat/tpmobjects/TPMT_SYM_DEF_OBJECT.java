package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

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
        switch(this.algorithm.getAlgId().getAlgId()) {
        	case TPM_ALG_AES:
        		//this.keyBits = new
        		// CURRENT TODO !
        		break;
        	case TPM_ALG_NULL:
        		break;
        	default:
        		break;
        } 
	}

	public String toString() {
        return "TPMT_SYM_DEF_OBJECT: \n" 
        		+ "algorithm = " + this.algorithm.toString() + "\n"
        		+ "keyBits = " + this.keyBits.toString() + "\n"
        		+ "mode = " + this.mode.toString() + "\n";
    }

}
