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

import static ch.systemsx.cisd.hdf5.HDF5BaseReader.REFERENCE_SIZE_IN_BYTES;
import static ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures.INT_NO_COMPRESSION;
import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_REF_OBJ;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * The implementation of {@link IHDF5ReferenceWriter}.
 * 
 * @author Bernd Rinn
 */
public class HDF5ReferenceWriter extends HDF5ReferenceReader implements IHDF5ReferenceWriter
{

    private final HDF5BaseWriter baseWriter;

    HDF5ReferenceWriter(HDF5BaseWriter baseWriter)
    {
        super(baseWriter);
        assert baseWriter != null;

        this.baseWriter = baseWriter;
    }

    // /////////////////////
    // Attributes
    // /////////////////////

    @Override
    public void setAttr(final String objectPath, final String name,
            final String referencedObjectPath)
    {
        assert objectPath != null;
        assert name != null;
        assert referencedObjectPath != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            final byte[] reference =
                                    baseWriter.h5.createObjectReference(baseWriter.fileId,
                                            referencedObjectPath);
                            if (baseWriter.useSimpleDataSpaceForAttributes)
                            {
                                final long dataSpaceId =
                                        baseWriter.h5.createSimpleDataSpace(new long[]
                                            { 1 }, registry);
                                baseWriter.setAttribute(objectPath, name, H5T_STD_REF_OBJ,
                                        H5T_STD_REF_OBJ, dataSpaceId, reference, registry);
                            } else
                            {
                                baseWriter.setAttribute(objectPath, name, H5T_STD_REF_OBJ,
                                        H5T_STD_REF_OBJ, -1, reference, registry);
                            }
                            return null; // Nothing to return.
                        }
                    };
        baseWriter.runner.call(addAttributeRunnable);
    }

    @Override
    public void setArrayAttr(final String objectPath, final String name,
            final String[] referencedObjectPaths)
    {
        assert objectPath != null;
        assert name != null;
        assert referencedObjectPaths != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> setAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] references =
                            baseWriter.h5.createObjectReferences(baseWriter.fileId,
                                    referencedObjectPaths);
                    if (baseWriter.useSimpleDataSpaceForAttributes)
                    {
                        final long dataSpaceId = baseWriter.h5.createSimpleDataSpace(new long[]
                            { references.length }, registry);
                        baseWriter.setAttribute(objectPath, name, H5T_STD_REF_OBJ, H5T_STD_REF_OBJ,
                                dataSpaceId, references);
                    } else
                    {
                        final long typeId =
                                baseWriter.h5.createArrayType(H5T_STD_REF_OBJ,
                                        referencedObjectPaths.length, registry);
                        baseWriter.setAttribute(objectPath, name, typeId, typeId, -1, references);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(setAttributeRunnable);
    }

    @Override
    public void setMDArrayAttr(final String objectPath, final String name,
            final MDArray<String> referencedObjectPaths)
    {
        assert objectPath != null;
        assert name != null;
        assert referencedObjectPaths != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> setAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] references =
                            baseWriter.h5.createObjectReferences(baseWriter.fileId,
                                    referencedObjectPaths.getAsFlatArray());
                    if (baseWriter.useSimpleDataSpaceForAttributes)
                    {
                        final long dataSpaceId =
                                baseWriter.h5.createSimpleDataSpace(
                                        referencedObjectPaths.longDimensions(), registry);
                        baseWriter.setAttribute(objectPath, name, H5T_STD_REF_OBJ, H5T_STD_REF_OBJ,
                                dataSpaceId, references);
                    } else
                    {
                        final long typeId =
                                baseWriter.h5.createArrayType(H5T_STD_REF_OBJ,
                                        referencedObjectPaths.dimensions(), registry);
                        baseWriter.setAttribute(objectPath, name, typeId, typeId, -1, references);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(setAttributeRunnable);
    }

    // /////////////////////
    // Data Sets
    // /////////////////////

    @Override
    public void write(String objectPath, String referencedObjectPath)
    {
        assert objectPath != null;
        assert referencedObjectPath != null;

        baseWriter.checkOpen();
        final byte[] reference =
                baseWriter.h5.createObjectReference(baseWriter.fileId, referencedObjectPath);
        baseWriter.writeScalar(objectPath, H5T_STD_REF_OBJ, H5T_STD_REF_OBJ, reference);
    }

    @Override
    public void writeArray(final String objectPath, final String[] referencedObjectPath)
    {
        writeArray(objectPath, referencedObjectPath, HDF5IntStorageFeatures.INT_NO_COMPRESSION);
    }

    @Override
    public void writeArray(final String objectPath, final String[] referencedObjectPaths,
            final HDF5IntStorageFeatures features)
    {
        assert referencedObjectPaths != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] references =
                            baseWriter.h5.createObjectReferences(baseWriter.fileId,
                                    referencedObjectPaths);
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath, H5T_STD_REF_OBJ, new long[]
                                { referencedObjectPaths.length }, REFERENCE_SIZE_IN_BYTES,
                                    features, registry);
                    H5Dwrite(dataSetId, H5T_STD_REF_OBJ, H5S_ALL, H5S_ALL, H5P_DEFAULT, references);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void createArray(final String objectPath, final int size)
    {
        createArray(objectPath, size, INT_NO_COMPRESSION);
    }

    @Override
    public void createArray(final String objectPath, final long size, final int blockSize)
    {
        createArray(objectPath, size, blockSize, INT_NO_COMPRESSION);
    }

    @Override
    public void createArray(final String objectPath, final int size,
            final HDF5IntStorageFeatures features)
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
                        baseWriter.createDataSet(objectPath, H5T_STD_REF_OBJ, features, new long[]
                            { 0 }, new long[]
                            { size }, REFERENCE_SIZE_IN_BYTES, registry);

                    } else
                    {
                        baseWriter.createDataSet(objectPath, H5T_STD_REF_OBJ, features, new long[]
                            { size }, null, REFERENCE_SIZE_IN_BYTES, registry);
                    }
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void createArray(final String objectPath, final long size, final int blockSize,
            final HDF5IntStorageFeatures features)
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
                    baseWriter.createDataSet(objectPath, H5T_STD_REF_OBJ, features, new long[]
                        { size }, new long[]
                        { blockSize }, REFERENCE_SIZE_IN_BYTES, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void writeArrayBlock(final String objectPath, final String[] referencedObjectPaths,
            final long blockNumber)
    {
        writeArrayBlockWithOffset(objectPath, referencedObjectPaths, referencedObjectPaths.length,
                referencedObjectPaths.length * blockNumber);
    }

    @Override
    public void writeArrayBlockWithOffset(final String objectPath,
            final String[] referencedObjectPaths, final int dataSize, final long offset)
    {
        assert objectPath != null;
        assert referencedObjectPaths != null;

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
                    final long[] references =
                            baseWriter.h5.createObjectReferences(baseWriter.fileId,
                                    referencedObjectPaths);
                    H5Dwrite(dataSetId, H5T_STD_REF_OBJ, memorySpaceId, dataSpaceId, H5P_DEFAULT,
                            references);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    @Override
    public void writeMDArray(final String objectPath, final MDArray<String> referencedObjectPaths)
    {
        writeMDArray(objectPath, referencedObjectPaths, HDF5IntStorageFeatures.INT_NO_COMPRESSION);
    }

    @Override
    public void writeMDArray(final String objectPath, final MDArray<String> referencedObjectPaths,
            final HDF5IntStorageFeatures features)
    {
        assert referencedObjectPaths != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] references =
                            baseWriter.h5.createObjectReferences(baseWriter.fileId,
                                    referencedObjectPaths.getAsFlatArray());
                    final long dataSetId =
                            baseWriter.getOrCreateDataSetId(objectPath, H5T_STD_REF_OBJ,
                                    referencedObjectPaths.longDimensions(),
                                    REFERENCE_SIZE_IN_BYTES, features, registry);
                    H5Dwrite(dataSetId, H5T_STD_REF_OBJ, H5S_ALL, H5S_ALL, H5P_DEFAULT, references);
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
                    if (features.requiresChunking())
                    {
                        final long[] nullDimensions = new long[dimensions.length];
                        baseWriter.createDataSet(objectPath, H5T_STD_REF_OBJ, features,
                                nullDimensions, MDAbstractArray.toLong(dimensions),
                                REFERENCE_SIZE_IN_BYTES, registry);
                    } else
                    {
                        baseWriter.createDataSet(objectPath, H5T_STD_REF_OBJ, features,
                                MDAbstractArray.toLong(dimensions), null, REFERENCE_SIZE_IN_BYTES,
                                registry);
                    }
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
                    baseWriter.createDataSet(objectPath, H5T_STD_REF_OBJ, features, dimensions,
                            MDAbstractArray.toLong(blockDimensions), REFERENCE_SIZE_IN_BYTES,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createRunnable);
    }

    @Override
    public void writeMDArrayBlock(final String objectPath,
            final MDArray<String> referencedObjectPaths, final long[] blockNumber)
    {
        assert blockNumber != null;

        final long[] dimensions = referencedObjectPaths.longDimensions();
        final long[] offset = new long[dimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            offset[i] = blockNumber[i] * dimensions[i];
        }
        writeMDArrayBlockWithOffset(objectPath, referencedObjectPaths, offset);
    }

    @Override
    public void writeMDArrayBlockWithOffset(final String objectPath,
            final MDArray<String> referencedObjectPaths, final long[] offset)
    {
        assert objectPath != null;
        assert referencedObjectPaths != null;
        assert offset != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long[] dimensions = referencedObjectPaths.longDimensions();
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
                    final long[] references =
                            baseWriter.h5.createObjectReferences(baseWriter.fileId,
                                    referencedObjectPaths.getAsFlatArray());
                    H5Dwrite(dataSetId, H5T_STD_REF_OBJ, memorySpaceId, dataSpaceId, H5P_DEFAULT,
                            references);
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
                    final long[] longBlockDimensions = MDAbstractArray.toLong(blockDimensions);
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
                    baseWriter.h5.setHyperslabBlock(memorySpaceId,
                            MDAbstractArray.toLong(memoryOffset), longBlockDimensions);
                    H5Dwrite(dataSetId, H5T_STD_REF_OBJ, memorySpaceId, dataSpaceId, H5P_DEFAULT,
                            data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }
}
