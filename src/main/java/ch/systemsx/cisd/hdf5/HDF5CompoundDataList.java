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

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list to be used to store the members of a compound. 
 *
 * @author Bernd Rinn
 */
public class HDF5CompoundDataList extends ArrayList<Object>
{
    private static final long serialVersionUID = 8683452581122892189L;
    
    /**
     * @see ArrayList#ArrayList()
     */
    public HDF5CompoundDataList()
    {
        super();
    }

    /**
     * @see ArrayList#ArrayList(Collection)
     */
    public HDF5CompoundDataList(Collection<? extends Object> c)
    {
        super(c);
    }

    /**
     * @see ArrayList#ArrayList(int)
     */
    public HDF5CompoundDataList(int initialCapacity)
    {
        super(initialCapacity);
    }
}
