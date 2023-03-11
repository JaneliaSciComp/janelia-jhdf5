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

import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT64;

import java.util.Date;
import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * The implementation of {@link IHDF5DateTimeReader}.
 * 
 * @author Bernd Rinn
 */
class HDF5DateTimeReader implements IHDF5DateTimeReader
{

    private final HDF5BaseReader baseReader;

    private final HDF5LongReader longReader;

    HDF5DateTimeReader(HDF5BaseReader baseReader, HDF5LongReader longReader)
    {
        assert baseReader != null;
        assert longReader != null;

        this.baseReader = baseReader;
        this.longReader = longReader;
    }

    @Override
    public long getAttrAsLong(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<Long> getAttributeRunnable = new ICallableWithCleanUp<Long>()
            {
                @Override
                public Long call(ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    final long attributeId =
                            baseReader.h5.openAttribute(objectId, attributeName, registry);
                    baseReader.checkIsTimeStamp(objectPath, attributeName, objectId, registry);
                    final long[] data =
                            baseReader.h5
                                    .readAttributeAsLongArray(attributeId, H5T_NATIVE_INT64, 1);
                    return data[0];
                }
            };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public Date getAttr(String objectPath, String attributeName)
    {
        return new Date(getAttrAsLong(objectPath, attributeName));
    }

    @Override
    public long[] getArrayAttrAsLong(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<long[]> getAttributeRunnable =
                new ICallableWithCleanUp<long[]>()
                    {
                        @Override
                        public long[] call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            baseReader.checkIsTimeStamp(objectPath, attributeName, objectId,
                                    registry);
                            return longReader.getLongArrayAttribute(objectId, attributeName, registry);
                        }
                    };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public MDLongArray getMDArrayAttrAsLong(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDLongArray> getAttributeRunnable =
                new ICallableWithCleanUp<MDLongArray>()
                    {
                        @Override
                        public MDLongArray call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            baseReader.checkIsTimeStamp(objectPath, attributeName, objectId,
                                    registry);
                            return longReader.getLongMDArrayAttribute(objectId, attributeName, registry);
                        }
                    };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public Date[] getArrayAttr(String objectPath, String attributeName)
    {
        final long[] timeStampArray = getArrayAttrAsLong(objectPath, attributeName);
        return timeStampsToDates(timeStampArray);
    }

    @Override
    public MDArray<Date> getMDArrayAttr(String objectPath, String attributeName)
    {
        final MDLongArray timeStampArray = getMDArrayAttrAsLong(objectPath, attributeName);
        return timeStampsToDates(timeStampArray);
    }

    @Override
    public boolean isTimeStamp(String objectPath, String attributeName) throws HDF5JavaException
    {
        final HDF5DataTypeVariant typeVariantOrNull =
                baseReader.tryGetTypeVariant(objectPath, attributeName);
        return typeVariantOrNull != null && typeVariantOrNull.isTimeStamp();
    }

    @Override
    public boolean isTimeStamp(final String objectPath) throws HDF5JavaException
    {
        final HDF5DataTypeVariant typeVariantOrNull = baseReader.tryGetTypeVariant(objectPath);
        return typeVariantOrNull != null && typeVariantOrNull.isTimeStamp();
    }

    @Override
    public long readTimeStamp(final String objectPath) throws HDF5JavaException
    {
        baseReader.checkOpen();
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<Long> readCallable = new ICallableWithCleanUp<Long>()
            {
                @Override
                public Long call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    baseReader.checkIsTimeStamp(objectPath, dataSetId, registry);
                    final long[] data = new long[1];
                    baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT64, data);
                    return data[0];
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public long[] readTimeStampArray(final String objectPath) throws HDF5JavaException
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<long[]> readCallable = new ICallableWithCleanUp<long[]>()
            {
                @Override
                public long[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    baseReader.checkIsTimeStamp(objectPath, dataSetId, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, registry);
                    final long[] data = new long[spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT64,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public long[] readTimeStampArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<long[]> readCallable = new ICallableWithCleanUp<long[]>()
            {
                @Override
                public long[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    baseReader.checkIsTimeStamp(objectPath, dataSetId, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, blockNumber * blockSize,
                                    blockSize, registry);
                    final long[] data = new long[spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT64,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public long[] readTimeStampArrayBlockWithOffset(final String objectPath, final int blockSize,
            final long offset)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<long[]> readCallable = new ICallableWithCleanUp<long[]>()
            {
                @Override
                public long[] call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    baseReader.checkIsTimeStamp(objectPath, dataSetId, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, offset, blockSize, registry);
                    final long[] data = new long[spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT64,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public Date readDate(final String objectPath) throws HDF5JavaException
    {
        return new Date(readTimeStamp(objectPath));
    }

    @Override
    public Date[] readDateArray(final String objectPath) throws HDF5JavaException
    {
        final long[] timeStampArray = readTimeStampArray(objectPath);
        return timeStampsToDates(timeStampArray);
    }

    @Override
    public MDLongArray readTimeStampMDArray(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDLongArray> readCallable = new ICallableWithCleanUp<MDLongArray>()
                    {
                        @Override
                        public MDLongArray call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                            baseReader.checkIsTimeStamp(objectPath, dataSetId, registry);
                            return longReader.readLongMDArray(dataSetId, registry);
                        }
                    };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public MDLongArray readTimeStampMDArrayBlock(final String objectPath, final int[] blockDimensions,
            final long[] blockNumber)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readTimeStampMDArrayBlockWithOffset(objectPath, blockDimensions, offset);
    }

    @Override
    public MDLongArray readTimeStampMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset)
    {
        assert objectPath != null;
        assert blockDimensions != null;
        assert offset != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDLongArray> readCallable = new ICallableWithCleanUp<MDLongArray>()
                    {
                        @Override
                        public MDLongArray call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                            baseReader.checkIsTimeStamp(objectPath, dataSetId, registry);
                            final DataSpaceParameters spaceParams =
                                    baseReader.getSpaceParameters(dataSetId, offset, blockDimensions, 
                                            registry);
                            final long[] dataBlock = new long[spaceParams.blockSize];
                            baseReader.h5.readDataSet(dataSetId, H5T_NATIVE_INT64, spaceParams.memorySpaceId,
                                    spaceParams.dataSpaceId, dataBlock);
                            return new MDLongArray(dataBlock, blockDimensions);
                        }
                    };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public int[] readToMDArrayWithOffset(final String objectPath, final MDLongArray array, final int[] memoryOffset)
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
                    baseReader.checkIsTimeStamp(objectPath, dataSetId, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getBlockSpaceParameters(dataSetId, memoryOffset, array
                                    .dimensions(), registry);
                    final long nativeDataTypeId =
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_INT64, registry);
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, 
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, array.
                            getAsFlatArray());
                    return MDArray.toInt(spaceParams.dimensions);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public int[] readToMDArrayBlockWithOffset(final String objectPath, final MDLongArray array,
            final int[] blockDimensions, final long[] offset, final int[] memoryOffset)
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
                    baseReader.checkIsTimeStamp(objectPath, dataSetId, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getBlockSpaceParameters(dataSetId, memoryOffset, array
                                    .dimensions(), offset, blockDimensions, registry);
                    final long nativeDataTypeId =
                            baseReader.getNativeDataTypeId(dataSetId, H5T_NATIVE_INT64, registry);
                    baseReader.h5.readDataSet(dataSetId, nativeDataTypeId, 
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, array
                            .getAsFlatArray());
                    return MDArray.toInt(spaceParams.dimensions);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public Iterable<HDF5DataBlock<long[]>> getTimeStampArrayNaturalBlocks(final String dataSetPath)
            throws HDF5JavaException
    {
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5DataBlock<long[]>>()
            {
                @Override
                public Iterator<HDF5DataBlock<long[]>> iterator()
                {
                    return new Iterator<HDF5DataBlock<long[]>>()
                        {
                            final HDF5NaturalBlock1DParameters.HDF5NaturalBlock1DIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5DataBlock<long[]> next()
                            {
                                final long offset = index.computeOffsetAndSizeGetOffset();
                                final long[] block =
                                        readTimeStampArrayBlockWithOffset(dataSetPath,
                                                index.getBlockSize(), offset);
                                return new HDF5DataBlock<long[]>(block, index.getAndIncIndex(),
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
    public Date[] readDateArrayBlock(String objectPath, int blockSize, long blockNumber)
    {
        final long[] timestampArray = readTimeStampArrayBlock(objectPath, blockSize, blockNumber);
        return timeStampsToDates(timestampArray);
    }

    @Override
    public Date[] readDateArrayBlockWithOffset(String objectPath, int blockSize, long offset)
    {
        final long[] timestampArray =
                readTimeStampArrayBlockWithOffset(objectPath, blockSize, offset);
        return timeStampsToDates(timestampArray);
    }

    @Override
    public Iterable<HDF5DataBlock<Date[]>> getDateArrayNaturalBlocks(final String dataSetPath)
            throws HDF5JavaException
    {
        final HDF5NaturalBlock1DParameters params =
                new HDF5NaturalBlock1DParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5DataBlock<Date[]>>()
            {
                @Override
                public Iterator<HDF5DataBlock<Date[]>> iterator()
                {
                    return new Iterator<HDF5DataBlock<Date[]>>()
                        {
                            final HDF5NaturalBlock1DParameters.HDF5NaturalBlock1DIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5DataBlock<Date[]> next()
                            {
                                final long offset = index.computeOffsetAndSizeGetOffset();
                                final long[] block =
                                        readTimeStampArrayBlockWithOffset(dataSetPath,
                                                index.getBlockSize(), offset);
                                return new HDF5DataBlock<Date[]>(timeStampsToDates(block),
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
    public Iterable<HDF5MDDataBlock<MDLongArray>> getTimeStampMDArrayNaturalBlocks(
            final String dataSetPath)
    {
        baseReader.checkOpen();
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5MDDataBlock<MDLongArray>>()
            {
                @Override
                public Iterator<HDF5MDDataBlock<MDLongArray>> iterator()
                {
                    return new Iterator<HDF5MDDataBlock<MDLongArray>>()
                        {
                            final HDF5NaturalBlockMDParameters.HDF5NaturalBlockMDIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5MDDataBlock<MDLongArray> next()
                            {
                                final long[] offset = index.computeOffsetAndSizeGetOffsetClone();
                                final MDLongArray data =
                                        readTimeStampMDArrayBlockWithOffset(dataSetPath,
                                                index.getBlockSize(), offset);
                                return new HDF5MDDataBlock<MDLongArray>(data,
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

    private static Date[] timeStampsToDates(final long[] timeStampArray)
    {
        assert timeStampArray != null;

        final Date[] dateArray = new Date[timeStampArray.length];
        for (int i = 0; i < dateArray.length; ++i)
        {
            dateArray[i] = new Date(timeStampArray[i]);
        }
        return dateArray;
    }

    private static MDArray<Date> timeStampsToDates(final MDLongArray timeStampArray)
    {
        assert timeStampArray != null;

        final long[] timeStampsFlat = timeStampArray.getAsFlatArray();
        final MDArray<Date> dateArray = new MDArray<Date>(Date.class, timeStampArray.dimensions());
        final Date[] datesFlat = dateArray.getAsFlatArray();
        for (int i = 0; i < datesFlat.length; ++i)
        {
            datesFlat[i] = new Date(timeStampsFlat[i]);
        }
        return dateArray;
    }

    @Override
    public MDArray<Date> readDateMDArray(String objectPath)
    {
        final MDLongArray timeStampArray = readTimeStampMDArray(objectPath);
        return timeStampsToDates(timeStampArray);
    }

    @Override
    public MDArray<Date> readDateMDArrayBlock(String objectPath, int[] blockDimensions,
            long[] blockNumber)
    {
        final MDLongArray timeStampArray =
                readTimeStampMDArrayBlock(objectPath, blockDimensions, blockNumber);
        return timeStampsToDates(timeStampArray);
    }

    @Override
    public MDArray<Date> readDateMDArrayBlockWithOffset(String objectPath, int[] blockDimensions,
            long[] offset)
    {
        final MDLongArray timeStampArray =
                readTimeStampMDArrayBlockWithOffset(objectPath, blockDimensions, offset);
        return timeStampsToDates(timeStampArray);
    }

    @Override
    public Iterable<HDF5MDDataBlock<MDArray<Date>>> getDateMDArrayNaturalBlocks(
            final String dataSetPath)
    {
        baseReader.checkOpen();
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(dataSetPath));

        return new Iterable<HDF5MDDataBlock<MDArray<Date>>>()
            {
                @Override
                public Iterator<HDF5MDDataBlock<MDArray<Date>>> iterator()
                {
                    return new Iterator<HDF5MDDataBlock<MDArray<Date>>>()
                        {
                            final HDF5NaturalBlockMDParameters.HDF5NaturalBlockMDIndex index =
                                    params.getNaturalBlockIndex();

                            @Override
                            public boolean hasNext()
                            {
                                return index.hasNext();
                            }

                            @Override
                            public HDF5MDDataBlock<MDArray<Date>> next()
                            {
                                final long[] offset = index.computeOffsetAndSizeGetOffsetClone();
                                final MDLongArray data =
                                        readTimeStampMDArrayBlockWithOffset(dataSetPath,
                                                index.getBlockSize(), offset);
                                return new HDF5MDDataBlock<MDArray<Date>>(timeStampsToDates(data),
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

}
