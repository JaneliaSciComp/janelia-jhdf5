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

import hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException;
import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * An interface that provides methods for reading boolean and bit field values from HDF5 files.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#bool()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5BooleanReader
{

    /**
     * Reads a <code>boolean</code> attribute named <var>attributeName</var> from the data set
     * <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param attributeName The name of the attribute to read.
     * @return The attribute value read from the data set.
     * @throws HDF5JavaException If the attribute is not a boolean type.
     */
    public boolean getAttr(String objectPath, String attributeName) throws HDF5JavaException;

    /**
     * Reads a <code>Boolean</code> value from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a boolean type.
     */
    public boolean read(String objectPath) throws HDF5JavaException;

    /**
     * Reads a bit field (which can be considered the equivalent to a boolean array of rank 1) from
     * the data set <var>objectPath</var> and returns it as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by
     * {@link IHDF5LongWriter#writeArray(String, long[])} cannot be read back by this method but
     * will throw a {@link HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The {@link BitSet} read from the data set.
     * @throws HDF5DatatypeInterfaceException If the <var>objectPath</var> is not of bit field type.
     */
    public BitSet readBitField(String objectPath) throws HDF5DatatypeInterfaceException;

    /**
     * Reads a block of a bit field (which can be considered the equivalent to a boolean array of
     * rank 1) from the data set <var>objectPath</var> and returns it as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by
     * {@link IHDF5LongWriter#writeArray(String, long[])} cannot be read back by this method but
     * will throw a {@link HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The size of the block (in 64 bit words) to read.
     * @param blockNumber The number of the block to read.
     * @return The {@link BitSet} read from the data set.
     * @throws HDF5DatatypeInterfaceException If the <var>objectPath</var> is not of bit field type.
     */
    public BitSet readBitFieldBlock(String objectPath, int blockSize, long blockNumber);

    /**
     * Reads a block of a bit field (which can be considered the equivalent to a boolean array of
     * rank 1) from the data set <var>objectPath</var> and returns it as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by
     * {@link IHDF5LongWriter#writeArray(String, long[])} cannot be read back by this method but
     * will throw a {@link HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The size of the block (in 64 bit words) to read.
     * @param offset The offset of the block (in 64 bit words) to start reading from.
     * @return The {@link BitSet} read from the data set.
     * @throws HDF5DatatypeInterfaceException If the <var>objectPath</var> is not of bit field type.
     */
    public BitSet readBitFieldBlockWithOffset(String objectPath, int blockSize, long offset);

    /**
     * Returns <code>true</code> if the <var>bitIndex</var> of the bit field dataset
     * <var>objectPath</var> is set, <code>false</code> otherwise.
     * <p>
     * Will also return <code>false</code>, if <var>bitIndex</var> is outside of the bitfield
     * dataset.
     */
    public boolean isBitSet(String objectPath, int bitIndex);

    /**
     * Reads a bit field array (which can be considered the equivalent to a boolean array of rank 2)
     * from the data set <var>objectPath</var> and returns it as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by
     * {@link IHDF5LongWriter#writeMatrix(String, long[][])} cannot be read back by this method but
     * will throw a {@link HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The {@link BitSet} array read from the data set.
     * @throws HDF5DatatypeInterfaceException If the <var>objectPath</var> is not of bit field type.
     */
    public BitSet[] readBitFieldArray(String objectPath);

    /**
     * Reads a block of a bit field array (which can be considered the equivalent to a boolean array
     * of rank 2) from the data set <var>objectPath</var> and returns it as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by
     * {@link IHDF5LongWriter#writeMatrix(String, long[][])} cannot be read back by this method but
     * will throw a {@link HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The size of the array block.
     * @param offset The offset in the array where to start reading the block. 
     * @return The {@link BitSet} array read from the data set.
     * @throws HDF5DatatypeInterfaceException If the <var>objectPath</var> is not of bit field type.
     */
    public BitSet[] readBitFieldArrayBlockWithOffset(String objectPath, int blockSize,
            long offset);

    /**
     * Reads a block of a bit field array (which can be considered the equivalent to a boolean array
     * of rank 2) from the data set <var>objectPath</var> and returns it as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by
     * {@link IHDF5LongWriter#writeMatrix(String, long[][])} cannot be read back by this method but
     * will throw a {@link HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param blockSize The size of the array block.
     * @param blockNumber The number of the array block (offset is <code>blockNumber * blockSize</code>.). 
     * @return The {@link BitSet} array read from the data set.
     * @throws HDF5DatatypeInterfaceException If the <var>objectPath</var> is not of bit field type.
     */
    public BitSet[] readBitFieldArrayBlock(String objectPath, int blockSize,
            long blockNumber);

}