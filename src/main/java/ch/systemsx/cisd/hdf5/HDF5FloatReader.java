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

import static ch.systemsx.cisd.hdf5.MatrixUtils.cardinalityBoundIndices;
import static ch.systemsx.cisd.hdf5.MatrixUtils.checkBoundIndices;
import static ch.systemsx.cisd.hdf5.MatrixUtils.createFullBlockDimensionsAndOffset;
import static hdf.hdf5lib.HDF5Constants.H5T_ARRAY;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;

import java.util.Arrays;
import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import ch.systemsx.cisd.hdf5.exceptions.HDF5SpaceRankMismatch;
import hdf.hdf5lib.HDF5Constants;

/**
 * The implementation of {@link IHDF5FloatReader}.
 * 
 * @author Bernd Rinn
 */
class HDF5FloatReader implements IHDF5FloatReader
{
    private final HDF5BaseReader baseReader;

    HDF5FloatReader(HDF5BaseReader baseReader)
    {
        assert baseReader != null;

        this.baseReader = baseReader;
    }

    // For Unit tests only.
    HDF5BaseReader getBaseReader()
    {
        return baseReader;
    }

    // /////////////////////
    // Attributes
    // /////////////////////

    @Override
    public float getAttr(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<Float> getAttributeRunnable = new ICallableWithCleanUp<Float>()
            {
                @Override
                public Float call(ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    final long attributeId =
                            baseReader.h5.openAttribute(objectId, attributeName, registry);
                    final float[] data =
                            baseReader.h5.readAttributeAsFloatArray(attributeId, H5T_NATIVE_FLOAT, 1);
                    return data[0];
                }
            };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public float[] getArrayAttr(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<float[]> getAttributeRunnable =
                new ICallableWithCleanUp<float[]>()
                    {
                        @Override
                        public float[] call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            return getFloatArrayAttribute(objectId, attributeName, registry);
                        }
                    };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public MDFloatArray getMDArrayAttr(final String objectPath,
            final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDFloatArray> getAttributeRunnable =
                new ICallableWithCleanUp<MDFloatArray>()
                    {
                        @Override
                        public MDFloatArray call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            return getFloatMDArrayAttribute(objectId, attributeName, registry);
                        }
                    };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public float[][] getMatrixAttr(final String objectPath, final String attributeName)
            throws HDF5JavaException
    {
        final MDFloatArray array = getMDArrayAttr(objectPath, attributeName);
        if (array.rank() != 2)
        {
            throw new HDF5JavaException("Array is supposed to be of rank 2, but is of rank "
                    + array.rank());
        }
        return array.toMatrix();
    }

    // /////////////////////
    // Data Sets
    // /////////////////////

    @Override
    public float read(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<Float> readCallable = new ICallableWithCleanUp<Float>()
            {
                @Override
                public Float call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final float[] data = new float[1];
                    baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_FLOAT, data);
                    return data[0];
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public float[] readArray(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<float[]> readCallable = new ICallableWithCleanUp<float[]>()
            {
                @Override
                public float[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    return readFloatArray(dataSetId, registry);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    private float[] readFloatArray(long dataSetId, ICleanUpRegistry registry)
    {
        try
        {
            final DataSpaceParameters spaceParams =
                    baseReader.getSpaceParameters(dataSetId, registry);
            final float[] data = new float[spaceParams.blockSize];
            baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_FLOAT, spaceParams.memorySpaceId,
                    spaceParams.dataSpaceId, data);
            return data;
        } catch (HDF5LibraryException ex)
        {
            if (ex.getMajorErrorNumber() == HDF5Constants.H5E_DATATYPE
                    && ex.getMinorErrorNumber() == HDF5Constants.H5E_CANTINIT)
            {
                // Check whether it is an array data type.
                final long dataTypeId = baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                if (baseReader.h5.getClassType(dataTypeId) == HDF5Constants.H5T_ARRAY)
                {
                    return readFloatArrayFromArrayType(dataSetId, dataTypeId, registry);
                }
            }
            throw ex;
        }
    }

    private float[] readFloatArrayFromArrayType(long dataSetId, final long dataTypeId,
            ICleanUpRegistry registry)
    {
        final long spaceId = baseReader.h5.createScalarDataSpace();
        final int[] dimensions = baseReader.h5.getArrayDimensions(dataTypeId);
        final float[] data = new float[HDF5Utils.getOneDimensionalArraySize(dimensions)];
        final long memoryDataTypeId =
                baseReader.h5.createArrayType(H5T_NATIVE_FLOAT, data.length, registry);
        baseReader.h5.readDataSet(dataSetId, memoryDataTypeId, spaceId, spaceId, data);
        return data;
    }

    @Override
    public int[] readToMDArrayWithOffset(final String objectPath, final MDFloatArray array,
            final int[] memoryOffset)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<int[]> readCallable = new ICallableWithCleanUp<int[]>()
            {
                @Override
                public int[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getBlockSpaceParameters(dataSetId, memoryOffset, array
                                    .dimensions(), registry);
                    final long nativeDataTypeId =
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_FLOAT, registry);
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, 
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, array.
                            getAsFlatArray());
                    return MDArray.toInt(spaceParams.dimensions);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public int[] readToMDArrayWithOffset(final HDF5DataSet dataSet, final MDFloatArray array,
            final int[] memoryOffset)
    {
        assert dataSet != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<int[]> readCallable = new ICallableWithCleanUp<int[]>()
            {
                @Override
                public int[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId = dataSet.getDataSetId();
                    final DataSpaceParameters spaceParams =
                            baseReader.getBlockSpaceParameters(dataSet, memoryOffset, 
                                        array.dimensions());
                    final long nativeDataTypeId =
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_FLOAT, registry);
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, 
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, array.
                            getAsFlatArray());
                    return MDArray.toInt(spaceParams.dimensions);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public int[] readToMDArrayBlockWithOffset(final String objectPath,
            final MDFloatArray array, final int[] blockDimensions, final long[] offset,
            final int[] memoryOffset)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<int[]> readCallable = new ICallableWithCleanUp<int[]>()
            {
                @Override
                public int[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getBlockSpaceParameters(dataSetId, memoryOffset, array
                                    .dimensions(), offset, blockDimensions, registry);
                    final long nativeDataTypeId =
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_FLOAT, registry);
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, 
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, array
                            .getAsFlatArray());
                    return MDArray.toInt(spaceParams.dimensions);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public int[] readToMDArrayBlockWithOffset(final HDF5DataSet dataSet,
            final MDFloatArray array, final int[] blockDimensions, final long[] offset,
            final int[] memoryOffset)
    {
        assert dataSet != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<int[]> readCallable = new ICallableWithCleanUp<int[]>()
            {
                @Override
                public int[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId = dataSet.getDataSetId();
                    final DataSpaceParameters spaceParams =
                            baseReader.getBlockSpaceParameters(dataSet, memoryOffset, array
                                    .dimensions(), offset, blockDimensions);
                    final long nativeDataTypeId =
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_FLOAT, registry);
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, 
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, array
                            .getAsFlatArray());
                    return MDArray.toInt(spaceParams.dimensions);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public float[] readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber)
    {
        return readArrayBlockWithOffset(objectPath, blockSize, blockNumber * blockSize);
    }

    @Override
    public float[] readArrayBlock(HDF5DataSet dataSet, int blockSize, long blockNumber)
    {
        return readArrayBlockWithOffset(dataSet, blockSize, blockNumber * blockSize);
    }

    @Override
    public float[] readArrayBlockWithOffset(final String objectPath, final int blockSize,
            final long offset)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<float[]> readCallable = new ICallableWithCleanUp<float[]>()
            {
                @Override
                public float[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, offset, blockSize, registry);
                    final float[] data = new float[spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_FLOAT, spaceParams.memorySpaceId,
                            spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public float[] readArrayBlockWithOffset(final HDF5DataSet dataSet, final int blockSize, final long offset)
    {
        assert dataSet != null;

        baseReader.checkOpen();
        baseReader.h5.checkRank(1, dataSet.getRank());
        final ICallableWithCleanUp<float[]> readCallable = new ICallableWithCleanUp<float[]>()
            {
                @Override
                public float[] call(ICleanUpRegistry registry)
                {
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSet, offset, blockSize);
                    final float[] data = new float[spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSet.getDataSetId(), H5T_NATIVE_FLOAT, spaceParams.memorySpaceId,
                            spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public float[][] readMatrix(final String objectPath) throws HDF5JavaException
    {
        final MDFloatArray array = readMDArray(objectPath);
        if (array.rank() != 2)
        {
            throw new HDF5JavaException("Array is supposed to be of rank 2, but is of rank "
                    + array.rank());
        }
        return array.toMatrix();
    }

    @Override
    public float[][] readMatrixBlock(final String objectPath, final int blockSizeX,
            final int blockSizeY, final long blockNumberX, final long blockNumberY) 
            throws HDF5JavaException
    {
        final MDFloatArray array = readMDArrayBlock(objectPath, new int[]
            { blockSizeX, blockSizeY }, new long[]
            { blockNumberX, blockNumberY });
        if (array.rank() != 2)
        {
            throw new HDF5JavaException("Array is supposed to be of rank 2, but is of rank "
                    + array.rank());
        }
        return array.toMatrix();
    }

    @Override
    public float[][] readMatrixBlockWithOffset(final String objectPath, final int blockSizeX,
            final int blockSizeY, final long offsetX, final long offsetY) throws HDF5JavaException
    {
        final MDFloatArray array = readMDArrayBlockWithOffset(objectPath, new int[]
            { blockSizeX, blockSizeY }, new long[]
            { offsetX, offsetY });
        if (array.rank() != 2)
        {
            throw new HDF5JavaException("Array is supposed to be of rank 2, but is of rank "
                    + array.rank());
        }
        return array.toMatrix();
    }

    @Override
    public MDFloatArray readMDArraySlice(String objectPath, IndexMap boundIndices)
    {
        baseReader.checkOpen();
        final long[] fullDimensions = baseReader.getDimensions(objectPath);
        final int[] fullBlockDimensions = new int[fullDimensions.length];
        final long[] fullOffset = new long[fullDimensions.length];
        final int cardBoundIndices = cardinalityBoundIndices(boundIndices);
        checkBoundIndices(objectPath, fullDimensions, cardBoundIndices);
        final int[] effectiveBlockDimensions = new int[fullBlockDimensions.length - cardBoundIndices];
        Arrays.fill(effectiveBlockDimensions, -1);
        createFullBlockDimensionsAndOffset(effectiveBlockDimensions, null, boundIndices, fullDimensions,
                fullBlockDimensions, fullOffset);
        final MDFloatArray result = readMDArrayBlockWithOffset(objectPath, fullBlockDimensions, fullOffset);
        if (fullBlockDimensions.length == cardBoundIndices) // no free indices
        {
	        return new MDFloatArray(result.getAsFlatArray(), new int[] { 1 });
	    } else
	    {
	        return new MDFloatArray(result.getAsFlatArray(), effectiveBlockDimensions);
	    }
    }

    @Override
    public MDFloatArray readMDArraySlice(HDF5DataSet dataSet, IndexMap boundIndices)
    {
        baseReader.checkOpen();
        final long[] fullDimensions = dataSet.getDimensions();
        final int[] fullBlockDimensions = new int[fullDimensions.length];
        final long[] fullOffset = new long[fullDimensions.length];
        final int cardBoundIndices = cardinalityBoundIndices(boundIndices);
        checkBoundIndices(dataSet.getDataSetPath(), fullDimensions, cardBoundIndices);
        final int[] effectiveBlockDimensions = new int[fullBlockDimensions.length - cardBoundIndices];
        Arrays.fill(effectiveBlockDimensions, -1);
        createFullBlockDimensionsAndOffset(effectiveBlockDimensions, null, boundIndices, fullDimensions,
                fullBlockDimensions, fullOffset);
        final MDFloatArray result = readMDArrayBlockWithOffset(dataSet, fullBlockDimensions, fullOffset);
        if (fullBlockDimensions.length == cardBoundIndices) // no free indices
        {
            return new MDFloatArray(result.getAsFlatArray(), new int[] { 1 });
        } else
        {
            return new MDFloatArray(result.getAsFlatArray(), effectiveBlockDimensions);
        }
    }
    
    @Override
    public MDFloatArray readMDArraySlice(String objectPath, long[] boundIndices)
    {
        baseReader.checkOpen();
        final long[] fullDimensions = baseReader.getDimensions(objectPath);
        final int[] fullBlockDimensions = new int[fullDimensions.length];
        final long[] fullOffset = new long[fullDimensions.length];
        final int cardBoundIndices = cardinalityBoundIndices(boundIndices);
        checkBoundIndices(objectPath, fullDimensions, boundIndices);
        final int[] effectiveBlockDimensions = new int[fullBlockDimensions.length - cardBoundIndices];
        Arrays.fill(effectiveBlockDimensions, -1);
        createFullBlockDimensionsAndOffset(effectiveBlockDimensions, null, boundIndices, fullDimensions,
                fullBlockDimensions, fullOffset);
        final MDFloatArray result = readMDArrayBlockWithOffset(objectPath, fullBlockDimensions, fullOffset);
        if (fullBlockDimensions.length == cardBoundIndices) // no free indices
        {
	        return new MDFloatArray(result.getAsFlatArray(), new int[] { 1 });
	    } else
	    {
	        return new MDFloatArray(result.getAsFlatArray(), effectiveBlockDimensions);
	    }
    }

    @Override
    public MDFloatArray readMDArraySlice(final HDF5DataSet dataSet, final long[] boundIndices)
    {
        baseReader.checkOpen();
        final long[] fullDimensions = dataSet.getDimensions();
        final int[] fullBlockDimensions = new int[fullDimensions.length];
        final long[] fullOffset = new long[fullDimensions.length];
        final int cardBoundIndices = cardinalityBoundIndices(boundIndices);
        checkBoundIndices(dataSet.getDataSetPath(), fullDimensions, boundIndices);
        final int[] effectiveBlockDimensions = new int[fullBlockDimensions.length - cardBoundIndices];
        Arrays.fill(effectiveBlockDimensions, -1);
        createFullBlockDimensionsAndOffset(effectiveBlockDimensions, null, boundIndices, fullDimensions,
                fullBlockDimensions, fullOffset);
        final MDFloatArray result = readMDArrayBlockWithOffset(dataSet, fullBlockDimensions, fullOffset);
        if (fullBlockDimensions.length == cardBoundIndices) // no free indices
        {
            return new MDFloatArray(result.getAsFlatArray(), new int[] { 1 });
        } else
        {
            return new MDFloatArray(result.getAsFlatArray(), effectiveBlockDimensions);
        }
    }

    @Override
    public MDFloatArray readMDArray(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDFloatArray> readCallable = new ICallableWithCleanUp<MDFloatArray>()
            {
                @Override
                public MDFloatArray call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    return readFloatMDArray(dataSetId, registry);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    MDFloatArray readMDArray(final HDF5DataSet dataSet)
    {
        assert dataSet != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDFloatArray> readCallable = new ICallableWithCleanUp<MDFloatArray>()
            {
                @Override
                public MDFloatArray call(ICleanUpRegistry registry)
                {
                    return readFloatMDArray(dataSet.getDataSetId(), registry);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    MDFloatArray readFloatMDArray(long dataSetId, ICleanUpRegistry registry)
    {
        try
        {
            final DataSpaceParameters spaceParams =
                    baseReader.getSpaceParameters(dataSetId, registry);
            final float[] data = new float[spaceParams.blockSize];
            baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_FLOAT, spaceParams.memorySpaceId,
                    spaceParams.dataSpaceId, data);
            return new MDFloatArray(data, spaceParams.dimensions);
        } catch (HDF5LibraryException ex)
        {
            if (ex.getMajorErrorNumber() == HDF5Constants.H5E_DATATYPE
                    && ex.getMinorErrorNumber() == HDF5Constants.H5E_CANTINIT)
            {
                // Check whether it is an array data type.
                final long dataTypeId = baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                if (baseReader.h5.getClassType(dataTypeId) == HDF5Constants.H5T_ARRAY)
                {
                    return readFloatMDArrayFromArrayType(dataSetId, dataTypeId, registry);
                }
            }
            throw ex;
        }
    }

    private MDFloatArray readFloatMDArrayFromArrayType(long dataSetId, final long dataTypeId,
            ICleanUpRegistry registry)
    {
        final int[] arrayDimensions = baseReader.h5.getArrayDimensions(dataTypeId);
        final long memoryDataTypeId =
                baseReader.h5.createArrayType(H5T_NATIVE_FLOAT, arrayDimensions, registry);
        final DataSpaceParameters spaceParams = baseReader.getSpaceParameters(dataSetId, registry);
        if (spaceParams.blockSize == 0)
        {
            final long spaceId = baseReader.h5.createScalarDataSpace();
            final float[] data = new float[MDArray.getLength(arrayDimensions)];
            baseReader.h5.readDataSet(dataSetId, memoryDataTypeId, spaceId, spaceId, data);
            return new MDFloatArray(data, arrayDimensions);
        } else
        {
            final float[] data =
                    new float[MDArray.getLength(arrayDimensions) * spaceParams.blockSize];
            baseReader.h5.readDataSet(dataSetId, memoryDataTypeId, spaceParams.memorySpaceId,
                    spaceParams.dataSpaceId, data);
            return new MDFloatArray(data, MatrixUtils.concat(MDArray.toInt(spaceParams.dimensions),
                    arrayDimensions));
        }
    }

    @Override
    public MDFloatArray readMDArray(String objectPath, HDF5ArrayBlockParams params)
    {
        baseReader.checkOpen();
        try (final HDF5DataSet dataSet = baseReader.openDataSet(objectPath))
        {
            return readMDArray(dataSet, params);
        }
    }

    @Override
    public MDFloatArray readMDArray(HDF5DataSet dataSet, HDF5ArrayBlockParams params)
    {
        if (params.hasBlock())
        {
            if (params.hasSlice())
            {
                if (params.getBoundIndexArray() != null)
                {
                    return readSlicedMDArrayBlockWithOffset(dataSet, params.getBlockDimensions(), params.getOffset(), params.getBoundIndexArray());
                } else
                {
                    return readSlicedMDArrayBlockWithOffset(dataSet, params.getBlockDimensions(), params.getOffset(), params.getBoundIndexMap());
                }
            }

            return readMDArrayBlockWithOffset(dataSet, params.getBlockDimensions(), params.getOffset());
        }

        if (params.hasSlice())
        {
            if (params.getBoundIndexArray() != null)
            {
                return readMDArraySlice(dataSet, params.getBoundIndexArray());
            } else
            {
                return readMDArraySlice(dataSet, params.getBoundIndexMap());
            }
        }

        return readMDArray(dataSet);
    }

    @Override
    public MDFloatArray readSlicedMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber, IndexMap boundIndices)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readSlicedMDArrayBlockWithOffset(objectPath, blockDimensions, offset, boundIndices);
    }

    @Override
    public MDFloatArray readSlicedMDArrayBlock(HDF5DataSet dataSet, int[] blockDimensions,
            long[] blockNumber, IndexMap boundIndices)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readSlicedMDArrayBlockWithOffset(dataSet, blockDimensions, offset, boundIndices);
    }

    @Override
    public MDFloatArray readSlicedMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber, long[] boundIndices)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readSlicedMDArrayBlockWithOffset(objectPath, blockDimensions, offset, boundIndices);
    }

    @Override
    public MDFloatArray readSlicedMDArrayBlock(HDF5DataSet dataSet, int[] blockDimensions,
            long[] blockNumber, long[] boundIndices)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readSlicedMDArrayBlockWithOffset(dataSet, blockDimensions, offset, boundIndices);
    }

    @Override
    public MDFloatArray readMDArrayBlock(final String objectPath, final int[] blockDimensions,
            final long[] blockNumber)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readMDArrayBlockWithOffset(objectPath, blockDimensions, offset);
    }

    @Override
    public MDFloatArray readSlicedMDArrayBlockWithOffset(String objectPath, int[] blockDimensions,
            long[] offset, IndexMap boundIndices)
    {
        baseReader.checkOpen();
        final int[] effectiveBlockDimensions = blockDimensions.clone();
        final long[] fullDimensions = baseReader.getDimensions(objectPath);
        final int[] fullBlockDimensions = new int[fullDimensions.length];
        final long[] fullOffset = new long[fullDimensions.length];
        checkBoundIndices(objectPath, fullDimensions, blockDimensions,
                cardinalityBoundIndices(boundIndices));
        createFullBlockDimensionsAndOffset(effectiveBlockDimensions, offset, boundIndices, fullDimensions,
                fullBlockDimensions, fullOffset);
        final MDFloatArray result = readMDArrayBlockWithOffset(objectPath, fullBlockDimensions, fullOffset);
        return new MDFloatArray(result.getAsFlatArray(), effectiveBlockDimensions);
    }

    @Override
    public MDFloatArray readSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, int[] blockDimensions,
            long[] offset, IndexMap boundIndices)
    {
        baseReader.checkOpen();
        final int[] effectiveBlockDimensions = blockDimensions.clone();
        final long[] fullDimensions = dataSet.getDimensions();
        final int[] fullBlockDimensions = new int[fullDimensions.length];
        final long[] fullOffset = new long[fullDimensions.length];
        checkBoundIndices(dataSet.getDataSetPath(), fullDimensions, blockDimensions,
                cardinalityBoundIndices(boundIndices));
        createFullBlockDimensionsAndOffset(effectiveBlockDimensions, offset, boundIndices, fullDimensions,
                fullBlockDimensions, fullOffset);
        final MDFloatArray result = readMDArrayBlockWithOffset(dataSet, fullBlockDimensions, fullOffset);
        return new MDFloatArray(result.getAsFlatArray(), effectiveBlockDimensions);
    }

    @Override
    public MDFloatArray readSlicedMDArrayBlockWithOffset(String objectPath, int[] blockDimensions,
            long[] offset, long[] boundIndices)
    {
        baseReader.checkOpen();
        final int[] effectiveBlockDimensions = blockDimensions.clone();
        final long[] fullDimensions = baseReader.getDimensions(objectPath);
        final int[] fullBlockDimensions = new int[fullDimensions.length];
        final long[] fullOffset = new long[fullDimensions.length];
        checkBoundIndices(objectPath, fullDimensions, blockDimensions,
                cardinalityBoundIndices(boundIndices));
        createFullBlockDimensionsAndOffset(effectiveBlockDimensions, offset, boundIndices, fullDimensions,
                fullBlockDimensions, fullOffset);
        final MDFloatArray result = readMDArrayBlockWithOffset(objectPath, fullBlockDimensions, fullOffset);
        return new MDFloatArray(result.getAsFlatArray(), effectiveBlockDimensions);
    }

    @Override
    public MDFloatArray readSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, int[] blockDimensions,
            long[] offset, long[] boundIndices)
    {
        baseReader.checkOpen();
        final int[] effectiveBlockDimensions = blockDimensions.clone();
        final long[] fullDimensions = dataSet.getDimensions();
        final int[] fullBlockDimensions = new int[fullDimensions.length];
        final long[] fullOffset = new long[fullDimensions.length];
        checkBoundIndices(dataSet.getDataSetPath(), fullDimensions, blockDimensions,
                cardinalityBoundIndices(boundIndices));
        createFullBlockDimensionsAndOffset(effectiveBlockDimensions, offset, boundIndices, fullDimensions,
                fullBlockDimensions, fullOffset);
        final MDFloatArray result = readMDArrayBlockWithOffset(dataSet, fullBlockDimensions, fullOffset);
        return new MDFloatArray(result.getAsFlatArray(), effectiveBlockDimensions);
    }

    @Override
    public MDFloatArray readMDArrayBlock(final HDF5DataSet dataSet, final int[] blockDimensions,
            final long[] blockNumber)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readMDArrayBlockWithOffset(dataSet, blockDimensions, offset);
    }

    @Override
    public MDFloatArray readMDArrayBlockWithOffset(final HDF5DataSet dataSet,
            final int[] blockDimensions, final long[] offset)
    {
        assert dataSet != null;
        assert blockDimensions != null;
        assert offset != null;

        baseReader.checkOpen();
        baseReader.h5.checkRank(blockDimensions.length, offset.length);
        baseReader.h5.checkRank(blockDimensions.length, dataSet.getRank());
        final ICallableWithCleanUp<MDFloatArray> readCallable = new ICallableWithCleanUp<MDFloatArray>()
            {
                @Override
                public MDFloatArray call(ICleanUpRegistry registry)
                {
                    final long dataSetId = dataSet.getDataSetId();
                    try
                    {
                        final DataSpaceParameters spaceParams =
                                baseReader.getSpaceParameters(dataSet, offset,
                                        blockDimensions);
                        final float[] dataBlock = new float[spaceParams.blockSize];
                        baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_FLOAT,
                                spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                dataBlock);
                        return new MDFloatArray(dataBlock, spaceParams.dimensions);
                    } catch (HDF5SpaceRankMismatch ex)
                    {
                        final HDF5DataSetInformation info =
                                baseReader.getDataSetInformation(dataSet.getDataSetPath(),
                                        DataTypeInfoOptions.MINIMAL, false);
                        if (ex.getSpaceRankExpected() - ex.getSpaceRankFound() == info
                                .getTypeInformation().getRank())
                        {
                            return readMDArrayBlockOfArrays(dataSetId, blockDimensions,
                                    offset, info, ex.getSpaceRankFound(), registry);
                        } else
                        {
                            throw ex;
                        }
                    }
                }
            };
        return baseReader.runner.call(readCallable);
    }
        
    @Override
    public MDFloatArray readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset)
    {
        assert objectPath != null;
        assert blockDimensions != null;
        assert offset != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDFloatArray> readCallable = new ICallableWithCleanUp<MDFloatArray>()
            {
                @Override
                public MDFloatArray call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    try
                    {
                        final DataSpaceParameters spaceParams =
                                baseReader.getSpaceParameters(dataSetId, offset,
                                        blockDimensions, registry);
                        final float[] dataBlock = new float[spaceParams.blockSize];
                        baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_FLOAT,
                                spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                dataBlock);
                        return new MDFloatArray(dataBlock, spaceParams.dimensions);
                    } catch (HDF5SpaceRankMismatch ex)
                    {
                        final HDF5DataSetInformation info =
                                baseReader.getDataSetInformation(objectPath,
                                        DataTypeInfoOptions.MINIMAL, false);
                        if (ex.getSpaceRankExpected() - ex.getSpaceRankFound() == info
                                .getTypeInformation().getRank())
                        {
                            return readMDArrayBlockOfArrays(dataSetId, blockDimensions,
                                    offset, info, ex.getSpaceRankFound(), registry);
                        } else
                        {
                            throw ex;
                        }
                    }
                }
            };
        return baseReader.runner.call(readCallable);
    }
    
