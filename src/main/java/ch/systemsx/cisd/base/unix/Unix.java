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

package ch.systemsx.cisd.base.unix;

import java.io.IOException;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.Group;
import jnr.posix.Passwd;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

/**
 * Access to a narrow set of POSIX file operations needed by the {@code h5ar} archiver, backed by
 * {@code com.github.jnr:jnr-posix} rather than the bespoke native library ("libunix") the original
 * cisd:base implementation used. jnr-posix is a mature, actively-maintained cross-platform POSIX
 * binding already managed by this project's own parent POM; it covers every operation needed here
 * without requiring a native library of this project's own.
 * <p>
 * This is a narrower re-implementation of the original {@code ch.systemsx.cisd.base.unix.Unix},
 * covering only the operations and {@link Stat}/{@link Group}/{@link Password} accessors that
 * {@code ch.systemsx.cisd.hdf5.h5ar} actually calls (verified against that package's source), not
 * the original's full surface.
 *
 * @author Bernd Rinn
 */
public final class Unix
{
    private Unix()
    {
        // Not to be instantiated.
    }

    private static final POSIX posix = POSIXFactory.getPOSIX();

    private static final boolean operational = isNotWindows();

    private static boolean isNotWindows()
    {
        return System.getProperty("os.name", "").toLowerCase().contains("windows") == false;
    }

    // Mode bits from <sys/stat.h>, used only to render permissions as an "rwxr-xr-x"-style string.
    public static final short S_ISUID = 04000;

    public static final short S_ISGID = 02000;

    public static final short S_ISVTX = 01000;

    public static final short S_IRUSR = 00400;

    public static final short S_IWUSR = 00200;

    public static final short S_IXUSR = 00100;

    public static final short S_IRGRP = 00040;

    public static final short S_IWGRP = 00020;

    public static final short S_IXGRP = 00010;

    public static final short S_IROTH = 00004;

    public static final short S_IWOTH = 00002;

    public static final short S_IXOTH = 00001;

    /** The permission bits proper, excluding the file-type bits packed into a raw POSIX mode_t. */
    private static final int PERMISSION_BITS_MASK = 07777;

    /**
     * A class representing the subset of the Unix <code>stat</code> struct that {@code h5ar} uses.
     */
    public static final class Stat
    {
        private final int uid;

        private final int gid;

        private final short permissions;

        private final FileLinkType linkType;

        private final long size;

        private final long lastModified;

        private String symbolicLinkOrNull;

        private Stat(FileStat fileStat)
        {
            this.uid = fileStat.uid();
            this.gid = fileStat.gid();
            this.permissions = (short) (fileStat.mode() & PERMISSION_BITS_MASK);
            this.linkType =
                    fileStat.isSymlink() ? FileLinkType.SYMLINK
                            : fileStat.isDirectory() ? FileLinkType.DIRECTORY
                                    : fileStat.isFile() ? FileLinkType.REGULAR_FILE
                                            : FileLinkType.OTHER;
            this.size = fileStat.st_size();
            this.lastModified = fileStat.mtime();
        }

        private void setSymbolicLinkOrNull(String symbolicLinkOrNull)
        {
            this.symbolicLinkOrNull = symbolicLinkOrNull;
        }

        /**
         * Get link target of the symbolic link or <code>null</code>, if this is not a link or the
         * link target has not been read.
         */
        public String tryGetSymbolicLink()
        {
            return symbolicLinkOrNull;
        }

        public int getUid()
        {
            return uid;
        }

        public int getGid()
        {
            return gid;
        }

        public short getPermissions()
        {
            return permissions;
        }

        public FileLinkType getLinkType()
        {
            return linkType;
        }

        public long getSize()
        {
            return size;
        }

        /**
         * Time when file data was last modified, in seconds since the epoch.
         */
        public long getLastModified()
        {
            return lastModified;
        }
    }

