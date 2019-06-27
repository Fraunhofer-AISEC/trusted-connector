package de.fhg.aisec.tpm2j.tpmu;

import de.fhg.aisec.tpm2j.TPM2Constants;
import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpm.TPM_ALG_ID.ALG_ID;

public class TPMU_HA extends StandardTPMStruct {

	/*
	 * TPMU_HA Union
	 * typedef union {
	 *     BYTE sha1[SHA1_DIGEST_SIZE];
	 *     BYTE sha256[SHA256_DIGEST_SIZE];
	 *     BYTE sm3_256[SM3_256_DIGEST_SIZE];
	 *     BYTE sha384[SHA384_DIGEST_SIZE];
	 *     BYTE sha512[SHA512_DIGEST_SIZE];
	 * } TPMU_HA;
	 */
	
	private int size = 0;
	private byte[] buffer = new byte[0];

	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}
	
	public TPMU_HA() {
	}
	
	// buffer size has to be set before we can read into the buffer
	public TPMU_HA(ALG_ID algId) throws Exception {
		switch(algId) {
			case TPM_ALG_SHA1:
				this.size = TPM2Constants.SHA1_DIGEST_SIZE;
				break;
			case TPM_ALG_SHA256:
				this.size = TPM2Constants.SHA256_DIGEST_SIZE;
				break;
			case TPM_ALG_SHA384:
				this.size = TPM2Constants.SHA384_DIGEST_SIZE;
				break;
			case TPM_ALG_SHA512:
				this.size = TPM2Constants.SHA512_DIGEST_SIZE;
				break;
			case TPM_ALG_SM3_256:
				this.size = TPM2Constants.SM3_256_DIGEST_SIZE;
				break;
			default:
				throw new Exception("TPMU_HA: hash algorithm \""+algId+"\" was not found.");
		}
		this.buffer = new byte[this.size];
	}	
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        if(this.size > 0 && this.size <= TPM2Constants.SHA512_DIGEST_SIZE) {
        	this.buffer = brw.readBytes(this.size);
        }
        else if(this.size == 0) {
        	throw new Exception("The buffer size of TPMU_HA has not been set."); 
        }
        else {
        	throw new Exception("The buffer of TPMU_HA is bigger than the allowed maximum of "+this.size+" byte.");
        }
	}

	@Override
	public String toString() {
		return "TPMU_HA[("+this.size+" byte) :"+ByteArrayUtil.toPrintableHexString(this.buffer)+"]";
	}
}
