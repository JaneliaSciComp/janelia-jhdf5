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
import java.util.Date;

import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * A HDF5 writer which contains only the basic methods. If you feel overwhelmed with all the methods
 * of {@link IHDF5Writer}, then assign the writer to a {@link IHDF5SimpleWriter} variable and let
 * the code completion of your IDE help you find the method you are looking for.
 * <p>
 * Usage:
 * 
 * <pre>
 * float[] f = new float[100];
 * ...
 * IHDF5SimpleWriter writer = HDF5FactoryProvider.get().open(new File(&quot;test.h5&quot;));
 * writer.writeFloatArray(&quot;/some/path/dataset&quot;, f);
 * writer.close();
 * </pre>
 * 
 * @author Bernd Rinn
 */
public interface IHDF5SimpleWriter extends IHDF5SimpleReader
{

    /**
     * Removes an object from the file. If there is more than one link to the object, only the
     * specified link will be removed.
     */
    public void delete(String objectPath);

    /**
     * Writes out a <code>boolean</code> value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param value The value of the data set.
     */
    public void writeBoolean(final String objectPath, final boolean value);

    /**
     * Writes out a bit field ((which can be considered the equivalent to a boolean array of rank
     * 1), provided as a Java {@link BitSet}.
     * <p>
     * Note that the storage form of the bit array is a <code>long[]</code>. However, it is marked
     * in HDF5 to be interpreted bit-wise. Thus a data set written by this method cannot be read
     * back by {@link #readLongArray(String)} but will throw a
     * {@link hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeBitField(final String objectPath, final BitSet data);

    /**
     * Writes out a <code>byte</code> array (of rank 1). Uses a compact storage layout. Should only
     * be used for small data sets.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeByteArray(final String objectPath, final byte[] data);

    /**
     * Writes out a <code>int</code> value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param value The value to write.
     */
    public void writeInt(final String objectPath, final int value);

    /**
     * Writes out a <code>int</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeIntArray(final String objectPath, final int[] data);

    /**
     * Writes out a <code>int</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     */
    public void writeIntMatrix(final String objectPath, final int[][] data);

    /**
     * Writes out a <code>long</code> value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param value The value to write.
     */
    public void writeLong(final String objectPath, final long value);

    /**
     * Writes out a <code>long</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeLongArray(final String objectPath, final long[] data);

    /**
     * Writes out a <code>long</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     */
    public void writeLongMatrix(final String objectPath, final long[][] data);

    /**
     * Writes out a <code>float</code> value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param value The value to write.
     */
    public void writeFloat(final String objectPath, final float value);

    /**
     * Writes out a <code>float</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeFloatArray(final String objectPath, final float[] data);

    /**
     * Writes out a <code>float</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     */
    public void writeFloatMatrix(final String objectPath, final float[][] data);

    /**
     * Writes out a <code>double</code> value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param value The value to write.
     */
    public void writeDouble(final String objectPath, final double value);

    /**
     * Writes out a <code>double</code> array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeDoubleArray(final String objectPath, final double[] data);

    /**
     * Writes out a <code>double</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     */
    public void writeDoubleMatrix(final String objectPath, final double[][] data);

    /**
     * Writes out a time stamp value provided as a {@link Date}. The data set will be tagged as type
     * variant {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     * <p>
     * <em>Note: This is a convenience method for <code></code> </em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param date The date to write.
     */
    public void writeDate(final String objectPath, final Date date);

    /**
     * Writes out a {@link Date} array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param dates The dates to write.
     */
    public void writeDateArray(final String objectPath, final Date[] dates);

    /**
     * Writes out a time duration value.
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param timeDuration The duration of time to write.
     */
    public void writeTimeDuration(final String objectPath, final HDF5TimeDuration timeDuration);

    /**
     * Writes out a time duration array (of rank 1).
     * <p>
     * <em>Note: Time durations are stored as <code>long[]</code> arrays and tagged as the according
     * type variant.</em>
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param timeDurations The time durations to write.
     */
    public void writeTimeDurationArray(final String objectPath,
            final HDF5TimeDurationArray timeDurations);

    /**
     * Writes out a <code>String</code> with a fixed maximal length.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeString(final String objectPath, final String data);

    /**
     * Writes out a <code>String</code> array (of rank 1). Each element of the array will have a
     * fixed maximal length which is given by the longest element.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>.
     */
    public void writeStringArray(final String objectPath, final String[] data);

    /**
     * Writes out a compound value. The type is inferred based on the values.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The value of the data set. May be a pojo (Data Transfer Object), a
     *            {@link HDF5CompoundDataMap}, {@link HDF5CompoundDataList} or <code>Object[]</code>
     *            .
     * @see CompoundType
     * @see CompoundElement
     */
    public <T> void writeCompound(String objectPath, T data);

    /**
     * Writes out an array (of rank 1) of compound values. The type is inferred based on the values.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The value of the data set. May be a pojo (Data Transfer Object), a
     *            {@link HDF5CompoundDataMap}, {@link HDF5CompoundDataList} or <code>Object[]</code>
     *            .
     * @see CompoundType
     * @see CompoundElement
     */
    public <T> void writeCompoundArray(final String objectPath, final T[] data);

    /**
     * Writes out an enum value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param value The value of the data set.
     * @throws HDF5JavaException If the enum type of <var>value</var> is not a type of this file.
     */
    public <T extends Enum<T>> void writeEnum(final String objectPath, final Enum<T> value) throws HDF5JavaException;

    /**
     * Writes out an array of enum values.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write.
     */
    public <T extends Enum<T>> void writeEnumArray(String objectPath, Enum<T>[] data);

    /**
     * Writes out an array of enum values.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param options The allowed values of the enumeration type.
     * @param data The data to write.
     */
    public void writeEnumArray(String objectPath, String[] options, String[] data);

}
