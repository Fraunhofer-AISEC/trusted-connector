package de.fhg.aisec.tpm2j.tools;

public interface ByteArrayable {
    public byte[] toBytes();
    public void fromBytes(byte[] source, int offset) throws Exception;
}