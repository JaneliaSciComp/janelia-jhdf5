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

import java.io.Closeable;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

import hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException;
import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * A HDF5 reader which contains only the basic methods. If you feel overwhelmed with all the methods
 * of {@link IHDF5Reader}, then assign the reader to a {@link IHDF5SimpleReader} variable and let
 * the code completion of your IDE help you find the method you are looking for.
 * <p>
 * Usage:
 * 
 * <pre>
 * IHDF5SimpleReader reader = HDF5FactoryProvider.get().openForReading(new File(&quot;test.h5&quot;));
 * float[] f = reader.readFloatArray(&quot;/some/path/dataset&quot;);
 * reader.close();
 * </pre>
 * 
 * @author Bernd Rinn
 */
public interface IHDF5SimpleReader extends Closeable
{

    /**
     * Closes this object and the file referenced by this object. This object must not be used after
     * being closed.
     */
    @Override
    public void close();

    /**
     * Returns <code>true</code>, if <var>objectPath</var> exists and <code>false</code> otherwise.
     */
    public boolean exists(final String objectPath);

    /**
     * Returns <code>true</code> if the <var>objectPath</var> exists and represents a group and
     * <code>false</code> otherwise.
     */
    public boolean isGroup(final String objectPath);

    /**
     * Returns the information about a data set as a {@link HDF5DataSetInformation} object. It is a
     * failure condition if the <var>dataSetPath</var> does not exist or does not identify a data
     * set.
     * 
     * @param dataSetPath The name (including path information) of the data set to return
     *            information about.
     */
    public HDF5DataSetInformation getDataSetInformation(final String dataSetPath);

    /**
     * Returns the members of <var>groupPath</var>. The order is <i>not</i> well defined.
     * 
     * @param groupPath The path of the group to get the members for.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    public List<String> getGroupMembers(final String groupPath);

    /**
     * Reads the data set <var>objectPath</var> as byte array (of rank 1).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public byte[] readAsByteArray(final String objectPath);

    /**
     * Reads a <code>Boolean</code> value from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a boolean type.
     */
    public boolean readBoolean(final String objectPath) throws HDF5JavaException;

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
    public BitSet readBitField(final String objectPath) throws HDF5DatatypeInterfaceException;

    /**
     * Reads a <code>int</code> value from the data set <var>objectPath</var>. This method doesn't
     * check the data space but simply reads the first value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The value read from the data set.
     */
    public int readInt(final String objectPath);

    /**
     * Reads a <code>int</code> array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public int[] readIntArray(final String objectPath);

    /**
     * Reads a <code>int</code> matrix (array of arrays) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public int[][] readIntMatrix(final String objectPath);

    /**
     * Reads a <code>long</code> value from the data set <var>objectPath</var>. This method doesn't
     * check the data space but simply reads the first value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The value read from the data set.
     */
    public long readLong(final String objectPath);

    /**
     * Reads a <code>long</code> array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public long[] readLongArray(final String objectPath);

    /**
     * Reads a <code>long</code> matrix (array of arrays) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public long[][] readLongMatrix(final String objectPath);

    /**
     * Reads a <code>float</code> value from the data set <var>objectPath</var>. This method doesn't
     * check the data space but simply reads the first value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The value read from the data set.
     */
    public float readFloat(final String objectPath);

    /**
     * Reads a <code>float</code> array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public float[] readFloatArray(final String objectPath);

    /**
     * Reads a <code>float</code> matrix (array of arrays) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public float[][] readFloatMatrix(final String objectPath);

    /**
     * Reads a <code>double</code> value from the data set <var>objectPath</var>. This method
     * doesn't check the data space but simply reads the first value.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The value read from the data set.
     */
    public double readDouble(final String objectPath);

