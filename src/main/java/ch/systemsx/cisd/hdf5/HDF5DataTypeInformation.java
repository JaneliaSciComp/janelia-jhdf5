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

import java.util.Objects;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;

/**
 * A class that holds relevant information about a data type.
 * 
 * @author Bernd Rinn
 */
public final class HDF5DataTypeInformation
{
    /**
     * An object that represents the options for a data type information object.
     * 
     * @author Bernd Rinn
     */
    public static final class DataTypeInfoOptions
    {
        public static final DataTypeInfoOptions MINIMAL = new DataTypeInfoOptions(false, false);

        public static final DataTypeInfoOptions ALL = new DataTypeInfoOptions(true, true);

        public static final DataTypeInfoOptions DEFAULT = new DataTypeInfoOptions(false, true);

        public static final DataTypeInfoOptions PATH = new DataTypeInfoOptions(true, false);

        private boolean knowsDataTypePath;

        private boolean knowsDataTypeVariant;

        DataTypeInfoOptions(boolean knowsDataTypePath, boolean knowsDataTypeVariant)
        {
            this.knowsDataTypePath = knowsDataTypePath;
            this.knowsDataTypeVariant = knowsDataTypeVariant;
        }

        DataTypeInfoOptions()
        {
            knowsDataTypePath = false;
            knowsDataTypeVariant = true;
        }

        public DataTypeInfoOptions path(boolean readDataTypePath)
        {
            this.knowsDataTypePath = readDataTypePath;
            return this;
        }

        public DataTypeInfoOptions path()
        {
            this.knowsDataTypePath = true;
            return this;
        }

        public DataTypeInfoOptions variant(boolean readDataTypeVariant)
        {
            this.knowsDataTypeVariant = readDataTypeVariant;
            return this;
        }

        public DataTypeInfoOptions noVariant()
        {
            this.knowsDataTypeVariant = false;
            return this;
        }

        public DataTypeInfoOptions all()
        {
            this.knowsDataTypePath = true;
            this.knowsDataTypeVariant = true;
            return this;
        }

        public DataTypeInfoOptions nothing()
        {
            this.knowsDataTypePath = false;
            this.knowsDataTypeVariant = false;
            return this;
        }

        public boolean knowsDataTypePath()
        {
            return knowsDataTypePath;
        }

        public boolean knowsDataTypeVariant()
        {
            return knowsDataTypeVariant;
        }

    }

    /**
     * Returns a new {@link DataTypeInfoOptions} object.
     */
    public static DataTypeInfoOptions options()
    {
        return new DataTypeInfoOptions();
    }

    private final boolean arrayType;

    private final boolean signed;

    private final boolean variableLengthString;

    private final String dataTypePathOrNull;

    private final String nameOrNull;

    private final HDF5DataClass dataClass;

    private int elementSize;

    private int numberOfElements;

    private CharacterEncoding encoding;

    private int[] dimensions;

    private String opaqueTagOrNull;

    private final DataTypeInfoOptions options;

    private HDF5DataTypeVariant typeVariantOrNull;

    HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, String houseKeepingNameSuffix, int elementSize, boolean signed)
    {
        this(dataTypePathOrNull, options, dataClass, CharacterEncoding.ASCII,
                houseKeepingNameSuffix, elementSize, 1, new int[0], false, signed, false, null);
    }

    HDF5DataTypeInformation(HDF5DataClass dataClass, String houseKeepingNameSuffix,
            int elementSize, boolean signed)
    {
        this(null, DataTypeInfoOptions.ALL, dataClass, CharacterEncoding.ASCII,
                houseKeepingNameSuffix, elementSize, 1, new int[0], false, signed, false, null);
    }

    HDF5DataTypeInformation(HDF5DataClass dataClass, String houseKeepingNameSuffix,
            int elementSize, int numberOfElements, boolean signed)
    {
        this(null, DataTypeInfoOptions.ALL, dataClass, CharacterEncoding.ASCII,
                houseKeepingNameSuffix, elementSize, numberOfElements, new int[]
                    { numberOfElements }, false, signed, false, null);

    }

    HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, CharacterEncoding encoding, String houseKeepingNameSuffix,
            int elementSize, boolean signed, boolean variableLengthString,
            String opaqueTagOrNull)
    {
        this(dataTypePathOrNull, options, dataClass, encoding, houseKeepingNameSuffix, elementSize,
                1, new int[0], false, signed, variableLengthString, opaqueTagOrNull);
    }

    HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, CharacterEncoding encoding, String houseKeepingNameSuffix,
            int elementSize, int[] dimensions, boolean arrayType, boolean signed,
            boolean variableLengthString)
    {
        this(dataTypePathOrNull, options, dataClass, encoding, houseKeepingNameSuffix, elementSize,
                MDAbstractArray.getLength(dimensions), dimensions, arrayType, signed,
                variableLengthString, null);

    }

    private HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, CharacterEncoding encoding, String houseKeepingNameSuffix,
            int elementSize, int numberOfElements, int[] dimensions, boolean arrayType,
            boolean signed, boolean variableLengthString, String opaqueTagOrNull)
    {
        if (dataClass == HDF5DataClass.BOOLEAN || dataClass == HDF5DataClass.STRING)
        {
            this.dataTypePathOrNull = null;
            this.nameOrNull = null;
        } else
        {
            this.dataTypePathOrNull = dataTypePathOrNull;
            this.nameOrNull =
                    HDF5Utils.tryGetDataTypeNameFromPath(dataTypePathOrNull,
                            houseKeepingNameSuffix, dataClass);
        }
        this.arrayType = arrayType;
        this.signed = signed;
        this.variableLengthString = variableLengthString;
        this.dataClass = dataClass;
        this.elementSize = elementSize;
        this.numberOfElements = numberOfElements;
        this.dimensions = dimensions;
        this.encoding = encoding;
        this.opaqueTagOrNull = opaqueTagOrNull;
        this.options = options;
    }

    /**
     * Returns the raw data class (<code>INTEGER</code>, <code>FLOAT</code>, ...) of this type.
     * <p>
     * May differ from {@link #getDataClass()} if it is the type of a scaled enum (
     * {@link HDF5DataTypeVariant#ENUM} or a scaled bitfield (@link
     * {@link HDF5DataTypeVariant#BITFIELD}.
     */
    public HDF5DataClass getRawDataClass()
    {
        return dataClass;
    }

    /**
     * Returns the data class (<code>INTEGER</code>, <code>FLOAT</code>, ...) of this type.
     */
    public HDF5DataClass getDataClass()
    {
        if (typeVariantOrNull == HDF5DataTypeVariant.ENUM)
        {
            return HDF5DataClass.ENUM;
        } else if (typeVariantOrNull == HDF5DataTypeVariant.BITFIELD)
        {
            return HDF5DataClass.BITFIELD;
        } else
        {
            return dataClass;
        }
    }

    /**
     * Returns the size of one element (in bytes) of this type. For strings, the total length.
     */
    public int getElementSize()
    {
        return elementSize;
    }

    /**
     * The length that is usable. Usually equals to {@link #getElementSize()}, except for Strings,
     * where it takes into account the character encoding.
     */
    public int getUsableLength()
    {
        if (dataClass == HDF5DataClass.STRING && elementSize > 0)
        {
            return variableLengthString ? -1 : elementSize / encoding.getMaxBytesPerChar();
        } else
        {
            return elementSize;
        }
    }

    /**
     * The element size as is relevant for padding to ensure memory alignment.
     */
    public int getElementSizeForPadding()
    {
        // Variable-length strings store a pointer.
        if (variableLengthString)
        {
            return HDFHelper.getMachineWordSize();
        }
        // Fixed-length strings are accessing single bytes.
        if (dataClass == HDF5DataClass.STRING)
        {
            return 1;
        }
        // Otherwise: use elementSize.
        return elementSize;
    }

    void setElementSize(int elementSize)
    {
        this.elementSize = elementSize;
    }

    /**
     * Returns the number of elements of this type.
     * <p>
     * This will be 1 except for array data types.
     */
    public int getNumberOfElements()
    {
        return numberOfElements;
    }

    /**
     * Returns the total size (in bytes) of this data set.
     */
    public int getSize()
    {
        return elementSize * numberOfElements;
    }

    /**
     * Returns the rank (number of dimensions) of this type (0 for a scalar type).
     */
    public int getRank()
    {
        return dimensions.length;
    }

    /**
     * Returns the dimensions along each axis of this type (an empty array for a scalar type).
     */
    public int[] getDimensions()
    {
        return dimensions;
    }

    void setDimensions(int[] dimensions)
    {
        this.dimensions = dimensions;
        this.numberOfElements = MDAbstractArray.getLength(dimensions);
    }

    /**
     * Returns <code>true</code> if this type is an HDF5 array type.
     */
    public boolean isArrayType()
    {
        return arrayType;
    }

    /**
     * Returns <code>true</code>, if this data set type has a sign anf <code>false</code> otherwise.
     */
    public boolean isSigned()
    {
        return signed;
    }

    /**
     * Returns <code>true</code>, if this data set type is a variable-length string, or
     * <code>false</code> otherwise.
     */
    public boolean isVariableLengthString()
    {
        return variableLengthString;
    }

    /**
     * Returns the tag of an opaque data type, or <code>null</code>, if this data type is not
     * opaque.
     */
    public String tryGetOpaqueTag()
    {
        return opaqueTagOrNull;
    }

    /**
     * Returns whether the data type path has been determined.
     * <p>
     * A return value of <code>true</code> does <i>not necessarily</i> mean that
     * {@link #tryGetDataTypePath()} will return a value other than <code>null</code>, but a return
     * value of <code>false</code> means that this method will always return <code>null</code>.
     */
    public boolean knowsDataTypePath()
    {
        return options.knowsDataTypePath();
    }

    /**
     * If this is a committed (named) data type and {@link #knowsDataTypePath()} ==
     * <code>true</code>, return the path of the data type. Otherwise <code>null</code> is returned.
     */
    public String tryGetDataTypePath()
    {
        return dataTypePathOrNull;
    }

    /**
     * Returns the name of this datatype, if it is a committed data type.
     */
    public String tryGetName()
    {
        return nameOrNull;
    }

    /**
     * Returns whether the data type variant has been determined.
     * <p>
     * A return value of <code>true</code> does <i>not necessarily</i> mean that
     * {@link #tryGetTypeVariant()} will return a value other than <code>null</code>, but a return
     * value of <code>false</code> means that this method will always return <code>null</code>.
     */
    public boolean knowsDataTypeVariant()
    {
        return options.knowsDataTypeVariant;
    }

    /**
     * Returns the {@link HDF5DataTypeVariant}, or <code>null</code>, if this type has no variant or
     * {@link #knowsDataTypeVariant()} == <code>false</code>.
     */
    public HDF5DataTypeVariant tryGetTypeVariant()
    {
        if (typeVariantOrNull == null && options.knowsDataTypeVariant())
        {
            return HDF5DataTypeVariant.NONE;
        } else
        {
            return typeVariantOrNull;
        }
    }

    private HDF5DataTypeVariant tryGetTypeVariantReplaceNoneWithNull()
    {
        return (typeVariantOrNull == HDF5DataTypeVariant.NONE) ? null : typeVariantOrNull;
    }

    void setTypeVariant(HDF5DataTypeVariant typeVariant)
    {
        this.typeVariantOrNull = typeVariant;
    }

    /**
     * Returns <code>true</code>, if the data set is a time stamp, or <code>false</code> otherwise.
     */
    public boolean isTimeStamp()
    {
        return (typeVariantOrNull != null) ? typeVariantOrNull.isTimeStamp() : false;
    }

    /**
     * Returns <code>true</code>, if the data set is a time duration, or <code>false</code>
     * otherwise.
     */
    public boolean isTimeDuration()
    {
        return (typeVariantOrNull != null) ? typeVariantOrNull.isTimeDuration() : false;
    }

    /**
     * Returns the time unit of the data set, if the data set is a time duration, or
     * <code>null</code> otherwise.
     */
    public HDF5TimeUnit tryGetTimeUnit()
    {
        return (typeVariantOrNull != null) ? typeVariantOrNull.tryGetTimeUnit() : null;
    }

    /**
     * Returns <code>true</code>, if the data set is an enumeration type.
     */
    public boolean isEnum()
    {
        return getDataClass() == HDF5DataClass.ENUM;
    }

    /**
     * Returns <code>true</code>, if the data set is a bitfield type.
     */
    public boolean isBitField()
    {
        return getDataClass() == HDF5DataClass.BITFIELD;
    }

    /**
     * Returns an appropriate Java type, or <code>null</code>, if this HDF5 type has no appropriate
     * Java type.
     */
    public Class<?> tryGetJavaType()
    {
        final int rank = (dimensions.length == 1 && dimensions[0] == 1) ? 0 : dimensions.length;
        final Class<?> overrideDataTypeOrNull =
                HDF5CompoundByteifyerFactory.tryGetOverrideJavaType(dataClass, rank, elementSize,
                        typeVariantOrNull);
        if (overrideDataTypeOrNull != null)
        {
            return overrideDataTypeOrNull;
        } else
        {
            return dataClass.getJavaTypeProvider().tryGetJavaType(rank, elementSize,
                    typeVariantOrNull);
        }
    }

    //
    // Object
    //

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj instanceof HDF5DataTypeInformation == false)
        {
            return false;
        }
        final HDF5DataTypeInformation that = (HDF5DataTypeInformation) obj;
        final HDF5DataTypeVariant thisTypeVariant = tryGetTypeVariant();
        final HDF5DataTypeVariant thatTypeVariant = that.tryGetTypeVariant();
        return dataClass.equals(that.dataClass) && elementSize == that.elementSize
                && encoding == that.encoding && numberOfElements == that.numberOfElements
                && Objects.equals(nameOrNull, that.nameOrNull)
                && Objects.equals(dataTypePathOrNull, that.dataTypePathOrNull)
                && Objects.equals(thisTypeVariant, thatTypeVariant);
    }

    @Override
    public int hashCode()
    {
        final HDF5DataTypeVariant typeVariant = tryGetTypeVariant();
        return ((((((17 * 59 + dataClass.hashCode()) * 59 + elementSize) * 59 + Objects
                .hashCode(encoding)) * 59 + numberOfElements) * 59 + Objects
                .hashCode(nameOrNull)) * 59 + Objects.hashCode(dataTypePathOrNull) * 59)
                + Objects.hashCode(typeVariant);
    }

    @Override
    public String toString()
    {
        final String name;
        if (nameOrNull != null)
        {
            name = "<" + nameOrNull + ">";
        } else
        {
            name = "";
        }
        final HDF5DataTypeVariant variantOrNull = tryGetTypeVariantReplaceNoneWithNull();
        if (numberOfElements == 1)
        {
            if (variantOrNull != null)
            {
                return name + dataClass + "(" + getUsableLength() + ")/" + variantOrNull.toString();
            } else
            {
                return name + dataClass + "(" + getUsableLength() + ")";
            }
        } else if (dimensions.length == 1)
        {
            if (variantOrNull != null)
            {
                return name + dataClass + "(" + getUsableLength() + ", #" + numberOfElements + ")/"
                        + variantOrNull.toString();
            } else
            {
                return name + dataClass + "(" + getUsableLength() + ", #" + numberOfElements + ")";
            }
        } else
        {
            final StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append(dataClass.toString());
            builder.append('(');
            builder.append(getUsableLength());
            builder.append(", [");
            for (int d : dimensions)
            {
                builder.append(d);
                builder.append(',');
            }
            if (dimensions.length > 0)
            {
                builder.setLength(builder.length() - 1);
            }
            builder.append(']');
            builder.append(')');
            if (typeVariantOrNull != null)
            {
                builder.append('/');
                builder.append(typeVariantOrNull.toString());
            }
            return builder.toString();
        }
    }
}
