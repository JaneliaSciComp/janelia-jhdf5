/*
 * Copyright 2007 - 2018 ETH Zuerich, CISD and SIS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.systemsx.cisd.base.io;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.nio.ByteOrder;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

/**
 * The interface of {@link java.io.RandomAccessFile}.
 * 
 * @author Bernd Rinn
 */
public interface IRandomAccessFile extends DataInput, DataOutput, Closeable, IInputStream,
        IOutputStream
{
    /**
     * Gets the byte-order (endiness) of the random access file. Default is network-byte order
     * (big-endian).
     */
    public ByteOrder getByteOrder();

    /**
     * Sets the byte-order (endiness) of the random access file.
     */
    public void setByteOrder(ByteOrder byteOrder);

    //
    // RandomAccessFile
    //

    /**
     * Returns the current offset in this file.
     * 
     * @return the offset from the beginning of the file, in bytes, at which the next read or write
     *         occurs.
     * @exception IOExceptionUnchecked if an I/O error occurs.
     */
    public long getFilePointer() throws IOExceptionUnchecked;

    /**
     * Sets the file-pointer offset, measured from the beginning of this file, at which the next
     * read or write occurs. The offset may be set beyond the end of the file. Setting the offset
     * beyond the end of the file does not change the file length. The file length will change only
     * by writing after the offset has been set beyond the end of the file.
     * 
     * @param pos the offset position, measured in bytes from the beginning of the file, at which to
     *            set the file pointer.
     * @exception IOExceptionUnchecked if <code>pos</code> is less than <code>0</code> or if an I/O
     *                error occurs.
     */
    public void seek(long pos) throws IOExceptionUnchecked;

    /**
     * Returns the length of this file.
     * 
     * @return the length of this file, measured in bytes.
     * @exception IOExceptionUnchecked if an I/O error occurs.
     */
    public long length() throws IOExceptionUnchecked;

    /**
     * Sets the length of this file.
     * <p>
     * If the present length of the file as returned by the <code>length</code> method is greater
     * than the <code>newLength</code> argument then the file will be truncated. In this case, if
     * the file offset as returned by the <code>getFilePointer</code> method is greater than
     * <code>newLength</code> then after this method returns the offset will be equal to
     * <code>newLength</code>.
     * <p>
     * If the present length of the file as returned by the <code>length</code> method is smaller
     * than the <code>newLength</code> argument then the file will be extended. In this case, the
     * contents of the extended portion of the file are not defined.
     * 
     * @param newLength The desired length of the file
     * @exception IOExceptionUnchecked If an I/O error occurs
     */
    public void setLength(long newLength) throws IOExceptionUnchecked;

    //
    // DataInput
    //

    /**
     * @see DataInput#readFully(byte[])
     */
    @Override
    public void readFully(byte b[]) throws IOExceptionUnchecked;

    /**
     * @see DataInput#readFully(byte[], int, int)
     */
    @Override
    public void readFully(byte b[], int off, int len) throws IOExceptionUnchecked;

    /**
     * @see DataInput#skipBytes(int)
     */
    @Override
    public int skipBytes(int n) throws IOExceptionUnchecked;

    /**
     * @see DataInput#readBoolean()
     */
    @Override
    public boolean readBoolean() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readByte()
     */
    @Override
    public byte readByte() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readUnsignedByte()
     */
    @Override
    public int readUnsignedByte() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readShort()
     */
    @Override
    public short readShort() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readUnsignedShort()
     */
    @Override
    public int readUnsignedShort() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readChar()
     */
    @Override
    public char readChar() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readInt()
     */
    @Override
    public int readInt() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readLong()
     */
    @Override
    public long readLong() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readFloat()
     */
    @Override
    public float readFloat() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readDouble()
     */
    @Override
    public double readDouble() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readLine()
     */
    @Override
    public String readLine() throws IOExceptionUnchecked;

    /**
     * @see DataInput#readUTF()
     */
    @Override
    public String readUTF() throws IOExceptionUnchecked;

    //
    // DataOutput
    //

    /**
     * @see DataOutput#write(int)
     */
    @Override
    public void write(int b) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#write(byte[])
     */
    @Override
    public void write(byte b[]) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#write(byte[], int, int)
     */
    @Override
    public void write(byte b[], int off, int len) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeBoolean(boolean)
     */
    @Override
    public void writeBoolean(boolean v) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeByte(int)
     */
    @Override
    public void writeByte(int v) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeShort(int)
     */
    @Override
    public void writeShort(int v) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeChar(int)
     */
    @Override
    public void writeChar(int v) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeInt(int)
     */
    @Override
    public void writeInt(int v) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeLong(long)
     */
    @Override
    public void writeLong(long v) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeFloat(float)
     */
    @Override
    public void writeFloat(float v) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeDouble(double)
     */
    @Override
    public void writeDouble(double v) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeBytes(String)
     */
    @Override
    public void writeBytes(String s) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeChars(String)
     */
    @Override
    public void writeChars(String s) throws IOExceptionUnchecked;

    /**
     * @see DataOutput#writeUTF(String)
     */
    @Override
    public void writeUTF(String str) throws IOExceptionUnchecked;
}
