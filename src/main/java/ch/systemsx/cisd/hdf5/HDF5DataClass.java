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

import static hdf.hdf5lib.HDF5Constants.H5T_BITFIELD;
import static hdf.hdf5lib.HDF5Constants.H5T_COMPOUND;
import static hdf.hdf5lib.HDF5Constants.H5T_ENUM;
import static hdf.hdf5lib.HDF5Constants.H5T_FLOAT;
import static hdf.hdf5lib.HDF5Constants.H5T_INTEGER;
import static hdf.hdf5lib.HDF5Constants.H5T_OPAQUE;
import static hdf.hdf5lib.HDF5Constants.H5T_REFERENCE;
import static hdf.hdf5lib.HDF5Constants.H5T_STRING;

import java.util.BitSet;
import java.util.Map;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDDoubleArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5CompoundByteifyerFactory.IHDF5CompoundMemberBytifyerFactory;

/**
 * Identifies the class of a data type. Note that for array types the class of the elements is
 * identified.
 * 
 * @author Bernd Rinn
 */
public enum HDF5DataClass
{
    // Implementation note: The order matters! ENUM needs to be before INTEGER, as H5Tdetect_class
    // will return TRUE for ENUM arrays when trying to detect an INTEGER class.
    BITFIELD(H5T_BITFIELD, new BasicJavaTypeProvider(BitSet.class, null, null, null)), ENUM(
            H5T_ENUM, new BasicJavaTypeProvider(HDF5EnumerationValue.class,
                    HDF5EnumerationValueArray.class, null, null)), INTEGER(H5T_INTEGER,
            new IntJavaTypeProvider()), FLOAT(H5T_FLOAT, new FloatJavaTypeProvider()), STRING(
            H5T_STRING, new BasicJavaTypeProvider(String.class, String[].class, String[][].class,
                    MDArray.class)), OPAQUE(H5T_OPAQUE, new BasicJavaTypeProvider(byte.class,
            byte[].class, byte[][].class, MDByteArray.class)), BOOLEAN(-1,
            new BasicJavaTypeProvider(boolean.class, BitSet.class, null, null)), COMPOUND(
            H5T_COMPOUND, new BasicJavaTypeProvider(Map.class, Map[].class, Map[][].class,
                    MDArray.class)), REFERENCE(H5T_REFERENCE, new BasicJavaTypeProvider(
            String.class, String[].class, String[][].class, MDArray.class)), OTHER(-1,
            new BasicJavaTypeProvider(null, null, null, null));

    /**
     * A role that can provide a java type for a data class, rank and element size.
     */
    interface IHDF5JavaTypeProvider
    {
        Class<?> tryGetJavaType(int rank, int elementSize, HDF5DataTypeVariant typeVariantOrNull);
    }

    private final int id;

    private final IHDF5JavaTypeProvider typeProvider;

    HDF5DataClass(int id, IHDF5JavaTypeProvider typeProvider)
    {
        this.id = id;
        this.typeProvider = typeProvider;
    }

    int getId()
    {
        return id;
    }

    /**
     * Returns a {@link IHDF5JavaTypeProvider} that returns the default Java type for this data
     * class.
     * <p>
     * Overriding the default for particular choices should be done by one of the
     * {@link IHDF5CompoundMemberBytifyerFactory}s in
     * {@link IHDF5CompoundMemberBytifyerFactory#tryGetOverrideJavaType(HDF5DataClass, int, int, HDF5DataTypeVariant)}.
     */
    IHDF5JavaTypeProvider getJavaTypeProvider()
    {
        return typeProvider;
    }

    /**
     * Returns the {@link HDF5DataClass} for the given data <var>classId</var>.
     * <p>
     * <b>Note:</b> This method will never return {@link #BOOLEAN}, but instead it will return
     * {@link #ENUM} for a boolean value as boolean values are actually enums in the HDF5 file.
     */
    static HDF5DataClass classIdToDataClass(final int classId)
    {
        for (HDF5DataClass clazz : values())
        {
            if (clazz.id == classId)
            {
                return clazz;
            }
        }
        return OTHER;
    }

