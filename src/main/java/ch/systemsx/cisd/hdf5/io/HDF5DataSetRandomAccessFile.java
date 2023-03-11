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

package ch.systemsx.cisd.hdf5.io;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.util.Arrays;

import hdf.hdf5lib.exceptions.HDF5Exception;
import ch.systemsx.cisd.base.convert.NativeData;
import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.base.io.IRandomAccessFile;
import ch.systemsx.cisd.hdf5.HDF5DataClass;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5OpaqueType;
import ch.systemsx.cisd.hdf5.HDF5StorageLayout;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ch.systemsx.cisd.hdf5.exceptions.HDF5FileNotFoundException;

/**
 * A {@link IRandomAccessFile} backed by an HDF5 dataset. The HDF5 dataset needs to be a byte array
 * (or opaque byte array) of rank 1.
 * 
 * @author Bernd Rinn
 */
public class HDF5DataSetRandomAccessFile implements IRandomAccessFile, Flushable
{
    private final IHDF5Reader reader;

    private final IHDF5Writer writerOrNull;

    private final String dataSetPath;

    private final HDF5DataSetInformation dataSetInfo;

    private final HDF5OpaqueType opaqueTypeOrNull;

    private final int blockSize;

    private final boolean extendable;

    private final boolean closeReaderOnCloseFile;

    private long length;

    private int realBlockSize;

    private byte[] block;

    private long blockOffset;

    private int positionInBlock;

    private boolean blockDirty;

    private long blockOffsetMark = -1;

    private int positionInBlockMark = -1;

    private boolean extensionPending;

    private ch.systemsx.cisd.base.convert.NativeData.ByteOrder byteOrder =
            ch.systemsx.cisd.base.convert.NativeData.ByteOrder.BIG_ENDIAN;

    /**
     * Creates a new HDF5DataSetRandomAccessFile for the given hdf5File and dataSetPath.
     */
    HDF5DataSetRandomAccessFile(File hdf5File, String dataSetPath,
            HDF5GenericStorageFeatures creationStorageFeature, int size, String opaqueTagOrNull,
            boolean readOnly)
    {
        this(createHDF5ReaderOrWriter(hdf5File, readOnly), dataSetPath, creationStorageFeature,
                size, opaqueTagOrNull, true);
    }

    private static IHDF5Reader createHDF5ReaderOrWriter(File hdf5File, boolean readOnly)
    {
        try
        {
            if (readOnly)
            {
                return HDF5FactoryProvider.get().openForReading(hdf5File);
            } else
            {
                return HDF5FactoryProvider.get().open(hdf5File);
            }
        } catch (HDF5FileNotFoundException ex)
        {
            throw new IOExceptionUnchecked(new FileNotFoundException(ex.getMessage()));
        } catch (HDF5Exception ex)
        {
            throw new IOExceptionUnchecked(ex);
        }
    }

