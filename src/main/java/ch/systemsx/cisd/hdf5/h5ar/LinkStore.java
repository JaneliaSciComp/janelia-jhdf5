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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ch.systemsx.cisd.hdf5.IHDF5Reader;

/**
 * A store for {@link LinkRecord}s.
 * 
 * @author Bernd Rinn
 */
final class LinkStore implements Iterable<LinkRecord>
{
    private Map<String, LinkRecord> linkMap;

    private LinkRecord[] sortedArrayOrNull;
    
    private boolean linkMapPopulated = false;

    /**
     * Creates a new empty link list.
     */
    LinkStore()
    {
        this(new LinkRecord[0]);
    }

    /**
     * Creates a new link store and populates it with <var>entries</var>.
     * 
     * @param sortedEntries The links to populate the store with initially. The links are expected to be
     *            sorted.
     */
    LinkStore(LinkRecord[] sortedEntries)
    {
        this.sortedArrayOrNull = sortedEntries;
    }

    private Map<String, LinkRecord> getLinkMap()
    {
        if (linkMapPopulated == false && sortedArrayOrNull != null)
        {
            linkMap = new HashMap<String, LinkRecord>(sortedArrayOrNull.length);
            // Build the map lazily.
            for (LinkRecord entry : sortedArrayOrNull)
            {
                linkMap.put(entry.getLinkName(), entry);
            }
            linkMapPopulated = true;
        }
        return linkMap;
    }

    /**
     * Returns an array of the links in this store, in the order defined by
     * {@link LinkRecord#compareTo(LinkRecord)}.
     */
    public synchronized LinkRecord[] getLinkArray()
    {
        if (sortedArrayOrNull == null)
        {
            sortedArrayOrNull = getLinkMap().values().toArray(new LinkRecord[getLinkMap().size()]);
            Arrays.sort(sortedArrayOrNull);
        }
        return sortedArrayOrNull;
    }

    public synchronized void amendLinkTargets(IHDF5Reader reader, String groupPath)
    {
        for (LinkRecord link : getLinkMap().values())
        {
            link.addLinkTarget(reader, groupPath);
        }
    }

    /**
     * Returns the link with {@link LinkRecord#getLinkName()} equal to <var>name</var>, or
     * <code>null</code>, if there is no such link in the directory index.
     */
    public synchronized LinkRecord tryGetLink(String name)
    {
        return getLinkMap().get(name);
    }

    public boolean exists(String name)
    {
        return tryGetLink(name) != null;
    }

    /**
     * Returns <code>true</code> if this list is empty.
     */
    public synchronized boolean isEmpty()
    {
        return getLinkMap().isEmpty();
    }

    //
    // Iterable<Link>
    //

    /**
     * Returns an iterator over all links in the list, in the order defined by
     * {@link LinkRecord#compareTo(LinkRecord)}.
     */
    @Override
    public synchronized Iterator<LinkRecord> iterator()
    {
        final LinkRecord[] list = getLinkArray();
        for (LinkRecord link : list)
        {
            link.resetVerification();
        }
        return new ArrayList<LinkRecord>(Arrays.asList(list)).iterator();
    }

    /**
     * Updates the <var>entries</var> in the store.
     */
    public synchronized void update(LinkRecord entry)
    {
        getLinkMap().put(entry.getLinkName(), entry);
        sortedArrayOrNull = null;
    }

    /**
     * Updates the <var>entries</var> in the store.
     */
    public synchronized void update(LinkRecord[] entries)
    {
        for (LinkRecord entry : entries)
        {
            getLinkMap().put(entry.getLinkName(), entry);
        }
        if (entries.length > 0)
        {
            sortedArrayOrNull = null;
        }
    }

    /**
     * Updates the <var>entries</var> in the store.
     */
    public synchronized void update(Collection<LinkRecord> entries)
    {
        for (LinkRecord entry : entries)
        {
            getLinkMap().put(entry.getLinkName(), entry);
        }
        if (entries.size() > 0)
        {
            sortedArrayOrNull = null;
        }
    }

    /**
     * Removes <var>linkName</var> from the store.
     * 
     * @return <code>true</code>, if it was removed, <code>false</code>, if it couldn't be found.
     */
    public synchronized boolean remove(String linkName)
    {
        final boolean storeChanged = (getLinkMap().remove(linkName) != null);
        if (storeChanged)
        {
            sortedArrayOrNull = null;
        }
        return storeChanged;
    }

}
