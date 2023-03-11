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
import java.io.OutputStream;
import java.util.zip.CRC32;

import org.apache.commons.io.FileUtils;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.base.unix.FileLinkType;
import ch.systemsx.cisd.base.unix.Unix;
import ch.systemsx.cisd.base.unix.Unix.Stat;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * A processor that extracts files from an archive to the file system.
 * 
 * @author Bernd Rinn
 */
class ArchiveEntryExtractProcessor implements IArchiveEntryProcessor
{
    private static final int ROOT_UID = 0;

    private final IArchiveEntryVisitor visitorOrNull;

    private final ArchivingStrategy strategy;

    private final File rootDirectory;

    private final String rootPathToStrip;

    private final byte[] buffer;

    private final GroupCache groupCache;

    ArchiveEntryExtractProcessor(IArchiveEntryVisitor visitorOrNull, ArchivingStrategy strategy,
            File rootDirectory, String rootPathToStrip, byte[] buffer)
    {
        this.visitorOrNull = visitorOrNull;
        this.strategy = strategy;
        this.rootDirectory = rootDirectory;
        final String normalizedRootPathToStrip = Utils.normalizePath(rootPathToStrip);
        this.rootPathToStrip =
                "/".equals(normalizedRootPathToStrip) ? "" : normalizedRootPathToStrip;
        this.buffer = buffer;
        this.groupCache = new GroupCache();
    }

    @Override
    public boolean process(String dir, String path, LinkRecord link, IHDF5Reader reader,
            IdCache idCache, IErrorStrategy errorStrategy) throws IOException
    {
        if (strategy.doExclude(path, link.isDirectory()))
        {
            return false;
        }
        final File file = createFile(path);
        if (link.isDirectory())
        {
            if (file.exists() && file.isDirectory() == false)
            {
                file.delete();
            }
            file.mkdirs();
            if (file.isDirectory() == false)
            {
                errorStrategy.dealWithError(new UnarchivingException(file, new IOException(
                        "Failed to make directory '" + file.getAbsolutePath() + "'.")));
            }
            if (visitorOrNull != null)
            {
                visitorOrNull.visit(new ArchiveEntry(dir, path, link, idCache));
            }
        } else if (link.tryGetLinkTarget() != null && Unix.isOperational())
        {
            try
            {
                file.delete();
                final String linkTarget = link.tryGetLinkTarget();
                Unix.createSymbolicLink(linkTarget, file.getAbsolutePath());
                restoreAttributes(file, link);
                if (visitorOrNull != null)
                {
                    visitorOrNull.visit(new ArchiveEntry(dir, path, link, idCache));
                }
            } catch (IOExceptionUnchecked ex)
            {
                errorStrategy.dealWithError(new UnarchivingException(file, ex));
            }
        } else
        {
            if (link.isSymLink())
            {
                if (Unix.isOperational() == false)
                {
                    errorStrategy.warning("Warning: extracting symlink as regular file because"
                            + " Unix calls are not available on this system.");
                } else
                {
                    errorStrategy.dealWithError(new UnarchivingException(path,
                            new HDF5JavaException("Symlink doesn't have a link target.")));
                }
            } else
            {
                try
                {
                    // Here we don't rely on link.getSize() to protect against wrong index entries.
                    final long size = reader.object().getSize(path);
                    final int crc32 = copyFromHDF5(reader, path, size, file);
                    restoreAttributes(file, link);
                    final FileSizeType sizeType = getFileSizeType(file);
                    link.setVerifiedType(sizeType.type);
                    link.setFileVerification(sizeType.size, crc32, file.lastModified()
                            / Utils.MILLIS_PER_SECOND);
                    final ArchiveEntry entry = new ArchiveEntry(dir, path, link, idCache);
                    if (visitorOrNull != null)
                    {
                        visitorOrNull.visit(entry);
                    }
                    if (entry.isOK() == false)
                    {
                        errorStrategy.dealWithError(new UnarchivingException(path, entry
                                .getStatus(true)));
                    }
                } catch (IOException ex)
                {
                    errorStrategy.dealWithError(new UnarchivingException(file, ex));
                } catch (HDF5Exception ex)
                {
                    errorStrategy.dealWithError(new UnarchivingException(path, ex));
                }
            }
        }
        return true;
    }

