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
 *
 * Modified in 2026 by the Howard Hughes Medical Institute for janelia-jhdf5.
 */

package ch.systemsx.cisd.base.mdarray;

import static org.testng.Assert.assertNotSame;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.testng.annotations.Test;


/**
 * Test cases for {@link MDAbstractArray}.
 * 
 * @author Bernd Rinn
 */
public class MDArrayTests
{
    @Test
    public void testGetLength()
    {
        assertEquals(0, MDAbstractArray.getLength(new int[]
            { 0 }, 0));
        assertEquals(1, MDAbstractArray.getLength(new int[]
            { 1 }, 0));
        assertEquals(2, MDAbstractArray.getLength(new int[]
            { 1 }, 2));
        assertEquals(15, MDAbstractArray.getLength(new int[]
            { 5, 3 }, 0));
        assertEquals(21, MDAbstractArray.getLength(new int[]
            { 5, 3 }, 7));
        assertEquals(15, MDAbstractArray.getLength(new int[]
            { 5, 3 }, 3));
        assertEquals(1, MDAbstractArray.getLength(new int[]
            { 1, 1, 1 }, 0));
        assertEquals(3, MDAbstractArray.getLength(new int[]
            { 1, 1, 1 }, 3));
        assertEquals(8, MDAbstractArray.getLength(new int[]
            { 2, 2, 2 }, 0));
        assertEquals(20, MDAbstractArray.getLength(new int[]
            { 2, 2, 2 }, 5));
        assertEquals(2, MDAbstractArray.getLength(new int[]
            { 1, 1, 2 }, 0));
        assertEquals(2, MDAbstractArray.getLength(new int[]
            { 1, 2, 1 }, 0));
        assertEquals(2, MDAbstractArray.getLength(new int[]
            { 2, 1, 1 }, 0));
        assertEquals(50, MDAbstractArray.getLength(new int[]
            { 10, 1, 5 }, 0));
        assertEquals(50, MDAbstractArray.getLength(new long[]
            { 10, 1, 5 }, 0));
    }

    @Test
    public void testToInt()
    {
        assertTrue(Arrays.equals(new int[]
            { 1, 2, 3 }, MDAbstractArray.toInt(new long[]
            { 1, 2, 3 })));
        assertTrue(Arrays.equals(new int[] {}, MDAbstractArray.toInt(new long[] {})));
    }

    @Test
    public void testComputeIndex()
    {
        MDArray<Object> array;
        array = new MDArray<Object>(Object.class, new int[]
            { 33 });
        assertEquals(17, array.computeIndex(new int[]
            { 17 }));
        assertTrue(Arrays.toString(array.computeReverseIndex(17)), Arrays.equals(new int[]
            { 17 }, array.computeReverseIndex(17)));
        array = new MDArray<Object>(Object.class, new int[]
            { 100, 10 });
        assertEquals(10 * 42 + 8, array.computeIndex(new int[]
            { 42, 8 }));
        assertTrue(Arrays.toString(array.computeReverseIndex(10 * 42 + 8)),
                Arrays.equals(new int[]
                    { 42, 8 }, array.computeReverseIndex(10 * 42 + 8)));
        array = new MDArray<Object>(Object.class, new int[]
            { 2, 7, 3 });
        assertEquals(3 * 7 * 1 + 3 * 3 + 2, array.computeIndex(new int[]
            { 1, 3, 2 }));
        assertTrue(Arrays.toString(array.computeReverseIndex(3 * 7 * 1 + 3 * 3 + 2)),
                Arrays.equals(new int[]
                    { 1, 3, 2 }, array.computeReverseIndex(3 * 7 * 1 + 3 * 3 + 2)));
    }

    @Test
    public void testComputeIndex2D()
    {
        MDArray<Object> array;
        array = new MDArray<Object>(Object.class, new int[]
            { 100, 10 });
        assertEquals(array.computeIndex(new int[]
            { 5, 8, }), array.computeIndex(5, 8));
        assertEquals(array.computeIndex(new int[]
            { 9, 1, }), array.computeIndex(9, 1));
        array = new MDArray<Object>(Object.class, new int[]
            { 101, 11 });
        assertEquals(array.computeIndex(new int[]
            { 5, 8, }), array.computeIndex(5, 8));
        assertEquals(array.computeIndex(new int[]
            { 9, 1, }), array.computeIndex(9, 1));
    }

