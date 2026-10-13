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

package ch.systemsx.cisd.base.namedthread;

/**
 * A {@link Thread} that knows its pool name.
 * 
 * @author Bernd Rinn
 */
public class PoolNameThread extends Thread
{
    private final String poolName;

    private final boolean addPoolName;

    public PoolNameThread(Runnable target, String poolName, boolean addPoolName)
    {
        super(target, poolName);
        this.poolName = poolName;
        this.addPoolName = addPoolName;
    }

    /**
     * Sets the thread's name to the <var>runnableName</var>, possibly adding the pool name if
     * <var>addPoolName</var> has been set to <code>true</code> in the constructor.
     */
    public void setRunnableName(String runnableName)
    {
        if (addPoolName)
        {
            setName(poolName + "::" + runnableName);
        } else
        {
            setName(runnableName);
        }
    }

    /** Clears the name of the runnable, setting the name of the thread to the pool name. */
    public void clearRunnableName()
    {
        setName(poolName);
    }
}
