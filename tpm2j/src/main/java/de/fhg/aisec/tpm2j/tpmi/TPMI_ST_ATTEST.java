package de.fhg.aisec.tpm2j.tpmi;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpm.TPM_ST;

public class TPMI_ST_ATTEST extends StandardTPMStruct {
	/*
	 * TPMI_ST_ATTEST Type
	 * typedef TPM_ST TPMI_ST_ATTEST;
	 */

	private TPM_ST stId;
	
	public TPMI_ST_ATTEST() {
	}
	
	public TPMI_ST_ATTEST(TPM_ST id) {
		this.stId = id;
	}	

	public TPMI_ST_ATTEST(byte[] buffer) throws Exception {
		this.fromBytes(buffer, 0);
	}

	public TPM_ST getStId() {
		return stId;
	}

	public void setStId(TPM_ST stId) {
		this.stId = stId;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(stId);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.stId = new TPM_ST();
        brw.readStruct(this.stId);
	}
	
	@Override
    public String toString() {
		return "TPMI_ST_ATTEST:[stId=" + this.stId.toString() + "]";
    }	

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
}
