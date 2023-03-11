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

import static hdf.hdf5lib.HDF5Constants.H5L_TYPE_EXTERNAL;
import static hdf.hdf5lib.HDF5Constants.H5L_TYPE_SOFT;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_DATASET;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_GROUP;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_NAMED_DATATYPE;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_NTYPES;
import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * The common super class of {@link HDF5LinkInformation} and {@link HDF5ObjectInformation}. 
 *
 * @author Bernd Rinn
 */
class HDF5CommonInformation
{

    protected final String path;
    protected final HDF5ObjectType type;

    static HDF5ObjectType objectTypeIdToObjectType(final int objectTypeId)
    {
        if (-1 == objectTypeId)
        {
            return HDF5ObjectType.NONEXISTENT;
        } else if (H5O_TYPE_GROUP == objectTypeId)
        {
            return HDF5ObjectType.GROUP;
        } else if (H5O_TYPE_DATASET == objectTypeId)
        {
            return HDF5ObjectType.DATASET;
        } else if (H5O_TYPE_NAMED_DATATYPE == objectTypeId)
        {
            return HDF5ObjectType.DATATYPE;
        } else if (objectTypeId >= H5O_TYPE_NTYPES)
        {
            final int linkTypeId = objectTypeId - H5O_TYPE_NTYPES;
            if (linkTypeId == H5L_TYPE_SOFT)
            {
                return HDF5ObjectType.SOFT_LINK;
            } else if (linkTypeId == H5L_TYPE_EXTERNAL)
            {
                return HDF5ObjectType.EXTERNAL_LINK;
            }
        }
        return HDF5ObjectType.OTHER;
    }

    HDF5CommonInformation(String path, HDF5ObjectType type)
    {
        assert path != null;
        assert type != null;

        this.path = path;
        this.type = type;
    }

    /**
     * @throws HDF5JavaException If the link does not exist.
     */
    public void checkExists() throws HDF5JavaException
    {
        if (exists() == false)
        {
            throw new HDF5JavaException("Link '" + getPath() + "' does not exist.");
        }
    }

    /**
     * Returns the path of this link in the HDF5 file.
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Returns the parent of the path of this link the HDF5 file. If this link corresponds to the
     * root, then this method will return the root ("/") itself.
     */
    public String getParentPath()
    {
        final int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex <= 0)
        {
            return "/";
        } else
        {
            return path.substring(0, lastSlashIndex);
        }
    }

    /**
     * Returns the name of this link in the HDF5 file (the path without the parent).
     */
    public String getName()
    {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Returns the type of this link.
     */
    public HDF5ObjectType getType()
    {
        return type;
    }

    /**
     * Returns <code>true</code>, if the link exists.
     */
    public boolean exists()
    {
        return HDF5ObjectType.exists(type);
    }

    /**
     * Returns <code>true</code>, if the link is a group.
     */
    public boolean isGroup()
    {
        return HDF5ObjectType.isGroup(type);
    }

    /**
     * Returns <code>true</code>, if the link is a data set.
     */
    public boolean isDataSet()
    {
        return HDF5ObjectType.isDataSet(type);
    }

    /**
     * Returns <code>true</code>, if the link is a data type.
     */
    public boolean isDataType()
    {
        return HDF5ObjectType.isDataType(type);
    }

}
