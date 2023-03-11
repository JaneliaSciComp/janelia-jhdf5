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

import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT32;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT8;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_U16LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_U32LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_U8LE;

import java.util.Iterator;
import java.util.List;

import hdf.hdf5lib.HDFNativeData;

/**
 * A class that represents an enumeration for a given HDF5 file and <var>values</var> array.
 * 
 * @author Bernd Rinn
 */
public final class HDF5EnumerationType extends HDF5DataType implements Iterable<String>
{
    /**
     * The storage form (as size in bytes) of an enumeration type.
     */
    public enum EnumStorageForm
    {
        /**
         * One byte, for up to 255 alternatives.
         */
        BYTE(1, H5T_NATIVE_INT8, H5T_STD_U8LE),
        /**
         * Two bytes, for up to 65535 alternatives.
         */
        SHORT(2, H5T_NATIVE_INT16, H5T_STD_U16LE),
        /**
         * Four bytes, for more than 65535 alternatives.
         */
        INT(4, H5T_NATIVE_INT32, H5T_STD_U32LE);

        private final byte storageSize;

        private final long intNativeType;

        private final long intStorageType;

        EnumStorageForm(int storageSize, long intNativeType, long intStorageType)
        {
            this.storageSize = (byte) storageSize;
            this.intNativeType = intNativeType;
            this.intStorageType = intStorageType;
        }

        /**
         * Return the number of bytes (1, 2 or 4) of this storage form.
         */
        public byte getStorageSize()
        {
            return storageSize;
        }

        long getIntNativeTypeId()
        {
            return intNativeType;
        }

        long getIntStorageTypeId()
        {
            return intStorageType;
        }
    }

    private final EnumerationType enumType;

    /**
     * Returns the storage data type id of the corresponding integer type of this type.
     */
    long getIntStorageTypeId()
    {
        return getStorageForm().getIntStorageTypeId();
    }

    /**
     * Returns the native data type id of the corresponding integer type of this type.
     */
    long getIntNativeTypeId()
    {
        return getStorageForm().getIntNativeTypeId();
    }

    HDF5EnumerationType(long fileId, long storageTypeId, long nativeTypeId, String nameOrNull,
            String[] values, HDF5BaseReader baseReader)
    {
        super(fileId, storageTypeId, nativeTypeId, baseReader);

        assert values != null;

        this.enumType = new EnumerationType(nameOrNull, values);
    }

    HDF5EnumerationType(long fileId, long storageTypeId, long nativeTypeId, EnumerationType enumType,
            HDF5BaseReader baseReader)
    {
        super(fileId, storageTypeId, nativeTypeId, baseReader);

        assert enumType != null;

        this.enumType = enumType;
    }

    EnumerationType getEnumType()
    {
        return enumType;
    }

    /**
     * Returns the ordinal value for the given string <var>value</var>, if <var>value</var> is a
     * member of the enumeration, and <code>null</code> otherwise.
     */
    public Integer tryGetIndexForValue(String value)
    {
        return enumType.tryGetIndexForValue(value);
    }

    /**
     * Returns the name of this type, if it exists and <code>null</code> otherwise.
     */
    @Override
    public String tryGetName()
    {
        return enumType.tryGetName();
    }

    /**
     * Returns the allowed values of this enumeration type.
     */
    public List<String> getValues()
    {
        return enumType.getValues();
    }

    /**
     * Returns the (file-independent) {@link EnumerationType} of this (file-dependen)
     * {@link HDF5EnumerationType}.
     */
    public EnumerationType getEnumerationType()
    {
        return enumType;
    }

    /**
     * Returns the {@link EnumStorageForm} of this enumeration type.
     */
    public EnumStorageForm getStorageForm()
    {
        return enumType.getStorageForm();
    }

    HDF5EnumerationValue createFromStorageForm(byte[] data, int offset)
    {
        return new HDF5EnumerationValue(this, getOrdinalFromStorageForm(data, offset));
    }

    String createStringFromStorageForm(byte[] data, int offset)
    {
        return enumType.createStringFromStorageForm(data, offset);
    }

    int getOrdinalFromStorageForm(byte[] data, int offset)
    {
        switch (getStorageForm())
        {
            case BYTE:
                return data[offset];
            case SHORT:
                return HDFNativeData.byteToShort(data, offset);
            case INT:
                return HDFNativeData.byteToInt(data, offset);
        }
        throw new Error("Illegal storage form (" + getStorageForm() + ".)");
    }

    //
    // Iterable
    //

    /**
     * Returns an {@link Iterator} over all values of this enumeration type.
     * {@link Iterator#remove()} is not allowed and will throw an
     * {@link UnsupportedOperationException}.
     */
    @Override
    public Iterator<String> iterator()
    {
        return enumType.iterator();
    }

    @Override
    public int hashCode()
    {
        return enumType.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final HDF5EnumerationType other = (HDF5EnumerationType) obj;
        return enumType.equals(other.enumType);
    }

}
