/*
 * Copyright 2015 ETH Zuerich, SIS
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

package ch.systemsx.cisd.hdf5;

import static hdf.hdf5lib.H5.H5Pclose;
import static hdf.hdf5lib.H5.H5Sclose;

/**
 * An object to represent a template of an HDF5 data set.
 * <p>
 * <i>Close it after usage is finished as otherwise you will leak resources from the HDF5
 * library.</i>
 * <p>
 * Open the object using the method <code>createArrayTemplate()</code> from one of the primitive writers and use it
 * in that writer to specify the parameters of a new data set. As it caches HDF5 objects, it will speed up the creation
 * process for repeated creation of data sets with the same template parameters.
 * <p>
 * A typical pattern for using this class is:
 * <pre>
 *    try (final HDF5DataSetTemplate tmpl =
 *            writer.float32().createArrayTemplate(length, length, HDF5IntStorageFeatures.INT_CONTIGUOUS))
 *    {
 *        for (int i = 0; i < num; ++i)
 *        {
 *            writer.float32().writeArray("ds" + i, array[i], tmpl);
 *        }
 *    }
 * </pre>
 * Assigning the <code>HDF5DataSetTemplate</code> object in a <code>try()</code> block is a recommened practice to
 * ensure that the underlying HDF5 objects are properly closed at the end.
 * 
 * @author Bernd Rinn
 */
public class HDF5DataSetTemplate implements AutoCloseable
{
    private final HDF5StorageLayout layout;

    private final long[] dimensions;

    private final long[] maxDimensions;

    private final long dataSetCreationPropertyListId;

    private final boolean closeCreationPropertyListId;

    private final long storageDataTypeId;

    private long dataspaceId;

    HDF5DataSetTemplate(long dataspaceId, long dataSetCreationPropertyListId,
            boolean closeCreationPropertyListId, long storageDataTypeId, long[] dimensions, 
            long[] maxDimensions, HDF5StorageLayout layout)
    {
        this.dataspaceId = dataspaceId;
        this.dataSetCreationPropertyListId = dataSetCreationPropertyListId;
        this.closeCreationPropertyListId = closeCreationPropertyListId;
        this.storageDataTypeId = storageDataTypeId;
        this.dimensions = dimensions;
        this.maxDimensions = maxDimensions;
        this.layout = layout;
    }

    long getDataspaceId()
    {
        return dataspaceId;
    }

    long getDataSetCreationPropertyListId()
    {
        return dataSetCreationPropertyListId;
    }

    long getStorageDataTypeId()
    {
        return storageDataTypeId;
    }

    long[] getDimensions()
    {
        return dimensions;
    }

    long[] getMaxDimensions()
    {
        return maxDimensions;
    }

    HDF5StorageLayout getLayout()
    {
        return layout;
    }

    @Override
    public void close()
    {
        if (dataspaceId > 0)
        {
            H5Sclose(dataspaceId);
            dataspaceId = -1;
            if (closeCreationPropertyListId)
            {
                H5Pclose(dataSetCreationPropertyListId);
            }
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) dataspaceId;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        HDF5DataSetTemplate other = (HDF5DataSetTemplate) obj;
        if (dataspaceId != other.dataspaceId)
        {
            return false;
        }
        return true;
    }

}
