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

import static ch.systemsx.cisd.hdf5.io.HDF5IOAdapterFactory.asInputStream;
import static ch.systemsx.cisd.hdf5.io.HDF5IOAdapterFactory.asOutputStream;
import static ch.systemsx.cisd.hdf5.io.HDF5IOAdapterFactory.asRandomAccessFile;
import static ch.systemsx.cisd.hdf5.io.HDF5IOAdapterFactory.asRandomAccessFileReadOnly;
import static ch.systemsx.cisd.hdf5.io.HDF5IOAdapterFactory.asRandomAccessFileReadWrite;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.convert.NativeData;
import ch.systemsx.cisd.base.convert.NativeData.ByteOrder;
import ch.systemsx.cisd.base.io.AdapterIInputStreamToInputStream;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5StorageLayout;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

/**
 * Test cases for {@link HDF5DataSetRandomAccessFile}.
 * 
 * @author Bernd Rinn
 */
public class HDF5DataSetRandomAccessFileTest
{

    private static final File rootDirectory = new File("target", "unit-test-wd");

    private static final File workingDirectory = new File(rootDirectory,
            "hdf5-dataset-random-access-file-wd");

    @BeforeSuite
    public void init()
    {
        workingDirectory.mkdirs();
        assertTrue(workingDirectory.isDirectory());
        workingDirectory.deleteOnExit();
        rootDirectory.deleteOnExit();
    }

    @Override
    protected void finalize() throws Throwable
    {
        // Delete the working directory
        if (workingDirectory.exists() && workingDirectory.canWrite())
        {
            workingDirectory.delete();
        }
        // Delete root directory
        if (rootDirectory.exists() && rootDirectory.canWrite())
        {
            rootDirectory.delete();
        }

        super.finalize();
    }

