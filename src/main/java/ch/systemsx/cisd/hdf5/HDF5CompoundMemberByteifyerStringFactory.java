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
import static hdf.hdf5lib.HDF5Constants.H5T_STD_REF_OBJ;

import java.lang.reflect.Field;

import ch.systemsx.cisd.base.convert.NativeData;
import ch.systemsx.cisd.base.convert.NativeData.ByteOrder;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.AccessType;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory;
import ch.systemsx.cisd.hdf5.HDF5ValueObjectByteifyer.IFileAccessProvider;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;

/**
 * A {@link HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory} for <code>String</code>
 * .
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundMemberByteifyerStringFactory implements IHDF5CompoundMemberBytifyerFactory
{

    private static abstract class HDF5StringMemberByteifyer extends HDF5MemberByteifyer
    {
        HDF5StringMemberByteifyer(Field fieldOrNull, String memberName, int size, int offset,
                int memOffset, CharacterEncoding encoding, int maxCharacters,
                boolean isVariableLengthType, boolean isReferenceType)
        {
            super(fieldOrNull, memberName, size, offset, memOffset, encoding, maxCharacters,
                    isVariableLengthType, isReferenceType);
        }

        /**
         * For strings, this is the <i>minimal</i> element size 1 for fixed strings or the size of a
         * pointer for variable-length strings.
         */
        @Override
        int getElementSize()
        {
            return isVariableLengthType() ? HDFHelper.getMachineWordSize() : 1;
        }

        @Override
        public boolean mayBeCut()
        {
            return true;
        }

        @Override
        protected long getMemberNativeTypeId()
        {
            return -1;
        }
    }

    @Override
    public boolean canHandle(Class<?> clazz, HDF5CompoundMemberInformation memberInfoOrNull)
    {
        if (memberInfoOrNull != null)
        {
            final HDF5DataClass dataClass = memberInfoOrNull.getType().getDataClass();
            return ((clazz == String.class) || (clazz == char[].class))
                    && (dataClass == HDF5DataClass.STRING || dataClass == HDF5DataClass.REFERENCE);
        } else
        {
            return (clazz == String.class) || (clazz == char[].class);
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
            int memOffset, IFileAccessProvider fileAccessProvider)
    {
        final String memberName = member.getMemberName();
        final int maxCharacters = member.getMemberTypeLength();
        final boolean isVariableLengthType =
                member.isVariableLength()
                        || (maxCharacters == 0 && member.tryGetHints() != null && member
                                .tryGetHints().isUseVariableLengthStrings());
        final boolean isReferenceType = member.isReference();
        // May be -1 if not known
        final long memberTypeId =
                isVariableLengthType ? fileAccessProvider.getVariableLengthStringDataTypeId()
                        : member.getStorageDataTypeId();
        final CharacterEncoding encoding =
                isReferenceType ? CharacterEncoding.ASCII : fileAccessProvider
                        .getCharacterEncoding(memberTypeId);
        final int size =
                (compoundMemberInfoOrNull != null) ? compoundMemberInfoOrNull.getType().getSize()
                        : encoding.getMaxBytesPerChar() * maxCharacters;
        final long stringOrRefDataTypeId =
                (memberTypeId < 0) ? (isReferenceType ? H5T_STD_REF_OBJ : fileAccessProvider
                        .getStringDataTypeId(size)) : memberTypeId;
        final boolean isCharArray = (memberClazz == char[].class);
        switch (accessType)
        {
            case FIELD:
                return createByteifyerForField(fieldOrNull, memberName, fileAccessProvider, offset,
                        memOffset, stringOrRefDataTypeId, maxCharacters, size, encoding,
                        isCharArray, isVariableLengthType, isReferenceType);
            case MAP:
                return createByteifyerForMap(memberName, fileAccessProvider, offset, memOffset,
                        stringOrRefDataTypeId, maxCharacters, size, encoding, isCharArray,
                        isVariableLengthType, isReferenceType);
            case LIST:
                return createByteifyerForList(memberName, fileAccessProvider, index, offset,
                        memOffset, stringOrRefDataTypeId, maxCharacters, size, encoding,
                        isCharArray, isVariableLengthType, isReferenceType);
            case ARRAY:
                return createByteifyerForArray(memberName, fileAccessProvider, index, offset,
                        memOffset, stringOrRefDataTypeId, maxCharacters, size, encoding,
                        isCharArray, isVariableLengthType, isReferenceType);
            default:
                throw new Error("Unknown access type");
        }
    }

    private static String refToStr(byte[] byteArr, int offset)
    {
        final long reference = NativeData.byteToLong(byteArr, ByteOrder.NATIVE, offset, 1)[0];
        return '\0' + Long.toString(reference);
    }

    static String bytesToString(byte[] byteArr, final int totalOffset, final int maxIdx,
            CharacterEncoding encoding, final boolean isVariableLengthType,
            final boolean isReferenceType)
    {
        final String s;
        if (isVariableLengthType)
        {
            s = HDFHelper.createVLStrFromCompound(byteArr, totalOffset);
        } else if (isReferenceType)
        {
            s = refToStr(byteArr, totalOffset);
        } else
        {
            s = StringUtils.fromBytes0Term(byteArr, totalOffset, maxIdx, encoding);
        }
        return s;
    }

    private HDF5MemberByteifyer createByteifyerForField(final Field field, final String memberName,
            final IFileAccessProvider fileAccessProvider, final int offset, int memOffset,
            final long stringOrRefDataTypeId, final int maxCharacters, final int size,
            final CharacterEncoding encoding, final boolean isCharArray,
            final boolean isVariableLengthType, final boolean isReferenceType)
    {
        ReflectionUtils.ensureAccessible(field);
        return new HDF5StringMemberByteifyer(field, memberName, size, offset, memOffset, encoding,
                maxCharacters, isVariableLengthType, isReferenceType)
            {
                @Override
                protected long getMemberStorageTypeId()
                {
                    return stringOrRefDataTypeId;
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    Object o = field.get(obj);
                    if (o == null)
                    {
                        throw new NullPointerException("Field '" + field.getName() + "' is null");

                    }
                    final String s = isCharArray ? new String((char[]) o) : o.toString();
                    if (isVariableLengthType)
                    {
                        final byte[] result = new byte[HDFHelper.getMachineWordSize()];
                        HDFHelper.compoundCpyVLStr(s, result, 0);
                        return result;
                    } else if (isReferenceType)
                    {
                        return fileAccessProvider.createObjectReference(s);
                    } else
                    {
                        return StringUtils.toBytes0Term(s, getMaxCharacters(), encoding);
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final int totalOffset = arrayOffset + offsetInMemory;
                    final int maxIdx = totalOffset + maxCharacters;
                    final String s =
                            bytesToString(byteArr, totalOffset, maxIdx, encoding,
                                    isVariableLengthType, isReferenceType);
                    field.set(obj, isCharArray ? s.toCharArray() : s);
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForMap(final String memberName,
            final IFileAccessProvider fileAccessProvider, final int offset, int memOffset,
            final long stringOrRefDataTypeId, final int maxCharacters, final int size,
            final CharacterEncoding encoding, final boolean isCharArray,
            final boolean isVariableLengthType, final boolean isReferenceType)
    {
        return new HDF5StringMemberByteifyer(null, memberName, size, offset, memOffset, encoding,
                maxCharacters, isVariableLengthType, isReferenceType)
            {
                @Override
                protected long getMemberStorageTypeId()
                {
                    return stringOrRefDataTypeId;
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    final Object o = getMap(obj, memberName);
                    final String s;
                    if (o.getClass() == char[].class)
                    {
                        s = new String((char[]) o);
                    } else
                    {
                        s = o.toString();
                    }
                    if (isVariableLengthType)
                    {
                        final byte[] result = new byte[HDFHelper.getMachineWordSize()];
                        HDFHelper.compoundCpyVLStr(s, result, 0);
                        return result;
                    } else if (isReferenceType)
                    {
                        return fileAccessProvider.createObjectReference(s);
                    } else
                    {
                        return StringUtils.toBytes0Term(s, getMaxCharacters(), encoding);
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final int totalOffset = arrayOffset + offsetInMemory;
                    final int maxIdx = totalOffset + maxCharacters;
                    final String s =
                            bytesToString(byteArr, totalOffset, maxIdx, encoding,
                                    isVariableLengthType, isReferenceType);
                    if (isCharArray)
                    {
                        putMap(obj, memberName, s.toCharArray());
                    } else
                    {
                        putMap(obj, memberName, s);
                    }
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForList(final String memberName,
            final IFileAccessProvider fileAccessProvider, final int index, final int offset,
            int memOffset, final long stringOrRefDataTypeId, final int maxCharacters,
            final int size, final CharacterEncoding encoding, final boolean isCharArray,
            final boolean isVariableLengthType, final boolean isReferenceType)
    {
        return new HDF5StringMemberByteifyer(null, memberName, size, offset, memOffset, encoding,
                maxCharacters, isVariableLengthType, isReferenceType)
            {
                @Override
                protected long getMemberStorageTypeId()
                {
                    return stringOrRefDataTypeId;
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    final Object o = getList(obj, index);
                    final String s;
                    if (o.getClass() == char[].class)
                    {
                        s = new String((char[]) o);
                    } else
                    {
                        s = o.toString();
                    }
                    if (isVariableLengthType)
                    {
                        final byte[] result = new byte[HDFHelper.getMachineWordSize()];
                        HDFHelper.compoundCpyVLStr(s, result, 0);
                        return result;
                    } else if (isReferenceType)
                    {
                        return fileAccessProvider.createObjectReference(s);
                    } else
                    {
                        return StringUtils.toBytes0Term(s, getMaxCharacters(), encoding);
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final int totalOffset = arrayOffset + offsetInMemory;
                    final int maxIdx = totalOffset + maxCharacters;
                    final String s =
                            bytesToString(byteArr, totalOffset, maxIdx, encoding,
                                    isVariableLengthType, isReferenceType);
                    if (isCharArray)
                    {
                        setList(obj, index, s.toCharArray());
                    } else
                    {
                        setList(obj, index, s);
                    }
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForArray(final String memberName,
            final IFileAccessProvider fileAccessProvider, final int index, final int offset,
            int memOffset, final long stringOrRefDataTypeId, final int maxCharacters,
            final int size, final CharacterEncoding encoding, final boolean isCharArray,
            final boolean isVariableLengthType, final boolean isReferenceType)
    {
        return new HDF5StringMemberByteifyer(null, memberName, size, offset, memOffset, encoding,
                maxCharacters, isVariableLengthType, isReferenceType)
            {
                @Override
                protected long getMemberStorageTypeId()
                {
                    return stringOrRefDataTypeId;
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    final Object o = getArray(obj, index);
                    final String s;
                    if (o.getClass() == char[].class)
                    {
                        s = new String((char[]) o);
                    } else
                    {
                        s = o.toString();
                    }
                    if (isVariableLengthType)
                    {
                        final byte[] result = new byte[HDFHelper.getMachineWordSize()];
                        HDFHelper.compoundCpyVLStr(s, result, 0);
                        return result;
                    } else if (isReferenceType)
                    {
                        return fileAccessProvider.createObjectReference(s);
                    } else
                    {
                        return StringUtils.toBytes0Term(s, getMaxCharacters(), encoding);
                    }
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final int totalOffset = arrayOffset + offsetInMemory;
                    final int maxIdx = totalOffset + maxCharacters;
                    final String s =
                            bytesToString(byteArr, totalOffset, maxIdx, encoding,
                                    isVariableLengthType, isReferenceType);
                    if (isCharArray)
                    {
                        setArray(obj, index, s.toCharArray());
                    } else
                    {
                        setArray(obj, index, s);
                    }
                }
            };
    }

}
