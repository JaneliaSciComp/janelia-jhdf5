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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigInteger;

import org.testng.annotations.Test;

import ch.systemsx.cisd.base.convert.NativeData;
import ch.systemsx.cisd.base.convert.NativeData.ByteOrder;

/**
 * Test cases for{@link UnsignedIntUtils}.
 * 
 * @author Bernd Rinn
 */
public class UnsignedIntUtilsTest
{

    @Test
    public void testToInt64()
    {
        final BigInteger veryLarge = new BigInteger("2").pow(64).subtract(new BigInteger("100"));
        final long int64 = UnsignedIntUtils.toInt64(veryLarge);
        assertTrue(int64 < 0);
        final BigInteger veryLarge2 = new BigInteger(1, NativeData.longToByte(new long[]
            { int64 }, ByteOrder.BIG_ENDIAN));
        assertEquals(veryLarge, veryLarge2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testToInt64_Overflow()
    {
        final BigInteger tooLarge = new BigInteger("2").pow(64);
        UnsignedIntUtils.toInt64(tooLarge);
    }

    @Test
    public void testToUint32()
    {
        final long veryLarge = (1L << 32L) - 17;
        final int veryLargeInt = UnsignedIntUtils.toInt32(veryLarge);
        assertTrue(veryLargeInt < 0);
        assertEquals(veryLarge, UnsignedIntUtils.toUint32(veryLargeInt));
    }

    @Test
    public void testToUint16()
    {
        final int veryLarge = 40000;
        final short veryLargeShort = UnsignedIntUtils.toInt16(veryLarge);
        assertTrue(veryLargeShort < 0);
        assertEquals(veryLarge, UnsignedIntUtils.toUint16(veryLargeShort));
    }

    @Test
    public void testToUint8()
    {
        final short veryLarge = 199;
        final byte veryLargeByte = UnsignedIntUtils.toInt8(veryLarge);
        assertTrue(veryLargeByte < 0);
        assertEquals(veryLarge, UnsignedIntUtils.toUint8(veryLargeByte));
    }
}
