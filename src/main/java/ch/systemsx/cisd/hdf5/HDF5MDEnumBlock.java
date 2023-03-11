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
 * A class that is used for iterating over an <code>Enum</code> data set block by block, using
 * <em>natural data blocks</em>. The <em>Natural block</em> for chunked data sets is a chunk, for
 * non-chunked data sets it is the complete array.
 * <p>
 * The pattern for using this class is:
 * 
 * <pre>
 * for (HDF5MDEnumBlock block : reader.getEnumMDNaturalBlocks(dsNameMD))
 * {
 *     HDF5EnumerationValueMDArray naturalBlock = block.getData();
 *     ... work on naturalBlock, use block.getIndex() or block.getOffset() where needed ...
 * }
 * </pre>
 * 
 * The iteration in the multi-dimensional case will be in C-order, that is last-index is iterated
 * over first.
 * <p>
 * <b>Note:</b> If the size of the data set is not an integer number of blocks, then the last block
 * will be smaller than the natural block size.
 * 
 * @author Bernd Rinn
 */
public class HDF5MDEnumBlock
{
    private final HDF5EnumerationValueMDArray data;

    private final long[] offset;

    private final long[] index;

    HDF5MDEnumBlock(HDF5EnumerationValueMDArray block, long[] index, long[] offset)
    {
        this.data = block;
        this.index = index;
        this.offset = offset;
    }

    /**
     * Returns the data block itself.
     */
    public HDF5EnumerationValueMDArray getData()
    {
        return data;
    }

    /**
     * Returns the offset in the data set for the current iteration in each dimension.
     */
    public long[] getOffset()
    {
        return offset;
    }

    /**
     * Returns the iteration index of this block, starting with <code>{ 0, ..., 0 }</code>.
     */
    public long[] getIndex()
    {
        return index;
    }

}
