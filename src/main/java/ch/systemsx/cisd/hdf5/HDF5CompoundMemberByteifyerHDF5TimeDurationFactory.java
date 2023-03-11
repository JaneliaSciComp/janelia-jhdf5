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
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I64LE;

import java.lang.reflect.Field;

import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.AccessType;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory;
import ch.systemsx.cisd.hdf5.HDF5ValueObjectByteifyer.IFileAccessProvider;
import hdf.hdf5lib.HDFNativeData;

/**
 * A {@link HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory} for
 * {@link HDF5TimeDuration}.
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundMemberByteifyerHDF5TimeDurationFactory implements
        IHDF5CompoundMemberBytifyerFactory
{

    @Override
    public boolean canHandle(Class<?> clazz, HDF5CompoundMemberInformation memberInfoOrNull)
    {
        if (memberInfoOrNull != null)
        {
            return (clazz == HDF5TimeDuration.class || Long.class.isAssignableFrom(clazz))
                    && memberInfoOrNull.getType().isTimeDuration();
        } else
        {
            return (clazz == HDF5TimeDuration.class);
        }
    }

    @Override
    public Class<?> tryGetOverrideJavaType(HDF5DataClass dataClass, int rank, int elementSize,
            HDF5DataTypeVariant typeVariantOrNull)
    {
        if (dataClass == HDF5DataClass.INTEGER && rank == 0 && elementSize == 8
                && typeVariantOrNull != null && typeVariantOrNull.isTimeDuration())
        {
            return HDF5TimeDuration.class;
        } else
        {
            return null;
        }
    }

    @Override
    public HDF5MemberByteifyer createBytifyer(AccessType accessType, Field fieldOrNull,
            HDF5CompoundMemberMapping member,
            HDF5CompoundMemberInformation compoundMemberInfoOrNull,
            HDF5EnumerationType enumTypeOrNull, Class<?> memberClazz, int index, int offset,
            int memOffset, IFileAccessProvider fileInfoProvider)
    {
        final String memberName = member.getMemberName();
        final HDF5DataTypeVariant typeVariant =
                HDF5DataTypeVariant.isTypeVariant(member.tryGetTypeVariant()) ? member
                        .tryGetTypeVariant() : HDF5DataTypeVariant.TIME_DURATION_MICROSECONDS;
        switch (accessType)
        {
            case FIELD:
                return createByteifyerForField(fieldOrNull, memberName, offset, memOffset,
                        typeVariant);
            case MAP:
                return createByteifyerForMap(memberName, offset, memOffset, typeVariant);
            case LIST:
                return createByteifyerForList(memberName, index, offset, memOffset, typeVariant);
            case ARRAY:
                return createByteifyerForArray(memberName, index, offset, memOffset,
                        typeVariant);
            default:
                throw new Error("Unknown access type");
        }
    }

    private HDF5MemberByteifyer createByteifyerForField(final Field field, final String memberName,
            final int offset, int memOffset, final HDF5DataTypeVariant typeVariant)
    {
        ReflectionUtils.ensureAccessible(field);
        return new HDF5MemberByteifyer(field, memberName, LONG_SIZE, offset, memOffset,
                false, typeVariant)
            {
                final HDF5TimeUnit timeUnit = HDF5DataTypeVariant.getTimeUnit(typeVariant);

                @Override
                int getElementSize()
                {
                    return 8;
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return H5T_STD_I64LE;
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
                    final HDF5TimeDuration duration = (HDF5TimeDuration) field.get(obj);
                    return HDFNativeData.longToByte(timeUnit.convert(duration.getValue(),
                            duration.getUnit()));
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    field.set(
                            obj,
                            new HDF5TimeDuration(HDFNativeData.byteToLong(byteArr, arrayOffset
                                    + offsetInMemory), timeUnit));
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForMap(final String memberName, final int offset,
            int memOffset, final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, LONG_SIZE, offset, memOffset,
                false, typeVariant)
            {
                final HDF5TimeUnit timeUnit = HDF5DataTypeVariant.getTimeUnit(typeVariant);

                @Override
                int getElementSize()
                {
                    return 8;
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return H5T_STD_I64LE;
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
                    final Object durationObj = getMap(obj, memberName);
                    if (durationObj instanceof HDF5TimeDuration)
                    {
                        final HDF5TimeDuration duration = (HDF5TimeDuration) durationObj;
                        return HDFNativeData.longToByte(timeUnit.convert(duration.getValue(),
                                duration.getUnit()));
                    } else
                    {
                        return HDFNativeData.longToByte(((Number) durationObj).longValue());
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    putMap(obj,
                            memberName,
                            new HDF5TimeDuration(HDFNativeData.byteToLong(byteArr, arrayOffset
                                    + offsetInMemory), timeUnit));
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForList(final String memberName, final int index,
            final int offset, int memOffset, final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, LONG_SIZE, offset, memOffset,
                false, typeVariant)
            {
                final HDF5TimeUnit timeUnit = HDF5DataTypeVariant.getTimeUnit(typeVariant);

                @Override
                int getElementSize()
                {
                    return 8;
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return H5T_STD_I64LE;
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
                    final Object durationObj = getList(obj, index);
                    if (durationObj instanceof HDF5TimeDuration)
                    {
                        final HDF5TimeDuration duration = (HDF5TimeDuration) durationObj;
                        return HDFNativeData.longToByte(timeUnit.convert(duration.getValue(),
                                duration.getUnit()));
                    } else
                    {
                        return HDFNativeData.longToByte(((Number) durationObj).longValue());
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    setList(obj,
                            index,
                            new HDF5TimeDuration(HDFNativeData.byteToLong(byteArr, arrayOffset
                                    + offsetInMemory), timeUnit));
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForArray(final String memberName, final int index,
            final int offset, int memOffset, final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, LONG_SIZE, offset, memOffset,
                false, typeVariant)
            {
                final HDF5TimeUnit timeUnit = HDF5DataTypeVariant.getTimeUnit(typeVariant);

                @Override
                int getElementSize()
                {
                    return 8;
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return H5T_STD_I64LE;
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
                    final Object durationObj = getArray(obj, index);
                    if (durationObj instanceof HDF5TimeDuration)
                    {
                        final HDF5TimeDuration duration = (HDF5TimeDuration) durationObj;
                        return HDFNativeData.longToByte(timeUnit.convert(duration.getValue(),
                                duration.getUnit()));
                    } else
                    {
                        return HDFNativeData.longToByte(((Number) durationObj).longValue());
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    setArray(
                            obj,
                            index,
                            new HDF5TimeDuration(HDFNativeData.byteToLong(byteArr, arrayOffset
                                    + offsetInMemory), timeUnit));
                }
            };
    }

}