    private MDFloatArray readMDArrayBlockOfArrays(final long dataSetId, final int[] blockDimensions,
            final long[] offset, final HDF5DataSetInformation info, final int spaceRank,
            final ICleanUpRegistry registry)
    {
        final int[] arrayDimensions = info.getTypeInformation().getDimensions();
        int[] effectiveBlockDimensions = blockDimensions;
        // We do not support block-wise reading of array types, check
        // that we do not have to and bail out otherwise.
        for (int i = 0; i < arrayDimensions.length; ++i)
        {
            final int j = spaceRank + i;
            if (effectiveBlockDimensions[j] < 0)
            {
                if (effectiveBlockDimensions == blockDimensions)
                {
                    effectiveBlockDimensions = blockDimensions.clone();
                }
                effectiveBlockDimensions[j] = arrayDimensions[i];
            }
            if (effectiveBlockDimensions[j] != arrayDimensions[i])
            {
                throw new HDF5JavaException(
                        "Block-wise reading of array type data sets is not supported.");
            }
        }
        final int[] spaceBlockDimensions = Arrays.copyOfRange(effectiveBlockDimensions, 0, spaceRank);
        final long[] spaceOfs = Arrays.copyOfRange(offset, 0, spaceRank);
        final DataSpaceParameters spaceParams =
                baseReader.getSpaceParameters(dataSetId, spaceOfs, spaceBlockDimensions, registry);
        final float[] dataBlock =
                new float[spaceParams.blockSize * info.getTypeInformation().getNumberOfElements()];
        final long memoryDataTypeId =
                baseReader.h5.createArrayType(H5T_NATIVE_FLOAT, info.getTypeInformation()
                        .getDimensions(), registry);
        baseReader.h5.readDataSet(dataSetId, memoryDataTypeId, spaceParams.memorySpaceId,
                spaceParams.dataSpaceId, dataBlock);
        return new MDFloatArray(dataBlock, effectiveBlockDimensions);
    }

