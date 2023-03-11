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

import static ch.systemsx.cisd.hdf5.HDF5Utils.OPAQUE_PREFIX;
import static ch.systemsx.cisd.hdf5.HDF5Utils.createDataTypePath;
import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;

import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * Implementation of {@link IHDF5OpaqueWriter}.
 * 
 * @author Bernd Rinn
 */
public class HDF5OpaqueWriter extends HDF5OpaqueReader implements IHDF5OpaqueWriter
{
    private final HDF5BaseWriter baseWriter;

    HDF5OpaqueWriter(HDF5BaseWriter baseWriter)
    {
        super(baseWriter);
        assert baseWriter != null;

        this.baseWriter = baseWriter;
    }

    @Override
    public void writeArray(final String objectPath, final String tag, final byte[] data)
    {
        writeArray(objectPath, tag, data,
                HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public void writeArray(final String objectPath, final String tag, final byte[] data,
            final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert tag != null;
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long dataTypeId = getOrCreateOpaqueTypeId(tag);
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath, dataTypeId, new long[]
                                { data.length }, 1, features, registry);
                    H5Dwrite(dataSetId, dataTypeId, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public HDF5OpaqueType createArray(String objectPath, String tag, int size)
    {
        return createArray(objectPath, tag, size,
                HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public HDF5OpaqueType createArray(final String objectPath, final String tag,
            final long size, final int blockSize)
    {
        return createArray(objectPath, tag, size, blockSize,
                HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION);
    }

    @Override
    public HDF5OpaqueType createArray(final String objectPath, final String tag,
            final int size, final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert tag != null;
        assert size >= 0;

        baseWriter.checkOpen();
        final long dataTypeId = getOrCreateOpaqueTypeId(tag);
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    if (features.requiresChunking())
                    {
                        baseWriter.createDataSet(objectPath, dataTypeId, features, new long[]
                            { 0 }, new long[]
                            { size }, 1, registry);
                    } else
                    {
                        baseWriter.createDataSet(objectPath, dataTypeId, features, new long[]
                            { size }, null, 1, registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
        return new HDF5OpaqueType(baseWriter.fileId, dataTypeId, tag, baseWriter);
    }

    @Override
    public HDF5OpaqueType createArray(final String objectPath, final String tag,
            final long size, final int blockSize, final HDF5GenericStorageFeatures features)
    {
        assert objectPath != null;
        assert tag != null;
        assert size >= 0;
        assert blockSize >= 0 && (blockSize <= size || size == 0);

        baseWriter.checkOpen();
        final long dataTypeId = getOrCreateOpaqueTypeId(tag);
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    baseWriter.createDataSet(objectPath, dataTypeId, features, new long[]
                        { size }, new long[]
                        { blockSize }, 1, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
        return new HDF5OpaqueType(baseWriter.fileId, dataTypeId, tag, baseWriter);
    }

    @Override
    public void writeArrayBlock(final String objectPath, final HDF5OpaqueType dataType,
            final byte[] data, final long blockNumber)
    {
        assert objectPath != null;
        assert dataType != null;
        assert data != null;

        baseWriter.checkOpen();
        dataType.check(baseWriter.fileId);
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] blockDimensions = new long[]
                        { data.length };
                    final long[] slabStartOrNull = new long[]
                        { data.length * blockNumber };
                    final long dataSetId =
                            baseWriter.h5.openAndExtendDataSet(baseWriter.fileId, objectPath,
                                    baseWriter.fileFormat, new long[]
                                        { data.length * (blockNumber + 1) }, false, registry);
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSetId, registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, slabStartOrNull, blockDimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(blockDimensions, registry);
                    H5Dwrite(dataSetId, dataType.getNativeTypeId(), memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeArrayBlockWithOffset(final String objectPath,
            final HDF5OpaqueType dataType, final byte[] data, final int dataSize, final long offset)
    {
        assert objectPath != null;
        assert dataType != null;
        assert data != null;

        baseWriter.checkOpen();
        dataType.check(baseWriter.fileId);
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
                    H5Dwrite(dataSetId, dataType.getNativeTypeId(), memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    private long getOrCreateOpaqueTypeId(final String tag)
    {
        final String dataTypePath =
                createDataTypePath(OPAQUE_PREFIX, baseWriter.houseKeepingNameSuffix, tag);
        long dataTypeId = baseWriter.getDataTypeId(dataTypePath);
        if (dataTypeId < 0)
        {
            dataTypeId = baseWriter.h5.createDataTypeOpaque(1, tag, baseWriter.fileRegistry);
            baseWriter.commitDataType(dataTypePath, dataTypeId);
        }
        return dataTypeId;
    }

}
