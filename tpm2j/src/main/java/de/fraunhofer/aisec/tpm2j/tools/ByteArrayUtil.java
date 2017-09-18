package de.fraunhofer.aisec.tpm2j.tools;

/*
 * Copyright (c) 2006, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in the 
 *    documentation and/or other materials provided with the distribution.
 *  - Neither the name of MIT nor the names of its contributors may be used 
 *    to endorse or promote products derived from this software without 
 *    specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * Original author:  Luis F. G. Sarmenta, MIT, 2006
 */

/**
 * This class contains useful functions (as static methods)
 * used by the TPM classes.
 * 
 * TODO: Implement Little Endian read and write
 * 
 * @author lfgs
 */
public class ByteArrayUtil
{
    public static byte[] toBytesShortBE( short w )
    {
        byte[] ret = new byte[2];
        writeShortBE( ret, 0, w );
        return ret;
    }

    public static int toUInt16( short s )
    {
        int ret = ((int) s) & 0x0000FFFF;
        return ret;
    }

    public static byte[] toBytesInt32BE( int w )
    {
        byte[] ret = new byte[4];
        writeInt32BE( ret, 0, w );
        return ret;
    }

    public static void writeBoolean( byte[] target, int offset, boolean b )
    {
        target[offset] = b ? (byte) 1 : (byte) 0;
    }

    public static void writeByte( byte[] target, int offset, byte b )
    {
        target[offset] = b;
    }

    public static void writeShortBE( byte[] target, int offset, short w )
    {
        target[offset] = (byte) ((w >> 8) & 0xFF); // most significant byte first
        target[offset + 1] = (byte) (w & 0xFF); // least significant byte next
    }

    public static void writeInt32BE( byte[] target, int offset, int w )
    {
        target[offset] = (byte) ((w >> 24) & 0xFF); // most significant byte first
        target[offset + 1] = (byte) ((w >> 16) & 0xFF);
        target[offset + 2] = (byte) ((w >> 8) & 0xFF);
        target[offset + 3] = (byte) (w & 0xFF); // least significant byte next
    }

    public static void writeLongBE( byte[] target, int offset, long w )
    {
        target[offset] = (byte) ((w >> 56) & 0xFF); // most significant byte first
        target[offset + 1] = (byte) ((w >> 48) & 0xFF);
        target[offset + 2] = (byte) ((w >> 40) & 0xFF);
        target[offset + 3] = (byte) ((w >> 32) & 0xFF);
        target[offset + 4] = (byte) ((w >> 24) & 0xFF);
        target[offset + 5] = (byte) ((w >> 16) & 0xFF);
        target[offset + 6] = (byte) ((w >> 8) & 0xFF);
        target[offset + 7] = (byte) (w & 0xFF); // least significant byte next
    }

    public static void writeBytes( byte[] target, int offset, byte[] data )
    {
        System.arraycopy( data, 0, target, offset, data.length );
    }

    // NOTE: Commenting this out since there is potential confusion here
    // with writeObjectsBE where the objects just happen to be byte[], int, and int
    //    public static void writeBytes( byte[] target, int targetOffset,
    //        byte[] source, int srcOffset, int length )
    //    {
    //        System.arraycopy( source, srcOffset, target, targetOffset, length );
    //    }

    /**
     * Write a varargs list of Objects (including primitive types
     * or primitive type wrappers, and ByteArrayables) into the destination array starting at offset
     * 
     * @param target -- byte[] to write into
     * @param targetOffset -- target offset to write to
     * @param objects -- any number of byte[] instances to write in sequence
     */
    public static void writeObjectsBE( byte[] target, int targetOffset,
        Object... objects )
    {
        byte[] concat = concatObjectsBE( objects );
        writeBytes( target, targetOffset, concat );
    }

    public static boolean readBoolean( byte[] source, int offset )
    {
        return (source[offset] != 0);
    }

    public static byte readByte( byte[] source, int offset )
    {
        return source[offset];
    }

    public static short readShortBE( byte[] source, int offset )
    {
        // NOTE: We need to do this so that the bytes don't get sign-extended
        // before doing the OR operation.
        // The following produces an int with all but the lowest byte equal to 0
        int byte0 = source[offset] & 0xff;
        int byte1 = source[offset + 1] & 0xff;

        return (short) ((byte0 << 8) | byte1);
    }

