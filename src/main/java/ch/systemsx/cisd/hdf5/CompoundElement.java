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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A marker annotation for fields intended to be mapped to an HDF5 compound data type member. This
 * marker interface is optional for many fields as otherwise the fields properties are inferred.
 * However, for arrays, <code>String</code>s and <code>BitSet</code>s the maximum length needs to be
 * given in {@link #dimensions()}.
 * 
 * @author Bernd Rinn
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface CompoundElement
{

    /**
     * The name of the member in the compound type. Leave empty to use the field name as member
     * name.
     */
    String memberName() default "";

    /**
     * The name of the type (for Java enumeration types only). Leave empty to use the simple class
     * name as the type name.
     */
    String typeName() default "";

    /**
     * The length / dimensions of the compound member. Is required for compound members that have a
     * variable length, e.g. strings or primitive arrays. Ignored for compound members that have a
     * fixed length, e.g. a float field.
     */
    int[] dimensions() default 0;
    
    /**
     * If <code>true</code>, map this integer field to an unsigned integer type. 
     */
    boolean unsigned() default false;
    
    /**
     * If <code>true</code>, map this string field to a variable-length string type.
     */
    boolean variableLength() default false;
    
    /**
     * If <code>true</code>, map this string field to an HDF5 reference type.
     */
    boolean reference() default false;

    /**
     * The {@link HDF5DataTypeVariant} of this compound element, if any.
     */
    HDF5DataTypeVariant typeVariant() default HDF5DataTypeVariant.NONE;
}
