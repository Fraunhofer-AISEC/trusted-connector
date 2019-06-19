package de.fhg.aisec.tpm2j.tools;

import java.util.Arrays;

public abstract class BasicByteArrayable implements ByteArrayable {
    
    public boolean equals(Object compare) {
        if (compare instanceof ByteArrayable) {
            byte[] argBytes = ((ByteArrayable) compare).toBytes();
            return Arrays.equals( this.toBytes(), argBytes );
        }
        else {
            return false;
        }

    }

    public int hashCode() {
        return Arrays.hashCode(this.toBytes());
    }

    public String toString() {
        return this.getClass().getName() + ":"+ ByteArrayUtil.toPrintableHexString(this);
    }

    public abstract byte[] toBytes();

    public abstract void fromBytes( byte[] source, int offset ) throws Exception;
}
