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

import static hdf.hdf5lib.H5.H5Aclose;
import static hdf.hdf5lib.H5.H5Acreate;
import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.H5.H5Sclose;
import static hdf.hdf5lib.H5.H5Screate_simple;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;
import static hdf.hdf5lib.HDF5Constants.H5T_IEEE_F32BE;
import static hdf.hdf5lib.HDF5Constants.H5T_IEEE_F32LE;
import static hdf.hdf5lib.HDF5Constants.H5T_IEEE_F64BE;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_DOUBLE;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import hdf.hdf5lib.HDFNativeData;;

/**
 * A writer for array type data sets.
 * 
 * @author Bernd Rinn
 */
public class HDF5ArrayTypeFloatWriter
{

    private final HDF5BaseWriter baseWriter;

    HDF5ArrayTypeFloatWriter(HDF5Writer writer)
    {
        baseWriter = writer.getBaseWriter();
    }

    public void writeFloatArrayBigEndian(final String objectPath, final float[] data,
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
                            baseWriter.getOrCreateDataSetId(objectPath, H5T_IEEE_F32BE, new long[]
                                { data.length }, 4, features, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_FLOAT, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    public void writeDoubleArrayBigEndian(final String objectPath, final double[] data,
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
                            baseWriter.getOrCreateDataSetId(objectPath, H5T_IEEE_F64BE, new long[]
                                { data.length }, 4, features, registry);
                    H5Dwrite(dataSetId, H5T_NATIVE_DOUBLE, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    public void writeFloatArrayArrayType(final String objectPath, final float[] data)
    {
        assert objectPath != null;
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long memoryTypeId =
                            baseWriter.h5.createArrayType(H5T_NATIVE_FLOAT, data.length, registry);
                    final long storageTypeId =
                            baseWriter.h5.createArrayType(H5T_IEEE_F32LE, data.length, registry);
                    final long dataSetId =
                            baseWriter.h5.createScalarDataSet(baseWriter.fileId, storageTypeId,
                                    objectPath, true, registry);
                    H5Dwrite(dataSetId, memoryTypeId, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    public void writeFloatArrayArrayType(final String objectPath, final MDFloatArray data)
    {
        assert objectPath != null;
        assert data != null;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long memoryTypeId =
                            baseWriter.h5.createArrayType(H5T_NATIVE_FLOAT, data.dimensions(),
                                    registry);
                    final long storageTypeId =
                            baseWriter.h5.createArrayType(H5T_IEEE_F32LE, data.dimensions(),
                                    registry);
                    final long dataSetId =
                            baseWriter.h5.createScalarDataSet(baseWriter.fileId, storageTypeId,
                                    objectPath, true, registry);
                    H5Dwrite(dataSetId, memoryTypeId, H5S_ALL, H5S_ALL, H5P_DEFAULT,
                            data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    public void writeFloat2DArrayArrayType1DSpace1d(final String objectPath, final MDFloatArray data)
    {
        assert objectPath != null;
        assert data != null;
        assert data.rank() == 2;

        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long memoryTypeId =
                            baseWriter.h5.createArrayType(H5T_NATIVE_FLOAT, data.dimensions()[1],
                                    registry);
                    final long storageTypeId =
                            baseWriter.h5.createArrayType(H5T_IEEE_F32LE, data.dimensions()[1], registry);
                    final long dataSetId =
                            baseWriter.h5.createDataSet(baseWriter.fileId, new long[]
                                { data.dimensions()[0] }, null, storageTypeId,
                                    HDF5FloatStorageFeatures.FLOAT_CONTIGUOUS, objectPath,
                                    HDF5StorageLayout.CONTIGUOUS, registry);
                    H5Dwrite(dataSetId, memoryTypeId, H5S_ALL, H5S_ALL, H5P_DEFAULT,
                            data.getAsFlatArray());
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    public void setFloatArrayAttributeDimensional(final String objectPath, final String name,
            final float[] value)
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
                    final long[] dimensions = new long[]
                        { value.length };
                    final long dataSpaceId =
                            H5Screate_simple(dimensions.length, dimensions, dimensions);
                    registry.registerCleanUp(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                H5Sclose(dataSpaceId);
                            }
                        });
                    final long objectId =
                            baseWriter.h5.openObject(baseWriter.fileId, objectPath, registry);
                    final long attributeId =
                            createAttribute(objectId, name, H5T_IEEE_F32LE, dataSpaceId, registry);
                    baseWriter.h5.writeAttribute(attributeId, H5T_NATIVE_FLOAT,
                            HDFNativeData.floatToByte(0, value.length, value));
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(addAttributeRunnable);
    }

    private long createAttribute(long locationId, String attributeName, long dataTypeId,
            long dataSpaceId, ICleanUpRegistry registry)
    {
        final long attributeId =
                H5Acreate(locationId, attributeName, dataTypeId, dataSpaceId, H5P_DEFAULT,
                        H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Aclose(attributeId);
                }
            });
        return attributeId;
    }

}
