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

import java.util.List;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;

/**
 * An interface to get information on HDF5 compound data sets and compound data types, and to create
 * compound types from mappings to Java classes.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#compound()}.
 * <p>
 * <h2>What is an {@link HDF5CompoundType}?</h2>
 * <p>
 * A {@link HDF5CompoundType} is a Java object representing both an HDF5 compound type in a
 * particular HDF5 file and the mapping of this HDF5 compound type to a representation in Java. A
 * Java representation can be either a plain-old Java object (POJO) where Java fields correspond to
 * HDF5 compound members, a map (see {@link HDF5CompoundDataMap}) where each HDF5 compound member is
 * represented by one key-value pair, a list (see {@link HDF5CompoundDataList}) or
 * <code>Object[]</code>, where the members of the HDF5 compound type are stored by their position
 * (or order) in the HDF5 compound type.
 * <p>
 * It is important to understand that creating the HDF5 compound type in memory (what members of what types 
 * it contains in what order) and mapping the members to Java (including the Java type and, for POJOs, the 
 * field) are two distinct steps. Different methods of this interface use two different approaches on how 
 * to create the HDF5 compound type: <code>getType()</code> and <code>getInferredType()</code> create them 
 * anew, based on the POJO class and the <code>HDF5CompoundMemberMapping</code>s provided, while 
 * <code>getNamedType()</code>, <code>getDataSetType()</code> and <code>getAttributeType()</code> read them 
 * from the HDF5 file. Whenever you are reading a compound from an HDF5 file, the second approach should be 
 * preferred as the HDF5 file is the authorative source of information on HDF5 types.
 * <p>
 * The following Java types can be mapped to compound members:
 * <ul>
 * <li>Primitive values</li>
 * <li>Primitive arrays</li>
 * <li>Primitive matrices (except <code>char[][]</code>)</li>
 * <li>{@link String} (fixed-length and variable-lengt)</li>
 * <li>{@link java.util.BitSet}</li>
 * <li>{@link java.util.Date}</li>
 * <li>{@link HDF5EnumerationValue}</li>
 * <li>{@link HDF5EnumerationValueArray}</li>
 * <li>Sub-classes of {@link MDAbstractArray}</li>
 * <li>References to data sets</li>
 * </ul>
 * 
 * @author Bernd Rinn
 */
public interface IHDF5CompoundInformationRetriever
{

    /**
     * An interface for inspecting the byte array of compounds and compound arrays just after they
     * are read from or before they are written to the HDF5 file.
     */
    public interface IByteArrayInspector
    {
        /**
         * Called with the byte array. The method can change the <var>byteArray</var> but does so on
         * its own risk!
         */
        void inspect(byte[] byteArray);
    }

    // /////////////////////
    // Information
    // /////////////////////

    /**
     * Returns the member information for the committed compound data type <var>compoundClass</var>
     * (using its "simple name") in the order that the members appear in the compound type. It is a
     * failure condition if this compound data type does not exist.
     */
    public <T> HDF5CompoundMemberInformation[] getMemberInfo(Class<T> compoundClass);

    /**
     * Returns the member information for the committed compound data type <var>dataTypeName</var>
     * in the order that the members appear in the compound type. It is a failure condition if this
     * compound data type does not exist. If the <var>dataTypeName</var> starts with '/', it will be
     * considered a data type path instead of a data type name.
     * 
     * @param dataTypeName The name of the compound data type to get the member information for.
     */
    public HDF5CompoundMemberInformation[] getMemberInfo(String dataTypeName);

    /**
     * Returns the member information for the committed compound data type <var>dataTypeName</var>
     * in the order that the members appear in the compound type. It is a failure condition if this
     * compound data type does not exist. If the <var>dataTypeName</var> starts with '/', it will be
     * considered a data type path instead of a data type name.
     * 
     * @param dataTypeName The name of the compound data type to get the member information for.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     */
    public HDF5CompoundMemberInformation[] getMemberInfo(String dataTypeName,
            DataTypeInfoOptions dataTypeInfoOptions);

