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
 *
 * Modified in 2026 by the Howard Hughes Medical Institute for janelia-jhdf5.
 */

package ch.systemsx.cisd.base.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;

/**
 * An <code>abstract</code> test case which accesses the file system.
 * <p>
 * It constructs an appropriate working directory which is test class specific.
 * </p>
 * 
 * @author Christian Ribeaud
 */
public abstract class AbstractFileSystemTestCase extends AssertJUnit
{
    protected static final String UNIT_TEST_WORKING_DIRECTORY = "unit-test-wd";

    protected static final String TARGETS_DIRECTORY = "target";

    protected static final File UNIT_TEST_ROOT_DIRECTORY = new File(TARGETS_DIRECTORY
            + File.separator + UNIT_TEST_WORKING_DIRECTORY);

    protected final File workingDirectory;

    private final boolean cleanAfterMethod;

    protected AbstractFileSystemTestCase()
    {
        this(true);
    }

    protected AbstractFileSystemTestCase(final boolean cleanAfterMethod)
    {
        workingDirectory = createWorkingDirectory();
        this.cleanAfterMethod = cleanAfterMethod;
    }

    /**
     * Creates a {@link File} with <var>name</var> in the working directory. Ensure it doesn't
     * exist and is deleted on exit.
     */
    protected File create(String name)
    {
        final File file = new File(workingDirectory, name);
        file.delete();
        file.deleteOnExit();
        return file;
    }

    /**
     * Creates a directory in the unit test root directory "target/unit-test-wd"
     */
    protected final File createDirectoryInUnitTestRoot(String dirName)
    {
        final File directory = new File(UNIT_TEST_ROOT_DIRECTORY, dirName);
        directory.mkdirs();
        directory.deleteOnExit();
        return directory;
    }

    private final File createWorkingDirectory()
    {
        return createDirectoryInUnitTestRoot(getClass().getName());
    }

    @BeforeMethod
    public void setUp() throws IOException
    {
        cleanUpDirectoryBeforeTheTest(workingDirectory);
    }

    /**
     * Deletes, recreates and verifies that this is the empty directory
     */
    protected void cleanUpDirectoryBeforeTheTest(File directory)
    {
        deleteDirectory(directory);
        directory.mkdirs();
        assertEquals(true, directory.isDirectory());
        File[] files = directory.listFiles();
        if (files != null)
        {
            assertEquals("Unexpected files " + Arrays.asList(files), 0, files.length);
        }
    }

    private void deleteDirectory(File dir)
    {
        try
        {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e)
        {
            System.err.println("Could not delete the directory " + dir.getPath() + " because: "
                    + e.getMessage());
            try
            {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e2)
            {
                System.err.println("Could not delete the directory " + dir.getPath()
                        + " in second try because: " + e2.getMessage());
            }
        }
    }

    @AfterClass
    public void afterClass() throws IOException
    {
        if (cleanAfterMethod == false)
        {
            return;
        }
        try
        {
            FileUtils.deleteDirectory(workingDirectory);
        } catch (FileNotFoundException ex)
        {
            // Ignore
        }
    }
}
