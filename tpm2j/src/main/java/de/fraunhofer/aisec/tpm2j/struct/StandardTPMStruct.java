package de.fraunhofer.aisec.tpm2j.struct;

import de.fraunhofer.aisec.tpm2j.tools.BasicByteArrayable;
/**
 * StandardTPMStruct is an abstract super class used by nearly all tpm2j structures.
 * these standard methods have to implement by each structure using it:
 * 
 * @author georgraess
 * @version 1.0.3
 * @since 01.12.2016
 */
public abstract class StandardTPMStruct extends BasicByteArrayable implements TPMStruct {
	/**
	 * default constructor.
	 * */
	public StandardTPMStruct() {
        super();
    }
	/**
	 * default constructor load data from a byte source.
	 * @param source source of the TPMStruct as byte array
	 * @throws Exception throws an Exception when this TPMStruct can not be constructed from the source
	 * */
    public StandardTPMStruct(byte[] source) throws Exception {
        this.fromBytes(source, 0);
    }
	/**
	 * alias for toBytes()
	 * @return byte[] this TPMStruct as a byte array.
	 * */
    public byte[] getBuffer() {
    	return this.toBytes();
    }
	/**
	 * abstract method toBytes() has to be defined by each StandardTPMStruct
	 * it returns this TPMStruct as a byte array.
	 * @return byte[] this TPMStruct as a byte array.
	 * */    
    public abstract byte[] toBytes();
	/**
	 * abstract method fromBytes() has to be defined by each StandardTPMStruct
	 * it constructs this TPMStruct from a byte array
	 * @param source source of the TPMStruct as byte array
	 * @param offset offset from the beginning inside the TPMStruct
	 * */
    public abstract void fromBytes(byte[] source, int offset) throws Exception;
	/**
	 * abstract method toString() has to be defined by each StandardTPMStruct
	 * it returns a String representation of this TPMStruct.
	 * @return String this TPMStruct in a String representation.
	 * */
    public abstract String toString();
}