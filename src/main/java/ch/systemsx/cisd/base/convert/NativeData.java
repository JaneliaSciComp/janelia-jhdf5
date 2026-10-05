/****************************************************************************
 * NCSA HDF                                                                 *
 * National Computational Science Alliance                                   *
 * University of Illinois at Urbana-Champaign                               *
 * 605 E. Springfield, Champaign IL 61820                                   *
 *                                                                          *
 * Center for Information Sciences and Databases, ETH Zurich, Switzerland   *
 *                                                                          *
 * For conditions of distribution and use, see the accompanying             *
 * COPYING file.                                                            *
 *                                                                          *
 ****************************************************************************/

package ch.systemsx.cisd.base.convert;

import java.nio.ByteBuffer;

/**
 * This class deals with arrays of numbers, converting from numbers to bytes and bytes to numbers.
 * <p>
 * These routines are used by class <b>HDFArray</b> to pass data to and from the HDF5 library.
 * <p>
 * Methods copyXxxToByte() convert a Java array of primitive numbers (int, short, ...) to a Java
 * array of bytes. Methods copyByteToXxx() convert from a Java array of bytes into a Java array of
 * primitive numbers (int, short, ...)
 * <p>
 * Variant interfaces convert only a sub-array.
 * <p>
 * Implemented purely in terms of {@link ByteBuffer}'s own endianness-aware bulk get/put, rather
 * than the native library the original cisd:base implementation preferred when available: every
 * platform this now needs to run on already supports {@link ByteBuffer} natively, so there is no
 * remaining reason to special-case a native path.
 */
public class NativeData
{
    /** Size of a <code>short</code> value in <code>byte</code>s. */
    public final static int SHORT_SIZE = 2;

    /** Size of a <code>char</code> value in <code>byte</code>s. */
    public final static int CHAR_SIZE = 2;

    /** Size of an <code>int</code> value in <code>byte</code>s. */
    public final static int INT_SIZE = 4;

    /** Size of a <code>long</code> value in <code>byte</code>s. */
    public final static int LONG_SIZE = 8;

    /** Size of a <code>float</code> value in <code>byte</code>s. */
    public final static int FLOAT_SIZE = 4;

    /** Size of a <code>double</code> value in <code>byte</code>s. */
    public final static int DOUBLE_SIZE = 8;

    /** Byte Order enumeration. */
    public enum ByteOrder
    {
        /** <code>byte[]</code> is in native byte order (that is: don't change byte order) */
        NATIVE(java.nio.ByteOrder.nativeOrder()),
        /** <code>byte[]</code> is in little endian byte order */
        LITTLE_ENDIAN(java.nio.ByteOrder.LITTLE_ENDIAN),
        /** <code>byte[]</code> is in big endian byte order */
        BIG_ENDIAN(java.nio.ByteOrder.BIG_ENDIAN);

        private final java.nio.ByteOrder nioByteOrder;

        ByteOrder(java.nio.ByteOrder nioByteOrder)
        {
            this.nioByteOrder = nioByteOrder;
        }

        java.nio.ByteOrder getNioByteOrder()
        {
            return nioByteOrder;
        }

        static ByteOrder getNativeByteOrder()
        {
            return NATIVE.nioByteOrder.equals(LITTLE_ENDIAN.nioByteOrder) ? LITTLE_ENDIAN
                    : BIG_ENDIAN;
        }
    }

    //
    // Public
    //

    /** No-op: kept only for source compatibility with callers of the original cisd:base API. */
    public static void ensureNativeLibIsLoaded()
    {
    }

    /**
     * Always returns <code>false</code>: this implementation has no native backing at all. Kept
     * only for source compatibility with callers of the original cisd:base API.
     */
    public static boolean isUseNativeLib()
    {
        return false;
    }

    /**
     * Returns the native byte order of the host running this JRE.
     */
    public static ByteOrder getNativeByteOrder()
    {
        return ByteOrder.getNativeByteOrder();
    }

