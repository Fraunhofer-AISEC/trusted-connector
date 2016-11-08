package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationClientHandler;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.TPM2Constants;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_PUBLIC_ID;

public class TPM2B_PUBLIC_KEY_RSA extends TPMU_PUBLIC_ID {
	
	/*
	 * TPM2B_PUBLIC_KEY_RSA Structure
	 * typedef struct {
	 *     UINT16 size;
	 *     BYTE   buffer[MAX_RSA_KEY_BYTES];
	 * } TPM2B_PUBLIC_KEY_RSA;
	 */
	
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationClientHandler.class);
	private byte[] buffer = new byte[TPM2Constants.MAX_RSA_KEY_BYTES];
	private short size = 0;

	
	public short getSize() {
		return size;
	}

	public void setSize(short size) {
		this.size = size;
	}

	public byte[] getBuffer() {
		return buffer;
	}
	
	public byte[] getKey() {
		return this.getBuffer();
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}
	
	public int getBufferLength() {
		return buffer.length;
	}	

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(size, buffer);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.size = brw.readShort();
        if(this.size <= TPM2Constants.MAX_RSA_KEY_BYTES) {
            this.buffer = new byte[this.getSize()];
            this.buffer = brw.readBytes(this.getSize());        	
        }
        else {
        	LOG.debug("error: buffer of TPM2B_PUBLIC_KEY_RSA is larger then MAX_RSA_KEY_BYTES");
        }
	}

	@Override
    public String toString() {
		String key = ByteArrayUtil.toPrintableHexString(this.buffer).replace("\n", "");
		return "TPM2B_PUBLIC_KEY_RSA:[(" + this.getSize() + " bytes): "+ key + "]";
    }
}
