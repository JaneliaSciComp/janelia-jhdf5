/*
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

import java.util.Arrays;

/**
 * The allocated chunks of a chunked data set, as reported by <code>H5Dchunk_iter</code>: for each
 * chunk its offset in the data set, where its (possibly compressed) bytes are in the file, how many
 * they are, and which filters were skipped when it was written.
 * <p>
 * Only chunks that are stored in the file are listed. A chunk that has never been written is not
 * in the index, and reads as the data set's fill value. The order of the chunks is unspecified.
 * <p>
 * The index is a snapshot. It does not follow later changes to the data set.
 * <p>
 * Offsets are in the same dimension order as {@link HDF5DataSetInformation#getDimensions()} and
 * {@link HDF5DataSetInformation#tryGetChunkSizes()}.
 * 
 * @see IHDF5ObjectReadOnlyInfoProviderHandler#getChunkIndex(String)
 */
public final class HDF5ChunkIndex
{
    private final int rank;

    /** Chunk <code>i</code>'s offset is <code>offsets[i * rank .. (i + 1) * rank - 1]</code>. */
    private final long[] offsets;

    private final int[] filterMasks;

    private final long[] addresses;

    private final long[] sizes;

    HDF5ChunkIndex(final int rank, final long[] offsets, final int[] filterMasks,
            final long[] addresses, final long[] sizes)
    {
        this.rank = rank;
        this.offsets = offsets;
        this.filterMasks = filterMasks;
        this.addresses = addresses;
        this.sizes = sizes;
    }

    /** Returns the rank (number of dimensions) of the data set. */
    public int getRank()
    {
        return rank;
    }

    /** Returns the number of allocated chunks. */
    public int getNumberOfChunks()
    {
        return sizes.length;
    }

    /** Returns the offset (in elements) of the first element of chunk <var>index</var>. */
    public long[] getOffset(final int index)
    {
        return Arrays.copyOfRange(offsets, index * rank, (index + 1) * rank);
    }

    /** Returns the offset of chunk <var>index</var> in dimension <var>dimension</var>. */
    public long getOffset(final int index, final int dimension)
    {
        if (dimension < 0 || dimension >= rank)
        {
            throw new IndexOutOfBoundsException("dimension " + dimension + ", rank " + rank);
        }
        return offsets[index * rank + dimension];
    }

    /**
     * Returns the filter mask of chunk <var>index</var>: bit <code>i</code> set means that filter
     * <code>i</code> of the data set's filter pipeline was <i>not</i> applied to this chunk.
     */
    public int getFilterMask(final int index)
    {
        return filterMasks[index];
    }

    /** Returns the address of chunk <var>index</var> in the file, as a byte offset. */
    public long getAddress(final int index)
    {
        return addresses[index];
    }

    /** Returns the size in bytes that chunk <var>index</var> occupies in the file (after filters). */
    public long getSize(final int index)
    {
        return sizes[index];
    }

    @Override
    public String toString()
    {
        return "HDF5ChunkIndex[rank=" + rank + ", chunks=" + sizes.length + "]";
    }
}
