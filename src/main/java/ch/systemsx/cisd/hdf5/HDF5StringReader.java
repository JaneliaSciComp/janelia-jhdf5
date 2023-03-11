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

import static ch.systemsx.cisd.hdf5.HDF5Utils.getOneDimensionalArraySize;
import static hdf.hdf5lib.HDF5Constants.H5T_STRING;

import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * The implementation of {@link IHDF5StringReader}.
 * 
 * @author Bernd Rinn
 */
public class HDF5StringReader implements IHDF5StringReader
{

    private final HDF5BaseReader baseReader;

    HDF5StringReader(HDF5BaseReader baseReader)
    {
        assert baseReader != null;

        this.baseReader = baseReader;
    }

    //
    // Attributes
    //

    @Override
    public String getAttr(final String objectPath, final String attributeName)
    {
        return getStringAttribute(objectPath, attributeName, false);
    }

    @Override
    public String getAttrRaw(final String objectPath, final String attributeName)
    {
        return getStringAttribute(objectPath, attributeName, true);
    }

    String getStringAttribute(final String objectPath, final String attributeName,
            final boolean readRaw)
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
                    return baseReader.getStringAttribute(objectId, objectPath, attributeName,
                            readRaw, registry);
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    @Override
    public String[] getArrayAttr(final String objectPath, final String attributeName)
    {
        return getStringArrayAttribute(objectPath, attributeName, false);
    }

    @Override
    public String[] getArrayAttrRaw(final String objectPath, final String attributeName)
    {
        return getStringArrayAttribute(objectPath, attributeName, true);
    }

    String[] getStringArrayAttribute(final String objectPath, final String attributeName,
            final boolean readRaw)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String[]> readRunnable = new ICallableWithCleanUp<String[]>()
            {
                @Override
                public String[] call(ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    return baseReader.getStringArrayAttribute(objectId, objectPath, attributeName,
                            readRaw, registry);
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    @Override
    public MDArray<String> getMDArrayAttr(final String objectPath, final String attributeName)
    {
        return getStringMDArrayAttribute(objectPath, attributeName, false);
    }

    @Override
    public MDArray<String> getMDArrayAttrRaw(final String objectPath, final String attributeName)
    {
        return getStringMDArrayAttribute(objectPath, attributeName, true);
    }

    MDArray<String> getStringMDArrayAttribute(final String objectPath, final String attributeName,
            final boolean readRaw)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDArray<String>> readRunnable =
                new ICallableWithCleanUp<MDArray<String>>()
                    {
                        @Override
                        public MDArray<String> call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            return baseReader.getStringMDArrayAttribute(objectId, objectPath,
                                    attributeName, readRaw, registry);
                        }
                    };
        return baseReader.runner.call(readRunnable);
    }

    //
    // Data Sets
    //

    @Override
    public String read(final String objectPath) throws HDF5JavaException
    {
        return readString(objectPath, false);
    }

    @Override
    public String readRaw(String objectPath) throws HDF5JavaException
    {
        return readString(objectPath, true);
    }

