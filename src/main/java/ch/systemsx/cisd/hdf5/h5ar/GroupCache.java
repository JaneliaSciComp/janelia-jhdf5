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

import org.apache.commons.lang3.ArrayUtils;

import ch.systemsx.cisd.base.unix.Unix;
import ch.systemsx.cisd.base.unix.Unix.Group;
import ch.systemsx.cisd.base.unix.Unix.Password;

/**
 * Cache for group affiliations of the current user.
 * 
 * @author Bernd RInn
 */
class GroupCache
{
    private final Password userOrNull;

    /** Gid -> Is user member? */
    private final Map<Integer, Boolean> gidMap = new HashMap<Integer, Boolean>();

    GroupCache()
    {
        this.userOrNull = Unix.isOperational() ? Unix.tryGetUserByUid(Unix.getUid()) : null;
    }

    boolean isUserInGroup(int gid)
    {
        if (userOrNull == null)
        {
            return false;
        }
        final Boolean cached = gidMap.get(gid);
        if (cached != null)
        {
            return cached;
        }
        final Group groupOrNull = Unix.tryGetGroupByGid(gid);
        if (groupOrNull != null)
        {
            final int idx =
                    ArrayUtils.indexOf(groupOrNull.getGroupMembers(), userOrNull.getUserName());
            final Boolean found =
                    idx != ArrayUtils.INDEX_NOT_FOUND ? Boolean.TRUE : Boolean.FALSE;
            gidMap.put(gid, found);
            return found;
        } else
        {
            gidMap.put(gid, Boolean.FALSE);
            return false;
        }
    }
}