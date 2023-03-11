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

import java.io.Closeable;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;

/**
 * A provider for {@link DirectoryIndex} objects.
 * 
 * @author Bernd Rinn
 */
interface IDirectoryIndexProvider extends Closeable
{
    public IDirectoryIndex get(String normalizedGroupPath, boolean withLinkTargets);

    public IErrorStrategy getErrorStrategy();
 
    @Override
    public void close();
}