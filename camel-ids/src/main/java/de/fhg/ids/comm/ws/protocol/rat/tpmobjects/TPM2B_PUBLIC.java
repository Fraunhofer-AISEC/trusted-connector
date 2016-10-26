package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPM2B_PUBLIC extends StandardTPMStruct {

	/*
	 * reconstructing a TPM2B_PUBLIC structure:
	 * 
     * // TPM2B_PUBLIC Structure
     * typedef struct {
     *     UINT16      size;
     *     TPMT_PUBLIC publicArea
     * } TPM2B_PUBLIC;
     * 
     */

    private short size;
    private TPMT_PUBLIC publicArea;

    public TPM2B_PUBLIC() {
        // do nothing
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
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.size = brw.readShort();
        this.publicArea = new TPMT_PUBLIC();
        brw.readStruct(this.publicArea);
    }

    public String toString() {
        return "TPM2B_PUBLIC: \n" 
            + "size = " + this.size + "\n" 
            + "publicArea: " + this.publicArea.toString();
    }
}
