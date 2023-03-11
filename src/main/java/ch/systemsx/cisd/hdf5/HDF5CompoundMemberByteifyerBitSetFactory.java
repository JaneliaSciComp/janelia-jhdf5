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

import static ch.systemsx.cisd.base.convert.NativeData.LONG_SIZE;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getArray;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getList;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getMap;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.putMap;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.setArray;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.setList;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_B64LE;

import java.lang.reflect.Field;
import java.util.BitSet;

import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.AccessType;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory;
import ch.systemsx.cisd.hdf5.HDF5ValueObjectByteifyer.IFileAccessProvider;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;

/**
 * A {@link HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory} for <code>BitSet</code>
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundMemberByteifyerBitSetFactory implements IHDF5CompoundMemberBytifyerFactory
{

    @Override
    public boolean canHandle(Class<?> clazz, HDF5CompoundMemberInformation memberInfoOrNull)
    {
        if (memberInfoOrNull != null)
        {
            return (clazz == BitSet.class)
                    && memberInfoOrNull.getType().getDataClass() == HDF5DataClass.BITFIELD;
        } else
        {
            return (clazz == BitSet.class);
        }
    }

    @Override
    public Class<?> tryGetOverrideJavaType(HDF5DataClass dataClass, int rank, int elementSize,
            HDF5DataTypeVariant typeVariantOrNull)
    {
        return null;
    }

    @Override
    public HDF5MemberByteifyer createBytifyer(final AccessType accessType, final Field fieldOrNull,
            final HDF5CompoundMemberMapping member,
            final HDF5CompoundMemberInformation compoundMemberInfoOrNull,
            HDF5EnumerationType enumTypeOrNull, final Class<?> memberClazz, final int index,
            final int offset, int memOffset, final IFileAccessProvider fileInfoProvider)
    {
        final String memberName = member.getMemberName();
        final int memberTypeLengthInLongs;
        if (compoundMemberInfoOrNull == null)
        {
            final int memberTypeLengthInBits = member.getMemberTypeLength();
            memberTypeLengthInLongs =
                    memberTypeLengthInBits / 64 + (memberTypeLengthInBits % 64 != 0 ? 1 : 0);
        } else
        {
            memberTypeLengthInLongs = compoundMemberInfoOrNull.getType().getNumberOfElements();
        }

        if (memberTypeLengthInLongs <= 0)
        {
            throw new IllegalArgumentException(
                    "Length of a bit field must be a positive number (len="
                            + memberTypeLengthInLongs + ").");
        }
        final long storageTypeId = member.getStorageDataTypeId();
        final long memberTypeId =
                (storageTypeId < 0) ? fileInfoProvider.getArrayTypeId(H5T_STD_B64LE,
                        memberTypeLengthInLongs) : storageTypeId;
        switch (accessType)
        {
            case FIELD:
                return createByteifyerForField(fieldOrNull, memberName, offset, memOffset,
                        memberTypeLengthInLongs, memberTypeId, member.tryGetTypeVariant());
            case MAP:
                return createByteifyerForMap(memberName, offset, memOffset,
                        memberTypeLengthInLongs, memberTypeId, member.tryGetTypeVariant());
            case LIST:
                return createByteifyerForList(memberName, index, offset, memOffset,
                        memberTypeLengthInLongs, memberTypeId, member.tryGetTypeVariant());
            case ARRAY:
                return createByteifyerForArray(memberName, index, offset, memOffset,
                        memberTypeLengthInLongs, memberTypeId, member.tryGetTypeVariant());
            default:
                throw new Error("Unknown access type");
        }
    }

    private HDF5MemberByteifyer createByteifyerForField(final Field field, final String memberName,
            final int offset, int memOffset, final int memberTypeLengthInLongs,
            final long memberTypeId, final HDF5DataTypeVariant typeVariant)
    {
        ReflectionUtils.ensureAccessible(field);
        return new HDF5MemberByteifyer(field, memberName, memberTypeLengthInLongs * LONG_SIZE,
                offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return 8;
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return memberTypeId;
                }

                @Override
                protected long getMemberNativeTypeId()
                {
                    return -1;
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    final BitSet bs = (BitSet) field.get(obj);
                    return HDFHelper.longToByte(BitSetConversionUtils.toStorageForm(bs));
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final BitSet bs =
                            BitSetConversionUtils.fromStorageForm(HDFHelper.byteToLong(byteArr,
                                    arrayOffset + offsetInMemory, memberTypeLengthInLongs));
                    field.set(obj, bs);
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForMap(final String memberName, final int offset,
            int memOffset, final int memberTypeLengthInLongs, final long memberTypeId,
            final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, memberTypeLengthInLongs * LONG_SIZE,
                offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return 8;
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return memberTypeId;
                }

                @Override
                protected long getMemberNativeTypeId()
                {
                    return -1;
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    final BitSet bs = (BitSet) getMap(obj, memberName);
                    return HDFHelper.longToByte(BitSetConversionUtils.toStorageForm(bs));
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final BitSet bitSet =
                            BitSetConversionUtils.fromStorageForm(HDFHelper.byteToLong(byteArr,
                                    arrayOffset + offsetInMemory, memberTypeLengthInLongs));
                    putMap(obj, memberName, bitSet);
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForList(final String memberName, final int index,
            final int offset, int memOffset, final int memberTypeLengthInLongs,
            final long memberTypeId, final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, memberTypeLengthInLongs * LONG_SIZE,
                offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return 8;
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return memberTypeId;
                }

                @Override
                protected long getMemberNativeTypeId()
                {
                    return -1;
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    final BitSet bs = (BitSet) getList(obj, index);
                    return HDFHelper.longToByte(BitSetConversionUtils.toStorageForm(bs));
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final BitSet bitSet =
                            BitSetConversionUtils.fromStorageForm(HDFHelper.byteToLong(byteArr,
                                    arrayOffset + offsetInMemory, memberTypeLengthInLongs));
                    setList(obj, index, bitSet);
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForArray(final String memberName, final int index,
            final int offset, int memOffset, final int memberTypeLengthInLongs,
            final long memberTypeId, final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, memberTypeLengthInLongs * LONG_SIZE,
                offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return 8;
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return memberTypeId;
                }

                @Override
                protected long getMemberNativeTypeId()
                {
                    return -1;
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    final BitSet bs = (BitSet) getArray(obj, index);
                    return HDFHelper.longToByte(BitSetConversionUtils.toStorageForm(bs));
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final BitSet bitSet =
                            BitSetConversionUtils.fromStorageForm(HDFHelper.byteToLong(byteArr,
                                    arrayOffset + offsetInMemory, memberTypeLengthInLongs));
                    setArray(obj, index, bitSet);
                }
            };
    }

}
