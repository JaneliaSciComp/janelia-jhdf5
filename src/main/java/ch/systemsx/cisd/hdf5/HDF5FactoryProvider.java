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

import java.io.File;

import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;
import hdf.hdf5lib.H5;

/**
 * Provides access to a factory for HDF5 readers and writers.
 * 
 * @author Bernd Rinn
 */
public final class HDF5FactoryProvider
{
    private static class HDF5Factory implements IHDF5Factory
    {

        @Override
        public IHDF5WriterConfigurator configure(File file)
        {
            return new HDF5WriterConfigurator(file);
        }

        @Override
        public IHDF5ReaderConfigurator configureForReading(File file)
        {
            return new HDF5ReaderConfigurator(file);
        }

        @Override
        public IHDF5Writer open(File file)
        {
            return new HDF5WriterConfigurator(file).writer();
        }

        @Override
        public IHDF5Reader openForReading(File file)
        {
            return new HDF5ReaderConfigurator(file).reader();
        }

        @Override
        public boolean isHDF5File(File file)
        {
            return H5.H5Fis_hdf5(file.getPath());
        }

        @Override
        public boolean hasMDCImage(File file)
        {
            return HDFHelper.H5Fhas_mdc_image(file.getPath());
        }
    }

    /**
     * The (only) instance of the factory.
     */
    private static IHDF5Factory factory = new HDF5Factory();

    private HDF5FactoryProvider()
    {
        // Not to be instantiated.
    }

    /**
     * Returns the {@link IHDF5Factory}. This is your access to creation of {@link IHDF5Reader} and
     * {@link IHDF5Writer} instances.
     */
    public static synchronized IHDF5Factory get()
    {
        return factory;
    }

    /**
     * Sets the {@link IHDF5Factory}. In normal operation this method is not used, but it is a hook
     * that can be used if you need to track or influence the factory's operation, for example for
     * mocking in unit tests.
     */
    public static synchronized void set(IHDF5Factory factory)
    {
        HDF5FactoryProvider.factory = factory;
    }

}
