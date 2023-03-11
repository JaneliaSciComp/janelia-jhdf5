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

import java.util.Date;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;

/**
 * An interface that provides methods for reading time and date values from HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#time()}.
 *
 * @author Bernd Rinn
 */
public interface IHDF5DateTimeReader
{

    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Returns <code>true</code>, if the attribute <var>attributeName</var> of data set
     * <var>objectPath</var> is a time stamp and <code>false</code> otherwise.
     */
    public boolean isTimeStamp(String objectPath, String attributeName) throws HDF5JavaException;

    /**
     * Reads a time stamp attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time stamp as number of milliseconds since January 1, 1970, 00:00:00 GMT.
     * @throws HDF5JavaException If the attribute <var>attributeName</var> of objct
     *             <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public long getAttrAsLong(String objectPath, String attributeName);

    /**
     * Reads a time stamp array attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time stamp array; each element is a number of milliseconds since January 1, 1970,
     *         00:00:00 GMT.
     * @throws HDF5JavaException If the attribute <var>attributeName</var> of objct
     *             <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public long[] getArrayAttrAsLong(String objectPath, String attributeName);

    /**
     * Reads a multi-dimension time stamp array attribute named <var>attributeName</var> from the
     * data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time stamp array; each element is a number of milliseconds since January 1, 1970,
     *         00:00:00 GMT.
     * @throws HDF5JavaException If the attribute <var>attributeName</var> of objct
     *             <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public MDLongArray getMDArrayAttrAsLong(String objectPath, String attributeName);

    /**
     * Reads a time stamp attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var> and returns it as a <code>Date</code>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time stamp as {@link java.util.Date}.
     * @throws HDF5JavaException If the attribute <var>attributeName</var> of objct
     *             <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public Date getAttr(String objectPath, String attributeName);

    /**
     * Reads a time stamp array attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var> and returns it as a <code>Date[]</code>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time stamp as {@link java.util.Date}.
     * @throws HDF5JavaException If the attribute <var>attributeName</var> of objct
     *             <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public Date[] getArrayAttr(String objectPath, String attributeName);

    /**
     * Reads a multi-dimension time stamp array attribute named <var>attributeName</var> from the
     * data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The time stamp as {@link java.util.Date}.
     * @throws HDF5JavaException If the attribute <var>attributeName</var> of objct
     *             <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public MDArray<Date> getMDArrayAttr(String objectPath, String attributeName);

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Returns <code>true</code>, if the data set given by <var>objectPath</var> is a time stamp and
     * <code>false</code> otherwise.
     */
    public boolean isTimeStamp(String objectPath) throws HDF5JavaException;

    /**
     * Reads a time stamp value from the data set <var>objectPath</var>. The time stamp is stored as
     * a <code>long</code> value in the HDF5 file. It needs to be tagged as type variant
     * {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
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
     * @return The time stamp as number of milliseconds since January 1, 1970, 00:00:00 GMT.
     * @throws HDF5JavaException If the <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public long readTimeStamp(String objectPath) throws HDF5JavaException;

    /**
     * Reads a time stamp array from the data set <var>objectPath</var>. The time stamp is stored as
     * a <code>long</code> value in the HDF5 file. It needs to be tagged as type variant
     * {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
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
     * @return The time stamp as number of milliseconds since January 1, 1970, 00:00:00 GMT.
     * @throws HDF5JavaException If the <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public long[] readTimeStampArray(String objectPath) throws HDF5JavaException;

    /**
     * Reads a block of a time stamp array (of rank 1) from the data set <var>objectPath</var>. The
     * time stamp is stored as a <code>long</code> value in the HDF5 file. It needs to be tagged as
     * type variant {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
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
     * @return The data read from the data set. The length will be min(size - blockSize*blockNumber,
     *         blockSize).
     */
    public long[] readTimeStampArrayBlock(String objectPath, int blockSize, long blockNumber);

    /**
     * Reads a block of a time stamp array (of rank 1) from the data set <var>objectPath</var>. The
     * time stamp is stored as a <code>long</code> value in the HDF5 file. It needs to be tagged as
     * type variant {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
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
     */
    public long[] readTimeStampArrayBlockWithOffset(String objectPath, int blockSize, long offset);

    /**
     * Provides all natural blocks of this one-dimensional data set of time stamps to iterate over.
     * 
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1.
     */
    public Iterable<HDF5DataBlock<long[]>> getTimeStampArrayNaturalBlocks(String dataSetPath)
            throws HDF5JavaException;

