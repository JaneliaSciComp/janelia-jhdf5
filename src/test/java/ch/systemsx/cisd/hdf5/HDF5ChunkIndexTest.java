package ch.systemsx.cisd.hdf5;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.bytedeco.javacpp.LongPointer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.mdarray.MDShortArray;
import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5JavaException;

public class HDF5ChunkIndexTest
{
    private static final int SIZE = 40;

    private static final int CHUNK = 16;

    /** 3 x 3 chunks, the last row and column of which are partial. */
    private static final int CHUNKS_PER_DIM = 3;

    private final List<File> tempFiles = new ArrayList<>();

    @AfterMethod(alwaysRun = true)
    public void deleteTempFiles()
    {
        for (final File file : tempFiles)
        {
            file.delete();
        }
        tempFiles.clear();
    }

    private File tempFile() throws IOException
    {
        final File file = File.createTempFile("chunk-index", ".h5");
        tempFiles.add(file);
        return file;
    }

    private static short value(final int row, final int col)
    {
        return (short) (row * 1000 + col);
    }

    private static MDShortArray block(final int row0, final int col0, final int rows, final int cols)
    {
        final short[] data = new short[rows * cols];
        for (int r = 0; r < rows; ++r)
            for (int c = 0; c < cols; ++c)
                data[r * cols + c] = value(row0 + r, col0 + c);
        return new MDShortArray(data, new int[] { rows, cols });
    }

