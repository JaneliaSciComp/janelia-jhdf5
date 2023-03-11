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

import java.lang.reflect.Array;
import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.base.convert.NativeData;
import ch.systemsx.cisd.base.convert.NativeData.ByteOrder;
import ch.systemsx.cisd.hdf5.HDF5EnumerationType.EnumStorageForm;
import hdf.hdf5lib.HDFNativeData;

/**
 * A class the represents an array of HDF enumeration values.
 * 
 * @author Bernd Rinn
 */
public class HDF5EnumerationValueArray implements Iterable<String>
{

    private final HDF5EnumerationType type;

    private final int length;

    private EnumStorageForm storageForm;

    private byte[] bArrayOrNull;

    private short[] sArrayOrNull;

    private int[] iArrayOrNull;

    HDF5EnumerationValueArray(HDF5EnumerationType type, Object array)
            throws IllegalArgumentException
    {
        this.type = type;
        if (array instanceof byte[])
        {
            final byte[] bArray = (byte[]) array;
            this.length = bArray.length;
            setOrdinalArray(bArray);
        } else if (array instanceof short[])
        {
            final short[] sArray = (short[]) array;
            this.length = sArray.length;
            setOrdinalArray(sArray);
        } else if (array instanceof int[])
        {
            final int[] iArray = (int[]) array;
            this.length = iArray.length;
            setOrdinalArray(iArray);
        } else
        {
            throw new IllegalArgumentException("array is of illegal type "
                    + array.getClass().getCanonicalName());
        }
    }

    /**
     * Creates an enumeration value array.
     * 
     * @param type The enumeration type of this value.
     * @param ordinalArray The array of ordinal values in the <var>type</var>.
     * @throws IllegalArgumentException If any of the ordinals in the <var>ordinalArray</var> is
     *             outside of the range of allowed values of the <var>type</var>.
     */
    public HDF5EnumerationValueArray(HDF5EnumerationType type, byte[] ordinalArray)
            throws IllegalArgumentException
    {
        this.type = type;
        this.length = ordinalArray.length;
        setOrdinalArray(ordinalArray);
    }

    /**
     * Creates an enumeration value array.
     * 
     * @param type The enumeration type of this value.
     * @param ordinalArray The array of ordinal values in the <var>type</var>.
     * @throws IllegalArgumentException If any of the ordinals in the <var>ordinalArray</var> is
     *             outside of the range of allowed values of the <var>type</var>.
     */
    public HDF5EnumerationValueArray(HDF5EnumerationType type, short[] ordinalArray)
            throws IllegalArgumentException
    {
        this.type = type;
        this.length = ordinalArray.length;
        setOrdinalArray(ordinalArray);
    }

    /**
     * Creates an enumeration value array.
     * 
     * @param type The enumeration type of this value.
     * @param ordinalArray The array of ordinal values in the <var>type</var>.
     * @throws IllegalArgumentException If any of the ordinals in the <var>ordinalArray</var> is
     *             outside of the range of allowed values of the <var>type</var>.
     */
    public HDF5EnumerationValueArray(HDF5EnumerationType type, int[] ordinalArray)
            throws IllegalArgumentException
    {
        this.type = type;
        this.length = ordinalArray.length;
        setOrdinalArray(ordinalArray);
    }

    /**
     * Creates an enumeration value array.
     * 
     * @param type The enumeration type of this value.
     * @param valueArray The array of enum values (each one needs to be one of the values of
     *            <var>type</var>).
     * @throws IllegalArgumentException If any of the values in the <var>valueArray</var> is not one
     *             of the values of <var>type</var>.
     */
    public HDF5EnumerationValueArray(HDF5EnumerationType type, Enum<?>[] valueArray)
            throws IllegalArgumentException
    {
        this(type, toString(valueArray));
    }

    private static String[] toString(Enum<?>[] valueArray)
    {
        final String[] result = new String[valueArray.length];
        for (int i = 0; i < valueArray.length; ++i)
        {
            result[i] = valueArray[i].name();
        }
        return result;
    }
    
    /**
     * Creates an enumeration value array.
     * 
     * @param type The enumeration type of this value.
     * @param valueArray The array of string values (each one needs to be one of the values of
     *            <var>type</var>).
     * @throws IllegalArgumentException If any of the values in the <var>valueArray</var> is not one
     *             of the values of <var>type</var>.
     */
    public HDF5EnumerationValueArray(HDF5EnumerationType type, String[] valueArray)
            throws IllegalArgumentException
    {
        this.type = type;
        this.length = valueArray.length;
        map(valueArray);
    }