    //
    // Auxiliary classes
    //

    private static class BasicJavaTypeProvider implements IHDF5JavaTypeProvider
    {
        private final Class<?> javaTypeScalarOrNull;

        private final Class<?> javaType1DArrayOrNull;

        private final Class<?> javaType2DArrayOrNull;

        private final Class<?> javaTypeMDArrayOrNull;

        BasicJavaTypeProvider(Class<?> javaTypeScalarOrNull, Class<?> javaType1DArrayOrNull,
                Class<?> javaType2DArrayOrNull, Class<?> javaTypeMDArrayOrNull)
        {
            this.javaTypeScalarOrNull = javaTypeScalarOrNull;
            this.javaType1DArrayOrNull = javaType1DArrayOrNull;
            this.javaType2DArrayOrNull = javaType2DArrayOrNull;
            this.javaTypeMDArrayOrNull = javaTypeMDArrayOrNull;
        }

        @Override
        public Class<?> tryGetJavaType(int rank, int elementSize,
                HDF5DataTypeVariant typeVariantOrNull)
        {
            if (rank == 0)
            {
                return javaTypeScalarOrNull;
            } else if (rank == 1)
            {
                return javaType1DArrayOrNull;
            } else if (rank == 2)
            {
                return javaType2DArrayOrNull;
            } else
            {
                return javaTypeMDArrayOrNull;
            }
        }
    }

    private static class IntJavaTypeProvider implements IHDF5JavaTypeProvider
    {
        @Override
        public Class<?> tryGetJavaType(int rank, int elementSize,
                HDF5DataTypeVariant typeVariantOrNull)
        {
            if (rank == 0)
            {
                switch (elementSize)
                {
                    case 1:
                        return byte.class;
                    case 2:
                        return short.class;
                    case 4:
                        return int.class;
                    case 8:
                        return long.class;
                    default:
                        return null;
                }
            } else if (rank == 1)
            {
                switch (elementSize)
                {
                    case 1:
                        return byte[].class;
                    case 2:
                        return short[].class;
                    case 4:
                        return int[].class;
                    case 8:
                        return long[].class;
                    default:
                        return null;
                }
            } else if (rank == 2)
            {
                switch (elementSize)
                {
                    case 1:
                        return byte[][].class;
                    case 2:
                        return short[][].class;
                    case 4:
                        return int[][].class;
                    case 8:
                        return long[][].class;
                    default:
                        return null;
                }
            } else
            {
                switch (elementSize)
                {
                    case 1:
                        return MDByteArray.class;
                    case 2:
                        return MDShortArray.class;
                    case 4:
                        return MDIntArray.class;
                    case 8:
                        return MDLongArray.class;
                    default:
                        return null;
                }
            }
        }
    }

    private static class FloatJavaTypeProvider implements IHDF5JavaTypeProvider
    {
        @Override
        public Class<?> tryGetJavaType(int rank, int elementSize,
                HDF5DataTypeVariant typeVariantOrNull)
        {
            if (rank == 0)
            {
                switch (elementSize)
                {
                    case 4:
                        return float.class;
                    case 8:
                        return double.class;
                    default:
                        return null;
                }
            } else if (rank == 1)
            {
                switch (elementSize)
                {
                    case 4:
                        return float[].class;
                    case 8:
                        return double[].class;
                    default:
                        return null;
                }
            } else if (rank == 2)
            {
                switch (elementSize)
                {
                    case 4:
                        return float[][].class;
                    case 8:
                        return double[][].class;
                    default:
                        return null;
                }
            } else
            {
                switch (elementSize)
                {
                    case 4:
                        return MDFloatArray.class;
                    case 8:
                        return MDDoubleArray.class;
                    default:
                        return null;
                }
            }
        }
    }

}