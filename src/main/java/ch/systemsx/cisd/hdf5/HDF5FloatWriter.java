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


import static ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures.FLOAT_NO_COMPRESSION;
import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;
import static hdf.hdf5lib.HDF5Constants.H5T_IEEE_F32LE;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * The implementation of {@link IHDF5FloatWriter}.
 * 
 * @author Bernd Rinn
 */
class HDF5FloatWriter extends HDF5FloatReader implements IHDF5FloatWriter
{
    private final HDF5BaseWriter baseWriter;

    HDF5FloatWriter(HDF5BaseWriter baseWriter)
    {
        super(baseWriter);
        assert baseWriter != null;

        this.baseWriter = baseWriter;
    }

    // /////////////////////
    // Attributes
    // /////////////////////

    @Override
    public void setAttr(final String objectPath, final String name, final float value)
    {
        assert objectPath != null;
        assert name != null;

        baseWriter.checkOpen();
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
                                baseWriter.setAttribute(objectPath, name, H5T_IEEE_F32LE,
                                        H5T_NATIVE_FLOAT, dataSpaceId, new float[]
                                            { value }, registry);
                            } else
                            {
                                baseWriter.setAttribute(objectPath, name, H5T_IEEE_F32LE,
                                        H5T_NATIVE_FLOAT, -1, new float[]
                                            { value }, registry);
                            }
                            return null; // Nothing to return.
                        }
                    };
        baseWriter.runner.call(addAttributeRunnable);
    }

    @Override
    public void setArrayAttr(final String objectPath, final String name,
            final float[] value)
    {
        assert objectPath != null;
        assert name != null;
        assert value != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> setAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    if (baseWriter.useSimpleDataSpaceForAttributes)
                    {
                        final long dataSpaceId = baseWriter.h5.createSimpleDataSpace(new long[]
                            { value.length }, registry);
                        baseWriter.setAttribute(objectPath, name, H5T_IEEE_F32LE, H5T_NATIVE_FLOAT,
                                dataSpaceId, value, registry);
                    } else
                    {
                        final long memoryTypeId =
                                baseWriter.h5.createArrayType(H5T_NATIVE_FLOAT, value.length, registry);
                        final long storageTypeId =
                                baseWriter.h5.createArrayType(H5T_IEEE_F32LE, value.length, registry);
                        baseWriter.setAttribute(objectPath, name, storageTypeId, memoryTypeId, -1, value, 
                                registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(setAttributeRunnable);
    }

    @Override
    public void setMDArrayAttr(final String objectPath, final String name,
            final MDFloatArray value)
    {
        assert objectPath != null;
        assert name != null;
        assert value != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> addAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    if (baseWriter.useSimpleDataSpaceForAttributes)
                    {
                        final long dataSpaceId =
                                baseWriter.h5.createSimpleDataSpace(value.longDimensions(), registry);
                        baseWriter.setAttribute(objectPath, name, H5T_IEEE_F32LE, H5T_NATIVE_FLOAT,
                                dataSpaceId, value.getAsFlatArray(), registry);
                    } else
                    {
                        final long memoryTypeId =
                                baseWriter.h5.createArrayType(H5T_NATIVE_FLOAT, value.dimensions(),
                                        registry);
                        final long storageTypeId =
                                baseWriter.h5.createArrayType(H5T_IEEE_F32LE, value.dimensions(),
                                        registry);
                        baseWriter.setAttribute(objectPath, name, storageTypeId, memoryTypeId, -1,
                                value.getAsFlatArray(), registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(addAttributeRunnable);
    }

    @Override
    public void setMatrixAttr(final String objectPath, final String name,
            final float[][] value)
    {
        setMDArrayAttr(objectPath, name, new MDFloatArray(value));
    }
    
    // /////////////////////
    // Data Sets
    // /////////////////////

    @Override
    public void write(final String objectPath, final float value)
    {
        assert objectPath != null;

        baseWriter.checkOpen();
        baseWriter.writeScalar(objectPath, H5T_IEEE_F32LE, H5T_NATIVE_FLOAT, value);
    }

    @Override
    public void writeArray(final String objectPath, final float[] data)
    {
        writeArray(objectPath, data, FLOAT_NO_COMPRESSION);
    }

    @Override
    public void writeArray(final String objectPath, final float[] data,
            final HDF5FloatStorageFeatures features)
    {
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath, 
                                H5T_IEEE_F32LE, new long[]
                                { data.length }, 4, features, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, H5S_ALL, H5S_ALL, H5P_DEFAULT, 
                            data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeArray(final String objectPath, final float[] data,
            final HDF5DataSetTemplate template)
    {
        assert data != null;
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.createDataSetFromTemplate(objectPath,
                                    template, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void createArray(final String objectPath, final int size)
    {
        createArray(objectPath, size, FLOAT_NO_COMPRESSION);
    }

    @Override
    public void createArray(final String objectPath, final long size, final int blockSize)
    {
        createArray(objectPath, size, blockSize, FLOAT_NO_COMPRESSION);
    }

    @Override
    public HDF5DataSet createArrayAndOpen(String objectPath, int size)
    {
        return createArrayAndOpen(objectPath, size, FLOAT_NO_COMPRESSION);
    }

    @Override
    public HDF5DataSet createArrayAndOpen(String objectPath, long size, int blockSize)
    {
        return createArrayAndOpen(objectPath, size, blockSize, FLOAT_NO_COMPRESSION);
    }

    @Override
    public void createArray(final String objectPath, final int size,
            final HDF5FloatStorageFeatures features)
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
                        baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE,
                            features, new long[] { 0 }, new long[] { size }, 4, registry);

                    } else
                    {
                        baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE,
                            features, new long[] { size }, null, 4, registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void createArray(final String objectPath, final HDF5DataSetTemplate template)
    {
        assert objectPath != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    baseWriter.createDataSetFromTemplate(objectPath, template, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }
    
    @Override
    public HDF5DataSet createArrayAndOpen(final String objectPath, final int size,
            final HDF5FloatStorageFeatures features)
    {
        assert objectPath != null;
        assert size >= 0;

        baseWriter.checkOpen();
        if (features.requiresChunking())
        {
            return baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE,
                features, new long[] { 0 }, new long[] { size }, 4);

        } else
        {
            return baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE,
                features, new long[] { size }, null, 4);
        }
    }

    @Override
    public HDF5DataSet createArrayAndOpen(String objectPath, HDF5DataSetTemplate template)
    {
        assert objectPath != null;

        baseWriter.checkOpen();
        final long dataSetId = baseWriter.createDataSetFromTemplate(objectPath, template, null);
        return new HDF5DataSet(baseWriter, objectPath, dataSetId, template.getDataspaceId(), 
        		template.getDimensions(), template.getMaxDimensions(), template.getLayout(), false);
    }
    
    @Override
    public void createArray(final String objectPath, final long size, final int blockSize,
            final HDF5FloatStorageFeatures features)
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
                    baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE, 
                        features, new long[] { size }, new long[]
                        { blockSize }, 4, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public HDF5DataSet createArrayAndOpen(final String objectPath, final long size, final int blockSize,
            final HDF5FloatStorageFeatures features)
    {
        assert objectPath != null;
        assert size >= 0;
        assert blockSize >= 0 && (blockSize <= size || size == 0);

        baseWriter.checkOpen();
        return baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE, 
                features, new long[] { size }, new long[]
                { blockSize }, 4);
    }

    @Override
    public HDF5DataSetTemplate createArrayTemplate(final long size, final int blockSize,
            final HDF5FloatStorageFeatures features)
    {
        assert size >= 0;
        assert blockSize >= 0 && (blockSize <= size || size == 0);

        baseWriter.checkOpen();
        return baseWriter.createDataSetTemplate(
                H5T_IEEE_F32LE, features, new long[]
                    { size }, new long[]
                    { blockSize }, 4);
    }

    @Override
    public void writeArrayBlock(final String objectPath, final float[] data,
            final long blockNumber)
    {
        writeArrayBlockWithOffset(objectPath, data, data.length, data.length * blockNumber);
    }

    @Override
    public void writeArrayBlock(final HDF5DataSet dataSet, final float[] data,
            final long blockNumber)
    {
        writeArrayBlockWithOffset(dataSet, data, data.length, data.length * blockNumber);
    }

    @Override
    public void writeArrayBlockWithOffset(final String objectPath, final float[] data,
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
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, memorySpaceId, dataSpaceId, 
                            H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeArrayBlockWithOffset(final HDF5DataSet dataSet, final float[] data,
            final int dataSize, final long offset)
    {
        assert dataSet != null;
        assert data != null;

        baseWriter.checkOpen();
        baseWriter.h5.checkRank(1, dataSet.getRank());
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] blockDimensions = new long[]
                        { dataSize };
                    final long[] slabStartOrNull = new long[]
                        { offset };
                    final long[] requiredDimensions = new long[]
                        { offset + dataSize };
                    if (baseWriter.h5.extendDataSet(dataSet, requiredDimensions, false, registry))
                    {
                        dataSet.setDimensions(requiredDimensions);
                    }
                    final long dataSpaceId =
                            baseWriter.h5.getDataSpaceForDataSet(dataSet.getDataSetId(), registry);
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, slabStartOrNull, blockDimensions);
                    final long memorySpaceId =
                            baseWriter.h5.createSimpleDataSpace(blockDimensions, registry);
                    H5Dwrite(dataSet.getDataSetId(), H5T_NATIVE_FLOAT, memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    /**
     * Writes out a <code>float</code> matrix (array of rank 2).
     * 
     * @param objectPath The name (including path information) of the data set object in the file.
     * @param data The data to write. Must not be <code>null</code>. All columns need to have the
     *            same length.
     */
    @Override
    public void writeMatrix(final String objectPath, final float[][] data)
    {
        writeMatrix(objectPath, data, FLOAT_NO_COMPRESSION);
    }

    @Override
    public void writeMatrix(final String objectPath, final float[][] data, 
            final HDF5FloatStorageFeatures features)
    {
        assert objectPath != null;
        assert data != null;
        assert HDF5Utils.areMatrixDimensionsConsistent(data);

        writeMDArray(objectPath, new MDFloatArray(data), features);
    }

    @Override
    public void createMatrix(final String objectPath, final int sizeX, 
            final int sizeY)
    {
        assert objectPath != null;
        assert sizeX >= 0;
        assert sizeY >= 0;

        createMDArray(objectPath, new int[] { sizeX, sizeY });
    }

    @Override
    public void createMatrix(final String objectPath, final int sizeX, 
            final int sizeY, final HDF5FloatStorageFeatures features)
    {
        assert objectPath != null;
        assert sizeX >= 0;
        assert sizeY >= 0;

        createMDArray(objectPath, new int[] { sizeX, sizeY }, features);
    }

    @Override
    public void createMatrix(final String objectPath, final long sizeX, final long sizeY,
            final int blockSizeX, final int blockSizeY)
    {
        assert objectPath != null;
        assert sizeX >= 0;
        assert sizeY >= 0;
        assert blockSizeX >= 0 && (blockSizeX <= sizeX || sizeX == 0);
        assert blockSizeY >= 0 && (blockSizeY <= sizeY || sizeY == 0);

        createMDArray(objectPath, new long[] { sizeX, sizeY }, new int[] { blockSizeX, blockSizeY });
    }

    @Override
    public void createMatrix(final String objectPath, final long sizeX, final long sizeY,
            final int blockSizeX, final int blockSizeY, final HDF5FloatStorageFeatures features)
    {
        assert objectPath != null;
        assert sizeX >= 0;
        assert sizeY >= 0;
        assert blockSizeX >= 0 && (blockSizeX <= sizeX || sizeX == 0);
        assert blockSizeY >= 0 && (blockSizeY <= sizeY || sizeY == 0);

        createMDArray(objectPath, new long[] { sizeX, sizeY }, new int[] { blockSizeX, blockSizeY }, features);
    }

    @Override
    public void writeMatrixBlock(final String objectPath, final float[][] data,
            final long blockNumberX, final long blockNumberY)
    {
        assert objectPath != null;
        assert data != null;

        writeMDArrayBlock(objectPath, new MDFloatArray(data), new long[]
            { blockNumberX, blockNumberY });
    }

    @Override
    public void writeMatrixBlockWithOffset(final String objectPath, final float[][] data,
            final long offsetX, final long offsetY)
    {
        assert objectPath != null;
        assert data != null;

        writeMDArrayBlockWithOffset(objectPath, new MDFloatArray(data, new int[]
            { data.length, data[0].length }), new long[]
            { offsetX, offsetY });
    }

    @Override
    public void writeMatrixBlockWithOffset(final String objectPath, final float[][] data,
            final int dataSizeX, final int dataSizeY, final long offsetX, final long offsetY)
    {
        assert objectPath != null;
        assert data != null;

        writeMDArrayBlockWithOffset(objectPath, new MDFloatArray(data, new int[]
            { dataSizeX, dataSizeY }), new long[]
            { offsetX, offsetY });
    }

    @Override
    public void writeMDArray(final String objectPath, final MDFloatArray data)
    {
        writeMDArray(objectPath, data, FLOAT_NO_COMPRESSION);
    }

    @Override
    public void writeMDArraySlice(String objectPath, MDFloatArray data, IndexMap boundIndices)
    {
        baseWriter.checkOpen();

        final int fullRank = baseWriter.getRank(objectPath);
        final int[] fullBlockDimensions = new int[fullRank];
        final long[] fullOffset = new long[fullRank];
        MatrixUtils.createFullBlockDimensionsAndOffset(data.dimensions(), null, boundIndices,
                fullRank, fullBlockDimensions, fullOffset);
        writeMDArrayBlockWithOffset(objectPath, new MDFloatArray(data.getAsFlatArray(),
                fullBlockDimensions), fullOffset);
    }

    @Override
    public void writeMDArraySlice(HDF5DataSet dataSet, MDFloatArray data, IndexMap boundIndices)
    {
        baseWriter.checkOpen();

        final int fullRank = dataSet.getFullRank();
        final int[] fullBlockDimensions = new int[fullRank];
        final long[] fullOffset = new long[fullRank];
        MatrixUtils.createFullBlockDimensionsAndOffset(data.dimensions(), null, boundIndices,
                fullRank, fullBlockDimensions, fullOffset);
        writeMDArrayBlockWithOffset(dataSet, new MDFloatArray(data.getAsFlatArray(),
                fullBlockDimensions), fullOffset);
    }

    @Override
    public void writeMDArraySlice(String objectPath, MDFloatArray data, long[] boundIndices)
    {
        baseWriter.checkOpen();

        final int fullRank = baseWriter.getRank(objectPath);
        final int[] fullBlockDimensions = new int[fullRank];
        final long[] fullOffset = new long[fullRank];
        MatrixUtils.createFullBlockDimensionsAndOffset(data.dimensions(), null, boundIndices,
                fullRank, fullBlockDimensions, fullOffset);
        writeMDArrayBlockWithOffset(objectPath, new MDFloatArray(data.getAsFlatArray(),
                fullBlockDimensions), fullOffset);
    }

    @Override
    public void writeMDArraySlice(HDF5DataSet dataSet, MDFloatArray data, long[] boundIndices)
    {
        baseWriter.checkOpen();

        final int fullRank = dataSet.getFullRank();
        final int[] fullBlockDimensions = new int[fullRank];
        final long[] fullOffset = new long[fullRank];
        MatrixUtils.createFullBlockDimensionsAndOffset(data.dimensions(), null, boundIndices,
                fullRank, fullBlockDimensions, fullOffset);
        writeMDArrayBlockWithOffset(dataSet, new MDFloatArray(data.getAsFlatArray(),
                fullBlockDimensions), fullOffset);
    }

    @Override
    public void writeMDArray(final String objectPath, final MDFloatArray data,
            final HDF5FloatStorageFeatures features)
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
                            baseWriter.getOrCreateDataSetId(objectPath, H5T_IEEE_F32LE, 
                                    data.longDimensions(), 4, features, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, H5S_ALL, H5S_ALL, H5P_DEFAULT, 
                            data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeMDArray(final String objectPath, final MDFloatArray data, final HDF5DataSetTemplate template)
    {
        assert data != null;
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseWriter.createDataSetFromTemplate(objectPath,
                                    template, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, H5S_ALL, H5S_ALL, H5P_DEFAULT, 
                            data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void createMDArray(final String objectPath, final int[] dimensions)
    {
        createMDArray(objectPath, dimensions, FLOAT_NO_COMPRESSION);
    }

    @Override
    public HDF5DataSet createMDArrayAndOpen(final String objectPath, final long[] dimensions,
            final int[] blockDimensions)
    {
        return createMDArrayAndOpen(objectPath, dimensions, blockDimensions, FLOAT_NO_COMPRESSION);
    }

    @Override
    public void createMDArray(final String objectPath, final long[] dimensions,
            final int[] blockDimensions)
    {
        createMDArray(objectPath, dimensions, blockDimensions, FLOAT_NO_COMPRESSION);
    }

    @Override
    public void createMDArray(final String objectPath, final int[] dimensions,
            final HDF5FloatStorageFeatures features)
    {
        assert objectPath != null;
        assert dimensions != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    if (features.requiresChunking())
                    {
                        final long[] nullDimensions = new long[dimensions.length];
                        baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE, 
                                features,
                                nullDimensions, MDArray.toLong(dimensions), 4, registry);
                    } else
                    {
                        baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE, 
                                features, MDArray.toLong(dimensions), null, 4, registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public HDF5DataSet createMDArrayAndOpen(final String objectPath, final int[] dimensions)
    {
        return createMDArrayAndOpen(objectPath, dimensions, FLOAT_NO_COMPRESSION);
    }
    
    @Override
    public HDF5DataSet createMDArrayAndOpen(final String objectPath, final int[] dimensions,
            final HDF5FloatStorageFeatures features)
    {
        assert objectPath != null;
        assert dimensions != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<HDF5DataSet> createRunnable = new ICallableWithCleanUp<HDF5DataSet>()
            {
                @Override
                public HDF5DataSet call(ICleanUpRegistry registry)
                {
                    if (features.requiresChunking())
                    {
                        final long[] nullDimensions = new long[dimensions.length];
                        return baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE, 
                                features, nullDimensions, MDArray.toLong(dimensions), 4);
                    } else
                    {
                        return baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE, 
                                features, MDArray.toLong(dimensions), null, 4);
                    }
                }
            };
        return baseWriter.runner.call(createRunnable);
    }

    @Override
    public void createMDArray(final String objectPath, final long[] dimensions,
            final int[] blockDimensions, final HDF5FloatStorageFeatures features)
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
                    baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE, 
                            features, dimensions, 
                            MDArray.toLong(blockDimensions), 4, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void createMDArray(final String objectPath, final HDF5DataSetTemplate template)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    baseWriter.createDataSetFromTemplate(objectPath, template, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public HDF5DataSet createMDArrayAndOpen(final String objectPath, final long[] dimensions,
            final int[] blockDimensions, final HDF5FloatStorageFeatures features)
    {
        assert objectPath != null;
        assert dimensions != null;
        assert blockDimensions != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<HDF5DataSet> createRunnable = new ICallableWithCleanUp<HDF5DataSet>()
            {
                @Override
                public HDF5DataSet call(ICleanUpRegistry registry)
                {
                    return baseWriter.createDataSet(objectPath, H5T_IEEE_F32LE, 
                            features, dimensions, MDArray.toLong(blockDimensions), 4);
                }
            };
        return baseWriter.runner.call(createRunnable);
    }

    @Override
    public HDF5DataSet createMDArrayAndOpen(final String objectPath, final HDF5DataSetTemplate template)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<HDF5DataSet> createRunnable = new ICallableWithCleanUp<HDF5DataSet>()
            {
                @Override
                public HDF5DataSet call(ICleanUpRegistry registry)
                {
                    final long dataSetId = baseWriter.createDataSetFromTemplate(objectPath, template, null);
                    return new HDF5DataSet(baseWriter, objectPath, dataSetId, template.getDataspaceId(), 
                            template.getDimensions(), template.getMaxDimensions(), template.getLayout(), false);
                }
            };
        return baseWriter.runner.call(createRunnable);
    }

    @Override
    public HDF5DataSetTemplate createMDArrayTemplate(long[] dimensions, int[] blockDimensions, 
            HDF5FloatStorageFeatures features)
    {
        assert dimensions != null;
        assert blockDimensions != null;;

        baseWriter.checkOpen();
        return baseWriter.createDataSetTemplate(
                H5T_IEEE_F32LE, features, dimensions, MDArray.toLong(blockDimensions), 4);
    }

    @Override
    public void writeMDArrayBlock(String objectPath, MDFloatArray data,
            long[] blockNumber)
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
    public void writeSlicedMDArrayBlock(String objectPath, MDFloatArray data,
            long[] blockNumber, IndexMap boundIndices)
    {
        assert blockNumber != null;

        final long[] dimensions = data.longDimensions();
        final long[] offset = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * dimensions[i];
        }
        writeSlicedMDArrayBlockWithOffset(objectPath, data, offset, boundIndices);
    }
    
    @Override
    public void writeSlicedMDArrayBlock(HDF5DataSet dataSet, MDFloatArray data,
            long[] blockNumber, IndexMap boundIndices)
    {
        assert blockNumber != null;

        final long[] dimensions = data.longDimensions();
        final long[] offset = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * dimensions[i];
        }
        writeSlicedMDArrayBlockWithOffset(dataSet, data, offset, boundIndices);
    }
    
    @Override
    public void writeSlicedMDArrayBlock(String objectPath, MDFloatArray data,
    		long[] blockNumber, long[] boundIndices)
    {
        assert blockNumber != null;

        final long[] dimensions = data.longDimensions();
        final long[] offset = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * dimensions[i];
        }
        writeSlicedMDArrayBlockWithOffset(objectPath, data, offset, boundIndices);
    }

    @Override
    public void writeSlicedMDArrayBlock(HDF5DataSet dataSet, MDFloatArray data, 
            long[] blockNumber, long[] boundIndices)
    {
        assert blockNumber != null;

        final long[] dimensions = data.longDimensions();
        final long[] offset = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * dimensions[i];
        }
        writeSlicedMDArrayBlockWithOffset(dataSet, data, offset, boundIndices);
    }

    @Override
    public void writeMDArray(HDF5DataSet dataSet, MDFloatArray data, HDF5ArrayBlockParams params)
    {
        assert dataSet != null;
        assert data != null;
        assert params != null;
        
        if (params.hasBlock())
        {
            if (params.hasSlice())
            {
                if (params.getBoundIndexArray() != null)
                {
                    writeSlicedMDArrayBlockWithOffset(dataSet, data, params.getOffset(data.dimensions()), 
                            params.getBoundIndexArray());
                } else
                {
                    writeSlicedMDArrayBlockWithOffset(dataSet, data, params.getOffset(data.dimensions()), 
                            params.getBoundIndexMap());
                }
                return;
            }

            writeMDArrayBlockWithOffset(dataSet, data, params.getOffset(data.dimensions()));
            return;
        }

        if (params.hasSlice())
        {
            if (params.getBoundIndexArray() != null)
            {
                writeMDArraySlice(dataSet, data, params.getBoundIndexArray());
            } else
            {
                writeMDArraySlice(dataSet, data, params.getBoundIndexMap());
            }
            return;
        }

        writeMDArrayBlockWithOffset(dataSet, data, new long[data.rank()]);
    }

    @Override
    public void writeMDArrayBlockWithOffset(final String objectPath, final MDFloatArray data,
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
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, memorySpaceId, dataSpaceId, 
                            H5P_DEFAULT, data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    public void writeMDArrayBlock(final HDF5DataSet dataSet, final MDFloatArray data,
            final long[] blockNumber)
    {
        assert blockNumber != null;

        final long[] dimensions = data.longDimensions();
        final long[] offset = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * dimensions[i];
        }
        writeMDArrayBlockWithOffset(dataSet, data, offset);
    }
    
    public void writeMDArrayBlockWithOffset(final HDF5DataSet dataSet, final MDFloatArray data,
            final long[] offset)
    {
        assert dataSet != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        baseWriter.h5.checkRank(data.rank(), offset.length);
        baseWriter.h5.checkRank(data.rank(), dataSet.getRank());
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
                    dataSet.extend(dataSetDimensions);
                    final long dataSpaceId = dataSet.getDataSpaceId(); 
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, offset, dimensions);
                    final long memorySpaceId = dataSet.getMemorySpaceId(dimensions); 
                    H5Dwrite(dataSet.getDataSetId(), H5T_NATIVE_FLOAT, memorySpaceId, dataSpaceId, 
                            H5P_DEFAULT, data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeSlicedMDArrayBlockWithOffset(String objectPath, MDFloatArray data,
            long[] offset, IndexMap boundIndices)
    {
        assert objectPath != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        final int fullRank = baseWriter.getRank(objectPath);
        final int[] fullBlockDimensions = new int[fullRank];
        final long[] fullOffset = new long[fullRank];
        MatrixUtils.createFullBlockDimensionsAndOffset(data.dimensions(), offset, boundIndices,
                fullRank, fullBlockDimensions, fullOffset);
        writeMDArrayBlockWithOffset(objectPath, new MDFloatArray(data.getAsFlatArray(),
                fullBlockDimensions), fullOffset);
    }

    @Override
    public void writeSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, MDFloatArray data,
            long[] offset, IndexMap boundIndices)
    {
        assert dataSet != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        final int fullRank = dataSet.getFullRank();
        final int[] fullBlockDimensions = new int[fullRank];
        final long[] fullOffset = new long[fullRank];
        MatrixUtils.createFullBlockDimensionsAndOffset(data.dimensions(), offset, boundIndices,
                fullRank, fullBlockDimensions, fullOffset);
        writeMDArrayBlockWithOffset(dataSet, new MDFloatArray(data.getAsFlatArray(),
                fullBlockDimensions), fullOffset);
    }

    @Override
    public void writeSlicedMDArrayBlockWithOffset(String objectPath, MDFloatArray data,
            long[] offset, long[] boundIndices)
    {
        assert objectPath != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        final int fullRank = baseWriter.getRank(objectPath);
        final int[] fullBlockDimensions = new int[fullRank];
        final long[] fullOffset = new long[fullRank];
        MatrixUtils.createFullBlockDimensionsAndOffset(data.dimensions(), offset, boundIndices,
                fullRank, fullBlockDimensions, fullOffset);
        writeMDArrayBlockWithOffset(objectPath, new MDFloatArray(data.getAsFlatArray(),
                fullBlockDimensions), fullOffset);
    }

    @Override
    public void writeSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, MDFloatArray data,
            long[] offset, long[] boundIndices)
    {
        assert dataSet != null;
        assert data != null;
        assert offset != null;

        baseWriter.checkOpen();
        final int fullRank = dataSet.getFullRank();
        final int[] fullBlockDimensions = new int[fullRank];
        final long[] fullOffset = new long[fullRank];
        MatrixUtils.createFullBlockDimensionsAndOffset(data.dimensions(), offset, boundIndices,
                fullRank, fullBlockDimensions, fullOffset);
        writeMDArrayBlockWithOffset(dataSet, new MDFloatArray(data.getAsFlatArray(),
                fullBlockDimensions), fullOffset);
    }

    @Override
    public void writeMDArrayBlockWithOffset(final String objectPath, final MDFloatArray data,
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
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeMDArrayBlockWithOffset(final HDF5DataSet dataSet, final MDFloatArray data,
            final int[] blockDimensions, final long[] offset, final int[] memoryOffset)
    {
        assert dataSet != null;
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
                    final long dataSetId = dataSet.getDataSetId();
                    final long dataSpaceId = dataSet.getDataSpaceId(); 
                    baseWriter.h5.setHyperslabBlock(dataSpaceId, offset, longBlockDimensions);
                    final long memorySpaceId = dataSet.getMemorySpaceId(memoryDimensions);
                    baseWriter.h5.setHyperslabBlock(memorySpaceId, MDArray.toLong(memoryOffset),
                            longBlockDimensions);
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, memorySpaceId, dataSpaceId,
                            H5P_DEFAULT, data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }
}
