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

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

/**
 * A class to update the {@link DirectoryIndex} from files on the filesystem.
 * 
 * @author Bernd Rinn
 */
final class DirectoryIndexUpdater
{
    private final IDirectoryIndexProvider indexProvider;

    private final IErrorStrategy errorStrategy;

    DirectoryIndexUpdater(IDirectoryIndexProvider indexProvider)
    {
        this.indexProvider = indexProvider;
        this.errorStrategy = indexProvider.getErrorStrategy();
    }

    void updateIndicesOnThePath(String rootDir, File path, int crc32, boolean immediateGroupOnly)
            throws IOExceptionUnchecked
    {
        String groupPath =
                rootDir.endsWith("/") ? rootDir.substring(0, rootDir.length() - 1) : rootDir;
        final IDirectoryIndex index = indexProvider.get(groupPath, false);
        final LinkRecord linkOrNull = LinkRecord.tryCreate(path, errorStrategy);
        if (linkOrNull == null)
        {
            throw new IOExceptionUnchecked("Cannot get link information for path '" + path + "'.");
        }
        linkOrNull.setCrc32(crc32);
        index.updateIndex(linkOrNull);

        if (immediateGroupOnly == false)
        {
            final String pathPrefixOnFSOrNull = tryGetPathPrefix(groupPath, path.getAbsolutePath());
            String groupName = Utils.getName(groupPath);
            groupPath = Utils.getParentPath(groupPath);
            while (groupName.length() > 0)
            {
                updateIndex(pathPrefixOnFSOrNull, groupPath, groupName);
                groupName = Utils.getName(groupPath);
                groupPath = Utils.getParentPath(groupPath);
            }
        }
    }

    private void updateIndex(String pathPrefixOnFSOrNull, String groupPath, String groupName)
    {
        final IDirectoryIndex index = indexProvider.get(groupPath, false);
        if (pathPrefixOnFSOrNull == null)
        {
            index.updateIndex(new LinkRecord(groupName));
        } else
        {
            final File groupPathFile = new File(pathPrefixOnFSOrNull, groupName);
            index.updateIndex(LinkRecord.tryCreate(groupPathFile, errorStrategy));
        }
    }

    private String tryGetPathPrefix(String root, String filePath)
    {
        final String parentPath = Utils.getParentPath(filePath);
        if (parentPath.endsWith(root) == false)
        {
            return null;
        }
        final String pathPrefix = parentPath.substring(0, parentPath.length() - root.length());
        if (pathPrefix.length() == 0)
        {
            return "/";
        } else
        {
            return pathPrefix;
        }
    }

}
