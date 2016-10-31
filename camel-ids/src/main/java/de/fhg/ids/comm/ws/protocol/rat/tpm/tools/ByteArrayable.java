package de.fhg.ids.comm.ws.protocol.rat.tpm.tools;

public interface ByteArrayable {
    public byte[] toBytes();
    public void fromBytes(byte[] source, int offset);
}