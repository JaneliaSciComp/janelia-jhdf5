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

import java.util.BitSet;

/**
 * An interface that provides methods for writing <code>boolean</code> values to HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Writer#bool()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5BooleanWriter extends IHDF5BooleanReader
{

    /**
     * Sets a <code>boolean</code> attribute to the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setAttr(String objectPath, String name, boolean value);

    /**
     * Writes out a <code>boolean</code> value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param value The value of the data set.
     */
    public void write(String objectPath, boolean value);

    /**
     * Writes out a bit field ((which can be considered the equivalent to a boolean array of rank
     * 1), provided as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link IHDF5LongReader#readArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeBitField(String objectPath, BitSet data);

    /**
     * Writes out a bit field ((which can be considered the equivalent to a boolean array of rank
     * 1), provided as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link IHDF5LongReader#readArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void writeBitField(String objectPath, BitSet data, HDF5GenericStorageFeatures features);

    /**
     * Creates a bit field (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size (in 64 bit words) of the bit field to create. This will be the total
     *            size for non-extendable data sets and the size of one chunk for extendable
     *            (chunked) data sets. For extendable data sets the initial size of the array will
     *            be 0, see
     *            {@link ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator#dontUseExtendableDataTypes}.
     */
    public void createBitField(String objectPath, int size);

    /**
     * Creates a bit field (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size (in 64 bit words) of the bit field to create. When using extendable data
     *            sets ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no
     *            data set smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}).
     */
    public void createBitField(String objectPath, long size, int blockSize);

    /**
     * Creates a bit field array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size (in 64 bit words) of the bit field to create. This will be the total
     *            size for non-extendable data sets and the size of one chunk for extendable
     *            (chunked) data sets. For extendable data sets the initial size of the array will
     *            be 0, see {@link HDF5IntStorageFeatures}.
     * @param features The storage features of the data set.
     */
    public void createBitField(String objectPath, int size, HDF5GenericStorageFeatures features);

    /**
     * Creates a bit field (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size (in 64 bit words) of the bit field to create. When using extendable data
     *            sets ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no
     *            data set smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})
     *            and <code>features</code> is <code>HDF5IntStorageFeature.INT_NO_COMPRESSION</code>
     *            .
     * @param features The storage features of the data set.
     */
    public void createBitField(String objectPath, long size, int blockSize,
            HDF5GenericStorageFeatures features);

    /**
     * Writes out a block of a bit field (of rank 1). The data set needs to have been created by
     * {@link #createBitField(String, long, int, HDF5GenericStorageFeatures)} beforehand.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createBitField(String, long, int, HDF5GenericStorageFeatures)} call that was used to
     * create the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param dataSize The (real) size of <code>data</code>
     * @param blockNumber The number of the block to write.
     */
    public void writeBitFieldBlock(String objectPath, BitSet data, int dataSize, long blockNumber);

    /**
     * Writes out a block of a <code>long</code> array (of rank 1). The data set needs to have been
     * created by {@link #createBitFieldArray(String, int, long, long, HDF5IntStorageFeatures)}
     * beforehand.
     * <p>
     * Use this method instead of {@link #writeBitFieldBlock(String, BitSet, int, long)} if the
     * total size of the data set is not a multiple of the block size.
     * <p>
     * <i>Note:</i> For best performance, the typical <var>dataSize</var> in this method should be
     * chosen to be equal to the <var>blockSize</var> argument of the
     * {@link #createBitField(String, long, int, HDF5GenericStorageFeatures)} call that was used to
     * create the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param dataSize The (real) size of <code>data</code>
     * @param offset The offset in the data set to start writing to.
     */
    public void writeBitFieldBlockWithOffset(String objectPath, BitSet data, int dataSize,
            long offset);

    /**
     * Writes out an array of bit fields (which can be considered the equivalent to a boolean array
     * of rank 2), provided as a Java array of {@link BitSet}s.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link IHDF5LongReader#readArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void writeBitFieldArray(String objectPath, BitSet[] data, HDF5IntStorageFeatures features);

    /**
     * Writes out an array of bit fields (which can be considered the equivalent to a boolean array
     * of rank 2), provided as a Java array of {@link BitSet}s.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link IHDF5LongReader#readArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeBitFieldArray(String objectPath, BitSet[] data);

    /**
     * Creates an array of bit fields (of rank 1) (which can be considered the equivalent to a
     * boolean array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param bitFieldSize The size of the bit fields in the array (in 64 bit words).
     * @param arraySize The size of the array of bit fields to create. When using extendable data
     *            sets ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no
     *            data set smaller than this size can be created, however data sets may be larger.
     * @param arrayBlockSize The size of one block (for block-wise IO). Ignored if no extendable
     *            data sets are used (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}) and
     *            <code>features</code> is <code>HDF5IntStorageFeature.INT_NO_COMPRESSION</code>.
     * @param features The storage features of the data set.
     */
    public void createBitFieldArray(String objectPath, int bitFieldSize, long arraySize,
            long arrayBlockSize, HDF5IntStorageFeatures features);

    /**
     * Creates an array of bit fields (of rank 1) (which can be considered the equivalent to a
     * boolean array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param bitFieldSize The size of the bit fields in the array (in 64 bit words).
     * @param arraySize The size of the array of bit fields to create. When using extendable data
     *            sets ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no
     *            data set smaller than this size can be created, however data sets may be larger.
     * @param arrayBlockSize The size of one block (for block-wise IO). Ignored if no extendable
     *            data sets are used (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}) and
     *            <code>features</code> is <code>HDF5IntStorageFeature.INT_NO_COMPRESSION</code>.
     */
    public void createBitFieldArray(String objectPath, int bitFieldSize, long arraySize,
            long arrayBlockSize);

    /**
     * Creates an empty array of bit fields (of rank 1) (which can be considered the equivalent to a
     * boolean array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param bitFieldSize The size of the bit fields in the array (in 64 bit words).
     * @param arraySize The size of the long array to create. When <i>requesting</i> a chunked data
     *            set (e.g. {@link HDF5IntStorageFeatures#INT_CHUNKED}), the initial size of the
     *            array will be 0 and the chunk size will be <var>arraySize</var>. When
     *            <i>allowing</i> a chunked data set (e.g.
     *            {@link HDF5IntStorageFeatures#INT_NO_COMPRESSION} when the writer is not
     *            configured to avoid extendable data types, see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial size
     *            and the chunk size of the array will be <var>arraySize</var>. When
     *            <i>enforcing</i> a on-extendable data set (e.g.
     *            {@link HDF5IntStorageFeatures#INT_CONTIGUOUS}), the initial size equals the total size
     *            and will be <var>arraySize</var>.
     * @param features The storage features of the data set.
     */
    public void createBitFieldArray(String objectPath, int bitFieldSize, long arraySize,
            HDF5IntStorageFeatures features);

    /**
     * Creates an empty array of bit fields (of rank 1) (which can be considered the equivalent to a
     * boolean array of rank 2). This method always creates an extendable data sets.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param bitFieldSize The size of the bit fields in the array (in 64 bit words).
     * @param arraySize When the writer is configured to use extendable data types (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial size
     *            and the chunk size of the array will be <var>arraySize</var>. When the writer is
     *            configured to <i>enforce</i> a on-extendable data set, the initial size equals the
     *            total size and will be <var>arraySize</var>.
     */
    public void createBitFieldArray(String objectPath, int bitFieldSize, long arraySize);

    /**
     * Writes out an array of bit fields (which can be considered the equivalent to a boolean array
     * of rank 2), provided as a Java array of {@link BitSet}s.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link IHDF5LongReader#readArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param dataSize The number of entries from <var>data</var> to write to the bitfield array
     *            (must be smaller or equals to <code>data.length</code>.
     * @param blockNumber The block number in the array (offset: multiply with
     *            <var>dataSize</var>>).
     */
    public void writeBitFieldArrayBlock(String objectPath, BitSet[] data, int dataSize,
            long blockNumber);

    /**
     * Writes out an array of bit fields (which can be considered the equivalent to a boolean array
     * of rank 2), provided as a Java array of {@link BitSet}s.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link IHDF5LongReader#readArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param blockNumber The block number in the array (offset: multiply with
     *            <code>data.length</code>).
     */
    public void writeBitFieldArrayBlock(String objectPath, BitSet[] data, long blockNumber);

    /**
     * Writes out an array of bit fields (which can be considered the equivalent to a boolean array
     * of rank 2), provided as a Java array of {@link BitSet}s.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link IHDF5LongReader#readArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param dataSize The number of entries from <var>data</var> to write to the bitfield array
     *            (must be smaller or equals to <code>data.length</code>.
     * @param offset The offset in the bitfield array where to start writing the data.
     */
    public void writeBitFieldArrayBlockWithOffset(String objectPath, BitSet[] data, int dataSize,
            long offset);

    /**
     * Writes out an array of bit fields (which can be considered the equivalent to a boolean array
     * of rank 2), provided as a Java array of {@link BitSet}s.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link IHDF5LongReader#readArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param offset The offset in the bitfield array where to start writing the data.
     */
    public void writeBitFieldArrayBlockWithOffset(String objectPath, BitSet[] data, long offset);

}