    private File writeDataSet(final HDF5IntStorageFeatures features) throws IOException
    {
        final File file = tempFile();
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            w.int16().createMDArray("/d", new long[] { SIZE, SIZE }, new int[] { CHUNK, CHUNK },
                    features);
            w.int16().writeMDArrayBlockWithOffset("/d", block(0, 0, SIZE, SIZE),
                    new long[] { 0, 0 });
        }
        return file;
    }

    /** The bytes of the chunk at (row0, col0) as stored (chunks are always whole, even at edges). */
    private static byte[] expectedChunkBytes(final int row0, final int col0)
    {
        final ByteBuffer bb = ByteBuffer.allocate(CHUNK * CHUNK * 2).order(ByteOrder.nativeOrder());
        for (int r = 0; r < CHUNK; ++r)
            for (int c = 0; c < CHUNK; ++c)
            {
                final boolean inside = row0 + r < SIZE && col0 + c < SIZE;
                bb.putShort(inside ? value(row0 + r, col0 + c) : 0);
            }
        return bb.array();
    }

    private static byte[] readFile(final File file, final long address, final long size)
            throws IOException
    {
        final byte[] bytes = new byte[(int) size];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            raf.seek(address);
            raf.readFully(bytes);
        }
        return bytes;
    }

    /**
     * Asserts that <var>index</var> lists every chunk of a 2-d data set of <var>rows</var> x
     * <var>cols</var> elements with chunks of <var>chunkRows</var> x <var>chunkCols</var> elements,
     * each exactly once, with the offsets in the order of the data set's dimensions.
     */
    private static void assertGrid(final HDF5ChunkIndex index, final int rows, final int cols,
            final int chunkRows, final int chunkCols)
    {
        final int chunksInRows = (rows + chunkRows - 1) / chunkRows;
        final int chunksInCols = (cols + chunkCols - 1) / chunkCols;
        assertEquals(2, index.getRank());
        assertEquals(chunksInRows * chunksInCols, index.getNumberOfChunks());
        final Set<String> seen = new HashSet<>();
        for (int i = 0; i < index.getNumberOfChunks(); ++i)
        {
            final long[] offset = index.getOffset(i);
            assertEquals(offset[0], index.getOffset(i, 0));
            assertEquals(offset[1], index.getOffset(i, 1));
            assertEquals(0, offset[0] % chunkRows);
            assertEquals(0, offset[1] % chunkCols);
            assertTrue(offset[0] < rows && offset[1] < cols);
            assertTrue(seen.add(offset[0] + "," + offset[1]));
        }
    }

    private static void assertGrid(final HDF5ChunkIndex index)
    {
        assertGrid(index, SIZE, SIZE, CHUNK, CHUNK);
        assertEquals(CHUNKS_PER_DIM * CHUNKS_PER_DIM, index.getNumberOfChunks());
    }

    @Test
    public void testUncompressedChunksCanBeReadFromTheFile() throws IOException
    {
        final File file = writeDataSet(HDF5IntStorageFeatures.INT_NO_COMPRESSION);
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            final HDF5ChunkIndex index = r.object().getChunkIndex("/d");
            assertGrid(index);
            for (int i = 0; i < index.getNumberOfChunks(); ++i)
            {
                assertEquals(0, index.getFilterMask(i));
                assertEquals(CHUNK * CHUNK * 2, index.getSize(i));
                final byte[] raw = readFile(file, index.getAddress(i), index.getSize(i));
                final byte[] expected = expectedChunkBytes((int) index.getOffset(i, 0),
                        (int) index.getOffset(i, 1));
                assertTrue("chunk " + i, java.util.Arrays.equals(expected, raw));
            }
        }
    }

    @Test
    public void testDeflatedChunksCanBeInflatedFromTheFile() throws IOException,
            DataFormatException
    {
        final File file = writeDataSet(HDF5IntStorageFeatures.INT_DEFLATE);
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            final HDF5ChunkIndex index = r.object().getChunkIndex("/d");
            assertGrid(index);
            for (int i = 0; i < index.getNumberOfChunks(); ++i)
            {
                final byte[] raw = readFile(file, index.getAddress(i), index.getSize(i));
                final byte[] decoded = new byte[CHUNK * CHUNK * 2];
                final Inflater inflater = new Inflater();
                try
                {
                    inflater.setInput(raw);
                    assertEquals(decoded.length, inflater.inflate(decoded));
                } finally
                {
                    inflater.end();
                }
                final byte[] expected = expectedChunkBytes((int) index.getOffset(i, 0),
                        (int) index.getOffset(i, 1));
                assertTrue("chunk " + i, java.util.Arrays.equals(expected, decoded));
            }
        }
    }

    /** Rows and columns differ, so swapped dimensions would show. */
    @Test
    public void testOffsetsAreInTheOrderOfTheDataSetDimensions() throws IOException
    {
        final int rows = 50;
        final int cols = 20;
        final File file = tempFile();
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            w.int16().createMDArray("/d", new long[] { rows, cols }, new int[] { 16, 8 },
                    HDF5IntStorageFeatures.INT_NO_COMPRESSION);
            w.int16().writeMDArrayBlockWithOffset("/d", block(0, 0, rows, cols),
                    new long[] { 0, 0 });
        }
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            assertEquals(rows, r.object().getDataSetInformation("/d").getDimensions()[0]);
            final HDF5ChunkIndex index = r.object().getChunkIndex("/d");
            assertGrid(index, rows, cols, 16, 8);
            assertEquals(12, index.getNumberOfChunks());
        }
    }

    @Test
    public void testExtendibleDataSet() throws IOException
    {
        final File file = tempFile();
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            // Extendible: the dimensions given are the chunk size, the data set starts empty.
            w.int16().createMDArray("/d", new int[] { CHUNK, CHUNK },
                    HDF5IntStorageFeatures.INT_NO_COMPRESSION);
            w.object().setDataSetDimensions("/d", new long[] { SIZE, SIZE });
            w.int16().writeMDArrayBlockWithOffset("/d", block(0, 0, SIZE, SIZE),
                    new long[] { 0, 0 });
        }
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            assertGrid(r.object().getChunkIndex("/d"));
        }
    }

    /** With early space allocation every chunk is allocated, written or not. */
    @Test
    public void testEarlyAllocationListsUnwrittenChunks() throws IOException
    {
        final File file = tempFile();
        long fileId = -1;
        long space = -1;
        long dcpl = -1;
        long dataSet = -1;
        try
        {
            fileId = H5.H5Fcreate(file.getPath(), HDF5Constants.H5F_ACC_TRUNC,
                    HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            space = H5.H5Screate_simple(2, new long[] { SIZE, SIZE }, null);
            dcpl = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
            H5.H5Pset_chunk(dcpl, 2, new long[] { CHUNK, CHUNK });
            H5.H5Pset_alloc_time(dcpl, HDF5Constants.H5D_ALLOC_TIME_EARLY);
            dataSet = H5.H5Dcreate(fileId, "/d", HDF5Constants.H5T_NATIVE_INT16, space,
                    HDF5Constants.H5P_DEFAULT, dcpl, HDF5Constants.H5P_DEFAULT);
        } finally
        {
            if (dataSet >= 0)
                H5.H5Dclose(dataSet);
            if (dcpl >= 0)
                H5.H5Pclose(dcpl);
            if (space >= 0)
                H5.H5Sclose(space);
            if (fileId >= 0)
                H5.H5Fclose(fileId);
        }
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            assertGrid(r.object().getChunkIndex("/d"));
        }
    }

    @Test
    public void testManyChunks() throws IOException
    {
        final int size = 1000;
        final int chunk = 10;
        final File file = tempFile();
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            w.int16().createMDArray("/d", new long[] { size, size }, new int[] { chunk, chunk },
                    HDF5IntStorageFeatures.INT_NO_COMPRESSION);
            w.int16().writeMDArrayBlockWithOffset("/d", block(0, 0, size, size),
                    new long[] { 0, 0 });
        }
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            assertGrid(r.object().getChunkIndex("/d"), size, size, chunk, chunk);
        }
    }

    @Test
    public void testUnwrittenChunksAreNotListed() throws IOException
    {
        final File file = tempFile();
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            w.int16().createMDArray("/d", new long[] { SIZE, SIZE }, new int[] { CHUNK, CHUNK },
                    HDF5IntStorageFeatures.INT_NO_COMPRESSION);
            w.int16().writeMDArrayBlockWithOffset("/d", block(CHUNK, 2 * CHUNK, CHUNK, 8),
                    new long[] { CHUNK, 2 * CHUNK });
        }
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            final HDF5ChunkIndex index = r.object().getChunkIndex("/d");
            assertEquals(1, index.getNumberOfChunks());
            assertEquals(CHUNK, index.getOffset(0, 0));
            assertEquals(2 * CHUNK, index.getOffset(0, 1));
        }
    }

    @Test
    public void testEmptyChunkedDataSetHasEmptyIndex() throws IOException
    {
        final File file = tempFile();
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            w.int16().createMDArray("/d", new long[] { SIZE, SIZE }, new int[] { CHUNK, CHUNK },
                    HDF5IntStorageFeatures.INT_NO_COMPRESSION);
        }
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            assertEquals(0, r.object().getChunkIndex("/d").getNumberOfChunks());
        }
    }

    @Test
    public void testIndexOutOfRangeIsRejected() throws IOException
    {
        final File file = writeDataSet(HDF5IntStorageFeatures.INT_NO_COMPRESSION);
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            final HDF5ChunkIndex index = r.object().getChunkIndex("/d");
            final int n = index.getNumberOfChunks();
            for (final int bad : new int[] { -1, n, n + 1 })
            {
                assertOutOfBounds(() -> index.getOffset(bad));
                assertOutOfBounds(() -> index.getOffset(bad, 0));
                assertOutOfBounds(() -> index.getFilterMask(bad));
                assertOutOfBounds(() -> index.getAddress(bad));
                assertOutOfBounds(() -> index.getSize(bad));
            }
            assertOutOfBounds(() -> index.getOffset(0, -1));
            assertOutOfBounds(() -> index.getOffset(0, index.getRank()));
        }
    }

    private static void assertOutOfBounds(final Runnable call)
    {
        try
        {
            call.run();
        } catch (IndexOutOfBoundsException e)
        {
            return;
        }
        fail("IndexOutOfBoundsException expected");
    }

    @Test
    public void testEquality() throws IOException
    {
        final File file = writeDataSet(HDF5IntStorageFeatures.INT_NO_COMPRESSION);
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            final HDF5ChunkIndex index = r.object().getChunkIndex("/d");
            final HDF5ChunkIndex again = r.object().getChunkIndex("/d");
            assertEquals(index, again);
            assertEquals(index.hashCode(), again.hashCode());
            assertFalse(index.equals(null));
            assertFalse(index.equals("not an index"));
            assertFalse(index.equals(new HDF5ChunkIndex(2, new long[0], new int[0], new long[0],
                    new long[0])));
        }
    }

    @Test
    public void testInconsistentArraysAreRejected()
    {
        try
        {
            new HDF5ChunkIndex(2, new long[] { 0, 0, 16 }, new int[1], new long[1], new long[1]);
            fail();
        } catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    /** The collector grows its arrays if H5Dchunk_iter reports more chunks than expected. */
    @Test
    public void testCollectorGrowsBeyondTheExpectedNumberOfChunks()
    {
        final HDF5.ChunkCollector collector = new HDF5.ChunkCollector(2, 0);
        final int n = 100;
        for (int i = 0; i < n; ++i)
        {
            final LongPointer offset = new LongPointer(new long[] { i * 16L, i * 8L });
            try
            {
                collector.call(offset, i, 1000L + i, 50L + i, null);
            } finally
            {
                offset.close();
            }
        }
        collector.rethrowFailure();
        final HDF5ChunkIndex index = collector.toChunkIndex();
        assertEquals(n, index.getNumberOfChunks());
        for (int i = 0; i < n; ++i)
        {
            assertEquals(i * 16L, index.getOffset(i, 0));
            assertEquals(i * 8L, index.getOffset(i, 1));
            assertEquals(i, index.getFilterMask(i));
            assertEquals(1000L + i, index.getAddress(i));
            assertEquals(50L + i, index.getSize(i));
        }
    }

    /** An exception in the callback stops the iteration and is not lost. */
    @Test
    public void testCollectorReportsAFailureInTheCallback()
    {
        final HDF5.ChunkCollector collector = new HDF5.ChunkCollector(2, 4);
        final int status = collector.call(null, 0, 0L, 0L, null);
        assertTrue("iteration not stopped", status != org.bytedeco.hdf5.global.hdf5.H5_ITER_CONT);
        try
        {
            collector.rethrowFailure();
            fail();
        } catch (NullPointerException e)
        {
            // expected
        }
    }

    @Test
    public void testNotChunkedAndClosed() throws IOException
    {
        final File file = tempFile();
        final IHDF5Writer w = HDF5Factory.open(file);
        try
        {
            w.int16().writeArray("/contiguous", new short[] { 1, 2, 3 },
                    HDF5IntStorageFeatures.INT_CONTIGUOUS);
            try
            {
                w.object().getChunkIndex("/contiguous");
                fail();
            } catch (HDF5JavaException e)
            {
                assertTrue(e.getMessage(), e.getMessage().contains("not chunked"));
            }
        } finally
        {
            w.close();
        }
        try
        {
            w.object().getChunkIndex("/contiguous");
            fail();
        } catch (HDF5JavaException e)
        {
            // expected
        }
    }
}
