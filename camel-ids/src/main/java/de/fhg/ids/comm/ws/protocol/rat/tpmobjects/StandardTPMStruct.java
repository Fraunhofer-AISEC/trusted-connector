package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ids.comm.ws.protocol.ProtocolMachine;

public abstract class StandardTPMStruct extends BasicByteArrayable implements TPMStruct {
	
	protected Logger LOG = LoggerFactory.getLogger(ProtocolMachine.class);
    
	public StandardTPMStruct() {
        super();
    }
	
    public StandardTPMStruct(byte[] source) {
        this.fromBytes( source, 0 );
    }

    // abstract method to be defined by each StandardTPMStruct
    public abstract byte[] toBytes();
    

    // abstract method to be defined by each StandardTPMStruct
    public abstract void fromBytes(byte[] source, int offset);

    
    // abstract method to be defined by each StandardTPMStruct
    public abstract String toString();

    // abstract method to be defined by each StandardTPMStruct
    public abstract byte[] getBuffer();
}