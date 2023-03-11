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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.base.io.AdapterIInputStreamToInputStream;
import ch.systemsx.cisd.base.io.AdapterIOutputStreamToOutputStream;
import ch.systemsx.cisd.base.io.IInputStream;
import ch.systemsx.cisd.base.io.IOutputStream;
import ch.systemsx.cisd.hdf5.HDF5DataBlock;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.FileFormatVersionBounds;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.SyncMode;
import ch.systemsx.cisd.hdf5.h5ar.NewArchiveEntry.NewDirectoryArchiveEntry;
import ch.systemsx.cisd.hdf5.h5ar.NewArchiveEntry.NewFileArchiveEntry;
import ch.systemsx.cisd.hdf5.h5ar.NewArchiveEntry.NewSymLinkArchiveEntry;
import ch.systemsx.cisd.hdf5.io.HDF5IOAdapterFactory;

/**
 * An archiver based on HDF5 as archive format for directory with fast random access to particular
 * files in the archive.
 * 
 * @author Bernd Rinn
 */
final class HDF5Archiver implements Closeable, Flushable, IHDF5Archiver, IHDF5ArchiveInfoProvider
{
    private static final String HOUSEKEEPING_SUFFIX = "\1\0";

    public static final int CHUNK_SIZE_AUTO = -1;

    private final static int MB = 1024 * 1024;

    final static int BUFFER_SIZE = 10 * MB;

    private final IHDF5Reader hdf5Reader;

    private final IHDF5Writer hdf5WriterOrNull;

    private final boolean closeReaderOnCloseFile;

    private final IErrorStrategy errorStrategy;

    private final IDirectoryIndexProvider indexProvider;

    private final byte[] buffer;

    private final HDF5ArchiveUpdater updaterOrNull;

    private final HDF5ArchiveDeleter deleterOrNull;

    private final HDF5ArchiveTraverser processor;

    private final IdCache idCache;

    static IHDF5Reader createHDF5Reader(File archiveFile)
    {
        return HDF5FactoryProvider.get().openForReading(archiveFile);
    }

    static IHDF5Writer createHDF5Writer(File archiveFile, FileFormatVersionBounds fileFormat, boolean noSync)
    {
        final IHDF5WriterConfigurator config = HDF5FactoryProvider.get().configure(archiveFile);
        config.fileFormat(fileFormat);
        if (fileFormat.getHighBound().supportsMDCCache())
        {
            config.generateMDCImage();
        }
        config.useUTF8CharacterEncoding();
        config.houseKeepingNameSuffix(HOUSEKEEPING_SUFFIX);
        if (noSync == false)
        {
            config.syncMode(SyncMode.SYNC);
        }
        return config.writer();
    }

    HDF5Archiver(File archiveFile, boolean readOnly)
    {
        this(archiveFile, readOnly, false, FileFormatVersionBounds.V1_8_V1_8, null);
    }

    HDF5Archiver(File archiveFile, boolean readOnly, boolean noSync, FileFormatVersionBounds fileFormatVersionBounds,
            IErrorStrategy errorStrategyOrNull)
    {
        this.buffer = new byte[BUFFER_SIZE];
        this.closeReaderOnCloseFile = true;
        this.hdf5WriterOrNull = readOnly ? null : createHDF5Writer(archiveFile, fileFormatVersionBounds, noSync);
        this.hdf5Reader =
                (hdf5WriterOrNull != null) ? hdf5WriterOrNull : createHDF5Reader(archiveFile);
        if (errorStrategyOrNull == null)
        {
            this.errorStrategy = IErrorStrategy.DEFAULT_ERROR_STRATEGY;
        } else
        {
            this.errorStrategy = errorStrategyOrNull;
        }
        this.indexProvider = new DirectoryIndexProvider(hdf5Reader, errorStrategy);
        this.idCache = new IdCache();
        this.processor = new HDF5ArchiveTraverser(new HDF5ArchiveTraverser.IDirectoryChecker()
            {
                @Override
                public boolean isDirectoryFollowSymlinks(ArchiveEntry entry)
                {
                    final ArchiveEntry resolvedEntry = tryResolveLink(entry);
                    return (resolvedEntry == null) ? false : resolvedEntry.isDirectory();
                }
            }, hdf5Reader, indexProvider, idCache);
        if (hdf5WriterOrNull == null)
        {
            this.updaterOrNull = null;
            this.deleterOrNull = null;
        } else
        {
            this.updaterOrNull =
                    new HDF5ArchiveUpdater(hdf5WriterOrNull, indexProvider, idCache, buffer);
            this.deleterOrNull = new HDF5ArchiveDeleter(hdf5WriterOrNull, indexProvider, idCache);
        }
    }

