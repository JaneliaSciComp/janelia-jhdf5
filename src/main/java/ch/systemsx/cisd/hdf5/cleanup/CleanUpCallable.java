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

/**
 * A class that implements the logic of cleaning up a resource even in case of an exception but
 * re-throws an exception of the clean up procedure only when the main procedure didn't throw one.
 * <code>CleanUpRunner</code>s can be stacked.
 * <p>
 * <em>This is an internal implementation class that is not meant to be used by users of the library.</em>
 * 
 * @author Bernd Rinn
 */
public final class CleanUpCallable
{
    /**
     * Runs a {@link ICallableWithCleanUp} and ensures that all registered clean-ups are performed
     * afterwards.
     */
    public <T> T call(ICallableWithCleanUp<T> runnable)
    {
        final CleanUpRegistry registry = new CleanUpRegistry();
        boolean exceptionThrown = true;
        try
        {
            T result = runnable.call(registry);
            exceptionThrown = false;
            return result;
        } finally
        {
            registry.cleanUp(exceptionThrown);
        }
    }
}
