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


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A class that holds relevant information about a data set.
 * 
 * @author Bernd Rinn
 */
public final class HDF5DataSetInformation
{
    private final HDF5DataTypeInformation typeInformation;

    private long[] dimensions;

    private long[] maxDimensions;

    private HDF5StorageLayout storageLayout = HDF5StorageLayout.NOT_APPLICABLE;

    private int[] chunkSizesOrNull;

    HDF5DataSetInformation(HDF5DataTypeInformation typeInformation,
            HDF5DataTypeVariant typeVariantOrNull)
    {
        this.typeInformation = typeInformation;
        if (typeVariantOrNull != null)
        {
            typeInformation.setTypeVariant(typeVariantOrNull);
        }
    }

    /**
     * Returns the data type information for the data set.
     */
    public HDF5DataTypeInformation getTypeInformation()
    {
        return typeInformation;
    }

    /**
     * Returns the data type variant of this data set, or <code>null</code>, if this data set is not
     * tagged with a type variant.
     */
    public HDF5DataTypeVariant tryGetTypeVariant()
    {
        return typeInformation.tryGetTypeVariant();
    }

    /**
     * Returns <code>true</code>, if the data set is a time stamp, or <code>false</code> otherwise.
     */
    public boolean isTimeStamp()
    {
        return typeInformation.isTimeStamp();
    }

    /**
     * Returns <code>true</code>, if the data set is a time duration, or <code>false</code>
     * otherwise.
     */
    public boolean isTimeDuration()
    {
        return typeInformation.isTimeDuration();
    }

    /**
     * Returns the time unit of the data set, if the data set is a time duration, or
     * <code>null</code> otherwise.
     */
    public HDF5TimeUnit tryGetTimeUnit()
    {
        return typeInformation.tryGetTimeUnit();
    }

    /**
     * Returns the array dimensions of the data set.
     */
    public long[] getDimensions()
    {
        return dimensions;
    }

    void setDimensions(long[] dimensions)
    {
        this.dimensions = dimensions;
    }

    /**
     * Returns the largest possible array dimensions of the data set.
     */
    public long[] getMaxDimensions()
    {
        return maxDimensions;
    }

    void setMaxDimensions(long[] maxDimensions)
    {
        this.maxDimensions = maxDimensions;
    }

    void setStorageLayout(HDF5StorageLayout storageLayout)
    {
        this.storageLayout = storageLayout;
    }

    /**
     * Returns the storage layout of the data set in the HDF5 file.
     */
    public HDF5StorageLayout getStorageLayout()
    {
        return storageLayout;
    }

    /**
     * Returns the chunk size in each array dimension of the data set, or <code>null</code>, if the
     * data set is not of {@link HDF5StorageLayout#CHUNKED}.
     */
    public int[] tryGetChunkSizes()
    {
        return chunkSizesOrNull;
    }

    void setChunkSizes(int[] chunkSizes)
    {
        this.chunkSizesOrNull = chunkSizes;
    }

    /**
     * Returns the rank (number of axis) of this data set.
     */
    public int getRank()
    {
        return dimensions.length;
    }

    /**
     * Returns <code>true</code>, if the rank of this data set is 0.
     */
    public boolean isScalar()
    {
        return dimensions.length == 0;
    }
    
    /**
     * Returns <code>true</code>, if this data set type has a sign anf <code>false</code> otherwise.
     */
    public boolean isSigned()
    {
        return typeInformation.isSigned();
    }

    /**
     * Returns the one-dimensional length of the multi-dimensional array defined by
     * <var>dimensions</var>.
     */
    private static long getLength(final long[] dimensions)
    {
        assert dimensions != null;

        if (dimensions.length == 0) // NULL data space needs to be treated differently
        {
            return 0;
        }
        long length = dimensions[0];
        for (int i = 1; i < dimensions.length; ++i)
        {
            length *= dimensions[i];
        }
        return length;
    }

    /**
     * Returns the total number of elements of this data set.
     */
    public long getNumberOfElements()
    {
        return getLength(dimensions);
    }

    /**
     * Returns the total size (in bytes) of this data set.
     */
    public long getSize()
    {
        return getLength(dimensions) * typeInformation.getElementSize();
    }

    //
    // Object
    //

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj instanceof HDF5DataSetInformation == false)
        {
            return false;
        }
        final HDF5DataSetInformation that = (HDF5DataSetInformation) obj;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(typeInformation, that.typeInformation);
        builder.append(dimensions, that.dimensions);
        builder.append(maxDimensions, that.maxDimensions);
        return builder.isEquals();
    }

    @Override
    public int hashCode()
    {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(typeInformation);
        builder.append(dimensions);
        builder.append(maxDimensions);
        return builder.toHashCode();
    }

    @Override
    public String toString()
    {
        return typeInformation.toString() + ":" + ArrayUtils.toString(dimensions);
    }
}
