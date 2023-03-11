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

import java.util.List;

import hdf.hdf5lib.exceptions.HDF5Exception;

import ch.systemsx.cisd.hdf5.IHDF5Writer;

/**
 * A class to delete paths from an <code>h5ar</code> archives.
 * 
 * @author Bernd Rinn
 */
class HDF5ArchiveDeleter
{
    private final IHDF5Writer hdf5Writer;

    private final IDirectoryIndexProvider indexProvider;
    
    private final IdCache idCache;

    public HDF5ArchiveDeleter(IHDF5Writer hdf5Writer, IDirectoryIndexProvider indexProvider, IdCache idCache)
    {
        this.hdf5Writer = hdf5Writer;
        this.indexProvider = indexProvider;
        this.idCache = idCache;
    }

    public HDF5ArchiveDeleter delete(List<String> hdf5ObjectPaths, IArchiveEntryVisitor entryVisitorOrNull)
    {
        for (String path : hdf5ObjectPaths)
        {
            final String normalizedPath = Utils.normalizePath(path);
            final String group = Utils.getParentPath(normalizedPath);
            final IDirectoryIndex index = indexProvider.get(group, false);
            try
            {
                final String name = Utils.getName(normalizedPath);
                LinkRecord link = index.tryGetLink(name);
                if (link == null)
                {
                    link = LinkRecord.tryReadFromArchive(hdf5Writer, normalizedPath);
                }
                if (link != null)
                {
                    hdf5Writer.delete(normalizedPath);
                    index.remove(name);
                    if (entryVisitorOrNull != null)
                    {
                        final ArchiveEntry entry = new ArchiveEntry(group, normalizedPath, link, idCache);
                        entryVisitorOrNull.visit(entry);
                    }
                }
            } catch (HDF5Exception ex)
            {
                indexProvider.getErrorStrategy().dealWithError(
                        new DeleteFromArchiveException(path, ex));
            }
        }
        return this;
    }

}
