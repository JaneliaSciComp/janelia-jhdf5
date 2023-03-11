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

import java.util.Arrays;

/**
 * An array of time durations.
 * 
 * @author Bernd Rinn
 */
public class HDF5TimeDurationArray
{
    final long[] timeDurations;

    final HDF5TimeUnit timeUnit;

    /**
     * Creates an array of <var>timeDurations</var> using a common <var>timeUnit</var>.
     */
    public HDF5TimeDurationArray(long[] timeDurations, HDF5TimeUnit timeUnit)
    {
        this.timeDurations = timeDurations;
        this.timeUnit = timeUnit;
    }

    /**
     * Creates a {@link HDF5TimeDurationArray} from the array of given <var>durationValues</var>
     * with the given <var>timeUnit</var>.
     */
    public static HDF5TimeDurationArray create(HDF5TimeUnit timeUnit, long... durationValues)
    {
        if (durationValues.length == 0)
        {
            return new HDF5TimeDurationArray(new long[0], timeUnit);
        }
        return new HDF5TimeDurationArray(durationValues, timeUnit);
    }

    /**
     * Creates a {@link HDF5TimeDurationArray} from the given <var>timeDurations</var>. Converts all
     * values to the smallest time unit found in <var>timeDurations</var>.
     */
    public static HDF5TimeDurationArray create(HDF5TimeDuration... timeDurations)
    {
        if (timeDurations.length == 0)
        {
            return new HDF5TimeDurationArray(new long[0], HDF5TimeUnit.SECONDS);
        }
        HDF5TimeUnit unit = timeDurations[0].getUnit();
        boolean needsConversion = false;
        for (int i = 1; i < timeDurations.length; ++i)
        {
            final HDF5TimeUnit u = timeDurations[i].getUnit();
            if (u != unit)
            {
                if (u.ordinal() < unit.ordinal())
                {
                    unit = u;
                }
                needsConversion = true;
            }
        }
        final long[] durations = new long[timeDurations.length];
        if (needsConversion)
        {
            for (int i = 0; i < timeDurations.length; ++i)
            {
                durations[i] = unit.convert(timeDurations[i]);
            }
        } else
        {
            for (int i = 0; i < timeDurations.length; ++i)
            {
                durations[i] = timeDurations[i].getValue();
            }
        }
        return new HDF5TimeDurationArray(durations, unit);
    }

    /**
     * Returns the time unit.
     */
    public HDF5TimeUnit getUnit()
    {
        return timeUnit;
    }

    /**
     * Returns the time duration values.
     */
    public long[] getValues()
    {
        return timeDurations;
    }

    /**
     * Returns the number of elements.
     */
    public int getLength()
    {
        return timeDurations.length;
    }

    /**
     * Returns the time duration values in the given <var>targetUnit</var>.
     */
    public long[] getValues(HDF5TimeUnit targetUnit)
    {
        if (targetUnit == timeUnit)
        {
            return timeDurations;
        }
        final long[] targetDurations = new long[timeDurations.length];
        for (int i = 0; i < targetDurations.length; ++i)
        {
            targetDurations[i] = targetUnit.convert(timeDurations[i], timeUnit);
        }
        return targetDurations;
    }

    /**
     * Returns the element <var>index</var>.
     */
    public HDF5TimeDuration get(int index)
    {
        return new HDF5TimeDuration(timeDurations[index], timeUnit);
    }

    /**
     * Returns the element <var>index</var> in the given <var>targetUnit</var>.
     */
    public HDF5TimeDuration get(int index, HDF5TimeUnit targetUnit)
    {
        if (targetUnit == timeUnit)
        {
            return new HDF5TimeDuration(timeDurations[index], timeUnit);
        } else
        {
            return new HDF5TimeDuration(targetUnit.convert(timeDurations[index], timeUnit),
                    targetUnit);
        }
    }

    /**
     * Returns the value element <var>index</var>.
     */
    public long getValue(int index)
    {
        return timeDurations[index];
    }

    /**
     * Returns the value element <var>index</var> in the given <var>targetUnit</var>.
     */
    public long getValue(int index, HDF5TimeUnit targetUnit)
    {
        return (targetUnit == timeUnit) ? timeDurations[index] : targetUnit.convert(
                timeDurations[index], timeUnit);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(timeDurations);
        result = prime * result + ((timeUnit == null) ? 0 : timeUnit.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final HDF5TimeDurationArray other = (HDF5TimeDurationArray) obj;
        if (Arrays.equals(timeDurations, other.timeDurations) == false)
        {
            return false;
        }
        if (timeUnit != other.timeUnit)
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "HDF5TimeDurationArray [timeDurations=" + Arrays.toString(timeDurations)
                + ", timeUnit=" + timeUnit + "]";
    }
}