    HDF5Archiver(IHDF5Reader reader, boolean enforceReadOnly, IErrorStrategy errorStrategyOrNull)
    {
        this.buffer = new byte[BUFFER_SIZE];
        this.closeReaderOnCloseFile = false;
        this.hdf5WriterOrNull =
                (enforceReadOnly == false && reader instanceof IHDF5Writer) ? (IHDF5Writer) reader
                        : null;
        if (errorStrategyOrNull == null)
        {
            this.errorStrategy = IErrorStrategy.DEFAULT_ERROR_STRATEGY;
        } else
        {
            this.errorStrategy = errorStrategyOrNull;
        }
        this.hdf5Reader = reader;
        this.indexProvider = new DirectoryIndexProvider(hdf5Reader, errorStrategy);
        this.idCache = new IdCache();
        this.processor = new HDF5ArchiveTraverser(new HDF5ArchiveTraverser.IDirectoryChecker()
            {
                @Override
                public boolean isDirectoryFollowSymlinks(ArchiveEntry entry)
                {
                    return tryResolveLink(entry).isDirectory();
                }
            }, hdf5Reader, indexProvider, idCache);
        if (hdf5WriterOrNull == null)
        {
            this.updaterOrNull = null;
            this.deleterOrNull = null;
        } else
        {
            this.updaterOrNull =
                    new HDF5ArchiveUpdater(hdf5WriterOrNull, indexProvider, idCache, buffer);
            this.deleterOrNull = new HDF5ArchiveDeleter(hdf5WriterOrNull, indexProvider, idCache);
        }
    }

    //
    // Closeable
    //

    @Override
    public void close()
    {
        if (isClosed() == false)
        {
            flush();
        }
        if (closeReaderOnCloseFile)
        {
            hdf5Reader.close();
        } else
        {
            indexProvider.close();
        }
    }

    @Override
    public boolean isClosed()
    {
        return hdf5Reader.file().isClosed();
    }

    //
    // Flusheable
    //

    @Override
    public void flush()
    {
        if (hdf5WriterOrNull != null)
        {
            hdf5WriterOrNull.file().flush();
        }
    }

    //
    // IHDF5ArchiveInfo
    //

    @Override
    public boolean exists(String path)
    {
        final String normalizedPath = Utils.normalizePath(path);
        final String parentPath = Utils.getParentPath(normalizedPath);
        final String name = Utils.getName(normalizedPath);
        return indexProvider.get(parentPath, false).exists(name);
    }

    @Override
    public boolean isDirectory(String path)
    {
        final String normalizedPath = Utils.normalizePath(path);
        final String parentPath = Utils.getParentPath(normalizedPath);
        final String name = Utils.getName(normalizedPath);
        return indexProvider.get(parentPath, false).isDirectory(name);
    }

    @Override
    public boolean isRegularFile(String path)
    {
        return isRegularFile(tryGetLink(path, false));
    }

    @Override
    public boolean isSymLink(String path)
    {
        return isSymLink(tryGetLink(path, false));
    }

    @Override
    public ArchiveEntry tryGetEntry(String path, boolean readLinkTarget)
    {
        final String normalizedPath = Utils.normalizePath(path);
        if ("/".equals(normalizedPath))
        {
            return new ArchiveEntry("", "/", LinkRecord.getLinkRecordForArchiveRoot(hdf5Reader
                    .file().getFile()), idCache);
        }
        final String parentPath = Utils.getParentPath(normalizedPath);
        final String name = Utils.getName(normalizedPath);
        return Utils.tryToArchiveEntry(parentPath, normalizedPath,
                indexProvider.get(parentPath, readLinkTarget).tryGetLink(name), idCache);
    }

    private LinkRecord tryGetLink(String path, boolean readLinkTargets)
    {
        final String normalizedPath = Utils.normalizePath(path);
        final String parentPath = Utils.getParentPath(normalizedPath);
        final String name = Utils.getName(normalizedPath);
        return indexProvider.get(parentPath, readLinkTargets).tryGetLink(name);
    }

