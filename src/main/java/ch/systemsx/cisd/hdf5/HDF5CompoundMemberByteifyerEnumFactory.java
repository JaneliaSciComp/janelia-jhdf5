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

import java.lang.reflect.Field;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.AccessType;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory;
import ch.systemsx.cisd.hdf5.HDF5CompoundMappingHints.EnumReturnType;
import ch.systemsx.cisd.hdf5.HDF5ValueObjectByteifyer.IFileAccessProvider;

/**
 * A {@link HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory} for
 * <code>HDF5EnumerationValue</code>.
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundMemberByteifyerEnumFactory implements IHDF5CompoundMemberBytifyerFactory
{

    @Override
    public boolean canHandle(Class<?> clazz, HDF5CompoundMemberInformation memberInfoOrNull)
    {
        if (memberInfoOrNull != null)
        {
            return ((clazz == HDF5EnumerationValue.class) || clazz.isEnum()
                    || clazz == String.class || Number.class.isAssignableFrom(clazz) || (clazz
                    .isPrimitive() && clazz != boolean.class))
                    && memberInfoOrNull.getType().getDataClass() == HDF5DataClass.ENUM;
        } else
        {
            return (clazz == HDF5EnumerationValue.class) || clazz.isEnum();
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
            HDF5EnumerationType compoundMemberInfoEnumTypeOrNull, final Class<?> memberClazz,
            final int index, final int offset, int memOffset, final IFileAccessProvider fileInfoProvider)
    {
        final String memberName = member.getMemberName();
        final HDF5EnumerationType enumTypeOrNull =
                member.tryGetEnumerationType() != null ? member.tryGetEnumerationType()
                        : compoundMemberInfoEnumTypeOrNull;
        if (enumTypeOrNull == null)
        {
            throw new HDF5JavaException("Enumeration type for member '" + memberName
                    + "' not known for member byteifyer.");
        }
        switch (accessType)
        {
            case FIELD:
                return createByteifyerForField(fieldOrNull, memberName, offset, memOffset,
                        enumTypeOrNull, member.tryGetTypeVariant(), getEnumReturnTypeFromField(fieldOrNull.getType()));
            case MAP:
                return createByteifyerForMap(memberName, offset, memOffset, enumTypeOrNull,
                        member.tryGetTypeVariant(), getEnumReturnType(member));
            case LIST:
                return createByteifyerForList(memberName, index, offset, memOffset, enumTypeOrNull,
                        member.tryGetTypeVariant(), getEnumReturnType(member));
            case ARRAY:
                return createByteifyerForArray(memberName, index, offset, memOffset,
                        enumTypeOrNull, member.tryGetTypeVariant(), getEnumReturnType(member));
            default:
                throw new Error("Unknown access type");
        }
    }

    /**
     * Returns the desired enumeration return type.
     */
    static EnumReturnType getEnumReturnTypeFromField(Class<?> type)
    {
        final Class<?> clazz = type.isArray() ? type.getComponentType() : type;
        if (Number.class.isAssignableFrom(clazz) || (clazz.isPrimitive() && clazz != boolean.class))
        {
            return EnumReturnType.ORDINAL;
        } else if (String.class == clazz)
        {
            return EnumReturnType.STRING;
        } else if (clazz.isEnum())
        {
            return EnumReturnType.JAVAENUMERATION;
        }
        {
            return EnumReturnType.HDF5ENUMERATIONVALUE;
        }
    }

    private HDF5MemberByteifyer createByteifyerForField(final Field field, final String memberName,
            final int offset, int memOffset, final HDF5EnumerationType enumType,
            final HDF5DataTypeVariant typeVariant, final EnumReturnType enumReturnType)
    {
        ReflectionUtils.ensureAccessible(field);
        return new HDF5MemberByteifyer(field, memberName, enumType.getStorageForm()
                .getStorageSize(), offset, memOffset, false, typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return enumType.getStorageForm().getStorageSize();
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return enumType.getStorageTypeId();
                }

                @Override
                protected long getMemberNativeTypeId()
                {
                    return enumType.getNativeTypeId();
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    return getEnum(obj).toStorageForm();
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final Object enumValue =
                            getEnumValue(enumType, byteArr, arrayOffset + offsetInMemory,
                                    enumReturnType, field);
                    field.set(obj, enumValue);
                }

                private HDF5EnumerationValue getEnum(Object obj) throws IllegalAccessException,
                        IllegalArgumentException
                {
                    assert obj != null;
                    final Object enumObj = field.get(obj);
                    if (enumObj instanceof HDF5EnumerationValue)
                    {
                        return (HDF5EnumerationValue) enumObj;
                    } else if (enumObj instanceof Number)
                    {
                        return new HDF5EnumerationValue(enumType, ((Number) enumObj).intValue());
                    } else
                    {
                        return new HDF5EnumerationValue(enumType, enumObj.toString());
                    }
                }

                @Override
                HDF5EnumerationType tryGetEnumType()
                {
                    return enumType;
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForMap(final String memberName, final int offset,
            int memOffset, final HDF5EnumerationType enumType, final HDF5DataTypeVariant typeVariant,
            final HDF5CompoundMappingHints.EnumReturnType enumReturnType)
    {
        return new HDF5MemberByteifyer(null, memberName,
                enumType.getStorageForm().getStorageSize(), offset, memOffset, false,
                typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return enumType.getStorageForm().getStorageSize();
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return enumType.getStorageTypeId();
                }

                @Override
                protected long getMemberNativeTypeId()
                {
                    return enumType.getNativeTypeId();
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    return getEnum(obj).toStorageForm();
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final Object enumValue =
                            getEnumValue(enumType, byteArr, arrayOffset + offsetInMemory,
                                    enumReturnType, null);
                    putMap(obj, memberName, enumValue);
                }

                private HDF5EnumerationValue getEnum(Object obj) throws IllegalAccessException,
                        IllegalArgumentException
                {
                    assert obj != null;
                    final Object enumObj = getMap(obj, memberName);
                    if (enumObj instanceof HDF5EnumerationValue)
                    {
                        return (HDF5EnumerationValue) enumObj;
                    } else if (enumObj instanceof Number)
                    {
                        return new HDF5EnumerationValue(enumType, ((Number) enumObj).intValue());
                    } else
                    {
                        return new HDF5EnumerationValue(enumType, enumObj.toString());
                    }
                }

                @Override
                HDF5EnumerationType tryGetEnumType()
                {
                    return enumType;
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForList(final String memberName, final int index,
            final int offset, int memOffset, final HDF5EnumerationType enumType,
            final HDF5DataTypeVariant typeVariant, final HDF5CompoundMappingHints.EnumReturnType enumReturnType)
    {
        return new HDF5MemberByteifyer(null, memberName,
                enumType.getStorageForm().getStorageSize(), offset, memOffset, false,
                typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return enumType.getStorageForm().getStorageSize();
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return enumType.getStorageTypeId();
                }

                @Override
                protected long getMemberNativeTypeId()
                {
                    return enumType.getNativeTypeId();
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    return getEnum(obj).toStorageForm();
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final Object enumValue =
                            getEnumValue(enumType, byteArr, arrayOffset + offsetInMemory,
                                    enumReturnType, null);
                    setList(obj, index, enumValue);
                }

                private HDF5EnumerationValue getEnum(Object obj) throws IllegalAccessException,
                        IllegalArgumentException
                {
                    assert obj != null;
                    final Object enumObj = getList(obj, index);
                    if (enumObj instanceof HDF5EnumerationValue)
                    {
                        return (HDF5EnumerationValue) enumObj;
                    } else if (enumObj instanceof Number)
                    {
                        return new HDF5EnumerationValue(enumType, ((Number) enumObj).intValue());
                    } else
                    {
                        return new HDF5EnumerationValue(enumType, enumObj.toString());
                    }
                }

                @Override
                HDF5EnumerationType tryGetEnumType()
                {
                    return enumType;
                }
            };
    }

    private HDF5MemberByteifyer createByteifyerForArray(final String memberName, final int index,
            final int offset, int memOffset, final HDF5EnumerationType enumType,
            final HDF5DataTypeVariant typeVariant, final HDF5CompoundMappingHints.EnumReturnType enumReturnType)
    {
        return new HDF5MemberByteifyer(null, memberName,
                enumType.getStorageForm().getStorageSize(), offset, memOffset, false,
                typeVariant)
            {
                @Override
                int getElementSize()
                {
                    return enumType.getStorageForm().getStorageSize();
                }

                @Override
                protected long getMemberStorageTypeId()
                {
                    return enumType.getStorageTypeId();
                }

                @Override
                protected long getMemberNativeTypeId()
                {
                    return enumType.getNativeTypeId();
                }

                @Override
                public byte[] byteify(long compoundDataTypeId, Object obj)
                        throws IllegalAccessException
                {
                    return getEnum(obj).toStorageForm();
                }

                @Override
                public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                        int arrayOffset) throws IllegalAccessException
                {
                    final Object enumValue =
                            getEnumValue(enumType, byteArr, arrayOffset + offsetInMemory,
                                    enumReturnType, null);
                    setArray(obj, index, enumValue);
                }

                private HDF5EnumerationValue getEnum(Object obj) throws IllegalAccessException,
                        IllegalArgumentException
                {
                    assert obj != null;
                    final Object enumObj = getArray(obj, index);
                    if (enumObj instanceof HDF5EnumerationValue)
                    {
                        return (HDF5EnumerationValue) enumObj;
                    } else if (enumObj instanceof Number)
                    {
                        return new HDF5EnumerationValue(enumType, ((Number) enumObj).intValue());
                    } else
                    {
                        return new HDF5EnumerationValue(enumType, enumObj.toString());
                    }
                }

                @Override
                HDF5EnumerationType tryGetEnumType()
                {
                    return enumType;
                }
            };
    }

    @SuppressWarnings(
        { "rawtypes", "unchecked" })
    static Object getEnumValue(final HDF5EnumerationType enumType, byte[] byteArr, int arrayOffset,
            final HDF5CompoundMappingHints.EnumReturnType enumReturnType, Field fieldOrNull)
    {
        switch (enumReturnType)
        {
            case HDF5ENUMERATIONVALUE:
                return enumType.createFromStorageForm(byteArr, arrayOffset);
            case STRING:
                return enumType.createStringFromStorageForm(byteArr, arrayOffset);
            case ORDINAL:
                return enumType.getOrdinalFromStorageForm(byteArr, arrayOffset);
            case JAVAENUMERATION:
            {
                if (fieldOrNull == null)
                {
                    throw new HDF5JavaException(
                            "JAVAENUMERATIONTYPE only available with access type FIELD");
                }
                final String value = enumType.createStringFromStorageForm(byteArr, arrayOffset);
                final Class<Enum> enumClass = (Class<Enum>) fieldOrNull.getType();
                return Enum.valueOf(enumClass, value);
            }
        }
        throw new Error("Unknown EnumReturnType " + enumReturnType);
    }

}
