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

import static ch.systemsx.cisd.hdf5.HDF5Utils.ENUM_PREFIX;
import static ch.systemsx.cisd.hdf5.HDF5Utils.createDataTypePath;

import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;
import ch.systemsx.cisd.hdf5.HDF5EnumerationType.EnumStorageForm;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * The implementation of {@link IHDF5EnumReader}.
 * 
 * @author Bernd Rinn
 */
class HDF5EnumReader implements IHDF5EnumReader
{
    protected final HDF5BaseReader baseReader;

    HDF5EnumReader(HDF5BaseReader baseReader)
    {
        assert baseReader != null;

        this.baseReader = baseReader;
    }

    // /////////////////////
    // Types
    // /////////////////////

    @Override
    public HDF5EnumerationType getType(final String name)
    {
        baseReader.checkOpen();
        final String dataTypePath =
                createDataTypePath(ENUM_PREFIX, baseReader.houseKeepingNameSuffix, name);
        final long storageDataTypeId = baseReader.getDataTypeId(dataTypePath);
        return baseReader.getEnumTypeForStorageDataType(name, storageDataTypeId, true, null, null,
                baseReader.fileRegistry);
    }

    @Override
    public HDF5EnumerationType getType(final String name, final String[] values)
            throws HDF5JavaException
    {
        return getType(name, values, true);
    }

    @Override
    public <T extends Enum<?>> HDF5EnumerationType getType(final Class<T> enumClass)
            throws HDF5JavaException
    {
        return getType(enumClass.getSimpleName(), ReflectionUtils.getEnumOptions(enumClass), true);
    }

    @Override
    public HDF5EnumerationType getType(final Class<? extends Enum<?>> enumClass, final boolean check)
            throws HDF5JavaException
    {
        return getType(enumClass.getSimpleName(), ReflectionUtils.getEnumOptions(enumClass), check);
    }

    @Override
    public HDF5EnumerationType getType(final String name, final Class<? extends Enum<?>> enumClass)
            throws HDF5JavaException
    {
        return getType(name, ReflectionUtils.getEnumOptions(enumClass), true);
    }

    @Override
    public <T extends Enum<?>> HDF5EnumerationType getType(final String name,
            final Class<T> enumClass, final boolean check) throws HDF5JavaException
    {
        return getType(name, ReflectionUtils.getEnumOptions(enumClass), check);
    }

