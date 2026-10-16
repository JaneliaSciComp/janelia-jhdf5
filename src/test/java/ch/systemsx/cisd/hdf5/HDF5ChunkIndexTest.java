package ch.systemsx.cisd.hdf5;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.testng.annotations.Test;

import ch.systemsx.cisd.base.mdarray.MDShortArray;
import hdf.hdf5lib.exceptions.HDF5JavaException;

public class HDF5ChunkIndexTest
{
    private static final int SIZE = 40;

    private static final int CHUNK = 16;

    /** 3 x 3 chunks, the last row and column of which are partial. */
    private static final int CHUNKS_PER_DIM = 3;

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

    private static File writeDataSet(final HDF5IntStorageFeatures features) throws IOException
    {
        final File file = File.createTempFile("chunk-index", ".h5");
        file.deleteOnExit();
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

    private static void assertGrid(final HDF5ChunkIndex index)
    {
        assertEquals(2, index.getRank());
        assertEquals(CHUNKS_PER_DIM * CHUNKS_PER_DIM, index.getNumberOfChunks());
        final Set<String> seen = new HashSet<>();
        for (int i = 0; i < index.getNumberOfChunks(); ++i)
        {
            final long[] offset = index.getOffset(i);
            assertEquals(offset[0], index.getOffset(i, 0));
            assertEquals(offset[1], index.getOffset(i, 1));
            assertEquals(0, offset[0] % CHUNK);
            assertEquals(0, offset[1] % CHUNK);
            assertTrue(offset[0] < SIZE && offset[1] < SIZE);
            assertTrue(seen.add(offset[0] + "," + offset[1]));
        }
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
                final Inflater inflater = new Inflater();
                inflater.setInput(raw);
                final byte[] decoded = new byte[CHUNK * CHUNK * 2];
                assertEquals(decoded.length, inflater.inflate(decoded));
                inflater.end();
                final byte[] expected = expectedChunkBytes((int) index.getOffset(i, 0),
                        (int) index.getOffset(i, 1));
                assertTrue("chunk " + i, java.util.Arrays.equals(expected, decoded));
            }
        }
    }

    @Test
    public void testUnwrittenChunksAreNotListed() throws IOException
    {
        final File file = File.createTempFile("chunk-index", ".h5");
        file.deleteOnExit();
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
        final File file = File.createTempFile("chunk-index", ".h5");
        file.deleteOnExit();
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
    public void testNotChunkedAndClosed() throws IOException
    {
        final File file = File.createTempFile("chunk-index", ".h5");
        file.deleteOnExit();
        final IHDF5Writer w = HDF5Factory.open(file);
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
        w.close();
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
