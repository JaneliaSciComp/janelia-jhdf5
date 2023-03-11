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

import java.util.HashMap;
import java.util.Map;

/**
 * A class to store general hints that can influence the compound member mapping.
 * 
 * @author Bernd Rinn
 */
public class HDF5CompoundMappingHints
{
    public enum EnumReturnType
    {
        ORDINAL, STRING, JAVAENUMERATION, HDF5ENUMERATIONVALUE
    }

    private EnumReturnType enumReturnType = EnumReturnType.HDF5ENUMERATIONVALUE;

    private boolean useVariableLengthStrings = false;

    private Map<String, HDF5EnumerationType> enumerationTypeMap;

    /**
     * Returns the desired return type for enums.
     */
    public EnumReturnType getEnumReturnType()
    {
        return enumReturnType;
    }

    /**
     * Sets the desired return type for enums.
     */
    public void setEnumReturnType(EnumReturnType enumReturnType)
    {
        this.enumReturnType = enumReturnType;
    }

    /**
     * Sets the return type for enums .
     * 
     * @return This object (for chaining)
     */
    public HDF5CompoundMappingHints enumReturnType(@SuppressWarnings("hiding")
    EnumReturnType enumReturnType)
    {
        this.enumReturnType = enumReturnType;
        return this;
    }

    /**
     * Adds an enum type mapping to this hints object.
     * 
     * @return The hint object.
     */
    public HDF5CompoundMappingHints enumTypeMapping(String memberName, HDF5EnumerationType enumType)
    {
        if (enumerationTypeMap == null)
        {
            enumerationTypeMap = new HashMap<String, HDF5EnumerationType>();
        }
        enumerationTypeMap.put(memberName, enumType);
        return this;
    }

    /**
     * Replaces the enum type mapping of this hints object.
     * 
     * @return The hint object.
     */
    public HDF5CompoundMappingHints enumTypeMapping(Map<String, HDF5EnumerationType> enumTypeMapping)
    {
        enumerationTypeMap = enumTypeMapping;
        return this;
    }

    /**
     * Returns the {@link HDF5EnumerationType} for the given <var>memberName</var>, or
     * <code>null</code>, if no mapping is available for this member.
     */
    public HDF5EnumerationType tryGetEnumType(String memberName)
    {
        if (enumerationTypeMap == null)
        {
            return null;
        }
        return enumerationTypeMap.get(memberName);
    }

    /**
     * Returns the desired enumeration return type.
     */
    public static EnumReturnType getEnumReturnType(HDF5CompoundMemberMapping mapping)
    {
        return (mapping.tryGetHints() == null) ? EnumReturnType.HDF5ENUMERATIONVALUE : mapping
                .tryGetHints().getEnumReturnType();
    }

    /**
     * Returns whether variable-length-string types should be used if the length is not set
     * explicitly.
     */
    public static boolean isUseVariableLengthStrings(HDF5CompoundMappingHints hintsOrNull)
    {
        return hintsOrNull == null ? false : hintsOrNull.useVariableLengthStrings;
    }

    /**
     * Returns whether variable-length-string types should be used if the length is not set
     * explicitly.
     */
    public boolean isUseVariableLengthStrings()
    {
        return useVariableLengthStrings;
    }

    /**
     * Sets whether variable-length-string types should be used if the length is not set explicitly.
     */
    public void setUseVariableLengthStrings(boolean useVariableLengthStrings)
    {
        this.useVariableLengthStrings = useVariableLengthStrings;
    }

    /**
     * Sets that variable-length-string types should be used if the length is not set explicitly.
     * 
     * @return The hint object.
     */
    public HDF5CompoundMappingHints useVariableLengthStrings()
    {
        this.useVariableLengthStrings = true;
        return this;
    }

    /**
     * Sets whether variable-length-string types should be used if the length is not set explicitly.
     * 
     * @return The hint object.
     */
    public HDF5CompoundMappingHints useVariableLengthStrings(@SuppressWarnings("hiding")
    boolean useVariableLengthStrings)
    {
        this.useVariableLengthStrings = useVariableLengthStrings;
        return this;
    }
}