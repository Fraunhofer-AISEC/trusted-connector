package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID;

public class TPMI_ALG_KEYEDHASH_SCHEME extends StandardTPMStruct {

	/*
	 * TPMI_ALG_KEYEDHASH_SCHEME Type
	 * typedef TPM_ALG_ID TPMI_ALG_KEYEDHASH_SCHEME;
	 */
	
	private TPM_ALG_ID algId;

	public TPM_ALG_ID getAlgId() {
		return algId;
	}

	public void setAlgId(TPM_ALG_ID algId) {
		this.algId = algId;
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
        return "TPMI_ALG_KEYEDHASH_SCHEME:[algId = " + this.algId.getAlgId().name() + "]";
    }

}
