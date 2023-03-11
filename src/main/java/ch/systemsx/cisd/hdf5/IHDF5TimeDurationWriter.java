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
 * An interface that provides methods for writing time duration values from HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Writer#duration()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5TimeDurationWriter extends IHDF5TimeDurationReader
{

    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Set a time duration value as attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param attributeName The name of the attribute.
     * @param timeDuration The value of the attribute.
     * @param timeUnit The unit of the attribute.
     */
    public void setAttr(String objectPath, String attributeName,
            long timeDuration, HDF5TimeUnit timeUnit);

    /**
     * Set a time duration value as attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param attributeName The name of the attribute.
     * @param timeDuration The value of the attribute.
     */
    public void setAttr(String objectPath, String attributeName,
            HDF5TimeDuration timeDuration);

    /**
     * Set a time duration array value as attribute on the referenced object. The smallest time unit
     * in <var>timeDurations</var> will be used as the time unit of the array.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * <p>
     * <em>Note: Time durations are stored as a <code>long[]</code> array.</em>
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param attributeName The name of the attribute.
     * @param timeDurations The value of the attribute.
     */
    public void setArrayAttr(String objectPath, String attributeName,
            HDF5TimeDurationArray timeDurations);

    /**
     * Set a multi-dimensional time duration array value as attribute on the referenced object. The
     * smallest time unit in <var>timeDurations</var> will be used as the time unit of the array.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * <p>
     * <em>Note: Time durations are stored as a <code>long[]</code> array.</em>
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param attributeName The name of the attribute.
     * @param timeDurations The value of the attribute.
     */
    public void setMDArrayAttr(String objectPath, String attributeName,
            HDF5TimeDurationMDArray timeDurations);

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Writes out a time duration value.
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param timeDuration The duration of time to write in the given <var>timeUnit</var>.
     * @param timeUnit The unit of the time duration.
     */
    public void write(String objectPath, long timeDuration, HDF5TimeUnit timeUnit);

    /**
     * Writes out a time duration value.
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param timeDuration The duration of time to write.
     */
    public void write(String objectPath, HDF5TimeDuration timeDuration);

    /**
     * Creates a time duration array (of rank 1).
     * <p>
     * <em>Note: Time durations are stored as <code>long</code> values.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the byte array to create. This will be the total size for
     *            non-extendable data sets and the size of one chunk for extendable (chunked) data
     *            sets. For extendable data sets the initial size of the array will be 0, see
     *            {@link ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator#dontUseExtendableDataTypes}.
     * @param timeUnit The unit of the time duration.
     */
    public void createArray(String objectPath, int size, HDF5TimeUnit timeUnit);

    /**
     * Creates a time duration array (of rank 1).
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the data set to create.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})
     *            and <code>deflate == false</code>.
     * @param timeUnit The unit of the time duration.
     */
    public void createArray(String objectPath, long size, int blockSize,
            HDF5TimeUnit timeUnit);

    /**
     * Creates a time duration array (of rank 1).
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the data set to create.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})
     *            and <code>deflate == false</code>.
     * @param timeUnit The unit of the time duration.
     * @param features The storage features of the data set.
     */
    public void createArray(String objectPath, long size, int blockSize,
            HDF5TimeUnit timeUnit, HDF5GenericStorageFeatures features);

    /**
     * Creates a time duration array (of rank 1).
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the array to create. This will be the total size for non-extendable
     *            data sets and the size of one chunk for extendable (chunked) data sets. For
     *            extendable data sets the initial size of the array will be 0, see
     *            {@link HDF5GenericStorageFeatures}.
     * @param timeUnit The unit of the time duration.
     * @param features The storage features of the data set.
     */
    public void createArray(String objectPath, int size, HDF5TimeUnit timeUnit,
            HDF5GenericStorageFeatures features);

    /**
     * Writes out a time duration array (of rank 1).
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param timeDurations The time durations to write.
     */
    public void writeArray(String objectPath, HDF5TimeDurationArray timeDurations);

    /**
     * Writes out a time duration array (of rank 1).
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param timeDurations The time durations to write.
     * @param features The storage features used to store the array.
     */
    public void writeArray(String objectPath, HDF5TimeDurationArray timeDurations,
            HDF5IntStorageFeatures features);

    /**
     * Writes out a block of a time duration array. The data set needs to have been created by
     * {@link #createArray(String, long, int, HDF5TimeUnit, HDF5GenericStorageFeatures)} beforehand.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createArray(String, long, int, HDF5TimeUnit, HDF5GenericStorageFeatures)} call that
     * was used to create the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param blockNumber The number of the block to write.
     */
    public void writeArrayBlock(String objectPath, HDF5TimeDurationArray data,
            long blockNumber);

    /**
     * Writes out a block of a time duration array. The data set needs to have been created by
     * {@link #createArray(String, long, int, HDF5TimeUnit, HDF5GenericStorageFeatures)} beforehand.
     * <p>
     * Use this method instead of {@link #writeArrayBlock(String, HDF5TimeDurationArray, long)} if
     * the total size of the data set is not a multiple of the block size.
     * <p>
     * <i>Note:</i> For best performance, the typical <var>dataSize</var> in this method should be
     * chosen to be equal to the <var>blockSize</var> argument of the
     * {@link #createArray(String, long, int, HDF5TimeUnit, HDF5GenericStorageFeatures)} call that
     * was used to create the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param dataSize The (real) size of <code>data</code> (needs to be <code><= data.length</code>
     *            )
     * @param offset The offset in the data set to start writing to.
     */
    public void writeArrayBlockWithOffset(String objectPath,
            HDF5TimeDurationArray data, int dataSize, long offset);

    /**
     * Writes out a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param features The storage features of the data set.
     */
    public void writeMDArray(String objectPath, HDF5TimeDurationMDArray data,
            HDF5IntStorageFeatures features);

    /**
     * Writes out a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     */
    public void writeMDArray(String objectPath, HDF5TimeDurationMDArray data);

    /**
     * Creates a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions When the writer is configured to use extendable data types (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial
     *            dimensions and the dimensions of a chunk of the array will be
     *            <var>dimensions</var>. When the writer is configured to <i>enforce</i> a
     *            on-extendable data set, the initial dimensions equal the dimensions and will be
     *            <var>dimensions</var>.
     */
    public void createMDArray(String objectPath, int[] dimensions,
            HDF5TimeUnit timeUnit);

    /**
     * Creates a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array.
     * @param blockDimensions The dimensions of one block (chunk) of the array.
     */
    public void createMDArray(String objectPath, long[] dimensions,
            int[] blockDimensions, HDF5TimeUnit timeUnit);

    /**
     * Creates a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the <code>long</code> array to create. When <i>requesting</i> 
     *            a chunked data set (e.g. {@link HDF5IntStorageFeatures#INT_CHUNKED}), 
     *            the initial size of the array will be 0 and the chunk size will be <var>dimensions</var>. 
     *            When <i>allowing</i> a chunked data set (e.g. 
     *            {@link HDF5IntStorageFeatures#INT_NO_COMPRESSION} when the writer is 
     *            not configured to avoid extendable data types, see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial size
     *            and the chunk size of the array will be <var>dimensions</var>. When <i>enforcing</i> a 
     *            on-extendable data set (e.g. 
     *            {@link HDF5IntStorageFeatures#INT_CONTIGUOUS}), the initial size equals 
     *            the total size and will be <var>dimensions</var>.
     * @param features The storage features of the data set.
     */
    public void createMDArray(String objectPath, int[] dimensions,
            HDF5TimeUnit timeUnit, HDF5IntStorageFeatures features);

    /**
     * Creates a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array.
     * @param blockDimensions The dimensions of one block (chunk) of the array.
     * @param features The storage features of the data set.
     */
    public void createMDArray(String objectPath, long[] dimensions,
            int[] blockDimensions, HDF5TimeUnit timeUnit,
            HDF5IntStorageFeatures features);

    /**
     * Writes out a block of a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param blockNumber The block number in each dimension (offset: multiply with the extend in
     *            the according dimension).
     */
    public void writeMDArrayBlock(String objectPath, HDF5TimeDurationMDArray data,
            long[] blockNumber);

    /**
     * Writes out a block of a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param offset The offset in the data set  to start writing to in each dimension.
     */
    public void writeMDArrayBlockWithOffset(String objectPath,
            HDF5TimeDurationMDArray data, long[] offset);

    /**
     * Writes out a block of a multi-dimensional array of time durations.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param blockDimensions The dimensions of the block to write to the data set.
     * @param offset The offset of the block in the data set to start writing to in each dimension.
     * @param memoryOffset The offset of the block in the <var>data</var> array.
     */
    public void writeMDArrayBlockWithOffset(String objectPath,
            HDF5TimeDurationMDArray data, int[] blockDimensions, long[] offset,
            int[] memoryOffset);

}
