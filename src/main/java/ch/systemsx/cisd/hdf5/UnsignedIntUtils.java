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

import java.math.BigInteger;

import ch.systemsx.cisd.base.convert.NativeData;
import ch.systemsx.cisd.base.convert.NativeData.ByteOrder;

/**
 * Utilities for converting signed integers to unsigned integers and vice versa.
 * 
 * @author Bernd Rinn
 */
public final class UnsignedIntUtils
{
    private final static short MAX_UINT_8_P1 = 256;

    private final static short MAX_UINT_8 = MAX_UINT_8_P1 - 1;

    private final static int MAX_UINT_16_P1 = 65536;

    private final static int MAX_UINT_16 = MAX_UINT_16_P1 - 1;

    private final static long MAX_UINT_32_P1 = 4294967296L;

    private final static long MAX_UINT_32 = MAX_UINT_32_P1 - 1;

    private final static BigInteger MAX_UINT_64_P1 = new BigInteger("2").pow(64);

    private final static BigInteger MAX_UINT_64 = MAX_UINT_64_P1.subtract(BigInteger.ONE);

    /**
     * Converts <var>value</var> to <code>int8</code>.
     * 
     * @throws IllegalArgumentException if <var>value</var> is either negative or too large to fit
     *             into <code>uint8</code>.
     */
    public static byte toInt8(int value) throws IllegalArgumentException
    {
        if (value < 0 || value > MAX_UINT_8)
        {
            throw new IllegalArgumentException("Value " + Integer.toString(value)
                    + " cannot be converted to uint8.");
        }
        return (byte) value;
    }

    /**
     * Converts <var>value</var> as <code>int16</code>.
     * 
     * @throws IllegalArgumentException if <var>value</var> is either negative or too large to fit
     *             into <code>uint16</code>.
     */
    public static short toInt16(int value) throws IllegalArgumentException
    {
        if (value < 0 || value > MAX_UINT_16)
        {
            throw new IllegalArgumentException("Value " + Integer.toString(value)
                    + " cannot be converted to uint16.");
        }
        return (short) value;
    }

    /**
     * Converts <var>value</var> as <code>int32</code>.
     * 
     * @throws IllegalArgumentException if <var>value</var> is either negative or too large to fit
     *             into <code>uint32</code>.
     */
    public static int toInt32(long value) throws IllegalArgumentException
    {
        if (value < 0 || value > MAX_UINT_32)
        {
            throw new IllegalArgumentException("Value " + Long.toString(value)
                    + " cannot be converted to uint32.");
        }
        return (int) value;
    }

    /**
     * Converts <var>value</var> as <code>int64</code>.
     * 
     * @throws IllegalArgumentException if <var>value</var> is either negative or too large to fit
     *             into <code>uint64</code>.
     */
    public static long toInt64(BigInteger value) throws IllegalArgumentException
    {
        if (value.compareTo(BigInteger.ZERO) < 0 || value.compareTo(MAX_UINT_64) > 0)
        {
            throw new IllegalArgumentException("Value " + value.toString()
                    + " cannot be converted to uint64.");
        }
        return value.longValue();
    }

    /**
     * Converts <var>value</var> to <code>uint8</code>.
     */
    public static short toUint8(byte value)
    {
        return (short) (value < 0 ? MAX_UINT_8_P1 + value : value);
    }

    /**
     * Converts <var>value</var> to <code>uint16</code>.
     */
    public static int toUint16(short value)
    {
        return value < 0 ? MAX_UINT_16_P1 + value : value;
    }

    /**
     * Converts <var>value</var> to <code>uint32</code>.
     */
    public static long toUint32(int value)
    {
        return value < 0 ? MAX_UINT_32_P1 + value : value;
    }

    /**
     * Converts <var>value</var> to <code>uint64</code>.
     */
    public static BigInteger toUint64(long value)
    {
        return new BigInteger(1, NativeData.longToByte(new long[]
            { value }, ByteOrder.BIG_ENDIAN));
    }

}
