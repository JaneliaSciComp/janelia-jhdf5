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

import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;

import java.util.List;
import java.util.Map;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * The implementation of {@link IHDF5CompoundWriter}.
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundWriter extends HDF5CompoundReader implements IHDF5CompoundWriter
{
    private final HDF5BaseWriter baseWriter;

    HDF5CompoundWriter(HDF5BaseWriter baseWriter, IHDF5EnumWriter enumWriter)
    {
        super(baseWriter, enumWriter);
        this.baseWriter = baseWriter;
    }

    private <T> HDF5CompoundType<T> getType(final String nameOrNull, final boolean anonymousType,
            Class<T> pojoClass, final boolean requireEqualsType,
            HDF5CompoundMemberMapping... members)
    {
        baseWriter.checkOpen();
        final HDF5ValueObjectByteifyer<T> objectByteifyer =
                baseWriter.createCompoundByteifyers(pojoClass, members, null);
        final String dataTypeName =
                anonymousType ? null : (nameOrNull != null) ? nameOrNull
                        : deriveCompoundNameFromClass(pojoClass);
        final long storageDataTypeId =
                getOrCreateCompoundDataType(dataTypeName, objectByteifyer,
                        baseWriter.keepDataSetIfExists);
        final long nativeDataTypeId = baseWriter.createNativeCompoundDataType(objectByteifyer);
        return new HDF5CompoundType<T>(baseWriter.fileId, storageDataTypeId, nativeDataTypeId,
                dataTypeName, pojoClass, requireEqualsType, objectByteifyer,
                new HDF5CompoundType.IHDF5InternalCompoundMemberInformationRetriever()
                    {
                        @Override
                        public HDF5CompoundMemberInformation[] getCompoundMemberInformation(
                                final DataTypeInfoOptions dataTypeOptions)
                        {
                            return HDF5CompoundWriter.this.getCompoundMemberInformation(
                                    storageDataTypeId, nameOrNull, dataTypeOptions);
                        }
                    }, baseReader);
    }

    @Override
    public <T> HDF5CompoundType<T> getType(final String name, final Class<T> pojoClass,
            boolean requireTypesToBeEqual, final HDF5CompoundMemberMapping... members)
    {
        return getType(name, false, pojoClass, requireTypesToBeEqual, members);
    }

    @Override
    public <T> HDF5CompoundType<T> getType(final String name, Class<T> pojoClass,
            HDF5CompoundMemberMapping... members)
    {
        return getType(name, false, pojoClass, true, members);
    }

    @Override
    public <T> HDF5CompoundType<T> getAnonType(Class<T> pojoClass,
            HDF5CompoundMemberMapping... members)
    {
        return getType(null, true, pojoClass, true, members);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredAnonType(Class<T> pojoClass,
            HDF5CompoundMappingHints hints)
    {
        return getType(
                null,
                true,
                pojoClass,
                true,
                addEnumTypes(HDF5CompoundMemberMapping.addHints(
                        HDF5CompoundMemberMapping.inferMapping(pojoClass), hints)));
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredAnonType(Class<T> pojoClass)
    {
        return getInferredAnonType(pojoClass, null);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredAnonType(T template)
    {
        return getInferredAnonType(template, null);
    }

    @Override
    @SuppressWarnings(
        { "unchecked", "rawtypes" })
    public <T> HDF5CompoundType<T> getInferredAnonType(T pojo, HDF5CompoundMappingHints hints)
    {
        if (Map.class.isInstance(pojo))
        {
            return (HDF5CompoundType<T>) getType(
                    null,
                    true,
                    Map.class,
                    true,
                    addEnumTypes(HDF5CompoundMemberMapping.addHints(
                            HDF5CompoundMemberMapping.inferMapping((Map) pojo), hints)));
        } else
        {
            final Class<T> pojoClass = (Class<T>) pojo.getClass();
            return getType(null, true, pojoClass, true,
                    addEnumTypes(HDF5CompoundMemberMapping.addHints(HDF5CompoundMemberMapping
                            .inferMapping(pojo, HDF5CompoundMemberMapping.inferEnumerationTypeMap(
                                    pojo, enumTypeRetriever), HDF5CompoundMappingHints
                                    .isUseVariableLengthStrings(hints)), hints)));
        }
    }

    private <T> HDF5CompoundType<T> getType(final String name, final boolean anonymousType,
            final HDF5CompoundType<T> templateType)
    {
        baseWriter.checkOpen();
        templateType.checkOpen();
        final HDF5ValueObjectByteifyer<T> objectByteifyer = templateType.getObjectByteifyer();
        final String dataTypeName =
                anonymousType ? null : (name == null) ? templateType.getName() : name;
        final long storageDataTypeId =
                getOrCreateCompoundDataType(dataTypeName, objectByteifyer,
                        baseWriter.keepDataSetIfExists);
        return getType(dataTypeName, storageDataTypeId, templateType.getCompoundType(), true,
                objectByteifyer);
    }

    @Override
    public <T> HDF5CompoundType<T> getInferredAnonType(T[] template)
    {
        return getInferredAnonType(template, null);
    }

    @Override
    @SuppressWarnings(
        { "unchecked", "rawtypes" })
    public <T> HDF5CompoundType<T> getInferredAnonType(T[] template, HDF5CompoundMappingHints hints)
    {
        final Class<?> componentType = template.getClass().getComponentType();
        if (template.length == 0)
        {
            return (HDF5CompoundType<T>) getInferredAnonType(componentType, hints);
        }
        if (Map.class.isAssignableFrom(componentType))
        {
            return (HDF5CompoundType<T>) getType(
                    null,
                    true,
                    Map.class,
                    true,
                    addEnumTypes(HDF5CompoundMemberMapping.addHints(
                            HDF5CompoundMemberMapping.inferMapping((Map) template[0]), hints)));
        } else
        {
            return (HDF5CompoundType<T>) getType(null, true, componentType, true,
                    addEnumTypes(HDF5CompoundMemberMapping.addHints(HDF5CompoundMemberMapping
                            .inferMapping(template, HDF5CompoundMemberMapping
                                    .inferEnumerationTypeMap(template, enumTypeRetriever),
                                    hints == null ? false : hints.isUseVariableLengthStrings()),
                            hints)));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public HDF5CompoundType<List<?>> getInferredAnonType(List<String> memberNames,
            List<?> template, HDF5CompoundMappingHints hints)
    {
        final HDF5CompoundType<?> type =
                getType(null, true, List.class, true, HDF5CompoundMemberMapping.addHints(
                        HDF5CompoundMemberMapping.inferMapping(memberNames, template), hints));
        return (HDF5CompoundType<List<?>>) type;
    }

    @Override
    public HDF5CompoundType<List<?>> getInferredAnonType(List<String> memberNames, List<?> template)
    {
        return getInferredAnonType(memberNames, template, null);
    }

    @Override
    public HDF5CompoundType<Object[]> getInferredAnonType(String[] memberNames, Object[] template)
    {
        return getInferredAnonType(memberNames, template, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public HDF5CompoundType<Object[]> getInferredAnonType(String[] memberNames, Object[] template,
            HDF5CompoundMappingHints hints)
    {
        final HDF5CompoundType<?> type =
                getType(null, true, List.class, true, HDF5CompoundMemberMapping.addHints(
                        HDF5CompoundMemberMapping.inferMapping(memberNames, template), hints));
        return (HDF5CompoundType<Object[]>) type;
    }

    @Override
    public <T> HDF5CompoundType<T> getClonedType(final HDF5CompoundType<T> templateType)
    {
        return getType(null, false, templateType);
    }

    private <T> String deriveCompoundNameFromClass(Class<T> pojoClass)
    {
        final CompoundType ct = pojoClass.getAnnotation(CompoundType.class);
        final String name = (ct != null) ? ct.name() : "";
        return name.length() == 0 ? pojoClass.getSimpleName() : name;
    }

    private <T> long getOrCreateCompoundDataType(final String dataTypeName,
            final HDF5ValueObjectByteifyer<T> objectByteifyer,
            boolean committedDataTypeHasPreference)
    {
        final boolean dataTypeNameGiven =
                (dataTypeName != null && "UNKNOWN".equals(dataTypeName) == false);
        final String dataTypePath =
                dataTypeNameGiven ? HDF5Utils.createDataTypePath(HDF5Utils.COMPOUND_PREFIX,
                        baseWriter.houseKeepingNameSuffix, dataTypeName) : null;
        final long committedStorageDataTypeId =
                dataTypeNameGiven ? baseWriter.getDataTypeId(dataTypePath) : -1;
        final boolean typeExists = (committedStorageDataTypeId >= 0);
        long storageDataTypeId = committedStorageDataTypeId;
        final boolean commitType;
        if (((typeExists == false) || (committedDataTypeHasPreference == false)))
        {
            storageDataTypeId = baseWriter.createStorageCompoundDataType(objectByteifyer);
            final boolean typesAreEqual =
                    typeExists
                            && baseWriter.h5.dataTypesAreEqual(committedStorageDataTypeId,
                                    storageDataTypeId);
            commitType = dataTypeNameGiven && ((typeExists == false) || (typesAreEqual == false));
            if (typeExists && commitType)
            {
                final String replacementDataTypePath = baseWriter.moveLinkOutOfTheWay(dataTypePath);
                baseReader.renameNamedDataType(dataTypePath, replacementDataTypePath);
            }
            if (typesAreEqual)
            {
                storageDataTypeId = committedStorageDataTypeId;
            }
        } else
        {
            commitType = false;
        }
        if (commitType)
        {
            baseWriter.commitDataType(dataTypePath, storageDataTypeId);
            final HDF5EnumerationValueArray typeVariants =
                    tryCreateDataTypeVariantArray(objectByteifyer);
            if (typeVariants != null)
            {
                baseWriter.setEnumArrayAttribute(dataTypePath, HDF5Utils
                        .getTypeVariantMembersAttributeName(baseWriter.houseKeepingNameSuffix),
                        typeVariants);
            }
        }
        return storageDataTypeId;
    }

    private <T> HDF5EnumerationValueArray tryCreateDataTypeVariantArray(
            final HDF5ValueObjectByteifyer<T> objectByteifyer)
    {
        final byte[] typeVariantOrdinals = new byte[objectByteifyer.getByteifyers().length];
        boolean hasTypeVariants = false;
        for (int i = 0; i < typeVariantOrdinals.length; ++i)
        {
            typeVariantOrdinals[i] =
                    (byte) objectByteifyer.getByteifyers()[i].getTypeVariant().ordinal();
            hasTypeVariants |= HDF5DataTypeVariant.isTypeVariant(typeVariantOrdinals[i]);
        }
        return hasTypeVariants ? new HDF5EnumerationValueArray(baseWriter.typeVariantDataType,
                typeVariantOrdinals) : null;
    }

    @Override
    public <T> void setAttr(final String objectPath, final String attributeName,
            final HDF5CompoundType<T> type, final T data)
    {
        primSetCompoundAttribute(objectPath, attributeName, type, data, null);
    }

    @Override
    public <T> void setAttr(final String objectPath, final String attributeName, final T data)
    {
        final HDF5CompoundType<T> inferredCompoundType = getInferredType(data);
        inferredCompoundType.checkMappingComplete();
        primSetCompoundAttribute(objectPath, attributeName, inferredCompoundType, data, null);
    }

    @Override
    public <T> void setArrayAttr(String objectPath, String attributeName, HDF5CompoundType<T> type,
            T[] value)
    {
        baseWriter.setCompoundArrayAttribute(objectPath, attributeName, type, value, null);
    }

    @Override
    public <T> void setArrayAttr(String objectPath, String attributeName, T[] value)
    {
        @SuppressWarnings("unchecked")
        final HDF5CompoundType<T> inferredCompoundType =
                getInferredType((Class<T>) value.getClass().getComponentType());
        inferredCompoundType.checkMappingComplete();
        baseWriter.setCompoundArrayAttribute(objectPath, attributeName, inferredCompoundType,
                value, null);
    }

    @Override
    public <T> void setMDArrayAttr(String objectPath, String attributeName,
            HDF5CompoundType<T> type, MDArray<T> value)
    {
        baseWriter.setCompoundMDArrayAttribute(objectPath, attributeName, type, value, null);
    }

    @Override
    public <T> void setMDArrayAttr(String objectPath, String attributeName, MDArray<T> value)
    {
        @SuppressWarnings("unchecked")
        final HDF5CompoundType<T> inferredCompoundType =
                getInferredType((Class<T>) value.getAsFlatArray().getClass().getComponentType());
        inferredCompoundType.checkMappingComplete();
        baseWriter.setCompoundMDArrayAttribute(objectPath, attributeName, inferredCompoundType,
                value, null);
    }

    private <T> void primSetCompoundAttribute(final String objectPath, final String attributeName,
            final HDF5CompoundType<?> type, final T data, final IByteArrayInspector inspectorOrNull)
    {
        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        @SuppressWarnings("unchecked")
        final byte[] byteArray =
                ((HDF5CompoundType<T>) type).getObjectByteifyer().byteify(type.getStorageTypeId(),
                        data);
        if (inspectorOrNull != null)
        {
            inspectorOrNull.inspect(byteArray);
        }
        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            if (baseWriter.useSimpleDataSpaceForAttributes)
                            {
                                final long dataSpaceId =
                                        baseWriter.h5.createSimpleDataSpace(new long[]
                                            { 1 }, registry);
                                baseWriter.setAttribute(objectPath, attributeName,
                                        type.getStorageTypeId(), type.getNativeTypeId(),
                                        dataSpaceId, byteArray, registry);
                            } else
                            {
                                baseWriter.setAttribute(objectPath, attributeName,
                                        type.getStorageTypeId(), type.getNativeTypeId(), -1,
                                        byteArray, registry);
                            }
                            baseWriter.h5.reclaimCompoundVL(type, byteArray);

                            return null; // Nothing to return.
                        }
                    };
        baseWriter.runner.call(addAttributeRunnable);
    }

    @Override
    public <T> void write(final String objectPath, final HDF5CompoundType<T> type, final T data)
    {
        primWriteCompound(objectPath, type, data, null);
    }

    @Override
    public <T> void write(final String objectPath, final HDF5CompoundType<T> type, final T data,
            final IByteArrayInspector inspectorOrNull)
    {
        primWriteCompound(objectPath, type, data, inspectorOrNull);
    }

    private <T> void primWriteCompound(final String objectPath, final HDF5CompoundType<?> type,
            final T data, final IByteArrayInspector inspectorOrNull)
    {
        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        @SuppressWarnings("unchecked")
        final byte[] byteArray =
                ((HDF5CompoundType<T>) type).getObjectByteifyer().byteify(type.getStorageTypeId(),
                        data);
        if (inspectorOrNull != null)
        {
            inspectorOrNull.inspect(byteArray);
        }
        baseWriter.writeScalar(objectPath, type.getStorageTypeId(), type.getNativeTypeId(),
                byteArray);
        baseWriter.h5.reclaimCompoundVL(type, byteArray);
    }

    @Override
    public <T> void write(String objectPath, T data)
    {
        final HDF5CompoundType<T> inferredCompoundType = getInferredType(data);
        inferredCompoundType.checkMappingComplete();
        primWriteCompound(objectPath, inferredCompoundType, data, null);
    }

    @Override
    public <T> void writeArray(final String objectPath, final HDF5CompoundType<T> type,
            final T[] data)
    {
        primWriteCompoundArray(objectPath, type, data,
                HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION, null);
    }

    @Override
    public <T> void writeArray(final String objectPath, final HDF5CompoundType<T> type,
            final T[] data, final HDF5GenericStorageFeatures features)
    {
        primWriteCompoundArray(objectPath, type, data, features, null);
    }

    @Override
    public <T> void writeArray(final String objectPath, final HDF5CompoundType<T> type,
            final T[] data, final HDF5GenericStorageFeatures features,
            final IByteArrayInspector inspectorOrNull)
    {
        primWriteCompoundArray(objectPath, type, data, features, inspectorOrNull);
    }

    private <T> void primWriteCompoundArray(final String objectPath,
            final HDF5CompoundType<?> type, final T[] data,
            final HDF5GenericStorageFeatures features, final IByteArrayInspector inspectorOrNull)
    {
        assert objectPath != null;
        assert type != null;
        assert data != null;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath, type.getStorageTypeId(),
                                    new long[]
                                        { data.length }, type.getObjectByteifyer()
                                            .getRecordSizeOnDisk(), features, registry);
                    @SuppressWarnings("unchecked")
                    final byte[] byteArray =
                            ((HDF5CompoundType<T>) type).getObjectByteifyer().byteify(
                                    type.getStorageTypeId(), data);
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArray);
                    }
                    H5Dwrite(dataSetId, type.getNativeTypeId(), H5S_ALL, H5S_ALL, H5P_DEFAULT,
                            byteArray);
                    baseWriter.h5.reclaimCompoundVL(type, byteArray);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void writeArray(String objectPath, T[] data)
    {
        writeArray(objectPath, data, HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public <T> void writeArray(String objectPath, T[] data, HDF5GenericStorageFeatures features)
    {
        assert data != null && data.length > 0;

        final HDF5CompoundType<T> inferredCompoundType = getInferredType(data);
        inferredCompoundType.checkMappingComplete();
        primWriteCompoundArray(objectPath, inferredCompoundType, data, features, null);
    }

    @Override
    public <T> void writeArrayBlock(final String objectPath, final HDF5CompoundType<T> type,
            final T[] data, final long blockNumber)
    {
        writeArrayBlock(objectPath, type, data, blockNumber, null);
    }

    @Override
    public <T> void writeArrayBlock(final String objectPath, final HDF5CompoundType<T> type,
            final T[] data, final long blockNumber, final IByteArrayInspector inspectorOrNull)
    {
        assert objectPath != null;
        assert type != null;
        assert data != null;
        assert blockNumber >= 0;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    final long size = data.length;
                    final long[] dimensions = new long[]
                        { size };
                    final long[] offset = new long[]
                        { size * blockNumber };
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, new long[]
                                        { data.length * (blockNumber + 1) }, false, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, offset, dimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(dimensions, registry);
                    final byte[] byteArray =
                            type.getObjectByteifyer().byteify(type.getStorageTypeId(), data);
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArray);
                    }
                    H5Dwrite(dataSetId, type.getNativeTypeId(), memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, byteArray);
                    baseWriter.h5.reclaimCompoundVL(type, byteArray);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void writeArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final T[] data, final long offset)
    {
        writeArrayBlockWithOffset(objectPath, type, data, offset, null);
    }

    @Override
    public <T> void writeArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final T[] data, final long offset,
            final IByteArrayInspector inspectorOrNull)
    {
        assert objectPath != null;
        assert type != null;
        assert data != null;
        assert offset >= 0;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final long size = data.length;
        final long[] dimensions = new long[]
            { size };
        final long[] offsetArray = new long[]
            { offset };
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, new long[]
                                        { offset + data.length }, false, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, offsetArray, dimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(dimensions, registry);
                    final byte[] byteArray =
                            type.getObjectByteifyer().byteify(type.getStorageTypeId(), data);
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArray);
                    }
                    H5Dwrite(dataSetId, type.getNativeTypeId(), memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, byteArray);
                    baseWriter.h5.reclaimCompoundVL(type, byteArray);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void createArray(String objectPath, HDF5CompoundType<T> type, int size)
    {
        createArray(objectPath, type, size, HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public <T> void createArray(final String objectPath, final HDF5CompoundType<T> type,
            final long size, final int blockSize)
    {
        createArray(objectPath, type, size, blockSize,
                HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public <T> void createArray(final String objectPath, final HDF5CompoundType<T> type,
            final long size, final int blockSize, final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert type != null;
        assert size >= 0;
        assert blockSize >= 0 && (blockSize <= size || size == 0);

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    baseWriter.createDataSet(objectPath, type.getStorageTypeId(), features,
                            new long[]
                                { size }, new long[]
                                { blockSize }, type.getObjectByteifyer().getRecordSizeOnDisk(),
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void createArray(final String objectPath, final HDF5CompoundType<T> type,
            final long size, final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert type != null;
        assert size >= 0;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    if (features.requiresChunking())
                    {
                        baseWriter.createDataSet(objectPath, type.getStorageTypeId(), features,
                                new long[]
                                    { 0 }, new long[]
                                    { size }, type.getObjectByteifyer().getRecordSizeOnDisk(),
                                registry);
                    } else
                    {
                        baseWriter.createDataSet(objectPath, type.getStorageTypeId(), features,
                                new long[]
                                    { size }, null,
                                type.getObjectByteifyer().getRecordSizeOnDisk(), registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void writeMDArray(final String objectPath, final HDF5CompoundType<T> type,
            final MDArray<T> data)
    {
        writeMDArray(objectPath, type, data, HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public <T> void writeMDArray(final String objectPath, final HDF5CompoundType<T> type,
            final MDArray<T> data, final HDF5GenericStorageFeatures features)
    {
        writeMDArray(objectPath, type, data, features, null);
    }

    @Override
    public <T> void writeMDArray(final String objectPath, final HDF5CompoundType<T> type,
            final MDArray<T> data, final HDF5GenericStorageFeatures features,
            final IByteArrayInspector inspectorOrNull)
    {
        assert objectPath != null;
        assert type != null;
        assert data != null;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        primWriteCompoundMDArray(objectPath, type, data, features, inspectorOrNull);
    }

    private <T> void primWriteCompoundMDArray(final String objectPath,
            final HDF5CompoundType<T> type, final MDArray<T> data,
            final HDF5GenericStorageFeatures features, final IByteArrayInspector inspectorOrNull)
    {
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath, type.getStorageTypeId(),
                                    MDAbstractArray.toLong(data.dimensions()), type
                                            .getObjectByteifyer().getRecordSizeOnDisk(), features,
                                    registry);
                    final byte[] byteArray =
                            type.getObjectByteifyer().byteify(type.getStorageTypeId(),
                                    data.getAsFlatArray());
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArray);
                    }
                    H5Dwrite(dataSetId, type.getNativeTypeId(), H5S_ALL, H5S_ALL, H5P_DEFAULT,
                            byteArray);
                    baseWriter.h5.reclaimCompoundVL(type, byteArray);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void writeMDArrayBlock(final String objectPath, final HDF5CompoundType<T> type,
            final MDArray<T> data, final long[] blockNumber)
    {
        writeMDArrayBlock(objectPath, type, data, blockNumber, null);
    }

    @Override
    public <T> void writeMDArrayBlock(final String objectPath, final HDF5CompoundType<T> type,
            final MDArray<T> data, final long[] blockNumber,
            final IByteArrayInspector inspectorOrNull)
    {
        final long[] dimensions = data.longDimensions();
        final long[] offset = new long[dimensions.length];
        final long[] dataSetDimensions = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * dimensions[i];
            dataSetDimensions[i] = offset[i] + dimensions[i];
        }
        writeCompoundMDArrayBlockWithOffset(objectPath, type, data.getAsFlatArray(), dimensions,
                offset, dataSetDimensions, inspectorOrNull);
    }

    @Override
    public <T> void writeMDArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final MDArray<T> data, final long[] offset)
    {
        writeMDArrayBlockWithOffset(objectPath, type, data, offset, null);
    }

    @Override
    public <T> void writeMDArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final MDArray<T> data, final long[] offset,
            final IByteArrayInspector inspectorOrNull)
    {
        final long[] dimensions = data.longDimensions();
        final long[] dataSetDimensions = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            dataSetDimensions[i] = offset[i] + dimensions[i];
        }
        writeCompoundMDArrayBlockWithOffset(objectPath, type, data.getAsFlatArray(), dimensions,
                offset, dataSetDimensions, inspectorOrNull);
    }

    private <T> void writeCompoundMDArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final T[] data, final long[] dimensions,
            final long[] offset, final long[] dataSetDimensions,
            final IByteArrayInspector inspectorOrNull)
    {
        assert objectPath != null;
        assert type != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, dataSetDimensions, false, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, offset, dimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(dimensions, registry);
                    final byte[] byteArray =
                            type.getObjectByteifyer().byteify(type.getStorageTypeId(), data);
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArray);
                    }
                    H5Dwrite(dataSetId, type.getNativeTypeId(), memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, byteArray);
                    baseWriter.h5.reclaimCompoundVL(type, byteArray);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void writeMDArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final MDArray<T> data, final int[] blockDimensions,
            final long[] offset, final int[] memoryOffset)
    {
        writeMDArrayBlockWithOffset(objectPath, type, data, blockDimensions, offset, memoryOffset,
                null);
    }

    @Override
    public <T> void writeMDArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final MDArray<T> data, final int[] blockDimensions,
            final long[] offset, final int[] memoryOffset, final IByteArrayInspector inspectorOrNull)
    {
        assert objectPath != null;
        assert type != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    final long[] memoryDimensions = data.longDimensions();
                    final long[] longBlockDimensions = MDAbstractArray.toLong(blockDimensions);
                    final long[] dataSetDimensions = new long[blockDimensions.length];
                    for (int i = 0; i < offset.length; ++i)
                    {
                        dataSetDimensions[i] = offset[i] + blockDimensions[i];
                    }
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, dataSetDimensions, false, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, offset, longBlockDimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(memoryDimensions, registry);
                    baseWriter.h5.setHyperslabBlock(memorySpaceId,
                            MDAbstractArray.toLong(memoryOffset), longBlockDimensions);
                    final byte[] byteArray =
                            type.getObjectByteifyer().byteify(type.getStorageTypeId(),
                                    data.getAsFlatArray());
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArray);
                    }
                    H5Dwrite(dataSetId, type.getNativeTypeId(), memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, byteArray);
                    baseWriter.h5.reclaimCompoundVL(type, byteArray);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void createMDArray(String objectPath, HDF5CompoundType<T> type, int[] dimensions)
    {
        createMDArray(objectPath, type, dimensions,
                HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public <T> void createMDArray(final String objectPath, final HDF5CompoundType<T> type,
            final long[] dimensions, final int[] blockDimensions)
    {
        createMDArray(objectPath, type, dimensions, blockDimensions,
                HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public <T> void createMDArray(final String objectPath, final HDF5CompoundType<T> type,
            final long[] dimensions, final int[] blockDimensions,
            final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert type != null;
        assert dimensions != null;
        assert blockDimensions != null;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    baseWriter.createDataSet(objectPath, type.getStorageTypeId(), features,
                            dimensions, MDAbstractArray.toLong(blockDimensions), type
                                    .getObjectByteifyer().getRecordSizeOnDisk(), registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void createMDArray(final String objectPath, final HDF5CompoundType<T> type,
            final int[] dimensions, final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert type != null;
        assert dimensions != null;

        baseWriter.checkOpen();
        type.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(final ICleanUpRegistry registry)
                {
                    if (features.requiresChunking())
                    {
                        final long[] nullDimensions = new long[dimensions.length];
                        baseWriter.createDataSet(objectPath, type.getStorageTypeId(), features,
                                nullDimensions, MDAbstractArray.toLong(dimensions), type
                                        .getObjectByteifyer().getRecordSizeOnDisk(), registry);
                    } else
                    {
                        baseWriter.createDataSet(objectPath, type.getStorageTypeId(), features,
                                MDAbstractArray.toLong(dimensions), null, type.getObjectByteifyer()
                                        .getRecordSizeOnDisk(), registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public <T> void writeMDArray(String objectPath, MDArray<T> data)
    {
        writeMDArray(objectPath, data, HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public <T> void writeMDArray(String objectPath, MDArray<T> data,
            HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert data != null && data.size() > 0;

        baseWriter.checkOpen();
        final HDF5CompoundType<T> inferredCompoundType = getInferredType(data.getAsFlatArray());
        inferredCompoundType.checkMappingComplete();
        primWriteCompoundMDArray(objectPath, inferredCompoundType, data, features, null);
    }

}
