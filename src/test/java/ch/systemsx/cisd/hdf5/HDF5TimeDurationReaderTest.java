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

import java.util.Arrays;

import org.testng.annotations.Test;

import ch.systemsx.cisd.base.mdarray.MDLongArray;

import static org.testng.AssertJUnit.*;

/**
 * Test cases for {@link HDF5TimeDurationReader}.
 * 
 * @author Bernd Rinn
 */
public class HDF5TimeDurationReaderTest
{

    @Test
    public void testConvertUnit()
    {
        MDLongArray array = new MDLongArray(new int[]
            { 5, 5, 5 });
        int[] ofs = new int[]
            { 1, 2, 1 };
        int[] dims = new int[]
            { 4, 3, 4 };
        Arrays.fill(array.getAsFlatArray(), 1);
        HDF5TimeDurationReader.convertUnit(array, HDF5TimeUnit.MINUTES, HDF5TimeUnit.SECONDS, dims,
                ofs);
        for (int x = 0; x < 5; ++x)
        {
            for (int y = 0; y < 5; ++y)
            {
                for (int z = 0; z < 5; ++z)
                {
                    final boolean converted =
                            (x >= ofs[0] && x < ofs[0] + dims[0] && y >= ofs[1]
                                    && y < ofs[1] + dims[1] && z >= ofs[2] && z < ofs[2] + dims[2]);
                    assertEquals(converted ? 60 : 1, array.get(x, y, z));
                }
            }
        }
    }
}