    /**
     * Creates a new HDF5DataSetRandomAccessFile for the given reader and dataSetPath.
     * <p>
     * If <code>reader instanceof IHDF5Writer</code>, the random access file will be in read-write
     * mode, else it will be in readonly mode.
     */
    HDF5DataSetRandomAccessFile(IHDF5Reader reader, String dataSetPath,
            HDF5GenericStorageFeatures creationStorageFeature, int size, String opaqueTagOrNull,
            boolean closeReaderOnCloseFile) throws IOExceptionUnchecked
    {
        this.closeReaderOnCloseFile = closeReaderOnCloseFile;
        final boolean readOnly = (reader instanceof IHDF5Writer) == false;
        try
        {
            if (readOnly)
            {
                this.reader = reader;
                this.writerOrNull = null;
            } else
            {
                this.writerOrNull = (IHDF5Writer) reader;
                this.writerOrNull.file().addFlushable(this);
                this.reader = writerOrNull;
                if (writerOrNull.exists(dataSetPath) == false)
                {
                    long maxSize = requiresFixedMaxSize(creationStorageFeature) ? size : 0;
                    if (opaqueTagOrNull == null)
                    {
                        writerOrNull.int8().createArray(dataSetPath, maxSize, size,
                                HDF5IntStorageFeatures.createFromGeneric(creationStorageFeature));
                    } else
                    {
                        writerOrNull.opaque().createArray(dataSetPath, opaqueTagOrNull, maxSize,
                                size, creationStorageFeature);
                    }
                }
            }
        } catch (HDF5Exception ex)
        {
            throw new IOExceptionUnchecked(ex);
        }
        this.dataSetPath = dataSetPath;
        this.dataSetInfo = reader.getDataSetInformation(dataSetPath);
        if (readOnly == false
                && dataSetInfo.getTypeInformation().getDataClass() == HDF5DataClass.OPAQUE)
        {
            this.opaqueTypeOrNull = reader.opaque().tryGetOpaqueType(dataSetPath);
        } else
        {
            this.opaqueTypeOrNull = null;
        }
        if (dataSetInfo.getRank() != 1)
        {
            throw new IOExceptionUnchecked("Dataset has wrong rank (r=" + dataSetInfo.getRank()
                    + ")");
        }
        if (dataSetInfo.getTypeInformation().getElementSize() != 1)
        {
            throw new IOExceptionUnchecked("Dataset has wrong element size (size="
                    + dataSetInfo.getTypeInformation().getElementSize() + " bytes)");
        }
        this.length = dataSetInfo.getSize();

        // Chunked data sets are read chunk by chunk, other layouts are read completely.
        if (dataSetInfo.getStorageLayout() == HDF5StorageLayout.CHUNKED)
        {
            this.blockSize = dataSetInfo.tryGetChunkSizes()[0];
        } else
        {
            // Limitation: we do not yet handle the case of contiguous data sets larger than 2GB
            if ((int) length != length())
            {
                throw new IOExceptionUnchecked("Dataset is too large (size=" + length + " bytes)");

            }
            this.blockSize = (int) length;
        }
        this.extendable = (dataSetInfo.getStorageLayout() == HDF5StorageLayout.CHUNKED);
        this.blockOffset = 0;
        this.block = new byte[blockSize];
        this.realBlockSize = -1;
        this.positionInBlock = 0;
    }

    private static boolean requiresFixedMaxSize(HDF5GenericStorageFeatures features)
    {
        return features.tryGetProposedLayout() != null
                && features.tryGetProposedLayout() != HDF5StorageLayout.CHUNKED;
    }

    private void ensureInitalizedForWriting(int lenCurrentOp) throws IOExceptionUnchecked
    {
        if (realBlockSize < 0)
        {
            realBlockSize = blockSize;
            long minLen = blockOffset + realBlockSize;
            final long oldLength = length();
            if (minLen > oldLength)
            {
                realBlockSize = Math.min(realBlockSize, lenCurrentOp);
                minLen = blockOffset + realBlockSize;
                if (minLen > oldLength)
                {
                    setLength(minLen);
                }
            }
            if ((oldLength - blockSize) > 0)
            {
                try
                {
                    this.realBlockSize =
                            reader.opaque().readArrayToBlockWithOffset(dataSetPath, block,
                                    realBlockSize, blockOffset, 0);
                } catch (HDF5Exception ex)
                {
                    throw new IOExceptionUnchecked(ex);
                }
            } else
            {
                Arrays.fill(block, (byte) 0);
            }
        }
    }

    private void ensureInitalizedForReading() throws IOExceptionUnchecked
    {
        if (realBlockSize < 0)
        {
            if (eof())
            {
                this.realBlockSize = 0;
            } else
            {
                try
                {
                    this.realBlockSize =
                            reader.opaque().readArrayToBlockWithOffset(dataSetPath, block, blockSize,
                                    blockOffset, 0);
                } catch (HDF5Exception ex)
                {
                    throw new IOExceptionUnchecked(ex);
                }
            }
        }
    }

    private void readBlock(long newBlockOffset) throws IOExceptionUnchecked
    {
        if (newBlockOffset != blockOffset)
        {
            flush();
            try
            {
                this.realBlockSize =
                        reader.opaque().readArrayToBlockWithOffset(dataSetPath, block, blockSize,
                                newBlockOffset, 0);
            } catch (HDF5Exception ex)
            {
                throw new IOExceptionUnchecked(ex);
            }
            this.blockOffset = newBlockOffset;
        }
    }

    private void readNextBlockResetPosition()
    {
        readBlock(blockOffset + realBlockSize);
        this.positionInBlock = 0;
    }

    private boolean eof()
    {
        return (available() == 0);
    }

    private void checkEoFOnWrite()
    {
        if (extendable == false && eof())
        {
            throw new IOExceptionUnchecked(new EOFException("Dataset is EOF and not extendable."));
        }
    }

    public File getHdf5File()
    {
        return reader.file().getFile();
    }

