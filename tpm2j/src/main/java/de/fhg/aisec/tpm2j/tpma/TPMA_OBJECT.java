package de.fhg.aisec.tpm2j.tpma;

import de.fhg.aisec.tpm2j.struct.StandardTPMStruct;
import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;

import java.util.Arrays;

/**
 * A TPMA_OBJECT bitmap, which describes the kind of key (you specify whether it’s a signing key, duplicable, and so on, just as though you were creating the key)
 * 
 * @author georgraess
 * @version 1.0.3
 * @since 01.12.2016
 */
public class TPMA_OBJECT extends StandardTPMStruct {
	
	/*
	 * TPMA_OBJECT Bits
	 * typedef struct {
	 *     UINT32 reserved1            : 1;
	 *     UINT32 fixedTPM             : 1;
	 *     UINT32 stClear              : 1;
	 *     UINT32 reserved4            : 1;
	 *     UINT32 fixedParent          : 1;
	 *     UINT32 sensitiveDataOrigin  : 1;
	 *     UINT32 userWithAuth         : 1;
	 *     UINT32 adminWithPolicy      : 1;
	 *     UINT32 reserved8_9          : 2;
	 *     UINT32 noDA                 : 1;
	 *     UINT32 encryptedDuplication : 1;
	 *     UINT32 reserved12_15        : 4;
	 *     UINT32 restricted           : 1;
	 *     UINT32 decrypt              : 1;
	 *     UINT32 sign                 : 1;
	 *     UINT32 reserved19_31        : 13;
	 * } TPMA_OBJECT;
	 */
	
	// a 4*8 = 32 bit bitmask
	byte[] mask = new byte[4];
	
	public TPMA_OBJECT() {
	}

	public TPMA_OBJECT(byte[] buffer) {
		this.fromBytes(buffer, 0);
	}
	
	public byte[] getMask() {
		return mask;
	}

	public void setMask(byte[] mask) {
		this.mask = mask;
	}

	public byte[] getByte(int from, int to) {
		return Arrays.copyOfRange(this.mask, from, to);
	}
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(mask);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
		this.mask = brw.readBytes(4);
	}

	@Override
	public String toString() {
		return "TPMA_OBJECT:[bitmap=0x" + ByteArrayUtil.toPrintableHexString(this.mask).replaceAll(" ", "").toLowerCase() + "]";
	}	

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
}
