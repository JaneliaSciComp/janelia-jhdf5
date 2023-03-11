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

/**
 * A class that is used for iterating over a data set block by block, using
 * <em>natural data blocks</em>. The <em>Natural block</em> for chunked data sets is a chunk, for
 * non-chunked data sets it is the complete array.
 * <p>
 * The pattern for using this class is:
 * 
 * <pre>
 * for (HDF5DataBlock&lt;int[]&gt; block : reader.getIntNaturalBlocks(dsName1D))
 * {
 *     float[] naturalBlock = block.getData();
 *     ... work on naturalBlock, use block.getIndex() and block.getOffset() where needed ...
 * }
 * </pre>
 * 
 * <b>Note:</b> If the size of the data set is not an integer number of blocks, then the last block
 * will be smaller than the natural block size.
 * 
 * @author Bernd Rinn
 */
public final class HDF5DataBlock<T>
{
    private final T data;

    private final long offset;

    private final long index;

    HDF5DataBlock(T block, long index, long offset)
    {
        this.data = block;
        this.index = index;
        this.offset = offset;
    }

    /**
     * Returns the data block itself.
     */
    public T getData()
    {
        return data;
    }

    /**
     * Returns the offset of this block in the data set.
     */
    public long getOffset()
    {
        return offset;
    }

    /**
     * Returns the iteration index of this block, starting with 0.
     */
    public long getIndex()
    {
        return index;
    }

}
