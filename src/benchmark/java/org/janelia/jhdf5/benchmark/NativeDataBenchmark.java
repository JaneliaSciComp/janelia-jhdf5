/*
 * Copyright 2026 Howard Hughes Medical Institute
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

package org.janelia.jhdf5.benchmark;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import ch.systemsx.cisd.base.convert.NativeData;
import ch.systemsx.cisd.hdf5.hdf5lib.HDF5NativeLibrary;
import hdf.hdf5lib.HDFNativeData;

/**
 * The pure-Java {@link NativeData} that replaced cisd:base's native library, against the two
 * alternatives still available: HDF5's own JNI converters ({@link HDFNativeData}, native byte
 * order only) and {@link ByteBuffer}. Each operation converts {@code n} values, allocating the
 * result array, as the library's callers do.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class NativeDataBenchmark
{
    @Param({ "1024", "1048576" })
    public int n;

    private byte[] intBytes;

    private byte[] doubleBytes;

    private int[] ints;

    private double[] doubles;

    private NativeData.ByteOrder swapped;

    private ByteOrder nioNative;

    @Setup
    public void setUp()
    {
        HDF5NativeLibrary.load();
        final Random random = new Random(42);
        ints = new int[n];
        doubles = new double[n];
        for (int i = 0; i < n; ++i)
        {
            ints[i] = random.nextInt();
            doubles[i] = random.nextDouble();
        }
        intBytes = NativeData.intToByte(ints, NativeData.ByteOrder.NATIVE);
        doubleBytes = NativeData.doubleToByte(doubles, NativeData.ByteOrder.NATIVE);
        nioNative = ByteOrder.nativeOrder();
        swapped = (nioNative == ByteOrder.LITTLE_ENDIAN) ? NativeData.ByteOrder.BIG_ENDIAN
                : NativeData.ByteOrder.LITTLE_ENDIAN;
    }

    // byte[] -> int[]

    @Benchmark
    public int[] byteToInt_nativeData()
    {
        return NativeData.byteToInt(intBytes, NativeData.ByteOrder.NATIVE);
    }

    @Benchmark
    public int[] byteToInt_nativeData_swapped()
    {
        return NativeData.byteToInt(intBytes, swapped);
    }

    @Benchmark
    public int[] byteToInt_hdfNativeData()
    {
        return HDFNativeData.byteToInt(0, n, intBytes);
    }

    @Benchmark
    public int[] byteToInt_byteBuffer()
    {
        final int[] result = new int[n];
        ByteBuffer.wrap(intBytes).order(nioNative).asIntBuffer().get(result);
        return result;
    }

    // int[] -> byte[]

    @Benchmark
    public byte[] intToByte_nativeData()
    {
        return NativeData.intToByte(ints, NativeData.ByteOrder.NATIVE);
    }

    @Benchmark
    public byte[] intToByte_nativeData_swapped()
    {
        return NativeData.intToByte(ints, swapped);
    }

    @Benchmark
    public byte[] intToByte_hdfNativeData()
    {
        return HDFNativeData.intToByte(0, n, ints);
    }

    @Benchmark
    public byte[] intToByte_byteBuffer()
    {
        final byte[] result = new byte[n * 4];
        ByteBuffer.wrap(result).order(nioNative).asIntBuffer().put(ints);
        return result;
    }

    // byte[] -> double[]

    @Benchmark
    public double[] byteToDouble_nativeData()
    {
        return NativeData.byteToDouble(doubleBytes, NativeData.ByteOrder.NATIVE);
    }

    @Benchmark
    public double[] byteToDouble_nativeData_swapped()
    {
        return NativeData.byteToDouble(doubleBytes, swapped);
    }

    @Benchmark
    public double[] byteToDouble_hdfNativeData()
    {
        return HDFNativeData.byteToDouble(0, n, doubleBytes);
    }

    @Benchmark
    public double[] byteToDouble_byteBuffer()
    {
        final double[] result = new double[n];
        ByteBuffer.wrap(doubleBytes).order(nioNative).asDoubleBuffer().get(result);
        return result;
    }

    // double[] -> byte[]

    @Benchmark
    public byte[] doubleToByte_nativeData()
    {
        return NativeData.doubleToByte(doubles, NativeData.ByteOrder.NATIVE);
    }

    @Benchmark
    public byte[] doubleToByte_hdfNativeData()
    {
        return HDFNativeData.doubleToByte(0, n, doubles);
    }

    @Benchmark
    public byte[] doubleToByte_byteBuffer()
    {
        final byte[] result = new byte[n * 8];
        ByteBuffer.wrap(result).order(nioNative).asDoubleBuffer().put(doubles);
        return result;
    }
}