    /**
     * A class representing the subset of the Unix <code>group</code> struct that {@code h5ar}
     * uses.
     */
    public static final class Group
    {
        private final String groupName;

        private final String[] groupMembers;

        private Group(jnr.posix.Group jnrGroup)
        {
            this.groupName = jnrGroup.getName();
            this.groupMembers = jnrGroup.getMembers();
        }

        public String getGroupName()
        {
            return groupName;
        }

        public String[] getGroupMembers()
        {
            return groupMembers;
        }
    }

    /**
     * A class representing the subset of the Unix <code>passwd</code> struct that {@code h5ar}
     * uses.
     */
    public static final class Password
    {
        private final String userName;

        private Password(Passwd jnrPasswd)
        {
            this.userName = jnrPasswd.getLoginName();
        }

        public String getUserName()
        {
            return userName;
        }
    }

    private static IOExceptionUnchecked statException(String operation, String path, Throwable cause)
    {
        return new IOExceptionUnchecked(new IOException(String.format(
                "Cannot %s of file '%s': %s", operation, path, cause.getMessage()), cause));
    }

    /**
     * Returns <code>true</code> if POSIX file operations are available on this platform (always
     * the case on Linux/macOS; never on Windows, which never had a native "libunix" backing to
     * begin with).
     */
    public static boolean isOperational()
    {
        return operational;
    }

    /**
     * Returns the uid of the user that started this process.
     */
    public static int getUid()
    {
        return posix.getuid();
    }

    /**
     * Returns the gid of the user that started this process.
     */
    public static int getGid()
    {
        return posix.getgid();
    }

    /**
     * Returns the information about <var>fileName</var>. Dereferences a symbolic link.
     *
     * @throws IOExceptionUnchecked If the information could not be obtained, e.g. because the file
     *             does not exist.
     */
    public static Stat getFileInfo(String fileName) throws IOExceptionUnchecked
    {
        try
        {
            return new Stat(posix.stat(fileName));
        } catch (RuntimeException ex)
        {
            throw statException("stat", fileName, ex);
        }
    }

    /**
     * Returns the information about <var>linkName</var>. Does not dereference a symbolic link.
     *
     * @throws IOExceptionUnchecked If the information could not be obtained, e.g. because the link
     *             does not exist.
     */
    public static Stat getLinkInfo(String linkName) throws IOExceptionUnchecked
    {
        return getLinkInfo(linkName, true);
    }

    /**
     * Returns the information about <var>linkName</var>. Does not dereference a symbolic link.
     *
     * @param readSymbolicLinkTarget If <code>true</code> and <var>linkName</var> is a symbolic
     *            link, also read the link's target (see {@link Stat#tryGetSymbolicLink()}).
     * @throws IOExceptionUnchecked If the information could not be obtained, e.g. because the link
     *             does not exist.
     */
    public static Stat getLinkInfo(String linkName, boolean readSymbolicLinkTarget)
            throws IOExceptionUnchecked
    {
        final Stat stat;
        try
        {
            stat = new Stat(posix.lstat(linkName));
        } catch (RuntimeException ex)
        {
            throw statException("lstat", linkName, ex);
        }
        if (readSymbolicLinkTarget && stat.getLinkType() == FileLinkType.SYMLINK)
        {
            stat.setSymbolicLinkOrNull(tryReadSymbolicLink(linkName));
        }
        return stat;
    }

    /**
     * Returns the information about <var>linkName</var>, or <code>null</code> if it does not
     * exist or the information could not be obtained. Does not dereference a symbolic link.
     */
    public static Stat tryGetLinkInfo(String linkName, boolean readSymbolicLinkTarget)
    {
        try
        {
            return getLinkInfo(linkName, readSymbolicLinkTarget);
        } catch (IOExceptionUnchecked ex)
        {
            return null;
        }
    }

