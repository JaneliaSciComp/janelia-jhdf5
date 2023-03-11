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

import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * The implementation of {@link IHDF5CompoundReader}.
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundReader extends HDF5CompoundInformationRetriever implements IHDF5CompoundReader
{

    HDF5CompoundReader(HDF5BaseReader baseReader, IHDF5EnumReader enumReader)
    {
        super(baseReader, enumReader);
    }

    @Override
    public <T> T getAttr(final String objectPath, final String attributeName,
            final HDF5CompoundType<T> type) throws HDF5JavaException
    {
        return primGetCompoundAttribute(objectPath, attributeName, type, null);
    }

    @Override
    public <T> T getAttr(final String objectPath, final String attributeName,
            final Class<T> pojoClass) throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5CompoundType<T> attributeCompoundType =
                getAttributeType(objectPath, attributeName, pojoClass);
        attributeCompoundType.checkMappingComplete();
        return primGetCompoundAttribute(objectPath, attributeName, attributeCompoundType, null);
    }

    @Override
    public <T> T[] getArrayAttr(String objectPath, String attributeName, HDF5CompoundType<T> type)
            throws HDF5JavaException
    {
        return primGetCompoundArrayAttribute(objectPath, attributeName, type, null);
    }

    @Override
    public <T> T[] getArrayAttr(String objectPath, String attributeName, Class<T> pojoClass)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5CompoundType<T> attributeCompoundType =
                getAttributeType(objectPath, attributeName, pojoClass);
        attributeCompoundType.checkMappingComplete();
        return primGetCompoundArrayAttribute(objectPath, attributeName, attributeCompoundType, null);
    }

    @Override
    public <T> MDArray<T> getMDArrayAttr(String objectPath, String attributeName,
            HDF5CompoundType<T> type) throws HDF5JavaException
    {
        return primGetCompoundMDArrayAttribute(objectPath, attributeName, type, null);
    }

    @Override
    public <T> MDArray<T> getMDArrayAttr(String objectPath, String attributeName, Class<T> pojoClass)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5CompoundType<T> attributeCompoundType =
                getAttributeType(objectPath, attributeName, pojoClass);
        attributeCompoundType.checkMappingComplete();
        return primGetCompoundMDArrayAttribute(objectPath, attributeName, attributeCompoundType,
                null);
    }

    private <T> T primGetCompoundAttribute(final String objectPath, final String attributeName,
            final HDF5CompoundType<T> type, final IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException
    {
        final ICallableWithCleanUp<T> readRunnable = new ICallableWithCleanUp<T>()
            {
                @Override
                public T call(final ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    final long attributeId =
                            baseReader.h5.openAttribute(objectId, attributeName, registry);
                    final long storageDataTypeId =
                            baseReader.h5.getDataTypeForAttribute(attributeId, registry);
                    checkCompoundType(storageDataTypeId, objectPath, type);
                    final long nativeDataTypeId = type.getNativeTypeId();
                    final byte[] byteArr =
                            baseReader.h5.readAttributeAsByteArray(attributeId, nativeDataTypeId,
                                    type.getObjectByteifyer().getRecordSizeInMemory());
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArr);
                    }
                    final T scalar = type.getObjectByteifyer().arrayifyScalar(storageDataTypeId, byteArr,
                            type.getCompoundType());
                    baseReader.h5.reclaimCompoundVL(type, byteArr);
                    return scalar;
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    private <T> T[] primGetCompoundArrayAttribute(final String objectPath,
            final String attributeName, final HDF5CompoundType<T> type,
            final IByteArrayInspector inspectorOrNull) throws HDF5JavaException
    {
        final ICallableWithCleanUp<T[]> readRunnable = new ICallableWithCleanUp<T[]>()
            {
                @Override
                public T[] call(final ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    final long attributeId =
                            baseReader.h5.openAttribute(dataSetId, attributeName, registry);
                    final long storageDataTypeId =
                            baseReader.h5.getDataTypeForAttribute(attributeId, registry);
                    final long nativeDataTypeId =
                            baseReader.h5.getNativeDataType(storageDataTypeId, registry);
                    final int len;
                    final long compoundTypeId;
                    if (baseReader.h5.getClassType(storageDataTypeId) == H5T_ARRAY)
                    {
                        final int[] arrayDimensions =
                                baseReader.h5.getArrayDimensions(storageDataTypeId);
                        len = HDF5Utils.getOneDimensionalArraySize(arrayDimensions);
                        compoundTypeId = baseReader.h5.getBaseDataType(storageDataTypeId, registry);
                        if (baseReader.h5.getClassType(compoundTypeId) != H5T_COMPOUND)
                        {
                            throw new HDF5JavaException("Attribute '" + attributeName
                                    + "' of object '" + objectPath
                                    + "' is not of type compound array.");
                        }
                    } else
                    {
                        if (baseReader.h5.getClassType(storageDataTypeId) != H5T_COMPOUND)
                        {
                            throw new HDF5JavaException("Attribute '" + attributeName
                                    + "' of object '" + objectPath
                                    + "' is not of type compound array.");
                        }
                        compoundTypeId = storageDataTypeId;
                        final long[] arrayDimensions =
                                baseReader.h5.getDataDimensionsForAttribute(attributeId, registry);
                        len = HDF5Utils.getOneDimensionalArraySize(arrayDimensions);
                    }
                    checkCompoundType(compoundTypeId, objectPath, type);
                    final byte[] byteArr =
                            baseReader.h5.readAttributeAsByteArray(attributeId, nativeDataTypeId,
                                    len * type.getRecordSizeInMemory());
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArr);
                    }
                    final T[] array = type.getObjectByteifyer().arrayify(storageDataTypeId, byteArr,
                            type.getCompoundType());
                    baseReader.h5.reclaimCompoundVL(type, byteArr);
                    return array;
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    private <T> MDArray<T> primGetCompoundMDArrayAttribute(final String objectPath,
            final String attributeName, final HDF5CompoundType<T> type,
            final IByteArrayInspector inspectorOrNull) throws HDF5JavaException
    {
        final ICallableWithCleanUp<MDArray<T>> readRunnable =
                new ICallableWithCleanUp<MDArray<T>>()
                    {
                        @Override
                        public MDArray<T> call(final ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            final long attributeId =
                                    baseReader.h5.openAttribute(dataSetId, attributeName, registry);
                            final long storageDataTypeId =
                                    baseReader.h5.getDataTypeForAttribute(attributeId, registry);
                            final long nativeDataTypeId =
                                    baseReader.h5.getNativeDataType(storageDataTypeId, registry);
                            final int len;
                            final int[] arrayDimensions;
                            final long compoundTypeId;
                            if (baseReader.h5.getClassType(storageDataTypeId) == H5T_ARRAY)
                            {
                                arrayDimensions =
                                        baseReader.h5.getArrayDimensions(storageDataTypeId);
                                len = MDAbstractArray.getLength(arrayDimensions);
                                compoundTypeId =
                                        baseReader.h5.getBaseDataType(storageDataTypeId, registry);
                                if (baseReader.h5.getClassType(compoundTypeId) != H5T_COMPOUND)
                                {
                                    throw new HDF5JavaException("Attribute '" + attributeName
                                            + "' of object '" + objectPath
                                            + "' is not of type compound array.");
                                }
                            } else
                            {
                                if (baseReader.h5.getClassType(storageDataTypeId) != H5T_COMPOUND)
                                {
                                    throw new HDF5JavaException("Attribute '" + attributeName
                                            + "' of object '" + objectPath
                                            + "' is not of type compound array.");
                                }
                                compoundTypeId = storageDataTypeId;
                                arrayDimensions =
                                        MDAbstractArray.toInt(baseReader.h5.getDataDimensionsForAttribute(
                                                attributeId, registry));
                                len = MDAbstractArray.getLength(arrayDimensions);
                            }
                            checkCompoundType(compoundTypeId, objectPath, type);
                            final byte[] byteArr =
                                    baseReader.h5.readAttributeAsByteArray(attributeId,
                                            nativeDataTypeId, len * type.getRecordSizeInMemory());
                            if (inspectorOrNull != null)
                            {
                                inspectorOrNull.inspect(byteArr);
                            }
                            final MDArray<T> array = new MDArray<T>(type.getObjectByteifyer().arrayify(
                                    storageDataTypeId, byteArr, type.getCompoundType()),
                                    arrayDimensions);
                            baseReader.h5.reclaimCompoundVL(type, byteArr);
                            return array;
                        }
                    };
        return baseReader.runner.call(readRunnable);
    }

    @Override
    public <T> T read(final String objectPath, final HDF5CompoundType<T> type)
            throws HDF5JavaException
    {
        return read(objectPath, type, null);
    }

    @Override
    public <T> T read(final String objectPath, final Class<T> pojoClass) throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5CompoundType<T> dataSetCompoundType = getDataSetType(objectPath, pojoClass);
        dataSetCompoundType.checkMappingComplete();
        return read(objectPath, dataSetCompoundType, null);
    }

    @Override
    public <T> T read(final String objectPath, final HDF5CompoundType<T> type,
            final IByteArrayInspector inspectorOrNull) throws HDF5JavaException
    {
        baseReader.checkOpen();
        type.check(baseReader.fileId);
        return primReadCompound(objectPath, -1, -1, type, inspectorOrNull);
    }

    @Override
    public <T> T[] readArray(final String objectPath, final HDF5CompoundType<T> type)
            throws HDF5JavaException
    {
        return readArray(objectPath, type, null);
    }

    @Override
    public <T> T[] readArray(final String objectPath, final HDF5CompoundType<T> type,
            final IByteArrayInspector inspectorOrNull) throws HDF5JavaException
    {
        baseReader.checkOpen();
        type.check(baseReader.fileId);
        return primReadCompoundArray(objectPath, -1, -1, type, inspectorOrNull);
    }

    @Override
    public <T> T[] readArray(final String objectPath, final Class<T> pojoClass)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5CompoundType<T> dataSetCompoundType = getDataSetType(objectPath, pojoClass);
        dataSetCompoundType.checkMappingComplete();
        return readArray(objectPath, dataSetCompoundType, null);
    }

    @Override
    public <T> T[] readArrayBlock(final String objectPath, final HDF5CompoundType<T> type,
            final int blockSize, final long blockNumber) throws HDF5JavaException
    {
        return readArrayBlock(objectPath, type, blockSize, blockNumber, null);
    }

    @Override
    public <T> T[] readArrayBlock(final String objectPath, final HDF5CompoundType<T> type,
            final int blockSize, final long blockNumber, final IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        type.check(baseReader.fileId);
        return primReadCompoundArray(objectPath, blockSize, blockSize * blockNumber, type,
                inspectorOrNull);
    }

    @Override
    public <T> T[] readArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final int blockSize, final long offset)
            throws HDF5JavaException
    {
        return readArrayBlockWithOffset(objectPath, type, blockSize, offset, null);
    }

    @Override
    public <T> T[] readArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final int blockSize, final long offset,
            final IByteArrayInspector inspectorOrNull) throws HDF5JavaException
    {
        baseReader.checkOpen();
        type.check(baseReader.fileId);
        return primReadCompoundArray(objectPath, blockSize, offset, type, inspectorOrNull);
    }

    @Override
    public <T> Iterable<HDF5DataBlock<T[]>> getArrayBlocks(final String objectPath,
            final HDF5CompoundType<T> type) throws HDF5JavaException
    {
        return getArrayBlocks(objectPath, type, null);
    }

    @Override
    public <T> Iterable<HDF5DataBlock<T[]>> getArrayBlocks(final String objectPath,
            final HDF5CompoundType<T> type, final IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        type.check(baseReader.fileId);
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(objectPath));

        return primGetCompoundArrayNaturalBlocks(objectPath, type, params, inspectorOrNull);
    }

    private <T> Iterable<HDF5DataBlock<T[]>> primGetCompoundArrayNaturalBlocks(
            final String objectPath, final HDF5CompoundType<T> type,
            final HDF5NaturalBlock1DParameters params, final IByteArrayInspector inspectorOrNull)
    {
        return new Iterable<HDF5DataBlock<T[]>>()
            {
                @Override
                public Iterator<HDF5DataBlock<T[]>> iterator()
                {
                    return new Iterator<HDF5DataBlock<T[]>>()
                        {
                            final HDF5NaturalBlock1DParameters.HDF5NaturalBlock1DIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5DataBlock<T[]> next()
                            {
                                final long offset = index.computeOffsetAndSizeGetOffset();
                                final T[] block =
                                        readArrayBlockWithOffset(objectPath, type,
                                                index.getBlockSize(), offset, inspectorOrNull);
                                return new HDF5DataBlock<T[]>(block, index.getAndIncIndex(), offset);
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
    public <T> Iterable<HDF5DataBlock<T[]>> getArrayBlocks(String objectPath, Class<T> pojoClass)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(objectPath));

        final HDF5CompoundType<T> dataSetCompoundType = getDataSetType(objectPath, pojoClass);
        dataSetCompoundType.checkMappingComplete();
        return primGetCompoundArrayNaturalBlocks(objectPath, dataSetCompoundType, params, null);
    }

    private <T> T primReadCompound(final String objectPath, final int blockSize, final long offset,
            final HDF5CompoundType<T> type, final IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException
    {
        final ICallableWithCleanUp<T> readRunnable = new ICallableWithCleanUp<T>()
            {
                @Override
                public T call(final ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final long storageDataTypeId =
                            baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                    checkCompoundType(storageDataTypeId, objectPath, type);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, offset, blockSize, registry);
                    final long nativeDataTypeId = type.getNativeTypeId();
                    final byte[] byteArr =
                            new byte[spaceParams.blockSize
                                    * type.getObjectByteifyer().getRecordSizeInMemory()];
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, byteArr);
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArr);
                    }
                    final T scalar = type.getObjectByteifyer().arrayifyScalar(storageDataTypeId, byteArr,
                            type.getCompoundType());
                    baseReader.h5.reclaimCompoundVL(type, byteArr);
                    return scalar;
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    private <T> T[] primReadCompoundArray(final String objectPath, final int blockSize,
            final long offset, final HDF5CompoundType<T> type,
            final IByteArrayInspector inspectorOrNull) throws HDF5JavaException
    {
        final ICallableWithCleanUp<T[]> readRunnable = new ICallableWithCleanUp<T[]>()
            {
                @Override
                public T[] call(final ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final long storageDataTypeId =
                            baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                    checkCompoundType(storageDataTypeId, objectPath, type);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, offset, blockSize, registry);
                    final long nativeDataTypeId = type.getNativeTypeId();
                    final byte[] byteArr =
                            new byte[spaceParams.blockSize
                                    * type.getObjectByteifyer().getRecordSizeInMemory()];
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, byteArr);
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArr);
                    }
                    final T[] array = type.getObjectByteifyer().arrayify(storageDataTypeId, byteArr,
                            type.getCompoundType());
                    baseReader.h5.reclaimCompoundVL(type, byteArr);
                    return array;
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    private void checkCompoundType(final long dataTypeId, final String path,
            final HDF5CompoundType<?> type) throws HDF5JavaException
    {
        final boolean isCompound = (baseReader.h5.getClassType(dataTypeId) == H5T_COMPOUND);
        if (isCompound == false)
        {
            throw new HDF5JavaException("Data set '" + path + "' is no compound.");
        }
        if (type.isRequireTypesToBeEqual())
        {
            final boolean isEqual =
                    (baseReader.h5.dataTypesAreEqual(dataTypeId, type.getStorageTypeId()));
            if (isEqual == false)
            {
                throw new HDF5JavaException("The compound type '" + type.getName()
                        + "' does not equal the compound type of data set '" + path + "'.");
            }
        }
    }

    @Override
    public <T> MDArray<T> readMDArray(final String objectPath, final HDF5CompoundType<T> type)
            throws HDF5JavaException
    {
        return readMDArrayBlockWithOffset(objectPath, type, null, null, null);
    }

    @Override
    public <T> MDArray<T> readMDArray(final String objectPath, final HDF5CompoundType<T> type,
            final IByteArrayInspector inspectorOrNull) throws HDF5JavaException
    {
        return readMDArrayBlockWithOffset(objectPath, type, null, null, inspectorOrNull);
    }

    @Override
    public <T> MDArray<T> readMDArray(String objectPath, Class<T> pojoClass)
            throws HDF5JavaException
    {
        final HDF5CompoundType<T> dataSetCompoundType = getDataSetType(objectPath, pojoClass);
        dataSetCompoundType.checkMappingComplete();
        return readMDArrayBlockWithOffset(objectPath, dataSetCompoundType, null, null, null);
    }

    @Override
    public <T> MDArray<T> readMDArrayBlock(final String objectPath, final HDF5CompoundType<T> type,
            final int[] blockDimensions, final long[] blockNumber) throws HDF5JavaException
    {
        return readMDArrayBlock(objectPath, type, blockDimensions, blockNumber, null);
    }

    @Override
    public <T> MDArray<T> readMDArrayBlock(final String objectPath, final HDF5CompoundType<T> type,
            final int[] blockDimensions, final long[] blockNumber,
            final IByteArrayInspector inspectorOrNull) throws HDF5JavaException
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockDimensions[i] * blockNumber[i];
        }
        return readMDArrayBlockWithOffset(objectPath, type, blockDimensions, offset,
                inspectorOrNull);
    }

    @Override
    public <T> MDArray<T> readMDArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final int[] blockDimensions, final long[] offset)
            throws HDF5JavaException
    {
        return readMDArrayBlockWithOffset(objectPath, type, blockDimensions, offset, null);
    }

    @Override
    public <T> MDArray<T> readMDArrayBlockWithOffset(final String objectPath,
            final HDF5CompoundType<T> type, final int[] dimensionsOrNull,
            final long[] offsetOrNull, final IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        type.check(baseReader.fileId);
        final ICallableWithCleanUp<MDArray<T>> readRunnable =
                new ICallableWithCleanUp<MDArray<T>>()
                    {
                        @Override
                        public MDArray<T> call(final ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final long storageDataTypeId =
                                    baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                            checkCompoundType(storageDataTypeId, objectPath, type);
                            final DataSpaceParameters spaceParams =
                                    baseReader.getSpaceParameters(dataSetId, offsetOrNull,
                                            dimensionsOrNull, registry);
                            final long nativeDataTypeId = type.getNativeTypeId();
                            final byte[] byteArr =
                                    new byte[spaceParams.blockSize
                                            * type.getObjectByteifyer().getRecordSizeInMemory()];
                            baseReader.h5.readDataSet(dataSetId, nativeDataTypeId,
                                    spaceParams.memorySpaceId, spaceParams.dataSpaceId, byteArr);
                            if (inspectorOrNull != null)
                            {
                                inspectorOrNull.inspect(byteArr);
                            }
                            final MDArray<T> array = new MDArray<T>(type.getObjectByteifyer().arrayify(
                                    storageDataTypeId, byteArr, type.getCompoundType()),
                                    spaceParams.dimensions);
                            baseReader.h5.reclaimCompoundVL(type, byteArr);
                            return array;
                        }
                    };
        return baseReader.runner.call(readRunnable);
    }

    @Override
    public <T> Iterable<HDF5MDDataBlock<MDArray<T>>> getMDArrayBlocks(final String objectPath,
            final HDF5CompoundType<T> type) throws HDF5JavaException
    {
        return getMDArrayBlocks(objectPath, type, null);
    }

    @Override
    public <T> Iterable<HDF5MDDataBlock<MDArray<T>>> getMDArrayBlocks(final String objectPath,
            final HDF5CompoundType<T> type, final IByteArrayInspector inspectorOrNull)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        type.check(baseReader.fileId);
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(objectPath,
                        DataTypeInfoOptions.MINIMAL, true));

        return primGetCompoundMDArrayNaturalBlocks(objectPath, type, params, inspectorOrNull);
    }

    private <T> Iterable<HDF5MDDataBlock<MDArray<T>>> primGetCompoundMDArrayNaturalBlocks(
            final String objectPath, final HDF5CompoundType<T> type,
            final HDF5NaturalBlockMDParameters params, final IByteArrayInspector inspectorOrNull)
    {
        return new Iterable<HDF5MDDataBlock<MDArray<T>>>()
            {
                @Override
                public Iterator<HDF5MDDataBlock<MDArray<T>>> iterator()
                {
                    return new Iterator<HDF5MDDataBlock<MDArray<T>>>()
                        {
                            final HDF5NaturalBlockMDParameters.HDF5NaturalBlockMDIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5MDDataBlock<MDArray<T>> next()
                            {
                                final long[] offset = index.computeOffsetAndSizeGetOffsetClone();
                                final MDArray<T> block =
                                        readMDArrayBlockWithOffset(objectPath, type,
                                                index.getBlockSize(), offset, inspectorOrNull);
                                return new HDF5MDDataBlock<MDArray<T>>(block,
                                        index.getIndexClone(), offset);
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
    public <T> Iterable<HDF5MDDataBlock<MDArray<T>>> getMDArrayBlocks(String objectPath,
            Class<T> pojoClass) throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(objectPath));

        final HDF5CompoundType<T> dataSetCompoundType = getDataSetType(objectPath, pojoClass);
        dataSetCompoundType.checkMappingComplete();
        return primGetCompoundMDArrayNaturalBlocks(objectPath, dataSetCompoundType, params, null);
    }

}
