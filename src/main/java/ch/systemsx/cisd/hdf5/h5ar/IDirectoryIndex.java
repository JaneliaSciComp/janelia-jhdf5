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
import java.io.Flushable;
import java.util.Collection;
import java.util.Iterator;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

/**
 * Memory representation of the directory index stored in an HDF5 archive.
 * <p>
 * Can operate in read-only or read-write mode. The mode is automatically determined by the
 * <var>hdf5Reader</var> provided the constructor: If this is an instance of {@link IHDF5Writer},
 * the directory index will be read-write, otherwise read-only.
 * 
 * @author Bernd Rinn
 */
interface IDirectoryIndex extends Iterable<LinkRecord>, Closeable, Flushable
{

    /**
     * Amend the index with link targets. If the links targets have already been read, this method
     * is a noop.
     */
    public void amendLinkTargets();

    public boolean exists(String name);

    public boolean isDirectory(String name);

    /**
     * Returns the link with {@link LinkRecord#getLinkName()} equal to <var>name</var>, or
     * <code>null</code>, if there is no such link in the directory index.
     */
    public LinkRecord tryGetLink(String name);

    /**
     * Returns <code>true</code>, if this class has link targets read.
     */
    public boolean hasLinkTargets();

    @Override
    public Iterator<LinkRecord> iterator();

    /**
     * Writes the directory index to the archive represented by <var>hdf5Writer</var>.
     * <p>
     * Works on the list data structure.
     */
    @Override
    public void flush();

    /**
     * Add <var>entries</var> to the index. Any link that already exists in the index will be
     * replaced.
     */
    public void updateIndex(LinkRecord[] entries);

    /**
     * Add <var>entries</var> to the index. Any link that already exists in the index will be
     * replaced.
     */
    public void updateIndex(Collection<LinkRecord> entries);

    /**
     * Add <var>entry</var> to the index. If it already exists in the index, it will be replaced.
     */
    public void updateIndex(LinkRecord entry);

    /**
     * Removes <var>linkName</var> from the index, if it is in.
     * 
     * @return <code>true</code>, if <var>linkName</var> was removed.
     */
    public boolean remove(String linkName);

    public boolean addFlushable(Flushable flushable);
    
    public boolean removeFlushable(Flushable flushable);
    
    @Override
    public void close() throws IOExceptionUnchecked;

}