    @Test
    public void testComputeIndex3()
    {
        MDArray<Object> array;
        array = new MDArray<Object>(Object.class, new int[]
            { 100, 10, 17 });
        assertEquals(array.computeIndex(new int[]
            { 5, 8, 16 }), array.computeIndex(5, 8, 16));
        assertEquals(array.computeIndex(new int[]
            { 9, 1, 5 }), array.computeIndex(9, 1, 5));
        array = new MDArray<Object>(Object.class, new int[]
            { 101, 11, 3 });
        assertEquals(array.computeIndex(new int[]
            { 5, 8, 0 }), array.computeIndex(5, 8, 0));
        assertEquals(array.computeIndex(new int[]
            { 9, 1, 2 }), array.computeIndex(9, 1, 2));
    }

    @Test
    public void testEmptyMatrix()
    {
        final MDFloatArray arr = new MDFloatArray(new float[0][0]);
        assertEquals(0, arr.dimensions()[0]);
        assertEquals(0, arr.dimensions()[1]);
    }

    @Test
    public void testChangeHyperRowCountIntArray()
    {
        final MDIntArray arr = new MDIntArray(new int[]
            { 2, 2 }, 3);
        assertEquals(2, arr.dimensions[0]);
        assertEquals(2, arr.dimensions[1]);
        arr.set(1, 0, 0);
        arr.set(2, 0, 1);
        arr.set(3, 1, 0);
        arr.set(4, 1, 1);

        final MDIntArray arr2 = new MDIntArray(arr.getCopyAsFlatArray(), arr.dimensions());
        assertTrue(arr2.equals(arr));

        arr.incNumberOfHyperRows(1);
        assertEquals(3, arr.dimensions[0]);
        assertEquals(1, arr.get(0, 0));
        assertEquals(2, arr.get(0, 1));
        assertEquals(3, arr.get(1, 0));
        assertEquals(4, arr.get(1, 1));
        arr.set(5, 2, 0);
        arr.set(6, 2, 1);
        arr.incNumberOfHyperRows(2);
        assertEquals(5, arr.dimensions[0]);
        assertEquals(1, arr.get(0, 0));
        assertEquals(2, arr.get(0, 1));
        assertEquals(3, arr.get(1, 0));
        assertEquals(4, arr.get(1, 1));
        assertEquals(5, arr.get(2, 0));
        assertEquals(6, arr.get(2, 1));
        arr.set(7, 3, 0);
        arr.set(8, 3, 1);
        arr.decNumberOfHyperRows(1);
        assertEquals(4, arr.dimensions[0]);
        assertEquals(1, arr.get(0, 0));
        assertEquals(2, arr.get(0, 1));
        assertEquals(3, arr.get(1, 0));
        assertEquals(4, arr.get(1, 1));
        assertEquals(5, arr.get(2, 0));
        assertEquals(6, arr.get(2, 1));
        assertEquals(7, arr.get(3, 0));
        assertEquals(8, arr.get(3, 1));
    }

    @Test
    public void testChangeHyperRowCountIntArrayFromZero()
    {
        final MDIntArray arr = new MDIntArray(new int[]
            { 0 });
        assertEquals(0, arr.size(0));
        arr.incNumberOfHyperRows(1);
        assertEquals(1, arr.size(0));
        arr.set(17, 0);
        assertEquals(17, arr.get(0));
        arr.incNumberOfHyperRows(1);
        arr.incNumberOfHyperRows(1);
        assertEquals(3, arr.size());
    }

    @Test
    public void testChangeHyperRowCountTArray()
    {
        final MDArray<Integer> arr = new MDArray<Integer>(Integer.class, new int[]
            { 2, 2 }, 3);
        assertEquals(2, arr.dimensions[0]);
        assertEquals(2, arr.dimensions[1]);
        arr.set(1, 0, 0);
        arr.set(2, 0, 1);
        arr.set(3, 1, 0);
        arr.set(4, 1, 1);
        arr.incNumberOfHyperRows(1);
        assertEquals(3, arr.dimensions[0]);
        assertEquals(1, (int) arr.get(0, 0));
        assertEquals(2, (int) arr.get(0, 1));
        assertEquals(3, (int) arr.get(1, 0));
        assertEquals(4, (int) arr.get(1, 1));
        arr.set(5, 2, 0);
        arr.set(6, 2, 1);
        arr.incNumberOfHyperRows(2);
        assertEquals(5, arr.dimensions[0]);
        assertEquals(1, (int) arr.get(0, 0));
        assertEquals(2, (int) arr.get(0, 1));
        assertEquals(3, (int) arr.get(1, 0));
        assertEquals(4, (int) arr.get(1, 1));
        assertEquals(5, (int) arr.get(2, 0));
        assertEquals(6, (int) arr.get(2, 1));
        arr.set(7, 3, 0);
        arr.set(8, 3, 1);
        arr.decNumberOfHyperRows(1);
        assertEquals(4, arr.dimensions[0]);
        assertEquals(1, (int) arr.get(0, 0));
        assertEquals(2, (int) arr.get(0, 1));
        assertEquals(3, (int) arr.get(1, 0));
        assertEquals(4, (int) arr.get(1, 1));
        assertEquals(5, (int) arr.get(2, 0));
        assertEquals(6, (int) arr.get(2, 1));
        assertEquals(7, (int) arr.get(3, 0));
        assertEquals(8, (int) arr.get(3, 1));
    }

