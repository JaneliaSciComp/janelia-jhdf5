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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;

import hdf.hdf5lib.exceptions.HDF5JavaException;

//import ch.rinn.restrictions.Private;

/**
 * Some utility methods used by {@link HDF5Reader} and {@link HDF5Writer}.
 * 
 * @author Bernd Rinn
 */
final class HDF5Utils
{

    /**
     * The name for an explicitly saved string length attribute.
     */
    static final String STRING_LENGTH_ATTRIBUTE_NAME = "STRING_LENGTH";

    /**
     * The name for a type variant attribute.
     */
    static final String TYPE_VARIANT_ATTRIBUTE_NAME = "TYPE_VARIANT";

    /** The minimal size of a chunk. */
    //@Private
    static final int MIN_CHUNK_SIZE = 1;

    /** The minimal size of a data set in order to allow for chunking. */
    private static final long MIN_TOTAL_SIZE_FOR_CHUNKING = 128L;

    /** The dimensions vector for a scalar data type. */
    static final long[] SCALAR_DIMENSIONS = new long[]
        { 1 };

    /** The prefix for opqaue data types. */
    static final String OPAQUE_PREFIX = "Opaque_";

    /** The prefix for enum data types. */
    static final String ENUM_PREFIX = "Enum_";

    /** The prefix for compound data types. */
    static final String COMPOUND_PREFIX = "Compound_";

    /**
     * The suffix for housekeeping files and groups. Setting this attribute overrides the default,
     * which is: __NAME__.
     */
    static final String HOUSEKEEPING_NAME_SUFFIX_ATTRIBUTE_NAME = "__HOUSEKEEPING_SUFFIX__";

    /**
     * The length of the suffix for housekeeping files and groups.
     */
    static final String HOUSEKEEPING_NAME_SUFFIX_STRINGLENGTH_ATTRIBUTE_NAME = "__"
            + STRING_LENGTH_ATTRIBUTE_NAME + "__" + HOUSEKEEPING_NAME_SUFFIX_ATTRIBUTE_NAME + "__";

    /**
     * The legacy attribute to signal that a data set is empty (for backward compatibility with
     * 8.10).
     */
    static final String DATASET_IS_EMPTY_LEGACY_ATTRIBUTE = "__EMPTY__";

    /** Returns the boolean data type. */
    static String getBooleanDataTypePath(String houseKeepingNameSuffix)
    {
        return getDataTypeGroup(houseKeepingNameSuffix) + "/" + ENUM_PREFIX + "Boolean";
    }

    /** Returns the data type specifying a type variant. */
    static String getTypeVariantDataTypePath(String houseKeepingNameSuffix)
    {
        return getDataTypeGroup(houseKeepingNameSuffix) + "/" + ENUM_PREFIX + "TypeVariant";
    }

    /** Returns the variable-length string data type. */
    static String getVariableLengthStringDataTypePath(String houseKeepingNameSuffix)
    {
        return getDataTypeGroup(houseKeepingNameSuffix) + "/String_VariableLength";
    }

    /**
     * Returns the attribute name to signal that this compound type has members with variant of the
     * member data type.
     */
    static String getTypeVariantMembersAttributeName(String houseKeepingNameSuffix)
    {
        return "".equals(houseKeepingNameSuffix) ? "__TYPE_VARIANT_MEMBERS__"
                : "TYPE_VARIANT_MEMBERS" + houseKeepingNameSuffix;
    }

    /** Returns the attribute to store the name of the enum data type. */
    static String getEnumTypeNameAttributeName(String houseKeepingNameSuffix)
    {
        return "".equals(houseKeepingNameSuffix) ? "__ENUM_TYPE_NAME__" : "ENUM_TYPE_NAME"
                + houseKeepingNameSuffix;
    }

    /** Returns the group to store all named derived data types in. */
    static String getDataTypeGroup(String houseKeepingNameSuffix)
    {
        return "".equals(houseKeepingNameSuffix) ? "/__DATA_TYPES__" : "/DATA_TYPES"
                + houseKeepingNameSuffix;
    }

    /**
     * All integer types in Java.
     */
    static Class<?>[] allIntegerTypes = new Class<?>[]
        { byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class,
                Long.class };

