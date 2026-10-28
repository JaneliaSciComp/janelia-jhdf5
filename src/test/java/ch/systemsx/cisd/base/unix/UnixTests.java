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

package ch.systemsx.cisd.base.unix;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.base.unix.Unix.Group;
import ch.systemsx.cisd.base.unix.Unix.Password;
import ch.systemsx.cisd.base.unix.Unix.Stat;

/**
 * Test cases for the {@link Unix} class.
 * <p>
 * Adapted from cisd:base's UnixTests to the subset of its API that this project's jnr-posix
 * backed {@link Unix} provides (the methods h5ar needs). Left out, as the methods don't exist
 * here: hard links, process detection, effective uid/gid, lookups by user or group name, file
 * (as opposed to link) timestamps and owner, hard-link counts, sub-second timestamps and
 * {@code getLastError()}. Everything is skipped where {@link Unix#isOperational()} is false.
 *
 * @author Bernd Rinn
 */
public class UnixTests extends AbstractFileSystemTestCase
{
    public UnixTests()
    {
        super();
    }

    @BeforeClass
    public void requireUnix()
    {
        if (Unix.isOperational() == false)
        {
            throw new SkipException("Unix is not operational on this platform.");
        }
    }

    @Test(groups =
        { "requires_unix" })
    public void testGetLinkInfoRegularFile() throws IOException
    {
        final short accessMode = (short) 0777;
        final String content = "someText\n";
        final File f = new File(workingDirectory, "someFile");
        FileUtils.writeStringToFile(f, content, Charset.defaultCharset());
        Unix.setAccessMode(f.getAbsolutePath(), accessMode);
        final Stat info = Unix.getLinkInfo(f.getAbsolutePath());
        // Setting a file's owner to what it already is works without privileges.
        Unix.setLinkOwner(f.getAbsolutePath(), info.getUid(), info.getGid());
        assertEquals(Unix.getUid(), info.getUid());
        assertEquals(content.length(), info.getSize());
        assertEquals(accessMode, info.getPermissions());
        assertEquals("root", Unix.tryGetUserNameForUid(0));
        assertEquals(FileLinkType.REGULAR_FILE, info.getLinkType());
        assertFalse(info.isSymbolicLink());
        assertNull(info.tryGetSymbolicLink());
        assertEquals(f.lastModified() / 1000, info.getLastModified());
    }

    @Test(groups =
        { "requires_unix" }, expectedExceptions = NullPointerException.class)
    public void testGetLinkNull() throws IOException
    {
        Unix.getLinkInfo(null);
    }

    @Test(groups =
        { "requires_unix" })
    public void testGetLinkInfoDirectory() throws IOException
    {
        final File d = new File(workingDirectory, "someDir");
        d.mkdir();
        final Stat info = Unix.getLinkInfo(d.getAbsolutePath());
        assertEquals(FileLinkType.DIRECTORY, info.getLinkType());
        assertFalse(info.isSymbolicLink());
    }

    @Test(groups =
        { "requires_unix" })
    public void testGetLinkInfoSymLink() throws IOException
    {
        final File f = new File(workingDirectory, "someOtherFile");
        final String content = "someMoreText\n";
        FileUtils.writeStringToFile(f, content, Charset.defaultCharset());
        final File s = new File(workingDirectory, "someLink");
        Unix.createSymbolicLink(f.getAbsolutePath(), s.getAbsolutePath());
        final Stat info = Unix.getLinkInfo(s.getAbsolutePath());
        assertEquals(FileLinkType.SYMLINK, info.getLinkType());
        assertTrue(info.isSymbolicLink());
        assertEquals(f.getAbsolutePath(), info.tryGetSymbolicLink());
        assertEquals(f.getAbsolutePath(), Unix.tryReadSymbolicLink(s.getAbsolutePath()));
        assertNull(Unix.getLinkInfo(s.getAbsolutePath(), false).tryGetSymbolicLink());
        final Stat info2 = Unix.getFileInfo(s.getAbsolutePath());
        assertEquals(content.length(), info2.getSize());
        assertEquals(FileLinkType.REGULAR_FILE, info2.getLinkType());
        assertFalse(info2.isSymbolicLink());
        assertNull(info2.tryGetSymbolicLink());
    }

