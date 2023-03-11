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

import java.util.List;

/**
 * An info provider for HDF5 archives.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5ArchiveInfoProvider
{
    //
    // Information about individual entries
    //

    /**
     * Returns <code>true</code>, if an entry <var>path</var> exists in the archive.
     * 
     * @param path The path to obtain information for.
     */
    public boolean exists(String path);

    /**
     * Returns <code>true</code>, if a directory entry <var>path</var> exists in the archive.
     * 
     * @param path The path to obtain information for.
     */
    public boolean isDirectory(String path);

    /**
     * Returns <code>true</code>, if a regular file entry <var>path</var> exists in the archive.
     * 
     * @param path The path to obtain information for.
     */
    public boolean isRegularFile(String path);

    /**
     * Returns <code>true</code>, if a symbolic link entry <var>path</var> exists in the archive.
     * 
     * @param path The path to obtain information for.
     */
    public boolean isSymLink(String path);

    /**
     * Returns an archive entry for <var>path</var>, or <code>null</code>, if the archive has no
     * archive entry for this <var>path</var>.
     * 
     * @param path The path to obtain information for.
     * @param readLinkTarget If <code>true</code> and if the entry is a symbolic link entry, read
     *            the link target.
     */
    public ArchiveEntry tryGetEntry(String path, boolean readLinkTarget);

    /**
     * Resolves the symbolic link of <var>entry</var>, if any.
     * 
     * @param entry The archive entry to resolve.
     * @return The resolved link, if <var>entry</var> is a symbolic link that links to an existing
     *         file or directory target, <code>null</code> if <var>entry</var> is a symbolic link
     *         that links to a non-existing target, or <var>entry</var>, if this is not a link.
     */
    public ArchiveEntry tryResolveLink(ArchiveEntry entry);

    /**
     * Returns the archive entry for <var>path</var>. If <var>path</var> is a symbolic link, the
     * entry will be resolved to the real file or directory in the archive, or <code>null</code>, if
     * the link target doesn't exist.
     * 
     * @param path The path in the archive to get the entry for.
     * @param keepPath If <code>true</code>, the resolved entry will keep the <var>path</var>, i.e.
     *            the returned entry of a symlink will look like a hard link. If <code>false</code>,
     *            the returned entry will be the entry of the resolved path.
     * @return The resolved link, if <var>path</var> denotes a file, directory, or symbolic link
     *         that links to an existing file or directory target, <code>null</code> if
     *         <var>path</var> denotes a symbolic link that links to a non-existing target.
     */
    public ArchiveEntry tryGetResolvedEntry(String path, boolean keepPath);

    //
    // Listing
    //

    /**
     * Returns the list of all entries in the archive recursively.
     * 
     * @return The list of archive entries.
     */
    public List<ArchiveEntry> list();

    /**
     * Returns the list of all entries below <var>fileOrDir</var> in the archive recursively.
     * 
     * @param fileOrDir The file to list or the directory to list the entries from recursively.
     * @return The list of archive entries.
     */
    public List<ArchiveEntry> list(String fileOrDir);

    /**
     * Returns the list of entries below <var>fileOrDir</var> in the archive.
     * 
     * @param fileOrDir The file to list or the directory to list the entries from.
     * @param params the parameters to modify the listing behavior.
     * @return The list of archive entries.
     */
    public List<ArchiveEntry> list(String fileOrDir, ListParameters params);

    /**
     * Returns the list of all entries below <var>fileOrDir</var> in the archive recursively.
     * 
     * @param fileOrDir The file to list or the directory to list the entries from recursively.
     * @param visitor The archive entry visitor to call for each entry.
     * @return This archive info provider.
     */
    public IHDF5ArchiveInfoProvider list(String fileOrDir, IArchiveEntryVisitor visitor);

    /**
     * Returns the list of entries below <var>fileOrDir</var> in the archive.
     * 
     * @param fileOrDir The file to list or the directory to list the entries from.
     * @param visitor The archive entry visitor to call for each entry.
     * @param params the parameters to modify the listing behavior.
     * @return This archive info provider.
     */
    public IHDF5ArchiveInfoProvider list(String fileOrDir, IArchiveEntryVisitor visitor,
            ListParameters params);

    /**
     * Performs an integrity of the archive.
     * 
     * @return All entries which failed the integrity check.
     */
    public List<ArchiveEntry> test();

}