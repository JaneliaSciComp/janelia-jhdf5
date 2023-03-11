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
 * An interface that provides methods for reading enumeration values from HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#enumeration()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5EnumReader extends IHDF5EnumTypeRetriever
{
    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Reads an <code>enum</code> attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute value read from the data set as a String.
     * @throws HDF5JavaException If the attribute is not an enum type.
     */
    public String getAttrAsString(final String objectPath, final String attributeName)
            throws HDF5JavaException;

    /**
     * Reads an <code>enum</code> attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute value read from the data set.
     * @throws HDF5JavaException If the attribute is not an enum type.
     */
    public HDF5EnumerationValue getAttr(final String objectPath, final String attributeName)
            throws HDF5JavaException;

    /**
     * Reads an <code>enum</code> attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @param enumClass the {@link Enum} class to represent the values of.
     * @return The attribute value read from the data set.
     * @throws HDF5JavaException If the attribute is not an enum type.
     */
    public <T extends Enum<T>> T getAttr(final String objectPath,
            final String attributeName, Class<T> enumClass) throws HDF5JavaException;

    /**
     * Reads an <code>enum</code> array (of rank 1) attribute named <var>attributeName</var> from
     * the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute values as read from the data set.
     * @throws HDF5JavaException If the attribute is not an enum type.
     */
    public HDF5EnumerationValueArray getArrayAttr(final String objectPath,
            final String attributeName) throws HDF5JavaException;

    /**
     * Reads an <code>enum</code> array (of rank 1) attribute named <var>attributeName</var> from
     * the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute values as read from the data set.
     * @throws HDF5JavaException If the attribute is not an enum type.
     */
    public HDF5EnumerationValueMDArray getMDArrayAttr(final String objectPath,
            final String attributeName) throws HDF5JavaException;

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Reads an <code>Enum</code> value from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set as a String.
     * @throws HDF5JavaException If the <var>objectPath</var> is not an enum type.
     */
    public String readAsString(final String objectPath) throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> value from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not of <var>enumType</var>.
     */
    public HDF5EnumerationValue read(final String objectPath) throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> value from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param enumClass the {@link Enum} class to represent the values of.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not of <var>enumType</var> or if
     *             <var>enumClass</var> is incompatible with the HDF5 enumeration type of
     *             <var>objectPath</var>.
     */
    public <T extends Enum<T>> T read(final String objectPath, Class<T> enumClass)
            throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> value from the data set <var>objectPath</var>.
     * <p>
     * This method is faster than {@link #read(String)} if the {@link HDF5EnumerationType} is
     * already available.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param enumType The enum type in the HDF5 file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not of <var>enumType</var>.
     */
    public HDF5EnumerationValue read(final String objectPath, final HDF5EnumerationType enumType)
            throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> value from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param enumType The enumeration type of this array.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not of <var>enumType</var>.
     */
    public HDF5EnumerationValueArray readArray(final String objectPath,
            final HDF5EnumerationType enumType) throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> value array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not of <var>enumType</var>.
     */
    public HDF5EnumerationValueArray readArray(final String objectPath)
            throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> value array block from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the return value of the
     *            {@link HDF5EnumerationValueArray#getLength()} returned if the data set is long
     *            enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @return The data read from the data set. The length will be min(size - blockSize*blockNumber,
     *         blockSize).
     */
    public HDF5EnumerationValueArray readArrayBlock(final String objectPath,
            final int blockSize, final long blockNumber);

    /**
     * Reads an <code>Enum</code> value array block from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param enumType The enumeration type of this array.
     * @param blockSize The block size (this will be the return value of the
     *            {@link HDF5EnumerationValueArray#getLength()} returned if the data set is long
     *            enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @return The data read from the data set. The length will be min(size - blockSize*blockNumber,
     *         blockSize).
     */
    public HDF5EnumerationValueArray readArrayBlock(final String objectPath,
            final HDF5EnumerationType enumType, final int blockSize, final long blockNumber);

    /**
     * Reads an <code>Enum</code> value array block from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the return value of the
     *            {@link HDF5EnumerationValueArray#getLength()} returned if the data set is long
     *            enough).
     * @param offset The offset of the block in the data set to start reading from (starting with
     *            0).
     * @return The data read from the data set. The length will be min(size - blockSize*blockNumber,
     *         blockSize).
     */
    public HDF5EnumerationValueArray readArrayBlockWithOffset(final String objectPath,
            final int blockSize, final long offset);

    /**
     * Reads an <code>Enum</code> value array block from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param enumType The enumeration type of this array.
     * @param blockSize The block size (this will be the return value of the
     *            {@link HDF5EnumerationValueArray#getLength()} returned if the data set is long
     *            enough).
     * @param offset The offset of the block in the data set to start reading from (starting with
     *            0).
     * @return The data read from the data set. The length will be min(size - blockSize*blockNumber,
     *         blockSize).
     */
    public HDF5EnumerationValueArray readArrayBlockWithOffset(final String objectPath,
            final HDF5EnumerationType enumType, final int blockSize, final long offset);

    /**
     * Reads an <code>Enum</code> array (of rank N) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not of <var>enumType</var>.
     */
    public HDF5EnumerationValueMDArray readMDArray(final String objectPath)
            throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> array (of rank N) from the data set <var>objectPath</var>.
     * <p>
     * This method is faster than {@link #read(String)} if the {@link HDF5EnumerationType} is
     * already available.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param enumType The enum type in the HDF5 file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not of <var>enumType</var>.
     */
    public HDF5EnumerationValueMDArray readMDArray(final String objectPath,
            final HDF5EnumerationType enumType) throws HDF5JavaException;

    /**
     * Reads a block from a <code>Enum</code> array block (of rank N) from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this enumeration type.
     * @param blockDimensions The extent of the block to write along each axis.
     * @param blockNumber The number of the block to write along each axis.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not an enum data set.
     */
    public HDF5EnumerationValueMDArray readMDArrayBlock(final String objectPath,
            final HDF5EnumerationType type, final int[] blockDimensions, final long[] blockNumber)
            throws HDF5JavaException;

    /**
     * Reads a block from a <code>Enum</code> array block (of rank N) from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block to write along each axis.
     * @param blockNumber The number of the block to write along each axis.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not an enum data set.
     */
    public HDF5EnumerationValueMDArray readMDArrayBlock(final String objectPath,
            final int[] blockDimensions, final long[] blockNumber) throws HDF5JavaException;

    /**
     * Reads a block from a <code>Enum</code> array block (of rank N) from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this <code>Enum</code> type.
     * @param blockDimensions The extent of the block to write along each axis.
     * @param offset The offset of the block to write in the data set along each axis.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not an enum data set.
     */
    public HDF5EnumerationValueMDArray readMDArrayBlockWithOffset(final String objectPath,
            final HDF5EnumerationType type, final int[] blockDimensions, final long[] offset)
            throws HDF5JavaException;

    /**
     * Reads a block from a <code>Enum</code> array block (of rank N) from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block to write along each axis.
     * @param offset The offset of the block to write in the data set along each axis.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not an enum data set.
     */
    public HDF5EnumerationValueMDArray readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this one-dimensional data set to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1.
     */
    public Iterable<HDF5DataBlock<HDF5EnumerationValueArray>> getArrayBlocks(
            final String objectPath) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this one-dimensional data set to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param enumType The enumeration type of this array.
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1.
     */
    public Iterable<HDF5DataBlock<HDF5EnumerationValueArray>> getArrayBlocks(
            final String objectPath, final HDF5EnumerationType enumType) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this enum type.
     * @see HDF5MDDataBlock
     * @throws HDF5JavaException If the data set is not an enum data set.
     */
    public Iterable<HDF5MDEnumBlock> getMDArrayBlocks(final String objectPath,
            final HDF5EnumerationType type) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @see HDF5MDDataBlock
     * @throws HDF5JavaException If the data set is not an enum data set.
     */
    public Iterable<HDF5MDEnumBlock> getMDArrayBlocks(final String objectPath)
            throws HDF5JavaException;

}
