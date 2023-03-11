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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.systemsx.cisd.base.unix.Unix;
import ch.systemsx.cisd.base.unix.Unix.Stat;

/**
 * Cache for ID -> Name mapping.
 * 
 * @author Bernd Rinn
 */
final class IdCache
{
    /** Gid -> Group Name */
    private final Map<Integer, String> gidMap = Collections
            .synchronizedMap(new HashMap<Integer, String>());

    /** Uid -> User Name */
    private final Map<Integer, String> uidMap = Collections
            .synchronizedMap(new HashMap<Integer, String>());

    /**
     * Returns the name for the given <var>uid</var>.
     */
    String getUser(LinkRecord link, boolean numeric)
    {
        return getUser(link.getUid(), numeric);
    }

    /**
     * Returns the name for the given <var>uid</var>.
     */
    String getUser(Stat link, boolean numeric)
    {
        return getUser(link.getUid(), numeric);
    }

    String getUser(int uid, boolean numeric)
    {
        String userNameOrNull = uidMap.get(uid);
        if (userNameOrNull == null)
        {
            userNameOrNull =
                    (numeric == false && Unix.isOperational()) ? Unix.tryGetUserNameForUid(uid)
                            : null;
            if (userNameOrNull == null)
            {
                userNameOrNull = Integer.toString(uid);
            }
            uidMap.put(uid, userNameOrNull);
        }
        return userNameOrNull;
    }

    /**
     * Returns the name for the given <var>gid</var>.
     */
    String getGroup(LinkRecord link, boolean numeric)
    {
        return getGroup(link.getGid(), numeric);
    }

    /**
     * Returns the name for the given <var>gid</var>.
     */
    String getGroup(Stat link, boolean numeric)
    {
        return getGroup(link.getGid(), numeric);
    }

    /**
     * Returns the name for the given <var>gid</var>.
     */
    String getGroup(int gid, boolean numeric)
    {
        String groupNameOrNull = gidMap.get(gid);
        if (groupNameOrNull == null)
        {
            groupNameOrNull =
                    (numeric == false && Unix.isOperational()) ? Unix.tryGetGroupNameForGid(gid)
                            : null;
            if (groupNameOrNull == null)
            {
                groupNameOrNull = Integer.toString(gid);
            }
            gidMap.put(gid, groupNameOrNull);
        }
        return groupNameOrNull;
    }
}