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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * Tests for {@link MatrixUtils}. 
 *
 * @author Bernd Rinn
 */
public class MatrixUtilsTest
{
    @Test
    public void testIncrementIdx()
    {
        int[] blockDims = new int[] { 2, 2, 2 }; 
        int[] offset = new int[] { 1, 2, 3 }; 
        int[] idx = offset.clone();
        assertTrue(MatrixUtils.incrementIdx(idx, blockDims, offset));
        assertTrue(Arrays.toString(idx), Arrays.equals(new int[] { 1, 2, 4 }, idx));
        assertTrue(MatrixUtils.incrementIdx(idx, blockDims, offset));
        assertTrue(Arrays.toString(idx), Arrays.equals(new int[] { 1, 3, 3 }, idx));
        assertTrue(MatrixUtils.incrementIdx(idx, blockDims, offset));
        assertTrue(Arrays.toString(idx), Arrays.equals(new int[] { 1, 3, 4 }, idx));
        assertTrue(MatrixUtils.incrementIdx(idx, blockDims, offset));
        assertTrue(Arrays.toString(idx), Arrays.equals(new int[] { 2, 2, 3 }, idx));
        assertTrue(MatrixUtils.incrementIdx(idx, blockDims, offset));
        assertTrue(Arrays.toString(idx), Arrays.equals(new int[] { 2, 2, 4 }, idx));
        assertTrue(MatrixUtils.incrementIdx(idx, blockDims, offset));
        assertTrue(Arrays.toString(idx), Arrays.equals(new int[] { 2, 3, 3 }, idx));
        assertTrue(MatrixUtils.incrementIdx(idx, blockDims, offset));
        assertTrue(Arrays.toString(idx), Arrays.equals(new int[] { 2, 3, 4 }, idx));
        assertFalse(MatrixUtils.incrementIdx(idx, blockDims, offset));
    }

}
