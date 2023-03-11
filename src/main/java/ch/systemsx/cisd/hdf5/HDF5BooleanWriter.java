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

import static ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION;
import static ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures.INT_NO_COMPRESSION;
import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_B64;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_UINT64;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_B64LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_U64LE;

import java.util.BitSet;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import hdf.hdf5lib.HDFNativeData;

/**
 * Implementation of {@link IHDF5BooleanWriter}.
 * 
 * @author Bernd Rinn
 */
public class HDF5BooleanWriter extends HDF5BooleanReader implements IHDF5BooleanWriter
{
    private final HDF5BaseWriter baseWriter;

    HDF5BooleanWriter(HDF5BaseWriter baseWriter)
    {
        super(baseWriter);
        assert baseWriter != null;

        this.baseWriter = baseWriter;
    }

    // /////////////////////
    // Attributes
    // /////////////////////

    @Override
    public void setAttr(final String objectPath, final String name, final boolean value)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            final byte byteValue = (byte) (value ? 1 : 0);
                            if (baseWriter.useSimpleDataSpaceForAttributes)
                            {
                                final long dataSpaceId =
                                        baseWriter.h5.createSimpleDataSpace(new long[]
                                            { 1 }, registry);
                                baseWriter.setAttribute(objectPath, name, baseWriter.booleanDataTypeId,
                                        baseWriter.booleanDataTypeId, dataSpaceId, new byte[]
                                            { byteValue }, registry);
                            } else
                            {
                                baseWriter.setAttribute(objectPath, name, baseWriter.booleanDataTypeId,
                                        baseWriter.booleanDataTypeId, -1, new byte[]
                                            { byteValue }, registry);
                            }
                            return null; // Nothing to return.
                        }
                    };
        baseWriter.runner.call(addAttributeRunnable);
    }

    // /////////////////////
    // Data Sets
    // /////////////////////

    @Override
    public void write(final String objectPath, final boolean value)
    {
        baseWriter.checkOpen();
        baseWriter.writeScalar(objectPath, baseWriter.booleanDataTypeId,
                baseWriter.booleanDataTypeId, HDFNativeData.byteToByte((byte) (value ? 1 : 0)));
    }

    @Override
    public void writeBitField(final String objectPath, final BitSet data)
    {
        writeBitField(objectPath, data, HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public void writeBitField(final String objectPath, final BitSet data,
            final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final int longBytes = 8;
                    final int longBits = longBytes * 8;
                    final int msb = data.length();
                    final int realLength = msb / longBits + (msb % longBits != 0 ? 1 : 0);
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath, H5T_STD_B64LE, new long[]
                                { realLength }, longBytes, features, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_B64, H5S_ALL, H5S_ALL, H5P_DEFAULT,
                            BitSetConversionUtils.toStorageForm(data));
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void createBitField(String objectPath, int size)
    {
        createBitField(objectPath, size, GENERIC_NO_COMPRESSION);

    }

    @Override
    public void createBitField(String objectPath, long size, int blockSize)
    {
        createBitField(objectPath, size, blockSize, GENERIC_NO_COMPRESSION);
    }

    @Override
    public void createBitField(final String objectPath, final int size,
            final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert size >= 0;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    if (features.requiresChunking())
                    {
                        baseWriter.createDataSet(objectPath, H5T_STD_B64LE, features, new long[]
                            { 0 }, new long[]
                            { size }, 8, registry);

                    } else
                    {
                        baseWriter.createDataSet(objectPath, H5T_STD_B64LE, features, new long[]
                            { size }, null, 8, registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void createBitField(final String objectPath, final long size, final int blockSize,
            final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert size >= 0;
        assert blockSize >= 0 && (blockSize <= size || size == 0);

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    baseWriter.createDataSet(objectPath, H5T_STD_B64LE, features, new long[]
                        { size }, new long[]
                        { blockSize }, 8, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void writeBitFieldBlock(String objectPath, BitSet data, int dataSize, long blockNumber)
    {
        writeBitFieldBlockWithOffset(objectPath, data, dataSize, dataSize * blockNumber);
    }

    @Override
    public void writeBitFieldBlockWithOffset(final String objectPath, final BitSet data,
            final int dataSize, final long offset)
    {
        assert objectPath != null;
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] blockDimensions = new long[]
                        { dataSize };
                    final long[] slabStartOrNull = new long[]
                        { offset };
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, new long[]
                                        { offset + dataSize }, false, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, slabStartOrNull, blockDimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(blockDimensions, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_B64, memorySpaceId, dataSpaceId, H5P_DEFAULT,
                            BitSetConversionUtils.toStorageForm(data, dataSize));
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeBitFieldArray(final String objectPath, final BitSet[] data)
    {
        writeBitFieldArray(objectPath, data, HDF5IntStorageFeatures.INT_NO_COMPRESSION);
    }

    @Override
    public void writeBitFieldArray(final String objectPath, final BitSet[] data,
            final HDF5IntStorageFeatures features)
    {
        assert objectPath != null;
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final int longBytes = 8;
                    final int longBits = longBytes * 8;
                    final int msb = BitSetConversionUtils.getMaxLength(data);
                    final int numberOfWords = msb / longBits + (msb % longBits != 0 ? 1 : 0);
                    if (features.isScaling() && msb < longBits)
                    {
                        final HDF5IntStorageFeatures actualFeatures =
                                HDF5IntStorageFeatures.build(features).scalingFactor((byte) msb)
                                        .features();
                        final long dataSetId =
                                baseWriter.getOrCreateDataSetId(objectPath, H5T_STD_U64LE,
                                        new long[]
                                            { numberOfWords, data.length }, longBytes,
                                        actualFeatures, registry);
                        H5Dwrite(dataSetId, H5T_NATIVE_UINT64, H5S_ALL, H5S_ALL, H5P_DEFAULT,
                                BitSetConversionUtils.toStorageForm(data, numberOfWords));
                        baseWriter
                                .setTypeVariant(dataSetId, HDF5DataTypeVariant.BITFIELD, registry);
                    } else
                    {
                        final HDF5IntStorageFeatures actualFeatures =
                                HDF5IntStorageFeatures.build(features).noScaling().features();
                        final long dataSetId =
                                baseWriter.getOrCreateDataSetId(objectPath, H5T_STD_B64LE,
                                        new long[]
                                            { numberOfWords, data.length }, longBytes,
                                        actualFeatures, registry);
                        H5Dwrite(dataSetId, H5T_NATIVE_B64, H5S_ALL, H5S_ALL, H5P_DEFAULT,
                                BitSetConversionUtils.toStorageForm(data, numberOfWords));
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void createBitFieldArray(final String objectPath, final int bitFieldSize,
            final long arraySize, final long arrayBlockSize, final HDF5IntStorageFeatures features)
    {
        assert objectPath != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final int longBytes = 8;
                    final int longBits = longBytes * 8;
                    final int numberOfWords =
                            bitFieldSize / longBits + (bitFieldSize % longBits != 0 ? 1 : 0);
                    if (features.requiresChunking() || arraySize > 0)
                    {
                        create(objectPath, new long[]
                            { numberOfWords, arraySize }, new long[]
                            { numberOfWords, arrayBlockSize }, features, registry);
                    } else
                    {
                        create(objectPath, new long[]
                                { numberOfWords, arrayBlockSize }, null, features, registry);
                    }
                    return null; // Nothing to return.
                }

                @SuppressWarnings("hiding")
                void create(final String objectPath, final long[] dimensions,
                        final long[] blockDimensionsOrNull, final HDF5IntStorageFeatures features,
                        ICleanUpRegistry registry)
                {
                    final int longBytes = 8;
                    final int longBits = longBytes * 8;
                    if (features.isScaling() && bitFieldSize < longBits)
                    {
                        final HDF5IntStorageFeatures actualFeatures =
                                HDF5IntStorageFeatures.build(features)
                                        .scalingFactor((byte) bitFieldSize).features();
                        final long dataSetId =
                                baseWriter.createDataSet(objectPath, H5T_STD_U64LE, actualFeatures,
                                        dimensions, blockDimensionsOrNull, longBytes, registry);
                        baseWriter
                                .setTypeVariant(dataSetId, HDF5DataTypeVariant.BITFIELD, registry);
                    } else
                    {
                        final HDF5IntStorageFeatures actualFeatures =
                                HDF5IntStorageFeatures.build(features).noScaling().features();
                        baseWriter.createDataSet(objectPath, H5T_STD_B64LE, actualFeatures,
                                dimensions, blockDimensionsOrNull, longBytes, registry);
                    }
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void createBitFieldArray(String objectPath, int bitFieldSize, long arrayBlockSize,
            HDF5IntStorageFeatures features)
    {
        createBitFieldArray(objectPath, bitFieldSize, 0, arrayBlockSize, features);
    }

    @Override
    public void createBitFieldArray(String objectPath, int bitFieldSize, long arraySize,
            long arrayBlockSize)
    {
        createBitFieldArray(objectPath, bitFieldSize, arraySize, arrayBlockSize, INT_NO_COMPRESSION);
    }

    @Override
    public void createBitFieldArray(String objectPath, int bitFieldSize, long arrayBlockSize)
    {
        createBitFieldArray(objectPath, bitFieldSize, 0, arrayBlockSize, INT_NO_COMPRESSION);
    }

    @Override
    public void writeBitFieldArrayBlockWithOffset(final String objectPath, final BitSet[] data,
            final int dataSize, final long offset)
    {
        assert objectPath != null;
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, new long[]
                                        { -1, offset + dataSize }, false, registry);
                    final long[] dimensions = baseWriter.h5.getDataDimensions(dataSetId, registry);
                    if (dimensions.length != 2)
                    {
                        throw new HDF5JavaException(
                                "Array is supposed to be of rank 2, but is of rank "
                                        + dimensions.length);
                    }
                    final int numberOfWords = dimToInt(dimensions[0]);
                    final long[] blockDimensions = new long[]
                        { numberOfWords, dataSize };
                    final long[] slabStartOrNull = new long[]
                        { 0, offset };
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, slabStartOrNull, blockDimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(blockDimensions, registry);

                    if (baseWriter.isScaledBitField(dataSetId, registry))
                    {
                        H5Dwrite(dataSetId, H5T_NATIVE_UINT64, memorySpaceId, dataSpaceId,
                                H5P_DEFAULT,
                                BitSetConversionUtils.toStorageForm(data, numberOfWords));
                    } else
                    {
                        H5Dwrite(dataSetId, H5T_NATIVE_B64, memorySpaceId, dataSpaceId,
                                H5P_DEFAULT,
                                BitSetConversionUtils.toStorageForm(data, numberOfWords));
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeBitFieldArrayBlockWithOffset(String objectPath, BitSet[] data, long offset)
    {
        writeBitFieldArrayBlockWithOffset(objectPath, data, data.length, offset);
    }

    @Override
    public void writeBitFieldArrayBlock(String objectPath, BitSet[] data, int dataSize,
            long blockNumber)
    {
        writeBitFieldArrayBlockWithOffset(objectPath, data, dataSize, dataSize * blockNumber);
    }

    @Override
    public void writeBitFieldArrayBlock(String objectPath, BitSet[] data, long blockNumber)
    {
        writeBitFieldArrayBlock(objectPath, data, data.length, blockNumber);
    }
}
