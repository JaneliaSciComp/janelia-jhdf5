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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A marker annotation for classes intended to be mapped to an HDF5 compound data type. This marker
 * interface is optional as inferring the field to member mapping works also when this annotation is
 * not present. However, this annotation is the only way to specify that not all fields should be
 * mapped to members but only those annotated with {@link CompoundElement}.
 * 
 * @author Bernd Rinn
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface CompoundType
{
    /**
     * The name this compound type should have in the HDF5 file. If left blank, the simple class
     * name will be used.
     */
    String name() default "";

    /**
     * Whether all fields should be mapped to members of the compound type or only the fields
     * annotated with {@link CompoundElement} (default: <code>true</code>).
     */
    boolean mapAllFields() default true;
}
