package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

import de.fhg.ids.comm.ws.protocol.rat.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.ByteArrayUtil;

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
		int bufferLength = this.getNameLength();
		return ByteArrayUtil.buildBuf(bufferLength, name);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.size = brw.readShort();
        this.setName(brw.readBytes(this.size));
	}

	@Override
	public String toString() {
		return "TPM2B_NAME:[(" + this.getSize() + " bytes): " + ByteArrayUtil.toPrintableHexString(this.name) + "]";
	}

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
}
