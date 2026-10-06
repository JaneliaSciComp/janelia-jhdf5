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

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.base.unix.FileLinkType;

/**
 * Test cases for {@link DirectoryIndexUpdater}.
 * 
 * @author Bernd Rinn
 */
public class DirectoryIndexUpdaterTest
{
    private static final File rootDirectory = new File("target", "unit-test-wd");

    private static final File workingDirectory = new File(rootDirectory,
            "hdf5-directory-index-updater-wd");

    private Mockery context;

    IDirectoryIndexProvider provider;

    private DirectoryIndexUpdater updater;

    @BeforeSuite
    public void init()
    {
        workingDirectory.mkdirs();
        assertTrue(workingDirectory.isDirectory());
        workingDirectory.deleteOnExit();
        rootDirectory.deleteOnExit();
    }
    
    @BeforeMethod
    public void initTest() throws IOException
    {
        FileUtils.cleanDirectory(workingDirectory);
        this.context = new Mockery();
        this.provider = context.mock(IDirectoryIndexProvider.class);
        context.checking(new Expectations()
            {
                {
                    one(provider).getErrorStrategy();
                    will(returnValue(IErrorStrategy.DEFAULT_ERROR_STRATEGY));
                }
            });

        this.updater = new DirectoryIndexUpdater(provider);
    }

    @AfterSuite
    protected void finalizeTest() throws Throwable
    {
        // Delete the working directory
        if (workingDirectory.exists() && workingDirectory.canWrite())
        {
            FileUtils.deleteDirectory(workingDirectory);
        }
        // Delete root directory
        if (rootDirectory.exists() && rootDirectory.canWrite())
        {
            rootDirectory.delete();
        }

        super.finalize();
    }

    static class LinkEntryEntryMatcher extends TypeSafeMatcher<LinkRecord>
    {
        private final FileLinkType type;
        
        private final String name;

        private final int crc32;

        private final long lastModified;

        LinkEntryEntryMatcher(FileLinkType type, String name, int crc32, long lastModified)
        {
            this.type = type;
            this.name = name;
            this.crc32 = crc32;
            this.lastModified = lastModified;
        }
        
        static LinkEntryEntryMatcher file(String name, int crc32, long lastModified)
        {
            return new LinkEntryEntryMatcher(FileLinkType.REGULAR_FILE, name, crc32, lastModified);
        }

        static LinkEntryEntryMatcher dir(String name, long lastModified)
        {
            return new LinkEntryEntryMatcher(FileLinkType.DIRECTORY, name, 0, lastModified);
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(toString());
        }

        @Override
        public boolean matchesSafely(LinkRecord item)
        {
            if (name.equals(item.getLinkName()) == false)
            {
                System.err.printf("linkName=%s (expected: %s)\n", item.getLinkName(), name);
                return false;
            }
            if (type != item.getLinkType())
            {
                System.err.printf("linkType=%s (expected: %s) [linkName=%s]\n", item.getLinkType(), type, name);
                return false;
            }
            if (crc32 != item.getCrc32())
            {
                System.err.printf("crc32=%s (expected: %s) [linkName=%s]\n", item.getCrc32(), crc32, name);
                return false;
            }
            if (Math.abs(lastModified - item.getLastModified()) > 1)
            {
                System.err.printf("lastModified=%s (expected: %s) [linkName=%s]\n", item.getLastModified(),
                        lastModified, name);
                return false;
            }
            return true;
        }

        @Override
        public String toString()
        {
            return "LinkEntryEntryMatcher [type=" + type + ", name=" + name + ", crc32=" + crc32
                    + ", lastModified=" + lastModified + "]";
        }

    }

    @Test
    public void testFileExistsOnFS() throws IOException
    {
        final String name = "abc";
        final int crc32 = 123;
        final long lastModified = 543L; 
        final File f = new File(workingDirectory, name);
        FileUtils.touch(f);
        f.setLastModified(lastModified * 1000L);
        context.checking(new Expectations()
            {
                {
                    final IDirectoryIndex indexBlaBlub  = context.mock(IDirectoryIndex.class, "indexBlaBlub");
                    one(provider).get("/bla/blub", false);
                    will(returnValue(indexBlaBlub));
                    one(indexBlaBlub).updateIndex(with(LinkEntryEntryMatcher.file(name, crc32, lastModified)));

                    final IDirectoryIndex indexBla  = context.mock(IDirectoryIndex.class, "indexBla");
                    one(provider).get("/bla", false);
                    will(returnValue(indexBla));
                    one(indexBla).updateIndex(with(LinkEntryEntryMatcher.dir("blub", System.currentTimeMillis()/1000)));

                    final IDirectoryIndex indexRoot  = context.mock(IDirectoryIndex.class, "indexRoot");
                    one(provider).get("/", false);
                    will(returnValue(indexRoot));
                    one(indexRoot).updateIndex(with(LinkEntryEntryMatcher.dir("bla", System.currentTimeMillis()/1000)));
                }
            });
        updater.updateIndicesOnThePath("/bla/blub", f, 123, false);
    }

    @Test
    public void testOnlyFileAndDirExistsOnFS() throws IOException
    {
        final String name = "abc";
        final int crc32 = 123;
        final long lastModified = 543L; 
        final File f = new File(new File(workingDirectory, "ttt"), name);
        f.getParentFile().mkdirs();
        FileUtils.touch(f);
        f.setLastModified(lastModified * 1000L);
        final long lastModifiedDir = 2222L; 
        f.getParentFile().setLastModified(lastModifiedDir * 1000L);
        context.checking(new Expectations()
            {
                {
                    final IDirectoryIndex indexTtt  = context.mock(IDirectoryIndex.class, "indexTtt");
                    one(provider).get("/ttt", false);
                    will(returnValue(indexTtt));
                    one(indexTtt).updateIndex(with(LinkEntryEntryMatcher.file(name, crc32, lastModified)));

                    final IDirectoryIndex indexRoot  = context.mock(IDirectoryIndex.class, "indexRoot");
                    one(provider).get("/", false);
                    will(returnValue(indexRoot));
                    one(indexRoot).updateIndex(with(LinkEntryEntryMatcher.dir("ttt", lastModifiedDir)));
                }
            });
        updater.updateIndicesOnThePath("/ttt", f, 123, false);
    }
}
