package org.janelia.jhdf5.benchmark;

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

import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;

/**
 * The variable-length string helpers of {@link HDFHelper}, which used to be native: copying a
 * Java string to a C string whose pointer is stored in a compound's byte buffer, reading such a
 * pointer back into a Java string, and freeing it. This is what every compound with a
 * variable-length string member goes through, once per member per record.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class VLStringBenchmark
{
    @Param({ "8", "256", "8192" })
    public int length;

    private String string;

    private byte[] record;

    private int[] vlMemberIndices;

    @Setup
    public void setUp()
    {
        final StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; ++i)
        {
            builder.append((char) ('a' + i % 26));
        }
        string = builder.toString();
        record = new byte[HDFHelper.getPointerSize()];
        vlMemberIndices = new int[] { 0 };
    }

    /** Write, read back and free: one member of one record. */
    @Benchmark
    public String roundTrip()
    {
        HDFHelper.compoundCpyVLStr(string, record, 0);
        final String result = HDFHelper.createVLStrFromCompound(record, 0);
        HDFHelper.freeCompoundVLStr(record, record.length, vlMemberIndices);
        return result;
    }

    /** Write and free only, as writing a compound does. */
    @Benchmark
    public int copyAndFree()
    {
        HDFHelper.compoundCpyVLStr(string, record, 0);
        return HDFHelper.freeCompoundVLStr(record, record.length, vlMemberIndices);
    }
}
