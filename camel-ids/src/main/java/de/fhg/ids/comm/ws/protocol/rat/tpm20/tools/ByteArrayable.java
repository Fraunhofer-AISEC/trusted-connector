package de.fhg.ids.comm.ws.protocol.rat.tpm20.tools;

public interface ByteArrayable {
    public byte[] toBytes();
    public void fromBytes(byte[] source, int offset);
}