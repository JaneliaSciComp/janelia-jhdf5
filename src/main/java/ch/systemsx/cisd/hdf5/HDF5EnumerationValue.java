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
 * A class the represents an HDF enumeration value.
 * 
 * @author Bernd Rinn
 */
public final class HDF5EnumerationValue
{
    private final HDF5EnumerationType type;

    private final int ordinal;

    /**
     * Creates an enumeration value.
     * 
     * @param type The enumeration type of this value.
     * @param value The value in the <var>type</var>.
     * @throws IllegalArgumentException If the <var>ordinal</var> is outside of the range of allowed
     *             values of the <var>type</var>.
     */
    public HDF5EnumerationValue(HDF5EnumerationType type, Enum<?> value)
            throws IllegalArgumentException
    {
        this(type, value.name());
    }

    /**
     * Creates an enumeration value.
     * 
     * @param type The enumeration type of this value.
     * @param ordinal The ordinal value of the value in the <var>type</var>.
     * @throws IllegalArgumentException If the <var>ordinal</var> is outside of the range of allowed
     *             values of the <var>type</var>.
     */
    public HDF5EnumerationValue(HDF5EnumerationType type, int ordinal)
            throws IllegalArgumentException
    {
        assert type != null;

        if (ordinal < 0 || ordinal >= type.getEnumType().getValueArray().length)
        {
            throw new IllegalArgumentException("valueIndex " + ordinal
                    + " out of allowed range [0.." + (type.getEnumType().getValueArray().length - 1)
                    + "] of type '" + type.getName() + "'.");
        }
        this.type = type;
        this.ordinal = ordinal;
    }

    /**
     * Creates an enumeration value.
     * 
     * @param type The enumeration type of this value.
     * @param value The string value (needs to be one of the values of <var>type</var>).
     * @throws IllegalArgumentException If the <var>value</var> is not one of the values of
     *             <var>type</var>.
     */
    public HDF5EnumerationValue(HDF5EnumerationType type, String value)
            throws IllegalArgumentException
    {
        assert type != null;
        assert value != null;

        final Integer valueIndexOrNull = type.tryGetIndexForValue(value);
        if (valueIndexOrNull == null)
        {
            throw new IllegalArgumentException("Value '" + value + "' is not allowed for type '"
                    + type.getName() + "'.");
        }
        this.type = type;
        this.ordinal = valueIndexOrNull;
    }

    /**
     * Returns the <var>type</var> of this enumeration value.
     */
    public HDF5EnumerationType getType()
    {
        return type;
    }

    /**
     * Returns the string value.
     */
    public String getValue()
    {
        return type.getEnumType().getValueArray()[ordinal];
    }

    /**
     * Returns the ordinal value.
     */
    public int getOrdinal()
    {
        return ordinal;
    }

    /**
     * Returns the value as Enum of type <var>enumClass</var>.
     */
    public <T extends Enum<T>> T getValue(Class<T> enumClass)
    {
        return Enum.valueOf(enumClass, getValue());
    }

    /**
     * Returns a description of this value.
     */
    public String getDescription()
    {
        return type.getName() + " [" + type.getEnumType().getValueArray()[ordinal] + "]";
    }

    byte[] toStorageForm()
    {
        return type.getEnumType().toStorageForm(ordinal);
    }

    //
    // Object
    //

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ordinal;
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
        HDF5EnumerationValue other = (HDF5EnumerationValue) obj;
        if (type == null)
        {
            if (other.type != null)
            {
                return false;
            }
        } else if (type.equals(other.type) == false)
        {
            return false;
        }
        if (ordinal != other.ordinal)
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return getValue();
    }

}
