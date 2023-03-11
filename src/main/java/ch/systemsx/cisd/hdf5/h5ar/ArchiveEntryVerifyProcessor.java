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
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.CRC32;

import hdf.hdf5lib.exceptions.HDF5Exception;

import org.apache.commons.io.FileUtils;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.base.unix.FileLinkType;
import ch.systemsx.cisd.base.unix.Unix;
import ch.systemsx.cisd.base.unix.Unix.Stat;
import ch.systemsx.cisd.base.utilities.OSUtilities;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

/**
 * An {@Link IArchiveEntryProcessor} that performs a verify operation versus a directory on
 * the file system.
 * 
 * @author Bernd Rinn
 */
class ArchiveEntryVerifyProcessor implements IArchiveEntryProcessor
{
    private final IArchiveEntryVisitor visitor;

    private final File rootDirectoryOnFS;

    private final Set<File> filesOnFSOrNull;

    private final String rootDirectoryInArchive;

    private final byte[] buffer;

    private final boolean checkAttributes;

    private final boolean numeric;

    ArchiveEntryVerifyProcessor(IArchiveEntryVisitor visitor, File rootDirectoryOnFS,
            Set<File> filesOnFSOrNull, byte[] buffer, boolean checkAttributes, boolean numeric)
    {
        this(visitor, rootDirectoryOnFS, filesOnFSOrNull, "", buffer, checkAttributes, numeric);
    }

    ArchiveEntryVerifyProcessor(IArchiveEntryVisitor visitor, File rootDirectoryOnFS,
            Set<File> filesOnFSOrNull, String rootDirectoryInArchive, byte[] buffer,
            boolean checkAttributes, boolean numeric)
    {
        this.visitor = visitor;
        this.rootDirectoryOnFS = rootDirectoryOnFS;
        this.filesOnFSOrNull = filesOnFSOrNull;
        this.rootDirectoryInArchive = Utils.normalizePath(rootDirectoryInArchive);
        this.buffer = buffer;
        this.checkAttributes = checkAttributes;
        this.numeric = numeric;
    }

    @Override
    public boolean process(String dir, String path, LinkRecord link, IHDF5Reader reader,
            IdCache idCache, IErrorStrategy errorStrategy) throws IOException
    {
        final String errorMessage = checkLink(link, path, idCache);
        visitor.visit(new ArchiveEntry(dir, path, link, idCache, errorMessage));
        if (filesOnFSOrNull != null)
        {
            filesOnFSOrNull.remove(new File(rootDirectoryOnFS, path));
        }
        return true;
    }

    @Override
    public void postProcessDirectory(String dir, String path, LinkRecord link, IHDF5Reader reader,
            IdCache idCache, IErrorStrategy errorStrategy) throws IOException, HDF5Exception
    {
    }

    private String checkLink(LinkRecord link, String path, IdCache idCache) throws IOException
    {
        if (rootDirectoryInArchive.length() > 0 && path.startsWith(rootDirectoryInArchive) == false)
        {
            return "Object '" + path + "' does not start with path prefix '"
                    + rootDirectoryInArchive + "'.";
        }
        final String strippedPath = path.substring(rootDirectoryInArchive.length());
        final File f = new File(rootDirectoryOnFS, strippedPath);
        if (exists(f) == false)
        {
            link.setVerifiedType(FileLinkType.OTHER);
            return "Object '" + strippedPath + "' does not exist on file system.";
        }
        final String symbolicLinkOrNull = tryGetSymbolicLink(f);
        if (symbolicLinkOrNull != null)
        {
            link.setVerifiedType(FileLinkType.SYMLINK);
            if (link.isSymLink() == false)
            {
                return "Object '" + strippedPath + "' is a " + link.getLinkType()
                        + " in archive, but a symlink on file system.";
            }
            if (symbolicLinkOrNull.equals(link.tryGetLinkTarget()) == false)
            {
                return "Symlink '" + strippedPath + "' links to '" + link.tryGetLinkTarget()
                        + "' in archive, but to '" + symbolicLinkOrNull + "' on file system";
            }
        } else if (f.isDirectory())
        {
            link.setVerifiedType(FileLinkType.DIRECTORY);
            if (link.isDirectory() == false)
            {
                if (Unix.isOperational() || OSUtilities.isWindows())
                {
                    return "Object '" + strippedPath + "' is a " + link.getLinkType()
                            + " in archive, but a directory on file system.";
                } else
                {
                    return "Object '" + strippedPath + "' is a " + link.getLinkType()
                            + " in archive, but a directory on file system (error may be "
                            + "inaccurate because Unix system calls are not available.)";
                }
            }
        } else
        {
            link.setVerifiedType(FileLinkType.REGULAR_FILE);
            if (link.isDirectory())
            {
                return "Object '" + strippedPath
                        + "' is a directory in archive, but a file on file system.";

            }
            if (link.isSymLink())
            {
                if (Unix.isOperational() || OSUtilities.isWindows())
                {
                    return "Object '" + strippedPath
                            + "' is a symbolic link in archive, but a file on file system.";
                } else
                {
                    return "Object '"
                            + strippedPath
                            + "' is a symbolic link in archive, but a file on file system "
                            + "(error may be inaccurate because Unix system calls are not available.).";
                }

            }
            final long size = f.length();
            final int crc32 = calcCRC32Filesystem(f, buffer);
            link.setFileVerification(size, crc32, f.lastModified() / Utils.MILLIS_PER_SECOND);
            if (link.getSize() != size)
            {
                return "File '" + f.getAbsolutePath() + "' failed size test, expected: "
                        + link.getSize() + ", found: " + size;
            }
            if (link.getSize() > 0 && link.getCrc32() == 0)
            {
                return "File '" + f.getAbsolutePath() + "': cannot verify (missing CRC checksum).";
            }
            if (link.getCrc32() != crc32)
            {
                return "File '" + f.getAbsolutePath() + "' failed CRC checksum test, expected: "
                        + Utils.crc32ToString(link.getCrc32()) + ", found: "
                        + Utils.crc32ToString(crc32) + ".";
            }
        }
        return checkAttributes ? doFilesystemAttributeCheck(f, idCache, link, numeric) : null;
    }