    /**
     * All float types in Java.
     */
    static Class<?>[] allFloatTypes = new Class<?>[]
        { float.class, Float.class, double.class, Double.class };

    /**
     * All types in Java that can store time durations.
     */
    static Class<?>[] allTimeDurationTypes = new Class<?>[]
        { byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class,
                Long.class, float.class, Float.class, double.class, Double.class,
                HDF5TimeDuration.class };

    /**
     * Returns the primitive type for wrapper classes of primitive types, and the <var>clazz</var>
     * itself, otherwise.
     */
    static Class<?> unwrapClass(Class<?> clazz)
    {
        if (clazz == Byte.class)
        {
            return byte.class;
        } else if (clazz == Short.class)
        {
            return short.class;
        } else if (clazz == Integer.class)
        {
            return int.class;
        } else if (clazz == Long.class)
        {
            return long.class;
        } else if (clazz == Float.class)
        {
            return float.class;
        } else if (clazz == Double.class)
        {
            return double.class;
        } else if (clazz == Boolean.class)
        {
            return boolean.class;
        } else
        {
            return clazz;
        }
    }

    static String getSuperGroup(String path)
    {
        assert path != null;

        final int lastIndexSlash = path.lastIndexOf('/');
        if (lastIndexSlash <= 0)
        {
            return "/";
        } else
        {
            return path.substring(0, lastIndexSlash);
        }
    }

    static boolean isEmpty(long[] dimensions)
    {
        for (long d : dimensions)
        {
            if (d == 0)
            {
                return true;
            }
        }
        return false;
    }