    /**
     * Reads the next 2 bytes at offset in target
     * as an unsigned short int (in Big-Endian order).
     * Note that this method MUST be used in place of <code>readShortBE()</code>
     * in places where the receiving variable must be an 
     * int -- e.g., in places where we are reading a value which
     * will be used as an array size or an offset, or anything
     * which will be used in an arithmetic operation.
     * Otherwise, the short returned to readShortBE may be
     * incorrectly treated as a negative number when it
     * is converted into an int.
     * 
     * @param source source to read from
     * @param offset from the beginnig
     * @return int as unsigned short integer (in Big-Endian order)
     */
    public static int readUInt16BE( byte[] source, int offset )
    {
        // NOTE: We need to do this so that the bytes don't get sign-extended
        // before doing the OR operation.
        // The following produces an int with all but the lowest byte equal to 0
        int byte0 = source[offset] & 0xff;
        int byte1 = source[offset + 1] & 0xff;

        return ((byte0 << 8) | byte1);
    }

    //    public static int readSInt16BE( byte[] target, int offset )
    //    {
    //        int ret = readUInt16BE( target, offset );
    //        if ( (ret & 0x8000) != 0 )
    //        {
    //            // sign-extend if bit 15 is 1
    //            ret = ret | 0xFFFF0000;
    //        }
    //        return ret;
    //    }

    public static int readInt32BE( byte[] source, int offset )
    {
        // NOTE: We need to do this so that the bytes don't get sign-extended
        // before doing the OR operation.
        // The following produces an int with all but the lowest byte equal to 0
        int byte0 = source[offset] & 0xff;
        int byte1 = source[offset + 1] & 0xff;
        int byte2 = source[offset + 2] & 0xff;
        int byte3 = source[offset + 3] & 0xff;

        return (byte0 << 24) | (byte1 << 16) | (byte2 << 8) | byte3;
    }

    public static long readLongBE( byte[] source, int offset )
    {
        // NOTE: We need to do this so that the bytes don't get sign-extended
        // before doing the OR operation.
        // The following produces an int with all but the lowest byte equal to 0
        long byte0 = source[offset] & 0xff;
        long byte1 = source[offset + 1] & 0xff;
        long byte2 = source[offset + 2] & 0xff;
        long byte3 = source[offset + 3] & 0xff;
        long byte4 = source[offset + 4] & 0xff;
        long byte5 = source[offset + 5] & 0xff;
        long byte6 = source[offset + 6] & 0xff;
        long byte7 = source[offset + 7] & 0xff;

        return (byte0 << 56) | (byte1 << 48) | (byte2 << 40) | (byte3 << 32)
            | (byte4 << 24) | (byte5 << 16) | (byte6 << 8) | byte7;
    }

    public static byte[] readBytes( byte[] source, int offset, int length )
    {
        byte[] ret = new byte[length];
        System.arraycopy( source, offset, ret, 0, length );
        return ret;
    }

    /**
     * Read array of bytes starting from offset to the end of the source array.
     * 
     * @param source -- byte array to read from
     * @param offset -- offset from the byte array
     * @return byte[] the read bytes
     */
    public static byte[] readBytesToEnd( byte[] source, int offset )
    {
        return ByteArrayUtil.readBytes( source, offset, source.length - offset );
    }

    /**
     * Fill an instantiated ByteArrayable 
     * from the bytes starting from offset.
     *
     * @param source -- byte array to read from
     * @param offset -- offset from the byte array
     * @param ba -- instance of a ByteArrayable to call fromBytes on
     * @throws Exception will be thrown if the struct can not be read 
     */
    public static void readStruct( byte[] source, int offset, ByteArrayable ba ) throws Exception
    {
        ba.fromBytes( source, offset );
    }

    public static byte[] concat( byte[]... arrays )
    {
        int totalLength = 0;
        for ( byte[] a : arrays )
        {
            if ( a != null )
            {
                totalLength += a.length;
            }
        }
        byte[] ret = new byte[totalLength];
        int cur = 0;
        for ( byte[] a : arrays )
        {
            if ( a != null )
            {
                System.arraycopy( a, 0, ret, cur, a.length );
                cur += a.length;
            }
        }
        return ret;
    }

    public static byte[] concat( ByteArrayable... bas )
    {
        byte[][] arrays = new byte[bas.length][];
        for ( int i = 0; i < arrays.length; i++ )
        {
            if ( bas[i] != null )
            {
                arrays[i] = bas[i].toBytes();
            }
            else
            {
                arrays[i] = new byte[0];
            }
        }
        return ByteArrayUtil.concat( arrays );
    }

