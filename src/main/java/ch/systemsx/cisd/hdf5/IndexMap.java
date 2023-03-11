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

/**
 * A map for storing index to index value mapping.
 *
 * @author Bernd Rinn
 */
public class IndexMap extends HashMap<Integer, Long>
{
    private static final long serialVersionUID = 1L;
    
    /**
     * Bind <code>index</code> to <code>indexValue</code>.
     * 
     * @return The map itself (for chained calls).
     */
    public IndexMap bind(int index, long indexValue)
    {
        put(index, indexValue);
        return this;
    }

    /**
     * Bind <code>index</code> to <code>indexValue</code>.
     * 
     * @return The map itself (for chained calls).
     */
    public IndexMap bind(int index, int indexValue)
    {
        put(index, (long) indexValue);
        return this;
    }
}
