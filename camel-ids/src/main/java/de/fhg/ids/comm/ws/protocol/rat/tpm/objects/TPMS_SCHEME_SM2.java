package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

import de.fhg.ids.comm.ws.protocol.rat.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.ByteArrayUtil;

public class TPMS_SCHEME_SM2 extends TPMU_ASYM_SCHEME {
	
	private TPMS_SCHEME_SIGHASH hashId;

	public TPMS_SCHEME_SIGHASH getHashId() {
		return hashId;
	}

	public void setHashId(TPMS_SCHEME_SIGHASH hashId) {
		this.hashId = hashId;
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
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
		return "TPMS_SCHEME_SM2:[hashId = " + this.hashId.toString() + "]";
	}

}