    /**
     * Reads a time stamp value from the data set <var>objectPath</var> and returns it as a
     * {@link Date}. The time stamp is stored as a <code>long</code> value in the HDF5 file. It
     * needs to be tagged as type variant
     * {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
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
     * @return The time stamp as {@link Date}.
     * @throws HDF5JavaException If the <var>objectPath</var> does not denote a time stamp.
     */
    public Date readDate(String objectPath) throws HDF5JavaException;

    /**
     * Reads a time stamp array (of rank 1) from the data set <var>objectPath</var> and returns it
     * as an array of {@link Date}s. The time stamp array is stored as a an array of
     * <code>long</code> values in the HDF5 file. It needs to be tagged as type variant
     * {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
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
     * @return The time stamp as {@link Date}.
     * @throws HDF5JavaException If the <var>objectPath</var> does not denote a time stamp.
     */
    public Date[] readDateArray(String objectPath) throws HDF5JavaException;

    /**
     * Reads a block of a {@link Date} array (of rank 1) from the data set <var>objectPath</var>.
     * The time stamp is stored as a <code>long</code> value in the HDF5 file. It needs to be tagged
     * as type variant {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
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
     * @return The data read from the data set. The length will be min(size - blockSize*blockNumber,
     *         blockSize).
     */
    public Date[] readDateArrayBlock(String objectPath, int blockSize, long blockNumber);

    /**
     * Reads a block of a {@link Date} array (of rank 1) from the data set <var>objectPath</var>.
     * The time stamp is stored as a <code>long</code> value in the HDF5 file. It needs to be tagged
     * as type variant {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
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
     */
    public Date[] readDateArrayBlockWithOffset(String objectPath, int blockSize, long offset);

    /**
     * Provides all natural blocks of this one-dimensional data set of {@link Date}s to iterate
     * over.
     * 
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1.
     */
    public Iterable<HDF5DataBlock<Date[]>> getDateArrayNaturalBlocks(String dataSetPath)
            throws HDF5JavaException;

    /**
     * Reads a multi-dimensional array of time stamps from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public MDLongArray readTimeStampMDArray(String objectPath);

    /**
     * Reads a multi-dimensional array of time stamps from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @return The data block read from the data set.
     */
    public MDLongArray readTimeStampMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber);

    /**
     * Reads a multi-dimensional array of time stamps from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @return The data block read from the data set.
     */
    public MDLongArray readTimeStampMDArrayBlockWithOffset(String objectPath,
            int[] blockDimensions, long[] offset);

    /**
     * Reads a multi-dimensional array data set <var>objectPath</var> of type time stamp into a
     * given <var>array</var> in memory.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param array The array to read the data into.
     * @param memoryOffset The offset in the array to write the data to.
     * @return The effective dimensions of the block in <var>array</var> that was filled.
     */
    public int[] readToMDArrayWithOffset(String objectPath, MDLongArray array, int[] memoryOffset);

    /**
     * Reads a block of the multi-dimensional array data set <var>objectPath</var> of type time
     * stamp into a given <var>array</var> in memory.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param array The array to read the data into.
     * @param blockDimensions The size of the block to read along each axis.
     * @param offset The offset of the block in the data set.
     * @param memoryOffset The offset of the block in the array to write the data to.
     * @return The effective dimensions of the block in <var>array</var> that was filled.
     */
    public int[] readToMDArrayBlockWithOffset(String objectPath, MDLongArray array,
            int[] blockDimensions, long[] offset, int[] memoryOffset);

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @see HDF5MDDataBlock
     */
    public Iterable<HDF5MDDataBlock<MDLongArray>> getTimeStampMDArrayNaturalBlocks(
            String dataSetPath);

    /**
     * Reads a multi-dimensional array of {@link Date}s from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public MDArray<Date> readDateMDArray(String objectPath);

    /**
     * Reads a multi-dimensional array of {@link Date}s from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @return The data block read from the data set.
     */
    public MDArray<Date> readDateMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber);

    /**
     * Reads a multi-dimensional array of {@link Date}s from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @return The data block read from the data set.
     */
    public MDArray<Date> readDateMDArrayBlockWithOffset(String objectPath, int[] blockDimensions,
            long[] offset);

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @see HDF5MDDataBlock
     */
    public Iterable<HDF5MDDataBlock<MDArray<Date>>> getDateMDArrayNaturalBlocks(String dataSetPath);

}