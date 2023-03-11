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

import static ch.systemsx.cisd.base.convert.NativeData.DOUBLE_SIZE;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getArray;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getList;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.getMap;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.putMap;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.setArray;
import static ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.setList;
import static hdf.hdf5lib.HDF5Constants.H5T_IEEE_F64LE;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

import ch.systemsx.cisd.base.mdarray.MDDoubleArray;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.AccessType;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory;
import ch.systemsx.cisd.hdf5.HDF5ValueObjectByteifyer.IFileAccessProvider;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;
import hdf.hdf5lib.HDFNativeData;

/**
 * A {@link HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory} for <code>double</code>
 * , <code>double[]</code>, <code>double[][]</code> and <code>MDDoubleArray</code>.
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundMemberByteifyerDoubleFactory implements IHDF5CompoundMemberBytifyerFactory
{

    private static Map<Class<?>, Rank> classToRankMap = new IdentityHashMap<Class<?>, Rank>();

    private enum Rank
    {
        SCALAR(double.class, 0), ARRAY1D(double[].class, 1), ARRAY2D(double[][].class, 2), ARRAYMD(
                MDDoubleArray.class, -1);

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
            if (rankOrNull == null || typeInfo.getDataClass() != HDF5DataClass.FLOAT
                    || typeInfo.getElementSize() != DOUBLE_SIZE)
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
                rank.isScalar() ? H5T_IEEE_F64LE : ((storageTypeId < 0) ? fileInfoProvider
                        .getArrayTypeId(H5T_IEEE_F64LE, dimensions) : storageTypeId);
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
        return new HDF5MemberByteifyer(field, memberName, DOUBLE_SIZE * len, offset,
                memOffset, false, typeVariant)
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
                    switch (rank)
                    {
                        case SCALAR:
                            return HDFNativeData.doubleToByte(field.getDouble(obj));
                        case ARRAY1D:
                            return HDFHelper.doubleToByte((double[]) field.get(obj));
                        case ARRAY2D:
                        {
                            final double[][] array = (double[][]) field.get(obj);
                            MatrixUtils.checkMatrixDimensions(memberName, dimensions, array);
                            return HDFHelper.doubleToByte(MatrixUtils.flatten(array));
                        }
                        case ARRAYMD:
                        {
                            final MDDoubleArray array = (MDDoubleArray) field.get(obj);
                            MatrixUtils.checkMDArrayDimensions(memberName, dimensions, array);
                            return HDFHelper.doubleToByte(array.getAsFlatArray());
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
                            field.setDouble(obj,
                                    HDFNativeData.byteToDouble(byteArr, arrayOffset + offsetInMemory));
                            break;
                        case ARRAY1D:
                            field.set(obj, HDFHelper.byteToDouble(byteArr, arrayOffset
                                    + offsetInMemory, len));
                            break;
                        case ARRAY2D:
                        {
                            final double[] array =
                                    HDFHelper.byteToDouble(byteArr, arrayOffset + offsetInMemory,
                                            len);
                            field.set(obj, MatrixUtils.shapen(array, dimensions));
                            break;
                        }
                        case ARRAYMD:
                        {
                            final double[] array =
                                    HDFHelper.byteToDouble(byteArr, arrayOffset + offsetInMemory,
                                            len);
                            field.set(obj, new MDDoubleArray(array, dimensions));
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
        return new HDF5MemberByteifyer(null, memberName, DOUBLE_SIZE * len, offset, memOffset,
                false, typeVariant)
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
                    switch (rank)
                    {
                        case SCALAR:
                            return HDFNativeData.doubleToByte(((Number) getMap(obj, memberName))
                                    .doubleValue());
                        case ARRAY1D:
                            return HDFHelper.doubleToByte((double[]) getMap(obj, memberName));
                        case ARRAY2D:
                        {
                            final double[][] array = (double[][]) getMap(obj, memberName);
                            MatrixUtils.checkMatrixDimensions(memberName, dimensions, array);
                            return HDFHelper.doubleToByte(MatrixUtils.flatten(array));
                        }
                        case ARRAYMD:
                        {
                            final MDDoubleArray array = (MDDoubleArray) getMap(obj, memberName);
                            MatrixUtils.checkMDArrayDimensions(memberName, dimensions, array);
                            return HDFHelper.doubleToByte(array.getAsFlatArray());
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
                            putMap(obj, memberName,
                                    HDFNativeData.byteToDouble(byteArr, arrayOffset + offsetInMemory));
                            break;
                        case ARRAY1D:
                            putMap(obj, memberName, HDFHelper.byteToDouble(byteArr, arrayOffset
                                    + offsetInMemory, len));
                            break;
                        case ARRAY2D:
                        {
                            final double[] array =
                                    HDFHelper.byteToDouble(byteArr, arrayOffset + offsetInMemory,
                                            len);
                            putMap(obj, memberName, MatrixUtils.shapen(array, dimensions));
                            break;
                        }
                        case ARRAYMD:
                        {
                            final double[] array =
                                    HDFHelper.byteToDouble(byteArr, arrayOffset + offsetInMemory,
                                            len);
                            putMap(obj, memberName, new MDDoubleArray(array, dimensions));
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
        return new HDF5MemberByteifyer(null, memberName, DOUBLE_SIZE * len, offset, memOffset,
                false, typeVariant)
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
                    switch (rank)
                    {
                        case SCALAR:
                            return HDFNativeData.doubleToByte(((Number) getList(obj, index))
                                    .doubleValue());
                        case ARRAY1D:
                            return HDFHelper.doubleToByte((double[]) getList(obj, index));
                        case ARRAY2D:
                        {
                            final double[][] array = (double[][]) getList(obj, index);
                            MatrixUtils.checkMatrixDimensions(memberName, dimensions, array);
                            return HDFHelper.doubleToByte(MatrixUtils.flatten(array));
                        }
                        case ARRAYMD:
                        {
                            final MDDoubleArray array = (MDDoubleArray) getList(obj, index);
                            MatrixUtils.checkMDArrayDimensions(memberName, dimensions, array);
                            return HDFHelper.doubleToByte(array.getAsFlatArray());
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
                            setList(obj, index,
                                    HDFNativeData.byteToDouble(byteArr, arrayOffset + offsetInMemory));
                            break;
                        case ARRAY1D:
                            putMap(obj, memberName, HDFHelper.byteToDouble(byteArr, arrayOffset
                                    + offsetInMemory, len));
                            break;
                        case ARRAY2D:
                        {
                            final double[] array =
                                    HDFHelper.byteToDouble(byteArr, arrayOffset + offsetInMemory,
                                            len);
                            setList(obj, index, MatrixUtils.shapen(array, dimensions));
                            break;
                        }
                        case ARRAYMD:
                        {
                            final double[] array =
                                    HDFHelper.byteToDouble(byteArr, arrayOffset + offsetInMemory,
                                            len);
                            setList(obj, index, new MDDoubleArray(array, dimensions));
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
        return new HDF5MemberByteifyer(null, memberName, DOUBLE_SIZE * len, offset, memOffset,
                false, typeVariant)
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
                    switch (rank)
                    {
                        case SCALAR:
                            return HDFNativeData.doubleToByte(((Number) getArray(obj, index))
                                    .doubleValue());
                        case ARRAY1D:
                            return HDFHelper.doubleToByte((double[]) getArray(obj, index));
                        case ARRAY2D:
                        {
                            final double[][] array = (double[][]) getArray(obj, index);
                            MatrixUtils.checkMatrixDimensions(memberName, dimensions, array);
                            return HDFHelper.doubleToByte(MatrixUtils.flatten(array));
                        }
                        case ARRAYMD:
                        {
                            final MDDoubleArray array = (MDDoubleArray) getArray(obj, index);
                            MatrixUtils.checkMDArrayDimensions(memberName, dimensions, array);
                            return HDFHelper.doubleToByte(array.getAsFlatArray());
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
                            setArray(obj, index,
                                    HDFNativeData.byteToDouble(byteArr, arrayOffset + offsetInMemory));
                            break;
                        case ARRAY1D:
                            setArray(obj, index, HDFHelper.byteToDouble(byteArr, arrayOffset
                                    + offsetInMemory, len));
                            break;
                        case ARRAY2D:
                        {
                            final double[] array =
                                    HDFHelper.byteToDouble(byteArr, arrayOffset + offsetInMemory,
                                            len);
                            setArray(obj, index, MatrixUtils.shapen(array, dimensions));
                            break;
                        }
                        case ARRAYMD:
                        {
                            final double[] array =
                                    HDFHelper.byteToDouble(byteArr, arrayOffset + offsetInMemory,
                                            len);
                            setArray(obj, index, new MDDoubleArray(array, dimensions));
                            break;
                        }
                        default:
                            throw new Error("Unknown rank.");
                    }
                }
            };
    }

}
