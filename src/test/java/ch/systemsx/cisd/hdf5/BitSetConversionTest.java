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

import java.util.Arrays;
import java.util.BitSet;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.mdarray.MDLongArray;

/**
 * Test cases for the BitSet conversion from / to storage form.
 * 
 * @author Bernd Rinn
 */
public class BitSetConversionTest
{
    private BitSet create(final Integer... indices)
    {
        final BitSet bs = new BitSet();
        for (final int index : indices)
        {
            bs.set(index);
        }
        return bs;
    }

    private BitSet[] create2D(final int length, final Integer... indices)
    {
        final BitSet[] bs = new BitSet[length];
        for (int i = 0; i < bs.length; ++i)
        {
            bs[i] = new BitSet();
            for (final int index : indices)
            {
                bs[i].set(index);
            }
        }
        return bs;
    }

    @DataProvider
    public Object[][] createBitSets()
    {
        final BitSet full4w = new BitSet();
        full4w.set(0, 256);

        return new Object[][]
            {
                { create() },
                { create(0) },
                { create(31) },
                { create(64) },
                { create(128) },
                { create(63, 191) },
                { create(64, 192) },
                { create(17, 88, 155) },
                { full4w }, };
    }

    @Test(dataProvider = "createBitSets")
    public void testBitSetRoundTrip(final BitSet bs)
    {
        final long[] bsArray = BitSetConversionUtils.toStorageForm(bs);
        final BitSet bs2 = BitSetConversionUtils.fromStorageForm(bsArray);
        assertEquals(bs, bs2);
    }

    @DataProvider
    public Object[][] createBitSetArrays()
    {
        final BitSet[] full4w = new BitSet[] { new BitSet(), new BitSet(), new BitSet(), };
        full4w[0].set(0, 256);
        full4w[1].set(0, 256);
        full4w[2].set(0, 256);

        return new Object[][]
            {
                { create2D(3) },
                { create2D(3, 0) },
                { create2D(3, 31) },
                { create2D(3, 64) },
                { create2D(3, 128) },
                { create2D(3, 63, 191) },
                { create2D(1, 64, 192) },
                { create2D(2, 17, 88, 155) },
                { full4w }, };
    }

    @Test(dataProvider = "createBitSetArrays")
    public void testBitSetArrayRoundTrip(final BitSet[] bs)
    {
        final int maxLength = BitSetConversionUtils.getMaxLength(bs);
        final long[] bsArray = BitSetConversionUtils.toStorageForm(bs, maxLength);
        final BitSet bs2[] = BitSetConversionUtils.fromStorageForm2D(new MDLongArray(bsArray, new int[] { maxLength, bs.length }));
        assertTrue(Arrays.equals(bs, bs2));
    }

}
