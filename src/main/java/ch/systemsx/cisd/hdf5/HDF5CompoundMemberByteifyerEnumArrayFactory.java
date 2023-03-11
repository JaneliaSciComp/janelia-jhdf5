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
import static ch.systemsx.cisd.hdf5.HDF5CompoundMappingHints.getEnumReturnType;
import static ch.systemsx.cisd.hdf5.HDF5CompoundMemberByteifyerEnumFactory.getEnumReturnTypeFromField;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.AccessType;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory;
import ch.systemsx.cisd.hdf5.HDF5CompoundMappingHints.EnumReturnType;
import ch.systemsx.cisd.hdf5.HDF5ValueObjectByteifyer.IFileAccessProvider;

/**
 * A {@link HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory} for
 * <code>HDF5EnumerationValueArray</code>.
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundMemberByteifyerEnumArrayFactory implements IHDF5CompoundMemberBytifyerFactory
{

    @Override
    public boolean canHandle(Class<?> clazz, HDF5CompoundMemberInformation memberInfoOrNull)
    {
        if (memberInfoOrNull != null)
        {
            return ((clazz == HDF5EnumerationValueArray.class)
                    || (clazz.isArray() && clazz.getComponentType().isEnum())
                    || clazz == String[].class || (clazz.isArray() && (Number.class
                    .isAssignableFrom(clazz.getComponentType()) || (clazz.getComponentType()
                    .isPrimitive() && clazz.getComponentType() != boolean.class))))
                    && memberInfoOrNull.getType().getDataClass() == HDF5DataClass.ENUM;
        } else
        {
            return (clazz == HDF5EnumerationValueArray.class)
                    || (clazz.isArray() && clazz.getComponentType().isEnum());
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
            HDF5CompoundMemberInformation compoundMemberInfoOrNull,
            HDF5EnumerationType compoundMemberInfoEnumTypeOrNull, Class<?> memberClazz,
            final int index, final int offset, int memOffset, final IFileAccessProvider fileInfoProvider)
    {
        final String memberName = member.getMemberName();
        HDF5EnumerationType enumTypeOrNull =
                member.tryGetEnumerationType() != null ? member.tryGetEnumerationType()
                        : compoundMemberInfoEnumTypeOrNull;
        if (enumTypeOrNull == null)
        {
            if (fieldOrNull.getType().isArray()
                    && fieldOrNull.getType().getComponentType().isEnum())
            {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumClass =
                        (Class<? extends Enum<?>>) fieldOrNull.getType().getComponentType();
                enumTypeOrNull =
                        fileInfoProvider.getEnumType(ReflectionUtils.getEnumOptions(enumClass));
            } else
            {
                throw new HDF5JavaException("Enumeration type not known for member byteifyer.");
            }
        }
        final int memberTypeLength =
                (compoundMemberInfoOrNull != null) ? compoundMemberInfoOrNull.getType()
                        .getNumberOfElements() : member.getMemberTypeLength();
        final long storageTypeId = member.getStorageDataTypeId();
        final long memberStorageTypeId =
                (storageTypeId < 0) ? fileInfoProvider.getArrayTypeId(
                        enumTypeOrNull.getStorageTypeId(), memberTypeLength) : storageTypeId;
        switch (accessType)
        {
            case FIELD:
            {
                if (fieldOrNull == null)
                {
                    throw new HDF5JavaException("No field for member " + memberName + ".");
                }
                return createByteifyerForField(fieldOrNull, memberName, offset, memOffset,
                        enumTypeOrNull, memberTypeLength, memberStorageTypeId, member.tryGetTypeVariant(),
                        getEnumReturnTypeFromField(fieldOrNull.getType()));
            }
            case MAP:
                return createByteifyerForMap(memberName, offset, memOffset, enumTypeOrNull,
                        memberTypeLength, memberStorageTypeId, member.tryGetTypeVariant(),
                        getEnumReturnType(member));
            case LIST:
                return createByteifyerForList(memberName, index, offset, memOffset, enumTypeOrNull,
                        memberTypeLength, memberStorageTypeId, member.tryGetTypeVariant(),
                        getEnumReturnType(member));
            case ARRAY:
                return createByteifyerForArray(memberName, index, offset, memOffset,
                        enumTypeOrNull, memberTypeLength, memberStorageTypeId, member.tryGetTypeVariant(),
                        getEnumReturnType(member));
        }
        throw new Error("Unknown access type");
    }

    private HDF5MemberByteifyer createByteifyerForField(final Field field, final String memberName,
            final int offset, int memOffset, final HDF5EnumerationType enumType,
            final int memberTypeLength, final long memberStorageTypeId, final HDF5DataTypeVariant typeVariant,
            final EnumReturnType enumReturnType)
    {
        ReflectionUtils.ensureAccessible(field);
        return new HDF5MemberByteifyer(field, memberName, enumType.getStorageForm()
                .getStorageSize() * memberTypeLength, offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return enumType.getStorageForm().getStorageSize();
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return memberStorageTypeId;
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
                    return getEnumArray(obj).toStorageForm();
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final Object enumValueArray =
                            getEnumValue(enumType, byteArr, arrayOffset + offsetInMemory,
                                    memberTypeLength, enumReturnType, field);
                    field.set(obj, enumValueArray);
                }

                private HDF5EnumerationValueArray getEnumArray(Object obj)
                        throws IllegalAccessException, IllegalArgumentException
                {
                    assert obj != null;
                    final Object enumArrayObj = field.get(obj);
                    return getEnumArrayFromField(enumArrayObj, enumType, enumReturnType);
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForMap(final String memberName, final int offset,
            int memOffset, final HDF5EnumerationType enumType, final int memberTypeLength,
            final long memberStorageTypeId, final HDF5DataTypeVariant typeVariant,
            final HDF5CompoundMappingHints.EnumReturnType enumReturnType)
    {
        return new HDF5MemberByteifyer(null, memberName, enumType.getStorageForm().getStorageSize()
                * memberTypeLength, offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return enumType.getStorageForm().getStorageSize();
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return memberStorageTypeId;
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
                    return getEnumArray(obj).toStorageForm();
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final Object enumValueArray =
                            getEnumValue(enumType, byteArr, arrayOffset + offsetInMemory,
                                    memberTypeLength, enumReturnType, null);
                    putMap(obj, memberName, enumValueArray);
                }

                private HDF5EnumerationValueArray getEnumArray(Object obj)
                        throws IllegalAccessException, IllegalArgumentException
                {
                    assert obj != null;
                    final Object enumArrayObj = getMap(obj, memberName);
                    return guessEnumArray(enumArrayObj, enumType);
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForList(final String memberName, final int index,
            final int offset, int memOffset, final HDF5EnumerationType enumType,
            final int memberTypeLength, final long memberStorageTypeId, final HDF5DataTypeVariant typeVariant,
            final HDF5CompoundMappingHints.EnumReturnType enumReturnType)
    {
        return new HDF5MemberByteifyer(null, memberName, enumType.getStorageForm().getStorageSize()
                * memberTypeLength, offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return enumType.getStorageForm().getStorageSize();
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return memberStorageTypeId;
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
                    return getEnumArray(obj).toStorageForm();
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final Object enumValueArray =
                            getEnumValue(enumType, byteArr, arrayOffset + offsetInMemory,
                                    memberTypeLength, enumReturnType, null);
                    setList(obj, index, enumValueArray);
                }

                private HDF5EnumerationValueArray getEnumArray(Object obj)
                        throws IllegalAccessException, IllegalArgumentException
                {
                    assert obj != null;
                    final Object enumArrayObj = getList(obj, index);
                    return guessEnumArray(enumArrayObj, enumType);
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForArray(final String memberName, final int index,
            final int offset, int memOffset, final HDF5EnumerationType enumType,
            final int memberTypeLength, final long memberStorageTypeId, final HDF5DataTypeVariant typeVariant,
            final HDF5CompoundMappingHints.EnumReturnType enumReturnType)
    {
        return new HDF5MemberByteifyer(null, memberName, enumType.getStorageForm().getStorageSize()
                * memberTypeLength, offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return enumType.getStorageForm().getStorageSize();
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return memberStorageTypeId;
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
                    return getEnumArray(obj).toStorageForm();
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final Object enumValueArray =
                            getEnumValue(enumType, byteArr, arrayOffset + offsetInMemory,
                                    memberTypeLength, enumReturnType, null);
                    setArray(obj, index, enumValueArray);
                }

                private HDF5EnumerationValueArray getEnumArray(Object obj)
                        throws IllegalAccessException, IllegalArgumentException
                {
                    assert obj != null;
                    final Object enumArrayObj = getArray(obj, index);
                    return guessEnumArray(enumArrayObj, enumType);
                }
            };
    }

    static HDF5EnumerationValueArray guessEnumArray(final Object enumArrayObj,
            final HDF5EnumerationType enumType)
    {
        if (enumArrayObj instanceof HDF5EnumerationValueArray)
        {
            return (HDF5EnumerationValueArray) enumArrayObj;
        } else if (enumArrayObj instanceof int[])
        {
            return new HDF5EnumerationValueArray(enumType, (int[]) enumArrayObj);
        } else if (enumArrayObj instanceof String[])
        {
            return new HDF5EnumerationValueArray(enumType, (String[]) enumArrayObj);
        } else if (enumArrayObj.getClass().isArray()
                && enumArrayObj.getClass().getComponentType().isEnum())
        {
            return new HDF5EnumerationValueArray(enumType, (Enum<?>[]) enumArrayObj);
        } else
        {
            final String[] options = new String[Array.getLength(enumArrayObj)];
            for (int i = 0; i < options.length; ++i)
            {
                options[i] = Array.get(enumArrayObj, i).toString();
            }
            return new HDF5EnumerationValueArray(enumType, options);
        }
    }

    static HDF5EnumerationValueArray getEnumArrayFromField(final Object enumArrayObj,
            final HDF5EnumerationType enumType,
            final HDF5CompoundMappingHints.EnumReturnType enumReturnType)
    {
        switch (enumReturnType)
        {
            case HDF5ENUMERATIONVALUE:
                return (HDF5EnumerationValueArray) enumArrayObj;
            case STRING:
                return new HDF5EnumerationValueArray(enumType, (String[]) enumArrayObj);
            case ORDINAL:
                return new HDF5EnumerationValueArray(enumType, enumArrayObj);
            case JAVAENUMERATION:
            {
                return new HDF5EnumerationValueArray(enumType, (Enum<?>[]) enumArrayObj);
            }
        }
        throw new Error("Unknown EnumReturnType " + enumReturnType);
    }

    static Object getEnumValue(final HDF5EnumerationType enumType, byte[] byteArr, int arrayOffset,
            final int length, final HDF5CompoundMappingHints.EnumReturnType enumReturnType,
            Field fieldOrNull)
    {
        switch (enumReturnType)
        {
            case HDF5ENUMERATIONVALUE:
                return HDF5EnumerationValueArray.fromStorageForm(enumType, byteArr, arrayOffset,
                        length);
            case STRING:
                return HDF5EnumerationValueArray.fromStorageFormToStringArray(enumType, byteArr,
                        arrayOffset, length);
            case ORDINAL:
                return HDF5EnumerationValueArray.fromStorageFormToIntArray(enumType, byteArr,
                        arrayOffset, length);
            case JAVAENUMERATION:
            {
                if (fieldOrNull == null)
                {
                    throw new HDF5JavaException(
                            "JAVAENUMERATIONTYPE only available with access type FIELD");
                }
                final String[] values =
                        HDF5EnumerationValueArray.fromStorageFormToStringArray(enumType, byteArr,
                                arrayOffset, length);
                @SuppressWarnings("unchecked")
                final Class<Enum<?>> enumClass =
                        (Class<Enum<?>>) fieldOrNull.getType().getComponentType();
                final Enum<?>[] result =
                        (Enum<?>[]) Array.newInstance(fieldOrNull.getType().getComponentType(),
                                values.length);
                for (int i = 0; i < result.length; ++i)
                {
                    result[i] = getValue(enumClass, values[i]);
                }
                return result;
            }
        }
        throw new Error("Unknown EnumReturnType " + enumReturnType);
    }

    /**
     * Returns the value as Enum of type <var>enumClass</var>.
     */
    private static Enum<?> getValue(Class<? extends Enum<?>> enumClass, String value)
    {
        return Enum.valueOf(enumClass.asSubclass(Enum.class), value);
    }

}