    /**
     * Changes the byte order of the bytes constituting <var>s</var>.
     */
    public static short changeByteOrder(short s)
    {
        return (short) ((s << 8) | ((s >> 8) & 0xff));
    }

    /**
     * Changes the byte order of the bytes constituting <var>c</var>.
     */
    public static char changeByteOrder(char c)
    {
        return (char) ((c << 8) | ((c >> 8) & 0xff));
    }

    /**
     * Changes the byte order of the bytes constituting <var>i</var>.
     */
    public static int changeByteOrder(int i)
    {
        return ((changeByteOrder((short) i) << 16) | (changeByteOrder((short) (i >> 16)) & 0xffff));
    }

    /**
     * Changes the byte order of the bytes constituting <var>f</var>.
     */
    public static float changeByteOrder(float f)
    {
        return Float.intBitsToFloat(changeByteOrder(Float.floatToRawIntBits(f)));
    }

    /**
     * Changes the byte order of the bytes constituting <var>l</var>.
     */
    public static long changeByteOrder(long l)
    {
        return (((long) changeByteOrder((int) (l)) << 32) | (changeByteOrder((int) (l >> 32)) & 0xffffffffL));
    }

    /**
     * Changes the byte order of the bytes constituting <var>d</var>.
     */
    public static double changeByteOrder(double d)
    {
        return Double.longBitsToDouble(changeByteOrder(Double.doubleToRawLongBits(d)));
    }