    /**
     * Returns the compound member information for the data set <var>dataSetPath</var> in the order
     * that the members appear in the compound type. It is a failure condition if this data set does
     * not exist or is not of compound type.
     * <p>
     * Call <code>Arrays.sort(compoundInformation)</code> to sort the array in alphabetical order of
     * names.
     * 
     * @throws HDF5JavaException If the data set is not of type compound.
     */
    public HDF5CompoundMemberInformation[] getDataSetInfo(String dataSetPath)
            throws HDF5JavaException;

    /**
     * Returns the compound member information for the data set <var>dataSetPath</var> in the order
     * that the members appear in the compound type. It is a failure condition if this data set does
     * not exist or is not of compound type.
     * <p>
     * Call <code>Arrays.sort(compoundInformation)</code> to sort the array in alphabetical order of
     * names.
     * 
     * @param dataSetPath The name of the data set to get the member information for.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     * @throws HDF5JavaException If the data set is not of type compound.
     */
    public HDF5CompoundMemberInformation[] getDataSetInfo(String dataSetPath,
            DataTypeInfoOptions dataTypeInfoOptions) throws HDF5JavaException;

    // /////////////////////
    // Types
    // /////////////////////

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>pojoClass</var>. The mapping is defined by <var>members</var>.
     * 
     * @param name The name of the compound in the HDF5 file.
     * @param pojoClass The plain old Java type that corresponds to this HDF5 type.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @param members The mapping from the Java compound type to the HDF5 type.
     */
    public <T> HDF5CompoundType<T> getType(String name, Class<T> pojoClass,
            boolean requireTypesToBeEqual, HDF5CompoundMemberMapping... members);

