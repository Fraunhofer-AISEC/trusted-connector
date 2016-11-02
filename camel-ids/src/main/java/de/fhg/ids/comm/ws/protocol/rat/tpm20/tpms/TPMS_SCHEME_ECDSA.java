package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_ASYM_SCHEME;

public class TPMS_SCHEME_ECDSA extends TPMU_ASYM_SCHEME {
	
	private TPMS_SCHEME_SIGHASH hashId;

	public TPMS_SCHEME_SIGHASH getHashId() {
		return hashId;
	}

	public void setHashId(TPMS_SCHEME_SIGHASH hashId) {
		this.hashId = hashId;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashId);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
		this.hashId = new TPMS_SCHEME_SIGHASH();
        brw.readStruct(this.hashId);
	}
	
	@Override
    public String toString() {
		return "TPMS_SCHEME_ECDSA:[\n\t\talgId = " + this.hashId.toString() + "\n]\n";
    }
}
