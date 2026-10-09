package ch.systemsx.cisd.hdf5;

import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5NativeLibrary.intConstant;
import hdf.hdf5lib.HDF5Constants;

/**
 * The storage layout of a data set in the HDF5 file. Not applicable for attributes.
 * 
 * @author Bernd Rinn
 */
public enum HDF5StorageLayout
{
    COMPACT(intConstant(() -> HDF5Constants.H5D_COMPACT)), CONTIGUOUS(intConstant(() -> HDF5Constants.H5D_CONTIGUOUS)), CHUNKED(
            intConstant(() -> HDF5Constants.H5D_CHUNKED)), NOT_APPLICABLE(-1);

    private int id;

    private HDF5StorageLayout(int id)
    {
        this.id = id;
    }

    static HDF5StorageLayout fromId(int id) throws IllegalArgumentException
    {
        for (HDF5StorageLayout layout : values())
        {
            if (layout.id == id)
            {
                return layout;
            }
        }
        throw new IllegalArgumentException("Illegal layout id " + id);
    }
}