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
import java.util.BitSet;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * An enum of all type variants. Type variants contain additional information on how to interpret a
 * data set, similar to the tag for the opaque type.
 * 
 * @author Bernd Rinn
 */
public enum HDF5DataTypeVariant
{
    //
    // Implementation note: Never change the order or the names of the values or else old files will
    // be interpreted wrongly!
    //
    // Appending of new type variants at the end of the list is fine.
    //

    /**
     * Used for data sets that encode time stamps as number of milli-seconds since midnight, January
     * 1, 1970 UTC (aka "start of the epoch").
     */
    TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH(long.class, Long.class, Date.class),

    /**
     * A time duration in micro-seconds.
     */
    TIME_DURATION_MICROSECONDS(HDF5Utils.allTimeDurationTypes),

    /**
     * A time duration in milli-seconds.
     */
    TIME_DURATION_MILLISECONDS(HDF5Utils.allTimeDurationTypes),

    /**
     * A time duration in seconds.
     */
    TIME_DURATION_SECONDS(HDF5Utils.allTimeDurationTypes),

    /**
     * A time duration in minutes.
     */
    TIME_DURATION_MINUTES(HDF5Utils.allTimeDurationTypes),

    /**
     * A time duration in hours.
     */
    TIME_DURATION_HOURS(HDF5Utils.allTimeDurationTypes),

    /**
     * A time duration in days.
     */
    TIME_DURATION_DAYS(HDF5Utils.allTimeDurationTypes),

    /**
     * An enumeration.
     */
    ENUM(HDF5EnumerationValue.class, HDF5EnumerationValueArray.class),

    /**
     * No type variant.
     */
    NONE,
    
    BITFIELD(BitSet.class);

    private Set<Class<?>> compatibleTypes;

    private HDF5DataTypeVariant(Class<?>... compatibleTypes)
    {
        this.compatibleTypes = new HashSet<Class<?>>(Arrays.asList(compatibleTypes));
    }

    /**
     * Returns <code>true</code>, if <var>typeVariantOrNull</var> is not
     * <code>null</codeL and not <code>NONE</code>.
     */
    public static boolean isTypeVariant(HDF5DataTypeVariant typeVariantOrNull)
    {
        return (typeVariantOrNull != null) && typeVariantOrNull.isTypeVariant();
    }

    /**
     * Returns <code>true</code>, if <var>typeVariantOrdinal</var> does not
     * represent <code>NONE</code>.
     */
    public static boolean isTypeVariant(int typeVariantOrdinal)
    {
        return typeVariantOrdinal != NONE.ordinal();
    }

    /**
     * Returns <var>typeVariantOrNull</var>, if it is not <code>null</code>, and <code>NONE</code>
     * otherwise.
     */
    public static HDF5DataTypeVariant maskNull(HDF5DataTypeVariant typeVariantOrNull)
    {
        return (typeVariantOrNull != null) ? typeVariantOrNull : NONE;
    }

    /**
     * Returns <var>typeVariantOrNull</var>, if it is not <code>NONE</code>, and <code>null</code>
     * otherwise.
     */
    public static HDF5DataTypeVariant unmaskNone(HDF5DataTypeVariant typeVariantOrNull)
    {
        return (typeVariantOrNull != NONE) ? typeVariantOrNull : null;
    }

    /**
     * Returns <code>true</code>, if this type variant is not <code>NONE</code>.
     */
    public boolean isTypeVariant()
    {
        return this != NONE;
    }

    /**
     * Returns <code>true</code>, if the type variant denoted by <var>typeVariantOrdinal</var>
     * corresponds to a time duration.
     */
    public static boolean isTimeDuration(final int typeVariantOrdinal)
    {
        return typeVariantOrdinal >= TIME_DURATION_MICROSECONDS.ordinal()
                && typeVariantOrdinal <= TIME_DURATION_DAYS.ordinal();
    }

    /**
     * Returns <code>true</code> if <var>type</var> is compatible with this type variant.
     */
    public boolean isCompatible(Class<?> type)
    {
        return compatibleTypes.contains(type);
    }
    
    /**
     * Returns the time unit for the given <var>typeVariant</var>. Note that it is an error
     * if <var>typeVariant</var> does not correspond to a time unit.
     */
    public static HDF5TimeUnit getTimeUnit(final HDF5DataTypeVariant typeVariant)
    {
        return HDF5TimeUnit.values()[typeVariant.ordinal()
                - HDF5DataTypeVariant.TIME_DURATION_MICROSECONDS.ordinal()];
    }

    /**
     * Returns the time unit for the given <var>typeVariantOrdinal</var>. Note that it is an error
     * if <var>typeVariantOrdinal</var> does not correspond to a time unit.
     */
    public static HDF5TimeUnit getTimeUnit(final int typeVariantOrdinal)
    {
        return HDF5TimeUnit.values()[typeVariantOrdinal
                - HDF5DataTypeVariant.TIME_DURATION_MICROSECONDS.ordinal()];
    }

    /**
     * Returns <code>true</code>, if this type variant corresponds to a time stamp.
     */
    public boolean isTimeStamp()
    {
        return this == TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH;
    }

    /**
     * Returns <code>true</code>, if this type variant corresponds to a time duration.
     */
    public boolean isTimeDuration()
    {
        return isTimeDuration(ordinal());
    }

    /**
     * Returns the time unit for this type variant or <code>null</code>, if this type variant is not
     * a time unit.
     */
    public HDF5TimeUnit tryGetTimeUnit()
    {
        final int ordinal = ordinal();
        return isTimeDuration(ordinal) ? getTimeUnit(ordinal) : null;
    }
    
}
