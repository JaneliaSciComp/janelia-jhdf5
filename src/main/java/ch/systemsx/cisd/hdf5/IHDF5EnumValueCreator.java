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

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;

/**
 * Interface for creation of enumeration values.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Writer#enumeration()}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5EnumValueCreator
{

    /**
     * Creates a new enumeration value with enumeration type name <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param value The string representation of the created enumeration value.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValue newVal(String typeName, String[] options, String value);

    /**
     * Creates a new enumeration value with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param value The string representation of the created enumeration value.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValue newAnonVal(String[] options, String value);

    /**
     * Creates a new enumeration value with enumeration type name <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param value The ordinal value of the created enumeration value.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValue newVal(String typeName, String[] options, final int value);

    /**
     * Creates a new enumeration value with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param value The ordinal of the created enumeration value.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValue newAnonVal(String[] options, int value);

    /**
     * Creates a new enumeration value with enumeration type name <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param value The ordinal value of the created enumeration value.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValue newVal(String typeName, String[] options, final short value);

    /**
     * Creates a new enumeration value with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param value The ordinal value of the created enumeration value.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValue newAnonVal(String[] options, short value);

    /**
     * Creates a new enumeration value with enumeration type name <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param value The ordinal value of the created enumeration value.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValue newVal(String typeName, String[] options, final byte value);

    /**
     * Creates a new enumeration value with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param value The ordinal of the created enumeration value.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValue newAnonVal(String[] options, byte value);

    /**
     * Creates a new enumeration value with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getAnonymousEnumType(value.class), getClass())</code>.
     * 
     * @param value The value (including the type) of the created enumeration value.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValue newAnonVal(Enum<T> value);

    /**
     * Creates a new enumeration value with an enumeration type of name
     * <code>value.getClass().getSimpleName()</code>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getEnumType(value.getClass()), value)</code>.
     * 
     * @param value The value (including the type) of the created enumeration value.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValue newVal(Enum<T> value);

    /**
     * Creates a new enumeration value with an enumeration type of name <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValue(writer.getEnumType(typeName, value.getClass()), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param value The value (including the type) of the created enumeration value.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValue newVal(String typeName, Enum<T> value);

    /**
     * Creates a new enumeration value array (of rank 1) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param values The string representations of the elements of the created enumeration value
     *            array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueArray newArray(String typeName, String[] options, String[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param values The string representations of the elements of the created enumeration value
     *            array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueArray newAnonArray(String[] options, String[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueArray newArray(String typeName, String[] options, int[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueArray newAnonArray(String[] options, int[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueArray newArray(String typeName, String[] options, short[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueArray newAnonArray(String[] options, short[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueArray newArray(String typeName, String[] options, byte[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueArray newAnonArray(String[] options, byte[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(value.getClass().getComponentType()), value)</code>.
     * 
     * @param values The value array (which has the type) of the created enumeration value array.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValueArray newAnonArray(Enum<T>[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, value.getClass().getComponentType()), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param values The value array (which has the type) of the created enumeration value array.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValueArray newArray(String typeName, Enum<T>[] values);

    /**
     * Creates a new enumeration value array (of rank 1) with an enumeration type of name
     * <code>value.class.getSimpleName()</code>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(value.getClass().getComponentType()), value)</code>.
     * 
     * @param values The value array (which has the type) of the created enumeration value array.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValueArray newArray(Enum<T>[] values);

    /**
     * Creates a new enumeration value array (of rank N) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param values The string representations of the elements of the created enumeration value
     *            array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueMDArray newMDArray(String typeName, String[] options,
            MDArray<String> values);

    /**
     * Creates a new enumeration value array (of rank N) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param values The string representations of the elements of the created enumeration value
     *            array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueMDArray newAnonMDArray(String[] options, MDArray<String> values);

    /**
     * Creates a new enumeration value array (of rank N) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueMDArray newMDArray(String typeName, String[] options,
            MDIntArray values);

    /**
     * Creates a new enumeration value array (of rank N) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueMDArray newAnonMDArray(String[] options, MDIntArray values);

    /**
     * Creates a new enumeration value array (of rank N) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueMDArray newMDArray(String typeName, String[] options,
            MDShortArray values);

    /**
     * Creates a new enumeration value array (of rank N) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueMDArray newAnonMDArray(String[] options, MDShortArray values);

    /**
     * Creates a new enumeration value array (of rank N) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueMDArray newMDArray(String typeName, String[] options,
            MDByteArray values);

    /**
     * Creates a new enumeration value array (of rank N) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, options), value)</code>.
     * 
     * @param options The values of the enumeration type.
     * @param values The ordinal values of the elements of the created enumeration value array.
     * @return The created enumeration value.
     */
    public HDF5EnumerationValueMDArray newAnonMDArray(String[] options, MDByteArray values);

    /**
     * Creates a new enumeration value array (of rank N) with an anonymous enumeration type.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getAnonymousEnumType(value.getAsFlatArray().getClass().getComponentType()), value)</code>.
     * 
     * @param values The value array (which has the type) of the created enumeration value array.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValueMDArray newAnonMDArray(MDArray<Enum<T>> values);

    /**
     * Creates a new enumeration value array (of rank N) with an enumeration type of name
     * <var>typeName</var>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(typeName, value.getAsFlatArray().getClass().getComponentType()), value)</code>.
     * 
     * @param typeName The name of the enumeration type.
     * @param values The value array (which has the type) of the created enumeration value array.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValueMDArray newMDArray(String typeName,
            MDArray<Enum<T>> values);

    /**
     * Creates a new enumeration value array (of rank N) with an enumeration type of name
     * <code>value.class.getSimpleName()</code>.
     * <p>
     * Shortcut for
     * <code>new HDF5EnumerationValueArray(writer.getEnumType(value.getAsFlatArray().getClass().getComponentType()), value)</code>.
     * 
     * @param values The value array (which has the type) of the created enumeration value array.
     * @return The created enumeration value.
     */
    public <T extends Enum<T>> HDF5EnumerationValueMDArray newMDArray(MDArray<Enum<T>> values);

}