    /**
     * Copies a range from an array of <code>int</code> into an array of <code>byte</code>.
     * 
     * @param inData The input array of <code>int</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>int</code> to
     *            start
     * @param outData The output array of <code>byte</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>byte</code> to
     *            start
     * @param len The number of <code>int</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyIntToByte(int[] inData, int inStart, byte[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(outData, outStart, len * INT_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asIntBuffer().put(inData, inStart, len);
    }

    /**
     * Copies a range from an array of <code>byte</code> into an array of <code>int</code>.
     * 
     * @param inData The input array of <code>byte</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>byte</code> to
     *            start
     * @param outData The output array of <code>int</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>int</code> to
     *            start
     * @param len The number of <code>int</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyByteToInt(byte[] inData, int inStart, int[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(inData, inStart, len * INT_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asIntBuffer().get(outData, outStart, len);
    }

    /**
     * Copies a range from an array of <code>long</code> into an array of <code>byte</code>.
     * 
     * @param inData The input array of <code>long</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>long</code> to
     *            start
     * @param outData The output array of <code>byte</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>byte</code> to
     *            start
     * @param len The number of <code>long</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyLongToByte(long[] inData, int inStart, byte[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(outData, outStart, len * LONG_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asLongBuffer().put(inData, inStart, len);
    }

    /**
     * Copies a range from an array of <code>byte</code> into an array of <code>long</code>.
     * 
     * @param inData The input array of <code>byte</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>byte</code> to
     *            start
     * @param outData The output array of <code>long</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>long</code> to
     *            start
     * @param len The number of <code>long</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyByteToLong(byte[] inData, int inStart, long[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(inData, inStart, len * LONG_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asLongBuffer().get(outData, outStart, len);
    }

    /**
     * Copies a range from an array of <code>short</code> into an array of <code>byte</code>.
     * 
     * @param inData The input array of <code>short</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>short</code> to
     *            start
     * @param outData The output array of <code>byte</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>byte</code> to
     *            start
     * @param len The number of <code>short</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyShortToByte(short[] inData, int inStart, byte[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(outData, outStart, len * SHORT_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asShortBuffer().put(inData, inStart, len);
    }

    /**
     * Copies a range from an array of <code>char</code> into an array of <code>byte</code>.
     * 
     * @param inData The input array of <code>char</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>char</code> to
     *            start
     * @param outData The output array of <code>byte</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>byte</code> to
     *            start
     * @param len The number of <code>char</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyCharToByte(char[] inData, int inStart, byte[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(outData, outStart, len * SHORT_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asCharBuffer().put(inData, inStart, len);
    }

    /**
     * Copies a range from an array of <code>byte</code> into an array of <code>short</code>.
     * 
     * @param inData The input array of <code>byte</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>byte</code> to
     *            start
     * @param outData The output array of <code>short</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>short</code> to
     *            start
     * @param len The number of <code>short</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyByteToShort(byte[] inData, int inStart, short[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(inData, inStart, len * SHORT_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asShortBuffer().get(outData, outStart, len);
    }

    /**
     * Copies a range from an array of <code>byte</code> into an array of <code>char</code>.
     * 
     * @param inData The input array of <code>byte</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>byte</code> to
     *            start
     * @param outData The output array of <code>short</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>short</code> to
     *            start
     * @param len The number of <code>short</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyByteToChar(byte[] inData, int inStart, char[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(inData, inStart, len * CHAR_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asCharBuffer().get(outData, outStart, len);
    }

    /**
     * Copies a range from an array of <code>float</code> into an array of <code>byte</code>.
     * 
     * @param inData The input array of <code>float</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>float</code> to
     *            start
     * @param outData The output array of <code>byte</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>byte</code> to
     *            start
     * @param len The number of <code>float</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyFloatToByte(float[] inData, int inStart, byte[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(outData, outStart, len * FLOAT_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asFloatBuffer().put(inData, inStart, len);
    }

    /**
     * Copies a range from an array of <code>byte</code> into an array of <code>float</code>.
     * 
     * @param inData The input array of <code>byte</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>byte</code> to
     *            start
     * @param outData The output array of <code>float</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>float</code> to
     *            start
     * @param len The number of <code>float</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyByteToFloat(byte[] inData, int inStart, float[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(inData, inStart, len * FLOAT_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asFloatBuffer().get(outData, outStart, len);
    }

    /**
     * Copies a range from an array of <code>double</code> into an array of <code>byte</code>.
     * 
     * @param inData The input array of <code>double</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>double</code> to
     *            start
     * @param outData The output array of <code>byte</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>byte</code> to
     *            start
     * @param len The number of <code>double</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyDoubleToByte(double[] inData, int inStart, byte[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(outData, outStart, len * DOUBLE_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asDoubleBuffer().put(inData, inStart, len);
    }

    /**
     * Copies a range from an array of <code>byte</code> into an array of <code>double</code>.
     * 
     * @param inData The input array of <code>byte</code> values.
     * @param inStart The position in the input array <code>inData</code> of <code>byte</code> to
     *            start
     * @param outData The output array of <code>double</code> values.
     * @param outStart The start in the output array <code>byteData</code> of <code>double</code> to
     *            start
     * @param len The number of <code>double</code> to copy
     * @param byteOrder The {@link ByteOrder}, encoding what byte order the <var>outData</var>
     *            should be in.
     */
    public static void copyByteToDouble(byte[] inData, int inStart, double[] outData, int outStart,
            int len, ByteOrder byteOrder)
    {
        final ByteBuffer bb = ByteBuffer.wrap(inData, inStart, len * DOUBLE_SIZE);
        bb.order(byteOrder.getNioByteOrder());
        bb.asDoubleBuffer().get(outData, outStart, len);
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>char[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @param start The position in the <var>byteArr</var> to start the conversion.
     * @param len The number of <code>short</code> values to convert.
     * @return The <code>char[]</code> array.
     */
    public static char[] byteToChar(byte[] byteArr, ByteOrder byteOrder, int start, int len)
    {
        final char[] array = new char[len];
        copyByteToChar(byteArr, start, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>char[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @return The <code>char[]</code> array.
     */
    public static char[] byteToChar(byte[] byteArr, ByteOrder byteOrder)
    {
        if (byteArr.length % CHAR_SIZE != 0)
        {
            throw new IllegalArgumentException("Length of byteArr does not match size of data type");
        }
        final int len = byteArr.length / SHORT_SIZE;
        final char[] array = new char[len];
        copyByteToChar(byteArr, 0, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>short[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @param start The position in the <var>byteArr</var> to start the conversion.
     * @param len The number of <code>short</code> values to convert.
     * @return The <code>short[]</code> array.
     */
    public static short[] byteToShort(byte[] byteArr, ByteOrder byteOrder, int start, int len)
    {
        final short[] array = new short[len];
        copyByteToShort(byteArr, start, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>short[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @return The <code>short[]</code> array.
     */
    public static short[] byteToShort(byte[] byteArr, ByteOrder byteOrder)
    {
        if (byteArr.length % SHORT_SIZE != 0)
        {
            throw new IllegalArgumentException("Length of byteArr does not match size of data type");
        }
        final int len = byteArr.length / SHORT_SIZE;
        final short[] array = new short[len];
        copyByteToShort(byteArr, 0, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>short[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @param start The position in <var>data</var> to start the conversion.
     * @param len The number of <code>short</code> values to convert.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] shortToByte(short[] data, ByteOrder byteOrder, int start, int len)
    {
        final byte[] byteArr = new byte[SHORT_SIZE * len];
        copyShortToByte(data, start, byteArr, 0, len, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>short[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] shortToByte(short[] data, ByteOrder byteOrder)
    {
        final byte[] byteArr = new byte[SHORT_SIZE * data.length];
        copyShortToByte(data, 0, byteArr, 0, data.length, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>char[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @param start The position in <var>data</var> to start the conversion.
     * @param len The number of <code>char</code> values to convert.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] charToByte(char[] data, ByteOrder byteOrder, int start, int len)
    {
        final byte[] byteArr = new byte[CHAR_SIZE * len];
        copyCharToByte(data, start, byteArr, 0, len, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>char[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] charToByte(char[] data, ByteOrder byteOrder)
    {
        final byte[] byteArr = new byte[CHAR_SIZE * data.length];
        copyCharToByte(data, 0, byteArr, 0, data.length, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>byte[]</code> array into an <code>int[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @param start The position in the <var>byteArr</var> to start the conversion.
     * @param len The number of <code>int</code> values to convert.
     * @return The <code>int[]</code> array.
     */
    public static int[] byteToInt(byte[] byteArr, ByteOrder byteOrder, int start, int len)
    {
        final int[] array = new int[len];
        copyByteToInt(byteArr, start, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>byte[]</code> array into an <code>int[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @return The <code>int[]</code> array.
     */
    public static int[] byteToInt(byte[] byteArr, ByteOrder byteOrder)
    {
        if (byteArr.length % INT_SIZE != 0)
        {
            throw new IllegalArgumentException("Length of byteArr does not match size of data type");
        }
        final int len = byteArr.length / INT_SIZE;
        final int[] array = new int[len];
        copyByteToInt(byteArr, 0, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>int[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @param start The position in <var>data</var> to start the conversion.
     * @param len The number of <code>int</code> values to convert.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] intToByte(int[] data, ByteOrder byteOrder, int start, int len)
    {
        final byte[] byteArr = new byte[INT_SIZE * len];
        copyIntToByte(data, start, byteArr, 0, len, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>int[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] intToByte(int[] data, ByteOrder byteOrder)
    {
        final byte[] byteArr = new byte[INT_SIZE * data.length];
        copyIntToByte(data, 0, byteArr, 0, data.length, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>long[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @param start The position in the <var>byteArr</var> to start the conversion.
     * @param len The number of <code>long</code> values to convert.
     * @return The <code>long[]</code> array.
     */
    public static long[] byteToLong(byte[] byteArr, ByteOrder byteOrder, int start, int len)
    {
        final long[] array = new long[len];
        copyByteToLong(byteArr, start, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>long[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @return The <code>long[]</code> array.
     */
    public static long[] byteToLong(byte[] byteArr, ByteOrder byteOrder)
    {
        if (byteArr.length % LONG_SIZE != 0)
        {
            throw new IllegalArgumentException("Length of byteArr does not match size of data type");
        }
        final int len = byteArr.length / LONG_SIZE;
        final long[] array = new long[len];
        copyByteToLong(byteArr, 0, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>long[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @param start The position in <var>data</var> to start the conversion.
     * @param len The number of <code>long</code> values to convert.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] longToByte(long[] data, ByteOrder byteOrder, int start, int len)
    {
        final byte[] byteArr = new byte[LONG_SIZE * len];
        copyLongToByte(data, start, byteArr, 0, len, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>long[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] longToByte(long[] data, ByteOrder byteOrder)
    {
        final byte[] byteArr = new byte[LONG_SIZE * data.length];
        copyLongToByte(data, 0, byteArr, 0, data.length, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>float[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @param start The position in the <var>byteArr</var> to start the conversion.
     * @param len The number of <code>float</code> values to convert.
     * @return The <code>float[]</code> array.
     */
    public static float[] byteToFloat(byte[] byteArr, ByteOrder byteOrder, int start, int len)
    {
        final float[] array = new float[len];
        copyByteToFloat(byteArr, start, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>float[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @return The <code>float[]</code> array.
     */
    public static float[] byteToFloat(byte[] byteArr, ByteOrder byteOrder)
    {
        if (byteArr.length % FLOAT_SIZE != 0)
        {
            throw new IllegalArgumentException("Length of byteArr does not match size of data type");
        }
        final int len = byteArr.length / FLOAT_SIZE;
        final float[] array = new float[len];
        copyByteToFloat(byteArr, 0, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>float[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @param start The position in <var>data</var> to start the conversion.
     * @param len The number of <code>float</code> values to convert.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] floatToByte(float[] data, ByteOrder byteOrder, int start, int len)
    {
        final byte[] byteArr = new byte[FLOAT_SIZE * len];
        copyFloatToByte(data, start, byteArr, 0, len, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>float[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] floatToByte(float[] data, ByteOrder byteOrder)
    {
        final byte[] byteArr = new byte[FLOAT_SIZE * data.length];
        copyFloatToByte(data, 0, byteArr, 0, data.length, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>double[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @param start The position in the <var>byteArr</var> to start the conversion.
     * @param len The number of <code>double</code> values to convert.
     * @return The <code>double[]</code> array.
     */
    public static double[] byteToDouble(byte[] byteArr, ByteOrder byteOrder, int start, int len)
    {
        final double[] array = new double[len];
        copyByteToDouble(byteArr, start, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>byte[]</code> array into a <code>double[]</code> array.
     * 
     * @param byteArr The <code>byte[]</code> to convert.
     * @param byteOrder The byte order of <var>byteArr</var>.
     * @return The <code>double[]</code> array.
     */
    public static double[] byteToDouble(byte[] byteArr, ByteOrder byteOrder)
    {
        if (byteArr.length % DOUBLE_SIZE != 0)
        {
            throw new IllegalArgumentException("Length of byteArr does not match size of data type");
        }
        final int len = byteArr.length / DOUBLE_SIZE;
        final double[] array = new double[len];
        copyByteToDouble(byteArr, 0, array, 0, len, byteOrder);
        return array;
    }

    /**
     * Converts a <code>double[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @param start The position in <var>data</var> to start the conversion.
     * @param len The number of <code>double</code> values to convert.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] doubleToByte(double[] data, ByteOrder byteOrder, int start, int len)
    {
        final byte[] byteArr = new byte[DOUBLE_SIZE * len];
        copyDoubleToByte(data, start, byteArr, 0, len, byteOrder);
        return byteArr;
    }

    /**
     * Converts a <code>double[]</code> array to a <code>byte[]</code> array.
     * 
     * @param data The array to convert.
     * @param byteOrder The byte order of the returned <code>byte[]</code>.
     * @return The converted <code>byte[]</code> array.
     */
    public static byte[] doubleToByte(double[] data, ByteOrder byteOrder)
    {
        final byte[] byteArr = new byte[DOUBLE_SIZE * data.length];
        copyDoubleToByte(data, 0, byteArr, 0, data.length, byteOrder);
        return byteArr;
    }

}