    static boolean isNonPositive(long[] dimensions)
    {
        for (long d : dimensions)
        {
            if (d <= 0)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the dimensions for a scalar, or <code>null</code>, if this data set is too small for
     * chunking.
     */
    static long[] tryGetChunkSizeForString(int len, boolean tryChunkedDS)
    {
        if (tryChunkedDS)
        {
            return (len < MIN_TOTAL_SIZE_FOR_CHUNKING) ? null : SCALAR_DIMENSIONS;
        } else
        {
            return null;
        }
    }

    /**
     * Returns a chunk size suitable for a data set with <var>dimension</var>, or <code>null</code>,
     * if this data set can't be reasonably chunk-ed.
     */
    static long[] tryGetChunkSize(final long[] dimensions, int elementLength, boolean tryChunkedDS,
            boolean enforceChunkedDS)
    {
        assert dimensions != null;

        if (enforceChunkedDS == false && tryChunkedDS == false)
        {
            return null;
        }
        final long[] chunkSize = new long[dimensions.length];
        long totalSize = elementLength;
        for (int i = 0; i < dimensions.length; ++i)
        {
            totalSize *= dimensions[i];
            chunkSize[i] = Math.max(MIN_CHUNK_SIZE, dimensions[i]);
        }
        if (enforceChunkedDS == false && totalSize < MIN_TOTAL_SIZE_FOR_CHUNKING)
        {
            return null;
        }
        return chunkSize;
    }

    /**
     * Returns a path for a data type with <var>name</var> and (optional) <var>appendices</var>.
     * <p>
     * <b>Special case:</b> If the <var>appendices</var> array contains exactly one element and if
     * this element starts with '/', this element itself will be considered the (complete) data type
     * path.
     */
    static String createDataTypePath(String name, String houseKeepingSuffix, String... appendices)
    {
        if (appendices.length == 1 && appendices[0].startsWith("/"))
        {
            return appendices[0];
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(getDataTypeGroup(houseKeepingSuffix));
        builder.append('/');
        builder.append(name);
        for (String app : appendices)
        {
            builder.append(app);
        }
        return builder.toString();
    }

    /**
     * Returns the name for a committed data type with <var>pathOrNull</var>. If
     * <code>pathOrNull == null</code>, the method will return <code>UNKNOWN</code>.
     */
    static String getDataTypeNameFromPath(String pathOrNull, String houseKeepingNameSuffix,
            HDF5DataClass dataClass)
    {
        return (pathOrNull == null) ? "UNKNOWN" : tryGetDataTypeNameFromPath(pathOrNull,
                houseKeepingNameSuffix, dataClass);
    }

    /**
     * Returns the name for a committed data type with <var>pathOrNull</var>. If
     * <code>pathOrNull == null</code>, the method will return <code>null</code>.
     */
    static String tryGetDataTypeNameFromPath(String pathOrNull, String houseKeepingNameSuffix,
            HDF5DataClass dataClass)
    {
        if (pathOrNull == null)
        {
            return null;
        } else
        {
            final String prefix = getPrefixForDataClass(dataClass);
            final String pathPrefix = createDataTypePath(prefix, houseKeepingNameSuffix);
            if (pathOrNull.startsWith(pathPrefix))
            {
                return pathOrNull.substring(pathPrefix.length());
            } else
            {
                final int lastPathSepIdx = pathOrNull.lastIndexOf('/');
                if (lastPathSepIdx >= 0)
                {
                    return pathOrNull.substring(lastPathSepIdx + 1);
                } else
                {
                    return pathOrNull;
                }
            }
        }
    }

    /**
     * Returns a prefix for a given data class, or <code>""</code>, if this data class does not have
     * a prefix.
     */
    static String getPrefixForDataClass(HDF5DataClass dataClass)
    {
        switch (dataClass)
        {
            case COMPOUND:
                return COMPOUND_PREFIX;
            case ENUM:
                return ENUM_PREFIX;
            case OPAQUE:
                return OPAQUE_PREFIX;
            default:
                return "";
        }
    }

    /**
     * Returns the length of a one-dimension array defined by <var>dimensions</var>.
     * 
     * @throws HDF5JavaException If <var>dimensions</var> do not define a one-dimensional array.
     */
    static int getOneDimensionalArraySize(final int[] dimensions)
    {
        assert dimensions != null;

        if (dimensions.length == 0) // Scalar data space needs to be treated differently
        {
            return 1;
        }
        if (dimensions.length != 1)
        {
            throw new HDF5JavaException("Data Set is expected to be of rank 1 (rank="
                    + dimensions.length + ")");
        }
        return dimensions[0];
    }

    /**
     * Returns the length of a one-dimension array defined by <var>dimensions</var>.
     * 
     * @throws HDF5JavaException If <var>dimensions</var> do not define a one-dimensional array or
     *             if <code>dimensions[0]</code> overflows the <code>int</code> type.
     */
    static int getOneDimensionalArraySize(final long[] dimensions)
    {
        assert dimensions != null;

        if (dimensions.length == 0) // Scalar data space needs to be treated differently
        {
            return 1;
        }
        if (dimensions.length != 1)
        {
            throw new HDF5JavaException("Data Set is expected to be of rank 1 (rank="
                    + dimensions.length + ")");
        }
        final int length = (int) dimensions[0];
        if (length != dimensions[0])
        {
            throw new HDF5JavaException("Length is too large (" + dimensions[0] + ")");
        }
        return length;
    }

    /** Returns the attribute to signal that this is a variant of the data type. */
    static String createObjectTypeVariantAttributeName(String houseKeepingNameSuffix)
    {
        return "".equals(houseKeepingNameSuffix) ? "__" + TYPE_VARIANT_ATTRIBUTE_NAME + "__"
                : TYPE_VARIANT_ATTRIBUTE_NAME + houseKeepingNameSuffix;
    }

    /**
     * Returns the type variant attribute for the given <var>attributeName</var>.
     */
    static String createAttributeTypeVariantAttributeName(String attributeName, String suffix)
    {
        final boolean noSuffix = "".equals(suffix);
        return (noSuffix ? "__" : "") + TYPE_VARIANT_ATTRIBUTE_NAME + "__" + attributeName
                + (noSuffix ? "__" : suffix);
    }

    /**
     * Returns <code>true</code>, if <var>name</var> denotes an internal name used by the library
     * for house-keeping.
     */
    private static boolean isInternalName(final String name)
    {
        return name.startsWith("__") && name.endsWith("__");
    }

    /**
     * Returns <code>true</code>, if <var>name</var> denotes an internal name used by the library
     * for house-keeping, given the <var>houseKeepingNameSuffix</var>.
     */
    static boolean isInternalName(String name, String houseKeepingNameSuffix)
    {
        return "".equals(houseKeepingNameSuffix) ? isInternalName(name) : name
                .endsWith(houseKeepingNameSuffix);
    }

    /**
     * Returns <code>true</code> if the given <var>name</var> is an internal name.
     */
    static boolean isInternalName(final String name, final String houseKeepingNameSuffix,
            final boolean filterRootAttributes)
    {
        if (filterRootAttributes)
        {
            return isInternalName(name, houseKeepingNameSuffix)
                    || HOUSEKEEPING_NAME_SUFFIX_ATTRIBUTE_NAME.equals(name)
                    || HOUSEKEEPING_NAME_SUFFIX_STRINGLENGTH_ATTRIBUTE_NAME.equals(name);
        } else
        {
            return isInternalName(name, houseKeepingNameSuffix);
        }
    }

    /**
     * Creates an internal name from the given <var>name</var>, using the
     * <var>houseKeepingNameSuffix</var>.
     */
    static String toHouseKeepingName(String name, String houseKeepingNameSuffix)
    {
        return "".equals(houseKeepingNameSuffix) ? "__" + name + "__" : name
                + houseKeepingNameSuffix;
    }

    /**
     * Creates an internal name from the given <var>objectPath</var>, using the
     * <var>houseKeepingNameSuffix</var>.
     */
    static String toHouseKeepingPath(String objectPath, String houseKeepingNameSuffix)
    {
        final int lastPathSeparator = objectPath.lastIndexOf('/') + 1;
        return lastPathSeparator > 0 ? objectPath.substring(0, lastPathSeparator)
                + toHouseKeepingName(objectPath.substring(lastPathSeparator),
                        houseKeepingNameSuffix) : toHouseKeepingName(objectPath,
                houseKeepingNameSuffix);
    }

    /**
     * Removes all internal names from the list <var>names</var>.
     * 
     * @return The list <var>names</var>.
     */
    static List<String> removeInternalNames(final List<String> names,
            final String houseKeepingNameSuffix, final boolean filterRootAttributes)
    {
        for (Iterator<String> iterator = names.iterator(); iterator.hasNext(); /**/)
        {
            final String memberName = iterator.next();
            if (isInternalName(memberName, houseKeepingNameSuffix, filterRootAttributes))
            {
                iterator.remove();
            }
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    static <T> T[] createArray(final Class<T> componentClass, final int vectorLength)
    {
        final T[] value = (T[]) java.lang.reflect.Array.newInstance(componentClass, vectorLength);
        return value;
    }

    /**
     * If all elements of <var>dimensions</var> are 1, the data set might be empty, then check
     * {@link #DATASET_IS_EMPTY_LEGACY_ATTRIBUTE} (for backward compatibility with 8.10)
     */
    static boolean mightBeEmptyInStorage(final long[] dimensions)
    {
        for (long d : dimensions)
        {
            if (d != 1L)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the consistency of the dimension of a given array.
     * <p>
     * As Java doesn't have a matrix data type, but only arrays of arrays, there is no way to ensure
     * in the language itself whether all rows have the same length.
     * 
     * @return <code>true</code> if the given matrix is consisten and <code>false</code> otherwise.
     */
    static boolean areMatrixDimensionsConsistent(Object a)
    {
        if (a.getClass().isArray() == false)
        {
            return false;
        }
        final int length = Array.getLength(a);
        if (length == 0)
        {
            return true;
        }
        final Object element = Array.get(a, 0);
        if (element.getClass().isArray())
        {
            final int elementLength = Array.getLength(element);
            for (int i = 0; i < length; ++i)
            {
                final Object o = Array.get(a, i);
                if (areMatrixDimensionsConsistent(o) == false)
                {
                    return false;
                }
                if (elementLength != Array.getLength(o))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if <var>subDimensions</var> are in bounds of <var>dimensions</var>.
     */
    static boolean isInBounds(long[] dimensions, long[] subDimensions)
    {
        assert dimensions.length == subDimensions.length;

        for (int i = 0; i < dimensions.length; ++i)
        {
            if (subDimensions[i] > dimensions[i])
            {
                return false;
            }
        }
        return true;
    }

}