    @Override
    public Iterable<HDF5DataBlock<float[]>> getArrayNaturalBlocks(final String dataSetPath)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5DataBlock<float[]>>()
            {
                @Override
                public Iterator<HDF5DataBlock<float[]>> iterator()
                {
                    return new Iterator<HDF5DataBlock<float[]>>()
                        {
                            final HDF5DataSet dataSet = baseReader.openDataSet(dataSetPath);
                        
                            final HDF5NaturalBlock1DParameters.HDF5NaturalBlock1DIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5DataBlock<float[]> next()
                            {
                                final long offset = index.computeOffsetAndSizeGetOffset();
                                final float[] block =
                                        readArrayBlockWithOffset(dataSet, index
                                                .getBlockSize(), offset);
                                return new HDF5DataBlock<float[]>(block, index.getAndIncIndex(), 
                                        offset);
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
    public Iterable<HDF5MDDataBlock<MDFloatArray>> getMDArrayNaturalBlocks(final String dataSetPath)
    {
        baseReader.checkOpen();
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5MDDataBlock<MDFloatArray>>()
            {
                @Override
                public Iterator<HDF5MDDataBlock<MDFloatArray>> iterator()
                {
                    return new Iterator<HDF5MDDataBlock<MDFloatArray>>()
                        {
                            final HDF5NaturalBlockMDParameters.HDF5NaturalBlockMDIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5MDDataBlock<MDFloatArray> next()
                            {
                                final long[] offset = index.computeOffsetAndSizeGetOffsetClone();
                                final MDFloatArray data =
                                        readMDArrayBlockWithOffset(dataSetPath, index
                                                .getBlockSize(), offset);
                                return new HDF5MDDataBlock<MDFloatArray>(data, index
                                        .getIndexClone(), offset);
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

    float[] getFloatArrayAttribute(final long objectId, final String attributeName,
            ICleanUpRegistry registry)
    {
        final long attributeId =
                baseReader.h5.openAttribute(objectId, attributeName, registry);
        final long attributeTypeId =
                baseReader.h5.getDataTypeForAttribute(attributeId, registry);
        final long memoryTypeId;
        final int len;
        if (baseReader.h5.getClassType(attributeTypeId) == H5T_ARRAY)
        {
            final int[] arrayDimensions =
                    baseReader.h5.getArrayDimensions(attributeTypeId);
            if (arrayDimensions.length != 1)
            {
                throw new HDF5JavaException(
                        "Array needs to be of rank 1, but is of rank "
                                + arrayDimensions.length);
            }
            len = arrayDimensions[0];
            memoryTypeId =
                    baseReader.h5.createArrayType(H5T_NATIVE_FLOAT, len,
                            registry);
        } else
        {
            final long[] arrayDimensions =
                    baseReader.h5.getDataDimensionsForAttribute(attributeId,
                            registry);
            memoryTypeId = H5T_NATIVE_FLOAT;
            len = HDF5Utils.getOneDimensionalArraySize(arrayDimensions);
        }
        final float[] data =
                baseReader.h5.readAttributeAsFloatArray(attributeId,
                        memoryTypeId, len);
        return data;
    }

    MDFloatArray getFloatMDArrayAttribute(final long objectId,
            final String attributeName, ICleanUpRegistry registry)
    {
        try
        {
            final long attributeId =
                    baseReader.h5.openAttribute(objectId, attributeName, registry);
            final long attributeTypeId =
                    baseReader.h5.getDataTypeForAttribute(attributeId, registry);
            final long memoryTypeId;
            final int[] arrayDimensions;
            if (baseReader.h5.getClassType(attributeTypeId) == H5T_ARRAY)
            {
                arrayDimensions = baseReader.h5.getArrayDimensions(attributeTypeId);
                memoryTypeId =
                        baseReader.h5.createArrayType(H5T_NATIVE_FLOAT,
                                arrayDimensions, registry);
            } else
            {
                arrayDimensions =
                        MDArray.toInt(baseReader.h5.getDataDimensionsForAttribute(
                                attributeId, registry));
                memoryTypeId = H5T_NATIVE_FLOAT;
            }
            final int len = MDArray.getLength(arrayDimensions);
            final float[] data =
                    baseReader.h5.readAttributeAsFloatArray(attributeId,
                            memoryTypeId, len);
            return new MDFloatArray(data, arrayDimensions);
        } catch (IllegalArgumentException ex)
        {
            throw new HDF5JavaException(ex.getMessage());
        }
    }
}