    @Override
    public ArchiveEntry tryResolveLink(ArchiveEntry entry)
    {
        if (entry == null)
        {
            return null;
        }
        ArchiveEntry workEntry = entry;
        String firstPath = null;
        if (entry.isSymLink())
        {
            Set<String> workPathSet = null;
            while (workEntry != null && workEntry.isSymLink())
            {
                if (firstPath == null)
                {
                    firstPath = workEntry.getPath();
                } else
                {
                    if (workPathSet == null)
                    {
                        workPathSet = new HashSet<String>();
                        workPathSet.add(firstPath);
                    }
                    if (workPathSet.contains(workEntry.getPath()))
                    {
                        // The link targets form a loop, resolve to null.
                        return null;
                    }
                    workPathSet.add(workEntry.getPath());
                }
                String linkTarget;
                if (workEntry.hasLinkTarget())
                {
                    linkTarget = workEntry.getLinkTarget();
                } else
                {
                    workEntry = tryGetEntry(workEntry.getPath(), true);
                    linkTarget = workEntry.getLinkTarget();
                }
                if (linkTarget.startsWith("/") == false)
                {
                    linkTarget = Utils.concatLink(workEntry.getParentPath(), linkTarget);
                }
                linkTarget = Utils.normalizePath(linkTarget);
                if (linkTarget == null) // impossible link target like '/..'
                {
                    return null;
                }
                workEntry = tryGetEntry(linkTarget, true);
            }
        }
        return workEntry;
    }

    @Override
    public ArchiveEntry tryGetResolvedEntry(String path, boolean keepPath)
    {
        final ArchiveEntry entry = tryGetEntry(path, true);
        ArchiveEntry resolvedEntry = tryResolveLink(entry);
        if (resolvedEntry == null)
        {
            return null;
        }
        if (entry != resolvedEntry && keepPath)
        {
            resolvedEntry = new ArchiveEntry(entry, resolvedEntry);
        }
        return resolvedEntry;
    }

    private static boolean isRegularFile(LinkRecord linkOrNull)
    {
        return linkOrNull != null && linkOrNull.isRegularFile();
    }

    private static boolean isSymLink(LinkRecord linkOrNull)
    {
        return linkOrNull != null && linkOrNull.isSymLink();
    }

    //
    // IHDF5ArchiveReader
    //

    @Override
    public List<ArchiveEntry> list()
    {
        return list("/", ListParameters.DEFAULT);
    }

    @Override
    public List<ArchiveEntry> list(final String fileOrDir)
    {
        return list(fileOrDir, ListParameters.DEFAULT);
    }

    @Override
    public List<ArchiveEntry> list(final String fileOrDir, final ListParameters params)
    {
        final List<ArchiveEntry> result = new ArrayList<ArchiveEntry>(100);
        list(fileOrDir, new IArchiveEntryVisitor()
            {
                @Override
                public void visit(ArchiveEntry entry)
                {
                    result.add(entry);
                }
            }, params);
        return result;
    }

    @Override
    public List<ArchiveEntry> test()
    {
        final List<ArchiveEntry> result = new ArrayList<ArchiveEntry>(100);
        list("/", new IArchiveEntryVisitor()
            {
                @Override
                public void visit(ArchiveEntry entry)
                {
                    if (entry.isOK() == false)
                    {
                        result.add(entry);
                    }
                }
            }, ListParameters.TEST);
        return result;
    }

    @Override
    public IHDF5Archiver list(String fileOrDir, IArchiveEntryVisitor visitor)
    {
        return list(fileOrDir, visitor, ListParameters.DEFAULT);
    }

