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

import ch.systemsx.cisd.hdf5.HDF5BaseReader.MDCImageGeneration;

/**
 * If you want the reader to perform numeric conversions, call {@link #performNumericConversions()}
 * before calling {@link #reader()}.
 * 
 * @author Bernd Rinn
 */
class HDF5ReaderConfigurator implements IHDF5ReaderConfigurator
{

    protected final File hdf5File;

    protected boolean performNumericConversions;

    protected boolean useUTF8CharEncoding;

    protected boolean autoDereference = true;

    protected HDF5Reader readerWriterOrNull;
    
    HDF5ReaderConfigurator(File hdf5File)
    {
        assert hdf5File != null;

        this.hdf5File = hdf5File.getAbsoluteFile();
    }

    @Override
    public boolean platformSupportsNumericConversions()
    {
        // Note: code in here any known exceptions of platforms not supporting numeric conversions.
        return true;
    }

    @Override
    public HDF5ReaderConfigurator performNumericConversions()
    {
        if (platformSupportsNumericConversions())
        {
            this.performNumericConversions = true;
        }
        return this;
    }

    @Override
    public HDF5ReaderConfigurator noAutoDereference()
    {
        this.autoDereference = false;
        return this;
    }

    @Override
    public IHDF5Reader reader()
    {
        if (readerWriterOrNull == null)
        {
            readerWriterOrNull =
                    new HDF5Reader(new HDF5BaseReader(hdf5File, performNumericConversions,
                            autoDereference, IHDF5WriterConfigurator.FileFormatVersionBounds.getDefault(), 
                            MDCImageGeneration.NO_GENERATE_MDC_IMAGE, false, ""));
        }
        return readerWriterOrNull;
    }

}
