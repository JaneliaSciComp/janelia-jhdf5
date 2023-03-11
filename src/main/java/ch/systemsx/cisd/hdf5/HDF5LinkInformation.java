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

package ch.systemsx.cisd.hdf5;

/**
 * Information about a link in an HDF5 file.
 * 
 * @author Bernd Rinn
 */
public final class HDF5LinkInformation extends HDF5CommonInformation
{
    static final HDF5LinkInformation ROOT_LINK_INFO =
            new HDF5LinkInformation("/", HDF5ObjectType.GROUP, null, null);

    private final String symbolicLinkTargetOrNull;
    
    private final String externalLinkFilenameOrNull;

    private final String externalLinkTargetOrNull;
    
    private HDF5LinkInformation(String path, HDF5ObjectType type, String linkFilenameOrNull, String linkTargetOrNull)
    {
        super(path, type);
        if (type == HDF5ObjectType.EXTERNAL_LINK && linkFilenameOrNull != null && linkTargetOrNull != null)
        {
            this.symbolicLinkTargetOrNull = "EXTERNAL::" + linkFilenameOrNull + "::" + linkTargetOrNull;
            this.externalLinkFilenameOrNull = linkFilenameOrNull;
            this.externalLinkTargetOrNull = linkTargetOrNull;
        } else
        {
            this.symbolicLinkTargetOrNull = linkTargetOrNull;
            this.externalLinkFilenameOrNull = null;
            this.externalLinkTargetOrNull = null;
        }
    }

    static HDF5LinkInformation create(String path, int typeId, String linkFilenameOrNull, String linkTargetOrNull)
    {
        final HDF5ObjectType type = objectTypeIdToObjectType(typeId);
        return new HDF5LinkInformation(path, type, linkFilenameOrNull, linkTargetOrNull);
    }

    /**
     * Returns the symbolic link target of this link, or <code>null</code>, if this link does not exist or is not a symbolic link.
     * <p>
     * Note that external links have a special format: They start with a prefix " <code>EXTERNAL::</code>", then comes the path of the external file
     * (beware that this part uses the native path separator, i.e. "\" on Windows). Finally, separated by "<code>::</code> ", the path of the link in
     * the external file is provided (this part always uses "/" as path separator).
     */
    public String tryGetSymbolicLinkTarget()
    {
        return symbolicLinkTargetOrNull;
    }

    /**
     * Returns the filename of an external link, or <code>null</code>, if this link does not exist or is not an external link.
     */
    public String tryGetExternalLinkFilename()
    {
        return externalLinkFilenameOrNull;
    }

    /**
     * Returns the external link target of this link, or <code>null</code>, if this link does not exist or is not an external link.
     */
    public String tryGetExternalLinkTarget()
    {
        return externalLinkTargetOrNull;
    }

    /**
     * Returns <code>true</code>, if the link is a soft link.
     */
    public boolean isSoftLink()
    {
        return HDF5ObjectType.isSoftLink(type);
    }

    /**
     * Returns <code>true</code>, if the link is an external link.
     */
    public boolean isExternalLink()
    {
        return HDF5ObjectType.isExternalLink(type);
    }

    /**
     * Returns <code>true</code>, if the link is either a soft link or an external link.
     */
    public boolean isSymbolicLink()
    {
        return HDF5ObjectType.isSymbolicLink(type);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final HDF5LinkInformation other = (HDF5LinkInformation) obj;
        if (path == null)
        {
            if (other.path != null)
            {
                return false;
            }
        } else if (path.equals(other.path) == false)
        {
            return false;
        }
        return true;
    }

}
