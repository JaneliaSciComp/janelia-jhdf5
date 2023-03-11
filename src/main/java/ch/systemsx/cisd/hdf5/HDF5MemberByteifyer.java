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

import static hdf.hdf5lib.H5.H5Tinsert;

import java.lang.reflect.Field;

import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;

/**
 * A class that byteifies member fields of objects.
 * 
 * @author Bernd Rinn
 */
abstract class HDF5MemberByteifyer
{
    private final Field fieldOrNull;

    private final String memberName;

    protected final int maxCharacters;

    protected final int size;

    protected final int offsetOnDisk;

    protected final int offsetInMemory;

    protected final CharacterEncoding encoding;

    private final HDF5DataTypeVariant typeVariant;

    private final boolean isVariableLengthType;

    HDF5MemberByteifyer(Field fieldOrNull, String memberName, int size, int offset, int memOffset,
            boolean isVariableLengthType, HDF5DataTypeVariant typeVariantOrNull)
    {
        this(fieldOrNull, memberName, size, offset, memOffset, CharacterEncoding.ASCII, size,
                isVariableLengthType, false, typeVariantOrNull);
    }

    HDF5MemberByteifyer(Field fieldOrNull, String memberName, int size, int offset, int memOffset,
            CharacterEncoding encoding, int maxCharacters, boolean isVariableLengthType,
            boolean isReferenceType)
    {
        this(fieldOrNull, memberName, size, offset, memOffset, encoding, maxCharacters,
                isVariableLengthType, isReferenceType, HDF5DataTypeVariant.NONE);
    }

    private HDF5MemberByteifyer(Field fieldOrNull, String memberName, int size, int offset,
            int memOffset, CharacterEncoding encoding, int maxCharacters,
            boolean isVariableLengthType, boolean isReferenceType,
            HDF5DataTypeVariant typeVariantOrNull)
    {
        this.isVariableLengthType = isVariableLengthType;
        this.fieldOrNull = fieldOrNull;
        this.memberName = memberName;
        this.maxCharacters = maxCharacters;
        if (isVariableLengthType)
        {
            this.size = HDFHelper.getMachineWordSize();
        } else if (isReferenceType)
        {
            this.size = HDF5BaseReader.REFERENCE_SIZE_IN_BYTES;
        } else
        {
            this.size = size;
        }
        this.offsetOnDisk = offset;
        this.offsetInMemory = PaddingUtils.padOffset(memOffset, getElementSize());
        this.encoding = encoding;
        this.typeVariant = HDF5DataTypeVariant.maskNull(typeVariantOrNull);
    }

    /**
     * Returns the size of one element of this data type in bytes.
     */
    abstract int getElementSize();

    abstract byte[] byteify(long compoundDataTypeId, Object obj) throws IllegalAccessException;

    abstract void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
            int arrayOffset) throws IllegalAccessException;

    abstract long getMemberStorageTypeId();

    /**
     * Returns -1 if the native type id should be inferred from the storage type id
     */
    abstract long getMemberNativeTypeId();

    HDF5EnumerationType tryGetEnumType()
    {
        return null;
    }

    void insertType(long dataTypeId)
    {
        H5Tinsert(dataTypeId, memberName, offsetOnDisk, getMemberStorageTypeId());
    }

    void insertNativeType(long dataTypeId, HDF5 h5, ICleanUpRegistry registry)
    {
        if (getMemberNativeTypeId() < 0)
        {
            H5Tinsert(dataTypeId, memberName, offsetInMemory,
                    h5.getNativeDataType(getMemberStorageTypeId(), registry));
        } else
        {
            H5Tinsert(dataTypeId, memberName, offsetInMemory, getMemberNativeTypeId());
        }
    }

    String getMemberName()
    {
        return memberName;
    }

    Field tryGetField()
    {
        return fieldOrNull;
    }

    int getMaxCharacters()
    {
        return maxCharacters;
    }

    int getSize()
    {
        return size;
    }

    int getOffsetOnDisk()
    {
        return offsetOnDisk;
    }

    int getTotalSizeOnDisk()
    {
        return offsetOnDisk + size;
    }

    int getOffsetInMemory()
    {
        return offsetInMemory;
    }

    int getTotalSizeInMemory()
    {
        return offsetInMemory + size;
    }

    HDF5DataTypeVariant getTypeVariant()
    {
        return typeVariant;
    }

    boolean isVariableLengthType()
    {
        return isVariableLengthType;
    }

    String describe()
    {
        if (fieldOrNull != null)
        {
            return "field '" + fieldOrNull.getName() + "' of class '"
                    + fieldOrNull.getDeclaringClass().getCanonicalName() + "'";
        } else
        {
            return "member '" + memberName + "'";
        }
    }

    boolean isDummy()
    {
        return false;
    }

    boolean mayBeCut()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return describe();
    }
}
