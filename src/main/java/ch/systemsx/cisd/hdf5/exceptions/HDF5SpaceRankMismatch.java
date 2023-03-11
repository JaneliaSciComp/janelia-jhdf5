/*
 * Copyright 2014 ETH Zuerich, SIS
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

package ch.systemsx.cisd.hdf5.exceptions;

import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * An exception for signaling that the data space of a data set has an unexpected rank.
 * 
 * @author Bernd Rinn
 */
public class HDF5SpaceRankMismatch extends HDF5JavaException
{
    private static final long serialVersionUID = 1L;

    private final int spaceRankExpected;

    private final int spaceRankFound;

    public HDF5SpaceRankMismatch(int spaceRankExpected, int spaceRankFound)
    {
        super("Data Set is expected to be of rank " + spaceRankExpected + " (rank="
                + spaceRankFound + ")");
        this.spaceRankExpected = spaceRankExpected;
        this.spaceRankFound = spaceRankFound;
    }

    public int getSpaceRankExpected()
    {
        return spaceRankExpected;
    }

    public int getSpaceRankFound()
    {
        return spaceRankFound;
    }

}
