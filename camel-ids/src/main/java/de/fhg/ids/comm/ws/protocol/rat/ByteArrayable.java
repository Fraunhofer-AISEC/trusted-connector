package de.fhg.ids.comm.ws.protocol.rat;

public interface ByteArrayable {
    public byte[] toBytes();
    public void fromBytes(byte[] source, int offset);
}