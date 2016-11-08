package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_HASH;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_ASYM_SCHEME;

public class TPMS_SCHEME_OAEP extends TPMU_ASYM_SCHEME {
	
	private TPMI_ALG_HASH hashId;

	public TPMI_ALG_HASH getHashId() {
		return hashId;
	}

	public void setHashId(TPMI_ALG_HASH hashId) {
		this.hashId = hashId;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashId);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.hashId = new TPMI_ALG_HASH();
        brw.readStruct(this.hashId);
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_OAEP:[\n\t\thashId = " + this.hashId.toString() + "\n]\n";
	}
}
