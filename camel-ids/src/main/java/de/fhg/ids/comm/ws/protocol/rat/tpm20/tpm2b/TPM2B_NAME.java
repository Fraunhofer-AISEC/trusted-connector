package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;

public class TPM2B_NAME extends StandardTPMStruct {
	
	/*
	 *  TPM2B_NAME Structure
	 *  typedef struct {
	 *      UINT16 size;
	 *      BYTE   name[sizeof(TPMU_NAME)];
	 *  } TPM2B_NAME;
	 */
	
	private short size = 0;
	private byte[] name = new byte[0];
	
	public short getSize() {
		return size;
	}

	public void setSize(short size) {
		this.size = size;
	}
	
	public byte[] getName() {
		return name;
	}
	
	public void setName(byte[] name) {
		this.name = name;
	}

	public int getNameLength() {
		return this.name.length;
	}
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(size, name);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.size = brw.readShort();
	    this.name = brw.readBytes(this.size);
	}

	@Override
	public String toString() {
		return "TPM2B_NAME:[(" + this.getSize() + " bytes): " + ByteArrayUtil.toPrintableHexString(this.name).replace("\n", "") + "]";
	}
}
