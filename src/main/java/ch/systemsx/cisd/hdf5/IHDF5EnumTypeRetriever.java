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

import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * An interface for retrieving HDF5 enum types. Depending on whether it is reader or a writer that
 * implements it, non-existing enum types may be created by calling the methods of this interface or
 * an exception may be thrown.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#enumeration()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5EnumTypeRetriever
{
    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. Use this method only when
     * you know that the type exists. If the <var>dataTypeName</var> starts with '/', it will be
     * considered a data type path instead of a data type name.
     * 
     * @param dataTypeName The name of the enumeration in the HDF5 file.
     */
    public HDF5EnumerationType getType(String dataTypeName);

    /**
     * Returns the enumeration type for the data set <var>dataSetPath</var>.
     * 
     * @param dataSetPath The name of data set to get the enumeration type for.
     */
    public HDF5EnumerationType getDataSetType(String dataSetPath);

    /**
     * Returns the enumeration type for the data set <var>dataSetPath</var>.
     * 
     * @param dataSetPath The name of data set.
     * @param attributeName The name of the attribute to get the type for.
     */
    public HDF5EnumerationType getAttributeType(String dataSetPath, String attributeName);

    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. If the type is read from the
     * file, it will check the type in the file with the <var>values</var>. If the
     * <var>dataTypeName</var> starts with '/', it will be considered a data type path instead of a
     * data type name.
     * 
     * @param dataTypeName The name of the enumeration in the HDF5 file.
     * @param values The values of the enumeration.
     * @throws HDF5JavaException If the data type exists and is not compatible with the
     *             <var>values</var> provided.
     */
    public HDF5EnumerationType getType(String dataTypeName, String[] values)
            throws HDF5JavaException;

    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. If the type is read from the
     * file, it will check the type in the file with the <var>values</var>. If the
     * <var>dataTypeName</var> starts with '/', it will be considered a data type path instead of a
     * data type name.
     * 
     * @param genericType The generic enum type (independent of this file).
     * @throws HDF5JavaException If the data type exists and is not compatible with the
     *             <var>values</var> provided.
     */
    public HDF5EnumerationType getType(EnumerationType genericType)
            throws HDF5JavaException;

    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. If the
     * <var>dataTypeName</var> starts with '/', it will be considered a data type path instead of a
     * data type name.
     * 
     * @param dataTypeName The name of the enumeration in the HDF5 file.
     * @param values The values of the enumeration.
     * @param check If <code>true</code> and if the data type already exists, check whether it is
     *            compatible with the <var>values</var> provided.
     * @throws HDF5JavaException If <code>check = true</code>, the data type exists and is not
     *             compatible with the <var>values</var> provided.
     */
    public HDF5EnumerationType getType(String dataTypeName, String[] values, boolean check)
            throws HDF5JavaException;

    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. If the type is read from the
     * file, it will check the type in the file with the <var>values</var>. If the
     * <var>dataTypeName</var> starts with '/', it will be considered a data type path instead of a
     * data type name.
     * 
     * @param genericType The generic enum type (independent of this file).
     * @param check If <code>true</code> and if the data type already exists, check whether it is
     *            compatible with the <var>values</var> provided.
     * @throws HDF5JavaException If <code>check = true</code>, the data type exists and is not
     *             compatible with the <var>values</var> provided.
     */
    public HDF5EnumerationType getType(EnumerationType genericType, boolean check)
            throws HDF5JavaException;

    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. Will check the type in the
     * file with the <var>values</var>. If the <var>dataTypeName</var> starts with '/', it will be
     * considered a data type path instead of a data type name.
     * 
     * @param dataTypeName The name of the enumeration in the HDF5 file.
     * @param enumClass The enumeration class to get the values from.
     * @throws HDF5JavaException If the data type exists and is not compatible with the
     *             <var>enumClass</var> provided.
     */
    public HDF5EnumerationType getType(String dataTypeName, Class<? extends Enum<?>> enumClass)
            throws HDF5JavaException;

    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. Will check the type in the
     * file with the <var>values</var>. If the <var>dataTypeName</var> starts with '/', it will be
     * considered a data type path instead of a data type name.
     * 
     * @param dataTypeName The name of the enumeration in the HDF5 file.
     * @param enumClass The enumeration class to get the values from.
     * @param check If <code>true</code> and if the data type already exists, check whether it is
     *            compatible with the <var>enumClass</var> provided.
     * @throws HDF5JavaException If the data type exists and is not compatible with the
     *             <var>values</var> provided.
     */
    public <T extends Enum<?>> HDF5EnumerationType getType(String dataTypeName, Class<T> enumClass,
            boolean check) throws HDF5JavaException;

    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. Will check the type in the
     * file with the <var>values</var>. Will use the simple class name of <var>enumClass</var> as
     * the data type name.
     * 
     * @param enumClass The enumeration class to get the values from.
     * @throws HDF5JavaException If the data type exists and is not compatible with the
     *             <var>values</var> provided.
     */
    public <T extends Enum<?>> HDF5EnumerationType getType(Class<T> enumClass)
            throws HDF5JavaException;

    /**
     * Returns the enumeration type <var>name</var> for this HDF5 file. Will check the type in the
     * file with the <var>values</var>. Will use the simple class name of <var>enumClass</var> as
     * the data type name.
     * 
     * @param enumClass The enumeration class to get the values from.
     * @param check If <code>true</code> and if the data type already exists, check whether it is
     *            compatible with the <var>enumClass</var> provided.
     * @throws HDF5JavaException If the data type exists and is not compatible with the
     *             <var>values</var> provided.
     */
    public HDF5EnumerationType getType(Class<? extends Enum<?>> enumClass, boolean check)
            throws HDF5JavaException;
}
