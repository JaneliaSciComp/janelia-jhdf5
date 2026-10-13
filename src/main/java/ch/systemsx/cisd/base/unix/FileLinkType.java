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

package ch.systemsx.cisd.base.unix;

import java.io.Serializable;

/**
 * The type of a link in the file system.
 * 
 * @author Bernd Rinn
 */
public enum FileLinkType implements Serializable
{
    REGULAR_FILE, DIRECTORY, SYMLINK, OTHER;

    /**
     * Returns <code>true</code> if the <var>linkMode</var> corresponds to a symbolic link.
     */
    static boolean isSymLink(long linkMode)
    {
        return linkMode == SYMLINK.ordinal();
    }
}