    /**
     * Reads a <code>double</code> array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public double[] readDoubleArray(final String objectPath);

    /**
     * Reads a <code>double</code> matrix (array of arrays) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public double[][] readDoubleMatrix(final String objectPath);

    /**
     * Reads a date value from the data set <var>objectPath</var>. It needs to have been written by
     * {@link IHDF5SimpleWriter#writeDate(String, Date)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The time stamp as {@link Date}.
     * @throws HDF5JavaException If the <var>objectPath</var> does not denote a time stamp.
     */
    public Date readDate(final String objectPath) throws HDF5JavaException;

    /**
     * Reads a date array (of rank 1) from the data set <var>objectPath</var>. It needs to have been
     * written by {@link IHDF5SimpleWriter#writeDateArray(String, Date[])}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The time stamp as {@link Date}.
     * @throws HDF5JavaException If the <var>objectPath</var> does not denote a time stamp.
     */
    public Date[] readDateArray(final String objectPath) throws HDF5JavaException;

    /**
     * Reads a time duration value from the data set <var>objectPath</var>. It needs to have been
     * written by {@link IHDF5SimpleWriter#writeTimeDuration(String, HDF5TimeDuration)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The time duration in seconds.
     * @throws HDF5JavaException If the <var>objectPath</var> is not tagged as a type variant that
     *             corresponds to a time duration.
     */
    public HDF5TimeDuration readTimeDuration(final String objectPath) throws HDF5JavaException;

    /**
     * Reads a time duration array from the data set <var>objectPath</var>. It needs to have been
     * written by {@link IHDF5SimpleWriter#writeTimeDurationArray(String, HDF5TimeDurationArray)}.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The time duration in seconds.
     * @throws HDF5JavaException If the <var>objectPath</var> is not defined as type variant
     *             {@link HDF5DataTypeVariant#TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH}.
     */
    public HDF5TimeDurationArray readTimeDurationArray(final String objectPath)
            throws HDF5JavaException;

    /**
     * Reads a <code>String</code> from the data set <var>objectPath</var>. Considers '\0' as end of
     * string. This needs to be a string type.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a string type.
     */
    public String readString(final String objectPath) throws HDF5JavaException;

    /**
     * Reads a <code>String</code> array (of rank 1) from the data set <var>objectPath</var>. The
     * elements of this data set need to be a string type.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set.
     */
    public String[] readStringArray(final String objectPath) throws HDF5JavaException;

    /**
     * Reads a compound from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     * @see CompoundType
     * @see CompoundElement
     */
    public <T> T readCompound(final String objectPath, final Class<T> pojoClass)
            throws HDF5JavaException;

    /**
     * Reads a compound array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param pojoClass The class to return the result in. Use {@link HDF5CompoundDataMap} to get it
     *            in a map, {@link HDF5CompoundDataList} to get it in a list, and
     *            <code>Object[]</code> to get it in an array, or use a pojo (Data Transfer Object),
     *            in which case the compound members will be mapped to Java fields.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not a compound data set.
     * @see CompoundType
     * @see CompoundElement
     */
    public <T> T[] readCompoundArray(final String objectPath, final Class<T> pojoClass)
            throws HDF5JavaException;

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
    public <T extends Enum<T>> T readEnum(final String objectPath, Class<T> enumClass)
            throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> value from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set as a String.
     * @throws HDF5JavaException If the <var>objectPath</var> is not an enum type.
     */
    public String readEnumAsString(final String objectPath) throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> value array from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param enumClass the {@link Enum} class to represent the values of.
     * @return The data read from the data set.
     * @throws HDF5JavaException If the <var>objectPath</var> is not of <var>enumType</var>.
     */
    public <T extends Enum<T>> T[] readEnumArray(final String objectPath, Class<T> enumClass)
            throws HDF5JavaException;

    /**
     * Reads an <code>Enum</code> array (of rank 1) from the data set <var>objectPath</var>.
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @return The data read from the data set as an array of Strings.
     * @throws HDF5JavaException If the <var>objectPath</var> is not an enum type.
     */
    public String[] readEnumArrayAsString(final String objectPath) throws HDF5JavaException;

}
