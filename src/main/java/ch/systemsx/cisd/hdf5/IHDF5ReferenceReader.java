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
 * An interface for reading references in HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#reference()}.
 * <p>
 * For an explanation about references, see {@link IHDF5ReferenceWriter}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5ReferenceReader
{
    // //////////////////////////////
    // Specific to object references
    // //////////////////////////////

    /**
     * Resolves the path of a reference which has been read without name resolution.
     * 
     * @param reference Reference encoded as string.
     * @return The path in the HDF5 file.
     * @see #readArray(String, boolean)
     * @throws HDF5JavaException if <var>reference</var> is not a string-encoded reference.
     */
    public String resolvePath(final String reference) throws HDF5JavaException;

    // /////////////////////
    // Attributes
    // /////////////////////

    /**
     * Reads an object reference attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>, resolving the name of the object. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The path of the object that the reference refers to, or an empty string, if the
     *         object reference refers to an unnamed object.
     */
    public String getAttr(final String objectPath, final String attributeName);

    /**
     * Reads an object reference attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @param resolveName If <code>true</code>, resolves the name of the object referenced,
     *            otherwise returns the references itself.
     * @return The path of the object that the reference refers to, or an empty string, if the
     *         object reference refers to an unnamed object.
     */
    public String getAttr(final String objectPath, final String attributeName,
            final boolean resolveName);

    /**
     * Reads a 1D object reference array attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>, resolving the names of the objects. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The paths of the objects that the references refers to. Each string may be empty, if
     *         the corresponding object reference refers to an unnamed object.
     */
    public String[] getArrayAttr(final String objectPath, final String attributeName);

    /**
     * Reads a 1D object reference array attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @param resolveName If <code>true</code>, resolves the names of the objects referenced,
     *            otherwise returns the references itself.
     * @return The paths of the objects that the references refers to. Each string may be empty, if
     *         the corresponding object reference refers to an unnamed object.
     */
    public String[] getArrayAttr(final String objectPath, final String attributeName,
            final boolean resolveName);

    /**
     * Reads an object reference array attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>, resolving the names of the objects. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The paths of the objects that the references refers to. Each string may be empty, if
     *         the corresponding object reference refers to an unnamed object.
     */
    public MDArray<String> getMDArrayAttr(final String objectPath, final String attributeName);

    /**
     * Reads an object reference array attribute named <var>attributeName</var> from the object
     * <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @param resolveName If <code>true</code>, resolves the names of the objects referenced,
     *            otherwise returns the references itself.
     * @return The paths of the objects that the references refers to. Each string may be empty, if
     *         the corresponding object reference refers to an unnamed object.
     */
    public MDArray<String> getMDArrayAttr(final String objectPath, final String attributeName,
            boolean resolveName);

    // /////////////////////
    // Data Sets
    // /////////////////////

    /**
     * Reads an object reference from the object <var>objectPath</var>, resolving the name of the
     * object. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The path of the object that the reference refers to, or an empty string, if the
     *         object reference refers to an unnamed object.
     */
    public String read(final String objectPath);

    /**
     * Reads an object reference from the object <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param resolveName If <code>true</code>, resolves the name of the object referenced,
     *            otherwise returns the references itself.
     * @return The path of the object that the reference refers to, or an empty string, if the
     *         object reference refers to an unnamed object.
     */
    public String read(final String objectPath, final boolean resolveName);

    /**
     * Reads an array of object references from the object <var>objectPath</var>, resolving the
     * names of the objects. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The array of the paths of objects that the references refers to. Each string may be
     *         empty, if the corresponding object reference refers to an unnamed object.
     */
    public String[] readArray(final String objectPath);

    /**
     * Reads an array of object references from the object <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param resolveName If <code>true</code>, resolves the names of the objects referenced,
     *            otherwise returns the references itself.
     * @return The array of the paths of objects that the references refers to. Each string may be
     *         empty, if the corresponding object reference refers to an unnamed object.
     */
    public String[] readArray(final String objectPath, boolean resolveName);

    /**
     * Reads a block from an array (of rank 1) of object references from the data set
     * <var>objectPath</var>, resolving the names of the objects. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the length of the <code>long[]</code> returned
     *            if the data set is long enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @return The referenced data set paths read from the data set. The length will be min(size -
     *         blockSize*blockNumber, blockSize).
     */
    public String[] readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber);

    /**
     * Reads a block from an array (of rank 1) of object references from the data set
     * <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the length of the <code>long[]</code> returned
     *            if the data set is long enough).
     * @param blockNumber The number of the block to read (starting with 0, offset: multiply with
     *            <var>blockSize</var>).
     * @param resolveName If <code>true</code>, resolves the names of the objects referenced,
     *            otherwise returns the references itself.
     * @return The referenced data set paths read from the data set. The length will be min(size -
     *         blockSize*blockNumber, blockSize).
     */
    public String[] readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber, final boolean resolveName);

    /**
     * Reads a block from an array (of rank 1) of object references from the data set
     * <var>objectPath</var>, resolving the names of the objects. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the length of the <code>long[]</code>
     *            returned).
     * @param offset The offset of the block in the data set to start reading from (starting with
     *            0).
     * @return The referenced data set paths block read from the data set.
     */
    public String[] readArrayBlockWithOffset(final String objectPath, final int blockSize,
            final long offset);

    /**
     * Reads a block from an array (of rank 1) of object references from the data set
     * <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The block size (this will be the length of the <code>long[]</code>
     *            returned).
     * @param offset The offset of the block in the data set to start reading from (starting with
     *            0).
     * @param resolveName If <code>true</code>, resolves the names of the objects referenced,
     *            otherwise returns the references itself.
     * @return The referenced data set paths block read from the data set.
     */
    public String[] readArrayBlockWithOffset(final String objectPath, final int blockSize,
            final long offset, final boolean resolveName);

    /**
     * Reads an array (or rank N) of object references from the object <var>objectPath</var>,
     * resolving the names of the objects. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The multi-dimensional array of the paths of objects that the references refers to.
     *         Each string may be empty, if the corresponding object reference refers to an unnamed
     *         object.
     */
    public MDArray<String> readMDArray(final String objectPath);

    /**
     * Reads an array (or rank N) of object references from the object <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param resolveName If <code>true</code>, resolves the names of the objects referenced,
     *            otherwise returns the references itself.
     * @return The multi-dimensional array of the paths of objects that the references refers to.
     *         Each string may be empty, if the corresponding object reference refers to an unnamed
     *         object.
     */
    public MDArray<String> readMDArray(final String objectPath, boolean resolveName);

    /**
     * Reads a multi-dimensional array of object references from the data set <var>objectPath</var>,
     * resolving the names of the objects. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @return The referenced data set paths block read from the data set.
     */
    public MDArray<String> readMDArrayBlock(final String objectPath, final int[] blockDimensions,
            final long[] blockNumber);

    /**
     * Reads a multi-dimensional array of object references from the data set <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param blockNumber The block number in each dimension (offset: multiply with the
     *            <var>blockDimensions</var> in the according dimension).
     * @param resolveName If <code>true</code>, resolves the names of the objects referenced,
     *            otherwise returns the references itself.
     * @return The referenced data set paths block read from the data set.
     */
    public MDArray<String> readMDArrayBlock(final String objectPath, final int[] blockDimensions,
            final long[] blockNumber, final boolean resolveName);

    /**
     * Reads a multi-dimensional array of object references from the data set <var>objectPath</var>,
     * resolving the names of the objects. <br>
     * <i>Note that resolving the name of the object is a time consuming operation. If you don't
     * need the name, but want to dereference the dataset, you don't need to resolve the name if the
     * reader / writer is configured for auto-dereferencing (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}).</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @return The referenced data set paths block read from the data set.
     */
    public MDArray<String> readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset);

    /**
     * Reads a multi-dimensional array of object references from the data set <var>objectPath</var>. <br>
     * <i>Note: if the reader has been configured to automatically resolve references (see
     * {@link IHDF5ReaderConfigurator#noAutoDereference()}), a reference can be provided in all
     * places where an object path is expected. This is considerably faster than resolving the
     * name/path of the reference if the name/path by itself is not needed.</i>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockDimensions The extent of the block in each dimension.
     * @param offset The offset in the data set to start reading from in each dimension.
     * @param resolveName If <code>true</code>, resolves the names of the objects referenced,
     *            otherwise returns the references itself.
     * @return The referenced data set paths block read from the data set.
     */
    public MDArray<String> readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset, final boolean resolveName);

    /**
     * Provides all natural blocks of this one-dimensional data set to iterate over.
     * 
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1.
     */
    public Iterable<HDF5DataBlock<String[]>> getArrayNaturalBlocks(final String dataSetPath);

    /**
     * Provides all natural blocks of this one-dimensional data set to iterate over.
     * 
     * @see HDF5DataBlock
     * @throws HDF5JavaException If the data set is not of rank 1.
     */
    public Iterable<HDF5DataBlock<String[]>> getArrayNaturalBlocks(final String dataSetPath,
            final boolean resolveName);

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @see HDF5MDDataBlock
     */
    public Iterable<HDF5MDDataBlock<MDArray<String>>> getMDArrayNaturalBlocks(
            final String dataSetPath);

    /**
     * Provides all natural blocks of this multi-dimensional data set to iterate over.
     * 
     * @see HDF5MDDataBlock
     */
    public Iterable<HDF5MDDataBlock<MDArray<String>>> getMDArrayNaturalBlocks(
            final String dataSetPath, final boolean resolveName);
}