    @Override
    public IHDF5Archiver list(final String fileOrDir, final IArchiveEntryVisitor visitor,
            final ListParameters params)
    {
        final String normalizedPath = Utils.normalizePath(fileOrDir);
        final IArchiveEntryVisitor decoratedVisitor = new IArchiveEntryVisitor()
            {
                @Override
                public void visit(ArchiveEntry entry)
                {
                    if (params.isIncludeTopLevelDirectoryEntry() == false
                            && normalizedPath.equals(entry.getPath()))
                    {
                        return;
                    }
                    ArchiveEntry workEntry = entry;
                    if (workEntry.isSymLink() && params.isResolveSymbolicLinks())
                    {
                        workEntry = tryResolveLink(workEntry);
                        if (workEntry == null)
                        {
                            return;
                        }
                        if (workEntry != entry)
                        {
                            workEntry = new ArchiveEntry(entry, workEntry);
                        }
                    }
                    if (params.isSuppressDirectoryEntries() == false
                            || workEntry.isDirectory() == false)
                    {
                        visitor.visit(workEntry);
                    }
                }
            };
        final ArchiveEntryListProcessor listProcessor =
                new ArchiveEntryListProcessor(decoratedVisitor, buffer, params.isTestArchive());
        processor.process(normalizedPath, params.isRecursive(), params.isReadLinkTargets(),
                params.isFollowSymbolicLinks(), listProcessor);
        return this;
    }

    @Override
    public List<ArchiveEntry> verifyAgainstFilesystem(File rootDirectoryOnFS)
    {
        return verifyAgainstFilesystem("/", rootDirectoryOnFS, VerifyParameters.DEFAULT);
    }

    @Override
    public List<ArchiveEntry> verifyAgainstFilesystem(File rootDirectoryOnFS, VerifyParameters params)
    {
        return verifyAgainstFilesystem("/", rootDirectoryOnFS, params);
    }