    @Test
    public void testWriterOpenAfterRAFileClosed()
    {
        final File dataSetFile = new File(workingDirectory, "testReaderOpenAfterRAFileClosed.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final IHDF5Writer writer =
                HDF5FactoryProvider.get().open(dataSetFile);
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFile(writer, dataSetName);
        raFile.close();
        // Checks that reader is still open
        writer.exists("/");
        writer.close();
    }
    
    @Test
    public void testReadContiguousByteByByte()
    {
        final File dataSetFile = new File(workingDirectory, "testReadContiguousByteByByte.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[1000];
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).dontUseExtendableDataTypes()
                        .writer();
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CONTIGUOUS, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final byte[] arrayRead = new byte[referenceArray.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int b;
        int idx = 0;
        while ((b = raFile.read()) >= 0)
        {
            arrayRead[idx++] = (byte) b;
            assertEquals(referenceArray.length - idx, raFile.available());
        }
        assertEquals(referenceArray.length, idx);
        assertTrue(Objects.deepEquals(referenceArray, arrayRead));
        raFile.close();
    }

    @Test
    public void testReadEmptyDataSetByByte()
    {
        final File dataSetFile = new File(workingDirectory, "testReadEmptyDataSetByByte.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.opaque().writeArray(dataSetName, "FILE", new byte[0]);
        writer.close();
        
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        assertEquals(-1, raFile.read());
        raFile.close();

        final HDF5DataSetRandomAccessFile raFile2 =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        assertEquals(-1, raFile2.read(new byte[10]));
        assertEquals(-1, raFile2.read());
        raFile2.close();
    }
    
    @Test
    public void testReadChunkedByteByByte()
    {
        final File dataSetFile = new File(workingDirectory, "testReadChunkedByteByByte.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[1000];
        final int chunkSize = referenceArray.length / 10;
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArray.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final byte[] arrayRead = new byte[referenceArray.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int b;
        int idx = 0;
        while ((b = raFile.read()) >= 0)
        {
            arrayRead[idx++] = (byte) b;
            assertEquals(referenceArray.length - idx, raFile.available());
        }
        assertEquals(referenceArray.length, idx);
        assertTrue(Objects.deepEquals(referenceArray, arrayRead));
        raFile.close();
    }

    @Test
    public void testReadContiguousBlockwise()
    {
        final File dataSetFile = new File(workingDirectory, "testReadContiguousBlockwise.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[1000];
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).dontUseExtendableDataTypes()
                        .writer();
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CONTIGUOUS, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final byte[] arrayRead = new byte[referenceArray.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int idx = 0;
        int bsize = referenceArray.length / 10;
        int bytesRead;
        while ((bytesRead = raFile.read(arrayRead, idx, bsize)) >= 0)
        {
            idx += bytesRead;
            assertEquals(referenceArray.length - idx, raFile.available());
        }
        assertEquals(referenceArray.length, idx);
        assertTrue(Objects.deepEquals(referenceArray, arrayRead));
        raFile.close();
    }

    @Test
    public void testReadChunkedBlockwiseMatch()
    {
        final File dataSetFile = new File(workingDirectory, "testReadChunkedBlockwiseMatch.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[1000];
        final int chunkSize = referenceArray.length / 10;
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArray.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final byte[] arrayRead = new byte[referenceArray.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int idx = 0;
        int bsize = chunkSize;
        int bytesRead;
        while ((bytesRead = raFile.read(arrayRead, idx, bsize)) >= 0)
        {
            idx += bytesRead;
            assertEquals(referenceArray.length - idx, raFile.available());
        }
        assertEquals(referenceArray.length, idx);
        assertTrue(Objects.deepEquals(referenceArray, arrayRead));
        raFile.close();
    }

    @Test
    public void testReadChunkedBlockwiseMismatch()
    {
        final File dataSetFile = new File(workingDirectory, "testReadChunkedBlockwiseMismatch.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[1000];
        final int chunkSize = referenceArray.length / 10;
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArray.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final byte[] arrayRead = new byte[referenceArray.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int idx = 0;
        int bsize = chunkSize + 1;
        int bytesRead;
        while ((bytesRead = raFile.read(arrayRead, idx, bsize)) >= 0)
        {
            idx += bytesRead;
            assertEquals(referenceArray.length - idx, raFile.available());
        }
        assertEquals(referenceArray.length, idx);
        for (int i = 0; i < idx; ++i)
        {
            if (referenceArray[i] != arrayRead[i])
            {
                System.err.println("Mismatch " + i + ": " + referenceArray[i] + ":" + arrayRead[i]);
                break;
            }
        }
        assertTrue(Objects.deepEquals(referenceArray, arrayRead));
        raFile.close();
    }

    @Test
    public void testSkip()
    {
        final File dataSetFile = new File(workingDirectory, "testSkip.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[1000];
        final int chunkSize = referenceArray.length / 10;
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArray.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final byte[] arrayRead = new byte[referenceArray.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int idx = 0;
        int bsize = chunkSize + 1;
        int bytesRead = raFile.read(arrayRead, idx, bsize);
        assertEquals(bsize, bytesRead);
        final int skipSize = 73;
        assertEquals(referenceArray.length - bsize, raFile.available());
        assertEquals(skipSize, raFile.skip(skipSize));
        assertEquals(referenceArray.length - bsize - skipSize, raFile.available());
        assertEquals(skipSize, raFile.skip(skipSize));
        assertEquals(referenceArray.length - bsize - 2 * skipSize, raFile.available());
        assertEquals(referenceArray[bsize + 2 * skipSize], (byte) raFile.read());
        raFile.close();
    }

    @Test
    public void testMarkSupport()
    {
        final File dataSetFile = new File(workingDirectory, "testMarkSupport.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[1000];
        final int chunkSize = referenceArray.length / 10;
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArray.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final byte[] arrayRead = new byte[referenceArray.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int idx = 0;
        int bsize = chunkSize + 1;
        int bytesRead = raFile.read(arrayRead, idx, bsize);
        assertEquals(bsize, bytesRead);
        assertTrue(raFile.markSupported());
        long markedFilePointer = raFile.getFilePointer();
        raFile.mark(0);
        assertEquals(markedFilePointer, raFile.getFilePointer());
        byte markedByte = (byte) raFile.read();
        assertEquals(markedFilePointer + 1, raFile.getFilePointer());
        final int skipSize = 73;
        assertEquals(skipSize, raFile.skip(skipSize));
        assertEquals(markedFilePointer + 1 + skipSize, raFile.getFilePointer());
        raFile.reset();
        assertEquals(markedFilePointer, raFile.getFilePointer());
        assertEquals(markedByte, (byte) raFile.read());
        assertEquals(skipSize, raFile.skip(skipSize));
        assertEquals(skipSize, raFile.skip(skipSize));
        assertEquals(markedFilePointer + 1 + 2 * skipSize, raFile.getFilePointer());
        raFile.reset();
        assertEquals(markedFilePointer, raFile.getFilePointer());
        assertEquals(markedByte, (byte) raFile.read());
        assertEquals(skipSize, raFile.skip(skipSize));
        assertEquals(skipSize, raFile.skip(skipSize));
        assertEquals(skipSize, raFile.skip(skipSize));
        markedFilePointer = raFile.getFilePointer();
        raFile.mark(0);
        assertEquals(markedFilePointer, raFile.getFilePointer());
        markedByte = (byte) raFile.read();
        assertEquals(markedFilePointer + 1, raFile.getFilePointer());
        assertEquals(skipSize, raFile.skip(skipSize));
        assertEquals(markedFilePointer + 1 + skipSize, raFile.getFilePointer());
        raFile.reset();
        assertEquals(markedFilePointer, raFile.getFilePointer());
        assertEquals(markedByte, (byte) raFile.read());
        raFile.close();
    }

    @Test
    public void testWriteTwiceSmallBuffer() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testWriteTwiceSmallBuffer.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[10];
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFile(writer, dataSetName, HDF5GenericStorageFeatures.GENERIC_CHUNKED,
                        11, null);
        raFile.write(referenceArray);
        raFile.write(referenceArray);
        raFile.flush();
        final HDF5DataSetInformation dsInfo = writer.getDataSetInformation(dataSetName);
        assertEquals(HDF5StorageLayout.CHUNKED, dsInfo.getStorageLayout());
        assertEquals(referenceArray.length * 2, dsInfo.getSize());
        assertNull(dsInfo.getTypeInformation().tryGetOpaqueTag());
        final byte[] arrayRead = writer.int8().readArray(dataSetName);
        assertEquals(referenceArray.length * 2, arrayRead.length);
        for (int i = 0; i < referenceArray.length; ++i)
        {
            assertEquals(i, referenceArray[i], arrayRead[i]);
        }
        for (int i = 0; i < referenceArray.length; ++i)
        {
            assertEquals(i, referenceArray[i], arrayRead[referenceArray.length + i]);
        }
        writer.close();
        raFile.close();
    }

    @Test
    public void testWriteTwiceLargeBuffer() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testWriteTwiceLargeBuffer.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[10];
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFile(writer, dataSetName, HDF5GenericStorageFeatures.GENERIC_CHUNKED,
                        100, null);
        raFile.write(referenceArray);
        raFile.write(referenceArray);
        raFile.flush();
        final HDF5DataSetInformation dsInfo = writer.getDataSetInformation(dataSetName);
        assertEquals(HDF5StorageLayout.CHUNKED, dsInfo.getStorageLayout());
        assertEquals(referenceArray.length * 2, dsInfo.getSize());
        assertNull(dsInfo.getTypeInformation().tryGetOpaqueTag());
        final byte[] arrayRead = writer.int8().readArray(dataSetName);
        assertEquals(referenceArray.length * 2, arrayRead.length);
        for (int i = 0; i < referenceArray.length; ++i)
        {
            assertEquals(i, referenceArray[i], arrayRead[i]);
        }
        for (int i = 0; i < referenceArray.length; ++i)
        {
            assertEquals(i, referenceArray[i], arrayRead[referenceArray.length + i]);
        }
        writer.close();
        raFile.close();
    }

    @Test
    public void testCopyIOUtils() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testCopyIOUtils.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[10000];
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        final OutputStream ostream =
                asOutputStream(writer, dataSetName, HDF5GenericStorageFeatures.GENERIC_CHUNKED,
                        12000, null);
        IOUtils.copyLarge(new ByteArrayInputStream(referenceArray), ostream);
        ostream.flush();
        final HDF5DataSetInformation dsInfo = writer.getDataSetInformation(dataSetName);
        assertEquals(HDF5StorageLayout.CHUNKED, dsInfo.getStorageLayout());
        assertEquals(referenceArray.length, dsInfo.getSize());
        writer.close();
        final InputStream istream = asInputStream(dataSetFile, dataSetName);
        final byte[] arrayRead = IOUtils.toByteArray(istream);
        assertTrue(Objects.deepEquals(referenceArray, arrayRead));
        istream.close();
    }

    @Test
    public void testSeek() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testSeek.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[10000];
        final int chunkSize = referenceArray.length / 10;
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArray.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        byte[] arrayRead = IOUtils.toByteArray(new AdapterIInputStreamToInputStream(raFile));
        assertTrue(Objects.deepEquals(referenceArray, arrayRead));
        raFile.seek(0);
        arrayRead = IOUtils.toByteArray(new AdapterIInputStreamToInputStream(raFile));
        assertTrue(Objects.deepEquals(referenceArray, arrayRead));
        raFile.seek(1113);
        assertEquals(referenceArray[1113], (byte) raFile.read());
        raFile.close();
    }

    @Test
    public void testLength() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testLength.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final byte[] referenceArray = new byte[10000];
        final int chunkSize = referenceArray.length / 10;
        for (int i = 0; i < referenceArray.length; ++i)
        {
            referenceArray[i] = (byte) i;
        }
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArray.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArray);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        assertEquals(referenceArray.length, raFile.length());
        raFile.close();
    }

    @Test
    public void testReadChunkedShortBigEndian()
    {
        final File dataSetFile = new File(workingDirectory, "testReadChunkedShortBigEndian.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final short[] referenceArrayShort = new short[50000];
        for (int i = 0; i < referenceArrayShort.length; ++i)
        {
            referenceArrayShort[i] = (short) i;
        }
        final byte[] referenceArrayByte =
                NativeData.shortToByte(referenceArrayShort, ByteOrder.BIG_ENDIAN);
        final int chunkSize = referenceArrayByte.length / 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArrayByte.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArrayByte);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final short[] arrayRead = new short[referenceArrayShort.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int idx = 0;
        while (raFile.available() >= 2)
        {

            arrayRead[idx++] = raFile.readShort();
            assertEquals(referenceArrayByte.length - idx * 2, raFile.available());
        }
        assertEquals(referenceArrayByte.length, idx * 2);
        assertTrue(Objects.deepEquals(referenceArrayShort, arrayRead));
        raFile.close();
    }

    @Test
    public void testReadChunkedShortLittleEndian()
    {
        final File dataSetFile = new File(workingDirectory, "testReadChunkedShortLittleEndian.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final short[] referenceArrayShort = new short[50000];
        for (int i = 0; i < referenceArrayShort.length; ++i)
        {
            referenceArrayShort[i] = (short) i;
        }
        final byte[] referenceArrayByte =
                NativeData.shortToByte(referenceArrayShort, ByteOrder.LITTLE_ENDIAN);
        final int chunkSize = referenceArrayByte.length / 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArrayByte.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArrayByte);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final short[] arrayRead = new short[referenceArrayShort.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        raFile.setByteOrder(java.nio.ByteOrder.LITTLE_ENDIAN);
        int idx = 0;
        while (raFile.available() >= 2)
        {

            arrayRead[idx++] = raFile.readShort();
            assertEquals(referenceArrayByte.length - idx * 2, raFile.available());
        }
        assertEquals(referenceArrayByte.length, idx * 2);
        assertTrue(Objects.deepEquals(referenceArrayShort, arrayRead));
        raFile.close();
    }

    @Test
    public void testReadChunkedDoubleBigEndian()
    {
        final File dataSetFile = new File(workingDirectory, "testReadChunkedDoubleBigEndian.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final double[] referenceArrayDouble = new double[12500];
        for (int i = 0; i < referenceArrayDouble.length; ++i)
        {
            referenceArrayDouble[i] = i;
        }
        final byte[] referenceArrayByte =
                NativeData.doubleToByte(referenceArrayDouble, ByteOrder.BIG_ENDIAN);
        final int chunkSize = referenceArrayByte.length / 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArrayByte.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArrayByte);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final double[] arrayRead = new double[referenceArrayDouble.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        int idx = 0;
        while (raFile.available() >= 2)
        {

            arrayRead[idx++] = raFile.readDouble();
            assertEquals(referenceArrayByte.length - idx * 8, raFile.available());
        }
        assertEquals(referenceArrayByte.length, idx * 8);
        assertTrue(Objects.deepEquals(referenceArrayDouble, arrayRead));
        raFile.close();
    }

    @Test
    public void testReadChunkedDoubleLittleEndian()
    {
        final File dataSetFile = new File(workingDirectory, "testReadChunkedDoubleLittleEndian.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final double[] referenceArrayDouble = new double[12500];
        for (int i = 0; i < referenceArrayDouble.length; ++i)
        {
            referenceArrayDouble[i] = i;
        }
        final byte[] referenceArrayByte =
                NativeData.doubleToByte(referenceArrayDouble, ByteOrder.LITTLE_ENDIAN);
        final int chunkSize = referenceArrayByte.length / 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, referenceArrayByte.length, chunkSize);
        writer.int8().writeArray(dataSetName, referenceArrayByte);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final double[] arrayRead = new double[referenceArrayDouble.length];
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        raFile.setByteOrder(java.nio.ByteOrder.LITTLE_ENDIAN);
        int idx = 0;
        while (raFile.available() >= 2)
        {

            arrayRead[idx++] = raFile.readDouble();
            assertEquals(referenceArrayByte.length - idx * 8, raFile.available());
        }
        assertEquals(referenceArrayByte.length, idx * 8);
        assertTrue(Objects.deepEquals(referenceArrayDouble, arrayRead));
        raFile.close();
    }

    @Test
    public void testReadChunkedStringReadline() throws UnsupportedEncodingException
    {
        final File dataSetFile = new File(workingDirectory, "testReadChunkedStringReadline.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final String reference = "One\nTwo\nThree\r\nFour";
        final byte[] bytesReference = reference.getBytes("ASCII");
        final int chunkSize = 4;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, bytesReference.length, chunkSize);
        writer.int8().writeArray(dataSetName, bytesReference);
        assertEquals(HDF5StorageLayout.CHUNKED, writer.getDataSetInformation(dataSetName)
                .getStorageLayout());
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadOnly(dataSetFile, dataSetName);
        assertEquals("One", raFile.readLine());
        assertEquals("Two", raFile.readLine());
        assertEquals("Three", raFile.readLine());
        assertEquals("Four", raFile.readLine());
        assertEquals(0, raFile.available());
        raFile.close();
    }

    @Test
    public void testWriteByteByByte() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testWriteByteByByte.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final int chunkSize = 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, 0, chunkSize);
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadWrite(dataSetFile, dataSetName);
        raFile.mark(0);
        for (int i = 0; i < 256; ++i)
        {
            raFile.write(i);
        }
        raFile.reset();
        final byte[] arrayRead = IOUtils.toByteArray(new AdapterIInputStreamToInputStream(raFile));
        assertEquals(256, arrayRead.length);
        for (int i = 0; i < 256; ++i)
        {
            assertEquals(Integer.toString(i), (byte) i, arrayRead[i]);
        }

        raFile.close();
    }

    @Test
    public void testWriteByteBlock() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testWriteByteBlock.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final int chunkSize = 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, 0, chunkSize);
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadWrite(dataSetFile, dataSetName);
        raFile.mark(0);
        final byte[] arr = new byte[256];
        for (int i = 0; i < 256; ++i)
        {
            arr[i] = (byte) i;
        }
        raFile.write(arr);
        raFile.reset();
        final byte[] arrayRead = IOUtils.toByteArray(new AdapterIInputStreamToInputStream(raFile));
        assertEquals(256, arrayRead.length);
        for (int i = 0; i < 256; ++i)
        {
            assertEquals(Integer.toString(i), (byte) i, arrayRead[i]);
        }

        raFile.close();
    }

    @Test
    public void testWriteDouble() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testWriteDouble.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final int chunkSize = 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, 0, chunkSize);
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadWrite(dataSetFile, dataSetName);
        raFile.mark(0);
        for (int i = 0; i < 256; ++i)
        {
            raFile.writeDouble(i);
        }
        raFile.reset();
        for (int i = 0; i < 256; ++i)
        {
            assertEquals(Integer.toString(i), (double) i, raFile.readDouble());
        }
        assertEquals(0, raFile.available());
        raFile.close();
    }

    @Test
    public void testWriteBytesOfString() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testWriteBytesOfString.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final int chunkSize = 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, 0, chunkSize);
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadWrite(dataSetFile, dataSetName);
        raFile.mark(0);
        raFile.writeBytes("TestString\n");
        raFile.reset();
        assertEquals("TestString", raFile.readLine());
        raFile.close();
    }

    @Test
    public void testWriteStringUTF8() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testWriteStringUTF8.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final int chunkSize = 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, 0, chunkSize);
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadWrite(dataSetFile, dataSetName);
        raFile.mark(0);
        raFile.writeUTF("TestString\u1873");
        raFile.reset();
        assertEquals("TestString\u1873", raFile.readUTF());
        raFile.close();
    }

    @Test
    public void testPendingExtension() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testPendingExtension.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();

        final int chunkSize = 10;
        final IHDF5Writer writer =
                HDF5FactoryProvider.get().configure(dataSetFile).keepDataSetsIfTheyExist().writer();
        writer.int8().createArray(dataSetName, 0, chunkSize);
        writer.close();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadWrite(dataSetFile, dataSetName);
        raFile.seek(20);
        raFile.write(42);
        assertEquals(21, raFile.length());
        raFile.seek(0);
        final byte[] arrayRead = IOUtils.toByteArray(new AdapterIInputStreamToInputStream(raFile));
        assertEquals(42, arrayRead[20]);
        for (int i = 0; i < 20; ++i)
        {
            assertEquals("Position " + i, 0, arrayRead[0]);
        }
        raFile.close();
    }

