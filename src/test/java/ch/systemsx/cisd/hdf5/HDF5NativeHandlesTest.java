package ch.systemsx.cisd.hdf5;

import static org.testng.AssertJUnit.assertArrayEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5JavaException;

public class HDF5NativeHandlesTest
{
    @Test
    public void testRawHyperslabReadMatchesJHDF5() throws IOException
    {
        final File file = File.createTempFile("native-handles", ".h5");
        file.deleteOnExit();
        final short[] data = new short[4 * 5 * 6];
        for (int i = 0; i < data.length; ++i)
            data[i] = (short) (i * 3);
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            w.uint16().writeMDArray("/m", new ch.systemsx.cisd.base.mdarray.MDShortArray(data, new int[] { 4, 5, 6 }));
        }
        try (IHDF5Reader r = HDF5Factory.openForReading(file))
        {
            final long fileId = r.getFileId();
            final long dxpl = r.getDataTransferPropertyListId();
            final long dset = H5.H5Dopen(fileId, "/m", HDF5Constants.H5P_DEFAULT);
            final long fileSpace = H5.H5Dget_space(dset);
            final long[] start = { 1, 2, 3 };
            final long[] count = { 2, 2, 2 };
            H5.H5Sselect_hyperslab(fileSpace, HDF5Constants.H5S_SELECT_SET, start, null, count, null);
            final long memSpace = H5.H5Screate_simple(3, count, null);
            final short[] raw = new short[8];
            H5.H5Dread(dset, HDF5Constants.H5T_NATIVE_USHORT, memSpace, fileSpace, dxpl, raw);
            H5.H5Sclose(memSpace);
            H5.H5Sclose(fileSpace);
            H5.H5Dclose(dset);

            final short[] expected =
                    r.uint16().readMDArrayBlockWithOffset("/m", new int[] { 2, 2, 2 }, new long[] { 1, 2, 3 }).getAsFlatArray();
            assertArrayEquals(expected, raw);
        }
    }

    @Test
    public void testHandlesRejectedAfterClose() throws IOException
    {
        final File file = File.createTempFile("native-handles", ".h5");
        file.deleteOnExit();
        final IHDF5Writer w = HDF5Factory.open(file);
        assertTrue(w.getFileId() > 0);
        w.close();
        try
        {
            w.getFileId();
            fail();
        } catch (HDF5JavaException e)
        {
            // expected
        }
        try
        {
            w.getDataTransferPropertyListId();
            fail();
        } catch (HDF5JavaException e)
        {
            // expected
        }
        try
        {
            w.getDataSetCreationPropertyListId();
            fail();
        } catch (HDF5JavaException e)
        {
            // expected
        }
        try
        {
            w.getCompactDataSetCreationPropertyListId();
            fail();
        } catch (HDF5JavaException e)
        {
            // expected
        }
        try
        {
            w.getLinkCreationPropertyListId();
            fail();
        } catch (HDF5JavaException e)
        {
            // expected
        }
    }

    @Test
    public void testRawCreateWithExposedCreationPropertyLists() throws IOException
    {
        final File file = File.createTempFile("native-handles", ".h5");
        file.deleteOnExit();
        try (IHDF5Writer w = HDF5Factory.open(file))
        {
            final long fileId = w.getFileId();
            final long lcpl = w.getLinkCreationPropertyListId();
            final long space = H5.H5Screate_simple(1, new long[] { 3 }, null);
            // Intermediate groups /a/b are created thanks to the link creation list.
            final long d1 = H5.H5Dcreate(fileId, "/a/b/contiguous", HDF5Constants.H5T_NATIVE_INT32,
                    space, lcpl, w.getDataSetCreationPropertyListId(), HDF5Constants.H5P_DEFAULT);
            H5.H5Dclose(d1);
            final long d2 = H5.H5Dcreate(fileId, "/a/b/compact", HDF5Constants.H5T_NATIVE_INT32,
                    space, lcpl, w.getCompactDataSetCreationPropertyListId(),
                    HDF5Constants.H5P_DEFAULT);
            H5.H5Dwrite(d2, HDF5Constants.H5T_NATIVE_INT32, HDF5Constants.H5S_ALL,
                    HDF5Constants.H5S_ALL, w.getDataTransferPropertyListId(), new int[] { 1, 2, 3 });
            H5.H5Dclose(d2);
            H5.H5Sclose(space);
            assertTrue(w.isGroup("/a/b"));
            assertEquals(HDF5StorageLayout.CONTIGUOUS,
                    w.getDataSetInformation("/a/b/contiguous").getStorageLayout());
            assertEquals(HDF5StorageLayout.COMPACT,
                    w.getDataSetInformation("/a/b/compact").getStorageLayout());
            assertArrayEquals(new int[] { 1, 2, 3 }, w.int32().readArray("/a/b/compact"));
        }
    }
}
