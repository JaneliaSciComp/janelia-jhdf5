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

/**
 * A <code>TimeUnit</code> represents a unit of a time duration. Each unit corresponds to one time
 * duration {@link HDF5DataTypeVariant}.
 * <p>
 * The conversion of time durations is heavily inspired by Doug Lea's class in the Java runtime
 * library.
 * 
 * @author Bernd Rinn
 */
public enum HDF5TimeUnit
{
    MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS;

    /** Lookup table for type variants. */
    private static final HDF5DataTypeVariant[] typeVariants =
        { HDF5DataTypeVariant.TIME_DURATION_MICROSECONDS,
                HDF5DataTypeVariant.TIME_DURATION_MILLISECONDS,
                HDF5DataTypeVariant.TIME_DURATION_SECONDS,
                HDF5DataTypeVariant.TIME_DURATION_MINUTES, HDF5DataTypeVariant.TIME_DURATION_HOURS,
                HDF5DataTypeVariant.TIME_DURATION_DAYS };

    /** Lookup table for conversion factors (to smaller units). */
    private static final long[][] multipliers =
        {
                // first dimension is the start unit, second is the delta
                // micro seconds
                { 1L },
                // milli seconds
                { 1L, 1000L },
                // seconds
                { 1L, 1000L, 1000L * 1000 },
                // minutes
                { 1L, 60L, 60L * 1000, 60L * 1000 * 1000 },
                // hours
                { 1L, 60L, 60L * 60, 60L * 60 * 1000, 60L * 60 * 1000 * 1000 },
                // days
                { 1L, 24L, 24L * 60, 24L * 60 * 60, 24L * 60 * 60 * 1000L,
                        24L * 60 * 60 * 1000 * 1000 } };

    /** Lookup table for conversion factors (to larger units). */
    private static final double[][] divisors =
        {
                // first dimension is the start unit, second is the delta
                // micro seconds
                { 1.0, 1000.0, 1000.0 * 1000, 1000.0 * 1000 * 60, 1000.0 * 1000 * 60 * 60,
                        1000.0 * 1000 * 60 * 60 * 24 },
                // millis seconds
                { 1.0, 1000.0, 1000.0 * 60, 1000.0 * 60 * 60, 1000.0 * 60 * 60 * 24 },
                // seconds
                { 1.0, 60.0, 60.0 * 60, 60 * 60 * 24 },
                // minutes
                { 1.0, 60.0, 60.0 * 24 },
                // hours
                { 1.0, 24.0 },
                // days
                { 1.0 } };

    /**
     * Lookup table to check saturation. Note that because we are dividing these down, we don't have
     * to deal with asymmetry of MIN/MAX values.
     */
    private static final long[][] overflows =
        {
                // first dimension is the start unit, second is the delta
                // micro seconds
                { -1 },
                // milli seconds
                { -1, Long.MAX_VALUE / 1000L },
                // seconds
                { -1, Long.MAX_VALUE / 1000L, Long.MAX_VALUE / (1000L * 1000) },
                // minutes
                { -1, Long.MAX_VALUE / 60L, Long.MAX_VALUE / (60L * 1000),
                        Long.MAX_VALUE / (60L * 1000 * 1000) },
                // hours
                { -1, Long.MAX_VALUE / 60L, Long.MAX_VALUE / (60L * 60),
                        Long.MAX_VALUE / (60L * 60 * 1000),
                        Long.MAX_VALUE / (60L * 60 * 1000 * 1000) },
                // days
                { -1, Long.MAX_VALUE / 24L, Long.MAX_VALUE / (24L * 60),
                        Long.MAX_VALUE / (24L * 60 * 60), Long.MAX_VALUE / (24L * 60 * 60 * 1000),
                        Long.MAX_VALUE / (24L * 60 * 60 * 1000 * 1000) } };

    private static long doConvert(int ordinal, int delta, long duration)
    {
        if (delta == 0)
        {
            return duration;
        }
        if (delta < 0)
        {
            return Math.round(duration / divisors[ordinal][-delta]);
        }
        final long overflow = overflows[ordinal][delta];
        if (duration > overflow)
        {
            return Long.MAX_VALUE;
        }
        if (duration < -overflow)
        {
            return Long.MIN_VALUE;
        }
        return duration * multipliers[ordinal][delta];
    }

    /**
     * Returns the type variant corresponding to this unit.
     */
    public HDF5DataTypeVariant getTypeVariant()
    {
        return typeVariants[ordinal()];
    }

    /**
     * Convert the given time duration in the given unit to this unit. Conversions from smaller to
     * larger units perform rounding, so they lose precision. Conversions from larger to smaller
     * units with arguments that would numerically overflow saturate to <code>Long.MIN_VALUE</code>
     * if negative or <code>Long.MAX_VALUE</code> if positive.
     * 
     * @param duration The time duration in the given <code>unit</code>.
     * @param unit The unit of the <code>duration</code> argument.
     * @return The converted duration in this unit, or <code>Long.MIN_VALUE</code> if conversion
     *         would negatively overflow, or <code>Long.MAX_VALUE</code> if it would positively
     *         overflow.
     */
    public long convert(long duration, HDF5TimeUnit unit)
    {
        final int currentUnitOrdinal = unit.ordinal();
        return doConvert(currentUnitOrdinal, currentUnitOrdinal - ordinal(), duration);
    }

