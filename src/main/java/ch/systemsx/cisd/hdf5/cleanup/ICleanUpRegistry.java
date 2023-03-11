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
 * A role that allows to register a clean-up method that is called regardless of whether an
 * exception occurs or not.
 * <p>
 * <em>This is an internal interface that is not meant to be used by users of the library.</em>
 * 
 * @author Bernd Rinn
 */
public interface ICleanUpRegistry
{

    /**
     * Register a clean-up to run when the main {@link Runnable} has been executed.
     */
    public void registerCleanUp(Runnable cleanUp);

}