    private static boolean exists(final File f)
    {
        if (Unix.isOperational())
        {
            return Unix.tryGetLinkInfo(f.getPath(), false) != null;
        } else
        {
            return f.exists();
        }
    }

    private static String tryGetSymbolicLink(File f)
    {
        if (Unix.isOperational())
        {
            return Unix.getLinkInfo(f.getPath()).tryGetSymbolicLink();
        } else
        {
            return null;
        }
    }

    private static int calcCRC32Filesystem(File source, byte[] buffer) throws IOException
    {
        try (final InputStream input = FileUtils.openInputStream(source))
        {
            final CRC32 crc32 = new CRC32();
            int n = 0;
            while (-1 != (n = input.read(buffer)))
            {
                crc32.update(buffer, 0, n);
            }
            return (int) crc32.getValue();
        }
    }

    private static String doFilesystemAttributeCheck(File file, IdCache idCache, LinkRecord link,
            boolean numeric)
    {
        final StringBuilder sb = new StringBuilder();
        if (link.hasLastModified())
        {
            final long expectedLastModifiedMillis = link.getLastModified() * Utils.MILLIS_PER_SECOND;
            final long foundLastModifiedMillis = Unix.getLinkInfo(file.getAbsolutePath()).getLastModified() * Utils.MILLIS_PER_SECOND;
            if (expectedLastModifiedMillis != foundLastModifiedMillis)
            {
                sb.append(String.format("'last modified time': (expected: "
                        + "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS, found: "
                        + "%2$tY-%2$tm-%2$td %2$tH:%2$tM:%2$tS) ", expectedLastModifiedMillis,
                        foundLastModifiedMillis));
            }
        }
        if (link.hasUnixPermissions() && Unix.isOperational())
        {
            final Stat info = Unix.getLinkInfo(file.getPath(), false);
            if (link.getPermissions() != info.getPermissions()
                    || link.getLinkType() != info.getLinkType())
            {
                sb.append(String.format("'access permissions': (expected: %s, found: %s) ", Utils
                        .permissionsToString(link.getPermissions(), link.isDirectory(), numeric),
                        Utils.permissionsToString(info.getPermissions(),
                                info.getLinkType() == FileLinkType.DIRECTORY, numeric)));
            }
            if (link.getUid() != info.getUid() || link.getGid() != info.getGid())
            {
                sb.append(String.format("'ownerwhip': (expected: %s:%s, found: %s:%s",
                        idCache.getUser(link, numeric), idCache.getGroup(link, numeric),
                        idCache.getUser(info, numeric), idCache.getGroup(info, numeric)));
            }
        }
        if (sb.length() == 0)
        {
            return null;
        } else
        {
            return "File '" + file.getAbsolutePath() + "': " + sb.toString();
        }
    }

    @Override
    public ArchiverException createException(String objectPath, String detailedMsg)
    {
        return new VerifyArchiveException(objectPath, detailedMsg);
    }

    @Override
    public ArchiverException createException(String objectPath, HDF5Exception cause)
    {
        return new VerifyArchiveException(objectPath, cause);
    }

    @Override
    public ArchiverException createException(String objectPath, RuntimeException cause)
    {
        return new VerifyArchiveException(objectPath, cause);
    }

    @Override
    public ArchiverException createException(File file, IOException cause)
    {
        return new VerifyArchiveException(file, cause);
    }

}
