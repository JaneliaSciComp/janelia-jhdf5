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

import ch.systemsx.cisd.base.mdarray.MDByteArray;

/**
 * An interface that provides methods for reading <code>byte</code> values from HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#int8()} or {@link IHDF5Reader#uint8()}.
 * <p>
 * <i>Note:</i> This interface supports block access and sliced access (which is a special cases of 
 * block access) to arrays. The performance of this block access can vary greatly depending on how 
 * the data are layed out in the HDF5 file. For best performance, the block (or slice) dimension should 
 * be chosen to be equal to the chunk dimensions of the array, as in this case the block written / read 
 * are stored as consecutive value in the HDF5 file and one write / read access will suffice.   
 * <p>
 * <i>Note:<i> If the values read are unsigned, use the methods in {@link UnsignedIntUtils} to convert 
 * to a larger Java integer type that can hold all values as unsigned.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5ByteReader
{
    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Reads a <code>byte</code> attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute value read from the data set.
     */
    public byte getAttr(String objectPath, String attributeName);

    /**
     * Reads a <code>byte[]</code> attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute value read from the data set.
     */
    public byte[] getArrayAttr(String objectPath, String attributeName);

    /**
     * Reads a multi-dimensional array <code>byte</code> attribute named <var>attributeName</var>
     * from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute array value read from the data set.
     */
    public MDByteArray getMDArrayAttr(String objectPath,
            String attributeName);

    /**
     * Reads a <code>byte</code> matrix attribute named <var>attributeName</var>
     * from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute matrix value read from the data set.
     */
    public byte[][] getMatrixAttr(String objectPath, String attributeName)
            throws HDF5JavaException;

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Reads a <code>byte</code> value from the data set <var>objectPath</var>. This method 
     * doesn't check the data space but simply reads the first value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The value read from the data set.
     */
    public byte read(String objectPath);

    /**
     * Reads a <code>byte</code> array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public byte[] readArray(String objectPath);

    /**
     * Reads a multi-dimensional <code>byte</code> array data set <var>objectPath</var>
     * into a given <var>array</var> in memory.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param array The array to read the data into.
     * @param memoryOffset The offset in the array to write the data to.
     * @return The effective dimensions of the block in <var>array</var> that was filled.
     */
    public int[] readToMDArrayWithOffset(String objectPath, 
    				MDByteArray array, int[] memoryOffset);

    /**
     * Reads a multi-dimensional <code>byte</code> array data set <var>objectPath</var>
     * into a given <var>array</var> in memory.
     * 
     * @param dataSet The data set to read from.
     * @param array The array to read the data into.
     * @param memoryOffset The offset in the array to write the data to.
     * @return The effective dimensions of the block in <var>array</var> that was filled.
     */
    public int[] readToMDArrayWithOffset(HDF5DataSet dataSet, 
    				MDByteArray array, int[] memoryOffset);

    /**
     * Reads a block of the multi-dimensional <code>byte</code> array data set
     * <var>objectPath</var> into a given <var>array</var> in memory.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param array The array to read the data into.
     * @param blockDimensions The size of the block to read along each axis.
     * @param offset The offset of the block in the data set.
     * @param memoryOffset The offset of the block in the array to write the data to.
     * @return The effective dimensions of the block in <var>array</var> that was filled.
     */
    public int[] readToMDArrayBlockWithOffset(String objectPath,
            MDByteArray array, int[] blockDimensions, long[] offset,
            int[] memoryOffset);

    /**
     * Reads a block of the multi-dimensional <code>byte</code> array data set
     * <var>objectPath</var> into a given <var>array</var> in memory.
     * 
     * @param dataSet The data set to read from.
     * @param array The array to read the data into.
     * @param blockDimensions The size of the block to read along each axis.
     * @param offset The offset of the block in the data set.
     * @param memoryOffset The offset of the block in the array to write the data to.
     * @return The effective dimensions of the block in <var>array</var> that was filled.
     */
    public int[] readToMDArrayBlockWithOffset(HDF5DataSet dataSet,
            MDByteArray array, int[] blockDimensions, long[] offset,
            int[] memoryOffset);

    /**
     * Reads a block from a <code>byte</code> array (of rank 1) from the data set 
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the length of the <code>byte[]</code> returned
     *            if the data set is long enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @return The data read from the data set. The length will be min(size - blockSize*blockNumber,
     *         blockSize).
     */
    public byte[] readArrayBlock(String objectPath, int blockSize,
            long blockNumber);

    /**
     * Reads a block from a <code>int</code> array (of rank 1) from the <var>dataSet</var>.
     * <p>
     * <i>This method is faster than {@link #readArrayBlock(String, int, long)} 
     * when called many times on the same data set.</i>
     * 
     * @param dataSet The data set to read from.
     * @param blockSize The block size (this will be the length of the <code>int[]</code> returned
     *            if the data set is long enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @return The data read from the data set. The length will be min(size - blockSize*blockNumber,
     *         blockSize).
     */
    public byte[] readArrayBlock(HDF5DataSet dataSet, int blockSize,
            long blockNumber);

    /**
     * Reads a block from <code>byte</code> array (of rank 1) from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the length of the <code>byte[]</code>
     *            returned).
     * @param offset The offset of the block in the data set to start reading from (starting with 0).
     * @return The data block read from the data set.
     */
    public byte[] readArrayBlockWithOffset(String objectPath, int blockSize,
            long offset);

    /**
     * Reads a block from <code>int</code> array (of rank 1) from the <var>dataSet</var>.
     * <p>
     * <i>This method is faster than {@link #readArrayBlockWithOffset(String, int, long)} 
     * when called many times on the same data set.</i>
     * 
     * @param dataSet The data set to read from.
     * @param blockSize The block size (this will be the length of the <code>int[]</code>
     *            returned).
     * @param offset The offset of the block in the data set to start reading from (starting with 0).
     * @return The data block read from the data set.
     */
    public byte[] readArrayBlockWithOffset(HDF5DataSet dataSet, int blockSize,
            long offset);

    /**
     * Reads a <code>byte</code> matrix (array of arrays) from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     *
     * @throws HDF5JavaException If the data set <var>objectPath</var> is not of rank 2.
     */
    public byte[][] readMatrix(String objectPath) throws HDF5JavaException;

    /**
     * Reads a <code>byte</code> matrix (array of arrays) from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSizeX The size of the block in the x dimension.
     * @param blockSizeY The size of the block in the y dimension.
     * @param blockNumberX The block number in the x dimension (offset: multiply with
     *            <code>blockSizeX</code>).
     * @param blockNumberY The block number in the y dimension (offset: multiply with
     *            <code>blockSizeY</code>).
     * @return The data block read from the data set.
     *
     * @throws HDF5JavaException If the data set <var>objectPath</var> is not of rank 2.
     */
    public byte[][] readMatrixBlock(String objectPath, int blockSizeX,
            int blockSizeY, long blockNumberX, long blockNumberY) 
            throws HDF5JavaException;

    /**
     * Reads a <code>byte</code> matrix (array of arrays) from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSizeX The size of the block in the x dimension.
     * @param blockSizeY The size of the block in the y dimension.
     * @param offsetX The offset in x dimension in the data set to start reading from.
     * @param offsetY The offset in y dimension in the data set to start reading from.
     * @return The data block read from the data set.
     *
     * @throws HDF5JavaException If the data set <var>objectPath</var> is not of rank 2.
     */
    public byte[][] readMatrixBlockWithOffset(String objectPath, 
    				int blockSizeX, int blockSizeY, long offsetX, long offsetY) 
    				throws HDF5JavaException;

    /**
     * Reads a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public MDByteArray readMDArray(String objectPath);

    /**
     * Reads part or all of a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param params The parameter block specifying the block or slice to read from the array.
     * @return The data read from the data set.
     */
    public MDByteArray readMDArray(String objectPath, HDF5ArrayBlockParams params);

    /**
     * Reads part or all of a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>.
     * 
     * @param dataSet The data set to read from.
     * @param params The parameter block specifying the block or slice to read from the array.
     * @return The data read from the data set.
     */
    public MDByteArray readMDArray(HDF5DataSet dataSet, HDF5ArrayBlockParams params);

    /**
     * Reads a slice of a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readMDArraySlice(String objectPath, IndexMap boundIndices);

    /**
     * Reads a slice of a multi-dimensional <code>byte</code> array from the data set
     * <var>dataSet</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to read from.
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readMDArraySlice(HDF5DataSet dataSet, IndexMap boundIndices);

    /**
     * Reads a slice of a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readMDArraySlice(String objectPath, long[] boundIndices);

    /**
     * Reads a slice of a multi-dimensional <code>byte</code> array from the data set
     * <var>dataSet</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to read from.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readMDArraySlice(HDF5DataSet dataSet, long[] boundIndices);

    /**
     * Reads a block from a multi-dimensional <code>byte</code> array from the data set 
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @return The data block read from the data set.
     */
    public MDByteArray readMDArrayBlock(String objectPath,
    				int[] blockDimensions, long[] blockNumber);

    /**
     * Reads a block from a multi-dimensional <code>byte</code> array from the data set 
     * <var>dataSet</var>.
     * 
     * @param dataSet The data set to read from.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @return The data block read from the data set.
     */
    public MDByteArray readMDArrayBlock(HDF5DataSet dataSet, int[] blockDimensions,
            long[] blockNumber);
    
    /**
     * Reads a sliced block from a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readSlicedMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber, IndexMap boundIndices);

    /**
     * Reads a sliced block from a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to read from.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readSlicedMDArrayBlock(HDF5DataSet dataSet, int[] blockDimensions,
            long[] blockNumber, IndexMap boundIndices);

    /**
     * Reads a sliced block from a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readSlicedMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber, long[] boundIndices);

    /**
     * Reads a sliced block from a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to read from.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readSlicedMDArrayBlock(HDF5DataSet dataSet, int[] blockDimensions,
            long[] blockNumber, long[] boundIndices);

    /**
     * Reads a block from a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @return The data block read from the data set.
     */
    public MDByteArray readMDArrayBlockWithOffset(String objectPath,
            int[] blockDimensions, long[] offset);
    
    /**
     * Reads a block from a multi-dimensional <code>byte</code> array from the data set
     * <var>dataSet</var>.
     * 
     * @param dataSet The data set to read from.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @return The data block read from the data set.
     */
    public MDByteArray readMDArrayBlockWithOffset(HDF5DataSet dataSet,
            int[] blockDimensions, long[] offset);
    
    /**
     * Reads a sliced block of a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readSlicedMDArrayBlockWithOffset(String objectPath, int[] blockDimensions,
            long[] offset, IndexMap boundIndices);

    /**
     * Reads a sliced block of a multi-dimensional <code>byte</code> array from the data set
     * <var>dataSet</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to read from.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, int[] blockDimensions,
            long[] offset, IndexMap boundIndices);
    
    /**
     * Reads a sliced block of a multi-dimensional <code>byte</code> array from the data set
     * <var>objectPath</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readSlicedMDArrayBlockWithOffset(String objectPath, int[] blockDimensions,
            long[] offset, long[] boundIndices);

    /**
     * Reads a sliced block of a multi-dimensional <code>byte</code> array from the data set
     * <var>dataSet</var>. The slice is defined by "bound indices", each of which is fixed to a
     * given value. The returned data block only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to read from.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     * @return The data block read from the data set.
     */
    public MDByteArray readSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, int[] blockDimensions,
            long[] offset, long[] boundIndices);
    
    /**
     * Provides all natural blocks of this one-dimensional data set to iterate over.
     * 
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1.
     */
    public Iterable<HDF5DataBlock<byte[]>> getArrayNaturalBlocks(
    									String dataSetPath)
            throws HDF5JavaException;

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @see HDF5MDDataBlock
     */
    public Iterable<HDF5MDDataBlock<MDByteArray>> getMDArrayNaturalBlocks(
    									String dataSetPath);
}
