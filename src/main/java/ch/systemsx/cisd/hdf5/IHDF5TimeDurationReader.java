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

/**
 * An interface that provides methods for reading time duration values from HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#duration()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5TimeDurationReader
{

    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Returns <code>true</code>, if the attribute <var>attributeName</var> of data set
     * <var>objectPath</var> is a time duration and <code>false</code> otherwise.
     */
    public boolean isTimeDuration(String objectPath, String attributeName) throws HDF5JavaException;

    /**
     * Returns the time unit, if the attribute given by <var>attributeName</var> of object
     * <var>objectPath</var> is a time duration and <code>null</code> otherwise.
     */
    public HDF5TimeUnit tryGetTimeUnit(String objectPath, String attributeName)
            throws HDF5JavaException;

    /**
     * Reads a time duration attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time duration.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDuration getAttr(String objectPath, String attributeName);

    /**
     * Reads a time duration array attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time duration.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDurationArray getArrayAttr(String objectPath, String attributeName);

    /**
     * Reads a multi-dimension time duration array attribute named <var>attributeName</var> from the
     * data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time duration.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDurationMDArray getMDArrayAttr(String objectPath, String attributeName);

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Returns <code>true</code>, if the data set given by <var>objectPath</var> is a time duration
     * and <code>false</code> otherwise.
     */
    public boolean isTimeDuration(String objectPath) throws HDF5JavaException;

    /**
     * Returns the time unit, if the data set given by <var>objectPath</var> is a time duration and
     * <code>null</code> otherwise.
     */
    public HDF5TimeUnit tryGetTimeUnit(String objectPath) throws HDF5JavaException;

    /**
     * Reads a time duration value and its unit from the data set <var>objectPath</var>. It needs to
     * be tagged as one of the type variants that indicate a time duration, for example
     * {@link HDF5DataTypeVariant#TIME_DURATION_SECONDS}.
     * <p>
     * This tagging is done by the writer when using
     * {@link IHDF5Writer#writeTimeDuration(String, HDF5TimeDuration)} or can be done by calling
     * {@link IHDF5ObjectReadWriteInfoProviderHandler#setTypeVariant(String, HDF5DataTypeVariant)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The time duration and its unit.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDuration read(String objectPath) throws HDF5JavaException;

    /**
     * Reads a time duration array from the data set <var>objectPath</var>. It needs to be tagged as
     * one of the type variants that indicate a time duration, for example
     * {@link HDF5DataTypeVariant#TIME_DURATION_SECONDS}.
     * <p>
     * This tagging is done by the writer when using
     * {@link IHDF5Writer#writeTimeDuration(String, HDF5TimeDuration)} or can be done by calling
     * {@link IHDF5ObjectReadWriteInfoProviderHandler#setTypeVariant(String, HDF5DataTypeVariant)},
     * most conveniantly by code like
     * 
     * <pre>
     * writer.addTypeVariant(&quot;/dataSetPath&quot;, HDF5TimeUnit.SECONDS.getTypeVariant());
     * </pre>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The time duration in seconds.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDurationArray readArray(String objectPath) throws HDF5JavaException;

    /**
     * Reads a block of a time duration array (of rank 1) from the data set <var>objectPath</var>.
     * It needs to be tagged as one of the type variants that indicate a time duration, for example
     * {@link HDF5DataTypeVariant#TIME_DURATION_SECONDS}.
     * <p>
     * This tagging is done by the writer when using
     * {@link IHDF5Writer#writeTimeDuration(String, HDF5TimeDuration)} or can be done by calling
     * {@link IHDF5ObjectReadWriteInfoProviderHandler#setTypeVariant(String, HDF5DataTypeVariant)},
     * most conveniently by code like
     * 
     * <pre>
     * writer.addTypeVariant(&quot;/dataSetPath&quot;, HDF5TimeUnit.SECONDS.getTypeVariant());
     * </pre>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the length of the <code>long[]</code> returned
     *            if the data set is long enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @return The data read from the data set. The length will be
     *         <code>min(size - blockSize*blockNumber,
     *         blockSize)</code>.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDurationArray readArrayBlock(String objectPath, int blockSize, long blockNumber)
            throws HDF5JavaException;

    /**
     * Reads a block of a time duration array (of rank 1) from the data set <var>objectPath</var>.
     * It needs to be tagged as one of the type variants that indicate a time duration, for example
     * {@link HDF5DataTypeVariant#TIME_DURATION_SECONDS}.
     * <p>
     * This tagging is done by the writer when using
     * {@link IHDF5Writer#writeTimeDuration(String, HDF5TimeDuration)} or can be done by calling
     * {@link IHDF5ObjectReadWriteInfoProviderHandler#setTypeVariant(String, HDF5DataTypeVariant)},
     * most conveniently by code like
     * 
     * <pre>
     * writer.addTypeVariant(&quot;/dataSetPath&quot;, HDF5TimeUnit.SECONDS.getTypeVariant());
     * </pre>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the length of the <code>long[]</code>
     *            returned).
     * @param offset The offset of the block in the data set to start reading from (starting with
     *            0).
     * @return The data block read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDurationArray readArrayBlockWithOffset(String objectPath, int blockSize,
            long offset) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this one-dimensional data set of time durations to iterate
     * over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of a time duration data type or not of rank
     *             1.
     */
    public Iterable<HDF5DataBlock<HDF5TimeDurationArray>> getArrayNaturalBlocks(String objectPath)
            throws HDF5JavaException;

    /**
     * Reads a multi-dimensional array of time durations from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDurationMDArray readMDArray(String objectPath) throws HDF5JavaException;

    /**
     * Reads a multi-dimensional array of time durations from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @return The data block read from the data set.
     */
    public HDF5TimeDurationMDArray readMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber) throws HDF5JavaException;

    /**
     * Reads a multi-dimensional array of time durations from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @return The data block read from the data set.
     */
    public HDF5TimeDurationMDArray readMDArrayBlockWithOffset(String objectPath,
            int[] blockDimensions, long[] offset);

    /**
     * Reads a multi-dimensional array of time durations from the data set <var>objectPath</var>
     * into a given <var>array</var> in memory.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param array The array to read the data into.
     * @param memoryOffset The offset in the array to write the data to.
     * @return The effective dimensions of the block in <var>array</var> that was filled.
     */
    public int[] readToMDArrayWithOffset(String objectPath, HDF5TimeDurationMDArray array,
            int[] memoryOffset);

    /**
     * Reads a block of the multi-dimensional <code>long</code> array data set <var>objectPath</var>
     * into a given <var>array</var> in memory.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param array The array to read the data into.
     * @param blockDimensions The size of the block to read along each axis.
     * @param offset The offset of the block in the data set.
     * @param memoryOffset The offset of the block in the array to write the data to.
     * @return The effective dimensions of the block in <var>array</var> that was filled.
     */
    public int[] readToMDArrayBlockWithOffset(String objectPath, HDF5TimeDurationMDArray array,
            int[] blockDimensions, long[] offset, int[] memoryOffset);

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @see HDF5MDDataBlock
     */
    public Iterable<HDF5MDDataBlock<HDF5TimeDurationMDArray>> getMDArrayNaturalBlocks(
            String dataSetPath);

}