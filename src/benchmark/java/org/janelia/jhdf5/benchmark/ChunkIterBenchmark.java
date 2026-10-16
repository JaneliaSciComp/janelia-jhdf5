package org.janelia.jhdf5.benchmark;

import static hdf.hdf5lib.HDF5Constants.H5D_ALLOC_TIME_EARLY;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_TRUNC;
import static hdf.hdf5lib.HDF5Constants.H5F_LIBVER_EARLIEST;
import static hdf.hdf5lib.HDF5Constants.H5F_LIBVER_LATEST;
import static hdf.hdf5lib.HDF5Constants.H5F_LIBVER_V18;
import static hdf.hdf5lib.HDF5Constants.H5P_DATASET_CREATE;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5P_FILE_ACCESS;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_UINT8;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.bytedeco.hdf5.H5D_chunk_iter_op_t;
import org.bytedeco.hdf5.global.hdf5;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.Pointer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import ch.systemsx.cisd.hdf5.hdf5lib.HDF5NativeLibrary;
import hdf.hdf5lib.H5;

/**
 * Enumerating all chunks of a chunked dataset: {@code H5Dchunk_iter} (one call, one callback per
 * chunk) against {@code H5Dget_chunk_info} by index and {@code H5Dget_chunk_info_by_coord}, all
 * through the JavaCPP binding, since {@code hdf.hdf5lib.H5} has none of them.
 * <p>
 * Same setup as the Julia benchmarks in JuliaIO/HDF5.jl#1031: a 2-D {@code uint8} dataset with
 * 16x16 chunks, allocated early, in a file with a 4 KiB metadata block size. {@code size} is the
 * dataset's edge length, so it has {@code (size/16)^2} chunks: 4 for 32 up to 16384 for 2048.
 * {@code libver} selects the chunk index: {@code earliest} (bounds EARLIEST..V18) gives the v1
 * B-tree, {@code latest} the fixed array that HDF5 1.10+ uses for fixed-size datasets.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
public class ChunkIterBenchmark
{
    private static final int CHUNK = 16;

    @Param({ "32", "64", "128", "256", "512", "1024", "2048" })
    public int size;

    @Param({ "earliest", "latest" })
    public String libver;

    private File file;

    private long fileId = -1;

    private long dataSetId = -1;

    private int numberOfChunks;

    private LongPointer offset;

    private IntPointer filterMask;

    private LongPointer address;

    private LongPointer chunkSize;

    private ChunkCollector collector;

    /** Counts the chunks and sums their addresses and sizes, so the work can't be optimized away. */
    static final class ChunkCollector extends H5D_chunk_iter_op_t
    {
        long count;

        long sum;

        @Override
        public int call(LongPointer chunkOffset, int chunkFilterMask, long chunkAddress,
                long chunkBytes, Pointer opData)
        {
            ++count;
            sum += chunkAddress + chunkBytes;
            return hdf5.H5_ITER_CONT;
        }
    }

    @Setup(Level.Trial)
    public void setUp() throws Exception
    {
        HDF5NativeLibrary.load();
        file = File.createTempFile("chunk-iter-" + size + "-" + libver + "-", ".h5");
        final long fapl = H5.H5Pcreate(H5P_FILE_ACCESS);
        if ("earliest".equals(libver))
        {
            H5.H5Pset_libver_bounds(fapl, H5F_LIBVER_EARLIEST, H5F_LIBVER_V18);
        } else
        {
            H5.H5Pset_libver_bounds(fapl, H5F_LIBVER_LATEST, H5F_LIBVER_LATEST);
        }
        H5.H5Pset_meta_block_size(fapl, 4096);
        fileId = H5.H5Fcreate(file.getPath(), H5F_ACC_TRUNC, H5P_DEFAULT, fapl);
        H5.H5Pclose(fapl);

        final long dcpl = H5.H5Pcreate(H5P_DATASET_CREATE);
        H5.H5Pset_chunk(dcpl, 2, new long[] { CHUNK, CHUNK });
        H5.H5Pset_alloc_time(dcpl, H5D_ALLOC_TIME_EARLY);
        final long space = H5.H5Screate_simple(2, new long[] { size, size }, null);
        dataSetId = H5.H5Dcreate(fileId, "test", H5T_NATIVE_UINT8, space, H5P_DEFAULT, dcpl, H5P_DEFAULT);
        H5.H5Pclose(dcpl);
        H5.H5Sclose(space);
        final byte[] data = new byte[size * size];
        Arrays.fill(data, (byte) 2);
        H5.H5Dwrite(dataSetId, H5T_NATIVE_UINT8, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
        H5.H5Fflush(fileId, hdf.hdf5lib.HDF5Constants.H5F_SCOPE_GLOBAL);

        numberOfChunks = (size / CHUNK) * (size / CHUNK);
        final long[] reported = new long[1];
        hdf5.H5Dget_num_chunks(dataSetId, H5S_ALL, reported);
        if (reported[0] != numberOfChunks)
        {
            throw new IllegalStateException("Expected " + numberOfChunks + " chunks, HDF5 reports " + reported[0]);
        }
        offset = new LongPointer(2);
        filterMask = new IntPointer(1);
        address = new LongPointer(1);
        chunkSize = new LongPointer(1);
        collector = new ChunkCollector();
    }

    @TearDown(Level.Trial)
    public void tearDown()
    {
        collector.close();
        chunkSize.close();
        address.close();
        filterMask.close();
        offset.close();
        H5.H5Dclose(dataSetId);
        H5.H5Fclose(fileId);
        file.delete();
    }

    @Benchmark
    public long chunkIter()
    {
        collector.count = 0;
        collector.sum = 0;
        if (hdf5.H5Dchunk_iter(dataSetId, H5P_DEFAULT, collector, null) < 0)
        {
            throw new IllegalStateException("H5Dchunk_iter failed");
        }
        if (collector.count != numberOfChunks)
        {
            throw new IllegalStateException("Visited " + collector.count + " of " + numberOfChunks + " chunks");
        }
        return collector.sum;
    }

    @Benchmark
    public long getChunkInfoByIndex()
    {
        long sum = 0;
        for (long i = 0; i < numberOfChunks; ++i)
        {
            if (hdf5.H5Dget_chunk_info(dataSetId, H5S_ALL, i, offset, filterMask, address, chunkSize) < 0)
            {
                throw new IllegalStateException("H5Dget_chunk_info failed for chunk " + i);
            }
            sum += address.get() + chunkSize.get();
        }
        return sum;
    }

    @Benchmark
    public long getChunkInfoByCoord()
    {
        long sum = 0;
        for (int row = 0; row < size; row += CHUNK)
        {
            for (int col = 0; col < size; col += CHUNK)
            {
                offset.put(0, row).put(1, col);
                if (hdf5.H5Dget_chunk_info_by_coord(dataSetId, offset, filterMask, address, chunkSize) < 0)
                {
                    throw new IllegalStateException("H5Dget_chunk_info_by_coord failed at " + row + "," + col);
                }
                sum += address.get() + chunkSize.get();
            }
        }
        return sum;
    }
}
