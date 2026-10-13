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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import ch.systemsx.cisd.hdf5.h5ar.ArchivingStrategy.CompressionStrategy;

/**
 * Test cases for {@link ArchivingStrategy}.
 * 
 * @author Bernd Rinn
 */
public class ArchivingStrategyTest
{
    @Test
    public void testCompressDefault()
    {
        assertEquals(CompressionStrategy.COMPRESS_NOTHING,
                ArchivingStrategy.DEFAULT_NO_COMPRESSION.getCompressionStrategy());
        assertFalse(ArchivingStrategy.DEFAULT_NO_COMPRESSION.doCompress("/test.txt"));
    }

    @Test
    public void testCompressDefaultWithCompression()
    {
        assertEquals(CompressionStrategy.USE_BLACK_WHITE_LISTS,
                ArchivingStrategy.DEFAULT.getCompressionStrategy());
        assertTrue(ArchivingStrategy.DEFAULT.doCompress("/test.txt"));
        assertFalse(ArchivingStrategy.DEFAULT.doCompress("/test.txt.gz"));
        assertFalse(ArchivingStrategy.DEFAULT.doCompress("/test.txt.bz2"));
        assertFalse(ArchivingStrategy.DEFAULT.doCompress("/test.txt.zip"));
    }

    @Test
    public void testCompressAll()
    {
        final ArchivingStrategy strategy = new ArchivingStrategy().compressAll();
        assertEquals(CompressionStrategy.COMPRESS_ALL, strategy.getCompressionStrategy());
        assertTrue(strategy.doCompress("/test.txt"));
    }

    @Test
    public void testCompressBlackList()
    {
        final ArchivingStrategy strategyCompressAll =
                new ArchivingStrategy().compressAll().addToCompressionBlackList(".*\\.txt");
        assertEquals(CompressionStrategy.USE_BLACK_WHITE_LISTS,
                strategyCompressAll.getCompressionStrategy());
        final ArchivingStrategy strategy =
                new ArchivingStrategy().compressAll().addToCompressionBlackList(".*\\.txt");
        assertEquals(CompressionStrategy.USE_BLACK_WHITE_LISTS, strategy.getCompressionStrategy());
        assertTrue(strategyCompressAll.doCompress("/test.dat"));
        assertFalse(strategyCompressAll.doCompress("/test.txt"));
        assertTrue(strategy.doCompress("/test.dat"));
        assertFalse(strategy.doCompress("/test.txt"));
    }

    @Test
    public void testCompressWhiteList()
    {
        final ArchivingStrategy strategyCompressAll =
                new ArchivingStrategy().compressAll().addToCompressionWhiteList(".*\\.txt");
        assertEquals(CompressionStrategy.USE_BLACK_WHITE_LISTS,
                strategyCompressAll.getCompressionStrategy());
        final ArchivingStrategy strategy =
                new ArchivingStrategy().addToCompressionWhiteList(".*\\.txt");
        assertEquals(CompressionStrategy.USE_BLACK_WHITE_LISTS, strategy.getCompressionStrategy());
        assertFalse(strategyCompressAll.doCompress("/test.dat"));
        assertTrue(strategyCompressAll.doCompress("/test.txt"));
        assertFalse(strategy.doCompress("/test.dat"));
        assertTrue(strategy.doCompress("/test.txt"));
    }

    @Test
    public void testCompressBlackWhiteList()
    {
        final ArchivingStrategy strategyCompressAll =
                new ArchivingStrategy().compressAll().addToCompressionBlackList(".*a[^/]*\\.txt")
                        .addToCompressionWhiteList(".*\\.txt");
        assertEquals(CompressionStrategy.USE_BLACK_WHITE_LISTS,
                strategyCompressAll.getCompressionStrategy());
        final ArchivingStrategy strategy =
                new ArchivingStrategy().addToCompressionBlackList(".*a[^/]*\\.txt")
                        .addToCompressionWhiteList(".*\\.txt");
        assertEquals(CompressionStrategy.USE_BLACK_WHITE_LISTS, strategy.getCompressionStrategy());
        assertFalse(strategyCompressAll.doCompress("/test.dat"));
        assertTrue(strategyCompressAll.doCompress("/test.txt"));
        assertFalse(strategyCompressAll.doCompress("/atest.txt"));
        assertFalse(strategy.doCompress("/test.dat"));
        assertTrue(strategy.doCompress("/test.txt"));
        assertFalse(strategy.doCompress("/atest.txt"));
    }

}