    @Test
    public void testMDIntArrayIterator()
    {
        final int[] linArray = new int[120];
        for (int i = 0; i < linArray.length; ++i)
        {
            linArray[i] = i;
        }
        final MDIntArray array = new MDIntArray(linArray, new int[]
            { 2, 3, 4, 5 });
        for (MDIntArray.ArrayEntry e : array)
        {
            assertEquals(e.getLinearIndex(), e.getValue().intValue());
            assertEquals(e.getLinearIndex(), array.computeIndex(e.getIndex()));
        }
    }

    @Test
    public void testMDFloatArrayMatrix()
    {
        final float[][] matrix1 = new float[][]
            {
                { 1f, 2f, 3f, 4f },
                { 5f, 6f, 7f, 8f },
                { 9f, 10f, 11f, 12f } };
        final MDFloatArray array = new MDFloatArray(matrix1);
        assertEquals(2, array.rank());
        assertEquals(12, array.size());
        assertEquals(3, array.size(0));
        assertEquals(4, array.size(1));
        assertEquals(7f, array.get(1, 2));
        final float[][] matrix2 = array.toMatrix();
        assertEquals(matrix1.length, matrix2.length);
        for (int i = 0; i < matrix1.length; ++i)
        {
            assertTrue(Arrays.equals(matrix1[i], matrix2[i]));
        }
    }

    @Test
    public void testCloneMDArray()
    {
        final float[][] matrix1 = new float[][]
                {
                    { 1f, 2f, 3f, 4f },
                    { 5f, 6f, 7f, 8f },
                    { 9f, 10f, 11f, 12f } };
        final MDFloatArray array = new MDFloatArray(matrix1);
        final MDFloatArray array2 = array.clone();
        assertNotSame(array2.getAsFlatArray(), array.getAsFlatArray());
        assertNotSame(array2.dimensions, array.dimensions);
        assertEquals(array2.hyperRowLength, array.hyperRowLength);
        assertEquals(array2.capacityHyperRows, array.capacityHyperRows);
        assertEquals(array2.size, array.size);
        assertEquals(2, array2.rank());
        assertEquals(12, array2.size());
        assertEquals(3, array2.size(0));
        assertEquals(4, array2.size(1));
        assertEquals(7f, array2.get(1, 2));
        final float[][] matrix2 = array2.toMatrix();
        assertEquals(matrix1.length, matrix2.length);
        for (int i = 0; i < matrix1.length; ++i)
        {
            assertTrue(Arrays.equals(matrix1[i], matrix2[i]));
        }
    }

    public static void main(String[] args) throws Throwable
    {
        System.out.println("Test class: " + MDArrayTests.class.getSimpleName());
        System.out.println();
        final MDArrayTests test = new MDArrayTests();
        for (Method m : MDArrayTests.class.getMethods())
        {
            final Test testAnnotation = m.getAnnotation(Test.class);
            if (testAnnotation == null)
            {
                continue;
            }
            if (m.getParameterTypes().length == 0)
            {
                System.out.println("Running " + m.getName());
                try
                {
                    m.invoke(test);
                } catch (InvocationTargetException wrapperThrowable)
                {
                    final Throwable th = wrapperThrowable.getCause();
                    boolean exceptionFound = false;
                    for (Class<?> expectedExClazz : testAnnotation.expectedExceptions())
                    {
                        if (expectedExClazz == th.getClass())
                        {
                            exceptionFound = true;
                            break;
                        }
                    }
                    if (exceptionFound == false)
                    {
                        throw th;
                    }
                }
            }
        }
        System.out.println("Tests OK!");
    }

}
