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

import static hdf.hdf5lib.HDF5Constants.H5T_STRING;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * Implementation of {@link IHDF5OpaqueReader}
 * 
 * @author Bernd Rinn
 */
public class HDF5OpaqueReader implements IHDF5OpaqueReader
{

    private final HDF5BaseReader baseReader;

    HDF5OpaqueReader(HDF5BaseReader baseReader)
    {
        assert baseReader != null;

        this.baseReader = baseReader;
    }

    @Override
    public String tryGetOpaqueTag(final String objectPath)
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<String> readTagCallable = new ICallableWithCleanUp<String>()
            {
                @Override
                public String call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final long dataTypeId = baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                    return baseReader.h5.tryGetOpaqueTag(dataTypeId);
                }
            };
        return baseReader.runner.call(readTagCallable);
    }

    @Override
    public HDF5OpaqueType tryGetOpaqueType(final String objectPath)
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5OpaqueType> readTagCallable =
                new ICallableWithCleanUp<HDF5OpaqueType>()
                    {
                        @Override
                        public HDF5OpaqueType call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final long dataTypeId =
                                    baseReader.h5.getDataTypeForDataSet(dataSetId,
                                            baseReader.fileRegistry);
                            final String opaqueTagOrNull =
                                    baseReader.h5.tryGetOpaqueTag(dataTypeId);
                            if (opaqueTagOrNull == null)
                            {
                                return null;
                            } else
                            {
                                return new HDF5OpaqueType(baseReader.fileId, dataTypeId,
                                        opaqueTagOrNull, baseReader);
                            }
                        }
                    };
        return baseReader.runner.call(readTagCallable);
    }

    @Override
    public byte[] readArray(final String objectPath)
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<byte[]> readCallable = new ICallableWithCleanUp<byte[]>()
            {
                @Override
                public byte[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, registry);
                    final long nativeDataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    final boolean isString =
                            (baseReader.h5.getClassType(nativeDataTypeId) == H5T_STRING);
                    byte[] data;
                    if (isString)
                    {
                        if (baseReader.h5.isVariableLengthString(nativeDataTypeId))
                        {
                            String[] value = new String[1];
                            baseReader.h5.readDataSetVL(dataSetId, nativeDataTypeId, value);
                            try
                            {
                                data = value[0].getBytes(CharacterEncoding.ASCII.getCharSetName());
                            } catch (UnsupportedEncodingException ex)
                            {
                                data = value[0].getBytes();
                            }
                        } else
                        {
                            final int size = baseReader.h5.getDataTypeSize(nativeDataTypeId);
                            data = new byte[size];
                            baseReader.h5.readDataSetNonNumeric(dataSetId, nativeDataTypeId, data);
                        }
                    } else
                    {
                        final int elementSize = baseReader.h5.getDataTypeSize(nativeDataTypeId);
                        data = new byte[spaceParams.blockSize * elementSize];
                        baseReader.h5.readDataSet(dataSetId, nativeDataTypeId,
                                spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    }
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public byte[] getArrayAttr(final String objectPath, final String attributeName)
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<byte[]> readCallable = new ICallableWithCleanUp<byte[]>()
            {
                @Override
                public byte[] call(ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    return baseReader.getAttributeAsByteArray(objectId, attributeName, registry);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public byte[] readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber) throws HDF5JavaException
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<byte[]> readCallable = new ICallableWithCleanUp<byte[]>()
            {
                @Override
                public byte[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, blockNumber * blockSize,
                                    blockSize, registry);
                    final long nativeDataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    checkNotAString(objectPath, nativeDataTypeId);
                    final int elementSize = baseReader.h5.getDataTypeSize(nativeDataTypeId);
                    final byte[] data = new byte[elementSize * spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public byte[] readArrayBlockWithOffset(final String objectPath, final int blockSize,
            final long offset) throws HDF5JavaException
    {
        baseReader.checkOpen();
        final ICallableWithCleanUp<byte[]> readCallable = new ICallableWithCleanUp<byte[]>()
            {
                @Override
                public byte[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, offset, blockSize, registry);
                    final long nativeDataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    checkNotAString(objectPath, nativeDataTypeId);
                    final int elementSize = baseReader.h5.getDataTypeSize(nativeDataTypeId);
                    final byte[] data = new byte[elementSize * spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public int readArrayToBlockWithOffset(final String objectPath, final byte[] buffer,
            final int blockSize, final long offset, final int memoryOffset)
            throws HDF5JavaException
    {
        if (blockSize + memoryOffset > buffer.length)
        {
            throw new HDF5JavaException("Buffer not large enough for blockSize and memoryOffset");
        }
        baseReader.checkOpen();
        final ICallableWithCleanUp<Integer> readCallable = new ICallableWithCleanUp<Integer>()
            {
                @Override
                public Integer call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, memoryOffset, offset,
                                    blockSize, registry);
                    final long nativeDataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    checkNotAString(objectPath, nativeDataTypeId);
                    final int elementSize = baseReader.h5.getDataTypeSize(nativeDataTypeId);
                    if ((blockSize + memoryOffset) * elementSize > buffer.length)
                    {
                        throw new HDF5JavaException(
                                "Buffer not large enough for blockSize and memoryOffset");
                    }
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, buffer);
                    return spaceParams.blockSize;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public Iterable<HDF5DataBlock<byte[]>> getArrayNaturalBlocks(final String dataSetPath)
            throws HDF5JavaException
    {
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5DataBlock<byte[]>>()
            {
                @Override
                public Iterator<HDF5DataBlock<byte[]>> iterator()
                {
                    return new Iterator<HDF5DataBlock<byte[]>>()
                        {
                            final HDF5NaturalBlock1DParameters.HDF5NaturalBlock1DIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5DataBlock<byte[]> next()
                            {
                                final long offset = index.computeOffsetAndSizeGetOffset();
                                final byte[] block =
                                        readArrayBlockWithOffset(dataSetPath,
                                                index.getBlockSize(), offset);
                                return new HDF5DataBlock<byte[]>(block, index.getAndIncIndex(),
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

    private void checkNotAString(final String objectPath, final long nativeDataTypeId)
    {
        final boolean isString =
                (baseReader.h5.getClassType(nativeDataTypeId) == H5T_STRING);
        if (isString)
        {
            throw new HDF5JavaException(objectPath + " cannot be a String.");
        }
    }

}
