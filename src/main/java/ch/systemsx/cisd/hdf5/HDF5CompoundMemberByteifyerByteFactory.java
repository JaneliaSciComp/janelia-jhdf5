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

import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getArray;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getList;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getMap;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.putMap;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.setArray;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.setList;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I8LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_U8LE;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.AccessType;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory;
import ch.systemsx.cisd.hdf5.HDF5ValueObjectByteifyer.IFileAccessProvider;
import hdf.hdf5lib.HDFNativeData;

/**
 * A {@link HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory} for <code>byte</code>,
 * <code>byte[]</code>, <code>byte[][]</code> and <code>MDByteArray</code>.
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundMemberByteifyerByteFactory implements IHDF5CompoundMemberBytifyerFactory
{

    private static Map<Class<?>, Rank> classToRankMap = new IdentityHashMap<Class<?>, Rank>();

    private enum Rank
    {
        SCALAR(byte.class, 0), ARRAY1D(byte[].class, 1), ARRAY2D(byte[][].class, 2), ARRAYMD(
                MDByteArray.class, -1);

        private final Class<?> clazz;

        private final int rank;

        Rank(Class<?> clazz, int rank)
        {
            this.clazz = clazz;
            this.rank = rank;
        }

        int getRank()
        {
            return rank;
        }

        boolean isScalar()
        {
            return rank == 0;
        }

        boolean anyRank()
        {
            return rank == -1;
        }

        Class<?> getClazz()
        {
            return clazz;
        }
    }

    static
    {
        for (Rank r : Rank.values())
        {
            classToRankMap.put(r.getClazz(), r);
        }
    }

    @Override
    public boolean canHandle(Class<?> clazz, HDF5CompoundMemberInformation memberInfoOrNull)
    {
        final Rank rankOrNull = classToRankMap.get(clazz);
        if (memberInfoOrNull != null)
        {
            final HDF5DataTypeInformation typeInfo = memberInfoOrNull.getType();
            if (rankOrNull == null || typeInfo.getDataClass() != HDF5DataClass.INTEGER
                    || typeInfo.getElementSize() != 1)
            {
                return false;
            }
            return rankOrNull.anyRank()
                    || (rankOrNull.getRank() == typeInfo.getDimensions().length)
                    || (rankOrNull.isScalar() && typeInfo.getDimensions().length == 1 && typeInfo
                            .getDimensions()[0] == 1);

        } else
        {
            return rankOrNull != null;
        }
    }

    @Override
    public Class<?> tryGetOverrideJavaType(HDF5DataClass dataClass, int rank, int elementSize,
            HDF5DataTypeVariant typeVariantOrNull)
    {
        return null;
    }

    @Override
    public HDF5MemberByteifyer createBytifyer(AccessType accessType, Field fieldOrNull,
            HDF5CompoundMemberMapping member,
            HDF5CompoundMemberInformation compoundMemberInfoOrNull,
            HDF5EnumerationType enumTypeOrNull, Class<?> memberClazz, int index, int offset,
            int memOffset, IFileAccessProvider fileInfoProvider)
    {
        final String memberName = member.getMemberName();
        final Rank rank = classToRankMap.get(memberClazz);
        final int len =
                (compoundMemberInfoOrNull != null) ? compoundMemberInfoOrNull.getType()
                        .getNumberOfElements() : rank.isScalar() ? 1 : member.getMemberTypeLength();
        final int[] dimensions = rank.isScalar() ? new int[]
            { 1 } : member.getMemberTypeDimensions();
        final long storageTypeId = member.getStorageDataTypeId();
        final long memberTypeId =
                rank.isScalar() ? (member.isUnsigned() ? H5T_STD_U8LE : H5T_STD_I8LE)
                        : ((storageTypeId < 0) ? fileInfoProvider.getArrayTypeId(
                                member.isUnsigned() ? H5T_STD_U8LE : H5T_STD_I8LE, dimensions)
                                : storageTypeId);
        switch (accessType)
        {
            case FIELD:
                return createByteifyerForField(fieldOrNull, memberName, offset, memOffset,
                        dimensions, len, memberTypeId, rank, member.tryGetTypeVariant());
            case MAP:
                return createByteifyerForMap(memberName, offset, memOffset, dimensions, len,
                        memberTypeId, rank, member.tryGetTypeVariant());
            case LIST:
                return createByteifyerForList(memberName, index, offset, memOffset, dimensions,
                        len, memberTypeId, rank, member.tryGetTypeVariant());
            case ARRAY:
                return createByteifyerForArray(memberName, index, offset, memOffset, dimensions,
                        len, memberTypeId, rank, member.tryGetTypeVariant());
            default:
                throw new Error("Unknown access type");
        }
    }

    private HDF5MemberByteifyer createByteifyerForField(final Field field, final String memberName,
            final int offset, int memOffset, final int[] dimensions, final int len,
            final long memberTypeId, final Rank rank, final HDF5DataTypeVariant typeVariant)
    {
        ReflectionUtils.ensureAccessible(field);
        return new HDF5MemberByteifyer(field, memberName, len, offset, memOffset, false,
                typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return 1;
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
                    switch (rank)
                    {
                        case SCALAR:
                            return HDFNativeData.byteToByte(field.getByte(obj));
                        case ARRAY1D:
                            return (byte[]) field.get(obj);
                        case ARRAY2D:
                        {
                            final byte[][] array = (byte[][]) field.get(obj);
                            MatrixUtils.checkMatrixDimensions(memberName, dimensions, array);
                            return MatrixUtils.flatten(array);
                        }
                        case ARRAYMD:
                        {
                            final MDByteArray array = (MDByteArray) field.get(obj);
                            MatrixUtils.checkMDArrayDimensions(memberName, dimensions, array);
                            return array.getAsFlatArray();
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    switch (rank)
                    {
                        case SCALAR:
                            field.setByte(obj, byteArr[arrayOffset + offsetInMemory]);
                            break;
                        case ARRAY1D:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            field.set(obj, array);
                            break;
                        }
                        case ARRAY2D:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            field.set(obj, MatrixUtils.shapen(array, dimensions));
                            break;
                        }
                        case ARRAYMD:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            field.set(obj, new MDByteArray(array, dimensions));
                            break;
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForMap(final String memberName, final int offset,
            int memOffset, final int[] dimensions, final int len, final long memberTypeId,
            final Rank rank, final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, len, offset, memOffset, false,
                typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return 1;
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
                    switch (rank)
                    {
                        case SCALAR:
                            return HDFNativeData.byteToByte((((Number) getMap(obj, memberName))
                                    .byteValue()));
                        case ARRAY1D:
                            return (byte[]) getMap(obj, memberName);
                        case ARRAY2D:
                        {
                            final byte[][] array = (byte[][]) getMap(obj, memberName);
                            MatrixUtils.checkMatrixDimensions(memberName, dimensions, array);
                            return MatrixUtils.flatten(array);
                        }
                        case ARRAYMD:
                        {
                            final MDByteArray array = (MDByteArray) getMap(obj, memberName);
                            MatrixUtils.checkMDArrayDimensions(memberName, dimensions, array);
                            return array.getAsFlatArray();
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    switch (rank)
                    {
                        case SCALAR:
                            putMap(obj, memberName, byteArr[arrayOffset + offsetInMemory]);
                            break;
                        case ARRAY1D:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            putMap(obj, memberName, array);
                            break;
                        }
                        case ARRAY2D:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            putMap(obj, memberName, MatrixUtils.shapen(array, dimensions));
                            break;
                        }
                        case ARRAYMD:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            putMap(obj, memberName, new MDByteArray(array, dimensions));
                            break;
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForList(final String memberName, final int index,
            final int offset, int memOffset, final int[] dimensions, final int len,
            final long memberTypeId, final Rank rank, final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, len, offset, memOffset, false,
                typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return 1;
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
                    switch (rank)
                    {
                        case SCALAR:
                            return HDFNativeData.byteToByte((((Number) getList(obj, index))
                                    .byteValue()));
                        case ARRAY1D:
                            return (byte[]) getList(obj, index);
                        case ARRAY2D:
                        {
                            final byte[][] array = (byte[][]) getList(obj, index);
                            MatrixUtils.checkMatrixDimensions(memberName, dimensions, array);
                            return MatrixUtils.flatten(array);
                        }
                        case ARRAYMD:
                        {
                            final MDByteArray array = (MDByteArray) getList(obj, index);
                            MatrixUtils.checkMDArrayDimensions(memberName, dimensions, array);
                            return array.getAsFlatArray();
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    switch (rank)
                    {
                        case SCALAR:
                            setList(obj, index, byteArr[arrayOffset + offsetInMemory]);
                            break;
                        case ARRAY1D:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            setList(obj, index, array);
                            break;
                        }
                        case ARRAY2D:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            setList(obj, index, MatrixUtils.shapen(array, dimensions));
                            break;
                        }
                        case ARRAYMD:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            setList(obj, index, new MDByteArray(array, dimensions));
                            break;
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForArray(final String memberName, final int index,
            final int offset, int memOffset, final int[] dimensions, final int len,
            final long memberTypeId, final Rank rank, final HDF5DataTypeVariant typeVariant)
    {
        return new HDF5MemberByteifyer(null, memberName, len, offset, memOffset, false,
                typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return 1;
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
                    switch (rank)
                    {
                        case SCALAR:
                            return HDFNativeData.byteToByte((((Number) getArray(obj, index))
                                    .byteValue()));
                        case ARRAY1D:
                            return (byte[]) getArray(obj, index);
                        case ARRAY2D:
                        {
                            final byte[][] array = (byte[][]) getArray(obj, index);
                            MatrixUtils.checkMatrixDimensions(memberName, dimensions, array);
                            return MatrixUtils.flatten(array);
                        }
                        case ARRAYMD:
                        {
                            final MDByteArray array = (MDByteArray) getArray(obj, index);
                            MatrixUtils.checkMDArrayDimensions(memberName, dimensions, array);
                            return array.getAsFlatArray();
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    switch (rank)
                    {
                        case SCALAR:
                            setArray(obj, index, byteArr[arrayOffset + offsetInMemory]);
                            break;
                        case ARRAY1D:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            setArray(obj, index, array);
                            break;
                        }
                        case ARRAY2D:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            setArray(obj, index, MatrixUtils.shapen(array, dimensions));
                            break;
                        }
                        case ARRAYMD:
                        {
                            final byte[] array = new byte[len];
                            System.arraycopy(byteArr, arrayOffset + offsetInMemory, array, 0,
                                    array.length);
                            setArray(obj, index, new MDByteArray(array, dimensions));
                            break;
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }
            };
    }

}
