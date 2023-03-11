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
 * An enumeration that represents the basic HDF5 object types.
 * 
 * @author Bernd Rinn
 */
public enum HDF5ObjectType
{
    DATASET, DATATYPE, GROUP, SOFT_LINK, EXTERNAL_LINK, OTHER, NONEXISTENT;

    /**
     * Returns <code>false</code>, if the <var>objectTypeOrNull</var> is equal to
     * {@link #NONEXISTENT} and <code>true</code> otherwise.
     * 
     * @param objectType The object type to check.
     */
    public static boolean exists(HDF5ObjectType objectType)
    {
        return (objectType != NONEXISTENT);
    }

    /**
     * Returns <code>true</code>, if the <var>objectTypeOrNull</var> is equal to {@link #GROUP} and
     * <code>false</code> otherwise.
     * 
     * @param objectType The object type to check.
     */
    public static boolean isGroup(HDF5ObjectType objectType)
    {
        return (objectType == GROUP);
    }

    /**
     * Returns <code>true</code>, if the <var>objectTypeOrNull</var> is equal to {@link #DATASET}
     * and <code>false</code> otherwise.
     * 
     * @param objectType The object type to check.
     */
    public static boolean isDataSet(HDF5ObjectType objectType)
    {
        return (objectType == DATASET);
    }

    /**
     * Returns <code>true</code>, if the <var>objectTypeOrNull</var> is equal to {@link #DATATYPE}
     * and <code>false</code> otherwise.
     * 
     * @param objectType The object type to check.
     */
    public static boolean isDataType(HDF5ObjectType objectType)
    {
        return (objectType == DATATYPE);
    }

    /**
     * Returns <code>true</code>, if the <var>objectTypeOrNull</var> is equal to {@link #SOFT_LINK}
     * and <code>false</code> otherwise.
     * 
     * @param objectType The object type to check.
     */
    public static boolean isSoftLink(HDF5ObjectType objectType)
    {
        return (objectType == SOFT_LINK);
    }

    /**
     * Returns <code>true</code>, if the <var>objectTypeOrNull</var> is equal to
     * {@link #EXTERNAL_LINK} and <code>false</code> otherwise.
     * 
     * @param objectType The object type to check (can be <code>null</code>).
     */
    public static boolean isExternalLink(HDF5ObjectType objectType)
    {
        return (objectType == EXTERNAL_LINK);
    }

    /**
     * Returns <code>true</code>, if the <var>objectTypeOrNull</var> is equal to either
     * {@link #SOFT_LINK} or {@link #EXTERNAL_LINK}. and <code>false</code> otherwise.
     * 
     * @param objectType The object type to check.
     */
    public static boolean isSymbolicLink(HDF5ObjectType objectType)
    {
        return (objectType == SOFT_LINK) || (objectType == EXTERNAL_LINK);
    }
}