    public String getDataSetPath()
    {
        return dataSetPath;
    }

    /**
     * Returns <code>true</code> if the HDF5 file has been opened in read-only mode.
     */
    public boolean isReadOnly()
    {
        return (writerOrNull == null);
    }

    private void extend(int numberOfBytesToExtend) throws IOExceptionUnchecked
    {
        final long len = length();
        final long pos = getFilePointer();
        final long newLen = pos + numberOfBytesToExtend;
        if (newLen > len)
        {
            if (extendable == false)
            {
                throw new IOExceptionUnchecked("Unable to extend dataset from " + len + " to "
                        + newLen + ": dataset is not extenable.");
            }
            setLength(pos + numberOfBytesToExtend);
        }
    }

    private void checkWrite(int lenCurrentOp) throws IOExceptionUnchecked
    {
        ensureInitalizedForWriting(lenCurrentOp);
        checkWriteDoNotExtend();
        if (extensionPending)
        {
            setLength(blockOffset + positionInBlock);
        }
    }

    private void checkWriteDoNotExtend() throws IOExceptionUnchecked
    {
        if (isReadOnly())
        {
            throw new IOExceptionUnchecked("HDF5 dataset opened in read-only mode.");
        }
    }

    @Override
    public long getFilePointer() throws IOExceptionUnchecked
    {
        return blockOffset + positionInBlock;
    }

    @Override
    public int read() throws IOExceptionUnchecked
    {
        ensureInitalizedForReading();
        if (positionInBlock == realBlockSize)
        {
            if (eof())
            {
                return -1;
            }
            readNextBlockResetPosition();
            if (eof())
            {
                return -1;
            }
        }
        return block[positionInBlock++] & 0xff;
    }