    /**
     * Convert the given time <var>durations</var> in the given time <var>unit</var> to this unit.
     * Conversions from smaller to larger units perform rounding, so they lose precision.
     * Conversions from larger to smaller units with arguments that would numerically overflow
     * saturate to <code>Long.MIN_VALUE</code> if negative or <code>Long.MAX_VALUE</code> if
     * positive.
     * 
     * @param durations The time durations.
     * @return The converted duration in this unit, or <code>Long.MIN_VALUE</code> if conversion
     *         would negatively overflow, or <code>Long.MAX_VALUE</code> if it would positively
     *         overflow.
     */
    public long[] convert(final HDF5TimeDurationArray durations)
    {
        if (this != durations.timeUnit)
        {
            final long[] convertedData = new long[durations.timeDurations.length];
            for (int i = 0; i < durations.timeDurations.length; ++i)
            {
                convertedData[i] = this.convert(durations.timeDurations[i], durations.timeUnit);
            }
            return convertedData;
        } else
        {
            return durations.timeDurations;
        }
    }

    /**
     * Convert the given time <var>durations</var> in the given time <var>unit</var> to this unit.
     * Conversions from smaller to larger units perform rounding, so they lose precision.
     * Conversions from larger to smaller units with arguments that would numerically overflow
     * saturate to <code>Long.MIN_VALUE</code> if negative or <code>Long.MAX_VALUE</code> if
     * positive.
     * 
     * @param durations The time durations.
     * @return The converted duration in this unit, or <code>Long.MIN_VALUE</code> if conversion
     *         would negatively overflow, or <code>Long.MAX_VALUE</code> if it would positively
     *         overflow.
     */
    public HDF5TimeDurationMDArray convert(final HDF5TimeDurationMDArray durations)
    {
        if (this != durations.timeUnit)
        {
            final long[] originalData = durations.getAsFlatArray();
            final long[] convertedData = new long[originalData.length];
            for (int i = 0; i < originalData.length; ++i)
            {
                convertedData[i] = this.convert(originalData[i], durations.timeUnit);
            }
            return new HDF5TimeDurationMDArray(convertedData, durations.dimensions(), this);
        } else
        {
            return durations;
        }
    }

    /**
     * Convert the given time <var>durations</var> in the given time <var>unit</var> to this unit.
     * Conversions from smaller to larger units perform rounding, so they lose precision.
     * Conversions from larger to smaller units with arguments that would numerically overflow
     * saturate to <code>Long.MIN_VALUE</code> if negative or <code>Long.MAX_VALUE</code> if
     * positive.
     * 
     * @param durations The time duration in the given <code>unit</code>.
     * @param unit The unit of the <code>duration</code> argument.
     * @return The converted duration in this unit, or <code>Long.MIN_VALUE</code> if conversion
     *         would negatively overflow, or <code>Long.MAX_VALUE</code> if it would positively
     *         overflow.
     */
    public long[] convert(final long[] durations, final HDF5TimeUnit unit)
    {
        if (this != unit)
        {
            final long[] convertedData = new long[durations.length];
            for (int i = 0; i < durations.length; ++i)
            {
                convertedData[i] = this.convert(durations[i], unit);
            }
            return convertedData;
        } else
        {
            return durations;
        }
    }

    /**
     * Convert the given time duration in the given unit to this unit. Conversions from smaller to
     * larger units perform rounding, so they lose precision. Conversions from larger to smaller
     * units with arguments that would numerically overflow saturate to <code>Long.MIN_VALUE</code>
     * if negative or <code>Long.MAX_VALUE</code> if positive.
     * 
     * @param duration The time duration and its unit.
     * @return The converted duration in this unit, or <code>Long.MIN_VALUE</code> if conversion
     *         would negatively overflow, or <code>Long.MAX_VALUE</code> if it would positively
     *         overflow.
     */
    public long convert(HDF5TimeDuration duration)
    {
        return convert(duration.getValue(), duration.getUnit());
    }

    /**
     * Convert the given time <var>durations</var> to this unit. Conversions from smaller to larger
     * units perform rounding, so they lose precision. Conversions from larger to smaller units with
     * arguments that would numerically overflow saturate to <code>Long.MIN_VALUE</code> if negative
     * or <code>Long.MAX_VALUE</code> if positive.
     * 
     * @return The converted durations in this unit, or <code>Long.MIN_VALUE</code> if conversion
     *         would negatively overflow, or <code>Long.MAX_VALUE</code> if it would positively
     *         overflow.
     */
    public long[] convert(final HDF5TimeDuration[] durations)
    {
        final long[] convertedData = new long[durations.length];
        for (int i = 0; i < durations.length; ++i)
        {
            convertedData[i] = this.convert(durations[i]);
        }
        return convertedData;
    }

}
