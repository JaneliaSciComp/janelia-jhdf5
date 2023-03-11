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

import hdf.hdf5lib.structs.H5O_info_t;

/**
 * Information about an object in an HDF5 file.
 * 
 * @author Bernd Rinn
 */
public final class HDF5ObjectInformation extends HDF5CommonInformation
{

    private final long fileNumber;

    private final long address;

    private final int referenceCount;

    private final long creationTime;

    private final long numberOfAttributes;

    HDF5ObjectInformation(String path, HDF5ObjectType objectType, H5O_info_t info)
    {
        super(path, objectType);
        this.fileNumber = info.fileno;
        this.address = info.addr;
        this.referenceCount = info.rc;
        this.creationTime = info.ctime;
        this.numberOfAttributes = info.num_attrs;
    }

    /**
     * Returns the file number that the object is in. Can be useful when external links are
     * involved.
     */
    public long getFileNumber()
    {
        return fileNumber;
    }

    /**
     * Returns the address of the object in the file. If the address of two links is the same, then
     * they point to the same object. Can be used to spot hard or soft links.
     */
    public long getAddress()
    {
        return address;
    }

    /**
     * Returns the number of references that point to this object. (Number of hard links that point
     * to the object).
     */
    public int getReferenceCount()
    {
        return referenceCount;
    }

    /**
     * Returns the time of creation of this object (as number of seconds since start of the epoch).
     * Note that this only works for data set, for groups, this will always return 0.
     */
    public long getCreationTime()
    {
        return creationTime;
    }

    /**
     * Returns the number of attributes that is object has.
     */
    public long getNumberOfAttributes()
    {
        return numberOfAttributes;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((int) address);
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
        final HDF5ObjectInformation other = (HDF5ObjectInformation) obj;
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
        if (other.address != address)
        {
            return false;
        }
        return true;
    }

}
