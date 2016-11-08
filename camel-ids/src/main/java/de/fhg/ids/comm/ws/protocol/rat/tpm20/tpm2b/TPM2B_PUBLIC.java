package de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b;

import java.math.BigInteger;

import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.StandardTPMStruct;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmt.TPMT_PUBLIC;

public class TPM2B_PUBLIC extends StandardTPMStruct {

	/*
     * TPM2B_PUBLIC Structure
     * typedef struct {
     *     UINT16      size;
     *     TPMT_PUBLIC publicArea
     * } TPM2B_PUBLIC;
     * 
     */

    private short size;
    private TPMT_PUBLIC publicArea;
    
    public TPM2B_PUBLIC() {
    }
    
    public TPM2B_PUBLIC(byte[] buffer) {
        this.fromBytes(buffer, 0);
    }

    public TPM2B_PUBLIC(short size, TPMT_PUBLIC publicArea) {
        this.size = size;
        this.publicArea = publicArea;
    }

    public short getSize() {
        return this.size;
    }

    public void setSize(short size) {
        this.size = size;
    }
    
    public TPMT_PUBLIC getPublicArea() {
        return this.publicArea;
    }

    public void setPublicArea(TPMT_PUBLIC publicArea) {
        this.publicArea = publicArea;
    }

    @Override
    public byte[] toBytes() {
        return ByteArrayUtil.buildBuf(size, publicArea);
    }

    @Override
    public void fromBytes( byte[] source, int offset ) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.size = brw.readShort();
        this.publicArea = new TPMT_PUBLIC();
        brw.readStruct(this.publicArea);
    }

    public String toString() {
        return "TPM2B_PUBLIC:[size = " + this.size + ", publicArea = " + this.publicArea.toString() + "]";
    }

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
	
	public BigInteger getModulus() {
		String hex = ByteArrayUtil.toPrintableHexString(this.getPublicArea().getUnique().getBuffer()).replaceAll("\n", " ").replaceAll(" ", "");
		return new BigInteger(hex, 16);
	}
}
