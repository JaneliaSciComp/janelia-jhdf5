package ch.systemsx.cisd.hdf5;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

/**
 * The filter plugins bundled with org.bytedeco:hdf5 have to be registered as soon as the library
 * is loaded, without the caller doing anything.
 */
public class HDF5PluginsTest
{
    private static final int BZIP2 = 307;

    private static final int LZF = 32000;

    private static final int BLOSC = 32001;

    private static final int LZ4 = 32004;

    private static final int BSHUF = 32008;

    private static final int ZFP = 32013;

    private static final int ZSTD = 32015;

    private static final int BLOSC2 = 32026;

    @Test
    public void testBundledFiltersAreAvailable() throws IOException
    {
        // Opening a file is what a user does first; this must be enough to register the plugins.
        final File file = File.createTempFile("plugins-avail", ".h5");
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            w.int32().write("/x", 1);
        } finally
        {
            file.delete();
        }
        for (int id : new int[] { BZIP2, LZF, BLOSC, LZ4, BSHUF, ZFP, ZSTD, BLOSC2 })
        {
            assertTrue("filter " + id + " is not available", H5.H5Zfilter_avail(id) > 0);
        }
    }

    @Test
    public void testRoundTripThroughPluginFilter() throws Exception
    {
        final File file = File.createTempFile("plugins-roundtrip", ".h5");
        final int n = 4096;
        final int[] data = new int[n];
        for (int i = 0; i < n; ++i)
            data[i] = i / 8;
        try
        {
            final long f = H5.H5Fcreate(file.getAbsolutePath(), HDF5Constants.H5F_ACC_TRUNC,
                    HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            final long space = H5.H5Screate_simple(1, new long[] { n }, null);
            final long dcpl = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
            H5.H5Pset_chunk(dcpl, 1, new long[] { n });
            H5.H5Pset_filter(dcpl, ZSTD, 0, 0, new int[0]);
            final long ds = H5.H5Dcreate(f, "d", HDF5Constants.H5T_NATIVE_INT, space,
                    HDF5Constants.H5P_DEFAULT, dcpl, HDF5Constants.H5P_DEFAULT);
            H5.H5Dwrite(ds, HDF5Constants.H5T_NATIVE_INT, HDF5Constants.H5S_ALL,
                    HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, data);
            assertTrue("data was not compressed", H5.H5Dget_storage_size(ds) < n * 4L);
            H5.H5Dclose(ds);
            H5.H5Pclose(dcpl);
            H5.H5Sclose(space);
            H5.H5Fclose(f);
            try (IHDF5Reader r = HDF5Factory.openForReading(file))
            {
                final int[] read = r.int32().readArray("/d");
                assertEquals(n, read.length);
                for (int i = 0; i < n; ++i)
                    assertEquals(data[i], read[i]);
            }
        } finally
        {
            file.delete();
        }
    }
}
