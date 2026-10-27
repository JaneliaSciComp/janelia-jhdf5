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
 * Only chunks that are stored in the file are listed. With the default (late) space allocation, a
 * chunk that has never been written is not in the index, and reads as the data set's fill value. If
 * the data set was created with early space allocation, every chunk is allocated and so is listed,
 * whether it was written or not. The order of the chunks is unspecified.
 * <p>
 * The index is a snapshot. It does not follow later changes to the data set: writing to the data
 * set can allocate new chunks, and rewriting or recompressing a chunk can move it to another address
 * or change its size. Addresses and sizes can also change when the file is rewritten, for example by
 * compacting it, so only read chunks from the file while nothing writes to it.
 * <p>
 * Offsets are in elements, and in the same dimension order as
 * {@link HDF5DataSetInformation#getDimensions()} and
 * {@link HDF5DataSetInformation#tryGetChunkSizes()}.
 * <p>
 * The bytes at an address are the chunk as stored: if the data set has filters (compression, byte
 * shuffling, ...), the caller has to reverse them. Bit <code>i</code> of the filter mask of a chunk
 * set means that filter <code>i</code> of the data set's pipeline was skipped for that chunk.
 * <p>
 * Two indexes are equal if they list the same chunks in the same order.
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
        final int n = sizes.length;
        if (rank < 0 || filterMasks.length != n || addresses.length != n
                || offsets.length != n * rank)
        {
            throw new IllegalArgumentException("Inconsistent chunk index arrays: rank " + rank
                    + ", offsets " + offsets.length + ", filter masks " + filterMasks.length
                    + ", addresses " + addresses.length + ", sizes " + n);
        }
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

    private void checkIndex(final int index)
    {
        if (index < 0 || index >= sizes.length)
        {
            throw new IndexOutOfBoundsException("chunk " + index + ", number of chunks "
                    + sizes.length);
        }
    }

    /**
     * Returns the offset (in elements) of the first element of chunk <var>index</var>.
     *
     * @throws IndexOutOfBoundsException if <var>index</var> is not in
     *             <code>0 .. getNumberOfChunks() - 1</code>.
     */
    public long[] getOffset(final int index)
    {
        checkIndex(index);
        return Arrays.copyOfRange(offsets, index * rank, (index + 1) * rank);
    }

    /**
     * Returns the offset (in elements) of chunk <var>index</var> in dimension <var>dimension</var>.
     *
     * @throws IndexOutOfBoundsException if <var>index</var> is not in
     *             <code>0 .. getNumberOfChunks() - 1</code>, or <var>dimension</var> is not in
     *             <code>0 .. getRank() - 1</code>.
     */
    public long getOffset(final int index, final int dimension)
    {
        checkIndex(index);
        if (dimension < 0 || dimension >= rank)
        {
            throw new IndexOutOfBoundsException("dimension " + dimension + ", rank " + rank);
        }
        return offsets[index * rank + dimension];
    }

    /**
     * Returns the filter mask of chunk <var>index</var>: bit <code>i</code> set means that filter
     * <code>i</code> of the data set's filter pipeline was <i>not</i> applied to this chunk.
     *
     * @throws IndexOutOfBoundsException if <var>index</var> is not in
     *             <code>0 .. getNumberOfChunks() - 1</code>.
     */
    public int getFilterMask(final int index)
    {
        checkIndex(index);
        return filterMasks[index];
    }

    /**
     * Returns the address of chunk <var>index</var> in the file, as a byte offset.
     *
     * @throws IndexOutOfBoundsException if <var>index</var> is not in
     *             <code>0 .. getNumberOfChunks() - 1</code>.
     */
    public long getAddress(final int index)
    {
        checkIndex(index);
        return addresses[index];
    }

    /**
     * Returns the size in bytes that chunk <var>index</var> occupies in the file (after filters).
     *
     * @throws IndexOutOfBoundsException if <var>index</var> is not in
     *             <code>0 .. getNumberOfChunks() - 1</code>.
     */
    public long getSize(final int index)
    {
        checkIndex(index);
        return sizes[index];
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof HDF5ChunkIndex == false)
        {
            return false;
        }
        final HDF5ChunkIndex that = (HDF5ChunkIndex) obj;
        return rank == that.rank && Arrays.equals(offsets, that.offsets)
                && Arrays.equals(filterMasks, that.filterMasks)
                && Arrays.equals(addresses, that.addresses) && Arrays.equals(sizes, that.sizes);
    }

    @Override
    public int hashCode()
    {
        int result = rank;
        result = 31 * result + Arrays.hashCode(offsets);
        result = 31 * result + Arrays.hashCode(filterMasks);
        result = 31 * result + Arrays.hashCode(addresses);
        result = 31 * result + Arrays.hashCode(sizes);
        return result;
    }

    @Override
    public String toString()
    {
        return "HDF5ChunkIndex[rank=" + rank + ", chunks=" + sizes.length + "]";
    }
}
