package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

import de.fhg.ids.comm.ws.protocol.rat.tpm.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm.tools.ByteArrayUtil;

public class TPMI_ALG_RSA_SCHEME extends StandardTPMStruct {

	/*
	 * TPMI_ALG_RSA_SCHEME Type
	 * typedef TPM_ALG_ID TPMI_ALG_RSA_SCHEME;
	 */
	
	private TPM_ALG_ID algId;

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
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.algId = new TPM_ALG_ID();
        brw.readStruct(this.algId);
	}

	@Override
    public String toString() {
        return "TPMI_ALG_RSA_SCHEME:[algId = " + this.algId.getAlgId().name() + "]";
    }

}
