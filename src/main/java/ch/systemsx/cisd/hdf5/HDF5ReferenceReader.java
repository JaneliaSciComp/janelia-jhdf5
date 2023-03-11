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
import static hdf.hdf5lib.HDF5Constants.H5T_REFERENCE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_REF_OBJ;

import java.util.Iterator;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import hdf.hdf5lib.HDF5Constants;

/**
 * A reader for HDF5 references.
 * 
 * @author Bernd Rinn
 */
public class HDF5ReferenceReader implements IHDF5ReferenceReader
{

    private final HDF5BaseReader baseReader;

    HDF5ReferenceReader(HDF5BaseReader baseReader)
    {
        assert baseReader != null;

        this.baseReader = baseReader;
    }

    // /////////////////////
    // Specific
    // /////////////////////

    @Override
    public String resolvePath(String reference)
    {
        assert reference != null;

        baseReader.checkOpen();
        if (reference.charAt(0) != '\0')
        {
            throw new HDF5JavaException(String.format("'%s' is not a reference.", reference));
        }
        return baseReader.h5.getReferencedObjectName(baseReader.fileId,
                Long.parseLong(reference.substring(1)));
    }

    private String refToStr(long reference)
    {
        return '\0' + Long.toString(reference);
    }

    private String[] refToStr(long[] references)
    {
        final String[] result = new String[references.length];
        for (int i = 0; i < references.length; ++i)
        {
            result[i] = '\0' + Long.toString(references[i]);
        }
        return result;
    }

    private void checkReference(final long dataTypeId, final String objectPath)
            throws HDF5JavaException
    {
        final boolean isReference = (baseReader.h5.getClassType(dataTypeId) == H5T_REFERENCE);
        if (isReference == false)
        {
            throw new HDF5JavaException("Dataset " + objectPath + " is not a reference.");
        }
    }

    private void checkRank1(final int[] arrayDimensions, final String objectPath)
    {
        if (arrayDimensions.length != 1)
        {
            throw new HDF5JavaException("Dataset " + objectPath
                    + ": array needs to be of rank 1, but is of rank " + arrayDimensions.length);
        }
    }

    private void checkRank1(final long[] arrayDimensions, final String objectPath)
    {
        if (arrayDimensions.length != 1)
        {
            throw new HDF5JavaException("Dataset " + objectPath
                    + ": array needs to be of rank 1, but is of rank " + arrayDimensions.length);
        }
    }

    // /////////////////////
    // Attributes
    // /////////////////////

    @Override
    public String getAttr(final String objectPath, final String attributeName)
    {
        return getAttr(objectPath, attributeName, true);
    }

    @Override
    public String getAttr(final String objectPath, final String attributeName,
            final boolean resolveName)
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
                    final long attributeId =
                            baseReader.h5.openAttribute(objectId, attributeName, registry);
                    final long dataTypeId =
                            baseReader.h5.getDataTypeForAttribute(attributeId, registry);
                    checkReference(dataTypeId, objectPath);
                    final long[] reference =
                            baseReader.h5.readAttributeAsLongArray(attributeId, dataTypeId, 1);
                    return resolveName ? baseReader.h5.getReferencedObjectName(attributeId,
                            reference[0]) : refToStr(reference[0]);
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    @Override
    public String[] getArrayAttr(final String objectPath,
            final String attributeName)
    {
        return getArrayAttr(objectPath, attributeName, true);
    }

