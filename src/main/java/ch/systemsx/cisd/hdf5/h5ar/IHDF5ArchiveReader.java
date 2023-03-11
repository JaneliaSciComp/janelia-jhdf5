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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import ch.systemsx.cisd.base.io.IInputStream;

/**
 * An interface for an HDF5 archive reader.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5ArchiveReader extends IHDF5ArchiveInfoProvider
{

    /**
     * Closes this object and the file referenced by this object. This object must not be used after
     * being closed. Calling this method for a second time is a no-op.
     */
    public void close();

    /**
     * Returns <code>true</code> if this archive reader has been already closed.
     */
    public boolean isClosed();

    //
    // Verification
    //

    /**
     * Verifies the content of the archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param rootDirectoryInArchive The root directory in the archive to start verify from. It will
     *            be stripped from each entry before <var>rootDirectoryOnFS</var> is added.
     * @param visitor The entry visitor to call for each entry. Call {@link ArchiveEntry#isOK()} to
     *            check whether verification was successful.
     * @param missingArchiveEntryVisitor The entry visitor to call for each file that exists on the
     *            filesystem, but is missing in the archive.
     * @param params The parameters to determine behavior of the verification process.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, IArchiveEntryVisitor visitor,
            IArchiveEntryVisitor missingArchiveEntryVisitor, VerifyParameters params);

    /**
     * Verifies the content of the archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param rootDirectoryInArchive The root directory in the archive to start verify from. It will
     *            be stripped from each entry before <var>rootDirectoryOnFS</var> is added.
     * @param visitor The entry visitor to call for each entry. Call {@link ArchiveEntry#isOK()} to
     *            check whether verification was successful.
     * @param params The parameters to determine behavior of the verification process.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, IArchiveEntryVisitor visitor, VerifyParameters params);

    /**
     * Verifies the content of the complete archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param visitor The entry visitor to call for each entry. Call {@link ArchiveEntry#isOK()} to
     *            check whether verification was successful.
     * @param missingArchiveEntryVisitor The entry visitor to call for each file that exists on the
     *            filesystem, but is missing in the archive.
     * @param params The parameters to determine behavior of the verification process.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, IArchiveEntryVisitor missingArchiveEntryVisitor,
            VerifyParameters params);

    /**
     * Verifies the content of the complete archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param visitor The entry visitor to call for each entry. Call {@link ArchiveEntry#isOK()} to
     *            check whether verification was successful.
     * @param params The parameters to determine behavior of the verification process.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, VerifyParameters params);

    /**
     * Verifies the content of the complete archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param visitor The entry visitor to call for each entry. Call {@link ArchiveEntry#isOK()} to
     *            check whether verification was successful.
     * @param missingArchiveEntryVisitor The entry visitor to call for each file that exists on the
     *            filesystem, but is missing in the archive.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor, IArchiveEntryVisitor missingArchiveEntryVisitor);

    /**
     * Verifies the content of the complete archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param visitor The entry visitor to call for each entry. Call {@link ArchiveEntry#isOK()} to
     *            check whether verification was successful.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            IArchiveEntryVisitor visitor);

    /**
     * Verifies the content of the archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param rootDirectoryInArchive The root directory in the archive to start verify from. It will
     *            be stripped from each entry before <var>rootDirectoryOnFS</var> is added.
     * @param params The parameters to determine behavior of the verification process.
     * @return The list of archive entries which failed verification.
     */
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive, VerifyParameters params);

    /**
     * Verifies the content of the archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param params The parameters to determine behavior of the verification process.
     * @return The list of archive entries which failed verification.
     */
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            VerifyParameters params);

    /**
     * Verifies the content of the archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @return The list of archive entries which failed verification.
     */
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS);

    /**
     * Verifies the content of the complete archive against the filesystem.
     * 
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @return The list of archive entries which failed verification.
     */
    public List<ArchiveEntry> verifyAgainstFilesystem(File rootDirectoryOnFS);

    /**
     * Verifies the content of the complete archive against the filesystem.
     * 
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param params The parameters to determine behavior of the verification process.
     * @return The list of archive entries which failed verification.
     */
    public List<ArchiveEntry> verifyAgainstFilesystem(File rootDirectoryOnFS, VerifyParameters params);

    /**
     * Verifies the content of the archive against the filesystem.
     * 
     * @param fileOrDir The file or directory entry in the archive to verify. May be empty, in which
     *            case all entries below <var>rootDirectoryInArchive</var> are verified.
     * @param rootDirectoryOnFS The root directory on the file system that should be added to each
     *            entry in the archive when comparing.
     * @param rootDirectoryInArchive The root directory in the archive to start verify from. It will
     *            be stripped from each entry before <var>rootDirectoryOnFS</var> is added.
     */
    public List<ArchiveEntry> verifyAgainstFilesystem(String fileOrDir, File rootDirectoryOnFS,
            String rootDirectoryInArchive);

    //
    // Extraction
    //

    /**
     * Extract the content of a file in the archive to an {@link OutputStream}.
     * 
     * @param path The path of the file to extract the content of.
     * @param out The output stream to extract the content to.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractFile(String path, OutputStream out);

    /**
     * Extract the content of a file in the archive to a byte array.
     * 
     * @param path The path of the file to extract the content of.
     * @return The byte array representing the content of the file.
     */
    public byte[] extractFileAsByteArray(String path);

    /**
     * Extract the content of a file in the archive as an {@link IInputStream}.
     * 
     * @param path The path of the file to extract the content of.
     * @return The input stream interface. If an error occurs and the
     *         {@link ch.systemsx.cisd.base.exceptions.IErrorStrategy} of the archive reader does
     *         not re-throw the exception, the return value will be <code>null</code> on errors.
     */
    public IInputStream extractFileAsIInputStream(String path);

    /**
     * Extract the content of a file in the archive as an {@link InputStream}.
     * 
     * @param path The path of the file to extract the content of.
     * @return The input stream. If an error occurs and the
     *         {@link ch.systemsx.cisd.base.exceptions.IErrorStrategy} of the archive reader does
     *         not re-throw the exception, the return value will be <code>null</code> on errors.
     */
    public InputStream extractFileAsInputStream(String path);

    /**
     * Extracts the complete archive to the file system.
     * 
     * @param rootDirectory The directory in the file system to use as root directory for the
     *            extracted archive path.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractToFilesystem(File rootDirectory);

    /**
     * Extracts a path from the archive to the file system.
     * 
     * @param rootDirectory The directory in the file system to use as root directory for the
     *            extracted archive path.
     * @param path The path in the archive to extract. This path will be kept unchanged when
     *            extracted.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractToFilesystem(File rootDirectory, String path);

    /**
     * Extracts a path from the archive to the file system.
     * 
     * @param rootDirectory The directory in the file system to use as root directory for the
     *            extracted archive path.
     * @param path The path in the archive to extract. This path will be kept unchanged when
     *            extracted.
     * @param visitorOrNull The entry visitor to call for each entry. Call
     *            {@link ArchiveEntry#isOK()} to check whether verification was successful. May be
     *            <code>null</code>.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractToFilesystem(File rootDirectory, String path,
            IArchiveEntryVisitor visitorOrNull);

    /**
     * Extracts a path from the archive to the file system.
     * 
     * @param rootDirectory The directory in the file system to use as root directory for the
     *            extracted archive path.
     * @param path The path in the archive to extract. This path will be kept unchanged when
     *            extracted.
     * @param strategy The strategy to determine which files and directories to extract and which
     *            ones to suppress.
     * @param visitorOrNull The entry visitor to call for each entry. Call
     *            {@link ArchiveEntry#isOK()} to check whether verification was successful. May be
     *            <code>null</code>.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractToFilesystem(File rootDirectory, String path,
            ArchivingStrategy strategy, IArchiveEntryVisitor visitorOrNull);

    /**
     * Extracts all paths below a given directory path from the archive to the file system.
     * 
     * @param rootDirectory The directory in the file system to use as root directory for the
     *            extracted archive path.
     * @param rootInArchive The root path in the archive to extract. This path will be stripped when
     *            extracted.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractToFilesystemBelowDirectory(File rootDirectory,
            String rootInArchive);

    /**
     * Extracts all paths below a given directory path from the archive to the file system.
     * 
     * @param rootDirectory The directory in the file system to use as root directory for the
     *            extracted archive path.
     * @param rootInArchive The root path in the archive to extract. This path will be stripped when
     *            extracted.
     * @param visitorOrNull The entry visitor to call for each entry. Call
     *            {@link ArchiveEntry#isOK()} to check whether verification was successful. May be
     *            <code>null</code>.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractToFilesystemBelowDirectory(File rootDirectory,
            String rootInArchive, IArchiveEntryVisitor visitorOrNull);

    /**
     * Extracts all paths below a given directory path from the archive to the file system.
     * 
     * @param rootDirectory The directory in the file system to use as root directory for the
     *            extracted archive path.
     * @param rootInArchive The root path in the archive to extract. This path will be stripped when
     *            extracted.
     * @param strategy The strategy to determine which files and directories to extract and which
     *            ones to suppress.
     * @param visitorOrNull The entry visitor to call for each entry. Call
     *            {@link ArchiveEntry#isOK()} to check whether verification was successful. May be
     *            <code>null</code>.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractToFilesystemBelowDirectory(File rootDirectory,
            String rootInArchive, ArchivingStrategy strategy, IArchiveEntryVisitor visitorOrNull);

    /**
     * Extracts a path from the archive below a given directory path to the file system.
     * 
     * @param rootDirectory The directory in the file system to use as root directory for the
     *            extracted archive path.
     * @param rootInArchive The root path in the archive to extract. This path will be stripped when
     *            extracted.
     * @param path The path in the archive to extract, relative to <var>rootPathInArchive</var>.
     *            This path will be kept unchanged when extracted.
     * @param strategy The strategy to determine which files and directories to extract and which
     *            ones to suppress.
     * @param visitorOrNull The entry visitor to call for each entry. Call
     *            {@link ArchiveEntry#isOK()} to check whether verification was successful. May be
     *            <code>null</code>.
     * @return This archive reader.
     */
    public IHDF5ArchiveReader extractToFilesystemBelowDirectory(File rootDirectory,
            String rootInArchive, String path, ArchivingStrategy strategy,
            IArchiveEntryVisitor visitorOrNull);
}