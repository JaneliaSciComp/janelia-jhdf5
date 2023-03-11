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
import java.io.OutputStream;
import java.util.List;

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.base.io.IOutputStream;
import ch.systemsx.cisd.hdf5.h5ar.NewArchiveEntry.NewDirectoryArchiveEntry;
import ch.systemsx.cisd.hdf5.h5ar.NewArchiveEntry.NewFileArchiveEntry;
import ch.systemsx.cisd.hdf5.h5ar.NewArchiveEntry.NewSymLinkArchiveEntry;

/**
 * An interface for the HDF5 archiver.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5Archiver extends IHDF5ArchiveReader
{

    /**
     * Flush the underlying HDF5 writer.
     */
    public void flush() throws IOException;

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param path The file or directory to archive. Everything below this path is archived.
     */
    public IHDF5Archiver archiveFromFilesystem(File path) throws IllegalStateException;

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param path The file or directory to archive. Everything below this path is archived.
     * @param strategy The archiving strategy to use. This strategy object determines which files to
     *            include and to exclude and which files to compress.
     */
    public IHDF5Archiver archiveFromFilesystem(File path, ArchivingStrategy strategy);

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param path The file or directory to archive. Everything below this path is archived.
     * @param visitor The {@link IArchiveEntryVisitor} to use. Can be <code>null</code>.
     */
    public IHDF5Archiver archiveFromFilesystem(File path, IArchiveEntryVisitor visitor);

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param path The file or directory to archive. Everything below this path is archived. The
     *            name part of <var>path</var> may be kept, depending on the value of
     *            <var>keepNameFromPath</var>.
     * @param strategy The archiving strategy to use. This strategy object determines which files to
     *            include and to exclude and which files to compress.
     * @param keepNameFromPath If <code>true</code>, the name part of <var>path</var> is kept in the
     *            archive. Otherwise, <var>path</var> will represent "/" in the archive.
     * @param visitor The {@link IArchiveEntryVisitor} to use. Can be <code>null</code>.
     */
    public IHDF5Archiver archiveFromFilesystem(File path, ArchivingStrategy strategy,
            boolean keepNameFromPath, IArchiveEntryVisitor visitor);

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param parentDirToStrip The parent directory of <var>path</var> on the filesystem which
     *            should be stripped in the archive. It is an error, if <var>parentDirToStrip</var>
     *            is not a parent directory of <var>path</var>. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and
     *            <code>parentDirToStrip=/home/joe/work</code>, then <code>c</code> will end up in
     *            the archive at the path <code>a/b</code>.
     * @param path The file or directory to archive.
     */
    public IHDF5Archiver archiveFromFilesystem(File parentDirToStrip, File path);

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param parentDirToStrip The parent directory of <var>path</var> on the filesystem which
     *            should be stripped in the archive. It is an error, if <var>parentDirToStrip</var>
     *            is not a parent directory of <var>path</var>. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and
     *            <code>parentDirToStrip=/home/joe/work</code>, then <code>c</code> will end up in
     *            the archive at the path <code>a/b</code>.
     * @param path The file or directory to archive.
     * @param strategy The archiving strategy to use. This strategy object determines which files to
     *            include and to exclude and which files to compress.
     */
    public IHDF5Archiver archiveFromFilesystem(File parentDirToStrip, File path,
            ArchivingStrategy strategy);

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param parentDirToStrip The parent directory of <var>path</var> on the filesystem which
     *            should be stripped in the archive. It is an error, if <var>parentDirToStrip</var>
     *            is not a parent directory of <var>path</var>. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and
     *            <code>parentDirToStrip=/home/joe/work</code>, then <code>c</code> will end up in
     *            the archive at the path <code>/a/b/c</code>.
     * @param path The file or directory to archive.
     * @param strategy The archiving strategy to use. This strategy object determines which files to
     *            include and to exclude and which files to compress.
     * @param visitor The {@link IArchiveEntryVisitor} to use. Can be <code>null</code>.
     */
    public IHDF5Archiver archiveFromFilesystem(File parentDirToStrip, File path,
            ArchivingStrategy strategy, IArchiveEntryVisitor visitor);

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param rootInArchive The root directory of <var>path</var> in the archive. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and <code>rootInArchive=/t</code>, then
     *            <code>c</code> will end up in the archive at the path <code>/t/c</code>. If
     *            <var>rootInArchive</var> is the last part of the parent directory of
     *            <var>path</var> on the filesystem, then its metadata will be taken from the
     *            filesystem.
     * @param path The file or directory to archive.
     */
    public IHDF5Archiver archiveFromFilesystem(String rootInArchive, File path);

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param rootInArchive The root directory of <var>path</var> in the archive. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and <code>rootInArchive=/t</code>, then
     *            <code>c</code> will end up in the archive at the path <code>/t/c</code>. If
     *            <var>rootInArchive</var> is the last part of the parent directory of
     *            <var>path</var> on the filesystem, then its metadata will be taken from the
     *            filesystem.
     * @param path The file or directory to archive.
     * @param strategy The archiving strategy to use. This strategy object determines which files to
     *            include and to exclude and which files to compress.
     */
    public IHDF5Archiver archiveFromFilesystem(String rootInArchive, File path,
            ArchivingStrategy strategy);

    /**
     * Archive the <var>path</var> from the filesystem.
     * 
     * @param rootInArchive The root directory of <var>path</var> in the archive. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and <code>rootInArchive=/t</code>, then
     *            <code>c</code> will end up in the archive at the path <code>/t/c</code>. If
     *            <var>rootInArchive</var> is the last part of the parent directory of
     *            <var>path</var> on the filesystem, then its metadata will be taken from the
     *            filesystem.
     * @param path The file or directory to archive.
     * @param strategy The archiving strategy to use. This strategy object determines which files to
     *            include and to exclude and which files to compress.
     * @param visitor The {@link IArchiveEntryVisitor} to use. Can be <code>null</code>.
     */
    public IHDF5Archiver archiveFromFilesystem(String rootInArchive, File path,
            ArchivingStrategy strategy, IArchiveEntryVisitor visitor);

    /**
     * Archive the content below <var>directory</var> from the filesystem.
     * 
     * @param rootInArchive The root directory of <var>path</var> in the archive. Example: If
     *            <code>directory=/home/joe/work/a/b</code>, directory <code>b</code> has two files
     *            <code>c</code> and <code>d</code>, and <code>rootInArchive=/t</code>, then the
     *            archive will have <code>c</code> at path <code>/t/c</code> and <code>d</code> at
     *            path <code>/t/d</code>.
     * @param directory The directory to archive the content of. It is an error if this is not a
     *            directory on the filesystem.
     */
    public IHDF5Archiver archiveFromFilesystemBelowDirectory(String rootInArchive, File directory);

    /**
     * Archive the content below <var>directory</var> from the filesystem.
     * 
     * @param rootInArchive The root directory of <var>path</var> in the archive. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and <code>rootInArchive=/t</code>, then
     *            <code>c</code> will end up in the archive at the path <code>/t/c</code>. If
     *            <var>rootInArchive</var> is the last part of the parent directory of
     *            <var>path</var> on the filesystem, then its metadata will be taken from the
     *            filesystem.
     * @param directory The directory to archive the content of. It is an error if this is not an
     *            existing directory.
     * @param strategy The archiving strategy to use. This strategy object determines which files to
     *            include and to exclude and which files to compress.
     */
    public IHDF5Archiver archiveFromFilesystemBelowDirectory(String rootInArchive, File directory,
            ArchivingStrategy strategy);

    /**
     * Archive the content below <var>directory</var> from the filesystem.
     * 
     * @param rootInArchive The root directory of <var>path</var> in the archive. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and <code>rootInArchive=/t</code>, then
     *            <code>c</code> will end up in the archive at the path <code>/t/c</code>. If
     *            <var>rootInArchive</var> is the last part of the parent directory of
     *            <var>path</var> on the filesystem, then its metadata will be taken from the
     *            filesystem.
     * @param directory The directory to archive the content of. It is an error if this is not an
     *            existing directory.
     * @param visitor The {@link IArchiveEntryVisitor} to use. Can be <code>null</code>.
     */
    public IHDF5Archiver archiveFromFilesystemBelowDirectory(String rootInArchive, File directory,
            IArchiveEntryVisitor visitor);

    /**
     * Archive the content below <var>directory</var> from the filesystem.
     * 
     * @param rootInArchive The root directory of <var>path</var> in the archive. Example: If
     *            <code>path=/home/joe/work/a/b/c</code> and <code>rootInArchive=/t</code>, then
     *            <code>c</code> will end up in the archive at the path <code>/t/c</code>. If
     *            <var>rootInArchive</var> is the last part of the parent directory of
     *            <var>path</var> on the filesystem, then its metadata will be taken from the
     *            filesystem.
     * @param directory The directory to archive the content of. It is an error if this is not an
     *            existing directory.
     * @param strategy The archiving strategy to use. This strategy object determines which files to
     *            include and to exclude and which files to compress.
     * @param visitor The {@link IArchiveEntryVisitor} to use. Can be <code>null</code>.
     */
    public IHDF5Archiver archiveFromFilesystemBelowDirectory(String rootInArchive, File directory,
            ArchivingStrategy strategy, IArchiveEntryVisitor visitor);

    /**
     * Archive the <var>data</var> as file in the archive.
     * 
     * @param path The path to archive the data at.
     * @param data The bytes to archive as file content.
     */
    public IHDF5Archiver archiveFile(String path, byte[] data);

    /**
     * Archive the <var>input</var> as file in the archive.
     * 
     * @param path The path to archive the data at.
     * @param input The input stream to get the file content from.
     */
    public IHDF5Archiver archiveFile(String path, InputStream input);

    /**
     * Archive the <var>input</var> as file in the archive.
     * 
     * @param entry The archive entry (defining the path) to archive the data at.
     * @param input The input stream to get the file content from.
     */
    public IHDF5Archiver archiveFile(NewFileArchiveEntry entry, InputStream input);

    /**
     * Archive the <var>data</var> as file in the archive.
     * 
     * @param entry The archive entry (defining the path) to archive the data at.
     * @param data The bytes to archive as file content.
     */
    public IHDF5Archiver archiveFile(NewFileArchiveEntry entry, byte[] data);

    /**
     * Return an {@link IOutputStream} that can be used to write the content of a file into the
     * archive.
     * 
     * @param entry The archive entry (defining the path) to archive the data at.
     * @return The output stream that the file content is written to.
     */
    public IOutputStream archiveFileAsIOutputStream(NewFileArchiveEntry entry);

    /**
     * Return an {@link OutputStream} that can be used to write the content of a file into the
     * archive.
     * 
     * @param entry The archive entry (defining the path) to archive the data at.
     * @return The output stream that the file content is written to.
     */
    public OutputStream archiveFileAsOutputStream(NewFileArchiveEntry entry);

    /**
     * Add a new symbolic link to the archive.
     * 
     * @param path The path where the symbolic link resides.
     * @param linkTarget The target where the symbolic link points to.
     */
    public IHDF5Archiver archiveSymlink(String path, String linkTarget);

    /**
     * Add a new symbolic link to the archive.
     * 
     * @param entry The archive entry describing the symbolic link.
     */
    public IHDF5Archiver archiveSymlink(NewSymLinkArchiveEntry entry);

    /**
     * Add a new directory to the archive.
     * 
     * @param path The path in the archive where the directory resides.
     */
    public IHDF5Archiver archiveDirectory(String path);

    /**
     * Add a new directory to the archive.
     * 
     * @param entry The archive entry describing the directory.
     */
    public IHDF5Archiver archiveDirectory(NewDirectoryArchiveEntry entry);

    /**
     * Deletes a <var>path</var> from the archive.
     * 
     * @param path The path to delete.
     */
    public IHDF5Archiver delete(String path);

    /**
     * Deletes a list of <var>paths</var> from the archive.
     * 
     * @param paths The paths to delete.
     */
    public IHDF5Archiver delete(List<String> paths);

    /**
     * Deletes a list of <var>paths</var> from the archive.
     * 
     * @param paths The paths to delete.
     * @param entryVisitorOrNull The visitor for each archive entry which is actually deleted. If no
     *            errors occur, the visitor will be called once for each path in the list of
     *            <var>paths</var>.
     */
    public IHDF5Archiver delete(List<String> paths, IArchiveEntryVisitor entryVisitorOrNull);

    // Method overridden from IHDF5ArchiveReader, see there for javadoc.

    @Override
    public IHDF5Archiver list(String fileOrDir, IArchiveEntryVisitor visitor);

    @Override
    public IHDF5Archiver list(String fileOrDir, IArchiveEntryVisitor visitor, ListParameters params);

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, IArchiveEntryVisitor visitor,
            IArchiveEntryVisitor missingArchiveEntryVisitor, VerifyParameters params);

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, IArchiveEntryVisitor visitor, VerifyParameters params);

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, IArchiveEntryVisitor missingArchiveEntryVisitor,
            VerifyParameters params);

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, VerifyParameters params);

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, IArchiveEntryVisitor missingArchiveEntryVisitor);

    @Override
    public IHDF5Archiver verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor);

    @Override
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, VerifyParameters params);

    @Override
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive);

    @Override
    public IHDF5Archiver extractFile(String path, OutputStream out) throws IOExceptionUnchecked;

    @Override
    public IHDF5Archiver extractToFilesystem(File rootDirectory);

    @Override
    public IHDF5Archiver extractToFilesystem(File rootDirectory, String path)
            throws IllegalStateException;

    @Override
    public IHDF5Archiver extractToFilesystem(File rootDirectory, String path,
            IArchiveEntryVisitor visitor) throws IllegalStateException;

    @Override
    public IHDF5Archiver extractToFilesystem(File rootDirectory, String path,
            ArchivingStrategy strategy, IArchiveEntryVisitor visitor) throws IllegalStateException;

    @Override
    public IHDF5Archiver extractToFilesystemBelowDirectory(File rootDirectory,
            String rootPathInArchive);
    
    @Override
    public IHDF5Archiver extractToFilesystemBelowDirectory(File rootDirectory,
            String rootPathInArchive, IArchiveEntryVisitor visitorOrNull);

    @Override
    public IHDF5Archiver extractToFilesystemBelowDirectory(File rootDirectory,
            String rootPathInArchive, ArchivingStrategy strategy, IArchiveEntryVisitor visitorOrNull);

    @Override
    public IHDF5Archiver extractToFilesystemBelowDirectory(File rootDirectory, String rootPathInArchive,
            String path, ArchivingStrategy strategy, IArchiveEntryVisitor visitorOrNull)
            throws IllegalStateException;
}