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

import static hdf.hdf5lib.HDF5Constants.H5T_ARRAY;
import static hdf.hdf5lib.HDF5Constants.H5T_COMPOUND;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * The implementation of {@link IHDF5CompoundInformationRetriever}.
 * 
 * @author Bernd Rinn
 */
abstract class HDF5CompoundInformationRetriever implements IHDF5CompoundInformationRetriever
{

    protected final HDF5BaseReader baseReader;

    protected final IHDF5EnumTypeRetriever enumTypeRetriever;

    HDF5CompoundInformationRetriever(HDF5BaseReader baseReader,
            IHDF5EnumTypeRetriever enumTypeRetriever)
    {
        assert baseReader != null;
        assert enumTypeRetriever != null;

        this.baseReader = baseReader;
        this.enumTypeRetriever = enumTypeRetriever;
    }

    @Override
    public <T> HDF5CompoundMemberInformation[] getMemberInfo(final Class<T> compoundClass)
    {
        return getMemberInfo(compoundClass.getSimpleName());
    }

    @Override
    public HDF5CompoundMemberInformation[] getMemberInfo(final String dataTypeName)
    {
        return getMemberInfo(dataTypeName, DataTypeInfoOptions.DEFAULT);
    }

    @Override
    public HDF5CompoundMemberInformation[] getMemberInfo(final String dataTypeName,
            final DataTypeInfoOptions dataTypeInfoOptions)
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5CompoundMemberInformation[]> writeRunnable =
                new ICallableWithCleanUp<HDF5CompoundMemberInformation[]>()
                    {
                        @Override
                        public HDF5CompoundMemberInformation[] call(final ICleanUpRegistry registry)
                        {
                            final String dataTypePath =
                                    HDF5Utils.createDataTypePath(HDF5Utils.COMPOUND_PREFIX,
                                            baseReader.houseKeepingNameSuffix, dataTypeName);
                            final long compoundDataTypeId =
                                    baseReader.h5.openDataType(baseReader.fileId, dataTypePath,
                                            registry);
                            final CompoundTypeInformation compoundInformation =
                                    getCompoundTypeInformation(compoundDataTypeId, dataTypePath,
                                            dataTypeInfoOptions, registry);
                            return compoundInformation.getCopyOfMembers();
                        }
                    };
        return baseReader.runner.call(writeRunnable);
    }

    @Override
    public HDF5CompoundMemberInformation[] getDataSetInfo(final String dataSetPath)
            throws HDF5JavaException
    {
        return getDataSetInfo(dataSetPath, DataTypeInfoOptions.DEFAULT);
    }

    @Override
    public HDF5CompoundMemberInformation[] getDataSetInfo(final String dataSetPath,
            final DataTypeInfoOptions dataTypeInfoOptions) throws HDF5JavaException
    {
        final ICallableWithCleanUp<HDF5CompoundMemberInformation[]> infoRunnable =
                new ICallableWithCleanUp<HDF5CompoundMemberInformation[]>()
                    {
                        @Override
                        public HDF5CompoundMemberInformation[] call(final ICleanUpRegistry registry)
                        {
                            return getFullCompoundDataSetInformation(dataSetPath,
                                    dataTypeInfoOptions, registry).getCopyOfMembers();
                        }
                    };
        final HDF5CompoundMemberInformation[] compoundInformation =
                baseReader.runner.call(infoRunnable);
        return compoundInformation;
    }

    private CompoundTypeInformation getFullCompoundAttributeInformation(final String objectPath,
            final String attributeName, final DataTypeInfoOptions dataTypeInfoOptions,
            final ICleanUpRegistry registry) throws HDF5JavaException
    {
        final long dataSetId = baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
        final long attributeId = baseReader.h5.openAttribute(dataSetId, attributeName, registry);
        final long storageDataTypeId = baseReader.h5.getDataTypeForAttribute(attributeId, registry);
        final long compoundDataTypeId;
        int classType = baseReader.h5.getClassType(storageDataTypeId);
        if (classType == H5T_ARRAY)
        {
            compoundDataTypeId = baseReader.h5.getBaseDataType(storageDataTypeId, registry);
            classType = baseReader.h5.getClassType(compoundDataTypeId);
        } else
        {
            compoundDataTypeId = storageDataTypeId;
        }
        if (classType != H5T_COMPOUND)
        {
            throw new HDF5JavaException("Attribute '" + attributeName + "' of object '"
                    + objectPath + "' is not of compound type.");
        }
        final String dataTypePathOrNull = baseReader.tryGetDataTypePath(compoundDataTypeId);
        final CompoundTypeInformation compoundInformation =
                getCompoundTypeInformation(compoundDataTypeId, dataTypePathOrNull,
                        dataTypeInfoOptions, registry);
        return compoundInformation;
    }

    private CompoundTypeInformation getFullCompoundDataSetInformation(final String dataSetPath,
            final DataTypeInfoOptions dataTypeInfoOptions, final ICleanUpRegistry registry)
            throws HDF5JavaException
    {
        final long dataSetId = baseReader.h5.openDataSet(baseReader.fileId, dataSetPath, registry);
        final long compoundDataTypeId = baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
        if (baseReader.h5.getClassType(compoundDataTypeId) != H5T_COMPOUND)
        {
            throw new HDF5JavaException("Data set '" + dataSetPath + "' is not of compound type.");
        }
        // Note: the type variants for the compound members are stored at the compound type.
        // So if we want to know the data set variant, we need to read the data type path as well.
        final String dataTypePathOrNull =
                (dataTypeInfoOptions.knowsDataTypePath() || dataTypeInfoOptions
                        .knowsDataTypeVariant()) ? baseReader
                        .tryGetDataTypePath(compoundDataTypeId) : null;
        final CompoundTypeInformation compoundInformation =
                getCompoundTypeInformation(compoundDataTypeId, dataTypePathOrNull,
                        dataTypeInfoOptions, registry);
        return compoundInformation;
    }

    private CompoundTypeInformation getFullCompoundDataTypeInformation(final String dataTypePath,
            final DataTypeInfoOptions dataTypeInfoOptions, final ICleanUpRegistry registry)
            throws HDF5JavaException
    {
        final long compoundDataTypeId =
                baseReader.h5.openDataType(baseReader.fileId, dataTypePath, registry);
        if (baseReader.h5.getClassType(compoundDataTypeId) != H5T_COMPOUND)
        {
            throw new HDF5JavaException("Data type '" + dataTypePath + "' is not a compound type.");
        }
        final CompoundTypeInformation compoundInformation =
                getCompoundTypeInformation(compoundDataTypeId, dataTypePath, dataTypeInfoOptions,
                        registry);
        return compoundInformation;
    }

    CompoundTypeInformation getCompoundTypeInformation(final long compoundDataTypeId,
            final String dataTypePathOrNull, final DataTypeInfoOptions dataTypeInfoOptions,
            final ICleanUpRegistry registry)
    {
        final String typeName =
                HDF5Utils.getDataTypeNameFromPath(dataTypePathOrNull,
                        baseReader.houseKeepingNameSuffix, HDF5DataClass.COMPOUND);
        final String[] memberNames =
                baseReader.h5.getNamesForEnumOrCompoundMembers(compoundDataTypeId);
        final long nativeCompoundDataTypeId =
                baseReader.h5.getNativeDataType(compoundDataTypeId, registry);
        final int recordSizeOnDisk = baseReader.h5.getDataTypeSize(compoundDataTypeId);
        final int recordSizeInMemory = baseReader.h5.getDataTypeSize(nativeCompoundDataTypeId);
        final CompoundTypeInformation compoundInfo =
                new CompoundTypeInformation(typeName, compoundDataTypeId, nativeCompoundDataTypeId,
                        memberNames.length, recordSizeOnDisk, recordSizeInMemory);
        final HDF5DataTypeVariant[] memberTypeVariantsOrNull =
                dataTypeInfoOptions.knowsDataTypeVariant() ? baseReader
                        .tryGetTypeVariantForCompoundMembers(dataTypePathOrNull, registry) : null;
        if (memberTypeVariantsOrNull != null
                && memberTypeVariantsOrNull.length != memberNames.length)
        {
            throw new HDF5JavaException(
                    "Invalid member data type variant information on committed data type '"
                            + dataTypePathOrNull + "'.");
        }
        int offsetOnDisk = 0;
        int offsetInMemory = 0;
        for (int i = 0; i < memberNames.length; ++i)
        {
            final long dataTypeId =
                    baseReader.h5.getDataTypeForIndex(compoundDataTypeId, i, registry);
            // This should safe us from computing the offsets ourselves, but as it turns out, the
            // offset for the native data type is wrong for bit fields,
            // Test failing: HDF5RoundtripTest.testCompoundMap()
            // Tested: 2014-07-28, HDF5 1.8.13
            // offsetOnDisk = baseReader.h5.getOffsetForCompoundMemberIndex(compoundDataTypeId, i);
            // offsetInMemory =
            // baseReader.h5.getOffsetForCompoundMemberIndex(nativeCompoundDataTypeId, i);
            compoundInfo.dataTypeIds[i] = dataTypeId;
            final HDF5DataTypeInformation dataTypeInformation =
                    baseReader.getDataTypeInformation(dataTypeId, dataTypeInfoOptions, registry);
            if (memberTypeVariantsOrNull != null && memberTypeVariantsOrNull[i].isTypeVariant())
            {
                dataTypeInformation.setTypeVariant(memberTypeVariantsOrNull[i]);
            }
            final HDF5EnumerationType enumTypeOrNull;
            if (dataTypeInformation.getDataClass() == HDF5DataClass.ENUM)
            {
                if (dataTypeInformation.isArrayType())
                {
                    final long baseDataSetType = baseReader.h5.getBaseDataType(dataTypeId, registry);
                    enumTypeOrNull =
                            baseReader.getEnumTypeForStorageDataType(null, baseDataSetType, false,
                                    null, null, registry);
                } else
                {
                    enumTypeOrNull =
                            baseReader.getEnumTypeForStorageDataType(null, dataTypeId, false, null,
                                    null, registry);
                }
            } else
            {
                enumTypeOrNull = null;
            }
            compoundInfo.enumTypes[i] = enumTypeOrNull;
            if (enumTypeOrNull != null)
            {
                compoundInfo.setMember(i, new HDF5CompoundMemberInformation(memberNames[i],
                        dataTypeInformation, offsetOnDisk, offsetInMemory, enumTypeOrNull
                                .getEnumType().getValueArray()));
            } else
            {
                compoundInfo.setMember(i, new HDF5CompoundMemberInformation(memberNames[i],
                        dataTypeInformation, offsetOnDisk, offsetInMemory));
            }
            final HDF5DataTypeInformation typeInfo = compoundInfo.getMember(i).getType();
            final int size = typeInfo.getSize();
            offsetOnDisk += size;
            offsetInMemory =
                    PaddingUtils.padOffset(offsetInMemory + size,
                            typeInfo.getElementSizeForPadding());

        }
        return compoundInfo;
    }

    @Override
    public <T> HDF5CompoundType<T> getType(final String name, final Class<T> pojoClass,
            boolean requireTypesToBeEqual, final HDF5CompoundMemberMapping... members)
    {
        baseReader.checkOpen();
        final HDF5ValueObjectByteifyer<T> objectArrayifyer =
                baseReader.createCompoundByteifyers(pojoClass, members, null);
        return getType(name, -1, pojoClass, requireTypesToBeEqual, objectArrayifyer);
    }

    @Override
    public <T> HDF5CompoundType<T> getType(final String name, final Class<T> pojoClass,
            final HDF5CompoundMemberMapping... members)
    {
        return getType(name, pojoClass, true, members);
    }

    <T> HDF5CompoundType<T> getType(final String name, long committedDataTypeId,
            final Class<T> compoundType, final boolean requireEqualsType,
            final HDF5ValueObjectByteifyer<T> objectArrayifyer)
    {
        final long storageDataTypeId =
                (committedDataTypeId < 0) ? baseReader
                        .createStorageCompoundDataType(objectArrayifyer) : committedDataTypeId;
        final long nativeDataTypeId = baseReader.createNativeCompoundDataType(objectArrayifyer);
        return new HDF5CompoundType<T>(baseReader.fileId, storageDataTypeId, nativeDataTypeId,
                name, compoundType, requireEqualsType, objectArrayifyer,
                new HDF5CompoundType.IHDF5InternalCompoundMemberInformationRetriever()
                    {
                        @Override
                        public HDF5CompoundMemberInformation[] getCompoundMemberInformation(
                                final DataTypeInfoOptions dataTypeInfoOptions)
                        {
                            return HDF5CompoundInformationRetriever.this
                                    .getCompoundMemberInformation(storageDataTypeId, name,
                                            dataTypeInfoOptions);
                        }
                    }, baseReader);
    }

    HDF5CompoundMemberInformation[] getCompoundMemberInformation(final long storageDataTypeId,
            final String dataTypeNameOrNull, final DataTypeInfoOptions dataTypeInfoOptions)
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5CompoundMemberInformation[]> writeRunnable =
                new ICallableWithCleanUp<HDF5CompoundMemberInformation[]>()
                    {
                        @Override
                        public HDF5CompoundMemberInformation[] call(final ICleanUpRegistry registry)
                        {
                            final String dataTypePath =
                                    (dataTypeNameOrNull == null) ? null : HDF5Utils
                                            .createDataTypePath(HDF5Utils.COMPOUND_PREFIX,
                                                    baseReader.houseKeepingNameSuffix,
                                                    dataTypeNameOrNull);
                            final CompoundTypeInformation compoundInformation =
                                    getCompoundTypeInformation(storageDataTypeId, dataTypePath,
                                            dataTypeInfoOptions, registry);
                            return compoundInformation.getCopyOfMembers();
                        }
                    };
        return baseReader.runner.call(writeRunnable);
    }

    @Override
    public <T> HDF5CompoundType<T> getType(final Class<T> pojoClass,
            final HDF5CompoundMemberMapping... members)
    {
        return getType(null, pojoClass, members);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(String name, Class<T> pojoClass,
            HDF5CompoundMappingHints hints)
    {
        return getInferredType(name, pojoClass, hints, true);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(String name, Class<T> pojoClass,
            HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual)
    {
        return getType(
                name,
                pojoClass,
                requireTypesToBeEqual,
                addEnumTypes(HDF5CompoundMemberMapping.addHints(
                        HDF5CompoundMemberMapping.inferMapping(pojoClass), hints)));
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(Class<T> pojoClass,
            HDF5CompoundMappingHints hints)
    {
        return getInferredType(null, pojoClass, hints);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(final String name, final Class<T> pojoClass)
    {
        return getInferredType(name, pojoClass, null);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(final Class<T> pojoClass)
    {
        return getInferredType(null, pojoClass);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(String name, T pojo,
            HDF5CompoundMappingHints hints)
    {
        return getInferredType(name, pojo, hints, true);
    }

    @Override
    @SuppressWarnings(
        { "unchecked", "rawtypes" })
    public <T> HDF5CompoundType<T> getInferredType(String name, T pojo,
            HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual)
    {
        if (Map.class.isInstance(pojo))
        {
            final String compoundTypeName =
                    (name == null) ? HDF5CompoundMemberMapping.constructCompoundTypeName(
                            ((Map) pojo).keySet(), true) : name;
            return (HDF5CompoundType<T>) getType(
                    compoundTypeName,
                    Map.class,
                    requireTypesToBeEqual,
                    addEnumTypes(HDF5CompoundMemberMapping.addHints(
                            HDF5CompoundMemberMapping.inferMapping((Map) pojo), hints)));
        } else
        {
            final Class<T> pojoClass = (Class<T>) pojo.getClass();
            return getType(name, pojoClass, requireTypesToBeEqual,
                    addEnumTypes(HDF5CompoundMemberMapping.addHints(HDF5CompoundMemberMapping
                            .inferMapping(pojo, HDF5CompoundMemberMapping.inferEnumerationTypeMap(
                                    pojo, enumTypeRetriever), HDF5CompoundMappingHints
                                    .isUseVariableLengthStrings(hints)), hints)));
        }
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(final String name, final T[] pojo)
    {
        return getInferredType(name, pojo, null);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(final T[] pojo)
    {
        return getInferredType((String) null, pojo, null);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(String name, T[] pojo,
            HDF5CompoundMappingHints hints)
    {
        return getInferredType(name, pojo, hints, true);
    }
    
    @Override
    @SuppressWarnings(
        { "unchecked", "rawtypes" })
    public <T> HDF5CompoundType<T> getInferredType(String name, T[] pojo,
            HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual)
    {
        final Class<?> componentType = pojo.getClass().getComponentType();
        if (pojo.length == 0)
        {
            return (HDF5CompoundType<T>) getInferredType(name, componentType, hints);
        }
        if (Map.class.isAssignableFrom(componentType))
        {
            final String compoundTypeName =
                    (name == null) ? HDF5CompoundMemberMapping.constructCompoundTypeName(
                            ((Map) pojo[0]).keySet(), true) : name;
            return (HDF5CompoundType<T>) getType(
                    compoundTypeName,
                    Map.class, 
                    requireTypesToBeEqual,
                    addEnumTypes(HDF5CompoundMemberMapping.addHints(
                            HDF5CompoundMemberMapping.inferMapping((Map) pojo[0]), hints)));
        } else
        {
            return (HDF5CompoundType<T>) getType(name, componentType, requireTypesToBeEqual,
                    addEnumTypes(HDF5CompoundMemberMapping.addHints(HDF5CompoundMemberMapping
                            .inferMapping(pojo, HDF5CompoundMemberMapping.inferEnumerationTypeMap(
                                    pojo, enumTypeRetriever),
                                    hints == null ? false : hints.isUseVariableLengthStrings()),
                            hints)));
        }
    }

    HDF5CompoundMemberMapping[] addEnumTypes(HDF5CompoundMemberMapping[] mapping)
    {
        for (HDF5CompoundMemberMapping m : mapping)
        {
            final Class<?> memberClass = m.tryGetMemberClass();
            if (memberClass != null)
            {
                if (memberClass.isEnum())
                {
                    @SuppressWarnings("unchecked")
                    final Class<? extends Enum<?>> enumClass =
                            (Class<? extends Enum<?>>) memberClass;
                    final String typeName =
                            (StringUtils.isBlank(m.tryGetEnumTypeName())) ? memberClass
                                    .getSimpleName() : m.tryGetEnumTypeName();
                    m.setEnumerationType(enumTypeRetriever.getType(typeName,
                            ReflectionUtils.getEnumOptions(enumClass)));
                } else if (memberClass == HDF5EnumerationValue.class
                        || memberClass == HDF5EnumerationValueArray.class
                        || memberClass == HDF5EnumerationValueMDArray.class)
                {
                    final HDF5CompoundMappingHints hintsOrNull = m.tryGetHints();
                    final HDF5EnumerationType typeOrNull =
                            (hintsOrNull != null) ? hintsOrNull.tryGetEnumType(m.getMemberName())
                                    : null;
                    if (typeOrNull != null)
                    {
                        m.setEnumerationType(typeOrNull);
                    }
                }
            }
        }
        return mapping;
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(final String name, final T pojo)
    {
        return getInferredType(name, pojo, null);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredType(final T pojo)
    {
        return getInferredType(null, pojo);
    }

    @Override
    public HDF5CompoundType<List<?>> getInferredType(String name, List<String> memberNames,
            List<?> data, HDF5CompoundMappingHints hints)
    {
        return getInferredType(name, memberNames, data, hints, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public HDF5CompoundType<List<?>> getInferredType(String name, List<String> memberNames,
            List<?> data, HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual)
    {
        final String compoundTypeName =
                (name == null) ? HDF5CompoundMemberMapping.constructCompoundTypeName(memberNames,
                        false) : name;
        final HDF5CompoundType<?> type =
                getType(compoundTypeName,
                        List.class,
                        requireTypesToBeEqual,
                        HDF5CompoundMemberMapping.addHints(
                                HDF5CompoundMemberMapping.inferMapping(memberNames, data), hints));
        return (HDF5CompoundType<List<?>>) type;
    }

    @Override
    public HDF5CompoundType<List<?>> getInferredType(String name, List<String> memberNames,
            List<?> data)
    {
        return getInferredType(name, memberNames, data, null);
    }

    @Override
    public HDF5CompoundType<List<?>> getInferredType(List<String> memberNames, List<?> data)
    {
        return getInferredType(null, memberNames, data);
    }

    @Override
    public HDF5CompoundType<List<?>> getInferredType(List<String> memberNames, List<?> data,
            HDF5CompoundMappingHints hints)
    {
        return getInferredType(null, memberNames, data, hints);
    }

    @Override
    public HDF5CompoundType<Object[]> getInferredType(String[] memberNames, Object[] data)
    {
        return getInferredType(null, memberNames, data);
    }

    @Override
    public HDF5CompoundType<Object[]> getInferredType(String name, String[] memberNames,
            Object[] data)
    {
        final String compoundTypeName =
                (name == null) ? HDF5CompoundMemberMapping.constructCompoundTypeName(
                        Arrays.asList(memberNames), false) : name;
        return getType(compoundTypeName, Object[].class,
                HDF5CompoundMemberMapping.inferMapping(memberNames, data));
    }

    @Override
    public HDF5CompoundType<Object[]> getInferredType(String name, String[] memberNames,
            Object[] data, HDF5CompoundMappingHints hints)
    {
        return getInferredType(name, memberNames, data, hints, true);
    }

    @Override
    public HDF5CompoundType<Object[]> getInferredType(String name, String[] memberNames,
            Object[] data, HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual)
    {
        final String compoundTypeName =
                (name == null) ? HDF5CompoundMemberMapping.constructCompoundTypeName(
                        Arrays.asList(memberNames), false) : name;
        return getType(compoundTypeName, Object[].class, requireTypesToBeEqual,
                HDF5CompoundMemberMapping.inferMapping(memberNames, data, hints));
    }

    @Override
    public HDF5CompoundType<Object[]> getInferredType(String[] memberNames, Object[] data,
            HDF5CompoundMappingHints hints)
    {
        return getInferredType(null, memberNames, data, hints);
    }

    @Override
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass,
            HDF5CompoundMemberMapping... members)
    {
        return getDataSetType(objectPath, pojoClass, true, members);
    }

    @Override
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass,
            boolean requireTypesToBeEqual, HDF5CompoundMemberMapping... members)
    {
        baseReader.checkOpen();
        final CompoundTypeInformation cpdTypeInfo =
                getFullCompoundDataSetInformation(objectPath, DataTypeInfoOptions.MINIMAL,
                        baseReader.fileRegistry);
        final HDF5CompoundType<T> typeForClass =
                getType(cpdTypeInfo.name, cpdTypeInfo.compoundDataTypeId, pojoClass,
                        requireTypesToBeEqual, createByteifyers(pojoClass, cpdTypeInfo, members));
        return typeForClass;
    }

    @Override
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass,
            HDF5CompoundMappingHints hints)
    {
        return getDataSetType(objectPath, pojoClass, hints, true);
    }

    @Override
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass,
            HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual)
    {
        baseReader.checkOpen();
        // We need to get ALL information for the type as otherwise the mapping might be wrong (due
        // to a missing data type variant).
        final CompoundTypeInformation cpdTypeInfo =
                getFullCompoundDataSetInformation(objectPath, DataTypeInfoOptions.ALL,
                        baseReader.fileRegistry);
        final HDF5CompoundType<T> typeForClass =
                getType(cpdTypeInfo.name, cpdTypeInfo.compoundDataTypeId, pojoClass,
                        requireTypesToBeEqual, createByteifyers(pojoClass, cpdTypeInfo, hints));
        return typeForClass;
    }

    @Override
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass)
    {
        return getDataSetType(objectPath, pojoClass, (HDF5CompoundMappingHints) null);
    }

    @Override
    public <T> HDF5CompoundType<T> getAttributeType(String objectPath, String attributeName,
            Class<T> pojoClass)
    {
        return getAttributeType(objectPath, attributeName, pojoClass, null);
    }

    @Override
    public <T> HDF5CompoundType<T> getAttributeType(String objectPath, String attributeName,
            Class<T> pojoClass, HDF5CompoundMappingHints hints)
    {
        return getAttributeType(objectPath, attributeName, pojoClass, hints,
                DataTypeInfoOptions.DEFAULT);
    }

    @Override
    public <T> HDF5CompoundType<T> getAttributeType(String objectPath, String attributeName,
            Class<T> pojoClass, HDF5CompoundMappingHints hints,
            DataTypeInfoOptions dataTypeInfoOptions)
    {
        return getAttributeType(objectPath, attributeName, pojoClass, hints, dataTypeInfoOptions,
                true);
    }

    @Override
    public <T> HDF5CompoundType<T> getAttributeType(String objectPath, String attributeName,
            Class<T> pojoClass, HDF5CompoundMappingHints hints,
            DataTypeInfoOptions dataTypeInfoOptions, boolean requireTypesToBeEqual)
    {
        final CompoundTypeInformation cpdTypeInfo =
                getFullCompoundAttributeInformation(objectPath, attributeName, dataTypeInfoOptions,
                        baseReader.fileRegistry);
        final HDF5CompoundType<T> typeForClass =
                getType(cpdTypeInfo.name, cpdTypeInfo.compoundDataTypeId, pojoClass,
                        requireTypesToBeEqual, createByteifyers(pojoClass, cpdTypeInfo, hints));
        return typeForClass;
    }

    @Override
    public <T> HDF5CompoundType<T> getNamedType(Class<T> pojoClass)
    {
        return getNamedType(pojoClass.getSimpleName(), pojoClass);
    }

    @Override
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass)
    {
        return getNamedType(dataTypeName, pojoClass, null, DataTypeInfoOptions.DEFAULT);
    }

    @Override
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass,
            HDF5CompoundMappingHints hints)
    {
        return getNamedType(dataTypeName, pojoClass, hints, DataTypeInfoOptions.DEFAULT);
    }

    @Override
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass,
            DataTypeInfoOptions dataTypeInfoOptions)
    {
        return getNamedType(dataTypeName, pojoClass, null, dataTypeInfoOptions);
    }

    @Override
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass,
            HDF5CompoundMappingHints hints, DataTypeInfoOptions dataTypeInfoOptions)
    {
        return getNamedType(dataTypeName, pojoClass, hints, dataTypeInfoOptions, true);
    }

    @Override
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass,
            HDF5CompoundMappingHints hints, DataTypeInfoOptions dataTypeInfoOptions,
            boolean requireTypesToBeEqual)
    {
        final String dataTypePath =
                HDF5Utils.createDataTypePath(HDF5Utils.COMPOUND_PREFIX,
                        baseReader.houseKeepingNameSuffix, dataTypeName);
        final CompoundTypeInformation cpdTypeInfo =
                getFullCompoundDataTypeInformation(dataTypePath, dataTypeInfoOptions,
                        baseReader.fileRegistry);
        final HDF5CompoundType<T> typeForClass =
                getType(dataTypeName, cpdTypeInfo.compoundDataTypeId, pojoClass,
                        requireTypesToBeEqual, createByteifyers(pojoClass, cpdTypeInfo, hints));
        return typeForClass;
    }

    private <T> HDF5ValueObjectByteifyer<T> createByteifyers(final Class<T> compoundClazz,
            final CompoundTypeInformation compoundTypeInfo,
            final HDF5CompoundMemberMapping[] mapping)
    {
        return baseReader.createCompoundByteifyers(compoundClazz, mapping, compoundTypeInfo);
    }

    private <T> HDF5ValueObjectByteifyer<T> createByteifyers(final Class<T> compoundClazz,
            final CompoundTypeInformation compoundTypeInfo,
            final HDF5CompoundMappingHints hintsOrNull)
    {
        return baseReader.createCompoundByteifyers(compoundClazz,
                inferMemberMapping(compoundClazz, compoundTypeInfo, hintsOrNull), compoundTypeInfo);
    }

    private HDF5CompoundMemberMapping[] inferMemberMapping(final Class<?> compoundClazz,
            final CompoundTypeInformation compoundTypeInfo,
            final HDF5CompoundMappingHints hintsOrNull)
    {
        final List<HDF5CompoundMemberMapping> mapping =
                new ArrayList<HDF5CompoundMemberMapping>(compoundTypeInfo.getNumberOfMembers());
        final Map<String, Field> fields = ReflectionUtils.getFieldMap(compoundClazz);
        for (int i = 0; i < compoundTypeInfo.getNumberOfMembers(); ++i)
        {
            final HDF5CompoundMemberInformation compoundMember = compoundTypeInfo.getMember(i);
            final long compoundMemberTypeId = compoundTypeInfo.dataTypeIds[i];
            final Field fieldOrNull = fields.get(compoundMember.getName());
            final String memberName = compoundMember.getName();
            final String fieldName = (fieldOrNull != null) ? fieldOrNull.getName() : memberName;
            final HDF5DataTypeInformation typeInfo = compoundMember.getType();
            final int[] dimensions = typeInfo.getDimensions();
            if (typeInfo.getDataClass() == HDF5DataClass.ENUM)
            {
                if (dimensions.length == 0 || (dimensions.length == 1 && dimensions[0] == 1))
                {
                    mapping.add(HDF5CompoundMemberMapping.mapping(memberName).fieldName(fieldName)
                            .enumType(compoundTypeInfo.enumTypes[i])
                            .typeVariant(typeInfo.tryGetTypeVariant()));
                } else if (dimensions.length == 1)
                {
                    mapping.add(HDF5CompoundMemberMapping.mappingWithStorageTypeId(
                            fieldName,
                            memberName,
                            new HDF5EnumerationType(baseReader.fileId, -1, baseReader.h5
                                    .getNativeDataType(compoundMemberTypeId,
                                            baseReader.fileRegistry), baseReader
                                    .getEnumDataTypeName(compoundMember.getType().tryGetName(),
                                            compoundMemberTypeId), compoundMember
                                    .tryGetEnumValues(), baseReader), dimensions,
                            compoundMemberTypeId, typeInfo.tryGetTypeVariant()));
                }
            } else if (typeInfo.getDataClass() == HDF5DataClass.STRING)
            {
                if (fieldOrNull != null && (fieldOrNull.getType() != String.class)
                        && (fieldOrNull.getType() != char[].class))
                {
                    throw new HDF5JavaException(
                            "Field of string type does not correspond to string or char[] value");
                }
                mapping.add(HDF5CompoundMemberMapping.mappingArrayWithStorageId(fieldName,
                        memberName, String.class, new int[]
                            { typeInfo.getElementSize() }, compoundMemberTypeId, false,
                        typeInfo.isVariableLengthString(), false, typeInfo.tryGetTypeVariant()));
            } else if (typeInfo.getDataClass() == HDF5DataClass.REFERENCE)
            {
                if (fieldOrNull != null && (fieldOrNull.getType() != String.class)
                        && (fieldOrNull.getType() != char[].class))
                {
                    throw new HDF5JavaException(
                            "Field of rererence type does not correspond to string or char[] value");
                }
                mapping.add(HDF5CompoundMemberMapping.mappingArrayWithStorageId(fieldName,
                        memberName, String.class, new int[]
                            { typeInfo.getElementSize() }, compoundMemberTypeId, false,
                        false, true, typeInfo.tryGetTypeVariant()));
            } else
            {
                final Class<?> memberClazz;
                if (fieldOrNull != null)
                {
                    memberClazz = fieldOrNull.getType();
                } else
                {
                    memberClazz = typeInfo.tryGetJavaType();
                }
                mapping.add(HDF5CompoundMemberMapping.mappingArrayWithStorageId(fieldName,
                        memberName, memberClazz, dimensions, compoundMemberTypeId,
                        false == compoundMember.getType().isSigned(), false, false, 
                        typeInfo.tryGetTypeVariant()));
            }
        }
        return HDF5CompoundMemberMapping.addHints(
                mapping.toArray(new HDF5CompoundMemberMapping[mapping.size()]), hintsOrNull);
    }

}