    @Override
    public void postProcessDirectory(String dir, String path, LinkRecord link, IHDF5Reader reader,
            IdCache idCache, IErrorStrategy errorStrategy) throws IOException, HDF5Exception
    {
        final File file = createFile(path);
        restoreAttributes(file, link);
    }

    private File createFile(String path)
    {
        final String workingPath =
                path.startsWith(rootPathToStrip) ? path.substring(rootPathToStrip.length()) : path;
        final File file = new File(rootDirectory, workingPath);
        return file;
    }

    /**
     * A record for file size and type.
     * 
     * @author Bernd Rinn
     */
    private static class FileSizeType
    {
        final FileLinkType type;

        final long size;

        FileSizeType(FileLinkType type, long size)
        {
            super();
            this.type = type;
            this.size = size;
        }
    }

    private FileSizeType getFileSizeType(final File file)
    {
        if (Unix.isOperational())
        {
            final Stat info = Unix.getLinkInfo(file.getPath(), false);
            return new FileSizeType(info.getLinkType(), info.getSize());
        } else
        {
            return new FileSizeType((file.isDirectory()) ? FileLinkType.DIRECTORY
                    : (file.isFile() ? FileLinkType.REGULAR_FILE : FileLinkType.OTHER),
                    file.length());
        }

    }

    private int copyFromHDF5(final IHDF5Reader reader, final String objectPath, final long size,
            File destination) throws IOException
    {
        try (final OutputStream output = FileUtils.openOutputStream(destination))
        {
            final CRC32 crc32 = new CRC32();
            long offset = 0;
            while (offset < size)
            {
                final int n =
                        reader.opaque().readArrayToBlockWithOffset(objectPath, buffer, buffer.length,
                                offset, 0);
                offset += n;
                output.write(buffer, 0, n);
                crc32.update(buffer, 0, n);
            }
            output.close(); // Make sure we don't silence exceptions on closing.
            return (int) crc32.getValue();
        }
    }

    private void restoreAttributes(File file, LinkRecord linkInfoOrNull)
    {
        assert file != null;

        if (linkInfoOrNull != null)
        {
            if (linkInfoOrNull.hasLastModified())
            {
                Unix.setLinkTimestamps(file.getAbsolutePath(), linkInfoOrNull.getLastModified(), linkInfoOrNull.getLastModified());
            }
            if (linkInfoOrNull.hasUnixPermissions() && Unix.isOperational())
            {
                if (linkInfoOrNull.isSymLink() == false)
                {
                    Unix.setAccessMode(file.getPath(), linkInfoOrNull.getPermissions());
                }
                if (Unix.getUid() == ROOT_UID) // Are we root?
                {
                    Unix.setLinkOwner(file.getPath(), linkInfoOrNull.getUid(), linkInfoOrNull.getGid());
                } else
                {
                    if (groupCache.isUserInGroup(linkInfoOrNull.getGid()))
                    {
                        Unix.setLinkOwner(file.getPath(), Unix.getUid(), linkInfoOrNull.getGid());
                    }
                }
            }
        }
    }

    @Override
    public ArchiverException createException(String objectPath, String detailedMsg)
    {
        return new UnarchivingException(objectPath, detailedMsg);
    }

    @Override
    public ArchiverException createException(String objectPath, HDF5Exception cause)
    {
        return new UnarchivingException(objectPath, cause);
    }

    @Override
    public ArchiverException createException(String objectPath, RuntimeException cause)
    {
        return new UnarchivingException(objectPath, cause);
    }

    @Override
    public ArchiverException createException(File file, IOException cause)
    {
        return new UnarchivingException(file, cause);
    }

}