    /**
     * Returns the compound type <var>name></var> for this HDF5 file.
     * 
     * @param name The name of the compound in the HDF5 file.
     * @param pojoClass The plain old Java type that corresponds to this HDF5 type.
     * @param members The mapping from the Java compound type to the HDF5 type.
     */
    public <T> HDF5CompoundType<T> getType(String name, Class<T> pojoClass,
            HDF5CompoundMemberMapping... members);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>pojoClass</var>. The name of the compound data type is chosen to be the simple name of
     * <var>pojoClass</var>. The mapping is defined by <var>members</var>.
     * 
     * @param pojoClass The plain old Java type that corresponds to this HDF5 type.
     * @param members The mapping from the Java compound type to the HDF5 type.
     */
    public <T> HDF5CompoundType<T> getType(Class<T> pojoClass, HDF5CompoundMemberMapping... members);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>pojoClass</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>pojoClass</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound in the HDF5 file.
     * @param pojoClass The plain old Java type that corresponds to this HDF5 type.
     * @param hints The hints to provide to the mapping procedure.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(String name, Class<T> pojoClass,
            HDF5CompoundMappingHints hints);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>pojoClass</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>pojoClass</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound in the HDF5 file.
     * @param pojoClass The plain old Java type that corresponds to this HDF5 type.
     * @param hints The hints to provide to the mapping procedure.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(String name, Class<T> pojoClass,
            HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>pojoClass</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>pojoClass</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound in the HDF5 file.
     * @param pojoClass The plain old Java type that corresponds to this HDF5 type.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(String name, Class<T> pojoClass);

    /**
     * Returns a compound type for this HDF5 file, compatible with <var>pojoClass</var>. The mapping
     * of the Java compound type to the HDF5 type is inferred by reflection from
     * <var>pojoClass</var> following basic rules on how Java data types are mapped to HDF5 data
     * types. As name of the HDF5 compound type, the simple name of <var>pojoClass</var> is chosen.
     * 
     * @param pojoClass The plain old Java type that corresponds to this HDF5 type.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(Class<T> pojoClass);

    /**
     * Returns a compound type for this HDF5 file, compatible with <var>pojoClass</var>. The mapping
     * of the Java compound type to the HDF5 type is inferred by reflection from
     * <var>pojoClass</var> following basic rules on how Java data types are mapped to HDF5 data
     * types. As name of the HDF5 compound type, the simple name of <var>pojoClass</var> is chosen.
     * 
     * @param pojoClass The plain old Java type that corresponds to this HDF5 type.
     * @param hints The hints to provide to the mapping procedure.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(Class<T> pojoClass,
            HDF5CompoundMappingHints hints);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>template</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param template The compound to infer the HDF5 compound type from.
     * @param hints The hints to provide to the mapping procedure.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(String name, T template,
            HDF5CompoundMappingHints hints);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>template</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param template The compound to infer the HDF5 compound type from.
     * @param hints The hints to provide to the mapping procedure.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(String name, T template,
            HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>template</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param template The compound to infer the HDF5 compound type from.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(String name, T template);

    /**
     * Returns a compound type for this HDF5 file, compatible with <var>template</var>. The mapping
     * of the Java compound type to the HDF5 type is inferred by reflection from <var>template</var>
     * following basic rules on how Java data types are mapped to HDF5 data types. As name of the
     * HDF5 compound type, the simple name of the class of <var>template</var> is chosen.
     * 
     * @param template The compound to infer the HDF5 compound type from.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(T template);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>template</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param template The compound array to infer the HDF5 compound type from.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(final String name, final T[] template);

    /**
     * Returns a compound type for this HDF5 file, compatible with <var>template</var>. The mapping
     * of the Java compound type to the HDF5 type is inferred by reflection from <var>template</var>
     * following basic rules on how Java data types are mapped to HDF5 data types. As name of the
     * HDF5 compound type, the simple name of the class of <var>template</var> is chosen.
     * 
     * @param template The compound array to infer the HDF5 compound type from.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(final T[] template);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>template</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param template The compound array to infer the HDF5 compound type from.
     * @param hints The hints to provide to the mapping procedure.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(String name, T[] template,
            HDF5CompoundMappingHints hints);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var>. The mapping of the Java compound type to the HDF5 type is inferred by
     * reflection from <var>template</var> following basic rules on how Java data types are mapped
     * to HDF5 data types.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param template The compound array to infer the HDF5 compound type from.
     * @param hints The hints to provide to the mapping procedure.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public <T> HDF5CompoundType<T> getInferredType(String name, T[] template,
            HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length as <var>memberNames</var>.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<List<?>> getInferredType(String name, List<String> memberNames,
            List<?> template);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length as <var>memberNames</var>.
     * @param hints The hints to provide to the mapping procedure.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<List<?>> getInferredType(String name, List<String> memberNames,
            List<?> template, HDF5CompoundMappingHints hints);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length as <var>memberNames</var>.
     * @param hints The hints to provide to the mapping procedure.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<List<?>> getInferredType(String name, List<String> memberNames,
            List<?> template, HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length as <var>memberNames</var>.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<List<?>> getInferredType(List<String> memberNames, List<?> template);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length as <var>memberNames</var>.
     * @param hints The hints to provide to the mapping procedure.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<List<?>> getInferredType(List<String> memberNames, List<?> template,
            HDF5CompoundMappingHints hints);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length than <var>memberNames</var>.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<Object[]> getInferredType(String name, String[] memberNames,
            Object[] template);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length than <var>memberNames</var>.
     * @param hints The hints to provide to the mapping procedure.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<Object[]> getInferredType(String name, String[] memberNames,
            Object[] template, HDF5CompoundMappingHints hints);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param name The name of the compound type in the HDF5 file.
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length than <var>memberNames</var>.
     * @param hints The hints to provide to the mapping procedure.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<Object[]> getInferredType(String name, String[] memberNames,
            Object[] template, HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length than <var>memberNames</var>.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<Object[]> getInferredType(String[] memberNames, Object[] template);

    /**
     * Returns a compound type <var>name></var> for this HDF5 file, compatible with
     * <var>template</var> and <var>memberNames</var>. The mapping of the Java compound type to the
     * HDF5 type is inferred by reflection of the elements of <var>template</var> following basic
     * rules on how Java data types are mapped to HDF5 data types. Each element of
     * <var>template</var> is considered a member of the compound. The names are taken from
     * <var>memberNames</var> in the same order as in <var>template</var>.
     * 
     * @param memberNames The names of the members.
     * @param template The compound to infer the HDF5 compound type from. Needs to have the same
     *            length than <var>memberNames</var>.
     * @param hints The hints to provide to the mapping procedure.
     * @see HDF5CompoundMemberMapping#inferMapping
     */
    public HDF5CompoundType<Object[]> getInferredType(String[] memberNames, Object[] template,
            HDF5CompoundMappingHints hints);

    /**
     * Returns the compound type for the given compound data set in <var>objectPath</var>, mapping
     * it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset to get the type from.
     * @param pojoClass The class to use for the mapping.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @param members The mapping from the Java compound type to the HDF5 type.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass,
            boolean requireTypesToBeEqual, HDF5CompoundMemberMapping... members);

    /**
     * Returns the compound type for the given compound data set in <var>objectPath</var>, mapping
     * it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset to get the type from.
     * @param pojoClass The class to use for the mapping.
     * @param members The mapping from the Java compound type to the HDF5 type.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass,
            HDF5CompoundMemberMapping... members);

    /**
     * Returns the compound type for the given compound data set in <var>objectPath</var>, mapping
     * it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset to get the type from.
     * @param pojoClass The class to use for the mapping.
     * @param hints The hints to provide to the mapping procedure.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass,
            HDF5CompoundMappingHints hints);

    /**
     * Returns the compound type for the given compound data set in <var>objectPath</var>, mapping
     * it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset to get the type from.
     * @param pojoClass The class to use for the mapping.
     * @param hints The hints to provide to the mapping procedure.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass,
            HDF5CompoundMappingHints hints, boolean requireTypesToBeEqual);

    /**
     * Returns the compound type for the given compound data set in <var>objectPath</var>, mapping
     * it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset to get the type from.
     * @param pojoClass The class to use for the mapping.
     */
    public <T> HDF5CompoundType<T> getDataSetType(String objectPath, Class<T> pojoClass);

    /**
     * Returns the compound type for the given compound attribute in <var>attributeName</var> of
     * <var>objectPath</var>, mapping it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset.
     * @param attributeName The name of the attribute to get the type for.
     * @param pojoClass The class to use for the mapping.
     */
    public <T> HDF5CompoundType<T> getAttributeType(String objectPath, String attributeName,
            Class<T> pojoClass);

    /**
     * Returns the compound type for the given compound attribute in <var>attributeName</var> of
     * <var>objectPath</var>, mapping it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset.
     * @param attributeName The name of the attribute to get the type for.
     * @param pojoClass The class to use for the mapping.
     * @param hints The hints to provide to the mapping procedure.
     */
    public <T> HDF5CompoundType<T> getAttributeType(String objectPath, String attributeName,
            Class<T> pojoClass, HDF5CompoundMappingHints hints);

    /**
     * Returns the compound type for the given compound attribute in <var>attributeName</var> of
     * <var>objectPath</var>, mapping it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset.
     * @param attributeName The name of the attribute to get the type for.
     * @param pojoClass The class to use for the mapping.
     * @param hints The hints to provide to the mapping procedure.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     */
    public <T> HDF5CompoundType<T> getAttributeType(String objectPath, String attributeName,
            Class<T> pojoClass, HDF5CompoundMappingHints hints,
            DataTypeInfoOptions dataTypeInfoOptions);

    /**
     * Returns the compound type for the given compound attribute in <var>attributeName</var> of
     * <var>objectPath</var>, mapping it to <var>pojoClass</var>.
     * 
     * @param objectPath The path of the compound dataset.
     * @param attributeName The name of the attribute to get the type for.
     * @param pojoClass The class to use for the mapping.
     * @param hints The hints to provide to the mapping procedure.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     */
    public <T> HDF5CompoundType<T> getAttributeType(String objectPath, String attributeName,
            Class<T> pojoClass, HDF5CompoundMappingHints hints,
            DataTypeInfoOptions dataTypeInfoOptions, boolean requireTypesToBeEqual);

    /**
     * Returns the named compound type with name <var>dataTypeName</var> from file, mapping it to
     * <var>pojoClass</var>. If the <var>dataTypeName</var> starts with '/', it will be considered a
     * data type path instead of a data type name.
     * <p>
     * <em>Note:</em> This method only works for compound data types 'committed' to the HDF5 file.
     * For files written with JHDF5 this will always be true, however, files created with other
     * libraries may not choose to commit compound data types.
     * 
     * @param dataTypeName The path to a committed data type, if starting with '/', or a name of a
     *            committed data type otherwise.
     * @param pojoClass The class to use for the mapping.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass);

    /**
     * Returns the named compound type with name <var>dataTypeName</var> from file, mapping it to
     * <var>pojoClass</var>. This method will use the default name for the compound data type as
     * chosen by JHDF5 and thus will likely only work on files written with JHDF5. The default name
     * is based on the simple name of <var>compoundType</var>.
     * 
     * @param pojoClass The class to use for the mapping and to get the name of named data type
     *            from.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getNamedType(Class<T> pojoClass);

    /**
     * Returns the named compound type with name <var>dataTypeName</var> from file, mapping it to
     * <var>pojoClass</var>. If the <var>dataTypeName</var> starts with '/', it will be considered a
     * data type path instead of a data type name.
     * <p>
     * <em>Note:</em> This method only works for compound data types 'committed' to the HDF5 file.
     * For files written with JHDF5 this will always be true, however, files created with other
     * libraries may not choose to commit compound data types.
     * 
     * @param dataTypeName The path to a committed data type, if starting with '/', or a name of a
     *            committed data type otherwise.
     * @param pojoClass The class to use for the mapping.
     * @param hints The hints to provide to the mapping procedure.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass,
            HDF5CompoundMappingHints hints);

    /**
     * Returns the named compound type with name <var>dataTypeName</var> from file, mapping it to
     * <var>pojoClass</var>. If the <var>dataTypeName</var> starts with '/', it will be considered a
     * data type path instead of a data type name.
     * <p>
     * <em>Note:</em> This method only works for compound data types 'committed' to the HDF5 file.
     * For files written with JHDF5 this will always be true, however, files created with other
     * libraries may not choose to commit compound data types.
     * 
     * @param dataTypeName The path to a committed data type, if starting with '/', or a name of a
     *            committed data type otherwise.
     * @param pojoClass The class to use for the mapping.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass,
            DataTypeInfoOptions dataTypeInfoOptions);

    /**
     * Returns the named compound type with name <var>dataTypeName</var> from file, mapping it to
     * <var>pojoClass</var>. If the <var>dataTypeName</var> starts with '/', it will be considered a
     * data type path instead of a data type name.
     * <p>
     * <em>Note:</em> This method only works for compound data types 'committed' to the HDF5 file.
     * For files written with JHDF5 this will always be true, however, files created with other
     * libraries may not choose to commit compound data types.
     * 
     * @param dataTypeName The path to a committed data type, if starting with '/', or a name of a
     *            committed data type otherwise.
     * @param pojoClass The class to use for the mapping.
     * @param hints The hints to provide to the mapping procedure.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass,
            HDF5CompoundMappingHints hints, DataTypeInfoOptions dataTypeInfoOptions);

    /**
     * Returns the named compound type with name <var>dataTypeName</var> from file, mapping it to
     * <var>pojoClass</var>. If the <var>dataTypeName</var> starts with '/', it will be considered a
     * data type path instead of a data type name.
     * <p>
     * <em>Note:</em> This method only works for compound data types 'committed' to the HDF5 file.
     * For files written with JHDF5 this will always be true, however, files created with other
     * libraries may not choose to commit compound data types.
     * 
     * @param dataTypeName The path to a committed data type, if starting with '/', or a name of a
     *            committed data type otherwise.
     * @param pojoClass The class to use for the mapping.
     * @param hints The hints to provide to the mapping procedure.
     * @param dataTypeInfoOptions The options on which information to get about the member data
     *            types.
     * @param requireTypesToBeEqual If <code>true</code>, this type is required to be equal to the
     *            type it tries to read, or else an {@link HDF5JavaException} will be thrown.
     * @return The compound data type.
     */
    public <T> HDF5CompoundType<T> getNamedType(String dataTypeName, Class<T> pojoClass,
            HDF5CompoundMappingHints hints, DataTypeInfoOptions dataTypeInfoOptions,
            boolean requireTypesToBeEqual);
}
