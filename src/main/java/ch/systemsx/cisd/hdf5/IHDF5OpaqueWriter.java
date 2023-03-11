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
 * An interface that provides methods for writing opaque values to HDF5 files. Opaque values are
 * represented as byte arrays, however, contrary to the methods in {@link IHDF5ByteWriter} there is
 * no notion on the interpretation of these values. The methods in this writer can be used to store
 * data sets which are a "black box". Note that there are no dedicated methods for reading opaque
 * types. Use the methods in {@link IHDF5OpaqueReader} instead which allow you to read any data set
 * as a byte array.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Writer#opaque()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5OpaqueWriter extends IHDF5OpaqueReader
{

    /**
     * Writes out an opaque data type described by <var>tag</var> and defined by a <code>byte</code>
     * array (of rank 1).
     * <p>
     * Note that there is no dedicated method for reading opaque types. Use the method
     * {@link IHDF5OpaqueReader#readArray(String)} instead.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param tag The tag of the data set.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeArray(final String objectPath, final String tag, final byte[] data);

    /**
     * Writes out an opaque data type described by <var>tag</var> and defined by a <code>byte</code>
     * array (of rank 1).
     * <p>
     * Note that there is no dedicated method for reading opaque types. Use the method
     * {@link IHDF5OpaqueReader#readArray(String)} instead.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param tag The tag of the data set.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void writeArray(final String objectPath, final String tag, final byte[] data,
            final HDF5GenericStorageFeatures features);

    /**
     * Creates an opaque data set that will be represented as a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the byte array to create.
     * @param blockSize The size of on block (for block-wise IO)
     * @return The {@link HDF5OpaqueType} that can be used in methods
     *         {@link #writeArrayBlock(String, HDF5OpaqueType, byte[], long)} and
     *         {@link #writeArrayBlockWithOffset(String, HDF5OpaqueType, byte[], int, long)}
     *         to represent this opaque type.
     */
    public HDF5OpaqueType createArray(final String objectPath, final String tag,
            final long size, final int blockSize);

    /**
     * Creates an opaque data set that will be represented as a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the byte array to create. This will be the total size for
     *            non-extendable data sets and the size of one chunk for extendable (chunked) data
     *            sets. For extendable data sets the initial size of the array will be 0, see
     *            {@link ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator#dontUseExtendableDataTypes}.
     * @return The {@link HDF5OpaqueType} that can be used in methods
     *         {@link #writeArrayBlock(String, HDF5OpaqueType, byte[], long)} and
     *         {@link #writeArrayBlockWithOffset(String, HDF5OpaqueType, byte[], int, long)}
     *         to represent this opaque type.
     */
    public HDF5OpaqueType createArray(final String objectPath, final String tag,
            final int size);

    /**
     * Creates an opaque data set that will be represented as a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the byte array to create.
     * @param blockSize The size of on block (for block-wise IO)
     * @param features The storage features of the data set.
     * @return The {@link HDF5OpaqueType} that can be used in methods
     *         {@link #writeArrayBlock(String, HDF5OpaqueType, byte[], long)} and
     *         {@link #writeArrayBlockWithOffset(String, HDF5OpaqueType, byte[], int, long)}
     *         to represent this opaque type.
     */
    public HDF5OpaqueType createArray(final String objectPath, final String tag,
            final long size, final int blockSize, final HDF5GenericStorageFeatures features);

    /**
     * Creates an opaque data set that will be represented as a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the byte array to create. This will be the total size for
     *            non-extendable data sets and the size of one chunk for extendable (chunked) data
     *            sets. For extendable data sets the initial size of the array will be 0, see
     *            {@link HDF5GenericStorageFeatures}.
     * @param features The storage features of the data set.
     * @return The {@link HDF5OpaqueType} that can be used in methods
     *         {@link #writeArrayBlock(String, HDF5OpaqueType, byte[], long)} and
     *         {@link #writeArrayBlockWithOffset(String, HDF5OpaqueType, byte[], int, long)}
     *         to represent this opaque type.
     */
    public HDF5OpaqueType createArray(final String objectPath, final String tag,
            final int size, final HDF5GenericStorageFeatures features);

    /**
     * Writes out a block of an opaque data type represented by a <code>byte</code> array (of rank
     * 1). The data set needs to have been created by
     * {@link #createArray(String, String, long, int, HDF5GenericStorageFeatures)}
     * beforehand.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createArray(String, String, long, int, HDF5GenericStorageFeatures)} call
     * that was used to created the data set.
     * <p>
     * Note that there is no dedicated method for reading opaque types. Use the method
     * {@link IHDF5OpaqueReader#readArrayBlock(String, int, long)} instead.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param blockNumber The number of the block to write.
     */
    public void writeArrayBlock(final String objectPath, final HDF5OpaqueType dataType,
            final byte[] data, final long blockNumber);

    /**
     * Writes out a block of an opaque data type represented by a <code>byte</code> array (of rank
     * 1). The data set needs to have been created by
     * {@link #createArray(String, String, long, int, HDF5GenericStorageFeatures)}
     * beforehand.
     * <p>
     * Use this method instead of
     * {@link #writeArrayBlock(String, HDF5OpaqueType, byte[], long)} if the total size of
     * the data set is not a multiple of the block size.
     * <p>
     * <i>Note:</i> For best performance, the <var>dataSize</var> in this method should be
     * chosen to be equal to the <var>blockSize</var> argument of the
     * {@link #createArray(String, String, long, int, HDF5GenericStorageFeatures)} call
     * that was used to created the data set.
     * <p>
     * Note that there is no dedicated method for reading opaque types. Use the method
     * {@link IHDF5OpaqueReader#readArrayBlockWithOffset(String, int, long)} instead.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param dataSize The (real) size of <code>data</code> (needs to be <code><= data.length</code>
     *            )
     * @param offset The offset in the data set to start writing to.
     */
    public void writeArrayBlockWithOffset(final String objectPath,
            final HDF5OpaqueType dataType, final byte[] data, final int dataSize, final long offset);

}