    private void map(String[] array) throws IllegalArgumentException
    {
        if (type.getEnumType().getValueArray().length < Byte.MAX_VALUE)
        {
            storageForm = EnumStorageForm.BYTE;
            bArrayOrNull = new byte[array.length];
            for (int i = 0; i < array.length; ++i)
            {
                final Integer indexOrNull = type.tryGetIndexForValue(array[i]);
                if (indexOrNull == null)
                {
                    throw new IllegalArgumentException("Value '" + array[i]
                            + "' is not allowed for type '" + type.getName() + "'.");
                }
                bArrayOrNull[i] = indexOrNull.byteValue();
            }
            sArrayOrNull = null;
            iArrayOrNull = null;
        } else if (type.getEnumType().getValueArray().length < Short.MAX_VALUE)
        {
            storageForm = EnumStorageForm.SHORT;
            bArrayOrNull = null;
            sArrayOrNull = new short[array.length];
            for (int i = 0; i < array.length; ++i)
            {
                final Integer indexOrNull = type.tryGetIndexForValue(array[i]);
                if (indexOrNull == null)
                {
                    throw new IllegalArgumentException("Value '" + array[i]
                            + "' is not allowed for type '" + type.getName() + "'.");
                }
                sArrayOrNull[i] = indexOrNull.shortValue();
            }
            iArrayOrNull = null;
        } else
        {
            storageForm = EnumStorageForm.INT;
            bArrayOrNull = null;
            sArrayOrNull = null;
            iArrayOrNull = new int[array.length];
            for (int i = 0; i < array.length; ++i)
            {
                final Integer indexOrNull = type.tryGetIndexForValue(array[i]);
                if (indexOrNull == null)
                {
                    throw new IllegalArgumentException("Value '" + array[i]
                            + "' is not allowed for type '" + type.getName() + "'.");
                }
                iArrayOrNull[i] = indexOrNull.intValue();
            }
        }
    }

    private void setOrdinalArray(byte[] array)
    {
        if (type.getEnumType().getValueArray().length < Byte.MAX_VALUE)
        {
            storageForm = EnumStorageForm.BYTE;
            bArrayOrNull = array;
            checkOrdinalArray(bArrayOrNull);
            sArrayOrNull = null;
            iArrayOrNull = null;
        } else if (type.getEnumType().getValueArray().length < Short.MAX_VALUE)
        {
            storageForm = EnumStorageForm.SHORT;
            bArrayOrNull = null;
            sArrayOrNull = toShortArray(array);
            checkOrdinalArray(sArrayOrNull);
            iArrayOrNull = null;
        } else
        {
            storageForm = EnumStorageForm.INT;
            bArrayOrNull = null;
            sArrayOrNull = null;
            iArrayOrNull = toIntArray(array);
            checkOrdinalArray(iArrayOrNull);
        }
    }

    private void setOrdinalArray(short[] array) throws IllegalArgumentException
    {
        if (type.getEnumType().getValueArray().length < Byte.MAX_VALUE)
        {
            storageForm = EnumStorageForm.BYTE;
            bArrayOrNull = toByteArray(array);
            checkOrdinalArray(bArrayOrNull);
            sArrayOrNull = null;
            iArrayOrNull = null;
        } else if (type.getEnumType().getValueArray().length < Short.MAX_VALUE)
        {
            storageForm = EnumStorageForm.SHORT;
            bArrayOrNull = null;
            sArrayOrNull = array;
            checkOrdinalArray(sArrayOrNull);
            iArrayOrNull = null;
        } else
        {
            storageForm = EnumStorageForm.INT;
            bArrayOrNull = null;
            sArrayOrNull = null;
            iArrayOrNull = toIntArray(array);
            checkOrdinalArray(iArrayOrNull);
        }
    }

    private void setOrdinalArray(int[] array) throws IllegalArgumentException
    {
        if (type.getEnumType().getValueArray().length < Byte.MAX_VALUE)
        {
            storageForm = EnumStorageForm.BYTE;
            bArrayOrNull = toByteArray(array);
            checkOrdinalArray(bArrayOrNull);
            sArrayOrNull = null;
            iArrayOrNull = null;
        } else if (type.getEnumType().getValueArray().length < Short.MAX_VALUE)
        {
            storageForm = EnumStorageForm.SHORT;
            bArrayOrNull = null;
            sArrayOrNull = toShortArray(array);
            checkOrdinalArray(sArrayOrNull);
            iArrayOrNull = null;
        } else
        {
            storageForm = EnumStorageForm.INT;
            bArrayOrNull = null;
            sArrayOrNull = null;
            iArrayOrNull = array;
            checkOrdinalArray(iArrayOrNull);
        }
    }