    @Override
    public int read(byte[] b) throws IOExceptionUnchecked
    {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOExceptionUnchecked
    {
        ensureInitalizedForReading();
        int realLen = getRealLen(len);
        if (realLen == 0)
        {
            return -1;
        }
        int bytesLeft = realLen;
        int currentOff = off;
        while (bytesLeft > 0)
        {
            final int lenInBlock = Math.min(bytesLeft, bytesLeftInBlock());
            System.arraycopy(block, positionInBlock, b, currentOff, lenInBlock);
            positionInBlock += lenInBlock;
            currentOff += lenInBlock;
            bytesLeft -= lenInBlock;
            if (bytesLeft > 0)
            {
                readNextBlockResetPosition();
            }
        }
        return realLen;
    }

    private int bytesLeftInBlock()
    {
        return (realBlockSize - positionInBlock);
    }

    private int getRealLen(int len)
    {
        return Math.min(len, available());
    }

    private long getRealLen(long len)
    {
        return Math.min(len, available());
    }

    @Override
    public long skip(long n) throws IOExceptionUnchecked
    {
        final long realN = getRealLen(n);
        seek(getFilePointer() + realN);
        return realN;
    }

    @Override
    public int available()
    {
        return (int) Math.min(availableLong(), Integer.MAX_VALUE);
    }

    private long availableLong()
    {
        return length() - getFilePointer();
    }

    @Override
    public void close() throws IOExceptionUnchecked
    {
        flush();
        if (closeReaderOnCloseFile)
        {
            try
            {
                reader.close();
            } catch (HDF5Exception ex)
            {
                throw new IOExceptionUnchecked(ex);
            }
        } else if (writerOrNull != null)
        {
            writerOrNull.file().removeFlushable(this);
        }
    }

    @Override
    public void mark(int readlimit)
    {
        this.blockOffsetMark = blockOffset;
        this.positionInBlockMark = positionInBlock;
    }

    @Override
    public void reset() throws IOExceptionUnchecked
    {
        if (blockOffsetMark < 0)
        {
            throw new IOExceptionUnchecked(new IOException("Stream not marked."));
        }
        readBlock(blockOffsetMark);
        this.positionInBlock = positionInBlockMark;
    }

    @Override
    public boolean markSupported()
    {
        return true;
    }

    @Override
    public void flush() throws IOExceptionUnchecked
    {
        if (isReadOnly() == false && blockDirty)
        {
            try
            {
                if (opaqueTypeOrNull != null)
                {
                    writerOrNull.opaque().writeArrayBlockWithOffset(dataSetPath, opaqueTypeOrNull,
                            block, realBlockSize, blockOffset);
                } else
                {
                    writerOrNull.int8().writeArrayBlockWithOffset(dataSetPath, block,
                            realBlockSize, blockOffset);
                }
            } catch (HDF5Exception ex)
            {
                throw new IOExceptionUnchecked(ex);
            }
            blockDirty = false;
        }
    }

    @Override
    public void synchronize() throws IOExceptionUnchecked
    {
        if (writerOrNull != null)
        {
            flush();
            try
            {
                writerOrNull.file().flushSyncBlocking();
            } catch (HDF5Exception ex)
            {
                throw new IOExceptionUnchecked(ex);
            }
        }
    }

    @Override
    public ByteOrder getByteOrder()
    {
        return byteOrder == ch.systemsx.cisd.base.convert.NativeData.ByteOrder.BIG_ENDIAN ? ByteOrder.BIG_ENDIAN
                : ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    public void setByteOrder(ByteOrder byteOrder)
    {
        if (byteOrder == ByteOrder.BIG_ENDIAN)
        {
            this.byteOrder = ch.systemsx.cisd.base.convert.NativeData.ByteOrder.BIG_ENDIAN;
        } else
        {
            this.byteOrder = ch.systemsx.cisd.base.convert.NativeData.ByteOrder.LITTLE_ENDIAN;
        }
    }

    @Override
    public void seek(long pos) throws IOExceptionUnchecked
    {
        if (pos < 0)
        {
            throw new IOExceptionUnchecked("New position may not be negative.");
        }
        if (isReadOnly() && pos >= length())
        {
            throw new IOExceptionUnchecked(
                    "In read-only mode, new position may not be larger than file size.");
        }
        final long newBlockOffset = (pos / blockSize) * blockSize;
        this.positionInBlock = (int) (pos % blockSize);
        if (newBlockOffset < length())
        {
            readBlock(newBlockOffset);
        } else
        {
            this.blockOffset = newBlockOffset;
            this.realBlockSize = positionInBlock + 1;
        }
        if (pos >= length())
        {
            this.extensionPending = true;
        }
    }

    @Override
    public long length() throws IOExceptionUnchecked
    {
        return length;
    }

    @Override
    public void setLength(long newLength) throws IOExceptionUnchecked
    {
        checkWriteDoNotExtend();
        if (extendable == false)
        {
            throw new IOExceptionUnchecked("setLength() called on non-extendable dataset.");
        }
        try
        {
            writerOrNull.object().setDataSetSize(dataSetPath, newLength);
        } catch (HDF5Exception ex)
        {
            throw new IOExceptionUnchecked(ex);
        }
        length = newLength;
    }

    @Override
    public void readFully(byte[] b) throws IOExceptionUnchecked
    {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOExceptionUnchecked
    {
        final int bytesRead = read(b, off, len);
        if (bytesRead != len)
        {
            throw new IOExceptionUnchecked(new EOFException());
        }
    }

    @Override
    public int skipBytes(int n) throws IOExceptionUnchecked
    {
        return (int) skip(n);
    }

    @Override
    public boolean readBoolean() throws IOExceptionUnchecked
    {
        return readUnsignedByte() != 0;
    }

    @Override
    public byte readByte() throws IOExceptionUnchecked
    {
        return (byte) readUnsignedByte();
    }

    @Override
    public int readUnsignedByte() throws IOExceptionUnchecked
    {
        final int b = read();
        if (b < 0)
        {
            throw new IOExceptionUnchecked(new EOFException());
        }
        return b;
    }

    @Override
    public short readShort() throws IOExceptionUnchecked
    {
        final byte[] byteArr = new byte[NativeData.SHORT_SIZE];
        readFully(byteArr);
        return NativeData.byteToShort(byteArr, byteOrder)[0];
    }

    @Override
    public int readUnsignedShort() throws IOExceptionUnchecked
    {
        return readShort() & 0xffff;
    }

    @Override
    public char readChar() throws IOExceptionUnchecked
    {
        final byte[] byteArr = new byte[NativeData.CHAR_SIZE];
        readFully(byteArr);
        return NativeData.byteToChar(byteArr, byteOrder)[0];
    }

    @Override
    public int readInt() throws IOExceptionUnchecked
    {
        final byte[] byteArr = new byte[NativeData.INT_SIZE];
        readFully(byteArr);
        return NativeData.byteToInt(byteArr, byteOrder)[0];
    }

    @Override
    public long readLong() throws IOExceptionUnchecked
    {
        final byte[] byteArr = new byte[NativeData.LONG_SIZE];
        readFully(byteArr);
        return NativeData.byteToLong(byteArr, byteOrder)[0];
    }

    @Override
    public float readFloat() throws IOExceptionUnchecked
    {
        final byte[] byteArr = new byte[NativeData.FLOAT_SIZE];
        readFully(byteArr);
        return NativeData.byteToFloat(byteArr, byteOrder)[0];
    }

    @Override
    public double readDouble() throws IOExceptionUnchecked
    {
        final byte[] byteArr = new byte[NativeData.DOUBLE_SIZE];
        readFully(byteArr);
        return NativeData.byteToDouble(byteArr, byteOrder)[0];
    }

    @Override
    public String readLine() throws IOExceptionUnchecked
    {
        final StringBuilder builder = new StringBuilder();
        int b;
        boolean byteRead = false;
        while ((b = read()) >= 0)
        {
            byteRead = true;
            final char c = (char) b;
            if (c == '\r')
            {
                continue;
            }
            if (c == '\n')
            {
                break;
            }
            builder.append(c);
        }
        if (byteRead == false)
        {
            return null;
        } else
        {
            return builder.toString();
        }
    }

    @Override
    public String readUTF() throws IOExceptionUnchecked
    {
        try
        {
            final byte[] strBuf = new byte[readUnsignedShort()];
            readFully(strBuf);
            return new String(strBuf, "UTF-8");
        } catch (UnsupportedEncodingException ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    @Override
    public void write(int b) throws IOExceptionUnchecked
    {
        checkWrite(1);
        extend(1);
        if (positionInBlock == realBlockSize)
        {
            checkEoFOnWrite();
            readNextBlockResetPosition();
            checkEoFOnWrite();
        }
        block[positionInBlock++] = (byte) b;
        blockDirty = true;
    }

    @Override
    public void write(byte[] b) throws IOExceptionUnchecked
    {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOExceptionUnchecked
    {
        checkWrite(len);
        extend(len);
        int bytesLeft = len;
        int currentOff = off;
        while (bytesLeft > 0)
        {
            final int lenInBlock = Math.min(bytesLeft, bytesLeftInBlock());
            System.arraycopy(b, currentOff, block, positionInBlock, lenInBlock);
            blockDirty = true;
            positionInBlock += lenInBlock;
            currentOff += lenInBlock;
            bytesLeft -= lenInBlock;
            if (bytesLeft > 0)
            {
                readNextBlockResetPosition();
            }
        }
    }

    @Override
    public void writeBoolean(boolean v) throws IOExceptionUnchecked
    {
        write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOExceptionUnchecked
    {
        write(v);
    }

    @Override
    public void writeShort(int v) throws IOExceptionUnchecked
    {
        write(NativeData.shortToByte(new short[]
            { (short) v }, byteOrder));
    }

    @Override
    public void writeChar(int v) throws IOExceptionUnchecked
    {
        write(NativeData.charToByte(new char[]
            { (char) v }, byteOrder));
    }

    @Override
    public void writeInt(int v) throws IOExceptionUnchecked
    {
        write(NativeData.intToByte(new int[]
            { v }, byteOrder));
    }

    @Override
    public void writeLong(long v) throws IOExceptionUnchecked
    {
        write(NativeData.longToByte(new long[]
            { v }, byteOrder));
    }

    @Override
    public void writeFloat(float v) throws IOExceptionUnchecked
    {
        write(NativeData.floatToByte(new float[]
            { v }, byteOrder));
    }

    @Override
    public void writeDouble(double v) throws IOExceptionUnchecked
    {
        write(NativeData.doubleToByte(new double[]
            { v }, byteOrder));
    }

    @Override
    public void writeBytes(String s) throws IOExceptionUnchecked
    {
        for (int i = 0; i < s.length(); i++)
        {
            write((byte) s.charAt(i));
        }
    }

    @Override
    public void writeChars(String s) throws IOExceptionUnchecked
    {
        for (int i = 0; i < s.length(); i++)
        {
            final char v = s.charAt(i);
            write((byte) ((v >>> 8) & 0xFF));
            write((byte) ((v >>> 0) & 0xFF));
        }
    }

    @Override
    public void writeUTF(String str) throws IOExceptionUnchecked
    {
        try
        {
            final byte[] strBuf = str.getBytes("UTF-8");
            writeShort(strBuf.length);
            write(strBuf);
        } catch (UnsupportedEncodingException ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

}
