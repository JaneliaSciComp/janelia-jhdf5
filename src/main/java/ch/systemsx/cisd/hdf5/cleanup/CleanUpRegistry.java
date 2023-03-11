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

package ch.systemsx.cisd.hdf5.cleanup;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that allows registering items for clean up and that allows to perform the clean up later.
 * <p>
 * <em>This is an internal implementation class that is not meant to be used by users of the library.</em>
 * 
 * @author Bernd Rinn
 */
public class CleanUpRegistry implements ICleanUpRegistry
{
    private final List<Runnable> cleanUpList = new ArrayList<Runnable>();

    /**
     * Creates a synchronized version of a {@link CleanUpRegistry}. 
     */
    public static CleanUpRegistry createSynchonized()
    {
        return new CleanUpRegistry()
            {
                @Override
                public synchronized void registerCleanUp(Runnable cleanUp)
                {
                    super.registerCleanUp(cleanUp);
                }

                @Override
                public synchronized void cleanUp(boolean suppressExceptions)
                {
                    super.cleanUp(suppressExceptions);
                }
            };
    }
    
    @Override
    public void registerCleanUp(Runnable cleanUp)
    {
        cleanUpList.add(cleanUp);
    }

    /**
     * Performs all clean-ups registered with {@link #registerCleanUp(Runnable)}.
     * 
     * @param suppressExceptions If <code>true</code>, all exceptions that happen during clean-up
     *            will be suppressed.
     */
    public void cleanUp(boolean suppressExceptions)
    {
        RuntimeException exceptionDuringCleanUp = null;
        for (int i = cleanUpList.size() - 1; i >= 0; --i)
        {
            final Runnable runnable = cleanUpList.get(i);
            try
            {
                runnable.run();
            } catch (RuntimeException ex)
            {
                if (suppressExceptions == false && exceptionDuringCleanUp == null)
                {
                    exceptionDuringCleanUp = ex;
                }
            }
        }
        cleanUpList.clear();
        if (exceptionDuringCleanUp != null)
        {
            throw exceptionDuringCleanUp;
        }
    }

}
