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

import static ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures.INT_NO_COMPRESSION;
import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT64;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I64LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_U64LE;

import java.util.Date;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import hdf.hdf5lib.HDFNativeData;

/**
 * Implementation of {@link IHDF5DateTimeWriter}.
 * 
 * @author Bernd Rinn
 */
public class HDF5DateTimeWriter extends HDF5DateTimeReader implements IHDF5DateTimeWriter
{
    private final HDF5BaseWriter baseWriter;

    HDF5DateTimeWriter(HDF5BaseWriter baseWriter, HDF5LongReader longReader)
    {
        super(baseWriter, longReader);

        assert baseWriter != null;

        this.baseWriter = baseWriter;
    }

    @Override
    public void setAttr(final String objectPath, final String name, final long timeStamp)
    {
        assert objectPath != null;
        assert name != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> addAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    if (baseWriter.useSimpleDataSpaceForAttributes)
                    {
                        final long dataSpaceId = baseWriter.h5.createSimpleDataSpace(new long[]
                            { 1 }, registry);
                        baseWriter
                                .setAttribute(
                                        objectPath,
                                        name,
                                        HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                                        H5T_STD_I64LE, H5T_NATIVE_INT64, dataSpaceId, new long[]
                                            { timeStamp }, registry);
                    } else
                    {
                        baseWriter
                                .setAttribute(
                                        objectPath,
                                        name,
                                        HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                                        H5T_STD_I64LE, H5T_NATIVE_INT64, -1, new long[]
                                            { timeStamp }, registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(addAttributeRunnable);
    }

    @Override
    public void setAttr(String objectPath, String name, Date date)
    {
        setAttr(objectPath, name, date.getTime());
    }

    @Override
    public void setArrayAttr(String objectPath, String name, Date[] dates)
    {
        setArrayAttr(objectPath, name, datesToTimeStamps(dates));
    }

    @Override
    public void setArrayAttr(final String objectPath, final String name, final long[] timeStamps)
    {
        assert objectPath != null;
        assert name != null;
        assert timeStamps != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> setAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    if (baseWriter.useSimpleDataSpaceForAttributes)
                    {
                        final long dataSpaceId = baseWriter.h5.createSimpleDataSpace(new long[]
                            { timeStamps.length }, registry);
                        baseWriter
                                .setAttribute(
                                        objectPath,
                                        name,
                                        HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                                        H5T_STD_I64LE, H5T_NATIVE_INT64, dataSpaceId, timeStamps,
                                        registry);
                    } else
                    {
                        final long memoryTypeId =
                                baseWriter.h5.createArrayType(H5T_NATIVE_INT64, timeStamps.length,
                                        registry);
                        final long storageTypeId =
                                baseWriter.h5.createArrayType(H5T_STD_I64LE, timeStamps.length,
                                        registry);
                        baseWriter
                                .setAttribute(
                                        objectPath,
                                        name,
                                        HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                                        storageTypeId, memoryTypeId, -1, timeStamps, registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(setAttributeRunnable);
    }

    @Override
    public void setMDArrayAttr(final String objectPath, final String name, final MDLongArray timeStamps)
    {
        assert objectPath != null;
        assert name != null;
        assert timeStamps != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> addAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    if (baseWriter.useSimpleDataSpaceForAttributes)
                    {
                        final long dataSpaceId =
                                baseWriter.h5.createSimpleDataSpace(timeStamps.longDimensions(),
                                        registry);
                        baseWriter
                                .setAttribute(
                                        objectPath,
                                        name,
                                        HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                                        H5T_STD_I64LE, H5T_NATIVE_INT64, dataSpaceId,
                                        timeStamps.getAsFlatArray(), registry);
                    } else
                    {
                        final long memoryTypeId =
                                baseWriter.h5.createArrayType(H5T_NATIVE_INT64, timeStamps.dimensions(),
                                        registry);
                        final long storageTypeId =
                                baseWriter.h5.createArrayType(H5T_STD_I64LE, timeStamps.dimensions(),
                                        registry);
                        baseWriter
                                .setAttribute(
                                        objectPath,
                                        name,
                                        HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                                        storageTypeId, memoryTypeId, -1, timeStamps.getAsFlatArray(),
                                        registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(addAttributeRunnable);
    }

    @Override
    public void setMDArrayAttr(String objectPath, String name, MDArray<Date> value)
    {
        setMDArrayAttr(objectPath, name, datesToTimeStamps(value));
    }

    @Override
    public void write(final String objectPath, final long timeStamp)
    {
        assert objectPath != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Object> writeScalarRunnable = new ICallableWithCleanUp<Object>()
            {
                @Override
                public Object call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.writeScalar(objectPath, H5T_STD_I64LE, H5T_NATIVE_INT64,
                                    HDFNativeData.longToByte(timeStamp), true, true, registry);
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeScalarRunnable);
    }

    @Override
    public void createArray(String objectPath, int size)
    {
        createArray(objectPath, size, HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public void createArray(final String objectPath, final long size, final int blockSize)
    {
        createArray(objectPath, size, blockSize, HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public void createArray(final String objectPath, final int size,
            final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert size >= 0;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final int longBytes = 8;
                    final long dataSetId;
                    if (features.requiresChunking())
                    {
                        dataSetId =
                                baseWriter.createDataSet(objectPath, H5T_STD_I64LE, features,
                                        new long[]
                                            { 0 }, new long[]
                                            { size }, longBytes, registry);
                    } else
                    {
                        dataSetId =
                                baseWriter.createDataSet(objectPath, H5T_STD_I64LE, features,
                                        new long[]
                                            { size }, null, longBytes, registry);
                    }
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void createArray(final String objectPath, final long length, final int blockSize,
            final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert length >= 0;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final int longBytes = 8;
                    final long dataSetId =
                            baseWriter.createDataSet(objectPath, H5T_STD_I64LE, features,
                                    new long[]
                                        { length }, new long[]
                                        { blockSize }, longBytes, registry);
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeArray(final String objectPath, final long[] timeStamps)
    {
        writeArray(objectPath, timeStamps, HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public void writeArray(final String objectPath, final long[] timeStamps,
            final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert timeStamps != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final int longBytes = 8;
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath, H5T_STD_I64LE, new long[]
                                { timeStamps.length }, longBytes, features, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_INT64, H5S_ALL, H5S_ALL, H5P_DEFAULT, timeStamps);
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeArrayBlock(final String objectPath, final long[] data, final long blockNumber)
    {
        assert objectPath != null;
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] dimensions = new long[]
                        { data.length };
                    final long[] slabStartOrNull = new long[]
                        { data.length * blockNumber };
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, new long[]
                                        { data.length * (blockNumber + 1) }, false, registry);
                    baseWriter.checkIsTimeStamp(objectPath, dataSetId, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, slabStartOrNull, dimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(dimensions, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_INT64, memorySpaceId, dataSpaceId, H5P_DEFAULT,
                            data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeArrayBlockWithOffset(final String objectPath, final long[] data,
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
                    baseWriter.checkIsTimeStamp(objectPath, dataSetId, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, slabStartOrNull, blockDimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(blockDimensions, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_INT64, memorySpaceId, dataSpaceId, H5P_DEFAULT,
                            data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void write(final String objectPath, final Date date)
    {
        write(objectPath, date.getTime());
    }

    @Override
    public void writeArray(final String objectPath, final Date[] dates)
    {
        writeArray(objectPath, datesToTimeStamps(dates));
    }

    @Override
    public void writeArray(final String objectPath, final Date[] dates,
            final HDF5GenericStorageFeatures features)
    {
        writeArray(objectPath, datesToTimeStamps(dates), features);
    }

    @Override
    public void writeMDArray(final String objectPath, final MDLongArray data,
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
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath,
                                    features.isSigned() ? H5T_STD_I64LE : H5T_STD_U64LE,
                                    data.longDimensions(), 8, features, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_INT64, H5S_ALL, H5S_ALL, H5P_DEFAULT,
                            data.getAsFlatArray());
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void createMDArray(final String objectPath, final int[] dimensions)
    {
        createMDArray(objectPath, dimensions, INT_NO_COMPRESSION);
    }

    @Override
    public void createMDArray(final String objectPath, final long[] dimensions,
            final int[] blockDimensions)
    {
        createMDArray(objectPath, dimensions, blockDimensions, INT_NO_COMPRESSION);
    }

    @Override
    public void createMDArray(final String objectPath, final int[] dimensions,
            final HDF5IntStorageFeatures features)
    {
        assert objectPath != null;
        assert dimensions != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long dataSetId;
                    if (features.requiresChunking())
                    {
                        final long[] nullDimensions = new long[dimensions.length];
                        dataSetId =
                                baseWriter.createDataSet(objectPath,
                                        features.isSigned() ? H5T_STD_I64LE : H5T_STD_U64LE,
                                        features, nullDimensions, MDArray.toLong(dimensions), 8,
                                        registry);
                    } else
                    {
                        dataSetId =
                                baseWriter.createDataSet(objectPath,
                                        features.isSigned() ? H5T_STD_I64LE : H5T_STD_U64LE,
                                        features, MDArray.toLong(dimensions), null, 8, registry);
                    }
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void createMDArray(final String objectPath, final long[] dimensions,
            final int[] blockDimensions, final HDF5IntStorageFeatures features)
    {
        assert objectPath != null;
        assert dimensions != null;
        assert blockDimensions != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.createDataSet(objectPath,
                                    features.isSigned() ? H5T_STD_I64LE : H5T_STD_U64LE, features,
                                    dimensions, MDArray.toLong(blockDimensions), 8, registry);
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void writeMDArrayBlock(final String objectPath, final MDLongArray data,
            final long[] blockNumber)
    {
        assert blockNumber != null;

        final long[] dimensions = data.longDimensions();
        final long[] offset = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * dimensions[i];
        }
        writeMDArrayBlockWithOffset(objectPath, data, offset);
    }

    @Override
    public void writeMDArrayBlockWithOffset(final String objectPath, final MDLongArray data,
            final long[] offset)
    {
        assert objectPath != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] dimensions = data.longDimensions();
                    assert dimensions.length == offset.length;
                    final long[] dataSetDimensions = new long[dimensions.length];
                    for (int i = 0; i < offset.length; ++i)
                    {
                        dataSetDimensions[i] = offset[i] + dimensions[i];
                    }
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, dataSetDimensions, false, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, offset, dimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(dimensions, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_INT64, memorySpaceId, dataSpaceId, H5P_DEFAULT,
                            data.getAsFlatArray());
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeMDArrayBlockWithOffset(final String objectPath, final MDLongArray data,
            final int[] blockDimensions, final long[] offset, final int[] memoryOffset)
    {
        assert objectPath != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] memoryDimensions = data.longDimensions();
                    assert memoryDimensions.length == offset.length;
                    final long[] longBlockDimensions = MDArray.toLong(blockDimensions);
                    assert longBlockDimensions.length == offset.length;
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
                    baseWriter.h5.setHyperslabBlock(memorySpaceId, MDArray.toLong(memoryOffset),
                            longBlockDimensions);
                    H5Dwrite(dataSetId, H5T_NATIVE_INT64, memorySpaceId, dataSpaceId, H5P_DEFAULT,
                            data.getAsFlatArray());
                    baseWriter.setTypeVariant(dataSetId,
                            HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeMDArray(final String objectPath, final MDArray<Date> data,
            final HDF5IntStorageFeatures features)
    {
        writeMDArray(objectPath, datesToTimeStamps(data), features);
    }

    @Override
    public void writeMDArrayBlock(final String objectPath, final MDArray<Date> data,
            final long[] blockNumber)
    {
        writeMDArrayBlock(objectPath, datesToTimeStamps(data), blockNumber);
    }

    @Override
    public void writeMDArrayBlockWithOffset(String objectPath, MDArray<Date> data, long[] offset)
    {
        writeMDArrayBlockWithOffset(objectPath, datesToTimeStamps(data), offset);
    }

    @Override
    public void writeMDArrayBlockWithOffset(final String objectPath, final MDArray<Date> data,
            final int[] blockDimensions, final long[] offset, final int[] memoryOffset)
    {
        writeMDArrayBlockWithOffset(objectPath, datesToTimeStamps(data), blockDimensions, offset,
                memoryOffset);
    }

    private static long[] datesToTimeStamps(Date[] dates)
    {
        assert dates != null;

        final long[] timestamps = new long[dates.length];
        for (int i = 0; i < timestamps.length; ++i)
        {
            timestamps[i] = dates[i].getTime();
        }
        return timestamps;
    }

    private static MDLongArray datesToTimeStamps(MDArray<Date> dates)
    {
        assert dates != null;

        final Date[] datesFlat = dates.getAsFlatArray();
        final MDLongArray timestamps = new MDLongArray(dates.dimensions());
        final long[] timestampsFlat = timestamps.getAsFlatArray();
        for (int i = 0; i < timestampsFlat.length; ++i)
        {
            timestampsFlat[i] = datesFlat[i].getTime();
        }
        return timestamps;
    }

}
