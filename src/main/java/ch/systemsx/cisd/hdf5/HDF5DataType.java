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

package ch.systemsx.cisd.hdf5;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;

/**
 * The abstract base class of Java wrappers for HDF data types.
 * 
 * @author Bernd Rinn
 */
public abstract class HDF5DataType
{

    private long fileId;

    private long storageTypeId;

    private long nativeTypeId;

    private final HDF5BaseReader baseReader;

    HDF5DataType(long fileId, long storageTypeId, long nativeTypeId, HDF5BaseReader baseReader)
    {
        assert fileId >= 0;

        this.fileId = fileId;
        this.storageTypeId = storageTypeId;
        this.nativeTypeId = nativeTypeId;
        this.baseReader = baseReader;
        baseReader.fileRegistry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    HDF5DataType.this.fileId = -1;
                    HDF5DataType.this.storageTypeId = -1;
                    HDF5DataType.this.nativeTypeId = -1;
                }
            });
    }

    /**
     * Returns the storage data type id of this type.
     */
    long getStorageTypeId()
    {
        return storageTypeId;
    }

    /**
     * Returns the native data type id of this type.
     */
    long getNativeTypeId()
    {
        return nativeTypeId;
    }

    /**
     * Checks whether this type is for file <var>expectedFileId</var>.
     * 
     * @throws HDF5JavaException If this type is not for file <var>expectedFileId</var>.
     */
    void check(final long expectedFileId) throws HDF5JavaException
    {
        if (fileId < 0)
        {
            throw new HDF5JavaException("Type " + getName() + " is closed.");
        }
        if (fileId != expectedFileId)
        {
            throw new HDF5JavaException("Type " + getName() + " is not from this file.");
        }
    }

    /**
     * Checks whether this type is open.
     * 
     * @throws HDF5JavaException If this type is not open.
     */
    void checkOpen() throws HDF5JavaException
    {
        if (fileId < 0)
        {
            throw new HDF5JavaException("Type " + getName() + " is closed.");
        }
    }

    /**
     * Returns a name for this type, or <code>null</code> if this type has no name.
     */
    public abstract String tryGetName();

    /**
     * Returns a name for this type, or <code>UNKNOWN</code if this type has no name.
     */
    public String getName()
    {
        final String nameOrNull = tryGetName();
        return (nameOrNull == null) ? "UNKNOWN" : nameOrNull;
    }

    /**
     * Returns the data type path of this type, or <code>null</code>, if this type is not a comitted
     * data type.
     */
    public String tryGetDataTypePath()
    {
        return getDataTypeInformation(DataTypeInfoOptions.PATH).tryGetDataTypePath();
    }

    /**
     * Returns the data type information for this data type.
     * 
     * @param dataTypeInfoOptions The options that decide how much information to fetch.
     */
    public HDF5DataTypeInformation getDataTypeInformation(
            final DataTypeInfoOptions dataTypeInfoOptions)
    {
        return baseReader.getDataTypeInformation(storageTypeId, dataTypeInfoOptions);
    }

    /**
     * Returns the data type information (with {@link DataTypeInfoOptions#DEFAULT}) for this data
     * type.
     */
    public HDF5DataTypeInformation getDataTypeInformation()
    {
        return baseReader.getDataTypeInformation(storageTypeId, DataTypeInfoOptions.DEFAULT);
    }

    //
    // Object
    //

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)fileId;
        result = prime * result + (int)storageTypeId;
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
        HDF5DataType other = (HDF5DataType) obj;
        if (fileId != other.fileId)
        {
            return false;
        }
        if (storageTypeId != other.storageTypeId)
        {
            return false;
        }
        return true;
    }

}
