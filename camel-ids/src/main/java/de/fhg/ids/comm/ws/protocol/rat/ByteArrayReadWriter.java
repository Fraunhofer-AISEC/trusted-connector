package de.fhg.ids.comm.ws.protocol.rat;

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
 * Convenience class that can be used in toBytes and fromBytes.
 * 
 * @author lfgs
 */
public class ByteArrayReadWriter
{
    private byte[] bytes;
    private int offset;

    public ByteArrayReadWriter( byte[] source, int offset )
    {
        this.bytes = source;
        this.offset = offset;
    }

    public ByteArrayReadWriter( byte[] source )
    {
        this( source, 0 );
    }
    
    public ByteArrayReadWriter( int length )
    {
        this( new byte[length], 0 );
    }
    
//    Commenting this out.  It is tempting to use this, but it can
//    cause problems when used in a fromBytes of a structure that's in the middle
//    of another structure because it might "eat up" the rest of the structure.
//    A better solution in such cases is to always include a size indicator
//    before writing out the bytes of the internal structure
//     
//    /**
//     * Checks if there are bytes left in the whole array.
//     */
//    public boolean hasMore()
//    {
//        return this.offset < this.bytes.length;
//    }

    public int getOffset()
    {
        return offset;
    }

    public void setOffset( int offset )
    {
        this.offset = offset;
    }

    public byte[] getBytes()
    {
        return bytes;
    }

    /**
     * Returns a clone of all the bytes written so far (up to offset).
     * 
     * @return
     */
    public byte[] getBytesWritten()
    {
        byte[] ret = new byte[offset];
        System.arraycopy( bytes, 0, ret, 0, offset );
        return ret;
    }

    /**
     * Read array of length bytes starting from offset.
     * 
     * @param length -- number of bytes to read
     * @return
     */
    public byte[] readBytes( int length )
    {
        byte[] ret = ByteArrayUtil.readBytes( bytes, offset, length );
        offset += length;
        return ret;
    }

    // NOTE: removed this method because it's dangerous.  It won't work
    // if the bytearrayable being read is in the middle of another structure.
//    /**
//     * Read array of bytes starting from offset to the end of the bytes array.
//     * 
//     * @return
//     */
//    public byte[] readBytesToEnd()
//    {
//        return this.readBytes( bytes.length - offset );
//    }
    
    /**
     * Reads the next Int32 as a size, and then returns
     * size bytes after that.
     * 
     * @return
     */
    public byte[] readSizedByteArray()
    {
        int size = this.readInt32();
        return this.readBytes( size );
    }

    /**
     * Read 1-byte value as a boolean
     * 
     * @return
     */
    public boolean readBoolean()
    {
        boolean ret = ByteArrayUtil.readBoolean( bytes, offset );
        offset += 1;
        return ret;
    }

    /**
     * Read a single byte from offset.
     * 
     * @return
     */
    public byte readByte()
    {
        byte ret = bytes[offset];
        offset += 1;
        return ret;
    }

    /**
     * Read 16-bit int from offset as a short
     *
     * @return
     */
    public short readShort()
    {
        short ret = ByteArrayUtil.readShortBE( bytes, offset );
        offset += 2;
        return ret;
    }

    /**
     * Read 16-bit int from offset as unsigned value
     * and return an int (to be used when the return
     * value will be used in a place where int's are
     * required, e.g., as an index or offset into an array).
     * Note that if you use getShort instead,
     * the short that is returned will automatically
     * be converted to an int, and any values above
     * 2^15-1 will be turned into a negative number
     * even though it's not supposed to be.
     *
     * @return
     */
    public int readUInt16()
    {
        int ret = ByteArrayUtil.readUInt16BE( bytes, offset );
        offset += 2;
        return ret;
    }

    /**
     * Read 32-bit int from offset
     * 
     * @return
     */
    public int readInt32()
    {
        int ret = ByteArrayUtil.readInt32BE( bytes, offset );
        offset += 4;
        return ret;
    }
    
    public long readLong()
    {
        long ret = ByteArrayUtil.readLongBE( bytes, offset );
        offset += 8;
        return ret;
    }

    /**
     * Fill an instantiated ByteArrayable 
     * from the bytes starting from offset.
     * Increments the internal offset by the
     * length of ba.toBytes() afterwards.
     * <p>
     * FIXME: Note that this is a bit inefficient since
     * it has to call toBytes just to get the length
     *
     * @param ba -- instance of a ByteArrayable to call fromBytes on
     */
    public void readStruct( ByteArrayable ba )
    {
        ba.fromBytes( bytes, offset );
        offset += ba.toBytes().length;
    }

    /**
     * Write 1-byte representation of a Boolean
     * 
     * @param w --16-bit integer value
     */
    public void writeBoolean( boolean b )
    {
        ByteArrayUtil.writeBoolean( bytes, offset, b );
        offset += 1;
    }

    /**
     * Writes a single byte at offset.
     * 
     * @param b -- 8-bit byte value
     */
    public void writeByte( byte b )
    {
        bytes[offset] = b;
        offset += 1;
    }

    /**
     * Write 16-bit integer at offset.
     * (Writes MSB first.)
     * 
     * @param w --16-bit integer value
     */
    public void writeShort( short w )
    {
        ByteArrayUtil.writeShortBE( bytes, offset, w );
        offset += 2;
    }

    /**
     * Write 32-bit integer at offset.
     * (Writes MSB first.)
     * 
     * @param w -- 32-bit integer value
     */
    public void writeInt32( int w )
    {
        ByteArrayUtil.writeInt32BE( bytes, offset, w );
        offset += 4;
    }
    
    public void writeLong( long l )
    {
        ByteArrayUtil.writeLongBE( bytes, offset, l );
        offset += 8;
    }

    /**
     * Write array of bytes starting at offset
     * 
     * @param data -- the bytes array of bytes
     */
    public void writeBytes( byte[] data )
    {
        ByteArrayUtil.writeBytes( bytes, offset, data );
        offset += data.length;
    }
    
    /**
     * Writes the size of the data first as an int32,
     * and then the data itself.  Data written in this
     * way, can be read using readSizedByteArray()
     * 
     * @param data
     */
    public void writeSizedByteArray( byte[] data )
    {
        this.writeInt32( data.length );
        this.writeBytes( data );
    }

    /**
     * Write a varargs list of byte arrays 
     * array starting at offset
     * 
     * @param arrays -- any number of byte[] instances to write in sequence
     */
    public void writeBytes( byte[]... arrays )
    {
        this.writeBytes( ByteArrayUtil.concat( arrays ) );
    }

    /**
     * Write a ByteArrayable object starting at offset
     * 
     * @param bytes -- byte[] to write into
     */
    public void writeBytes( ByteArrayable source )
    {
        this.writeBytes( source.toBytes() );
    }

    /**
     * Write a varargs list of Objects (including primitive types
     * or ByteArrayables) into the sourceination array starting at offset
     * 
     * @param fields -- any number of byte[] instances to write in sequence
     */
    public void writeBytes( Object... fields )
    {
        byte[] concat = ByteArrayUtil.concatObjectsBE( fields );
        this.writeBytes( concat );
    }

}