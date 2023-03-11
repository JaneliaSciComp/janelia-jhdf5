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

package ch.systemsx.cisd.hdf5.h5ar;

import java.io.File;
import java.io.IOException;

import hdf.hdf5lib.exceptions.HDF5Exception;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

/**
 * A processor for an archive entry.
 * 
 * @author Bernd Rinn
 */
interface IArchiveEntryProcessor
{
    /**
     * Performs any kind of processing of the given <var>link</var>.
     * 
     * @param dir The directory the current link is in.
     * @param path The path of the current link (including the link name)
     * @param link The link in the archive.
     * @param reader The HDF5 reader.
     * @param idCache The cached map of user and group ids to names.
     * @param errorStrategy The strategy object for errors.
     * @return <code>true</code> for continuing processing this <var>link</var>, <code>false</code>
     *         to skip over this entry (only relevant for directory links).
     */
    public boolean process(String dir, String path, LinkRecord link, IHDF5Reader reader,
            IdCache idCache, IErrorStrategy errorStrategy) throws IOException, HDF5Exception;

    /**
     * Performs any kind of post-processing of a directory. This is called after all files in the
     * directory have been processed.
     * 
     * @param dir The directory the current link is in.
     * @param path The path of the current link (including the link name)
     * @param link The link in the archive.
     * @param reader The HDF5 reader.
     * @param idCache The cached map of user and group ids to names.
     * @param errorStrategy The strategy object for errors.
     */
    public void postProcessDirectory(String dir, String path, LinkRecord link, IHDF5Reader reader,
            IdCache idCache, IErrorStrategy errorStrategy) throws IOException, HDF5Exception;

    /**
     * Creates the appropriate exception class for this processor.
     */
    public ArchiverException createException(String objectPath, String detailedMsg);

    /**
     * Creates the appropriate exception class for this processor.
     */
    public ArchiverException createException(String objectPath, HDF5Exception cause);

    /**
     * Creates the appropriate exception class for this processor.
     */
    public ArchiverException createException(String objectPath, RuntimeException cause);

    /**
     * Creates the appropriate exception class for this processor.
     */
    public ArchiverException createException(File file, IOException cause);

}