    @Test
    public void testEmptyDatasetDefaultParameters() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testEmptyDatasetDefaultParameters.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFileReadWrite(dataSetFile, dataSetName);
        raFile.seek(20);
        raFile.close();
        final IHDF5Reader reader = HDF5FactoryProvider.get().openForReading(dataSetFile);
        assertTrue(reader.exists(dataSetName));
        final HDF5DataSetInformation info = reader.getDataSetInformation(dataSetName);
        assertEquals(0, info.getSize());
        assertEquals(HDF5StorageLayout.CHUNKED, info.getStorageLayout());
        assertEquals("FILE", info.getTypeInformation().tryGetOpaqueTag());
        final int[] chunkSizesOrNull = info.tryGetChunkSizes();
        assertNotNull(chunkSizesOrNull);
        assertEquals(1, chunkSizesOrNull.length);
        assertEquals(1024 * 1024, chunkSizesOrNull[0]);
        reader.close();
    }

    @Test
    public void testEmptyDatasetOpaqueSmallChunkSize() throws IOException
    {
        final File dataSetFile =
                new File(workingDirectory, "testEmptyDatasetOpaqueSmallChunkSize.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();
        final int chunkSize = 10 * 1024;
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFile(dataSetFile, dataSetName,
                        HDF5GenericStorageFeatures.GENERIC_CHUNKED, chunkSize, "FILE");
        raFile.seek(20);
        raFile.close();
        final IHDF5Reader reader = HDF5FactoryProvider.get().openForReading(dataSetFile);
        assertTrue(reader.exists(dataSetName));
        final HDF5DataSetInformation info = reader.getDataSetInformation(dataSetName);
        assertEquals(0, info.getSize());
        assertEquals(HDF5StorageLayout.CHUNKED, info.getStorageLayout());
        assertEquals("FILE", info.getTypeInformation().tryGetOpaqueTag());
        final int[] chunkSizesOrNull = info.tryGetChunkSizes();
        assertNotNull(chunkSizesOrNull);
        assertEquals(1, chunkSizesOrNull.length);
        assertEquals(chunkSize, chunkSizesOrNull[0]);
        reader.close();
    }

    @Test
    public void testEmptyDatasetContiguous() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testEmptyDatasetContiguous.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFile(dataSetFile, dataSetName,
                        HDF5GenericStorageFeatures.GENERIC_CONTIGUOUS, 1024, null);
        raFile.seek(20);
        raFile.close();
        final IHDF5Reader reader = HDF5FactoryProvider.get().openForReading(dataSetFile);
        assertTrue(reader.exists(dataSetName));
        final HDF5DataSetInformation info = reader.getDataSetInformation(dataSetName);
        assertEquals(1024, info.getSize());
        assertEquals(HDF5StorageLayout.CONTIGUOUS, info.getStorageLayout());
        assertNull(info.getTypeInformation().tryGetOpaqueTag());
        final int[] chunkSizesOrNull = info.tryGetChunkSizes();
        assertNull(chunkSizesOrNull);
        reader.close();
    }

    @Test
    public void testEmptyDatasetCompact() throws IOException
    {
        final File dataSetFile = new File(workingDirectory, "testEmptyDatasetCompact.h5");
        final String dataSetName = "ds";
        dataSetFile.delete();
        assertFalse(dataSetFile.exists());
        dataSetFile.deleteOnExit();
        final HDF5DataSetRandomAccessFile raFile =
                asRandomAccessFile(dataSetFile, dataSetName,
                        HDF5GenericStorageFeatures.GENERIC_COMPACT, 1024, null);
        raFile.seek(20);
        raFile.close();
        final IHDF5Reader reader = HDF5FactoryProvider.get().openForReading(dataSetFile);
        assertTrue(reader.exists(dataSetName));
        final HDF5DataSetInformation info = reader.getDataSetInformation(dataSetName);
        assertEquals(1024, info.getSize());
        assertEquals(HDF5StorageLayout.COMPACT, info.getStorageLayout());
        assertNull(info.getTypeInformation().tryGetOpaqueTag());
        final int[] chunkSizesOrNull = info.tryGetChunkSizes();
        assertNull(chunkSizesOrNull);
        reader.close();
    }
}