    String readString(final String objectPath, final boolean readRaw) throws HDF5JavaException
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
                    final long dataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    final boolean isString = (baseReader.h5.getClassType(dataTypeId) == H5T_STRING);
                    if (isString == false)
                    {
                        throw new HDF5JavaException(objectPath + " needs to be a String.");
                    }
                    if (baseReader.h5.isVariableLengthString(dataTypeId))
                    {
                        String[] data = new String[1];
                        baseReader.h5.readDataSetVL(dataSetId, dataTypeId, data);
                        return data[0];
                    } else
                    {
                        final int size = baseReader.h5.getDataTypeSize(dataTypeId);
                        final CharacterEncoding encoding =
                                baseReader.h5.getCharacterEncoding(dataTypeId);
                        byte[] data = new byte[size];
                        baseReader.h5.readDataSetNonNumeric(dataSetId, dataTypeId, data);
                        return readRaw ? StringUtils.fromBytes(data, encoding) : StringUtils
                                .fromBytes0Term(data, encoding);
                    }
                }
            };
        return baseReader.runner.call(writeRunnable);
    }

    @Override
    public String[] readArrayRaw(final String objectPath) throws HDF5JavaException
    {
        return readStringArray(objectPath, true);
    }

    @Override
    public String[] readArray(final String objectPath) throws HDF5JavaException
    {
        return readStringArray(objectPath, false);
    }

    String[] readStringArray(final String objectPath, final boolean readRaw)
            throws HDF5JavaException
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String[]> writeRunnable = new ICallableWithCleanUp<String[]>()
            {
                @Override
                public String[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final long[] dimensions = baseReader.h5.getDataDimensions(dataSetId, registry);
                    final int oneDimSize = getOneDimensionalArraySize(dimensions);
                    final String[] data = new String[oneDimSize];
                    final long dataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    if (baseReader.h5.isVariableLengthString(dataTypeId))
                    {
                        baseReader.h5.readDataSetVL(dataSetId, dataTypeId, data);
                    } else
                    {
                        final boolean isString =
                                (baseReader.h5.getClassType(dataTypeId) == H5T_STRING);
                        if (isString == false)
                        {
                            throw new HDF5JavaException(objectPath + " needs to be a String.");
                        }
                        final int strLength;
                        final byte[] bdata;
                        if (readRaw)
                        {
                            strLength = baseReader.h5.getDataTypeSize(dataTypeId);
                            bdata = new byte[oneDimSize * strLength];
                            baseReader.h5.readDataSetNonNumeric(dataSetId, dataTypeId, bdata);
                        } else
                        {
                            strLength = -1;
                            bdata = null;
                            baseReader.h5.readDataSetString(dataSetId, dataTypeId, data);
                        }
                        if (bdata != null && readRaw)
                        {
                            final CharacterEncoding encoding =
                                    baseReader.h5.getCharacterEncoding(dataTypeId);
                            for (int i = 0, startIdx = 0; i < oneDimSize; ++i, startIdx +=
                                    strLength)
                            {
                                data[i] =
                                        StringUtils.fromBytes(bdata, startIdx,
                                                startIdx + strLength, encoding);
                            }
                        }
                    }
                    return data;
                }
            };
        return baseReader.runner.call(writeRunnable);
    }

    @Override
    public String[] readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber)
    {
        return readArrayBlockWithOffset(objectPath, blockSize, blockSize * blockNumber);
    }

    @Override
    public String[] readArrayBlockRaw(String objectPath, int blockSize, long blockNumber)
    {
        return readArrayBlockWithOffsetRaw(objectPath, blockSize, blockSize * blockNumber);
    }

    String[] readArrayBlockWithOffset(final String objectPath, final int blockSize,
            final long offset, final boolean readRaw)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String[]> readCallable = new ICallableWithCleanUp<String[]>()
            {
                @Override
                public String[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, offset, blockSize, registry);
                    final String[] data = new String[spaceParams.blockSize];
                    final long dataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    if (baseReader.h5.isVariableLengthString(dataTypeId))
                    {
                        baseReader.h5.readDataSetVL(dataSetId, dataTypeId,
                                spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    } else
                    {
                        final boolean isString =
                                (baseReader.h5.getClassType(dataTypeId) == H5T_STRING);
                        if (isString == false)
                        {
                            throw new HDF5JavaException(objectPath + " needs to be a String.");
                        }

                        final int strLength;
                        final byte[] bdata;
                        if (readRaw)
                        {
                            strLength = baseReader.h5.getDataTypeSize(dataTypeId);
                            bdata = new byte[spaceParams.blockSize * strLength];
                            baseReader.h5.readDataSetNonNumeric(dataSetId, dataTypeId,
                                    spaceParams.memorySpaceId, spaceParams.dataSpaceId, bdata);
                        } else
                        {
                            strLength = -1;
                            bdata = null;
                            baseReader.h5.readDataSetString(dataSetId, dataTypeId,
                                    spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                        }
                        if (bdata != null && readRaw)
                        {
                            final CharacterEncoding encoding =
                                    baseReader.h5.getCharacterEncoding(dataTypeId);
                            for (int i = 0, startIdx = 0; i < spaceParams.blockSize; ++i, startIdx +=
                                    strLength)
                            {
                                data[i] =
                                        StringUtils.fromBytes(bdata, startIdx,
                                                startIdx + strLength, encoding);
                            }
                        }
                    }
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public String[] readArrayBlockWithOffset(String objectPath, int blockSize, long offset)
    {
        return readArrayBlockWithOffset(objectPath, blockSize, offset, false);
    }

    @Override
    public String[] readArrayBlockWithOffsetRaw(String objectPath, int blockSize, long offset)
    {
        return readArrayBlockWithOffset(objectPath, blockSize, offset, true);
    }

    @Override
    public MDArray<String> readMDArray(final String objectPath)
    {
        return readStringMDArray(objectPath, false);
    }

    @Override
    public MDArray<String> readMDArrayRaw(final String objectPath)
    {
        return readStringMDArray(objectPath, true);
    }

    MDArray<String> readStringMDArray(final String objectPath, final boolean readRaw)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDArray<String>> readCallable =
                new ICallableWithCleanUp<MDArray<String>>()
                    {
                        @Override
                        public MDArray<String> call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final DataSpaceParameters spaceParams =
                                    baseReader.getSpaceParameters(dataSetId, registry);
                            final String[] data = new String[spaceParams.blockSize];
                            final long dataTypeId =
                                    baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                            if (baseReader.h5.isVariableLengthString(dataTypeId))
                            {
                                baseReader.h5.readDataSetVL(dataSetId, dataTypeId,
                                        spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                            } else
                            {
                                final boolean isString =
                                        (baseReader.h5.getClassType(dataTypeId) == H5T_STRING);
                                if (isString == false)
                                {
                                    throw new HDF5JavaException(objectPath
                                            + " needs to be a String.");
                                }

                                final int strLength;
                                final byte[] bdata;
                                if (readRaw)
                                {
                                    strLength = baseReader.h5.getDataTypeSize(dataTypeId);
                                    bdata = new byte[spaceParams.blockSize * strLength];
                                    baseReader.h5.readDataSetNonNumeric(dataSetId, dataTypeId,
                                            bdata);
                                } else
                                {
                                    strLength = -1;
                                    bdata = null;
                                    baseReader.h5.readDataSetString(dataSetId, dataTypeId, data);
                                }
                                if (bdata != null && readRaw)
                                {
                                    final CharacterEncoding encoding =
                                            baseReader.h5.getCharacterEncoding(dataTypeId);
                                    for (int i = 0, startIdx = 0; i < spaceParams.blockSize; ++i, startIdx +=
                                            strLength)
                                    {
                                        data[i] =
                                                StringUtils.fromBytes(bdata, startIdx, startIdx
                                                        + strLength, encoding);
                                    }
                                }
                            }
                            return new MDArray<String>(data, spaceParams.dimensions);
                        }
                    };
        return baseReader.runner.call(readCallable);
    }

    MDArray<String> readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset, final boolean readRaw)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDArray<String>> readCallable =
                new ICallableWithCleanUp<MDArray<String>>()
                    {
                        @Override
                        public MDArray<String> call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final DataSpaceParameters spaceParams =
                                    baseReader.getSpaceParameters(dataSetId, offset,
                                            blockDimensions, registry);
                            final String[] dataBlock = new String[spaceParams.blockSize];
                            final long dataTypeId =
                                    baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                            if (baseReader.h5.isVariableLengthString(dataTypeId))
                            {
                                baseReader.h5.readDataSetVL(dataSetId, dataTypeId,
                                        spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                        dataBlock);
                            } else
                            {
                                final boolean isString =
                                        (baseReader.h5.getClassType(dataTypeId) == H5T_STRING);
                                if (isString == false)
                                {
                                    throw new HDF5JavaException(objectPath
                                            + " needs to be a String.");
                                }

                                final int strLength;
                                byte[] bdata = null;
                                if (readRaw)
                                {
                                    strLength = baseReader.h5.getDataTypeSize(dataTypeId);
                                    bdata = new byte[spaceParams.blockSize * strLength];
                                    baseReader.h5.readDataSetNonNumeric(dataSetId, dataTypeId,
                                            spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                            bdata);
                                } else
                                {
                                    strLength = -1;
                                    baseReader.h5.readDataSetString(dataSetId, dataTypeId,
                                            spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                            dataBlock);
                                }
                                if (bdata != null && readRaw)
                                {
                                    final CharacterEncoding encoding =
                                            baseReader.h5.getCharacterEncoding(dataTypeId);
                                    for (int i = 0, startIdx = 0; i < spaceParams.blockSize; ++i, startIdx +=
                                            strLength)
                                    {
                                        dataBlock[i] =
                                                StringUtils.fromBytes(bdata, startIdx, startIdx
                                                        + strLength, encoding);
                                    }
                                }
                            }
                            return new MDArray<String>(dataBlock, blockDimensions);
                        }
                    };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public MDArray<String> readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset)
    {
        return readMDArrayBlockWithOffset(objectPath, blockDimensions, offset, false);
    }

    @Override
    public MDArray<String> readMDArrayBlockWithOffsetRaw(String objectPath, int[] blockDimensions,
            long[] offset)
    {
        return readMDArrayBlockWithOffset(objectPath, blockDimensions, offset, true);
    }

    @Override
    public MDArray<String> readMDArrayBlock(final String objectPath, final int[] blockDimensions,
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
    public MDArray<String> readMDArrayBlockRaw(String objectPath, int[] blockDimensions,
            long[] blockNumber)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readMDArrayBlockWithOffsetRaw(objectPath, blockDimensions, offset);
    }

    Iterable<HDF5DataBlock<String[]>> getArrayNaturalBlocks(final String dataSetPath,
            final boolean readRaw) throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5DataBlock<String[]>>()
            {
                @Override
                public Iterator<HDF5DataBlock<String[]>> iterator()
                {
                    return new Iterator<HDF5DataBlock<String[]>>()
                        {
                            final HDF5NaturalBlock1DParameters.HDF5NaturalBlock1DIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5DataBlock<String[]> next()
                            {
                                final long offset = index.computeOffsetAndSizeGetOffset();
                                final String[] block =
                                        readRaw ? readArrayBlockWithOffsetRaw(dataSetPath,
                                                index.getBlockSize(), offset)
                                                : readArrayBlockWithOffset(dataSetPath,
                                                        index.getBlockSize(), offset);
                                return new HDF5DataBlock<String[]>(block, index.getAndIncIndex(),
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
    public Iterable<HDF5DataBlock<String[]>> getArrayNaturalBlocks(final String dataSetPath)
            throws HDF5JavaException
    {
        return getArrayNaturalBlocks(dataSetPath, false);
    }

    @Override
    public Iterable<HDF5DataBlock<String[]>> getArrayNaturalBlocksRaw(String dataSetPath)
            throws HDF5JavaException
    {
        return getArrayNaturalBlocks(dataSetPath, true);
    }

    Iterable<HDF5MDDataBlock<MDArray<String>>> getMDArrayNaturalBlocks(final String objectPath,
            final boolean readRaw)
    {
        baseReader.checkOpen();
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(objectPath));

        return new Iterable<HDF5MDDataBlock<MDArray<String>>>()
            {
                @Override
                public Iterator<HDF5MDDataBlock<MDArray<String>>> iterator()
                {
                    return new Iterator<HDF5MDDataBlock<MDArray<String>>>()
                        {
                            final HDF5NaturalBlockMDParameters.HDF5NaturalBlockMDIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5MDDataBlock<MDArray<String>> next()
                            {
                                final long[] offset = index.computeOffsetAndSizeGetOffsetClone();
                                final MDArray<String> data =
                                        readRaw ? readMDArrayBlockWithOffsetRaw(objectPath,
                                                index.getBlockSize(), offset)
                                                : readMDArrayBlockWithOffset(objectPath,
                                                        index.getBlockSize(), offset);
                                return new HDF5MDDataBlock<MDArray<String>>(data,
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
    public Iterable<HDF5MDDataBlock<MDArray<String>>> getMDArrayNaturalBlocks(
            final String objectPath)
    {
        return getMDArrayNaturalBlocks(objectPath, false);
    }

    @Override
    public Iterable<HDF5MDDataBlock<MDArray<String>>> getMDArrayNaturalBlocksRaw(String objectPath)
    {
        return getMDArrayNaturalBlocks(objectPath, true);
    }

}
