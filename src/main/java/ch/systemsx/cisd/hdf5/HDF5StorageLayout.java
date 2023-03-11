package ch.systemsx.cisd.hdf5;

import hdf.hdf5lib.HDF5Constants;

/**
 * The storage layout of a data set in the HDF5 file. Not applicable for attributes.
 * 
 * @author Bernd Rinn
 */
public enum HDF5StorageLayout
{
    COMPACT(HDF5Constants.H5D_COMPACT), CONTIGUOUS(HDF5Constants.H5D_CONTIGUOUS), CHUNKED(
            HDF5Constants.H5D_CHUNKED), NOT_APPLICABLE(-1);

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