    @Override
    public HDF5EnumerationType getType(final String name, final String[] values, final boolean check)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5EnumerationType dataType = getType(name);
        if (check)
        {
            baseReader.checkEnumValues(dataType.getStorageTypeId(), values, name);
        }
        return dataType;
    }

    @Override
    public HDF5EnumerationType getType(EnumerationType genericType) throws HDF5JavaException
    {
        return getType(genericType, true);
    }

    @Override
    public HDF5EnumerationType getType(EnumerationType genericType, boolean check)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5EnumerationType dataType = getType(genericType.tryGetName());
        if (check)
        {
            baseReader.checkEnumValues(dataType.getStorageTypeId(), genericType.getValueArray(),
                    genericType.tryGetName());
        }
        return dataType;
    }

    @Override
    public HDF5EnumerationType getDataSetType(final String dataSetPath)
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5EnumerationType> readEnumTypeCallable =
                new ICallableWithCleanUp<HDF5EnumerationType>()
                    {
                        @Override
                        public HDF5EnumerationType call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, dataSetPath,
                                            registry);
                            return getEnumTypeForDataSetId(dataSetId, dataSetPath,
                                    baseReader.isScaledEnum(dataSetId, registry), registry);
                        }
                    };
        return baseReader.runner.call(readEnumTypeCallable);
    }

    private HDF5EnumerationType getEnumTypeForDataSetId(final long objectId,
            final String objectName, final boolean scaledEnum, final ICleanUpRegistry registry)
    {
        if (scaledEnum)
        {
            final String enumTypeName =
                    baseReader.getStringAttribute(objectId, objectName, HDF5Utils
                            .getEnumTypeNameAttributeName(baseReader.houseKeepingNameSuffix),
                            false, registry);
            return getType(enumTypeName);
        } else
        {
            final long storageDataTypeId =
                    baseReader.h5.getDataTypeForDataSet(objectId, baseReader.fileRegistry);
            return baseReader.getEnumTypeForStorageDataType(null, storageDataTypeId, true,
                    objectName, null, baseReader.fileRegistry);
        }
    }

    @Override
    public HDF5EnumerationType getAttributeType(final String dataSetPath, final String attributeName)
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5EnumerationType> readEnumTypeCallable =
                new ICallableWithCleanUp<HDF5EnumerationType>()
                    {
                        @Override
                        public HDF5EnumerationType call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, dataSetPath,
                                            registry);
                            final long attributeId =
                                    baseReader.h5.openAttribute(dataSetId, attributeName, registry);
                            final long storageDataTypeId =
                                    baseReader.h5.getDataTypeForAttribute(attributeId,
                                            baseReader.fileRegistry);
                            return baseReader.getEnumTypeForStorageDataType(null,
                                    storageDataTypeId, true, dataSetPath, attributeName,
                                    baseReader.fileRegistry);
                        }
                    };
        return baseReader.runner.call(readEnumTypeCallable);
    }

    // /////////////////////
    // Attributes
    // /////////////////////

    @Override
    public String getAttrAsString(final String objectPath, final String attributeName)
            throws HDF5JavaException
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String> readRunnable = new ICallableWithCleanUp<String>()
            {
                @Override
                public String call(ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    final long attributeId =
                            baseReader.h5.openAttribute(objectId, attributeName, registry);
                    final long storageDataTypeId =
                            baseReader.h5.getDataTypeForAttribute(attributeId, registry);
                    final long nativeDataTypeId =
                            baseReader.h5.getNativeDataType(storageDataTypeId, registry);

                    final long enumDataTypeId =
                            baseReader.getEnumDataTypeId(storageDataTypeId, registry);
                    final int size = baseReader.h5.getDataTypeSize(enumDataTypeId);
                    final byte[] data =
                            baseReader.h5.readAttributeAsByteArray(attributeId, nativeDataTypeId,
                                    size);
                    final String value =
                            baseReader.h5.getNameForEnumOrCompoundMemberIndex(enumDataTypeId,
                                    EnumerationType.fromStorageForm(data, 0, size));
                    if (value == null)
                    {
                        throw new HDF5JavaException("Attribute " + attributeName + " of object "
                                + objectPath + " needs to be an Enumeration.");
                    }
                    return value;
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    @Override
    public HDF5EnumerationValue getAttr(final String objectPath, final String attributeName)
            throws HDF5JavaException
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5EnumerationValue> readRunnable =
                new ICallableWithCleanUp<HDF5EnumerationValue>()
                    {
                        @Override
                        public HDF5EnumerationValue call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            final long attributeId =
                                    baseReader.h5.openAttribute(objectId, attributeName, registry);
                            final long storageDataTypeId =
                                    baseReader.h5.getDataTypeForAttribute(attributeId,
                                            baseReader.fileRegistry);
                            final long enumTypeId =
                                    baseReader.getEnumDataTypeId(storageDataTypeId,
                                            baseReader.fileRegistry);
                            final HDF5EnumerationType enumType =
                                    baseReader.getEnumTypeForStorageDataType(null, enumTypeId,
                                            true, objectPath, attributeName,
                                            baseReader.fileRegistry);
                            final long nativeDataTypeId;
                            if (storageDataTypeId != enumTypeId) // Array data type
                            {
                                nativeDataTypeId =
                                        baseReader.h5
                                                .getNativeDataType(storageDataTypeId, registry);
                            } else
                            {
                                nativeDataTypeId = enumType.getNativeTypeId();
                            }
                            final int enumOrdinal =
                                    baseReader.getEnumOrdinal(attributeId, nativeDataTypeId,
                                            enumType);
                            return new HDF5EnumerationValue(enumType, enumOrdinal);
                        }
                    };

        return baseReader.runner.call(readRunnable);
    }

    @Override
    public <T extends Enum<T>> T getAttr(String objectPath, String attributeName, Class<T> enumClass)
            throws HDF5JavaException
    {
        final String value = getAttrAsString(objectPath, attributeName);
        try
        {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex)
        {
            throw new HDF5JavaException("The Java enum class " + enumClass.getCanonicalName()
                    + " has no value '" + value + "'.");
        }
    }

    @Override
    public HDF5EnumerationValueArray getArrayAttr(final String objectPath,
            final String attributeName) throws HDF5JavaException
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5EnumerationValueArray> readRunnable =
                new ICallableWithCleanUp<HDF5EnumerationValueArray>()
                    {
                        @Override
                        public HDF5EnumerationValueArray call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            final long attributeId =
                                    baseReader.h5.openAttribute(objectId, attributeName, registry);
                            return baseReader.getEnumValueArray(attributeId, objectPath,
                                    attributeName, registry);
                        }

                    };
        return baseReader.runner.call(readRunnable);
    }

    public String[] getEnumArrayAttributeAsString(final String objectPath,
            final String attributeName) throws HDF5JavaException
    {
        final HDF5EnumerationValueArray array = getArrayAttr(objectPath, attributeName);
        return array.toStringArray();
    }

    @Override
    public HDF5EnumerationValueMDArray getMDArrayAttr(final String objectPath,
            final String attributeName) throws HDF5JavaException
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5EnumerationValueMDArray> readRunnable =
                new ICallableWithCleanUp<HDF5EnumerationValueMDArray>()
                    {
                        @Override
                        public HDF5EnumerationValueMDArray call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            final long attributeId =
                                    baseReader.h5.openAttribute(objectId, attributeName, registry);
                            return baseReader.getEnumValueMDArray(attributeId, objectPath,
                                    attributeName, registry);
                        }

                    };
        return baseReader.runner.call(readRunnable);
    }

    // /////////////////////
    // Data Sets
    // /////////////////////

    @Override
    public String readAsString(final String objectPath) throws HDF5JavaException
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String> writeRunnable = new ICallableWithCleanUp<String>()
            {
                @Override
                public String call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final long storageDataTypeId =
                            baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                    final long nativeDataTypeId =
                            baseReader.h5.getNativeDataType(storageDataTypeId, registry);
                    final int size = baseReader.h5.getDataTypeSize(nativeDataTypeId);
                    final byte[] data = new byte[size];
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, data);
                    final String value =
                            baseReader.h5.getNameForEnumOrCompoundMemberIndex(storageDataTypeId,
                                    EnumerationType.fromStorageForm(data));
                    if (value == null)
                    {
                        throw new HDF5JavaException(objectPath + " needs to be an Enumeration.");
                    }
                    return value;
                }
            };
        return baseReader.runner.call(writeRunnable);
    }

    @Override
    public <T extends Enum<T>> T read(String objectPath, Class<T> enumClass)
            throws HDF5JavaException
    {
        final String value = readAsString(objectPath);
        try
        {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex)
        {
            throw new HDF5JavaException("The Java enum class " + enumClass.getCanonicalName()
                    + " has no value '" + value + "'.");
        }
    }

    @Override
    public HDF5EnumerationValue read(final String objectPath) throws HDF5JavaException
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5EnumerationValue> readRunnable =
                new ICallableWithCleanUp<HDF5EnumerationValue>()
                    {
                        @Override
                        public HDF5EnumerationValue call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final HDF5EnumerationType enumType =
                                    getEnumTypeForDataSetId(dataSetId, objectPath, false, registry);
                            return readEnumValue(dataSetId, enumType);
                        }
                    };

        return baseReader.runner.call(readRunnable);
    }

    @Override
    public HDF5EnumerationValue read(final String objectPath, final HDF5EnumerationType enumType)
            throws HDF5JavaException
    {
        assert objectPath != null;
        assert enumType != null;

        baseReader.checkOpen();
        enumType.check(baseReader.fileId);
        final ICallableWithCleanUp<HDF5EnumerationValue> readRunnable =
                new ICallableWithCleanUp<HDF5EnumerationValue>()
                    {
                        @Override
                        public HDF5EnumerationValue call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            return readEnumValue(dataSetId, enumType);
                        }
                    };

        return baseReader.runner.call(readRunnable);
    }

    private HDF5EnumerationValue readEnumValue(final long dataSetId,
            final HDF5EnumerationType enumType)
    {
        final byte[] data = new byte[enumType.getStorageForm().getStorageSize()];
        baseReader.h5.readDataSet(dataSetId, enumType.getNativeTypeId(), data);
        return new HDF5EnumerationValue(enumType, EnumerationType.fromStorageForm(data));
    }

    @Override
    public HDF5EnumerationValueArray readArray(final String objectPath,
            final HDF5EnumerationType enumTypeOrNull) throws HDF5JavaException
    {
        assert objectPath != null;

        baseReader.checkOpen();
        if (enumTypeOrNull != null)
        {
            enumTypeOrNull.check(baseReader.fileId);
        }
        final ICallableWithCleanUp<HDF5EnumerationValueArray> readRunnable =
                new ICallableWithCleanUp<HDF5EnumerationValueArray>()
                    {
                        @Override
                        public HDF5EnumerationValueArray call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final long[] dimensions =
                                    baseReader.h5.getDataDimensions(dataSetId, registry);
                            final boolean scaledEnum = baseReader.isScaledEnum(dataSetId, registry);
                            final HDF5EnumerationType actualEnumType =
                                    (enumTypeOrNull == null) ? getEnumTypeForDataSetId(dataSetId,
                                            objectPath, scaledEnum, registry) : enumTypeOrNull;
                            final int arraySize = HDF5Utils.getOneDimensionalArraySize(dimensions);
                            final EnumStorageForm storageForm = actualEnumType.getStorageForm();
                            final byte[] data = new byte[arraySize * storageForm.getStorageSize()];
                            if (scaledEnum)
                            {
                                baseReader.h5.readDataSet(dataSetId, actualEnumType
                                        .getStorageForm().getIntNativeTypeId(), data);
                            } else
                            {
                                baseReader.h5.readDataSet(dataSetId,
                                        actualEnumType.getNativeTypeId(), data);
                            }
                            return new HDF5EnumerationValueArray(actualEnumType,
                                    EnumerationType.fromStorageForm(data, storageForm));
                        }
                    };

        return baseReader.runner.call(readRunnable);
    }

    @Override
    public HDF5EnumerationValueArray readArray(final String objectPath) throws HDF5JavaException
    {
        return readArray(objectPath, (HDF5EnumerationType) null);
    }

    @Override
    public HDF5EnumerationValueArray readArrayBlockWithOffset(final String objectPath,
            final HDF5EnumerationType enumTypeOrNull, final int blockSize, final long offset)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        if (enumTypeOrNull != null)
        {
            enumTypeOrNull.check(baseReader.fileId);
        }
        final ICallableWithCleanUp<HDF5EnumerationValueArray> readRunnable =
                new ICallableWithCleanUp<HDF5EnumerationValueArray>()
                    {
                        @Override
                        public HDF5EnumerationValueArray call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final DataSpaceParameters spaceParams =
                                    baseReader.getSpaceParameters(dataSetId, offset, blockSize,
                                            registry);
                            final boolean scaledEnum = baseReader.isScaledEnum(dataSetId, registry);
                            final HDF5EnumerationType actualEnumType =
                                    (enumTypeOrNull == null) ? getEnumTypeForDataSetId(dataSetId,
                                            objectPath, scaledEnum, registry) : enumTypeOrNull;
                            final byte[] data =
                                    new byte[spaceParams.blockSize
                                            * actualEnumType.getStorageForm().getStorageSize()];
                            if (scaledEnum)
                            {
                                baseReader.h5.readDataSet(dataSetId, actualEnumType
                                        .getStorageForm().getIntNativeTypeId(),
                                        spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                            } else
                            {
                                baseReader.h5.readDataSet(dataSetId,
                                        actualEnumType.getNativeTypeId(),
                                        spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                            }
                            return new HDF5EnumerationValueArray(actualEnumType,
                                    EnumerationType.fromStorageForm(data,
                                            actualEnumType.getStorageForm()));
                        }
                    };

        return baseReader.runner.call(readRunnable);
    }

    @Override
    public HDF5EnumerationValueArray readArrayBlockWithOffset(final String objectPath,
            final int blockSize, final long offset)
    {
        return readArrayBlockWithOffset(objectPath, (HDF5EnumerationType) null, blockSize, offset);
    }

    @Override
    public HDF5EnumerationValueArray readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber)
    {
        return readArrayBlockWithOffset(objectPath, (HDF5EnumerationType) null, blockSize,
                blockNumber * blockSize);
    }

    @Override
    public HDF5EnumerationValueArray readArrayBlock(final String objectPath,
            final HDF5EnumerationType enumType, final int blockSize, final long blockNumber)
    {
        return readArrayBlockWithOffset(objectPath, enumType, blockSize, blockNumber * blockSize);
    }

    @Override
    public HDF5EnumerationValueMDArray readMDArray(final String objectPath)
            throws HDF5JavaException
    {
        return readMDArray(objectPath, (HDF5EnumerationType) null);
    }

    @Override
    public HDF5EnumerationValueMDArray readMDArray(final String objectPath,
            final HDF5EnumerationType enumTypeOrNull) throws HDF5JavaException
    {
        return readMDArrayBlockWithOffset(objectPath, enumTypeOrNull, null, null);
    }

    @Override
    public HDF5EnumerationValueMDArray readMDArrayBlockWithOffset(final String objectPath,
            final HDF5EnumerationType enumTypeOrNull, final int[] blockDimensionsOrNull,
            final long[] offsetOrNull)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        if (enumTypeOrNull != null)
        {
            enumTypeOrNull.check(baseReader.fileId);
        }
        final ICallableWithCleanUp<HDF5EnumerationValueMDArray> writeRunnable =
                new ICallableWithCleanUp<HDF5EnumerationValueMDArray>()
                    {
                        @Override
                        public HDF5EnumerationValueMDArray call(final ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final boolean scaledEnum = baseReader.isScaledEnum(dataSetId, registry);
                            final HDF5EnumerationType actualEnumType =
                                    (enumTypeOrNull == null) ? getEnumTypeForDataSetId(dataSetId,
                                            objectPath, scaledEnum, registry) : enumTypeOrNull;
                            final DataSpaceParameters spaceParams =
                                    baseReader.getSpaceParameters(dataSetId, offsetOrNull,
                                            blockDimensionsOrNull, registry);
                            final EnumStorageForm storageForm = actualEnumType.getStorageForm();
                            final byte[] byteArr =
                                    new byte[spaceParams.blockSize * storageForm.getStorageSize()];
                            if (scaledEnum)
                            {
                                baseReader.h5
                                        .readDataSet(dataSetId, storageForm.getIntNativeTypeId(),
                                                spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                                byteArr);
                            } else
                            {
                                baseReader.h5
                                        .readDataSet(dataSetId, actualEnumType.getNativeTypeId(),
                                                spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                                byteArr);
                            }
                            return new HDF5EnumerationValueMDArray(actualEnumType,
                                    EnumerationType.fromStorageForm(byteArr,
                                            spaceParams.dimensions, storageForm));
                        }
                    };
        return baseReader.runner.call(writeRunnable);
    }

    @Override
    public HDF5EnumerationValueMDArray readMDArrayBlock(String objectPath,
            HDF5EnumerationType type, int[] blockDimensions, long[] blockNumber)
            throws HDF5JavaException
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockDimensions[i] * blockNumber[i];
        }
        return readMDArrayBlockWithOffset(objectPath, type, blockDimensions, offset);
    }

    @Override
    public HDF5EnumerationValueMDArray readMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber) throws HDF5JavaException
    {
        return readMDArrayBlock(objectPath, null, blockDimensions, blockNumber);
    }

    @Override
    public HDF5EnumerationValueMDArray readMDArrayBlockWithOffset(String objectPath,
            int[] blockDimensions, long[] offset) throws HDF5JavaException
    {
        return readMDArrayBlockWithOffset(objectPath, null, blockDimensions, offset);
    }

    @Override
    public Iterable<HDF5DataBlock<HDF5EnumerationValueArray>> getArrayBlocks(
            final String objectPath, final HDF5EnumerationType enumTypeOrNull)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(objectPath));

        return new Iterable<HDF5DataBlock<HDF5EnumerationValueArray>>()
            {
                @Override
                public Iterator<HDF5DataBlock<HDF5EnumerationValueArray>> iterator()
                {
                    return new Iterator<HDF5DataBlock<HDF5EnumerationValueArray>>()
                        {
                            final HDF5NaturalBlock1DParameters.HDF5NaturalBlock1DIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5DataBlock<HDF5EnumerationValueArray> next()
                            {
                                final long offset = index.computeOffsetAndSizeGetOffset();
                                final HDF5EnumerationValueArray block =
                                        readArrayBlockWithOffset(objectPath, enumTypeOrNull,
                                                index.getBlockSize(), offset);
                                return new HDF5DataBlock<HDF5EnumerationValueArray>(block,
                                        index.getAndIncIndex(), offset);
                            }

                            @Override
                            public void remove()
                            {
                                throw new UnsupportedOperationException();
                            }
                        };
                }
            };
    }

    @Override
    public Iterable<HDF5DataBlock<HDF5EnumerationValueArray>> getArrayBlocks(final String objectPath)
            throws HDF5JavaException
    {
        return getArrayBlocks(objectPath, (HDF5EnumerationType) null);
    }

    @Override
    public Iterable<HDF5MDEnumBlock> getMDArrayBlocks(final String objectPath,
            final HDF5EnumerationType enumTypeOrNull) throws HDF5JavaException
    {
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(objectPath,
                        DataTypeInfoOptions.MINIMAL, true));
        return new Iterable<HDF5MDEnumBlock>()
            {
                @Override
                public Iterator<HDF5MDEnumBlock> iterator()
                {
                    return new Iterator<HDF5MDEnumBlock>()
                        {
                            final HDF5NaturalBlockMDParameters.HDF5NaturalBlockMDIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5MDEnumBlock next()
                            {
                                final long[] offset = index.computeOffsetAndSizeGetOffsetClone();
                                final HDF5EnumerationValueMDArray block =
                                        readMDArrayBlockWithOffset(objectPath, enumTypeOrNull,
                                                index.getBlockSize(), offset);
                                return new HDF5MDEnumBlock(block, index.getIndexClone(), offset);
                            }

                            @Override
                            public void remove()
                            {
                                throw new UnsupportedOperationException();
                            }
                        };
                }
            };
    }

    @Override
    public Iterable<HDF5MDEnumBlock> getMDArrayBlocks(String objectPath) throws HDF5JavaException
    {
        return getMDArrayBlocks(objectPath, null);
    }

}