    /**
     * Returns the value of the symbolic link <var>linkName</var>, or <code>null</code>, if
     * <var>linkName</var> is not a symbolic link.
     *
     * @throws IOExceptionUnchecked If the information could not be obtained, e.g. because the link
     *             does not exist.
     */
    public static String tryReadSymbolicLink(String linkName) throws IOExceptionUnchecked
    {
        final Stat stat = getLinkInfo(linkName, false);
        if (stat.getLinkType() != FileLinkType.SYMLINK)
        {
            return null;
        }
        try
        {
            return posix.readlink(linkName);
        } catch (IOException | RuntimeException ex)
        {
            throw statException("readlink", linkName, ex);
        }
    }

    /**
     * Creates a symbolic link <var>linkName</var> that points to <var>fileName</var>.
     */
    public static void createSymbolicLink(String fileName, String linkName)
            throws IOExceptionUnchecked
    {
        final int result = posix.symlink(fileName, linkName);
        if (result < 0)
        {
            throw new IOExceptionUnchecked(new IOException(String.format(
                    "Creating symbolic link '%s' -> '%s' failed", linkName, fileName)));
        }
    }

    /**
     * Sets the access mode of <var>fileName</var>.
     */
    public static void setAccessMode(String fileName, short mode) throws IOExceptionUnchecked
    {
        final int result = posix.chmod(fileName, mode);
        if (result < 0)
        {
            throw new IOExceptionUnchecked(new IOException(String.format(
                    "Cannot set mode of file '%s'", fileName)));
        }
    }

    /**
     * Sets the owner of <var>linkName</var> to the specified <var>uid</var> and <var>gid</var>
     * values. Does not dereference a symbolic link.
     */
    public static void setLinkOwner(String linkName, int uid, int gid) throws IOExceptionUnchecked
    {
        final int result = posix.lchown(linkName, uid, gid);
        if (result < 0)
        {
            throw new IOExceptionUnchecked(new IOException(String.format(
                    "Cannot set link owner of file '%s'", linkName)));
        }
    }

    /**
     * Changes link timestamps of a file, directory or link. Does not dereference a symbolic link.
     *
     * @param fileName The name of the file or link to change the timestamp of.
     * @param accessTimeSecs The new access time in seconds since start of the epoch.
     * @param modificationTimeSecs The new modification time in seconds since start of the epoch.
     */
    public static void setLinkTimestamps(String fileName, long accessTimeSecs,
            long modificationTimeSecs) throws IOExceptionUnchecked
    {
        final int result =
                posix.lutimes(fileName, new long[] { accessTimeSecs, 0 }, new long[] {
                        modificationTimeSecs, 0 });
        if (result < 0)
        {
            throw new IOExceptionUnchecked(new IOException(String.format(
                    "Cannot set link timestamps of file '%s'", fileName)));
        }
    }

    /**
     * Returns the name of the user identified by <var>uid</var>, or <code>null</code> if no such
     * user exists.
     */
    public static String tryGetUserNameForUid(int uid)
    {
        final Passwd passwd = posix.getpwuid(uid);
        return passwd == null ? null : passwd.getLoginName();
    }

    /**
     * Returns the name of the group identified by <var>gid</var>, or <code>null</code> if no such
     * group exists.
     */
    public static String tryGetGroupNameForGid(int gid)
    {
        final jnr.posix.Group group = posix.getgrgid(gid);
        return group == null ? null : group.getName();
    }

    /**
     * Returns the passwd entry for <var>uid</var>, or <code>null</code> if no such user exists.
     */
    public static Password tryGetUserByUid(int uid)
    {
        final Passwd passwd = posix.getpwuid(uid);
        return passwd == null ? null : new Password(passwd);
    }

    /**
     * Returns the group entry for <var>gid</var>, or <code>null</code> if no such group exists.
     */
    public static Group tryGetGroupByGid(int gid)
    {
        final jnr.posix.Group group = posix.getgrgid(gid);
        return group == null ? null : new Group(group);
    }
}
