/*
 * Copyright 2007 - 2018 ETH Zuerich, SIS
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

import static hdf.hdf5lib.H5.H5Dclose;
import static hdf.hdf5lib.H5.H5Dget_type;
import static hdf.hdf5lib.H5.H5Sclose;
import static hdf.hdf5lib.H5.H5Scopy;
import static hdf.hdf5lib.H5.H5Screate_simple;
import static hdf.hdf5lib.H5.H5Tclose;

import java.util.Arrays;

import hdf.hdf5lib.H5;

/**
 * An object to represent an HDF5 data set.
 * <p>
 * <i>Close it after usage is finished as otherwise you will leak resources from the HDF5
 * library.</i>
 * <p>
 * Open the object using the method {@link IHDF5ObjectReadOnlyInfoProviderHandler#openDataSet(String)} and use it
 * in readers and writers instead of the data set path. As it caches HDF5 objects, it will speed up the access
 * for repeated access to the same data set.
 * <p>
 * A typical pattern for using this class is:
 * <pre>
 *    try (final HDF5DataSet ds = reader.object().openDataSet("/path/to/dataset"))
 *    {
 *        for (long bx = 0; bx < 8; ++bx)
 *        {
 *            final float[] dataRead =
 *                    reader.float32().readArrayBlock(ds, length, bx);
 *            ... work with dataRead ...
 *        }
 *    }
 * </pre>
 * Assigning the <code>HDF5DataSet</code> object in a <code>try()</code> block is a recommened practice to ensure that 
 * the underlying HDF5 object is properly closed at the end. 
 * 
 * @author Bernd Rinn
 */
public class HDF5DataSet implements AutoCloseable
{
    private final HDF5BaseReader baseReader;
    
    private final HDF5 h5;
    
    private final String dataSetPath;

    private final HDF5StorageLayout layout;

    private long dataSpaceId;
    
    private long[] maxDimensions;

    private long[] dimensions;

    private long dataSetId;
    
    private long[] memoryBlockDimensions;
    
    private long memorySpaceId;
    
    private long dataTypeId;
    
    private int fullRank;

    HDF5DataSet(HDF5BaseReader baseReader, String datasetPath, long dataSetId, long dataSpaceId, long[] dimensions,
            long[] maxDimensionsOrNull, HDF5StorageLayout layout, boolean ownDataSpaceId)
    {
        this.baseReader = baseReader;
        this.h5 = baseReader.h5;
        this.dataSetPath = datasetPath;
        this.dataSetId = dataSetId;
        if (ownDataSpaceId)
        {
            this.dataSpaceId = dataSpaceId;
        } else
        {
            this.dataSpaceId = H5Scopy(dataSpaceId);
        }
        this.maxDimensions = maxDimensionsOrNull;
        this.dimensions = dimensions;
        this.layout = layout;
        this.memoryBlockDimensions = null;
        this.memorySpaceId = -1;
        this.dataTypeId = -1;
        this.fullRank = -1;
    }

    /**
     * Returns the path of this data set.
     */
    public String getDataSetPath()
    {
        return dataSetPath;
    }

    long getDataSetId()
    {
        return dataSetId;
    }

    long getDataSpaceId()
    {
        H5.H5Sselect_all(dataSpaceId);
        return dataSpaceId;
    }
    
    long getMemorySpaceId(long[] memoryBlockDimensions)
    {
        if (false == Arrays.equals(this.memoryBlockDimensions, memoryBlockDimensions))
        {
            closeMemorySpaceId();
            this.memoryBlockDimensions = memoryBlockDimensions;
            this.memorySpaceId = H5Screate_simple(memoryBlockDimensions.length, memoryBlockDimensions, null);
        }
        H5.H5Sselect_all(memorySpaceId);
        return memorySpaceId;
    }

    long[] getDimensions()
    {
        return dimensions;
    }

    void setDimensions(long[] dimensions)
    {
        this.dimensions = dimensions;
    }

    long[] getMaxDimensions()
    {
        if (maxDimensions == null)
        {
            this.maxDimensions = h5.getDataSpaceMaxDimensions(dataSpaceId, dimensions.length);
        }
        return maxDimensions;
    }

    HDF5StorageLayout getLayout()
    {
        return layout;
    }
    
    int getRank()
    {
        return dimensions.length;
    }

    int getFullRank()
    {
        if (fullRank == -1)
        {
            this.fullRank = baseReader.getRank(dataSetPath);
        }
        return fullRank;
    }

    void extend(long[] requiredDimensions)
    {
        final long[] newDimensions = h5.computeNewDimensions(dimensions, requiredDimensions, false);
        if (false == Arrays.equals(dimensions, newDimensions))
        {
            closeDataSpaceId();
            h5.extendDataSet(this, newDimensions, false);
            this.dimensions = newDimensions;
            this.dataSpaceId = h5.getDataSpaceForDataSet(dataSetId, null);
        }
    }

    long getDataTypeId()
    {
        if (dataTypeId == -1)
        {
            this.dataTypeId = H5Dget_type(dataSetId);
        }
        return dataTypeId;
    }

    @Override
    public void close()
    {
        closeDataSetId();
        closeDataSpaceId();
        closeMemorySpaceId();
        closeDataTypeId();
    }

    private void closeDataTypeId()
    {
        if (dataTypeId > -1)
        {
            H5Tclose(dataTypeId);
            dataTypeId = -1;
        }
    }

    private void closeDataSetId()
    {
        if (dataSetId > 0)
        {
            H5Dclose(dataSetId);
            dataSetId = -1;
        }
    }

    private void closeDataSpaceId()
    {
        if (dataSpaceId > -1)
        {
            H5Sclose(dataSpaceId);
            dataSpaceId = -1;
        }
    }
    
    private void closeMemorySpaceId()
    {
        if (memorySpaceId > 0)
        {
            H5Sclose(memorySpaceId);
            memoryBlockDimensions = null;
            memorySpaceId = -1;
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dataSetPath == null) ? 0 : dataSetPath.hashCode());
        result = prime * result + (int) dataSetId;
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
        HDF5DataSet other = (HDF5DataSet) obj;
        if (dataSetPath == null)
        {
            if (other.dataSetPath != null)
            {
                return false;
            }
        } else if (!dataSetPath.equals(other.dataSetPath))
        {
            return false;
        }
        if (dataSetId != other.dataSetId)
        {
            return false;
        }
        return true;
    }

}
