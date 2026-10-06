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

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * @author Bernd Rinn
 */
public class HDF5UtilsTest
{

    /** The attribute to signal that this is a variant of the data type. */
    static final String TYPE_VARIANT_ATTRIBUTE_OLD = "__TYPE_VARIANT__";

    /**
     * Returns the type variant attribute for the given <var>attributeName</var>.
     */
    static String createTypeVariantAttributeNameOld(String attributeName)
    {
        return TYPE_VARIANT_ATTRIBUTE_OLD + attributeName + "__";
    }

    @Test
    public void testAttributeTypeVariantAttributeName()
    {
        assertEquals("__TYPE_VARIANT__abc__",
                HDF5Utils.createAttributeTypeVariantAttributeName("abc", ""));
        assertEquals(
                "__TYPE_VARIANT____abc____",
                HDF5Utils.createAttributeTypeVariantAttributeName(
                        HDF5Utils.toHouseKeepingName("abc", ""), ""));
        assertEquals("TYPE_VARIANT__abcXX",
                HDF5Utils.createAttributeTypeVariantAttributeName("abc", "XX"));
        assertEquals(
                "TYPE_VARIANT__abcXXXX",
                HDF5Utils.createAttributeTypeVariantAttributeName(
                        HDF5Utils.toHouseKeepingName("abc", "XX"), "XX"));
    }

}
