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

import java.util.NoSuchElementException;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;

/**
 * A class for computing the parameters of multi-dimensional natural blocks.
 * 
 * @author Bernd Rinn
 */
final class HDF5NaturalBlockMDParameters
{
    private final int rank;

    private final long[] numberOfBlocks;
    
    private final int[] naturalBlockSize;
    
    private final int[] lastBlockSize;

    final class HDF5NaturalBlockMDIndex
    {
        private long[] index = new long[rank];

        private long[] offset = new long[rank];

        private int[] blockSize = naturalBlockSize.clone();

        private boolean indexCalculated = true;

        boolean hasNext()
        {
            if (indexCalculated)
            {
                return true;
            }
            for (int i = index.length - 1; i >= 0; --i)
            {
                ++index[i];
                if (index[i] < numberOfBlocks[i])
                {
                    offset[i] += naturalBlockSize[i];
                    if (index[i] == numberOfBlocks[i] - 1)
                    {
                        blockSize[i] = lastBlockSize[i];
                    }
                    indexCalculated = true;
                    break;
                } else
                {
                    index[i] = 0;
                    offset[i] = 0;
                    blockSize[i] = naturalBlockSize[i];
                }
            }
            return indexCalculated;
        }

        long[] computeOffsetAndSizeGetOffsetClone()
        {
            if (hasNext() == false)
            {
                throw new NoSuchElementException();
            }
            indexCalculated = false;
            return offset.clone();
        }

        int[] getBlockSize()
        {
            return blockSize;
        }
        
        long[] getIndexClone()
        {
            return index.clone();
        }
    }

    HDF5NaturalBlockMDParameters(final HDF5DataSetInformation info)
    {
        rank = info.getRank();
        final long[] dimensions = info.getDimensions();
        naturalBlockSize =
                (info.getStorageLayout() == HDF5StorageLayout.CHUNKED) ? info.tryGetChunkSizes()
                        : MDAbstractArray.toInt(dimensions);
        numberOfBlocks = new long[rank];
        lastBlockSize = new int[rank];
        for (int i = 0; i < dimensions.length; ++i)
        {
            final int sizeModNaturalBlockSize = (int) (dimensions[i] % naturalBlockSize[i]);
            numberOfBlocks[i] =
                    (dimensions[i] / naturalBlockSize[i]) + (sizeModNaturalBlockSize != 0 ? 1 : 0);
            lastBlockSize[i] =
                    (sizeModNaturalBlockSize != 0) ? sizeModNaturalBlockSize : naturalBlockSize[i];
        }
    }

    HDF5NaturalBlockMDIndex getNaturalBlockIndex()
    {
        return new HDF5NaturalBlockMDIndex();
    }

}
