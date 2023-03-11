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

import ch.systemsx.cisd.hdf5.exceptions.HDF5SpaceRankMismatch;
import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * A parameter class to specify the block and slice to read from or write to a multi-dimensional array. 
 */
public final class HDF5ArrayBlockParams
{
    int[] blockDimensions;
    
    long[] blockIndex;
    
    long[] blockOffset;
    
    long[] boundIndexArray;
    
    IndexMap boundIndexMap;
    
    HDF5ArrayBlockParams()
    {
        // Only HDF5ArrayBlockParamsBuilder can instantiate this class.
    }

    int[] getBlockDimensions()
    {
        return blockDimensions;
    }

    long[] getOffset(int[] blockDimensions)
    {
        if (blockOffset == null)
        {
            if (blockDimensions == null)
            {
                throw new HDF5JavaException("No block dimensions set");
            }
            blockOffset = new long[blockDimensions.length];
            if (blockIndex != null)
            {
                if (blockDimensions.length != blockIndex.length)
                {
                    throw new HDF5SpaceRankMismatch(blockDimensions.length, blockIndex.length);
                }
                for (int i = 0; i < blockOffset.length; ++i)
                {
                    blockOffset[i] = blockIndex[i] * blockDimensions[i];
                }
            }
        }
        return blockOffset;
    }
    
    long[] getOffset()
    {
        return getOffset(blockDimensions);
    }

    long[] getBoundIndexArray()
    {
        return boundIndexArray;
    }

    IndexMap getBoundIndexMap()
    {
        return boundIndexMap;
    }
    
    boolean hasBlock()
    {
        return blockDimensions != null || blockIndex != null || blockOffset != null;
    }
    
    boolean hasSlice()
    {
        return boundIndexArray != null || boundIndexMap != null;
    }

    //
    // Public interface
    //
    
    /**
     * Sets an array block dimensions.
     * 
     * @param dimensions The block dimensions.
     */
    public HDF5ArrayBlockParams block(int... dimensions)
    {
        this.blockDimensions = dimensions;
        return this;
    }

    /**
     * Sets the array block indices.
     *  
     * @param blockIndex The block index in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     */
    public HDF5ArrayBlockParams index(long... blockIndex)
    {
        this.blockIndex = blockIndex;
        return this;
    }
    
    /**
     * Sets the array block offset.
     *  
     * @param offset The offset in the array to start reading from in each dimension.
     */
    public HDF5ArrayBlockParams offset(long... offset)
    {
        this.blockOffset = offset;
        return this;
    }
    
    /**
     * Sets an array slice. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The data block read or written only contains the free (i.e. non-fixed) indices.
     * 
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public HDF5ArrayBlockParams slice(long... boundIndices)
    {
        boundIndexArray = boundIndices;
        return this;
    }
    
    /**
     * Sets an array slice. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The data block read or written only contains the free (i.e. non-fixed) indices.
     * 
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     */
    public HDF5ArrayBlockParams slice(IndexMap boundIndices)
    {
        boundIndexMap = boundIndices;
        return this;
    }
    
}
