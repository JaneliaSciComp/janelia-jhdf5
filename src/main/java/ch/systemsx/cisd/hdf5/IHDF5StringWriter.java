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

import ch.systemsx.cisd.base.mdarray.MDArray;

/**
 * An interface that provides methods for writing <code>String</code> values to HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Writer#string()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5StringWriter extends IHDF5StringReader
{

    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Sets a string attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setAttr(final String objectPath, final String name, final String value);

    /**
     * Sets a string attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     * @param maxLength The maximal length of the value.
     */
    public void setAttr(final String objectPath, final String name, final String value,
            final int maxLength);

    /**
     * Sets a string array attribute on the referenced object. The length of the array is taken to
     * be the longest string in <var>value</var>.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setArrayAttr(final String objectPath, final String name, final String[] value);

    /**
     * Sets a string array attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     * @param maxLength The maximal length of any element in <var>value</var>.
     */
    public void setArrayAttr(final String objectPath, final String name, final String[] value,
            final int maxLength);

    /**
     * Sets a multi-dimensional string array attribute on the referenced object. The length of the
     * array is taken to be the longest string in <var>value</var>.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setMDArrayAttr(final String objectPath, final String name,
            final MDArray<String> value);

    /**
     * Sets a multi-dimensional string array attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     * @param maxLength The maximal length of the value.
     */
    public void setMDArrayAttr(final String objectPath, final String name,
            final MDArray<String> value, final int maxLength);

    /**
     * Sets a string attribute with variable length on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setAttrVL(final String objectPath, final String name, final String value);

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Writes out a <code>String</code> with a fixed maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param maxLength The maximal length of the <var>data</var>.
     */
    public void write(final String objectPath, final String data, final int maxLength);

    /**
     * Writes out a <code>String</code> with a fixed maximal length (which is the length of the
     * string <var>data</var>).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void write(final String objectPath, final String data);

    /**
     * Writes out a <code>String</code> with a fixed maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void write(final String objectPath, final String data,
            final HDF5GenericStorageFeatures features);

    /**
     * Writes out a <code>String</code> with a fixed maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param maxLength The maximal length of the <var>data</var>.
     * @param features The storage features of the data set.
     */
    public void write(final String objectPath, final String data, final int maxLength,
            final HDF5GenericStorageFeatures features);

    /**
     * Writes out a <code>String</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void writeArray(final String objectPath, final String[] data,
            final HDF5GenericStorageFeatures features);

    /**
     * Writes out a <code>String</code> array (of rank 1). Each element of the array will have a
     * fixed maximal length which is defined by the longest string in <var>data</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeArray(final String objectPath, final String[] data);

    /**
     * Writes out a <code>String</code> array (of rank 1). Each element of the array will have a
     * fixed maximal length which is given by <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param maxLength The maximal length of any of the strings in <var>data</var>.
     */
    public void writeArray(final String objectPath, final String[] data, final int maxLength);

    /**
     * Writes out a <code>String</code> array (of rank 1). Each element of the array will have a
     * fixed maximal length which is given by <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param maxLength The maximal length of any of the strings in <var>data</var>.
     * @param features The storage features of the data set.
     */
    public void writeArray(final String objectPath, final String[] data, final int maxLength,
            final HDF5GenericStorageFeatures features);

    /**
     * Writes out a <code>String</code> array (of rank N). Each element of the array will have a
     * fixed maximal length which is defined by the longest string in <var>data</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeMDArray(final String objectPath, final MDArray<String> data);

    /**
     * Writes out a <code>String</code> array (of rank N). Each element of the array will have a
     * fixed maximal length which is defined by the longest string in <var>data</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void writeMDArray(final String objectPath, final MDArray<String> data,
            final HDF5GenericStorageFeatures features);

    /**
     * Writes out a <code>String</code> array (of rank N). Each element of the array will have a
     * fixed maximal length which is given by <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param maxLength The maximal length of any of the strings in <var>data</var>.
     */
    public void writeMDArray(final String objectPath, final MDArray<String> data,
            final int maxLength);

    /**
     * Writes out a <code>String</code> array (of rank N). Each element of the array will have a
     * fixed maximal length which is given by <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param maxLength The maximal length of any of the strings in <var>data</var>.
     * @param features The storage features of the data set.
     */
    public void writeMDArray(final String objectPath, final MDArray<String> data,
            final int maxLength, final HDF5GenericStorageFeatures features);

    /**
     * Creates a <code>String</code> array (of rank 1) for Strings of length <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param maxLength The maximal length of one String in the array.
     * @param size The size of the byte array to create. This will be the total size for
     *            non-extendable data sets and the size of one chunk for extendable (chunked) data
     *            sets. For extendable data sets the initial size of the array will be 0, see
     *            {@link ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator#dontUseExtendableDataTypes}.
     */
    public void createArray(final String objectPath, final int maxLength, final int size);

    /**
     * Creates a <code>String</code> array (of rank 1) for Strings of length <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param maxLength The maximal length of one String in the array.
     * @param size The size of the String array to create. When using extendable data sets ((see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data set
     *            smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}).
     */
    public void createArray(final String objectPath, final int maxLength, final long size,
            final int blockSize);

    /**
     * Creates a <code>String</code> array (of rank 1) for Strings of length <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param maxLength The maximal length of one String in the array.
     * @param size The size of the array to create. This will be the total size for non-extendable
     *            data sets and the size of one chunk for extendable (chunked) data sets. For
     *            extendable data sets the initial size of the array will be 0, see
     *            {@link HDF5GenericStorageFeatures}.
     */
    public void createArray(final String objectPath, final int maxLength, final int size,
            final HDF5GenericStorageFeatures features);

    /**
     * Creates a <code>String</code> array (of rank 1) for Strings of length <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param maxLength The maximal length of one String in the array.
     * @param size The size of the String array to create. When using extendable data sets ((see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data set
     *            smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}).
     * @param features The storage features of the data set.
     */
    public void createArray(final String objectPath, final int maxLength, final long size,
            final int blockSize, final HDF5GenericStorageFeatures features);

    /**
     * Writes out a block of a <code>String</code> array (of rank 1). The data set needs to have
     * been created by
     * {@link #createArray(String, int, long, int, HDF5GenericStorageFeatures)} beforehand.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createArray(String, int, long, int, HDF5GenericStorageFeatures)} call that was
     * used to create the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param blockNumber The number of the block to write.
     */
    public void writeArrayBlock(final String objectPath, final String[] data,
            final long blockNumber);

    /**
     * Writes out a block of a <code>String</code> array (of rank 1). The data set needs to have
     * been created by
     * {@link #createArray(String, int, long, int, HDF5GenericStorageFeatures)} beforehand.
     * <p>
     * Use this method instead of {@link #writeArrayBlock(String, String[], long)} if the
     * total size of the data set is not a multiple of the block size.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createArray(String, int, long, int, HDF5GenericStorageFeatures)} call that was
     * used to create the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param dataSize The (real) size of <code>data</code> (needs to be <code><= data.length</code>
     *            )
     * @param offset The offset in the data set to start writing to.
     */
    public void writeArrayBlockWithOffset(final String objectPath, final String[] data,
            final int dataSize, final long offset);

    /**
     * Creates a <code>String</code> array (of rank N) for Strings of length <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param maxLength The maximal length of one String in the array.
     * @param dimensions The size of the String array to create. When using extendable data sets
     *            ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data
     *            set smaller than this size can be created, however data sets may be larger.
     */
    public void createMDArray(final String objectPath, final int maxLength,
            final int[] dimensions);

    /**
     * Creates a <code>String</code> array (of rank N) for Strings of length <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param maxLength The maximal length of one String in the array.
     * @param dimensions The size of the String array to create. When using extendable data sets
     *            ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data
     *            set smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block in each dimension (for block-wise IO). Ignored if no
     *            extendable data sets are used (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}).
     */
    public void createMDArray(final String objectPath, final int maxLength,
            final long[] dimensions, final int[] blockSize);

    /**
     * Creates a <code>String</code> array (of rank N) for Strings of length <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param maxLength The maximal length of one String in the array.
     * @param dimensions The size of the String array to create. When using extendable data sets
     *            ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data
     *            set smaller than this size can be created, however data sets may be larger.
     * @param features The storage features of the data set.
     */
    public void createMDArray(final String objectPath, final int maxLength,
            final int[] dimensions, final HDF5GenericStorageFeatures features);

    /**
     * Creates a <code>String</code> array (of rank N) for Strings of length <var>maxLength</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param maxLength The maximal length of one String in the array.
     * @param dimensions The size of the String array to create. When using extendable data sets
     *            ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data
     *            set smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block in each dimension (for block-wise IO). Ignored if no
     *            extendable data sets are used (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}).
     * @param features The storage features of the data set.
     */
    public void createMDArray(final String objectPath, final int maxLength,
            final long[] dimensions, final int[] blockSize,
            final HDF5GenericStorageFeatures features);

    /**
     * Writes out a block of a <code>String</code> array (of rank N). The data set needs to have
     * been created by
     * {@link #createMDArray(String, int, long[], int[], HDF5GenericStorageFeatures)}
     * beforehand.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createMDArray(String, int, long[], int[], HDF5GenericStorageFeatures)} call
     * that was used to create the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param blockNumber The number of the block to write in each dimension.
     */
    public void writeMDArrayBlock(final String objectPath, final MDArray<String> data,
            final long[] blockNumber);

    /**
     * Writes out a block of a <code>String</code> array (of rank N). The data set needs to have
     * been created by
     * {@link #createMDArray(String, int, long[], int[], HDF5GenericStorageFeatures)}
     * beforehand.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createMDArray(String, int, long[], int[], HDF5GenericStorageFeatures)} call
     * that was used to create the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param offset The offset in the data set to start writing to.
     */
    public void writeMDArrayBlockWithOffset(final String objectPath,
            final MDArray<String> data, final long[] offset);

    /**
     * Writes out a <code>String</code> with variable maximal length.
     * <p>
     * The advantage of this method over {@link #write(String, String)} is that when writing a
     * new string later it can have a different (also greater) length. The disadvantage is that it
     * it is more time consuming to read and write this kind of string and that it can't be
     * compressed.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeVL(final String objectPath, final String data);

    /**
     * Writes out a <code>String[]</code> where each String of the array has a variable maximal
     * length.
     * <p>
     * The advantage of this method over {@link #writeArray(String, String[])} is that when
     * writing a new string later it can have a different (also greater) length. The disadvantage is
     * that it it is more time consuming to read and write this kind of string and that it can't be
     * compressed.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeArrayVL(final String objectPath, final String[] data);

    /**
     * Writes out a <code>String[]</code> where each String of the array has a variable maximal
     * length.
     * <p>
     * The advantage of this method over {@link #writeArray(String, String[])} is that when
     * writing a new string later it can have a different (also greater) length. The disadvantage is
     * that it it is more time consuming to read and write this kind of string and that it can't be
     * compressed.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void writeArrayVL(final String objectPath, final String[] data,
            final HDF5GenericStorageFeatures features);

    /**
     * Creates a <code>String[]</code> where each String of the array has a variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the byte array to create. This will be the total size for
     *            non-extendable data sets and the size of one chunk for extendable (chunked) data
     *            sets. For extendable data sets the initial size of the array will be 0, see
     *            {@link ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator#dontUseExtendableDataTypes}.
     */
    public void createArrayVL(final String objectPath, final int size);

    /**
     * Creates a <code>String[]</code> where each String of the array has a variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The intial size of the array.
     * @param blockSize The size of block in the array.
     */
    public void createArrayVL(final String objectPath, final long size,
            final int blockSize);

    /**
     * Creates a <code>String[]</code> where each String of the array has a variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The initial size of the array.
     * @param blockSize The size of block in the array.
     * @param features The storage features of the data set.
     */
    public void createArrayVL(final String objectPath, final long size,
            final int blockSize, final HDF5GenericStorageFeatures features);

    /**
     * Creates a <code>String[]</code> where each String of the array has a variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the byte array to create. This will be the total size for
     *            non-extendable data sets and the size of one chunk for extendable (chunked) data
     *            sets. For extendable data sets the initial size of the array will be 0, see
     *            {@link HDF5GenericStorageFeatures}.
     * @param features The storage features of the data set.
     */
    public void createArrayVL(final String objectPath, final int size,
            final HDF5GenericStorageFeatures features);

    /**
     * Creates a multi-dimensional <code>String</code> array where each String of the array has a
     * variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The initial dimensions (along each axis) of the array.
     * @param features The storage features of the data set.
     */
    public void createMDArrayVL(final String objectPath, final int[] dimensions,
            final HDF5GenericStorageFeatures features);

    /**
     * Creates a multi-dimensional <code>String</code> array where each String of the array has a
     * variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The initial dimensions (along each axis) of the array.
     */
    public void createMDArrayVL(final String objectPath, final int[] dimensions);

    /**
     * Creates a multi-dimensional <code>String</code> array where each String of the array has a
     * variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The initial dimensions (along each axis) of the array.
     * @param blockSize The size of a contiguously stored block (along each axis) in the array.
     * @param features The storage features of the data set.
     */
    public void createMDArrayVL(final String objectPath, final long[] dimensions,
            final int[] blockSize, final HDF5GenericStorageFeatures features);

    /**
     * Creates a multi-dimensional <code>String</code> array where each String of the array has a
     * variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The initial dimensions (along each axis) of the array.
     * @param blockSize The size of a contiguously stored block (along each axis) in the array.
     */
    public void createMDArrayVL(final String objectPath, final long[] dimensions,
            final int[] blockSize);

    /**
     * Writes out a <code>String</code> array (of rank N). Each element of the array will have a
     * variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeMDArrayVL(final String objectPath, final MDArray<String> data);

    /**
     * Writes out a <code>String</code> array (of rank N). Each element of the array will have a
     * variable maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void writeMDArrayVL(final String objectPath,
            final MDArray<String> data, final HDF5GenericStorageFeatures features);
}
