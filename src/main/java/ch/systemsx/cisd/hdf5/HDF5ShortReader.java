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
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;

import java.util.Arrays;
import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import ch.systemsx.cisd.hdf5.exceptions.HDF5SpaceRankMismatch;
import hdf.hdf5lib.HDF5Constants;

/**
 * The implementation of {@link IHDF5ShortReader}.
 * 
 * @author Bernd Rinn
 */
class HDF5ShortReader implements IHDF5ShortReader
{
    private final HDF5BaseReader baseReader;

    HDF5ShortReader(HDF5BaseReader baseReader)
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
    public short getAttr(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<Short> getAttributeRunnable = new ICallableWithCleanUp<Short>()
            {
                @Override
                public Short call(ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    final long attributeId =
                            baseReader.h5.openAttribute(objectId, attributeName, registry);
                    final short[] data =
                            baseReader.h5.readAttributeAsShortArray(attributeId, H5T_NATIVE_INT16, 1);
                    return data[0];
                }
            };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public short[] getArrayAttr(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<short[]> getAttributeRunnable =
                new ICallableWithCleanUp<short[]>()
                    {
                        @Override
                        public short[] call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            return getShortArrayAttribute(objectId, attributeName, registry);
                        }
                    };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public MDShortArray getMDArrayAttr(final String objectPath,
            final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDShortArray> getAttributeRunnable =
                new ICallableWithCleanUp<MDShortArray>()
                    {
                        @Override
                        public MDShortArray call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            return getShortMDArrayAttribute(objectId, attributeName, registry);
                        }
                    };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public short[][] getMatrixAttr(final String objectPath, final String attributeName)
            throws HDF5JavaException
    {
        final MDShortArray array = getMDArrayAttr(objectPath, attributeName);
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
    public short read(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<Short> readCallable = new ICallableWithCleanUp<Short>()
            {
                @Override
                public Short call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final short[] data = new short[1];
                    baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT16, data);
                    return data[0];
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public short[] readArray(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<short[]> readCallable = new ICallableWithCleanUp<short[]>()
            {
                @Override
                public short[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    return readShortArray(dataSetId, registry);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    private short[] readShortArray(long dataSetId, ICleanUpRegistry registry)
    {
        try
        {
            final DataSpaceParameters spaceParams =
                    baseReader.getSpaceParameters(dataSetId, registry);
            final short[] data = new short[spaceParams.blockSize];
            baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT16, spaceParams.memorySpaceId,
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
                    return readShortArrayFromArrayType(dataSetId, dataTypeId, registry);
                }
            }
            throw ex;
        }
    }

    private short[] readShortArrayFromArrayType(long dataSetId, final long dataTypeId,
            ICleanUpRegistry registry)
    {
        final long spaceId = baseReader.h5.createScalarDataSpace();
        final int[] dimensions = baseReader.h5.getArrayDimensions(dataTypeId);
        final short[] data = new short[HDF5Utils.getOneDimensionalArraySize(dimensions)];
        final long memoryDataTypeId =
                baseReader.h5.createArrayType(H5T_NATIVE_INT16, data.length, registry);
        baseReader.h5.readDataSet(dataSetId, memoryDataTypeId, spaceId, spaceId, data);
        return data;
    }

    @Override
    public int[] readToMDArrayWithOffset(final String objectPath, final MDShortArray array,
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
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_INT16, registry);
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, 
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, array.
                            getAsFlatArray());
                    return MDArray.toInt(spaceParams.dimensions);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public int[] readToMDArrayWithOffset(final HDF5DataSet dataSet, final MDShortArray array,
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
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_INT16, registry);
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
            final MDShortArray array, final int[] blockDimensions, final long[] offset,
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
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_INT16, registry);
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
            final MDShortArray array, final int[] blockDimensions, final long[] offset,
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
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_INT16, registry);
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, 
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, array
                            .getAsFlatArray());
                    return MDArray.toInt(spaceParams.dimensions);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public short[] readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber)
    {
        return readArrayBlockWithOffset(objectPath, blockSize, blockNumber * blockSize);
    }

    @Override
    public short[] readArrayBlock(HDF5DataSet dataSet, int blockSize, long blockNumber)
    {
        return readArrayBlockWithOffset(dataSet, blockSize, blockNumber * blockSize);
    }

    @Override
    public short[] readArrayBlockWithOffset(final String objectPath, final int blockSize,
            final long offset)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<short[]> readCallable = new ICallableWithCleanUp<short[]>()
            {
                @Override
                public short[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, offset, blockSize, registry);
                    final short[] data = new short[spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT16, spaceParams.memorySpaceId,
                            spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public short[] readArrayBlockWithOffset(final HDF5DataSet dataSet, final int blockSize, final long offset)
    {
        assert dataSet != null;

        baseReader.checkOpen();
        baseReader.h5.checkRank(1, dataSet.getRank());
        final ICallableWithCleanUp<short[]> readCallable = new ICallableWithCleanUp<short[]>()
            {
                @Override
                public short[] call(ICleanUpRegistry registry)
                {
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSet, offset, blockSize);
                    final short[] data = new short[spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSet.getDataSetId(), H5T_NATIVE_INT16, spaceParams.memorySpaceId,
                            spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public short[][] readMatrix(final String objectPath) throws HDF5JavaException
    {
        final MDShortArray array = readMDArray(objectPath);
        if (array.rank() != 2)
        {
            throw new HDF5JavaException("Array is supposed to be of rank 2, but is of rank "
                    + array.rank());
        }
        return array.toMatrix();
    }

    @Override
    public short[][] readMatrixBlock(final String objectPath, final int blockSizeX,
            final int blockSizeY, final long blockNumberX, final long blockNumberY) 
            throws HDF5JavaException
    {
        final MDShortArray array = readMDArrayBlock(objectPath, new int[]
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
    public short[][] readMatrixBlockWithOffset(final String objectPath, final int blockSizeX,
            final int blockSizeY, final long offsetX, final long offsetY) throws HDF5JavaException
    {
        final MDShortArray array = readMDArrayBlockWithOffset(objectPath, new int[]
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
    public MDShortArray readMDArraySlice(String objectPath, IndexMap boundIndices)
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
        final MDShortArray result = readMDArrayBlockWithOffset(objectPath, fullBlockDimensions, fullOffset);
        if (fullBlockDimensions.length == cardBoundIndices) // no free indices
        {
	        return new MDShortArray(result.getAsFlatArray(), new int[] { 1 });
	    } else
	    {
	        return new MDShortArray(result.getAsFlatArray(), effectiveBlockDimensions);
	    }
    }

    @Override
    public MDShortArray readMDArraySlice(HDF5DataSet dataSet, IndexMap boundIndices)
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
        final MDShortArray result = readMDArrayBlockWithOffset(dataSet, fullBlockDimensions, fullOffset);
        if (fullBlockDimensions.length == cardBoundIndices) // no free indices
        {
            return new MDShortArray(result.getAsFlatArray(), new int[] { 1 });
        } else
        {
            return new MDShortArray(result.getAsFlatArray(), effectiveBlockDimensions);
        }
    }
    
    @Override
    public MDShortArray readMDArraySlice(String objectPath, long[] boundIndices)
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
        final MDShortArray result = readMDArrayBlockWithOffset(objectPath, fullBlockDimensions, fullOffset);
        if (fullBlockDimensions.length == cardBoundIndices) // no free indices
        {
	        return new MDShortArray(result.getAsFlatArray(), new int[] { 1 });
	    } else
	    {
	        return new MDShortArray(result.getAsFlatArray(), effectiveBlockDimensions);
	    }
    }

    @Override
    public MDShortArray readMDArraySlice(final HDF5DataSet dataSet, final long[] boundIndices)
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
        final MDShortArray result = readMDArrayBlockWithOffset(dataSet, fullBlockDimensions, fullOffset);
        if (fullBlockDimensions.length == cardBoundIndices) // no free indices
        {
            return new MDShortArray(result.getAsFlatArray(), new int[] { 1 });
        } else
        {
            return new MDShortArray(result.getAsFlatArray(), effectiveBlockDimensions);
        }
    }

    @Override
    public MDShortArray readMDArray(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDShortArray> readCallable = new ICallableWithCleanUp<MDShortArray>()
            {
                @Override
                public MDShortArray call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    return readShortMDArray(dataSetId, registry);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    MDShortArray readMDArray(final HDF5DataSet dataSet)
    {
        assert dataSet != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDShortArray> readCallable = new ICallableWithCleanUp<MDShortArray>()
            {
                @Override
                public MDShortArray call(ICleanUpRegistry registry)
                {
                    return readShortMDArray(dataSet.getDataSetId(), registry);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    MDShortArray readShortMDArray(long dataSetId, ICleanUpRegistry registry)
    {
        try
        {
            final DataSpaceParameters spaceParams =
                    baseReader.getSpaceParameters(dataSetId, registry);
            final short[] data = new short[spaceParams.blockSize];
            baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT16, spaceParams.memorySpaceId,
                    spaceParams.dataSpaceId, data);
            return new MDShortArray(data, spaceParams.dimensions);
        } catch (HDF5LibraryException ex)
        {
            if (ex.getMajorErrorNumber() == HDF5Constants.H5E_DATATYPE
                    && ex.getMinorErrorNumber() == HDF5Constants.H5E_CANTINIT)
            {
                // Check whether it is an array data type.
                final long dataTypeId = baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                if (baseReader.h5.getClassType(dataTypeId) == HDF5Constants.H5T_ARRAY)
                {
                    return readShortMDArrayFromArrayType(dataSetId, dataTypeId, registry);
                }
            }
            throw ex;
        }
    }

    private MDShortArray readShortMDArrayFromArrayType(long dataSetId, final long dataTypeId,
            ICleanUpRegistry registry)
    {
        final int[] arrayDimensions = baseReader.h5.getArrayDimensions(dataTypeId);
        final long memoryDataTypeId =
                baseReader.h5.createArrayType(H5T_NATIVE_INT16, arrayDimensions, registry);
        final DataSpaceParameters spaceParams = baseReader.getSpaceParameters(dataSetId, registry);
        if (spaceParams.blockSize == 0)
        {
            final long spaceId = baseReader.h5.createScalarDataSpace();
            final short[] data = new short[MDArray.getLength(arrayDimensions)];
            baseReader.h5.readDataSet(dataSetId, memoryDataTypeId, spaceId, spaceId, data);
            return new MDShortArray(data, arrayDimensions);
        } else
        {
            final short[] data =
                    new short[MDArray.getLength(arrayDimensions) * spaceParams.blockSize];
            baseReader.h5.readDataSet(dataSetId, memoryDataTypeId, spaceParams.memorySpaceId,
                    spaceParams.dataSpaceId, data);
            return new MDShortArray(data, MatrixUtils.concat(MDArray.toInt(spaceParams.dimensions),
                    arrayDimensions));
        }
    }

    @Override
    public MDShortArray readMDArray(String objectPath, HDF5ArrayBlockParams params)
    {
        baseReader.checkOpen();
        try (final HDF5DataSet dataSet = baseReader.openDataSet(objectPath))
        {
            return readMDArray(dataSet, params);
        }
    }

    @Override
    public MDShortArray readMDArray(HDF5DataSet dataSet, HDF5ArrayBlockParams params)
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
    public MDShortArray readSlicedMDArrayBlock(String objectPath, int[] blockDimensions,
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
    public MDShortArray readSlicedMDArrayBlock(HDF5DataSet dataSet, int[] blockDimensions,
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
    public MDShortArray readSlicedMDArrayBlock(String objectPath, int[] blockDimensions,
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
    public MDShortArray readSlicedMDArrayBlock(HDF5DataSet dataSet, int[] blockDimensions,
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
    public MDShortArray readMDArrayBlock(final String objectPath, final int[] blockDimensions,
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
    public MDShortArray readSlicedMDArrayBlockWithOffset(String objectPath, int[] blockDimensions,
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
        final MDShortArray result = readMDArrayBlockWithOffset(objectPath, fullBlockDimensions, fullOffset);
        return new MDShortArray(result.getAsFlatArray(), effectiveBlockDimensions);
    }

    @Override
    public MDShortArray readSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, int[] blockDimensions,
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
        final MDShortArray result = readMDArrayBlockWithOffset(dataSet, fullBlockDimensions, fullOffset);
        return new MDShortArray(result.getAsFlatArray(), effectiveBlockDimensions);
    }

    @Override
    public MDShortArray readSlicedMDArrayBlockWithOffset(String objectPath, int[] blockDimensions,
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
        final MDShortArray result = readMDArrayBlockWithOffset(objectPath, fullBlockDimensions, fullOffset);
        return new MDShortArray(result.getAsFlatArray(), effectiveBlockDimensions);
    }

    @Override
    public MDShortArray readSlicedMDArrayBlockWithOffset(HDF5DataSet dataSet, int[] blockDimensions,
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
        final MDShortArray result = readMDArrayBlockWithOffset(dataSet, fullBlockDimensions, fullOffset);
        return new MDShortArray(result.getAsFlatArray(), effectiveBlockDimensions);
    }

    @Override
    public MDShortArray readMDArrayBlock(final HDF5DataSet dataSet, final int[] blockDimensions,
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
    public MDShortArray readMDArrayBlockWithOffset(final HDF5DataSet dataSet,
            final int[] blockDimensions, final long[] offset)
    {
        assert dataSet != null;
        assert blockDimensions != null;
        assert offset != null;

        baseReader.checkOpen();
        baseReader.h5.checkRank(blockDimensions.length, offset.length);
        baseReader.h5.checkRank(blockDimensions.length, dataSet.getRank());
        final ICallableWithCleanUp<MDShortArray> readCallable = new ICallableWithCleanUp<MDShortArray>()
            {
                @Override
                public MDShortArray call(ICleanUpRegistry registry)
                {
                    final long dataSetId = dataSet.getDataSetId();
                    try
                    {
                        final DataSpaceParameters spaceParams =
                                baseReader.getSpaceParameters(dataSet, offset,
                                        blockDimensions);
                        final short[] dataBlock = new short[spaceParams.blockSize];
                        baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT16,
                                spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                dataBlock);
                        return new MDShortArray(dataBlock, spaceParams.dimensions);
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
    public MDShortArray readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset)
    {
        assert objectPath != null;
        assert blockDimensions != null;
        assert offset != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDShortArray> readCallable = new ICallableWithCleanUp<MDShortArray>()
            {
                @Override
                public MDShortArray call(ICleanUpRegistry registry)
                {
                    final long dataSetId = 
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    try
                    {
                        final DataSpaceParameters spaceParams =
                                baseReader.getSpaceParameters(dataSetId, offset,
                                        blockDimensions, registry);
                        final short[] dataBlock = new short[spaceParams.blockSize];
                        baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT16,
                                spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                dataBlock);
                        return new MDShortArray(dataBlock, spaceParams.dimensions);
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
    
    private MDShortArray readMDArrayBlockOfArrays(final long dataSetId, final int[] blockDimensions,
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
        final short[] dataBlock =
                new short[spaceParams.blockSize * info.getTypeInformation().getNumberOfElements()];
        final long memoryDataTypeId =
                baseReader.h5.createArrayType(H5T_NATIVE_INT16, info.getTypeInformation()
                        .getDimensions(), registry);
        baseReader.h5.readDataSet(dataSetId, memoryDataTypeId, spaceParams.memorySpaceId,
                spaceParams.dataSpaceId, dataBlock);
        return new MDShortArray(dataBlock, effectiveBlockDimensions);
    }

    @Override
    public Iterable<HDF5DataBlock<short[]>> getArrayNaturalBlocks(final String dataSetPath)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5DataBlock<short[]>>()
            {
                @Override
                public Iterator<HDF5DataBlock<short[]>> iterator()
                {
                    return new Iterator<HDF5DataBlock<short[]>>()
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
                            public HDF5DataBlock<short[]> next()
                            {
                                final long offset = index.computeOffsetAndSizeGetOffset();
                                final short[] block =
                                        readArrayBlockWithOffset(dataSet, index
                                                .getBlockSize(), offset);
                                return new HDF5DataBlock<short[]>(block, index.getAndIncIndex(), 
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
    public Iterable<HDF5MDDataBlock<MDShortArray>> getMDArrayNaturalBlocks(final String dataSetPath)
    {
        baseReader.checkOpen();
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5MDDataBlock<MDShortArray>>()
            {
                @Override
                public Iterator<HDF5MDDataBlock<MDShortArray>> iterator()
                {
                    return new Iterator<HDF5MDDataBlock<MDShortArray>>()
                        {
                            final HDF5NaturalBlockMDParameters.HDF5NaturalBlockMDIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5MDDataBlock<MDShortArray> next()
                            {
                                final long[] offset = index.computeOffsetAndSizeGetOffsetClone();
                                final MDShortArray data =
                                        readMDArrayBlockWithOffset(dataSetPath, index
                                                .getBlockSize(), offset);
                                return new HDF5MDDataBlock<MDShortArray>(data, index
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

    short[] getShortArrayAttribute(final long objectId, final String attributeName,
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
                    baseReader.h5.createArrayType(H5T_NATIVE_INT16, len,
                            registry);
        } else
        {
            final long[] arrayDimensions =
                    baseReader.h5.getDataDimensionsForAttribute(attributeId,
                            registry);
            memoryTypeId = H5T_NATIVE_INT16;
            len = HDF5Utils.getOneDimensionalArraySize(arrayDimensions);
        }
        final short[] data =
                baseReader.h5.readAttributeAsShortArray(attributeId,
                        memoryTypeId, len);
        return data;
    }

    MDShortArray getShortMDArrayAttribute(final long objectId,
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
                        baseReader.h5.createArrayType(H5T_NATIVE_INT16,
                                arrayDimensions, registry);
            } else
            {
                arrayDimensions =
                        MDArray.toInt(baseReader.h5.getDataDimensionsForAttribute(
                                attributeId, registry));
                memoryTypeId = H5T_NATIVE_INT16;
            }
            final int len = MDArray.getLength(arrayDimensions);
            final short[] data =
                    baseReader.h5.readAttributeAsShortArray(attributeId,
                            memoryTypeId, len);
            return new MDShortArray(data, arrayDimensions);
        } catch (IllegalArgumentException ex)
        {
            throw new HDF5JavaException(ex.getMessage());
        }
    }
}
