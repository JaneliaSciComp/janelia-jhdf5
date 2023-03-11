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

import java.util.HashMap;
import java.util.Map;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

/**
 * A provider for {@link DirectoryIndex} objects.
 * 
 * @author Bernd Rinn
 */
class DirectoryIndexProvider implements IDirectoryIndexProvider
{
    private final Map<String, DirectoryIndex> cacheMap = new HashMap<String, DirectoryIndex>();

    private final IHDF5Reader reader;

    private final IErrorStrategy errorStrategy;

    DirectoryIndexProvider(IHDF5Reader reader, IErrorStrategy errorStrategy)
    {
        this.reader = reader;
        this.errorStrategy = errorStrategy;
    }

    @Override
    public synchronized IDirectoryIndex get(String normalizedGroupPath, boolean withLinkTargets)
    {
        final String nonEmptyGroupPath =
                (normalizedGroupPath.length() == 0) ? "/" : normalizedGroupPath;
        DirectoryIndex index = cacheMap.get(nonEmptyGroupPath);
        if (index == null)
        {
            index = new DirectoryIndex(reader, nonEmptyGroupPath, errorStrategy, withLinkTargets);
            cacheMap.put(nonEmptyGroupPath, index);
        } else if (withLinkTargets)
        {
            index.amendLinkTargets();
        }
        return index;
    }

    @Override
    public IErrorStrategy getErrorStrategy()
    {
        return errorStrategy;
    }

    @Override
    public synchronized void close() throws IOExceptionUnchecked
    {
        IOExceptionUnchecked exeptionOrNull = null;
        for (DirectoryIndex index : cacheMap.values())
        {
            try
            {
                index.close();
            } catch (IOExceptionUnchecked ex)
            {
                if (exeptionOrNull == null)
                {
                    exeptionOrNull = ex;
                }
            }
        }
        if (exeptionOrNull != null)
        {
            throw exeptionOrNull;
        }
    }

}
