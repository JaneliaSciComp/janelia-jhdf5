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

import java.util.HashMap;
import java.util.Map;

/**
 * A map to be used to store the member data of a compound. 
 *
 * @author Bernd Rinn
 */
public class HDF5CompoundDataMap extends HashMap<String, Object>
{
    private static final long serialVersionUID = 362498820763181265L;

    /**
     * @see HashMap#HashMap()
     */
    public HDF5CompoundDataMap()
    {
        super();
    }

    /**
     * @see HashMap#HashMap(int, float)
     */
    public HDF5CompoundDataMap(int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    /**
     * @see HashMap#HashMap(int)
     */
    public HDF5CompoundDataMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * @see HashMap#HashMap(Map)
     */
    public HDF5CompoundDataMap(Map<? extends String, ? extends Object> m)
    {
        super(m);
    }
}
