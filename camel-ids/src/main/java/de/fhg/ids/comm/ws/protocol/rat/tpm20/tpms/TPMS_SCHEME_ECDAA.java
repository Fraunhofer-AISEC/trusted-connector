package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmi.TPMI_ALG_HASH;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_ASYM_SCHEME;

public class TPMS_SCHEME_ECDAA extends TPMU_ASYM_SCHEME {
	
	/*
	 * TPMS_SCHEME_ECDAA Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH hashAlg;
	 *     UINT16        count;
	 * } TPMS_SCHEME_ECDAA;
	 */
	
	private TPMI_ALG_HASH hashId;
	private short count;

	public TPMI_ALG_HASH getHashId() {
		return hashId;
	}

	public void setHashId(TPMI_ALG_HASH hashId) {
		this.hashId = hashId;
	}

	public short getCount() {
		return count;
	}

	public void setCount(short count) {
		this.count = count;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashId, count);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.hashId = new TPMI_ALG_HASH();
        brw.readStruct(this.hashId);
        this.count = brw.readShort();
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_ECDAA:[\n\t\thashId := " + this.hashId.toString() + "\n"
				+ "\t\tcount := " + this.count + "\n]\n";
	}
}
