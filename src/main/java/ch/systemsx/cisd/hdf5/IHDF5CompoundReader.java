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

import ch.systemsx.cisd.base.mdarray.MDArray;

/**
 * An interface that provides methods for reading compound values from HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#compound()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5CompoundReader extends IHDF5CompoundInformationRetriever
{

    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Reads a compound attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the object in the file.
     * @param type The type definition of this compound type.
     * @return The data read from the attribute.
     * @throws HDF5JavaException If the <var>attributeName</var> is not a compound attribute.
     */
    public <T> T getAttr(String objectPath, String attributeName, HDF5CompoundType<T> type)
            throws HDF5JavaException;

    /**
     * Reads a compound attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @return The data read from the attribute.
     * @throws HDF5JavaException If the <var>attributeName</var> is not a compound attribute.
     */
    public <T> T getAttr(String objectPath, String attributeName, Class<T> pojoClass)
            throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the object in the file.
     * @param type The type definition of this compound type.
     * @return The data read from the attribute.
     * @throws HDF5JavaException If the <var>attributeName</var> is not a compound attribute.
     */
    public <T> T[] getArrayAttr(String objectPath, String attributeName, HDF5CompoundType<T> type)
            throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @return The data read from the attribute.
     * @throws HDF5JavaException If the <var>attributeName</var> is not a compound attribute.
     */
    public <T> T[] getArrayAttr(String objectPath, String attributeName, Class<T> pojoClass)
            throws HDF5JavaException;

    /**
     * Reads a compound array (of rank N) attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the object in the file.
     * @param type The type definition of this compound type.
     * @return The data read from the attribute.
     * @throws HDF5JavaException If the <var>attributeName</var> is not a compound attribute.
     */
    public <T> MDArray<T> getMDArrayAttr(String objectPath, String attributeName,
            HDF5CompoundType<T> type) throws HDF5JavaException;

    /**
     * Reads a compound array (of rank N) attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @return The data read from the attribute.
     * @throws HDF5JavaException If the <var>attributeName</var> is not a compound attribute.
     */
    public <T> MDArray<T> getMDArrayAttr(String objectPath, String attributeName, Class<T> pojoClass)
            throws HDF5JavaException;

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Reads a compound from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> T read(String objectPath, HDF5CompoundType<T> type) throws HDF5JavaException;

    /**
     * Reads a compound from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set or if the
     *             mapping between the compound type and the POJO is not complete.
     * @see CompoundType
     * @see CompoundElement
     */
    public <T> T read(String objectPath, Class<T> pojoClass) throws HDF5JavaException;

    /**
     * Reads a compound from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into a Java object.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> T read(String objectPath, HDF5CompoundType<T> type,
            IByteArrayInspector inspectorOrNull) throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> T[] readArray(String objectPath, HDF5CompoundType<T> type) throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into Java objects.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> T[] readArray(String objectPath, HDF5CompoundType<T> type,
            IByteArrayInspector inspectorOrNull) throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set or if the
     *             mapping between the compound type and the POJO is not complete.
     * @see CompoundType
     * @see CompoundElement
     */
    public <T> T[] readArray(String objectPath, Class<T> pojoClass) throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param blockSize The block size (this will be the length of the <code>float[]</code> returned
     *            if the data set is long enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> T[] readArrayBlock(String objectPath, HDF5CompoundType<T> type, int blockSize,
            long blockNumber) throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param blockSize The block size (this will be the length of the <code>float[]</code> returned
     *            if the data set is long enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into Java objects.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> T[] readArrayBlock(String objectPath, HDF5CompoundType<T> type, int blockSize,
            long blockNumber, IByteArrayInspector inspectorOrNull) throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param blockSize The block size (this will be the length of the <code>float[]</code> returned
     *            if the data set is long enough).
     * @param offset The offset of the block to read (starting with 0).
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> T[] readArrayBlockWithOffset(String objectPath, HDF5CompoundType<T> type,
            int blockSize, long offset) throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param blockSize The block size (this will be the length of the <code>float[]</code> returned
     *            if the data set is long enough).
     * @param offset The offset of the block to read (starting with 0).
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into Java objects.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> T[] readArrayBlockWithOffset(String objectPath, HDF5CompoundType<T> type,
            int blockSize, long offset, IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException;

    /**
     * Provides all natural blocks of this one-dimensional data set of compounds to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1 or not a compound data set.
     */
    public <T> Iterable<HDF5DataBlock<T[]>> getArrayBlocks(String objectPath,
            HDF5CompoundType<T> type) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this one-dimensional data set of compounds to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into Java objects.
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1 or not a compound data set.
     */
    public <T> Iterable<HDF5DataBlock<T[]>> getArrayBlocks(String objectPath,
            HDF5CompoundType<T> type, IByteArrayInspector inspectorOrNull) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this one-dimensional data set of compounds to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1 or not a compound data set.
     * @throws HDF5JavaException If the data set is not of rank 1, not a compound data set or if the
     *             mapping between the compound type and the POJO is not complete.
     */
    public <T> Iterable<HDF5DataBlock<T[]>> getArrayBlocks(String objectPath, Class<T> pojoClass)
            throws HDF5JavaException;

    /**
     * Reads a compound array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> MDArray<T> readMDArray(String objectPath, HDF5CompoundType<T> type)
            throws HDF5JavaException;

    /**
     * Reads a compound array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set or if the
     *             mapping between the compound type and the POJO is not complete.
     * @see CompoundType
     * @see CompoundElement
     */
    public <T> MDArray<T> readMDArray(String objectPath, Class<T> pojoClass)
            throws HDF5JavaException;

    /**
     * Reads a compound array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into Java objects.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> MDArray<T> readMDArray(String objectPath, HDF5CompoundType<T> type,
            IByteArrayInspector inspectorOrNull) throws HDF5JavaException;

    /**
     * Reads a block from a compound array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param blockDimensions The extent of the block to write along each axis.
     * @param blockNumber The number of the block to write along each axis.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> MDArray<T> readMDArrayBlock(String objectPath, HDF5CompoundType<T> type,
            int[] blockDimensions, long[] blockNumber) throws HDF5JavaException;

    /**
     * Reads a block from a compound array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param blockDimensions The extent of the block to write along each axis.
     * @param blockNumber The number of the block to write along each axis.
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into Java objects.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound type.
     */
    public <T> MDArray<T> readMDArrayBlock(String objectPath, HDF5CompoundType<T> type,
            int[] blockDimensions, long[] blockNumber, IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException;

    /**
     * Reads a block from a compound array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param blockDimensions The extent of the block to write along each axis.
     * @param offset The offset of the block to write in the data set along each axis.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> MDArray<T> readMDArrayBlockWithOffset(String objectPath, HDF5CompoundType<T> type,
            int[] blockDimensions, long[] offset) throws HDF5JavaException;

    /**
     * Reads a block from a compound array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param blockDimensions The extent of the block to write along each axis.
     * @param offset The offset of the block to write in the data set along each axis.
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into Java objects.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     */
    public <T> MDArray<T> readMDArrayBlockWithOffset(String objectPath, HDF5CompoundType<T> type,
            int[] blockDimensions, long[] offset, IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException;

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @see HDF5MDDataBlock
     */
    public <T> Iterable<HDF5MDDataBlock<MDArray<T>>> getMDArrayBlocks(String objectPath,
            HDF5CompoundType<T> type) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param type The type definition of this compound type.
     * @param inspectorOrNull The inspector to be called before the byte array read from the HDF5
     *            file is translated back into Java objects.
     * @see HDF5MDDataBlock
     */
    public <T> Iterable<HDF5MDDataBlock<MDArray<T>>> getMDArrayBlocks(String objectPath,
            HDF5CompoundType<T> type, IByteArrayInspector inspectorOrNull) throws HDF5JavaException;

    /**
     * Provides all natural blocks of this multi-dimensional data set of compounds to iterate over.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array.
     * @see HDF5DataBlock
     * @see CompoundType
     * @see CompoundElement
     * @throws HDF5JavaException If the data set is not a compound data set or if the mapping
     *             between the compound type and the POJO is not complete.
     */
    public <T> Iterable<HDF5MDDataBlock<MDArray<T>>> getMDArrayBlocks(String objectPath,
            Class<T> pojoClass) throws HDF5JavaException;

}
