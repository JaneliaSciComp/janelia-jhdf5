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

import ch.systemsx.cisd.base.utilities.OSUtilities;
import ch.systemsx.cisd.hdf5.HDF5BaseReader.MDCImageGeneration;

/**
 * The configuration of the writer is done by chaining calls to configuration methods before calling
 * {@link #writer()}.
 * 
 * @author Bernd Rinn
 */
final class HDF5WriterConfigurator extends HDF5ReaderConfigurator implements
        IHDF5WriterConfigurator
{

    private boolean useExtentableDataTypes = true;

    private boolean overwriteFile = false;

    private boolean keepDataSetIfExists = false;

    private boolean useSimpleDataSpaceForAttributes = false;

    private FileFormatVersionBounds fileFormatVersionBounds = FileFormatVersionBounds.getDefault();
    
    private MDCImageGeneration mdcImageGeneration = MDCImageGeneration.KEEP_MDC_IMAGE;
    
    private String houseKeepingNameSuffix = "";

    // For Windows, use a blocking sync mode by default as otherwise the mandatory locks are up for
    // some surprises after the file has been closed.
    private SyncMode syncMode = OSUtilities.isWindows() ? SyncMode.SYNC_ON_FLUSH_BLOCK
            : SyncMode.SYNC_ON_FLUSH;

    public HDF5WriterConfigurator(File hdf5File)
    {
        super(hdf5File);
    }

    @Override
    public HDF5WriterConfigurator overwrite()
    {
        this.overwriteFile = true;
        return this;
    }

    @Override
    public HDF5WriterConfigurator keepDataSetsIfTheyExist()
    {
        this.keepDataSetIfExists = true;
        return this;
    }

    @Override
    public HDF5WriterConfigurator dontUseExtendableDataTypes()
    {
        this.useExtentableDataTypes = false;
        return this;
    }

    @Override
    public HDF5WriterConfigurator useSimpleDataSpaceForAttributes()
    {
        this.useSimpleDataSpaceForAttributes = true;
        return this;
    }

    @Override
    public HDF5WriterConfigurator fileFormat(FileFormatVersionBounds newFileFormat)
    {
        this.fileFormatVersionBounds = newFileFormat;
        return this;
    }

    @Override
    public HDF5WriterConfigurator syncMode(SyncMode newSyncMode)
    {
        this.syncMode = newSyncMode;
        return this;
    }

    @Override
    public IHDF5WriterConfigurator houseKeepingNameSuffix(@SuppressWarnings("hiding")
    String houseKeepingNameSuffix)
    {
        this.houseKeepingNameSuffix = houseKeepingNameSuffix;
        return this;
    }

    @Override
    public HDF5WriterConfigurator performNumericConversions()
    {
        return (HDF5WriterConfigurator) super.performNumericConversions();
    }

    @Override
    public HDF5WriterConfigurator generateMDCImage()
    {
        this.mdcImageGeneration = MDCImageGeneration.GENERATE_MDC_IMAGE;
        return this;
    }

    @Override
    public IHDF5WriterConfigurator noGenerateMDCImage()
    {
        this.mdcImageGeneration = MDCImageGeneration.NO_GENERATE_MDC_IMAGE;
        return this;
    }
    @Override
    public IHDF5WriterConfigurator keepMDCImage()
    {
        this.mdcImageGeneration = MDCImageGeneration.KEEP_MDC_IMAGE;
        return this;
    }

    @Override
    public HDF5WriterConfigurator useUTF8CharacterEncoding()
    {
        this.useUTF8CharEncoding = true;
        return this;
    }

    @Override
    public HDF5WriterConfigurator noAutoDereference()
    {

        return (HDF5WriterConfigurator) super.noAutoDereference();
    }

    @Override
    public IHDF5Writer writer()
    {
        if (readerWriterOrNull == null)
        {
            readerWriterOrNull =
                    new HDF5Writer(new HDF5BaseWriter(hdf5File, performNumericConversions,
                            useUTF8CharEncoding, autoDereference, fileFormatVersionBounds,
                            mdcImageGeneration, useExtentableDataTypes, overwriteFile, 
                            keepDataSetIfExists, useSimpleDataSpaceForAttributes, houseKeepingNameSuffix, 
                            syncMode));
        }
        return (HDF5Writer) readerWriterOrNull;
    }
}
