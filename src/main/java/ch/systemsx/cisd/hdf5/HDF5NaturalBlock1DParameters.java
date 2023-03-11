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

import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * A class for computing the parameters of one-dimensional natural blocks.
 * 
 * @author Bernd Rinn
 */
final class HDF5NaturalBlock1DParameters
{
    private final int naturalBlockSize;

    private final long numberOfBlocks;

    private final int lastBlockSize;

    final class HDF5NaturalBlock1DIndex
    {
        private long index = 0;

        private long offset;

        private int blockSize;

        boolean hasNext()
        {
            return index < numberOfBlocks;
        }

        long computeOffsetAndSizeGetOffset()
        {
            if (hasNext() == false)
            {
                throw new NoSuchElementException();
            }
            offset = naturalBlockSize * index;
            blockSize = (index == numberOfBlocks - 1) ? lastBlockSize : naturalBlockSize;
            return offset;
        }

        int getBlockSize()
        {
            return blockSize;
        }

        long getAndIncIndex()
        {
            return index++;
        }
    }

    HDF5NaturalBlock1DParameters(final HDF5DataSetInformation info)
    {
        if (info.getRank() > 1)
        {
            throw new HDF5JavaException("Data Set is expected to be of rank 1 (rank="
                    + info.getRank() + ")");
        }
        final long size = info.getDimensions()[0];
        naturalBlockSize =
                (info.getStorageLayout() == HDF5StorageLayout.CHUNKED) ? info.tryGetChunkSizes()[0]
                        : (int) size;
        final int sizeModNaturalBlockSize = (int) (size % naturalBlockSize);
        numberOfBlocks = (size / naturalBlockSize) + (sizeModNaturalBlockSize != 0 ? 1 : 0);
        lastBlockSize = (sizeModNaturalBlockSize != 0) ? sizeModNaturalBlockSize : naturalBlockSize;
    }

    HDF5NaturalBlock1DIndex getNaturalBlockIndex()
    {
        return new HDF5NaturalBlock1DIndex();
    }

}
