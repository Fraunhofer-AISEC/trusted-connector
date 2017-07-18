package de.fraunhofer.aisec.tpm2j.tpmi;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID;

public class TPMI_ALG_PUBLIC extends StandardTPMStruct {
	/* 
	 * TPMI_ALG_PUBLIC Type
	 * typedef TPM_ALG_ID TPMI_ALG_PUBLIC;
	 */
	
	private TPM_ALG_ID algId;
	
	public TPMI_ALG_PUBLIC() {
		// nothing
	}
	
	public TPMI_ALG_PUBLIC(TPM_ALG_ID algId) {
		this.algId = algId;
	}
	
	public TPM_ALG_ID getAlgId() {
		return algId;
	}

	public void setAlgId(TPM_ALG_ID algId) {
		this.algId = algId;
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(algId);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.algId = new TPM_ALG_ID();
        brw.readStruct(this.algId);
	}

	@Override
	public String toString() {
		return "TPMI_ALG_PUBLIC:[algId=" + this.algId.toString() + "]";
	}
	
}
