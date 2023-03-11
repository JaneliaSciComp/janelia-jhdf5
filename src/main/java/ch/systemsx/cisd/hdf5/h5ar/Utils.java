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

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import ch.systemsx.cisd.base.unix.FileLinkType;
import ch.systemsx.cisd.base.unix.Unix;
import ch.systemsx.cisd.base.utilities.OSUtilities;
import ch.systemsx.cisd.hdf5.HDF5ObjectType;

/**
 * Utility methods for h5ar.
 * 
 * @author Bernd Rinn
 */
final class Utils
{
    static final long MILLIS_PER_SECOND = 1000L;

    final static int UNKNOWN = -1;

    final static short UNKNOWN_S = -1;

    private static final char[] HEX_CHARACTERS =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', };

    private Utils()
    {
        // Not to be instantiated.
    }

    /**
     * Converts a CRC32 checksum to a string representation.
     */
    static String crc32ToString(final int checksum)
    {
        final char buf[] = new char[8];
        int w = checksum;
        for (int i = 0, x = 7; i < 4; i++)
        {
            buf[x--] = HEX_CHARACTERS[w & 0xf];
            buf[x--] = HEX_CHARACTERS[(w >>> 4) & 0xf];
            w >>= 8;
        }
        return new String(buf);
    }

    /**
     * Creates a string representation for the given permissions.
     */
    static String permissionsToString(int permissions, boolean directory, boolean numeric)
    {
        if (numeric)
        {
            return Integer.toString(permissions, 8);
        } else
        {
            final short perms = (short) permissions;
            final StringBuilder b = new StringBuilder();
            b.append(directory ? 'd' : '-');
            b.append((perms & Unix.S_IRUSR) != 0 ? 'r' : '-');
            b.append((perms & Unix.S_IWUSR) != 0 ? 'w' : '-');
            b.append((perms & Unix.S_IXUSR) != 0 ? ((perms & Unix.S_ISUID) != 0 ? 's' : 'x')
                    : ((perms & Unix.S_ISUID) != 0 ? 'S' : '-'));
            b.append((perms & Unix.S_IRGRP) != 0 ? 'r' : '-');
            b.append((perms & Unix.S_IWGRP) != 0 ? 'w' : '-');
            b.append((perms & Unix.S_IXGRP) != 0 ? ((perms & Unix.S_ISGID) != 0 ? 's' : 'x')
                    : ((perms & Unix.S_ISGID) != 0 ? 'S' : '-'));
            b.append((perms & Unix.S_IROTH) != 0 ? 'r' : '-');
            b.append((perms & Unix.S_IWOTH) != 0 ? 'w' : '-');
            b.append((perms & Unix.S_IXOTH) != 0 ? ((perms & Unix.S_ISVTX) != 0 ? 't' : 'x')
                    : ((perms & Unix.S_ISVTX) != 0 ? 'T' : '-'));
            return b.toString();
        }
    }

    /**
     * Returns the parent of <var>normalizedPath</var>, or "" if <var>normalizedPath</var> is the
     * root "/".
     */
    static String getParentPath(String normalizedPath)
    {
        final int lastSlashIdx = normalizedPath.lastIndexOf('/');
        if (lastSlashIdx <= 0)
        {
            return normalizedPath.length() <= 1 ? "" : "/";
        } else
        {
            return normalizedPath.substring(0, lastSlashIdx);
        }
    }

    /**
     * Returns the name part of <var>path</var>.
     */
    static String getName(String path)
    {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static String normalizeToUnix(String unixOrWindowsPath)
    {
        final String pathToNormalize =
                OSUtilities.isWindows() ? unixOrWindowsPath.replace('/', '\\') : unixOrWindowsPath;
        final String normalized = FilenameUtils.normalize(pathToNormalize);
        return OSUtilities.isWindows() && normalized != null ? normalized.replace('\\', '/') : normalized;
    }

    /**
     * Returns a normalized path: it starts with "/" and doesn't have "/" at the end, except if it
     * is the root path "/". This method internally calls {@link FilenameUtils#normalize(String)}
     * and thus removes any '.' and '..' elements.
     */
    static String normalizePath(String hdf5ObjectPath)
    {
        String prenormalizedPath = normalizeToUnix(hdf5ObjectPath);
        if (prenormalizedPath == null)
        {
            prenormalizedPath = normalizeToUnix(hdf5ObjectPath.replace("//", "/"));
            if (prenormalizedPath == null)
            {
                prenormalizedPath = hdf5ObjectPath.replace("//", "/");
            }
        }
        final String pathStartingWithSlash =
                (prenormalizedPath.startsWith("/") ? prenormalizedPath : "/" + prenormalizedPath);
        return (pathStartingWithSlash.length() > 1 && pathStartingWithSlash.endsWith("/")) ? pathStartingWithSlash
                .substring(0, pathStartingWithSlash.length() - 1) : pathStartingWithSlash;
    }

    /**
     * Returns the absolute normalized {@link File} for <var>path</var>.
     */
    static File normalizePath(File path)
    {
        return new File(FilenameUtils.normalizeNoEndSeparator(path.getAbsolutePath()));
    }

    /**
     * Concatenates <var>parentDirectory</var> and <var>name</var> to a new path and return it.
     */
    static String concatLink(String parentDirectory, String name)
    {
        return parentDirectory.endsWith("/") ? parentDirectory + name : parentDirectory + "/"
                + name;
    }

    /**
     * /** Returns an {@link ArchiveEntry} from a {@link LinkRecord}. Can handle <code>null</code>
     * {@link LinkRecord}s.
     */
    static ArchiveEntry tryToArchiveEntry(String dir, String path, LinkRecord linkOrNull,
            IdCache idCache)
    {
        return linkOrNull != null ? new ArchiveEntry(dir, path, linkOrNull, idCache) : null;
    }

    /**
     * Returns the UID of the current user or {@link Utils#UNKNOWN}, if that cannot be determined.
     */
    static int getCurrentUid()
    {
        if (Unix.isOperational())
        {
            return Unix.getUid();
        } else
        {
            return Utils.UNKNOWN;
        }
    }

    /**
     * Returns the GID of the current user or {@link Utils#UNKNOWN}, if that cannot be determined.
     */
    static int getCurrentGid()
    {
        if (Unix.isOperational())
        {
            return Unix.getGid();
        } else
        {
            return Utils.UNKNOWN;
        }
    }

    static FileLinkType translateType(final HDF5ObjectType hdf5Type)
    {
        switch (hdf5Type)
        {
            case DATASET:
                return FileLinkType.REGULAR_FILE;
            case GROUP:
                return FileLinkType.DIRECTORY;
            case SOFT_LINK:
                return FileLinkType.SYMLINK;
            default:
                return FileLinkType.OTHER;
        }
    }

}
