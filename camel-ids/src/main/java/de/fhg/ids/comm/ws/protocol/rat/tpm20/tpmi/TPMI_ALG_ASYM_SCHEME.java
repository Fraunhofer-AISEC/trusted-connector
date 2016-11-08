package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID;

public class TPMI_ALG_ASYM_SCHEME extends StandardTPMStruct {
	/*
	 * TPMI_ALG_ASYM_SCHEME Type
	 * typedef TPM_ALG_ID TPMI_ALG_ASYM_SCHEME;
	 */

	private TPM_ALG_ID algId;
	
	public TPMI_ALG_ASYM_SCHEME() {
	}
	
	public TPMI_ALG_ASYM_SCHEME(TPM_ALG_ID id) {
		this.algId = id;
	}	

	public TPMI_ALG_ASYM_SCHEME(byte[] buffer) {
		this.fromBytes(buffer, 0);
	}

	public TPM_ALG_ID getAlgId() {
		return algId;
	}

	public void setAlgId(TPM_ALG_ID hashId) {
		this.algId = hashId;
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
		return "TPMI_ALG_ASYM_SCHEME:[algId = " + this.algId.toString() + "]";
    }

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 	
}