    private byte[] toByteArray(short[] array) throws IllegalArgumentException
    {
        final byte[] bArray = new byte[array.length];
        for (int i = 0; i < array.length; ++i)
        {
            bArray[i] = (byte) array[i];
            if (bArray[i] != array[i])
            {
                throw new IllegalArgumentException("Value " + array[i]
                        + " cannot be stored in byte array");
            }
        }
        return bArray;
    }

    private byte[] toByteArray(int[] array) throws IllegalArgumentException
    {
        final byte[] bArray = new byte[array.length];
        for (int i = 0; i < array.length; ++i)
        {
            bArray[i] = (byte) array[i];
            if (bArray[i] != array[i])
            {
                throw new IllegalArgumentException("Value " + array[i]
                        + " cannot be stored in byte array");
            }
        }
        return bArray;
    }

    private short[] toShortArray(byte[] array)
    {
        final short[] sArray = new short[array.length];
        for (int i = 0; i < array.length; ++i)
        {
            sArray[i] = array[i];
        }
        return sArray;
    }

    private short[] toShortArray(int[] array) throws IllegalArgumentException
    {
        final short[] sArray = new short[array.length];
        for (int i = 0; i < array.length; ++i)
        {
            sArray[i] = (short) array[i];
            if (sArray[i] != array[i])
            {
                throw new IllegalArgumentException("Value " + array[i]
                        + " cannot be stored in short array");
            }
        }
        return sArray;
    }

    private int[] toIntArray(byte[] array)
    {
        final int[] iArray = new int[array.length];
        for (int i = 0; i < array.length; ++i)
        {
            iArray[i] = array[i];
        }
        return iArray;
    }

    private int[] toIntArray(short[] array)
    {
        final int[] iArray = new int[array.length];
        for (int i = 0; i < array.length; ++i)
        {
            iArray[i] = array[i];
        }
        return iArray;
    }

    private void checkOrdinalArray(byte[] array) throws IllegalArgumentException
    {
        for (int i = 0; i < array.length; ++i)
        {
            if (array[i] < 0 || array[i] >= type.getEnumType().getValueArray().length)
            {
                throw new IllegalArgumentException("valueIndex " + array[i]
                        + " out of allowed range [0.." + (type.getEnumType().getValueArray().length - 1)
                        + "] of type '" + type.getName() + "'.");
            }
        }
    }

    private void checkOrdinalArray(short[] array) throws IllegalArgumentException
    {
        for (int i = 0; i < array.length; ++i)
        {
            if (array[i] < 0 || array[i] >= type.getEnumType().getValueArray().length)
            {
                throw new IllegalArgumentException("valueIndex " + array[i]
                        + " out of allowed range [0.." + (type.getEnumType().getValueArray().length - 1)
                        + "] of type '" + type.getName() + "'.");
            }
        }
    }

    private void checkOrdinalArray(int[] array) throws IllegalArgumentException
    {
        for (int i = 0; i < array.length; ++i)
        {
            if (array[i] < 0 || array[i] >= type.getEnumType().getValueArray().length)
            {
                throw new IllegalArgumentException("valueIndex " + array[i]
                        + " out of allowed range [0.." + (type.getEnumType().getValueArray().length - 1)
                        + "] of type '" + type.getName() + "'.");
            }
        }
    }

    EnumStorageForm getStorageForm()
    {
        return storageForm;
    }

    byte[] getStorageFormBArray()
    {
        return bArrayOrNull;
    }

    short[] getStorageFormSArray()
    {
        return sArrayOrNull;
    }

    int[] getStorageFormIArray()
    {
        return iArrayOrNull;
    }

    /**
     * Returns the <var>type</var> of this enumeration array.
     */
    public HDF5EnumerationType getType()
    {
        return type;
    }

    /**
     * Returns the number of members of this enumeration array.
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Returns the ordinal value for the <var>arrayIndex</var>.
     * 
     * @param arrayIndex The index in the array to get the ordinal for.
     */
    public int getOrdinal(int arrayIndex)
    {
        if (bArrayOrNull != null)
        {
            return bArrayOrNull[arrayIndex];
        } else if (sArrayOrNull != null)
        {
            return sArrayOrNull[arrayIndex];
        } else
        {
            return iArrayOrNull[arrayIndex];
        }
    }

