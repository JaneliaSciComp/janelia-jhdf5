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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.BitSet;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDDoubleArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;

/**
 * Contains information about one member of an HDF5 compound data type.
 * 
 * @author Bernd Rinn
 */
public final class HDF5CompoundMemberInformation implements
        Comparable<HDF5CompoundMemberInformation>
{
    private final String memberName;

    private final HDF5DataTypeInformation dataTypeInformation;

    private final int offsetOnDisk;

    private final int offsetInMemory;

    private final String[] enumValuesOrNull;

    HDF5CompoundMemberInformation(String memberName, HDF5DataTypeInformation dataTypeInformation,
            int offsetOnDisk, int offsetInMemory, String[] enumValuesOrNull)
    {
        assert memberName != null;
        assert dataTypeInformation != null;
        assert offsetOnDisk >= 0;

        this.memberName = memberName;
        this.dataTypeInformation = dataTypeInformation;
        this.enumValuesOrNull = enumValuesOrNull;
        this.offsetOnDisk = offsetOnDisk;
        this.offsetInMemory = PaddingUtils.padOffset(offsetInMemory, dataTypeInformation.getElementSizeForPadding());

    }

    HDF5CompoundMemberInformation(String memberName, HDF5DataTypeInformation dataTypeInformation,
            int offsetOnDisk, int offsetInMemory)
    {
        this(memberName, dataTypeInformation, offsetOnDisk, offsetInMemory, null);
    }

    /**
     * Returns the name of the member.
     */
    public String getName()
    {
        return memberName;
    }

    /**
     * Returns the type information of the member.
     */
    public HDF5DataTypeInformation getType()
    {
        return dataTypeInformation;
    }

    /**
     * Returns the values of the enumeration type of this compound member, if it is of an
     * enumeration type and <code>null</code> otherwise.
     */
    public String[] tryGetEnumValues()
    {
        return enumValuesOrNull;
    }

    /**
     * Returns the byte offset of this member within the compound data type on disk. 0 meaning that
     * the member is the first one in the compound data type.
     * <p>
     * The on-disk representation is packed.
     */
    public int getOffsetOnDisk()
    {
        return offsetOnDisk;
    }

    /**
     * Returns the byte offset of this member within the compound data type in memory. 0 meaning
     * that the member is the first one in the compound data type.
     * <p>
     * The in-memory representation may contain padding to ensure that read access is always
     * aligned.
     */
    public int getOffsetInMemory()
    {
        return offsetInMemory;
    }

    /**
     * Creates the compound member information for the given <var>compoundClass</var> and
     * <var>members</var>. The returned array will contain the members in the order of the
     * <var>members</var>.
     * <p>
     * Call <code>Arrays.sort(compoundInformation)</code> to sort the array in alphabetical order of
     * names.
     * <p>
     * Can be used to compare compound types, e.g. via
     * {@link java.util.Arrays#equals(Object[], Object[])}.
     */
    public static HDF5CompoundMemberInformation[] create(Class<?> compoundClass,
            String houseKeepingNameSuffix, final HDF5CompoundMemberMapping... members)
    {
        assert compoundClass != null;
        final HDF5CompoundMemberInformation[] info =
                new HDF5CompoundMemberInformation[members.length];
        int offsetOnDisk = 0;
        int offsetInMemory = 0;
        for (int i = 0; i < info.length; ++i)
        {
            info[i] =
                    new HDF5CompoundMemberInformation(members[i].getMemberName(),
                            getTypeInformation(compoundClass, houseKeepingNameSuffix, members[i]),
                            offsetOnDisk, offsetInMemory);
            final int elementSize = info[i].getType().getElementSize();
            final int size = info[i].getType().getSize();
            offsetOnDisk += size;
            offsetInMemory = PaddingUtils.padOffset(offsetInMemory + size, elementSize);
        }
        Arrays.sort(info);
        return info;
    }

    private static HDF5DataTypeInformation getTypeInformation(Class<?> compoundClass,
            String houseKeepingNameSuffix, final HDF5CompoundMemberMapping member)
    {
        final Field fieldOrNull = member.tryGetField(compoundClass);
        final Class<?> fieldTypeOrNull = (fieldOrNull == null) ? null : fieldOrNull.getType();
        final HDF5DataTypeInformation typeInfo;
        if (fieldTypeOrNull == boolean.class)
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.BOOLEAN, houseKeepingNameSuffix, 1,
                            false);
        } else if (fieldTypeOrNull == byte.class || fieldTypeOrNull == byte[].class
                || fieldTypeOrNull == byte[][].class || fieldTypeOrNull == MDByteArray.class)
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.INTEGER, houseKeepingNameSuffix, 1,
                            false == member.isUnsigned());
        } else if (fieldTypeOrNull == short.class || fieldTypeOrNull == short[].class
                || fieldTypeOrNull == short[][].class || fieldTypeOrNull == MDShortArray.class)
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.INTEGER, houseKeepingNameSuffix, 2,
                            false == member.isUnsigned());
        } else if (fieldTypeOrNull == int.class || fieldTypeOrNull == int[].class
                || fieldTypeOrNull == int[][].class || fieldTypeOrNull == MDIntArray.class)
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.INTEGER, houseKeepingNameSuffix, 4,
                            false == member.isUnsigned());
        } else if (fieldTypeOrNull == long.class || fieldTypeOrNull == long[].class
                || fieldTypeOrNull == long[][].class || fieldTypeOrNull == MDLongArray.class)
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.INTEGER, houseKeepingNameSuffix, 8,
                            false == member.isUnsigned());
        } else if (fieldTypeOrNull == BitSet.class)
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.BITFIELD, houseKeepingNameSuffix, 8,
                            member.getMemberTypeLength() / 64
                                    + (member.getMemberTypeLength() % 64 != 0 ? 1 : 0), false);
        } else if (fieldTypeOrNull == float.class || fieldTypeOrNull == float[].class
                || fieldTypeOrNull == float[][].class || fieldTypeOrNull == MDFloatArray.class)
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.FLOAT, houseKeepingNameSuffix, 4,
                            true);
        } else if (fieldTypeOrNull == double.class || fieldTypeOrNull == double[].class
                || fieldTypeOrNull == double[][].class || fieldTypeOrNull == MDDoubleArray.class)
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.FLOAT, houseKeepingNameSuffix, 8,
                            true);
        } else if (fieldTypeOrNull == String.class || fieldTypeOrNull == char[].class)
        {
            if (member.isReference())
            {
                typeInfo =
                        new HDF5DataTypeInformation(HDF5DataClass.REFERENCE, houseKeepingNameSuffix,
                                HDF5BaseReader.REFERENCE_SIZE_IN_BYTES, false);
            } else
            {
                typeInfo =
                        new HDF5DataTypeInformation(HDF5DataClass.STRING, houseKeepingNameSuffix,
                                member.getMemberTypeLength(), false);
            }
        } else if (fieldTypeOrNull == HDF5EnumerationValue.class)
        {
            final DataTypeInfoOptions options =
                    new DataTypeInfoOptions("UNKNOWN".equals(member.tryGetEnumerationType()
                            .getName()) == false, member.tryGetTypeVariant() != null);
            typeInfo =
                    new HDF5DataTypeInformation(
                            options.knowsDataTypePath() ? HDF5Utils.createDataTypePath(
                                    HDF5Utils.ENUM_PREFIX, houseKeepingNameSuffix, member
                                            .tryGetEnumerationType().getName()) : null, options,
                            HDF5DataClass.ENUM, houseKeepingNameSuffix, member
                                    .tryGetEnumerationType().getStorageForm().getStorageSize(),
                            false);
            if (options.knowsDataTypeVariant())
            {
                typeInfo.setTypeVariant(member.tryGetTypeVariant());
            }
        } else
        {
            typeInfo =
                    new HDF5DataTypeInformation(HDF5DataClass.OTHER, houseKeepingNameSuffix, -1,
                            false);
        }
        if (fieldTypeOrNull != null
                && (fieldTypeOrNull.isArray() && fieldTypeOrNull != char[].class)
                || MDAbstractArray.class.isAssignableFrom(fieldTypeOrNull))
        {
            typeInfo.setDimensions(member.getMemberTypeDimensions());
        }
        return typeInfo;
    }

    //
    // Object
    //

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj instanceof HDF5CompoundMemberInformation == false)
        {
            return false;
        }
        final HDF5CompoundMemberInformation that = (HDF5CompoundMemberInformation) obj;
        return memberName.equals(that.memberName)
                && dataTypeInformation.equals(that.dataTypeInformation);
    }

    @Override
    public int hashCode()
    {
        return (17 * 59 + memberName.hashCode()) * 59 + dataTypeInformation.hashCode();
    }

    @Override
    public String toString()
    {
        return memberName + ":" + dataTypeInformation.toString();
    }

    //
    // Comparable<HDF5CompoundMemberInformation>
    //

    @Override
    public int compareTo(HDF5CompoundMemberInformation o)
    {
        return memberName.compareTo(o.memberName);
    }

}