    public static byte[] concatObjectsBE( Object... objects )
    {
        byte[][] arrays = new byte[objects.length][];
        for ( int i = 0; i < objects.length; i++ )
        {
            Object o = objects[i];

            if ( o instanceof Boolean )
            {
                arrays[i] = new byte[1];
                writeBoolean( arrays[i], 0, ((Boolean) o).booleanValue() );
            }
            if ( o instanceof Byte )
            {
                arrays[i] = new byte[1];
                writeByte( arrays[i], 0, ((Byte) o).byteValue() );
            }
            else if ( o instanceof Short )
            {
                arrays[i] = new byte[2];
                writeShortBE( arrays[i], 0, ((Short) o).shortValue() );
            }
            else if ( o instanceof Integer )
            {
                arrays[i] = new byte[4];
                writeInt32BE( arrays[i], 0, ((Integer) o).intValue() );
            }
            else if ( o instanceof Long )
            {
                arrays[i] = new byte[8];
                writeLongBE( arrays[i], 0, ((Long) o).longValue() );
            }
            else if ( o instanceof byte[] )
            {
                arrays[i] = (byte[]) o;
            }
            else if ( o instanceof ByteArrayable )
            {
                arrays[i] = ((ByteArrayable) o).toBytes();
            }
        }
        return ByteArrayUtil.concat( arrays );
    }

    /**
     * Produces a string using each byte as an ASCII character
     * @param data byte array that will be converted to a ascii string
     * @return String the ascii String
     */
    public static String toASCIIString( byte[] data )
    {
        if ( data == null )
        {
            return null;
        }

        // TODO: write more efficient implementation
        String s = "";
        for ( int i = 0; i < data.length; i++ )
        {
            char b = (char) data[i];
            //            if ( (b >= 0) && (b < 16) )
            //            {
            //                s = s + "0";
            //            }
            s = s + b;
        }
        return s;
    }

    /**
     * Produces a string with each byte in two-digit hex
     * and no spaces between each byte.
     * @param data byte array that will be converted to a hex string
     * @return String the hex String
     */
    public static String toHexString( byte[] data )
    {
        if ( data == null )
        {
            return null;
        }

        // TODO: write more efficient implementation
        String s = "";
        for ( int i = 0; i < data.length; i++ )
        {
            byte b = data[i];
            if ( (b >= 0) && (b < 16) )
            {
                s = s + "0";
            }
            s = s + Integer.toHexString( b & 0xff );
        }
        return s;
    }

    public static byte[] parseHexString( String s )
        throws NumberFormatException
    {
        s = s.toUpperCase();
        if ( s.startsWith( "0X" ) )
        {
            s = s.substring( 2 );
        }
        if ( (s.length() & 1) != 0 )
        {
            // if length is not even, add 0 to beginning
            s = "0" + s;
        }

        byte[] bytes = new byte[s.length() / 2];

        for ( int i = 0; i < s.length(); i += 2 )
        {
            char h = s.charAt( i );
            byte hVal = 0;
            if ( (h >= '0') && (h <= '9') )
            {
                hVal = (byte) (h - '0');
            }
            else if ( (h >= 'A') && (h <= 'F') )
            {
                hVal = (byte) ((h - 'A') + 10);
            }
            else
            {
                throw new NumberFormatException();
            }

            char l = s.charAt( i + 1 );
            byte lVal = 0;
            if ( (l >= '0') && (l <= '9') )
            {
                lVal = (byte) (l - '0');
            }
            else if ( (l >= 'A') && (l <= 'F') )
            {
                lVal = (byte) ((l - 'A') + 10);
            }
            else
            {
                throw new NumberFormatException();
            }
            byte val = (byte) ((hVal << 4) | lVal);
            bytes[i / 2] = val;
        }

        return bytes;
    }

    /**
     * Produces a string with each byte in two-digit hex
     * and spaces between each byte.
     * @param data byte array which will be printed
     * @return String the printable String
     */
    public static String toPrintableHexString( byte[] data )
    {
        if ( data == null )
        {
            return null;
        }

        // TODO: write more efficient implementation
        String s = "";
        if ( data.length > 20 )
        {
            s = "\n";
        }
        for ( int i = 0; i < data.length; i++ )
        {
            byte b = data[i];

            // for debugging purposes, to see if any of the bytes are ASCII
            //            if ( b >= 0x20 && b <= 127 )
            //            {
            //                s = s + " " + (char) b;
            //            }

            if ( (b >= 0) && (b < 16) )
            {
                s = s + "0";
            }
            s = s + Integer.toHexString( b & 0xff );

            if ( i < data.length - 1 )
            {
                if ( i % 20 == 19 )
                {
                    s = s + "\n";
                }
                else
                {
                    s = s + " ";
                }
            }
        }
        if ( data.length > 20 )
        {
            s = s + "\n";
        }
        return s;
    }

    public static String toPrintableHexString( ByteArrayable ba )
    {
        if ( ba == null )
        {
            return null;
        }
        return ByteArrayUtil.toPrintableHexString( ba.toBytes() );
    }

    public static byte[] buildBuf( Object... objects )
    {
        return concatObjectsBE( objects );
    }
}