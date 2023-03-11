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
import ch.systemsx.cisd.base.mdarray.MDLongArray;

/**
 * An interface for writing references. 
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Writer#reference()}.
 * <p>
 * References can refer to objects or regions of datasets. This
 * version only supports object references.
 * <p>
 * <b>Note:</b> References are a low-level feature and it is easy to get dangling or even wrong
 * references by using them. If you have a choice, don't use them, but use links instead. If you
 * have to use them, e.g. to comply with a pre-defined format definition, use them with care. The
 * most important fact to know about references is that they don't keep an object alive. Once the
 * last link to the object is gone, the object is gone as well. The reference will be
 * <i>dangling</i>. If, at a later time, another object header is written to the same place in the
 * file, the reference will refer to this new object, which is most likely an undesired effect
 * (<i>wrong reference</i>). By default JHDF5 itself deletes existing datasets before writing new
 * content to a dataset of the same name, which may lead to the described problem of dangling or
 * wrong references without any explicit call to {@link IHDF5Writer#delete(String)}. Thus, HDF5
 * files with references should always be opened for writing using the
 * {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()} setting.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5ReferenceWriter extends IHDF5ReferenceReader
{

    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Sets an object reference attribute to the referenced object.
     * <p>
     * Both the object referenced with <var>objectPath</var> and <var>referencedObjectPath</var>
     * must exist, that is it need to have been written before by one of the <code>write()</code> or
     * <code>create()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param referencedObjectPath The path of the object to reference.
     */
    public void setAttr(final String objectPath, final String name,
            final String referencedObjectPath);

    /**
     * Sets a 1D object reference array attribute to referenced objects.
     * <p>
     * Both the object referenced with <var>objectPath</var> and all
     * <var>referencedObjectPaths</var> must exist, that is it need to have been written before by
     * one of the <code>write()</code> or <code>create()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param referencedObjectPaths The paths of the objects to reference.
     */
    public void setArrayAttr(final String objectPath, final String name,
            final String[] referencedObjectPaths);

    /**
     * Sets an object reference array attribute to referenced objects.
     * <p>
     * Both the object referenced with <var>objectPath</var> and all
     * <var>referencedObjectPaths</var> must exist, that is it need to have been written before by
     * one of the <code>write()</code> or <code>create()</code> methods.
     * 
     * @param objectPath The name of the object to add the attribute to.
     * @param name The name of the attribute.
     * @param referencedObjectPaths The paths of the objects to reference.
     */
    public void setMDArrayAttr(final String objectPath, final String name,
            final MDArray<String> referencedObjectPaths);

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Writes an object reference to the referenced object.
     * <p>
     * The object referenced with <var>referencedObjectPath</var> must exist, that is it need to
     * have been written before by one of the <code>write()</code> or <code>create()</code> methods.
     * 
     * @param objectPath The name of the object to write.
     * @param referencedObjectPath The path of the object to reference.
     */
    public void write(String objectPath, String referencedObjectPath);

    /**
     * Writes an array (of rank 1) of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPath The names of the object to write.
     */
    public void writeArray(final String objectPath, final String[] referencedObjectPath);

    /**
     * Writes an array (of rank 1) of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPath The names of the object to write.
     * @param features The storage features of the data set.
     */
    public void writeArray(final String objectPath, final String[] referencedObjectPath,
            final HDF5IntStorageFeatures features);

    /**
     * Creates an array (of rank 1) of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the array to create. This will be the total size for non-extendable
     *            data sets and the size of one chunk for extendable (chunked) data sets. For
     *            extendable data sets the initial size of the array will be 0, see
     *            {@link ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator#dontUseExtendableDataTypes}.
     */
    public void createArray(final String objectPath, final int size);

    /**
     * Creates an array (of rank 1) of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the array to create. When using extendable data sets ((see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data set
     *            smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}).
     */
    public void createArray(final String objectPath, final long size, final int blockSize);

    /**
     * Creates an array (of rank 1) of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the array to create. This will be the total size for non-extendable
     *            data sets and the size of one chunk for extendable (chunked) data sets. For
     *            extendable data sets the initial size of the array will be 0, see
     *            {@link HDF5IntStorageFeatures}.
     * @param features The storage features of the data set.
     */
    public void createArray(final String objectPath, final int size,
            final HDF5IntStorageFeatures features);

    /**
     * Creates an array (of rank 1) of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param size The size of the array to create. When using extendable data sets ((see
     *            {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})), then no data set
     *            smaller than this size can be created, however data sets may be larger.
     * @param blockSize The size of one block (for block-wise IO). Ignored if no extendable data
     *            sets are used (see {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()})
     *            and <code>features</code> is <code>HDF5IntStorageFeature.INTNO_COMPRESSION</code>.
     * @param features The storage features of the data set.
     */
    public void createArray(final String objectPath, final long size, final int blockSize,
            final HDF5IntStorageFeatures features);

    /**
     * Writes out a block of an array (of rank 1) of object references. The data set needs to have
     * been created by {@link #createArray(String, long, int, HDF5IntStorageFeatures)} beforehand.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createArray(String, long, int, HDF5IntStorageFeatures)} call that was used to create
     * the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPaths The paths of the referenced objects to write. The length defines
     *            the block size. Must not be <code>null</code> or of length 0.
     * @param blockNumber The number of the block to write.
     */
    public void writeArrayBlock(final String objectPath, final String[] referencedObjectPaths,
            final long blockNumber);

    /**
     * Writes out a block of an array (of rank 1) of object references. The data set needs to have
     * been created by {@link #createArray(String, long, int, HDF5IntStorageFeatures)} beforehand.
     * <p>
     * <i>Note:</i> For best performance, the block size in this method should be chosen to be equal
     * to the <var>blockSize</var> argument of the
     * {@link #createArray(String, long, int, HDF5IntStorageFeatures)} call that was used to create
     * the data set.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPaths The paths of the referenced objects to write. The length defines
     *            the block size. Must not be <code>null</code> or of length 0.
     * @param dataSize The (real) size of <code>data</code> (needs to be <code><= data.length</code>
     *            )
     * @param offset The offset in the data set to start writing to.
     */
    public void writeArrayBlockWithOffset(final String objectPath,
            final String[] referencedObjectPaths, final int dataSize, final long offset);

    /**
     * Writes an array (of rank N) of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPaths The names of the object to write.
     */
    public void writeMDArray(final String objectPath, final MDArray<String> referencedObjectPaths);

    /**
     * Writes an array (of rank N) of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPaths The names of the object to write.
     * @param features The storage features of the data set.
     */
    public void writeMDArray(final String objectPath, final MDArray<String> referencedObjectPaths,
            final HDF5IntStorageFeatures features);

    /**
     * Creates a multi-dimensional array of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array to create. This will be the total dimensions
     *            for non-extendable data sets and the dimensions of one chunk (extent along each
     *            axis) for extendable (chunked) data sets. For extendable data sets the initial
     *            size of the array along each axis will be 0, see
     *            {@link ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator#dontUseExtendableDataTypes}.
     */
    public void createMDArray(final String objectPath, final int[] dimensions);

    /**
     * Creates a multi-dimensional array of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array.
     * @param blockDimensions The dimensions of one block (chunk) of the array.
     */
    public void createMDArray(final String objectPath, final long[] dimensions,
            final int[] blockDimensions);

    /**
     * Creates a multi-dimensional array of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array to create. This will be the total dimensions
     *            for non-extendable data sets and the dimensions of one chunk (extent along each
     *            axis) for extendable (chunked) data sets. For extendable data sets the initial
     *            size of the array along each axis will be 0, see
     *            {@link ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator#dontUseExtendableDataTypes}.
     * @param features The storage features of the data set.
     */
    public void createMDArray(final String objectPath, final int[] dimensions,
            final HDF5IntStorageFeatures features);

    /**
     * Creates a multi-dimensional array of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dimensions The dimensions of the array.
     * @param blockDimensions The dimensions of one block (chunk) of the array.
     * @param features The storage features of the data set.
     */
    public void createMDArray(final String objectPath, final long[] dimensions,
            final int[] blockDimensions, final HDF5IntStorageFeatures features);

    /**
     * Writes out a block of a multi-dimensional array of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPaths The paths of the object references to write. Must not be
     *            <code>null</code>. All columns need to have the same length.
     * @param blockNumber The block number in each dimension (offset: multiply with the extend in
     *            the according dimension).
     */
    public void writeMDArrayBlock(final String objectPath,
            final MDArray<String> referencedObjectPaths, final long[] blockNumber);

    /**
     * Writes out a block of a multi-dimensional array of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPaths The paths of the object references to write. Must not be
     *            <code>null</code>.
     * @param offset The offset in the data set to start writing to in each dimension.
     */
    public void writeMDArrayBlockWithOffset(final String objectPath,
            final MDArray<String> referencedObjectPaths, final long[] offset);

    /**
     * Writes out a block of a multi-dimensional array of object references.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param referencedObjectPaths The paths of the object references to write. Must not be
     *            <code>null</code>.
     * @param blockDimensions The dimensions of the block to write to the data set.
     * @param offset The offset of the block in the data set to start writing to in each dimension.
     * @param memoryOffset The offset of the block in the <var>data</var> array.
     */
    public void writeMDArrayBlockWithOffset(final String objectPath,
            final MDLongArray referencedObjectPaths, final int[] blockDimensions,
            final long[] offset, final int[] memoryOffset);
}
