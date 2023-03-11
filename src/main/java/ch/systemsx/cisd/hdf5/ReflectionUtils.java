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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for reflection, used for inferring the mapping between a compound data type and
 * the fields of a Java class.
 * <p>
 * <i>This is an internal API that should not be expected to be stable between releases!</i>
 * 
 * @author Bernd Rinn
 */
public final class ReflectionUtils
{

    private ReflectionUtils()
    {
        // Cannot be instantiated
    }

    /**
     * Returns a map from field names to fields for all fields in the given <var>clazz</var>.
     */
    public static Map<String, Field> getFieldMap(final Class<?> clazz)
    {
        return getFieldMap(clazz, true);
    }

    /**
     * Returns a map from field names to fields for all fields in the given <var>clazz</var>.
     * 
     * @param clazz The clazz to get the fields from.
     * @param excludeNonMappedFields If <code>true</code>, do not include fields to the map which
     *            are not supposed to be mapped to HDF5 members.
     */
    public static Map<String, Field> getFieldMap(final Class<?> clazz,
            boolean excludeNonMappedFields)
    {
        final Map<String, Field> map = new HashMap<String, Field>();
        final CompoundType ct = clazz.getAnnotation(CompoundType.class);
        final boolean includeAllFields =
                excludeNonMappedFields ? ((ct != null) ? ct.mapAllFields() : true) : true;
        for (Class<?> c = clazz; c != null; c = c.getSuperclass())
        {
            for (Field f : c.getDeclaredFields())
            {
                final CompoundElement e = f.getAnnotation(CompoundElement.class);
                if (e != null && org.apache.commons.lang3.StringUtils.isNotEmpty(e.memberName()))
                {
                    map.put(e.memberName(), f);
                } else if (e != null || includeAllFields)
                {
                    map.put(f.getName(), f);
                }
            }
        }
        return map;
    }

    /**
     * Ensures that the given <var>member</var> is accessible even if by definition it is not.
     */
    public static void ensureAccessible(final AccessibleObject memberOrNull)
    {
        if (memberOrNull != null && memberOrNull.isAccessible() == false)
        {
            memberOrNull.setAccessible(true);
        }
    }

    /**
     * Creates an object of <var>clazz</var> using the default constructor, making the default
     * constructor accessible if necessary.
     */
    public static <T> Constructor<T> getDefaultConstructor(final Class<T> clazz)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException
    {
        final Constructor<T> defaultConstructor = clazz.getDeclaredConstructor();
        ensureAccessible(defaultConstructor);
        return defaultConstructor;

    }

    /**
     * Creates an object of <var>clazz</var> using the default constructor, making the default
     * constructor accessible if necessary.
     */
    public static <T> T newInstance(final Class<T> clazz) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException
    {
        final Constructor<T> defaultConstructor = clazz.getDeclaredConstructor();
        ensureAccessible(defaultConstructor);
        return defaultConstructor.newInstance();

    }

    /**
     * Returns the enum options of the given <var>enumClass</var>. If <var>enumClass</var> is not an
     * enum class, return an empty array.
     */
    public static String[] getEnumOptions(Class<? extends Enum<?>> enumClass)
    {
        final Enum<?>[] constants = enumClass.getEnumConstants();
        if (constants == null)
        {
            return new String[0];
        }
        final String[] options = new String[constants.length];
        for (int i = 0; i < options.length; ++i)
        {
            options[i] = constants[i].name();
        }
        return options;
    }

}
