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
 * A class that represents an opaque data type for a given HDF5 file and tag.
 *
 * @author Bernd Rinn
 */
public final class HDF5OpaqueType extends HDF5DataType
{

    private final String tag;
    
    HDF5OpaqueType(long fileId, long typeId, String tag, HDF5BaseReader baseReader)
    {
        super(fileId, typeId, typeId, baseReader);

        assert tag != null;
        
        this.tag = tag;
    }

    /**
     * Returns the tag of this opaque type.
     */
    public String getTag()
    {
        return tag;
    }

    @Override
    public String tryGetName()
    {
        return tag;
    }

}
