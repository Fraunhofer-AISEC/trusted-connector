package de.fraunhofer.aisec.tpm2j.tpmi;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;

public class TPMI_YES_NO extends StandardTPMStruct {
	/*
	 * TPMI_YES_NO Type
	 * typedef BYTE TPMI_YES_NO;
	 */
	
	byte tpmiYesNo;

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(tpmiYesNo);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.tpmiYesNo = brw.readByte();
	}

	@Override
	public String toString() {
		byte[] array = { (byte) tpmiYesNo };
		return "TPMI_YES_NO:["+ByteArrayUtil.toPrintableHexString(array)+"]";
	}

}
