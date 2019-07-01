package de.fhg.aisec.ids.comm;

public class ByteArrayUtil {

    private static final String[] lookup = new String[256];

    static {
        for (int i = 0; i < lookup.length; ++i) {
            if (i < 16) {
                lookup[i] = "0" + Integer.toHexString(i);
            } else {
                lookup[i] = Integer.toHexString(i);
            }
        }
    }

    public static String toPrintableHexString(byte[] bytes) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0 && i % 16 == 0) {
                s.append('\n');
            } else {
                s.append(' ');
            }
            s.append(lookup[bytes[i] & 0xff]);
        }
        return s.toString();
    }

}