    @Override
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS)
    {
        return verifyAgainstFilesystem(fileOrDir, rootDirectoryOnFS, VerifyParameters.DEFAULT);
    }

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor)
    {
        return verifyAgainstFilesystem(fileOrDir, rootDirectoryOnFS, visitor,
                VerifyParameters.DEFAULT);
    }

    @Override
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            VerifyParameters params)
    {
        final List<ArchiveEntry> verifyErrors = new ArrayList<ArchiveEntry>();
        verifyAgainstFilesystem(fileOrDir, rootDirectoryOnFS, new IArchiveEntryVisitor()
            {
                @Override
                public void visit(ArchiveEntry entry)
                {
                    if (entry.isOK() == false)
                        verifyErrors.add(entry);
                }
            }, params);
        return verifyErrors;
    }

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, IArchiveEntryVisitor missingArchiveEntryVisitorOrNull,
            VerifyParameters params)
    {
        final Set<File> filesOnFSOrNull =
                (missingArchiveEntryVisitorOrNull != null)
                    ? getFiles(Paths.get(rootDirectoryOnFS.toString(), fileOrDir).toFile(), params.isRecursive())
                    : null;
        final ArchiveEntryVerifyProcessor verifyProcessor =
                new ArchiveEntryVerifyProcessor(visitor, rootDirectoryOnFS, filesOnFSOrNull,
                        buffer, params.isVerifyAttributes(), params.isNumeric());
        processor.process(fileOrDir, params.isRecursive(), true, false, verifyProcessor);
        if (filesOnFSOrNull != null && filesOnFSOrNull.isEmpty() == false)
        {
            for (File f : filesOnFSOrNull)
            {
                missingArchiveEntryVisitorOrNull.visit(new ArchiveEntry(HDF5ArchiveUpdater
                        .getRelativePath(rootDirectoryOnFS, f.getParentFile()), HDF5ArchiveUpdater
                        .getRelativePath(rootDirectoryOnFS, f), LinkRecord.getLinkRecordForLink(f),
                        idCache));
            }
        }
        return this;
    }

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, VerifyParameters params)
    {
        return verifyAgainstFilesystem(fileOrDir, rootDirectoryOnFS, visitor, null, params);
    }

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, IArchiveEntryVisitor visitor,
            IArchiveEntryVisitor missingArchiveEntryVisitorOrNull, VerifyParameters params)
    {
        final Set<File> filesOnFSOrNull =
                (missingArchiveEntryVisitorOrNull != null)
                    ? getFiles(Paths.get(rootDirectoryOnFS.toString(), fileOrDir).toFile(), params.isRecursive())
                    : null;
        final ArchiveEntryVerifyProcessor verifyProcessor =
                new ArchiveEntryVerifyProcessor(visitor, rootDirectoryOnFS, filesOnFSOrNull,
                        rootDirectoryInArchive, buffer, params.isVerifyAttributes(),
                        params.isNumeric());
        processor.process(fileOrDir, params.isRecursive(), true, false, verifyProcessor);
        if (filesOnFSOrNull != null && filesOnFSOrNull.isEmpty() == false)
        {
            for (File f : filesOnFSOrNull)
            {
                missingArchiveEntryVisitorOrNull.visit(new ArchiveEntry(f.getParent(), f.getPath(),
                        LinkRecord.getLinkRecordForLink(f), idCache));
            }
        }
        return this;
    }

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, IArchiveEntryVisitor visitor, VerifyParameters params)
    {
        return verifyAgainstFilesystem(fileOrDir, rootDirectoryOnFS, rootDirectoryInArchive,
                visitor, null, params);
    }

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, IArchiveEntryVisitor missingArchiveEntryVisitor)
    {
        return verifyAgainstFilesystem(fileOrDir, rootDirectoryOnFS, visitor,
                missingArchiveEntryVisitor, VerifyParameters.DEFAULT);
    }

    @Override
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, VerifyParameters params)
    {
        final List<ArchiveEntry> verifyErrors = new ArrayList<ArchiveEntry>();
        verifyAgainstFilesystem(fileOrDir, rootDirectoryOnFS, rootDirectoryInArchive,
                new IArchiveEntryVisitor()
                    {
                        @Override
                        public void visit(ArchiveEntry entry)
                        {
                            if (entry.isOK() == false)
                            {
                                verifyErrors.add(entry);
                            }
                        }
                    }, params);
        return verifyErrors;
    }

    @Override
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive)
    {
        return verifyAgainstFilesystem(fileOrDir, rootDirectoryOnFS, rootDirectoryInArchive,
                VerifyParameters.DEFAULT);
    }

    private static Set<File> getFiles(File fsRoot, boolean recursive)
    {
        final Set<File> result = new HashSet<File>();
        if (fsRoot.isDirectory())
        {
            addFilesInDir(fsRoot, result, recursive);
        }
        return result;
    }

    private static void addFilesInDir(File dir, Set<File> files, boolean recursive)
    {
        for (File f : dir.listFiles())
        {
            files.add(f);
            if (recursive && f.isDirectory())
            {
                addFilesInDir(f, files, recursive);
            }
        }
    }

    @Override
    public IHDF5Archiver extractFile(String path, OutputStream out) throws IOExceptionUnchecked
    {
        if (hdf5Reader.object().isDataSet(path) == false)
        {
            errorStrategy.dealWithError(new UnarchivingException(path, "not found in archive"));
            return this;
        }
        try
        {
            for (HDF5DataBlock<byte[]> block : hdf5Reader.opaque().getArrayNaturalBlocks(path))
            {
                out.write(block.getData());
            }
        } catch (IOException ex)
        {
            errorStrategy.dealWithError(new UnarchivingException(new File("stdout"), ex));
        }
        return this;
    }

    @Override
    public byte[] extractFileAsByteArray(String path) throws IOExceptionUnchecked
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        extractFile(path, out);
        return out.toByteArray();
    }

    @Override
    public IInputStream extractFileAsIInputStream(String path)
    {
        if (hdf5Reader.object().isDataSet(path) == false)
        {
            errorStrategy.dealWithError(new UnarchivingException(path, "not found in archive"));
            return null;
        }
        return HDF5IOAdapterFactory.asIInputStream(hdf5Reader, path);
    }

    @Override
    public InputStream extractFileAsInputStream(String path)
    {
        return new AdapterIInputStreamToInputStream(extractFileAsIInputStream(path));
    }

    @Override
    public IHDF5Archiver extractToFilesystem(File rootDirectory) throws IllegalStateException
    {
        return extractToFilesystemBelowDirectory(rootDirectory, "", "/", ArchivingStrategy.DEFAULT,
                null);
    }

    @Override
    public IHDF5Archiver extractToFilesystem(File rootDirectory, String path)
            throws IllegalStateException
    {
        return extractToFilesystemBelowDirectory(rootDirectory, "", path,
                ArchivingStrategy.DEFAULT, null);
    }

    @Override
    public IHDF5Archiver extractToFilesystem(File rootDirectory, String path,
            IArchiveEntryVisitor visitorOrNull) throws IllegalStateException
    {
        return extractToFilesystemBelowDirectory(rootDirectory, "", path,
                ArchivingStrategy.DEFAULT, visitorOrNull);
    }

    @Override
    public IHDF5Archiver extractToFilesystem(File rootDirectory, String path,
            ArchivingStrategy strategy, IArchiveEntryVisitor visitorOrNull)
            throws IllegalStateException
    {
        return extractToFilesystemBelowDirectory(rootDirectory, "", path, strategy, visitorOrNull);
    }

    @Override
    public IHDF5Archiver extractToFilesystemBelowDirectory(File rootDirectory,
            String rootPathInArchive)
    {
        return extractToFilesystemBelowDirectory(rootDirectory, rootPathInArchive, "",
                ArchivingStrategy.DEFAULT, null);
    }

    @Override
    public IHDF5Archiver extractToFilesystemBelowDirectory(File rootDirectory,
            String rootPathInArchive, IArchiveEntryVisitor visitorOrNull)
    {
        return extractToFilesystemBelowDirectory(rootDirectory, rootPathInArchive, "",
                ArchivingStrategy.DEFAULT, visitorOrNull);

    }

    @Override
    public IHDF5Archiver extractToFilesystemBelowDirectory(File rootDirectory,
            String rootPathInArchive, ArchivingStrategy strategy, IArchiveEntryVisitor visitorOrNull)
    {
        return extractToFilesystemBelowDirectory(rootDirectory, rootPathInArchive, "", strategy,
                visitorOrNull);
    }

    @Override
    public IHDF5Archiver extractToFilesystemBelowDirectory(File rootDirectory,
            String rootPathInArchive, String path, ArchivingStrategy strategy,
            IArchiveEntryVisitor visitorOrNull) throws IllegalStateException
    {
        final IArchiveEntryProcessor extractor =
                new ArchiveEntryExtractProcessor(visitorOrNull, strategy, rootDirectory,
                        rootPathInArchive, buffer);
        processor.process(Utils.concatLink(rootPathInArchive, path), true, true, false, extractor);
        return this;
    }

    //
    // IHDF5Archiver
    //

    @Override
    public IHDF5Archiver archiveFromFilesystem(File path) throws IllegalStateException
    {
        return archiveFromFilesystem(path, ArchivingStrategy.DEFAULT, false,
                (IArchiveEntryVisitor) null);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(File path, ArchivingStrategy strategy)
            throws IllegalStateException
    {
        return archiveFromFilesystem(path, strategy, false, (IArchiveEntryVisitor) null);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(File path, IArchiveEntryVisitor entryVisitorOrNull)
            throws IllegalStateException
    {
        return archiveFromFilesystem(path, ArchivingStrategy.DEFAULT, false, entryVisitorOrNull);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(File path, ArchivingStrategy strategy,
            boolean keepNameFromPath, IArchiveEntryVisitor entryVisitorOrNull)
            throws IllegalStateException
    {
        checkReadWrite();
        updaterOrNull
                .archive(path, strategy, CHUNK_SIZE_AUTO, keepNameFromPath, entryVisitorOrNull);
        return this;
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(File parentDirToStrip, File path)
            throws IllegalStateException
    {
        return archiveFromFilesystem(parentDirToStrip, path, ArchivingStrategy.DEFAULT);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(File parentDirToStrip, File path,
            ArchivingStrategy strategy) throws IllegalStateException
    {
        return archiveFromFilesystem(parentDirToStrip, path, strategy, null);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(File parentDirToStrip, File path,
            ArchivingStrategy strategy, IArchiveEntryVisitor entryVisitorOrNull)
            throws IllegalStateException
    {
        checkReadWrite();
        updaterOrNull
                .archive(parentDirToStrip, path, strategy, CHUNK_SIZE_AUTO, entryVisitorOrNull);
        return this;
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(String rootInArchive, File path)
    {
        return archiveFromFilesystem(rootInArchive, path, ArchivingStrategy.DEFAULT, null);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(String rootInArchive, File path,
            ArchivingStrategy strategy)
    {
        return archiveFromFilesystem(rootInArchive, path, strategy, null);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystem(String rootInArchive, File path,
            ArchivingStrategy strategy, IArchiveEntryVisitor entryVisitorOrNull)
    {
        checkReadWrite();
        updaterOrNull.archive(rootInArchive, path, strategy, CHUNK_SIZE_AUTO, entryVisitorOrNull);
        return this;
    }

    @Override
    public IHDF5Archiver archiveFromFilesystemBelowDirectory(String rootInArchive, File directory)
    {
        return archiveFromFilesystemBelowDirectory(rootInArchive, directory,
                ArchivingStrategy.DEFAULT, null);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystemBelowDirectory(String rootInArchive, File directory,
            ArchivingStrategy strategy)
    {
        return archiveFromFilesystemBelowDirectory(rootInArchive, directory, strategy, null);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystemBelowDirectory(String rootInArchive, File directory,
            IArchiveEntryVisitor visitor)
    {
        return archiveFromFilesystemBelowDirectory(rootInArchive, directory,
                ArchivingStrategy.DEFAULT, visitor);
    }

    @Override
    public IHDF5Archiver archiveFromFilesystemBelowDirectory(String rootInArchive, File directory,
            ArchivingStrategy strategy, IArchiveEntryVisitor entryVisitorOrNull)
    {
        checkReadWrite();
        updaterOrNull.archiveBelow(rootInArchive, directory, strategy, CHUNK_SIZE_AUTO,
                entryVisitorOrNull);
        return this;
    }

    @Override
    public IHDF5Archiver archiveFile(String path, byte[] data) throws IllegalStateException
    {
        return archiveFile(NewArchiveEntry.file(path), new ByteArrayInputStream(data));
    }

    @Override
    public IHDF5Archiver archiveFile(String path, InputStream input)
    {
        return archiveFile(NewArchiveEntry.file(path), input);
    }

    @Override
    public IHDF5Archiver archiveFile(NewFileArchiveEntry entry, byte[] data)
    {
        return archiveFile(entry, new ByteArrayInputStream(data));
    }

    @Override
    public OutputStream archiveFileAsOutputStream(NewFileArchiveEntry entry)
    {
        return new AdapterIOutputStreamToOutputStream(archiveFileAsIOutputStream(entry));
    }

    @Override
    public IOutputStream archiveFileAsIOutputStream(NewFileArchiveEntry entry)
    {
        checkReadWrite();
        final LinkRecord link = new LinkRecord(entry);
        final IOutputStream stream =
                updaterOrNull.archiveFile(entry.getParentPath(), link, entry.isCompress(),
                        entry.getChunkSize());
        return stream;
    }

    @Override
    public IHDF5Archiver archiveFile(NewFileArchiveEntry entry, InputStream input)
    {
        checkReadWrite();
        final LinkRecord link = new LinkRecord(entry);
        updaterOrNull.archive(entry.getParentPath(), link, input, entry.isCompress(),
                entry.getChunkSize());
        entry.setCrc32(link.getCrc32());
        return this;
    }

    @Override
    public IHDF5Archiver archiveSymlink(String path, String linkTarget)
    {
        return archiveSymlink(NewArchiveEntry.symlink(path, linkTarget));
    }

    @Override
    public IHDF5Archiver archiveSymlink(NewSymLinkArchiveEntry entry)
    {
        checkReadWrite();
        final LinkRecord link = new LinkRecord(entry);
        updaterOrNull.archive(entry.getParentPath(), link, null, false, CHUNK_SIZE_AUTO);
        return this;
    }

    @Override
    public IHDF5Archiver archiveDirectory(String path)
    {
        return archiveDirectory(NewArchiveEntry.directory(path));
    }

    @Override
    public IHDF5Archiver archiveDirectory(NewDirectoryArchiveEntry entry)
            throws IllegalStateException, IllegalArgumentException
    {
        checkReadWrite();
        final LinkRecord link = new LinkRecord(entry);
        updaterOrNull.archive(entry.getParentPath(), link, null, false, CHUNK_SIZE_AUTO);
        return this;
    }

    @Override
    public IHDF5Archiver delete(String hdf5ObjectPath)
    {
        return delete(Collections.singletonList(hdf5ObjectPath), null);
    }

    @Override
    public IHDF5Archiver delete(List<String> hdf5ObjectPaths)
    {
        return delete(hdf5ObjectPaths, null);
    }

    @Override
    public IHDF5Archiver delete(List<String> hdf5ObjectPaths,
            IArchiveEntryVisitor entryVisitorOrNull)
    {
        checkReadWrite();
        deleterOrNull.delete(hdf5ObjectPaths, entryVisitorOrNull);
        return this;
    }

    private void checkReadWrite()
    {
        if (updaterOrNull == null)
        {
            throw new IllegalStateException("Cannot update archive in read-only mode.");
        }
    }

}