    @Test(groups =
        { "requires_unix" })
    public void testSetLinkTimestamps() throws IOException
    {
        final File f = new File(workingDirectory, "someOtherFile");
        FileUtils.writeStringToFile(f, "someMoreText\n", Charset.defaultCharset());
        final File s = new File(workingDirectory, "someLink");
        Unix.createSymbolicLink(f.getAbsolutePath(), s.getAbsolutePath());
        final long fileModified = Unix.getLinkInfo(f.getAbsolutePath()).getLastModified();
        final long newLinkModified = fileModified - 24 * 3600;
        Unix.setLinkTimestamps(s.getAbsolutePath(), newLinkModified, newLinkModified);
        // The link's own timestamp changed...
        assertEquals(newLinkModified, Unix.getLinkInfo(s.getAbsolutePath(), false).getLastModified());
        // ...but the file it points to is untouched.
        assertEquals(fileModified, Unix.getFileInfo(s.getAbsolutePath()).getLastModified());
        assertEquals(fileModified, Unix.getLinkInfo(f.getAbsolutePath()).getLastModified());
        final long newFileModified = fileModified - 2 * 24 * 3600;
        Unix.setLinkTimestamps(f.getAbsolutePath(), newFileModified, newFileModified);
        assertEquals(newFileModified, Unix.getLinkInfo(f.getAbsolutePath()).getLastModified());
        assertEquals(newFileModified, f.lastModified() / 1000);
    }

    @Test(groups =
        { "requires_unix" })
    public void testGetLinkInfoSymLinkDanglingLink() throws IOException
    {
        final File s = new File(workingDirectory, "someDanglingLink");
        Unix.createSymbolicLink("link_to_nowhere", s.getAbsolutePath());
        final Stat info = Unix.tryGetLinkInfo(s.getAbsolutePath(), true);
        assertNotNull(info);
        assertEquals(FileLinkType.SYMLINK, info.getLinkType());
        assertTrue(info.isSymbolicLink());
        assertEquals("link_to_nowhere", info.tryGetSymbolicLink());
        try
        {
            Unix.getFileInfo(s.getAbsolutePath());
            fail("getFileInfo() on a dangling link should fail");
        } catch (IOExceptionUnchecked ex)
        {
            // Expected: the target doesn't exist.
        }
    }

    @Test(groups =
        { "requires_unix" })
    public void testGetLinkInfoNonExistent() throws IOException
    {
        final File s = new File(workingDirectory, "nonExistent");
        assertNull(Unix.tryGetLinkInfo(s.getAbsolutePath(), false));
        try
        {
            Unix.getLinkInfo(s.getAbsolutePath());
            fail("getLinkInfo() on a missing file should fail");
        } catch (IOExceptionUnchecked ex)
        {
            // Expected.
        }
    }

    @Test(groups =
        { "requires_unix" }, expectedExceptions = NullPointerException.class)
    public void testCreateSymbolicLinkNull() throws IOException
    {
        Unix.createSymbolicLink(null, null);
    }

    @Test(groups =
        { "requires_unix" })
    public void testGetUid()
    {
        assertTrue(Unix.getUid() >= 0);
    }

    @Test(groups =
        { "requires_unix" })
    public void testGetGid()
    {
        assertTrue(Unix.getGid() >= 0);
    }

    @Test(groups =
        { "requires_unix" })
    public void testTryGetGroupNameForGid()
    {
        final String rootGroup = Unix.tryGetGroupNameForGid(0);
        assertTrue(rootGroup, "root".equals(rootGroup) || "wheel".equals(rootGroup));
    }

    @Test(groups =
        { "requires_unix" })
    public void testTryGetGroupByGid()
    {
        final Group group = Unix.tryGetGroupByGid(0);
        assertNotNull(group);
        final String rootGroup = group.getGroupName();
        assertTrue(rootGroup, "root".equals(rootGroup) || "wheel".equals(rootGroup));
        assertNotNull(group.getGroupMembers());
    }

    @Test(groups =
        { "requires_unix" })
    public void testTryGetUserByUid()
    {
        final Password user = Unix.tryGetUserByUid(0);
        assertNotNull(user);
        assertEquals("root", user.getUserName());
        assertEquals("root", Unix.tryGetUserNameForUid(0));
    }
}
