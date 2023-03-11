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

import ch.systemsx.cisd.base.utilities.AbstractBuildAndEnvironmentInfo;


/**
 * The build and environment information for JHDF5.
 *
 * @author Franz-Josef Elmer
 */
public class BuildAndEnvironmentInfo extends AbstractBuildAndEnvironmentInfo
{
    private final static String BASE = "jhdf5";
    
    public final static BuildAndEnvironmentInfo INSTANCE = new BuildAndEnvironmentInfo();
    
    private BuildAndEnvironmentInfo()
    {
        super(BASE);
    }

    /**
     * Shows build and environment information on the console.
     */
    public static void main(String[] args)
    {
        System.out.println(INSTANCE);
    }

}