    /**
     * Returns the string value for <var>arrayIndex</var>.
     * 
     * @param arrayIndex The index in the array to get the value for.
     */
    public String getValue(int arrayIndex)
    {
        return type.getValues().get(getOrdinal(arrayIndex));
    }

    /**
     * Returns the value as Enum of type <var>enumClass</var>.
     * 
     * @param enumClass The class to return the value as.
     * @param arrayIndex The index in the array to get the value for.
     */
    public <T extends Enum<T>> T getValue(Class<T> enumClass, int arrayIndex)
    {
        return Enum.valueOf(enumClass, getValue(arrayIndex));
    }

    /**
     * Returns the string values for all elements of this array.
     */
    public String[] toStringArray()
    {
        final int len = getLength();
        final String[] values = new String[len];
        for (int i = 0; i < len; ++i)
        {
            values[i] = getValue(i);
        }
        return values;
    }

    /**
     * Returns the values for all elements of this array as Enums of type <var>enumClass</var>.
     */
    public <T extends Enum<T>> T[] toEnumArray(Class<T> enumClass)
    {
        final int len = getLength();
        @SuppressWarnings("unchecked")
        final T[] result = (T[]) Array.newInstance(enumClass, len);
        for (int i = 0; i < len; ++i)
        {
            try
            {
                result[i] = Enum.valueOf(enumClass, getValue(i));
            } catch (IllegalArgumentException ex)
            {
                throw new HDF5JavaException("The Java enum class " + enumClass.getCanonicalName()
                        + " has no value '" + getValue(i) + "'.");
            }
        }
        return result;
    }

    byte[] toStorageForm()
    {
        switch (getStorageForm())
        {
            case BYTE:
                return getStorageFormBArray();
            case SHORT:
                return NativeData.shortToByte(getStorageFormSArray(), ByteOrder.NATIVE);
            case INT:
                return NativeData.intToByte(getStorageFormIArray(), ByteOrder.NATIVE);
        }
        throw new Error("Illegal storage form (" + getStorageForm() + ".)");
    }

    static HDF5EnumerationValueArray fromStorageForm(HDF5EnumerationType enumType, byte[] data,
            int offset, int len)
    {
        switch (enumType.getStorageForm())
        {
            case BYTE:
                final byte[] subArray = new byte[len];
                System.arraycopy(data, offset, subArray, 0, len);
                return new HDF5EnumerationValueArray(enumType, subArray);
            case SHORT:
                return new HDF5EnumerationValueArray(enumType, HDFNativeData.byteToShort(offset, len, data));
            case INT:
                return new HDF5EnumerationValueArray(enumType, HDFNativeData.byteToInt(offset, len, data));
        }
        throw new Error("Illegal storage form (" + enumType.getStorageForm() + ".)");
    }

    static String[] fromStorageFormToStringArray(HDF5EnumerationType enumType, byte[] data,
            int offset, int len)
    {
        final String[] valueArray = new String[len];
        for (int i = 0; i < len; ++i)
        {
            valueArray[i] = enumType.createStringFromStorageForm(data, offset + i);
        }
        return valueArray;
    }

    static int[] fromStorageFormToIntArray(HDF5EnumerationType enumType, byte[] data,
            int offset, int len)
    {
        final int[] valueArray = new int[len];
        for (int i = 0; i < len; ++i)
        {
            valueArray[i] = enumType.getOrdinalFromStorageForm(data, offset + i);
        }
        return valueArray;
    }

    //
    // Iterable
    //

    @Override
    public Iterator<String> iterator()
    {
        return new Iterator<String>()
            {
                private int index = 0;

                @Override
                public boolean hasNext()
                {
                    return index < length;
                }

                @Override
                public String next()
                {
                    return getValue(index++);
                }

                @Override
                public void remove() throws UnsupportedOperationException
                {
                    throw new UnsupportedOperationException();
                }

            };
    }

    @Override
    public String toString()
    {
        final StringBuilder b = new StringBuilder();
        b.append(type.getName());
        b.append(" [");
        for (String value : this)
        {
            b.append(value);
            b.append(",");
        }
        b.setLength(b.length() - 1);
        b.append("]");
        return b.toString();
    }

}
