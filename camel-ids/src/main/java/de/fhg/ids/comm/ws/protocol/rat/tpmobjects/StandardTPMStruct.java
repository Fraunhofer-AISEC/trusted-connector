package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public abstract class StandardTPMStruct extends BasicByteArrayable implements TPMStruct {
    
	public StandardTPMStruct() {
        super();
    }
	
    public StandardTPMStruct(byte[] source) {
        this.fromBytes( source, 0 );
    }

    // abstract method to define
    public abstract byte[] toBytes();
    
    // abstract method to define
    public abstract void fromBytes(byte[] source, int offset);
    
}