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

import ch.systemsx.cisd.base.mdarray.MDByteArray;

/**
 * An interface that provides methods for writing <code>byte</code> values to HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Writer#int8()} or {@link IHDF5Writer#uint8()}.
 * <p>
 * <i>Note:</i> This interface supports block access and sliced access (which is a special cases of 
 * block access) to arrays. The performance of this block access can vary greatly depending on how 
 * the data are layed out in the HDF5 file. For best performance, the block (or slice) dimension should 
 * be chosen to be equal to the chunk dimensions of the array, as in this case the block written / read 
 * are stored as consecutive value in the HDF5 file and one write / read access will suffice.
 * <p>   
 * <i>Note:</i> If you need to convert from and to unsigned values, use the methods of 
 * {@link UnsignedIntUtils}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5ByteWriter extends IHDF5ByteReader
{
    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Set a <code>byte</code> attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setAttr(String objectPath, String name, byte value);

    /**
     * Set a <code>byte[]</code> attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setArrayAttr(String objectPath, String name, byte[] value);

    /**
     * Set a multi-dimensional code>byte</code> attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setMDArrayAttr(String objectPath, String name, MDByteArray value);

    /**
     * Set a <code>byte[][]</code> attribute on the referenced object.
     * <p>
     * The referenced object must exist, that is it need to have been written before by one of the
     * <code>write()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void setMatrixAttr(String objectPath, String name, byte[][] value);
    
    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Writes out a <code>byte</code> value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param value The value to write.
     */
    public void write(String objectPath, byte value);

    /**
     * Writes out a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeArray(String objectPath, byte[] data);

    /**
     * Writes out a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param features The storage features of the data set.
     */
    public void writeArray(String objectPath, byte[] data, 
            HDF5IntStorageFeatures features);

    /**
     * Writes out a <code>byte</code> array (of rank 1). When creating many data sets with the same
     * features, this method will be faster than
     * {@link #writeArray(String, byte[], HDF5IntStorageFeatures)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param template The template to be used to determine the features of the data set.
     * @throws hdf.hdf5lib.exceptions.HDF5LibraryException If a data set with name <code>objectPath</code> already exists.
     */
    public void writeArray(String objectPath, byte[] data, HDF5DataSetTemplate template);

    /**
     * Creates a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size When the writer is configured to use extendable data types (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial size
     *            and the chunk size of the array will be <var>size</var>. When the writer is
     *            configured to <i>enforce</i> a non-extendable data set, the initial size equals the
     *            total size and will be <var>size</var>.
     */
    public void createArray(String objectPath, int size);

    /**
     * Creates a <code>byte</code> array (of rank 1) and opens it for reading and writing.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size When the writer is configured to use extendable data types (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial size
     *            and the chunk size of the array will be <var>size</var>. When the writer is
     *            configured to <i>enforce</i> a non-extendable data set, the initial size equals the
     *            total size and will be <var>size</var>.
     * @return The created data set.
     */
    public HDF5DataSet createArrayAndOpen(String objectPath, int size);

    /**
     * Creates a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the byte array to create. When using extendable data sets 
     *          ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data 
     *          set smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if non-extendable data 
     *          sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}).
     */
    public void createArray(String objectPath, long size, int blockSize);

    /**
     * Creates a <code>byte</code> array (of rank 1) and opens it for reading and writing.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size When the writer is configured to use extendable data types (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial size
     *            and the chunk size of the array will be <var>size</var>. When the writer is
     *            configured to <i>enforce</i> a non-extendable data set, the initial size equals the
     *            total size and will be <var>size</var>.
     * @param blockSize The size of one block (for block-wise IO). Ignored if non-extendable data 
     *          sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}).
     * @return The created data set.
     */
    public HDF5DataSet createArrayAndOpen(String objectPath, long size, int blockSize);

    /**
     * Creates a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the <code>byte</code> array to create. When <i>requesting</i> a 
     *            chunked data set (e.g. {@link HDF5IntStorageFeatures#INT_CHUNKED}), 
     *            the initial size of the array will be 0 and the chunk size will be <var>arraySize</var>. 
     *            When <i>allowing</i> a chunked data set (e.g. 
     *            {@link HDF5IntStorageFeatures#INT_NO_COMPRESSION} when the writer is 
     *            not configured to avoid extendable data types, see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial size
     *            and the chunk size of the array will be <var>arraySize</var>. When <i>enforcing</i> a 
     *            on-extendable data set (e.g. 
     *            {@link HDF5IntStorageFeatures#INT_CONTIGUOUS}), the initial size equals 
     *            the total size and will be <var>arraySize</var>.
     * @param features The storage features of the data set.
     */
    public void createArray(String objectPath, int size,
            HDF5IntStorageFeatures features);
    
    /**
     * Creates a <code>byte</code> array (of rank 1) and opens it for reading and writing.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the <code>int</code> array to create. When <i>requesting</i> a 
     *            chunked data set (e.g. {@link HDF5IntStorageFeatures#INT_CHUNKED}), 
     *            the initial size of the array will be 0 and the chunk size will be <var>arraySize</var>. 
     *            When <i>allowing</i> a chunked data set (e.g. 
     *            {@link HDF5IntStorageFeatures#INT_NO_COMPRESSION} when the writer is 
     *            not configured to avoid extendable data types, see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial size
     *            and the chunk size of the array will be <var>arraySize</var>. When <i>enforcing</i> a 
     *            on-extendable data set (e.g. 
     *            {@link HDF5IntStorageFeatures#INT_CONTIGUOUS}), the initial size equals 
     *            the total size and will be <var>arraySize</var>.
     * @param features The storage features of the data set.
     * @return The created data set.
     */
    public HDF5DataSet createArrayAndOpen(String objectPath, int size,
            HDF5IntStorageFeatures features);
    
    /**
     * Creates a <code>byte</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the int array to create. When using extendable data sets 
     *          ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data 
     *          set smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data 
     *          sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}) and 
     *                <code>features</code> is <code>HDF5IntStorageFeature.INT_NO_COMPRESSION</code>.
     * @param features The storage features of the data set.
     */
    public void createArray(String objectPath, long size, int blockSize,
            HDF5IntStorageFeatures features);

    /**
     * Creates a <code>byte</code> array (of rank 1) and opens it for reading and writing.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the int array to create. When using extendable data sets 
     *          ((see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data 
     *          set smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data 
     *          sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}) and 
     *                <code>features</code> is <code>HDF5IntStorageFeature.INT_NO_COMPRESSION</code>.
     * @param features The storage features of the data set.
     * @return The created data set.
     */
    public HDF5DataSet createArrayAndOpen(String objectPath, long size, int blockSize,
            HDF5IntStorageFeatures features);
    
    /**
     * Creates a <code>byte</code> array (of rank 1). When creating many data sets with the same
     * features, this method will be faster than
     * {@link #createArray(String, long, int, HDF5IntStorageFeatures)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param template The template to be used to determine the features of the data set.
     * @throws hdf.hdf5lib.exceptions.HDF5LibraryException If a data set with name <code>objectPath</code> already exists.
     */
    public void createArray(String objectPath, HDF5DataSetTemplate template);

    /**
     * Creates a <code>byte</code> array (of rank 1) and opens it for reading and writing.
     * When creating many data sets with the same features, this method will be faster than
     * {@link #createArrayAndOpen(String, long, int, HDF5IntStorageFeatures)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param template The template to be used to determine the features of the data set.
     * @return The created data set.
     * @throws hdf.hdf5lib.exceptions.HDF5LibraryException If a data set with name 
     *         <code>objectPath</code> already exists.
     */
    public HDF5DataSet createArrayAndOpen(String objectPath, HDF5DataSetTemplate template);
    
    /**
     * Creates a <code>byte</code> array template (of rank 1) which can be used in
     * {@link #writeArray(String, byte[], HDF5DataSetTemplate)} to create data sets.
     * It is meant to be used when creating many data sets with the same features.
     * <p>
     * <i>The template returned by this method must be closed after work, as otherwise resources of
     * the HDF5 library are leaked.</i>
     * 
     * @param size The size of the byte array to be created. When using extendable data sets ((see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data set
     *            smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})
     *            and <code>features</code> is <code>HDF5IntStorageFeature.INT_NO_COMPRESSION</code>
     *            .
     * @param features The storage features of the data set.
     */
    public HDF5DataSetTemplate createArrayTemplate(long size, int blockSize,
            HDF5IntStorageFeatures features);

    /**
     * Writes out a block of a <code>byte</code> array (of rank 1). The data set needs to have
     * been created by {@link #createArray(String, long, int, HDF5IntStorageFeatures)}
     * beforehand.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param blockNumber The number of the block to write.
     */
    public void writeArrayBlock(String objectPath, byte[] data,
            long blockNumber);

    /**
     * Writes out a block of a <code>byte</code> array (of rank 1). The data set needs to have
     * been created by {@link #createArray(String, long, int, HDF5IntStorageFeatures)}
     * beforehand.
     * <p>
     * <i>This method is faster than {@link #writeArrayBlock(String, byte[], long)} 
     * when called many times on the same data set.</i>
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param blockNumber The number of the block to write.
     */
    public void writeArrayBlock(HDF5DataSet dataSet, byte[] data,
            long blockNumber);

    /**
     * Writes out a block of a <code>byte</code> array (of rank 1). The data set needs to have
     * been created by {@link #createArray(String, long, int, HDF5IntStorageFeatures)}
     * beforehand.
     * <p>
     * Use this method instead of {@link #writeArrayBlock(String, byte[], long)} if the
     * total size of the data set is not a multiple of the block size.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param dataSize The (real) size of <code>data</code> (needs to be <code><= data.length</code>
     *            )
     * @param offset The offset in the data set to start writing to.
     */
    public void writeArrayBlockWithOffset(String objectPath, byte[] data,
            int dataSize, long offset);

    /**
     * Writes out a block of a <code>byte</code> array (of rank 1). The data set needs to have
     * been created by {@link #createArray(String, long, int, HDF5IntStorageFeatures)}
     * beforehand.
     * <p>
     * Use this method instead of {@link #writeArrayBlock(HDF5DataSet, byte[], long)} if the
     * total size of the data set is not a multiple of the block size.
     * <p>
     * <i>This method is faster than {@link #writeArrayBlockWithOffset(String, byte[], int, long)} 
     * when called many times on the same data set.</i>
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param offset The offset in the data set to start writing to.
     */
    public void writeArrayBlockWithOffset(HDF5DataSet dataSet, byte[] data,
            int dataSize, long offset);
            
    /**
     * Writes out a <code>byte</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     */
    public void writeMatrix(String objectPath, byte[][] data);

    /**
     * Writes out a <code>byte</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param features The storage features of the data set.
     */
    public void writeMatrix(String objectPath, byte[][] data, 
            HDF5IntStorageFeatures features);

    /**
     * Creates a <code>byte</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param sizeX The size of one block in the x dimension. See
     *            {@link #createMDArray(String, int[])} on the different
     *            meanings of this parameter.
     * @param sizeY The size of one block in the y dimension. See
     *            {@link #createMDArray(String, int[])} on the different
     *            meanings of this parameter.
     */
    public void createMatrix(String objectPath, int sizeX, int sizeY);

    /**
     * Creates a <code>byte</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param sizeX The size of one block in the x dimension. See
     *            {@link #createMDArray(String, int[], HDF5IntStorageFeatures)} on the different
     *            meanings of this parameter.
     * @param sizeY The size of one block in the y dimension. See
     *            {@link #createMDArray(String, int[], HDF5IntStorageFeatures)} on the different
     *            meanings of this parameter.
     * @param features The storage features of the data set.
     */
    public void createMatrix(String objectPath, int sizeX, int sizeY,
    		HDF5IntStorageFeatures features);

    /**
     * Creates a <code>byte</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param sizeX The size of the x dimension of the byte matrix to create.
     * @param sizeY The size of the y dimension of the byte matrix to create.
     * @param blockSizeX The size of one block in the x dimension.
     * @param blockSizeY The size of one block in the y dimension.
     */
    public void createMatrix(String objectPath, long sizeX, long sizeY,
            int blockSizeX, int blockSizeY);

    /**
     * Creates a <code>byte</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param sizeX The size of the x dimension of the byte matrix to create.
     * @param sizeY The size of the y dimension of the byte matrix to create.
     * @param blockSizeX The size of one block in the x dimension.
     * @param blockSizeY The size of one block in the y dimension.
     * @param features The storage features of the data set.
     */
    public void createMatrix(String objectPath, long sizeX, long sizeY,
            int blockSizeX, int blockSizeY, HDF5IntStorageFeatures features);

    /**
     * Writes out a block of a <code>byte</code> matrix (array of rank 2). The data set needs to
     * have been created by
     * {@link #createMatrix(String, long, long, int, int, HDF5IntStorageFeatures)} beforehand.
     * <p>
     * Use this method instead of
     * {@link #createMatrix(String, long, long, int, int, HDF5IntStorageFeatures)} if the total
     * size of the data set is not a multiple of the block size.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. The length defines the block size. Must not be
     *            <code>null</code> or of length 0.
     * @param blockNumberX The block number in the x dimension (offset: multiply with
     *            <code>data.length</code>).
     * @param blockNumberY The block number in the y dimension (offset: multiply with
     *            <code>data[0.length</code>).
     */
    public void writeMatrixBlock(String objectPath, byte[][] data,
            long blockNumberX, long blockNumberY);

    /**
     * Writes out a block of a <code>byte</code> matrix (array of rank 2). The data set needs to
     * have been created by
     * {@link #createMatrix(String, long, long, int, int, HDF5IntStorageFeatures)} beforehand.
     * <p>
     * Use this method instead of {@link #writeMatrixBlock(String, byte[][], long, long)} if
     * the total size of the data set is not a multiple of the block size.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write.
     * @param offsetX The x offset in the data set to start writing to.
     * @param offsetY The y offset in the data set to start writing to.
     */
    public void writeMatrixBlockWithOffset(String objectPath, byte[][] data,
            long offsetX, long offsetY);

    /**
     * Writes out a block of a <code>byte</code> matrix (array of rank 2). The data set needs to
     * have been created by
     * {@link #createMatrix(String, long, long, int, int, HDF5IntStorageFeatures)} beforehand.
     * <p>
     * Use this method instead of {@link #writeMatrixBlock(String, byte[][], long, long)} if
     * the total size of the data set is not a multiple of the block size.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write.
     * @param dataSizeX The (real) size of <code>data</code> along the x axis (needs to be
     *            <code><= data.length</code> )
     * @param dataSizeY The (real) size of <code>data</code> along the y axis (needs to be
     *            <code><= data[0].length</code> )
     * @param offsetX The x offset in the data set to start writing to.
     * @param offsetY The y offset in the data set to start writing to.
     */
    public void writeMatrixBlockWithOffset(String objectPath, byte[][] data,
            int dataSizeX, int dataSizeY, long offsetX, long offsetY);

    /**
     * Writes out a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     */
    public void writeMDArray(String objectPath, MDByteArray data);

    /**
     * Writes out a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param features The storage features of the data set.
     */
    public void writeMDArray(String objectPath, MDByteArray data,
            HDF5IntStorageFeatures features);

    /**
     * Writes out a block or a slice of a multi-dimensional <code>byte</code> array.
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. Must not be <code>null</code>.
     * @param params The parameter block specifying the block or slice to read from the array.
     */
    public void writeMDArray(HDF5DataSet dataSet, MDByteArray data,
            HDF5ArrayBlockParams params);
    
    /**
     * Writes out a multi-dimensional <code>byte</code> array. When creating many data sets with the same
     * features, this method will be faster than
     * {@link #writeMDArray(String, MDByteArray, HDF5IntStorageFeatures)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param template The template to be used to determine the features of the data set.
     * @throws hdf.hdf5lib.exceptions.HDF5LibraryException If a data set with name <code>objectPath</code> already exists.
     */
    public void writeMDArray(String objectPath, MDByteArray data, HDF5DataSetTemplate template);

    /**
     * Writes out a slice of a multi-dimensional <code>byte</code> array. The slice is defined by
     * "bound indices", each of which is fixed to a given value. The <var>data</var> object only  
     * contains the free (i.e. non-fixed) indices.
     * <p> 
     * <i>Note:</i>The object identified by <var>objectPath</var> needs to exist when this method is 
     * called. This method will <i>not</i> create the array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     */
    public void writeMDArraySlice(String objectPath, MDByteArray data, IndexMap boundIndices);

    /**
     * Writes out a slice of a multi-dimensional <code>byte</code> array. The slice is defined by
     * "bound indices", each of which is fixed to a given value. The <var>data</var> object only  
     * contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     */
    public void writeMDArraySlice(HDF5DataSet dataSet, MDByteArray data, IndexMap boundIndices);
    
    /**
     * Writes out a slice of a multi-dimensional <code>byte</code> array. The slice is defined by
     * "bound indices", each of which is fixed to a given value. The <var>data</var> object only  
     * contains the free (i.e. non-fixed) indices.
     * <p> 
     * <i>Note:</i>The object identified by <var>objectPath</var> needs to exist when this method is 
     * called. This method will <i>not</i> create the array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public void writeMDArraySlice(String objectPath, MDByteArray data, long[] boundIndices);

    /**
     * Writes out a slice of a multi-dimensional <code>byte</code> array. The slice is defined by
     * "bound indices", each of which is fixed to a given value. The <var>data</var> object only  
     * contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public void writeMDArraySlice(HDF5DataSet dataSet, MDByteArray data, long[] boundIndices);
    
    /**
     * Creates a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions When the writer is configured to use extendable data types (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial dimensions
     *            and the dimensions of a chunk of the array will be <var>dimensions</var>. When the 
     *            writer is configured to <i>enforce</i> a non-extendable data set, the initial dimensions 
     *            equal the dimensions and will be <var>dimensions</var>.
     */
    public void createMDArray(String objectPath, int[] dimensions);

    /**
     * Creates a multi-dimensional <code>byte</code> array and opens it for reading and writing.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions When the writer is configured to use extendable data types (see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}), the initial dimensions
     *            and the dimensions of a chunk of the array will be <var>dimensions</var>. When the 
     *            writer is configured to <i>enforce</i> a non-extendable data set, the initial dimensions 
     *            equal the dimensions and will be <var>dimensions</var>.
     * @return The created data set.
     */
    public HDF5DataSet createMDArrayAndOpen(String objectPath, int[] dimensions);
    
    /**
     * Creates a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array.
     * @param blockDimensions The dimensions of one block (chunk) of the array.
     */
    public void createMDArray(String objectPath, long[] dimensions,
            int[] blockDimensions);

    /**
     * Creates a multi-dimensional <code>byte</code> array and opens it for reading and writing.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array.
     * @param blockDimensions The dimensions of one block (chunk) of the array.
     * @return the created data set.
     */
    public HDF5DataSet createMDArrayAndOpen(String objectPath, long[] dimensions,
            int[] blockDimensions);
    
    /**
     * Creates a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the <code>byte</code> array to create. When <i>requesting</i> 
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
            HDF5IntStorageFeatures features);

    /**
     * Creates a multi-dimensional <code>byte</code> array and opens it for reading and writing.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the <code>byte</code> array to create. When <i>requesting</i> 
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
     * @return The created data set.
     */
    public HDF5DataSet createMDArrayAndOpen(String objectPath, int[] dimensions,
            HDF5IntStorageFeatures features);
        
    /**
     * Creates a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array.
     * @param blockDimensions The dimensions of one block (chunk) of the array.
     * @param features The storage features of the data set.
     */
    public void createMDArray(String objectPath, long[] dimensions,
            int[] blockDimensions, HDF5IntStorageFeatures features);

    /**
     * Creates a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array.
     * @param blockDimensions The dimensions of one block (chunk) of the array.
     * @param features The storage features of the data set.
     * @return The created data set.
     */
    public HDF5DataSet createMDArrayAndOpen(String objectPath, long[] dimensions,
            int[] blockDimensions, HDF5IntStorageFeatures features);
    
    /**
     * Creates a multi-dimensional <code>byte</code> array. When creating many data sets with the same
     * features, this method will be faster than
     * {@link #createMDArray(String, long[], int[], HDF5IntStorageFeatures)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param template The template to be used to determine the features of the data set.
     * @throws hdf.hdf5lib.exceptions.HDF5LibraryException If a data set with name <code>objectPath</code> already exists.
     */
    public void createMDArray(String objectPath, HDF5DataSetTemplate template);

    /**
     * Creates a multi-dimensional <code>byte</code> array and opens it for reading and writing. 
     * When creating many data sets with the same features, this method will be faster than
     * {@link #createMDArrayAndOpen(String, long[], int[], HDF5IntStorageFeatures)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param template The template to be used to determine the features of the data set.
     * @throws hdf.hdf5lib.exceptions.HDF5LibraryException If a data set with name <code>objectPath</code> already exists.
     */
    public HDF5DataSet createMDArrayAndOpen(String objectPath, HDF5DataSetTemplate template);

    /**
     * Creates a multi-dimensional <code>byte</code> array template which can be used e.g. in
     * {@link #writeArray(String, byte[], HDF5DataSetTemplate)} to create data sets.
     * It is meant to be used when creating many data sets with the same features.
     * <p>
     * <i>The template returned by this method must be closed after work, as otherwise resources of
     * the HDF5 library are leaked.</i>
     * 
     * @param dimensions The dimensions of the array. When using extendable data sets ((see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data set
     *            smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})
     *            and <code>features</code> doesn't enforce them.
     * @param features The storage features of the data set.
     */
    public HDF5DataSetTemplate createMDArrayTemplate(long[] dimensions, int[] blockSize,
            HDF5IntStorageFeatures features);

    /**
     * Writes out a block of a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param blockNumber The block number in each dimension (offset: multiply with the extend in
     *            the according dimension).
     */
    public void writeMDArrayBlock(String objectPath, MDByteArray data,
            long[] blockNumber);

    /**
     * Writes out a block of a multi-dimensional <code>byte</code> array.
     * 
     * @param dataset The data set to write to.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param blockNumber The block number in each dimension (offset: multiply with the extend in
     *            the according dimension).
     */
    public void writeMDArrayBlock(HDF5DataSet dataset, MDByteArray data,
            long[] blockNumber);
    
    /**
     * Writes out a sliced block of a multi-dimensional <code>byte</code> array. The slice is
     * defined by "bound indices", each of which is fixed to a given value. The <var>data</var> 
     * object only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param blockNumber The block number in each dimension (offset: multiply with the extend in
     *            the according dimension).
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public void writeSlicedMDArrayBlock(String objectPath, MDByteArray data, long[] blockNumber,
            IndexMap boundIndices);

    /**
     * Writes out a sliced block of a multi-dimensional <code>byte</code> array. The slice is
     * defined by "bound indices", each of which is fixed to a given value. The <var>data</var> 
     * object only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataset The data set to write to.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param blockNumber The block number in each dimension (offset: multiply with the extend in
     *            the according dimension).
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public void writeSlicedMDArrayBlock(HDF5DataSet dataset, MDByteArray data, long[] blockNumber,
            IndexMap boundIndices);

    /**
     * Writes out a sliced block of a multi-dimensional <code>byte</code> array. The slice is
     * defined by "bound indices", each of which is fixed to a given value. The <var>data</var> 
     * object only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param blockNumber The block number in each dimension (offset: multiply with the extend in
     *            the according dimension).
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     */
    public void writeSlicedMDArrayBlock(String objectPath, MDByteArray data, long[] blockNumber,
            long[] boundIndices);

    /**
     * Writes out a sliced block of a multi-dimensional <code>byte</code> array. The slice is
     * defined by "bound indices", each of which is fixed to a given value. The <var>data</var> 
     * object only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataset The data set to write to.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param blockNumber The block number in each dimension (offset: multiply with the extend in
     *            the according dimension).
     * @param boundIndices The mapping of indices to index values which should be bound. For example
     *            a map of <code>new IndexMap().mapTo(2, 5).mapTo(4, 7)</code> has 2 and 4 as bound
     *            indices and binds them to the values 5 and 7, respectively.
     */
    public void writeSlicedMDArrayBlock(HDF5DataSet dataset, MDByteArray data, long[] blockNumber,
            long[] boundIndices);

    /**
     * Writes out a block of a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param offset The offset in the data set  to start writing to in each dimension.
     */
    public void writeMDArrayBlockWithOffset(String objectPath, MDByteArray data,
            long[] offset);

    /**
     * Writes out a block of a multi-dimensional <code>byte</code> array.
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param offset The offset in the data set  to start writing to in each dimension.
     */
    public void writeMDArrayBlockWithOffset(HDF5DataSet dataSet, MDByteArray data,
            long[] offset);
    
    /**
     * Writes out a sliced block of a multi-dimensional <code>byte</code> array. The slice is
     * defined by "bound indices", each of which is fixed to a given value. The <var>data</var> 
     * object only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param offset The offset in the data set to start writing to in each dimension.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public void writeSlicedMDArrayBlockWithOffset(String objectPath, MDByteArray data,
            long[] offset, IndexMap boundIndices);

    /**
     * Writes out a sliced block of a multi-dimensional <code>byte</code> array. The slice is
     * defined by "bound indices", each of which is fixed to a given value. The <var>data</var> 
     * object only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param offset The offset in the data set to start writing to in each dimension.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public void writeSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, MDByteArray data,
            long[] offset, IndexMap boundIndices);

    /**
     * Writes out a sliced block of a multi-dimensional <code>byte</code> array. The slice is
     * defined by "bound indices", each of which is fixed to a given value. The <var>data</var> 
     * object only contains the free (i.e. non-fixed) indices.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param offset The offset in the data set to start writing to in each dimension.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public void writeSlicedMDArrayBlockWithOffset(String objectPath, MDByteArray data,
            long[] offset, long[] boundIndices);

    /**
     * Writes out a sliced block of a multi-dimensional <code>byte</code> array. The slice is
     * defined by "bound indices", each of which is fixed to a given value. The <var>data</var> 
     * object only contains the free (i.e. non-fixed) indices.
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     * @param offset The offset in the data set to start writing to in each dimension.
     * @param boundIndices The array containing the values of the bound indices at the respective
     *            index positions, and -1 at the free index positions. For example an array of
     *            <code>new long[] { -1, -1, 5, -1, 7, -1 }</code> has 2 and 4 as bound indices and
     *            binds them to the values 5 and 7, respectively.
     */
    public void writeSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, MDByteArray data,
            long[] offset, long[] boundIndices);

   /**
     * Writes out a block of a multi-dimensional <code>byte</code> array.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     * @param blockDimensions The dimensions of the block to write to the data set.
     * @param offset The offset of the block in the data set to start writing to in each dimension.
     * @param memoryOffset The offset of the block in the <var>data</var> array.
     */
    public void writeMDArrayBlockWithOffset(String objectPath, MDByteArray data,
            int[] blockDimensions, long[] offset, int[] memoryOffset);

   /**
     * Writes out a block of a multi-dimensional <code>byte</code> array.
     * 
     * @param dataSet The data set to write to.
     * @param data The data to write. Must not be <code>null</code>.
     * @param blockDimensions The dimensions of the block to write to the data set.
     * @param offset The offset of the block in the data set to start writing to in each dimension.
     * @param memoryOffset The offset of the block in the <var>data</var> array.
     */
    public void writeMDArrayBlockWithOffset(HDF5DataSet dataSet, MDByteArray data,
            int[] blockDimensions, long[] offset, int[] memoryOffset);
}
