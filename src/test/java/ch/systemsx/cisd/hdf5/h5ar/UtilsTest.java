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

package ch.systemsx.cisd.hdf5.h5ar;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test cases for {@link Utils}.
 *
 * @author Bernd Rinn
 */
public class UtilsTest
{
    @Test
    public void testNormalizePath()
    {
        assertEquals("/", Utils.normalizePath("/"));
        assertEquals("/a", Utils.normalizePath("a"));
        assertEquals("/a", Utils.normalizePath("a/"));
        assertEquals("/a", Utils.normalizePath("/a/"));
        assertEquals("/a/b/c", Utils.normalizePath("a/b/c"));
        assertEquals("/a/c", Utils.normalizePath("a/b/../c/./"));
    }
    
    @Test
    public void testGetParentPath()
    {
        assertEquals("", Utils.getParentPath("/"));
        assertEquals("/", Utils.getParentPath("/dir"));
        assertEquals("/some", Utils.getParentPath("/some/dir"));
    }
    
    @Test
    public void testConcatLink()
    {
        assertEquals("/", Utils.concatLink(Utils.getParentPath("/"), Utils.getName("/")));
        assertEquals("/a", Utils.concatLink(Utils.getParentPath("/a"), Utils.getName("/a")));
        assertEquals("/a/b", Utils.concatLink(Utils.getParentPath("/a/b"), Utils.getName("/a/b")));
    }
    
}
