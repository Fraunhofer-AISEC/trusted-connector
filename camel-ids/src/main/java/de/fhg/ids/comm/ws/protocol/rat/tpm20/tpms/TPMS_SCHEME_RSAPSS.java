package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_ASYM_SCHEME;

public class TPMS_SCHEME_RSAPSS extends TPMU_ASYM_SCHEME {
	
	private TPMS_SCHEME_SIGHASH scheme;

	public TPMS_SCHEME_SIGHASH getScheme() {
		return scheme;
	}

	public void setScheme(TPMS_SCHEME_SIGHASH scheme) {
		this.scheme = scheme;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(scheme);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.scheme = new TPMS_SCHEME_SIGHASH();
        brw.readStruct(this.scheme);
	}

	@Override
	public String toString() {
		return "TPMS_SCHEME_RSAPSS:[\n\t\talgId = " + this.scheme.toString() + "\n]\n";
	}
}
