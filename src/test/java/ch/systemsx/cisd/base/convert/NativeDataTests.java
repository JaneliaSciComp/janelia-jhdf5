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

package ch.systemsx.cisd.base.convert;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.convert.NativeData.ByteOrder;

/**
 * Test cases for {@link NativeData}.
 * 
 * @author Bernd Rinn
 */
public class NativeDataTests
{

    @DataProvider(name = "getOfs")
    private Object[][] getOfs()
    {
        return new Object[][]
            {
                { 0, 0 },
                { 0, 1 },
                { 0, 2 },
                { 0, 3 },
                { 1, 0 },
                { 1, 1 },
                { 1, 2 },
                { 1, 3 },
                { 2, 0 },
                { 2, 1 },
                { 2, 2 },
                { 2, 3 },
                { 3, 0 },
                { 3, 1 },
                { 3, 2 },
                { 3, 3 }, };
    }

    @Test(dataProvider = "getOfs")
    public void testIntToByteToInt(int sourceOfs, int targetOfs)
    {
        final int sizeOfTarget = 4;
        final int[] orignalArr = new int[]
            { -1, 17, 100000, -1000000 };
        final int[] iarr = new int[sourceOfs + orignalArr.length];
        System.arraycopy(orignalArr, 0, iarr, sourceOfs, orignalArr.length);
        final byte[] barr = new byte[iarr.length * sizeOfTarget + targetOfs];
        NativeData.copyIntToByte(iarr, sourceOfs, barr, targetOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        final int[] iarr2 = new int[(barr.length - targetOfs) / sizeOfTarget];
        NativeData.copyByteToInt(barr, targetOfs, iarr2, sourceOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        assertTrue(Arrays.equals(iarr, iarr2));
    }

    @Test
    public void testIntChangeByteOrderAndBack()
    {
        assertEquals(0, NativeData.changeByteOrder(NativeData.changeByteOrder(0)));
        assertEquals(1, NativeData.changeByteOrder(NativeData.changeByteOrder(1)));
        assertEquals(-1, NativeData.changeByteOrder(NativeData.changeByteOrder(-1)));
        assertEquals(Integer.MAX_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Integer.MAX_VALUE)));
        assertEquals(Integer.MIN_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Integer.MIN_VALUE)));
    }

    @Test
    public void testShortChangeByteOrderAndBack()
    {
        assertEquals((short) 0, NativeData.changeByteOrder(NativeData.changeByteOrder((short) 0)));
        assertEquals((short) 1, NativeData.changeByteOrder(NativeData.changeByteOrder((short) 1)));
        assertEquals((short) -1, NativeData.changeByteOrder(NativeData.changeByteOrder((short) -1)));
        assertEquals(Short.MAX_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Short.MAX_VALUE)));
        assertEquals(Short.MIN_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Short.MIN_VALUE)));
    }

    @Test
    public void testCharChangeByteOrderAndBack()
    {
        assertEquals((char) 0, NativeData.changeByteOrder(NativeData.changeByteOrder((char) 0)));
        assertEquals((char) 1, NativeData.changeByteOrder(NativeData.changeByteOrder((char) 1)));
        assertEquals((char) -1, NativeData.changeByteOrder(NativeData.changeByteOrder((char) -1)));
        assertEquals((char) Short.MAX_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder((char) Short.MAX_VALUE)));
        assertEquals((char) Short.MIN_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder((char) Short.MIN_VALUE)));
    }

    @Test
    public void testLongChangeByteOrderAndBack()
    {
        assertEquals(0, NativeData.changeByteOrder(NativeData.changeByteOrder(0L)));
        assertEquals(1, NativeData.changeByteOrder(NativeData.changeByteOrder(1L)));
        assertEquals(-1, NativeData.changeByteOrder(NativeData.changeByteOrder(-1L)));
        assertEquals(Long.MAX_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Long.MIN_VALUE)));
    }

    @Test
    public void testFloatChangeByteOrderAndBack()
    {
        assertEquals(0f, NativeData.changeByteOrder(NativeData.changeByteOrder(0f)));
        assertEquals(1f, NativeData.changeByteOrder(NativeData.changeByteOrder(1f)));
        assertEquals(-1f, NativeData.changeByteOrder(NativeData.changeByteOrder(-1f)));
        assertEquals(Float.MAX_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Float.MAX_VALUE)));
        assertEquals(Float.MIN_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Float.MIN_VALUE)));
        assertEquals(-Float.MAX_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(-Float.MAX_VALUE)));
        assertEquals(-Float.MIN_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(-Float.MIN_VALUE)));
    }

    @Test
    public void testDoubleChangeByteOrderAndBack()
    {
        assertEquals(0., NativeData.changeByteOrder(NativeData.changeByteOrder(0.)));
        assertEquals(1., NativeData.changeByteOrder(NativeData.changeByteOrder(1.)));
        assertEquals(-1., NativeData.changeByteOrder(NativeData.changeByteOrder(-1.)));
        assertEquals(Double.MAX_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Double.MAX_VALUE)));
        assertEquals(Double.MIN_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(Double.MIN_VALUE)));
        assertEquals(-Double.MAX_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(-Double.MAX_VALUE)));
        assertEquals(-Double.MIN_VALUE,
                NativeData.changeByteOrder(NativeData.changeByteOrder(-Double.MIN_VALUE)));
    }

    @Test(dataProvider = "getOfs")
    public void testLongToByteToLong(int sourceOfs, int targetOfs)
    {
        final int sizeOfTarget = 8;
        final long[] orignalArr = new long[]
            { -1, 17, 100000, -1000000 };
        final long[] iarr = new long[sourceOfs + orignalArr.length];
        System.arraycopy(orignalArr, 0, iarr, sourceOfs, orignalArr.length);
        final byte[] barr = new byte[iarr.length * sizeOfTarget + targetOfs];
        NativeData.copyLongToByte(iarr, sourceOfs, barr, targetOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        final long[] iarr2 = new long[(barr.length - targetOfs) / sizeOfTarget];
        NativeData.copyByteToLong(barr, targetOfs, iarr2, sourceOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        assertTrue(Arrays.equals(iarr, iarr2));
    }

    @Test(dataProvider = "getOfs")
    public void testShortToByteToShort(int sourceOfs, int targetOfs)
    {
        final int sizeOfTarget = 2;
        final short[] orignalArr = new short[]
            { -1, 17, 20000, (short) -50000 };
        final short[] iarr = new short[sourceOfs + orignalArr.length];
        System.arraycopy(orignalArr, 0, iarr, sourceOfs, orignalArr.length);
        final byte[] barr = new byte[iarr.length * sizeOfTarget + targetOfs];
        NativeData.copyShortToByte(iarr, sourceOfs, barr, targetOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        final short[] iarr2 = new short[(barr.length - targetOfs) / sizeOfTarget];
        NativeData.copyByteToShort(barr, targetOfs, iarr2, sourceOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        assertTrue(Arrays.equals(iarr, iarr2));
    }

    @Test(dataProvider = "getOfs")
    public void testCharToByteToChar(int sourceOfs, int targetOfs)
    {
        final int sizeOfTarget = 2;
        final char[] orignalArr = new char[]
            { 'c', ';', '\u0222', '\u1000' };
        final char[] iarr = new char[sourceOfs + orignalArr.length];
        System.arraycopy(orignalArr, 0, iarr, sourceOfs, orignalArr.length);
        final byte[] barr = new byte[iarr.length * sizeOfTarget + targetOfs];
        NativeData.copyCharToByte(iarr, sourceOfs, barr, targetOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        final char[] iarr2 = new char[(barr.length - targetOfs) / sizeOfTarget];
        NativeData.copyByteToChar(barr, targetOfs, iarr2, sourceOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        assertTrue(Arrays.equals(iarr, iarr2));
    }

    @Test(dataProvider = "getOfs")
    public void testFloatToByteToFloat(int sourceOfs, int targetOfs)
    {
        final int sizeOfTarget = 4;
        final float[] orignalArr = new float[]
            { -1, 17, 3.14159f, -1e6f };
        final float[] iarr = new float[sourceOfs + orignalArr.length];
        System.arraycopy(orignalArr, 0, iarr, sourceOfs, orignalArr.length);
        final byte[] barr = new byte[iarr.length * sizeOfTarget + targetOfs];
        NativeData.copyFloatToByte(iarr, sourceOfs, barr, targetOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        final float[] iarr2 = new float[(barr.length - targetOfs) / sizeOfTarget];
        NativeData.copyByteToFloat(barr, targetOfs, iarr2, sourceOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        assertTrue(Arrays.equals(iarr, iarr2));
    }

    @Test(dataProvider = "getOfs")
    public void testDoubleToByteToDouble(int sourceOfs, int targetOfs)
    {
        final int sizeOfTarget = 8;
        final double[] orignalArr = new double[]
            { -1, 17, 3.14159, -1e42 };
        final double[] iarr = new double[sourceOfs + orignalArr.length];
        System.arraycopy(orignalArr, 0, iarr, sourceOfs, orignalArr.length);
        final byte[] barr = new byte[iarr.length * sizeOfTarget + targetOfs];
        NativeData.copyDoubleToByte(iarr, sourceOfs, barr, targetOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        final double[] iarr2 = new double[(barr.length - targetOfs) / sizeOfTarget];
        NativeData.copyByteToDouble(barr, targetOfs, iarr2, sourceOfs, orignalArr.length,
                NativeData.ByteOrder.NATIVE);
        assertTrue(Arrays.equals(iarr, iarr2));
    }

    @Test
    public void testShortEndianConversion()
    {
        final short[] values = new short[]
            { 1, 2, 4, 8, 16, 256, 512 };
        final short[] convertedValuesExpected = new short[]
            { 1 << 8, 1 << 9, 1 << 10, 1 << 11, 1 << 12, 1, 2 };
        final short[] convertedValuesFound =
                NativeData.byteToShort(NativeData.shortToByte(values, ByteOrder.BIG_ENDIAN),
                        ByteOrder.LITTLE_ENDIAN);
        assertTrue(Arrays.equals(convertedValuesExpected, convertedValuesFound));
    }

    @Test
    public void testIntEndianConversion()
    {
        final int[] values = new int[]
            { 1, 2, 4, 8, 16, 256, 1 << 16 };
        final int[] convertedValuesExpected = new int[]
            { 1 << 24, 1 << 25, 1 << 26, 1 << 27, 1 << 28, 1 << 16, 256 };
        final int[] convertedValuesFound =
                NativeData.byteToInt(NativeData.intToByte(values, ByteOrder.BIG_ENDIAN),
                        ByteOrder.LITTLE_ENDIAN);
        assertTrue(Arrays.equals(convertedValuesExpected, convertedValuesFound));
    }

    @Test
    public void testLongEndianConversion()
    {
        final long[] values = new long[]
            { 1, 2, 4, 8, 16, 256, 1L << 16, 1L << 24 };
        final long[] convertedValuesExpected = new long[]
            { 1L << 56, 1L << 57, 1L << 58, 1L << 59, 1L << 60, 1L << 48, 1L << 40, 1L << 32 };
        final long[] convertedValuesFound =
                NativeData.byteToLong(NativeData.longToByte(values, ByteOrder.BIG_ENDIAN),
                        ByteOrder.LITTLE_ENDIAN);
        assertTrue(Arrays.equals(convertedValuesExpected, convertedValuesFound));
    }

    @Test
    public void testFloatLittleEndianRoundtrip()
    {
        final float[] values = new float[]
            { 1.1f, 2.2f, 3.3f, 1e-25f, 1e25f };
        final float[] convertedValuesFound =
                NativeData.byteToFloat(NativeData.floatToByte(values, ByteOrder.LITTLE_ENDIAN),
                        ByteOrder.LITTLE_ENDIAN);
        assertTrue(Arrays.equals(values, convertedValuesFound));
    }

    @Test
    public void testFloatBigEndianRoundtrip()
    {
        final float[] values = new float[]
            { 1.1f, 2.2f, 3.3f, 1e-25f, 1e25f };
        final float[] convertedValuesFound =
                NativeData.byteToFloat(NativeData.floatToByte(values, ByteOrder.BIG_ENDIAN),
                        ByteOrder.BIG_ENDIAN);
        assertTrue(Arrays.equals(values, convertedValuesFound));
    }

    @Test
    public void testDoubleLittleEndianRoundtrip()
    {
        final double[] values = new double[]
            { 1.1f, 2.2f, 3.3f, 1e-25f, 1e25f };
        final double[] convertedValuesFound =
                NativeData.byteToDouble(NativeData.doubleToByte(values, ByteOrder.LITTLE_ENDIAN),
                        ByteOrder.LITTLE_ENDIAN);
        assertTrue(Arrays.equals(values, convertedValuesFound));
    }

    @Test
    public void testDoubleBigEndianRoundtrip()
    {
        final double[] values = new double[]
            { 1.1, 2.2, 3.3, 1e-25, 1e25 };
        final double[] convertedValuesFound =
                NativeData.byteToDouble(NativeData.doubleToByte(values, ByteOrder.BIG_ENDIAN),
                        ByteOrder.BIG_ENDIAN);
        assertTrue(Arrays.equals(values, convertedValuesFound));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNPE()
    {
        NativeData.copyByteToLong(null, 0, null, 0, 0, ByteOrder.NATIVE);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testIOOB()
    {
        NativeData.copyByteToLong(new byte[] {}, -1, new long[] {}, 0, 0, ByteOrder.NATIVE);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testIOOB2()
    {
        NativeData.copyByteToLong(new byte[] {}, 0, new long[] {}, 10, 0, ByteOrder.NATIVE);
    }

    @Test
    public void testPlatformEndiness()
    {
        final double[] values = new double[]
            { 1.1, 2.2, 3.3, 1e-200, 1e200 };
        final double[] valuesLE =
                NativeData.byteToDouble(NativeData.doubleToByte(values, ByteOrder.LITTLE_ENDIAN),
                        ByteOrder.NATIVE);
        final double[] valuesBE =
                NativeData.byteToDouble(NativeData.doubleToByte(values, ByteOrder.BIG_ENDIAN),
                        ByteOrder.NATIVE);
        if (Arrays.equals(values, valuesLE))
        {
            assertEquals(NativeData.ByteOrder.LITTLE_ENDIAN, NativeData.getNativeByteOrder());
            assertFalse(Arrays.equals(values, valuesBE));
        }
        if (Arrays.equals(values, valuesBE))
        {
            assertEquals(NativeData.ByteOrder.BIG_ENDIAN, NativeData.getNativeByteOrder());
            assertFalse(Arrays.equals(values, valuesLE));
        }
    }

    @Test
    public void testFloatToByteNonNativeByteOrderPartialOutputArray()
    {
        final int sizeOfTarget = 4;
        final ByteOrder nonNativeByteOrder =
                (NativeData.getNativeByteOrder() == ByteOrder.LITTLE_ENDIAN) ? ByteOrder.BIG_ENDIAN
                        : ByteOrder.LITTLE_ENDIAN;
        final float[] iarr = new float[]
            { -1, 17, 3.14159f, -1e6f };
        final byte[] headerArray = new byte[]
            { 1, 2, 3, 4 };
        final byte[] trailerArray = new byte[]
            { 5, 6, 7, 8 };
        final byte[] barr =
                new byte[iarr.length * sizeOfTarget + headerArray.length + trailerArray.length];
        System.arraycopy(headerArray, 0, barr, 0, headerArray.length);
        System.arraycopy(trailerArray, 0, barr, headerArray.length + iarr.length * sizeOfTarget,
                trailerArray.length);
        NativeData.copyFloatToByte(iarr, 0, barr, headerArray.length, iarr.length,
                nonNativeByteOrder);
        final byte[] headerArray2 = ArrayUtils.subarray(barr, 0, headerArray.length);
        final byte[] trailerArray2 =
                ArrayUtils.subarray(barr, headerArray.length + iarr.length * sizeOfTarget,
                        barr.length);
        assertTrue(Arrays.equals(headerArray, headerArray2));
        assertTrue(Arrays.equals(trailerArray, trailerArray2));
    }

    private void afterClass()
    {
    }

    private void setUp()
    {
    }

    public static void main(String[] args) throws Throwable
    {
        System.out.println("Test class: " + NativeDataTests.class.getSimpleName());
        System.out.println();
        NativeData.ensureNativeLibIsLoaded();
        final NativeDataTests test = new NativeDataTests();
        try
        {
            for (Method m : NativeDataTests.class.getMethods())
            {
                final Test testAnnotation = m.getAnnotation(Test.class);
                if (testAnnotation == null)
                {
                    continue;
                }
                if (m.getParameterTypes().length == 0)
                {
                    System.out.println("Running " + m.getName());
                    test.setUp();
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
                if (m.getParameterTypes().length == 2
                        && "getOfs".equals(testAnnotation.dataProvider()))
                {
                    System.out.println("Running " + m.getName());
                    test.setUp();
                    try
                    {
                        final Object[][] testArgs = test.getOfs();
                        for (Object[] a : testArgs)
                        {
                            System.out.println(" Arguments: " + Arrays.toString(a));
                            m.invoke(test, a);
                        }
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
        } finally
        {
            test.afterClass();
        }
    }

}
