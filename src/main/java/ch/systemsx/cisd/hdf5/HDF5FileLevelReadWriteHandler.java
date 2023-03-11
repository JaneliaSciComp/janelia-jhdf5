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

import java.io.Flushable;

import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.FileFormatVersionBounds;

/**
 * Implementation of {@link IHDF5FileLevelReadWriteHandler}.
 *
 * @author Bernd Rinn
 */
final class HDF5FileLevelReadWriteHandler extends HDF5FileLevelReadOnlyHandler implements IHDF5FileLevelReadWriteHandler
{
    private final HDF5BaseWriter baseWriter;

    HDF5FileLevelReadWriteHandler(HDF5BaseWriter baseWriter)
    {
        super(baseWriter);
        assert baseWriter != null;

        this.baseWriter = baseWriter;
    }

    // /////////////////////
    // Configuration
    // /////////////////////

    @Override
    public boolean isUseExtendableDataTypes()
    {
        return baseWriter.useExtentableDataTypes;
    }

    @Override
    public FileFormatVersionBounds getFileFormatVersionBounds()
    {
        return baseWriter.fileFormat;
    }

    // /////////////////////
    // File
    // /////////////////////

    @Override
    public void flush()
    {
        baseWriter.checkOpen();
        baseWriter.flush();
    }

    @Override
    public void flushSyncBlocking()
    {
        baseWriter.checkOpen();
        baseWriter.flushSyncBlocking();
    }

    @Override
    public boolean addFlushable(Flushable flushable)
    {
        return baseWriter.addFlushable(flushable);
    }

    @Override
    public boolean removeFlushable(Flushable flushable)
    {
        return baseWriter.removeFlushable(flushable);
    }

}