    @Override
    public String[] getArrayAttr(final String objectPath,
            final String attributeName, final boolean resolveName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String[]> getAttributeRunnable =
                new ICallableWithCleanUp<String[]>()
                    {
                        @Override
                        public String[] call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            final long attributeId =
                                    baseReader.h5.openAttribute(objectId, attributeName, registry);
                            final long attributeTypeId =
                                    baseReader.h5.getDataTypeForAttribute(attributeId, registry);
                            final long memoryTypeId;
                            final int len;
                            if (baseReader.h5.getClassType(attributeTypeId) == H5T_ARRAY)
                            {
                                final long baseDataTypeId =
                                        baseReader.h5.getBaseDataType(attributeTypeId, registry);
                                checkReference(baseDataTypeId, objectPath);
                                final int[] arrayDimensions =
                                        baseReader.h5.getArrayDimensions(attributeTypeId);
                                checkRank1(arrayDimensions, objectPath);
                                len = arrayDimensions[0];
                                memoryTypeId =
                                        baseReader.h5.createArrayType(H5T_STD_REF_OBJ, len,
                                                registry);
                            } else
                            {
                                checkReference(attributeTypeId, objectPath);
                                final long[] arrayDimensions =
                                        baseReader.h5.getDataDimensionsForAttribute(attributeId,
                                                registry);
                                checkRank1(arrayDimensions, objectPath);
                                memoryTypeId = H5T_STD_REF_OBJ;
                                len = HDF5Utils.getOneDimensionalArraySize(arrayDimensions);
                            }
                            final long[] references =
                                    baseReader.h5.readAttributeAsLongArray(attributeId,
                                            memoryTypeId, len);
                            return resolveName ? baseReader.h5.getReferencedObjectNames(
                                    attributeId, references) : refToStr(references);
                        }
                    };
        return baseReader.runner.call(getAttributeRunnable);
    }

    @Override
    public MDArray<String> getMDArrayAttr(final String objectPath,
            final String attributeName)
    {
        return getMDArrayAttr(objectPath, attributeName, true);
    }

    @Override
    public MDArray<String> getMDArrayAttr(final String objectPath,
            final String attributeName, final boolean resolveName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<MDArray<String>> getAttributeRunnable =
                new ICallableWithCleanUp<MDArray<String>>()
                    {
                        @Override
                        public MDArray<String> call(ICleanUpRegistry registry)
                        {
                            try
                            {
                                final long objectId =
                                        baseReader.h5.openObject(baseReader.fileId, objectPath,
                                                registry);
                                final long attributeId =
                                        baseReader.h5.openAttribute(objectId, attributeName,
                                                registry);
                                final long attributeTypeId =
                                        baseReader.h5
                                                .getDataTypeForAttribute(attributeId, registry);
                                final long memoryTypeId;
                                final int[] arrayDimensions;
                                if (baseReader.h5.getClassType(attributeTypeId) == H5T_ARRAY)
                                {
                                    final long baseDataTypeId =
                                            baseReader.h5
                                                    .getBaseDataType(attributeTypeId, registry);
                                    checkReference(baseDataTypeId, objectPath);
                                    arrayDimensions =
                                            baseReader.h5.getArrayDimensions(attributeTypeId);
                                    memoryTypeId =
                                            baseReader.h5.createArrayType(H5T_STD_REF_OBJ,
                                                    arrayDimensions, registry);
                                } else
                                {
                                    checkReference(attributeTypeId, objectPath);
                                    arrayDimensions =
                                            MDAbstractArray.toInt(baseReader.h5
                                                    .getDataDimensionsForAttribute(attributeId,
                                                            registry));
                                    memoryTypeId = H5T_STD_REF_OBJ;
                                }
                                final int len;
                                len = MDAbstractArray.getLength(arrayDimensions);
                                final long[] references =
                                        baseReader.h5.readAttributeAsLongArray(attributeId,
                                                memoryTypeId, len);
                                return new MDArray<String>(
                                        resolveName ? baseReader.h5.getReferencedObjectNames(
                                                attributeId, references) : refToStr(references),
                                        arrayDimensions);
                            } catch (IllegalArgumentException ex)
                            {
                                throw new HDF5JavaException(ex.getMessage());
                            }
                        }
                    };
        return baseReader.runner.call(getAttributeRunnable);
    }

    // /////////////////////
    // Data Sets
    // /////////////////////

    @Override
    public String read(final String objectPath)
    {
        return read(objectPath, true);
    }

    @Override
    public String read(final String objectPath, final boolean resolveName)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String> readRunnable = new ICallableWithCleanUp<String>()
            {
                @Override
                public String call(ICleanUpRegistry registry)
                {
                    final long dataSetId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    final long objectReferenceDataTypeId =
                            baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                    checkReference(objectReferenceDataTypeId, objectPath);
                    final long[] reference = new long[1];
                    baseReader.h5.readDataSet(dataSetId, objectReferenceDataTypeId, reference);
                    return resolveName ? baseReader.h5.getReferencedObjectName(dataSetId,
                            reference[0]) : refToStr(reference[0]);
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    @Override
    public String[] readArray(final String objectPath)
    {
        return readArray(objectPath, true);
    }

    @Override
    public String[] readArray(final String objectPath, final boolean resolveName)
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
                    final long dataTypeId = baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                    final long[] references;
                    if (baseReader.h5.getClassType(dataTypeId) == H5T_REFERENCE)
                    {
                        final DataSpaceParameters spaceParams =
                                baseReader.getSpaceParameters(dataSetId, registry);
                        checkRank1(spaceParams.dimensions, objectPath);
                        references = new long[spaceParams.blockSize];
                        baseReader.h5.readDataSet(dataSetId, dataTypeId, spaceParams.memorySpaceId,
                                spaceParams.dataSpaceId, references);
                    } else if (baseReader.h5.getClassType(dataTypeId) == HDF5Constants.H5T_ARRAY
                            && baseReader.h5.getClassType(baseReader.h5.getBaseDataType(dataTypeId,
                                    registry)) == H5T_REFERENCE)
                    {
                        final long spaceId = baseReader.h5.createScalarDataSpace();
                        final int[] dimensions = baseReader.h5.getArrayDimensions(dataTypeId);
                        checkRank1(dimensions, objectPath);
                        final int len = dimensions[0];
                        references = new long[len];
                        final long memoryTypeId =
                                baseReader.h5.createArrayType(H5T_STD_REF_OBJ, len, registry);
                        baseReader.h5.readDataSet(dataSetId, memoryTypeId, spaceId, spaceId,
                                references);
                    } else
                    {
                        throw new HDF5JavaException("Dataset " + objectPath
                                + " is not a reference.");
                    }
                    return resolveName ? baseReader.h5.getReferencedObjectNames(baseReader.fileId,
                            references) : refToStr(references);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public String[] readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber)
    {
        return readArrayBlockWithOffset(objectPath, blockSize, blockNumber
                * blockSize, true);
    }

    @Override
    public String[] readArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber, final boolean resolveName)
    {
        return readArrayBlockWithOffset(objectPath, blockSize, blockNumber
                * blockSize, resolveName);
    }

    @Override
    public String[] readArrayBlockWithOffset(final String objectPath,
            final int blockSize, final long offset)
    {
        return readArrayBlockWithOffset(objectPath, blockSize, offset, true);
    }

    @Override
    public String[] readArrayBlockWithOffset(final String objectPath,
            final int blockSize, final long offset, final boolean resolveName)
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
                    final long[] references = new long[spaceParams.blockSize];
                    baseReader.h5.readDataSet(dataSetId, H5T_STD_REF_OBJ,
                            spaceParams.memorySpaceId, spaceParams.dataSpaceId, references);
                    return resolveName ? baseReader.h5.getReferencedObjectNames(baseReader.fileId,
                            references) : refToStr(references);
                }
            };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public MDArray<String> readMDArray(final String objectPath)
    {
        return readMDArray(objectPath, true);
    }

    @Override
    public MDArray<String> readMDArray(final String objectPath,
            final boolean resolveName)
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
                            final long dataTypeId =
                                    baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                            final long[] references;
                            final int[] dimensions;
                            if (baseReader.h5.getClassType(dataTypeId) == H5T_REFERENCE)
                            {
                                final DataSpaceParameters spaceParams =
                                        baseReader.getSpaceParameters(dataSetId, registry);
                                dimensions = MDAbstractArray.toInt(spaceParams.dimensions);
                                references = new long[spaceParams.blockSize];
                                baseReader.h5.readDataSet(dataSetId, dataTypeId,
                                        spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                        references);
                            } else if (baseReader.h5.getClassType(dataTypeId) == HDF5Constants.H5T_ARRAY
                                    && baseReader.h5.getClassType(baseReader.h5.getBaseDataType(
                                            dataTypeId, registry)) == H5T_REFERENCE)
                            {
                                final long spaceId = baseReader.h5.createScalarDataSpace();
                                dimensions = baseReader.h5.getArrayDimensions(dataTypeId);
                                final int len = MDAbstractArray.getLength(dimensions);
                                references = new long[len];
                                final long memoryTypeId =
                                        baseReader.h5.createArrayType(H5T_STD_REF_OBJ, len,
                                                registry);
                                baseReader.h5.readDataSet(dataSetId, memoryTypeId, spaceId,
                                        spaceId, references);
                            } else
                            {
                                throw new HDF5JavaException("Dataset " + objectPath
                                        + " is not a reference.");
                            }
                            final String[] referencedObjectNames =
                                    resolveName ? baseReader.h5.getReferencedObjectNames(
                                            baseReader.fileId, references) : refToStr(references);
                            return new MDArray<String>(referencedObjectNames, dimensions);
                        }
                    };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public MDArray<String> readMDArrayBlock(final String objectPath,
            final int[] blockDimensions, final long[] blockNumber)
    {
        return readMDArrayBlock(objectPath, blockDimensions, blockNumber, true);
    }

    @Override
    public MDArray<String> readMDArrayBlock(final String objectPath,
            final int[] blockDimensions, final long[] blockNumber, final boolean resolveName)
    {
        final long[] offset = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * blockDimensions[i];
        }
        return readMDArrayBlockWithOffset(objectPath, blockDimensions, offset,
                resolveName);
    }

    @Override
    public MDArray<String> readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset)
    {
        return readMDArrayBlockWithOffset(objectPath, blockDimensions, offset, true);
    }

    @Override
    public MDArray<String> readMDArrayBlockWithOffset(final String objectPath,
            final int[] blockDimensions, final long[] offset, final boolean resolveName)
    {
        assert objectPath != null;
        assert blockDimensions != null;
        assert offset != null;

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
                            final long[] referencesBlock = new long[spaceParams.blockSize];
                            baseReader.h5.readDataSet(dataSetId, H5T_STD_REF_OBJ,
                                    spaceParams.memorySpaceId, spaceParams.dataSpaceId,
                                    referencesBlock);
                            final String[] referencedObjectNamesBlock =
                                    resolveName ? baseReader.h5.getReferencedObjectNames(
                                            baseReader.fileId, referencesBlock)
                                            : refToStr(referencesBlock);
                            return new MDArray<String>(referencedObjectNamesBlock, blockDimensions);
                        }
                    };
        return baseReader.runner.call(readCallable);
    }

    @Override
    public Iterable<HDF5DataBlock<String[]>> getArrayNaturalBlocks(
            final String dataSetPath) throws HDF5JavaException
    {
        return getArrayNaturalBlocks(dataSetPath, true);
    }
    
    @Override
    public Iterable<HDF5DataBlock<String[]>> getArrayNaturalBlocks(
            final String dataSetPath, final boolean resolveName) throws HDF5JavaException
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
                                final String[] referencesBlock =
                                        readArrayBlockWithOffset(dataSetPath,
                                                index.getBlockSize(), offset, resolveName);
                                return new HDF5DataBlock<String[]>(referencesBlock,
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
    public Iterable<HDF5MDDataBlock<MDArray<String>>> getMDArrayNaturalBlocks(
            final String dataSetPath)
    {
        return getMDArrayNaturalBlocks(dataSetPath, true);
    }
    
    @Override
    public Iterable<HDF5MDDataBlock<MDArray<String>>> getMDArrayNaturalBlocks(
            final String dataSetPath, final boolean resolveName)
    {
        baseReader.checkOpen();
        final HDF5NaturalBlockMDParameters params =
                new HDF5NaturalBlockMDParameters(baseReader.getDataSetInformation(dataSetPath));

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
                                        readMDArrayBlockWithOffset(dataSetPath,
                                                index.getBlockSize(), offset, resolveName);
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

}
