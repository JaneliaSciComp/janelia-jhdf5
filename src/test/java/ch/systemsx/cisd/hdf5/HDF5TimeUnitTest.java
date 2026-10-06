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

import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

/**
 * Test cases for {@link HDF5TimeUnit}.
 * 
 * @author Bernd Rinn
 */
public class HDF5TimeUnitTest
{

    @Test
    public void testConversion()
    {
        assertEquals(3, HDF5TimeUnit.HOURS.convert(10000L, HDF5TimeUnit.SECONDS));
        assertEquals(10000L, HDF5TimeUnit.MILLISECONDS.convert(10L, HDF5TimeUnit.SECONDS));
        assertEquals(120L, HDF5TimeUnit.MINUTES.convert(2L, HDF5TimeUnit.HOURS));
        assertEquals(2L * 3600 * 1000 * 1000, HDF5TimeUnit.MICROSECONDS.convert(2L,
                HDF5TimeUnit.HOURS));
        // Overflow
        assertEquals(Long.MIN_VALUE, HDF5TimeUnit.MICROSECONDS.convert(Long.MIN_VALUE / 24,
                HDF5TimeUnit.DAYS));
    }

    @Test
    public void testTypeVariant()
    {
        assertEquals(HDF5DataTypeVariant.TIME_DURATION_DAYS, HDF5TimeUnit.DAYS.getTypeVariant());
        assertEquals(HDF5DataTypeVariant.TIME_DURATION_HOURS, HDF5TimeUnit.HOURS.getTypeVariant());
        assertEquals(HDF5DataTypeVariant.TIME_DURATION_SECONDS, HDF5TimeUnit.SECONDS
                .getTypeVariant());
        assertEquals(HDF5DataTypeVariant.TIME_DURATION_MINUTES, HDF5TimeUnit.MINUTES
                .getTypeVariant());
        assertEquals(HDF5DataTypeVariant.TIME_DURATION_MILLISECONDS, HDF5TimeUnit.MILLISECONDS
                .getTypeVariant());
        assertEquals(HDF5DataTypeVariant.TIME_DURATION_MICROSECONDS, HDF5TimeUnit.MICROSECONDS
                .getTypeVariant());
    }
}
