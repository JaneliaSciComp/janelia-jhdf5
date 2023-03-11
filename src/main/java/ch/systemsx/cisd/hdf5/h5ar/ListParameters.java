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

/**
 * A class that represents parameters for {@link HDF5Archiver#list(String, ListParameters)}.
 * 
 * @author Bernd Rinn
 */
public final class ListParameters
{
    private final boolean recursive;

    private final boolean readLinkTargets;

    private final boolean testArchive;

    private final boolean suppressDirectoryEntries;

    private final boolean includeTopLevelDirectoryEntry;

    private final boolean resolveSymbolicLinks;

    private final boolean followSymbolicLinks;

    /**
     * The default list parameters:
     * <ul>
     * <li>recursive</li>
     * <li>does not test the integrity of the archive</li>
     * <li>does not suppress directory entries</li>
     * <li>exclude the top-level directory</li>
     * <li>reads symbolic link targets</li>
     * <li>does not resolve symbolic links</li>
     * <li>does not follow symbolic links</li>
     * </ul>
     */
    public static final ListParameters DEFAULT = new ListParameters(true, true, false, false,
            false, false, false);

    /**
     * The list parameters for testing the archive integrity:
     * <ul>
     * <li>recursive</li>
     * <li>tests the integrity of the archive</li>
     * <li>does not suppress directory entries</li>
     * <li>includes the top-level directory</li>
     * <li>reads symbolic link targets</li>
     * <li>does not resolve symbolic links</li>
     * <li>does not follow symbolic links</li>
     * </ul>
     */
    public static final ListParameters TEST = new ListParameters(true, true, true, false, true,
            false, false);

    /**
     * A class for constructing a new list parameters object.
     */
    public static final class ListParametersBuilder
    {
        private boolean recursive = true;

        private boolean readLinkTargets = true;

        private boolean testArchive = false;

        private boolean suppressDirectoryEntries = false;

        private boolean includeTopLevelDirectoryEntry = false;

        private boolean resolveSymbolicLinks = false;

        private boolean followSymbolicLinks = false;

        private ListParametersBuilder()
        {
        }

        /**
         * Perform a non-recursive listing, i.e. do not traverse sub-directories.
         */
        public ListParametersBuilder nonRecursive()
        {
            this.recursive = false;
            return this;
        }

        /**
         * If <var>recursive</var> is <code>true</code>, perform a recursive listing, if it is
         * <code>false</code>, perform a non-recursive listing, i.e. do not traverse
         * sub-directories.
         */
        public ListParametersBuilder recursive(@SuppressWarnings("hiding")
        boolean recursive)
        {
            this.recursive = recursive;
            return this;
        }

        /**
         * Do not read the link target of symbolic links.
         */
        public ListParametersBuilder noReadLinkTarget()
        {
            this.readLinkTargets = false;
            return this;
        }

        /**
         * If <var>readLinkTargets</var> is <code>true</code>, then read the link targets of
         * symbolic links, if it is <code>false</code>, do not read the link targets.
         */
        public ListParametersBuilder readLinkTargets(@SuppressWarnings("hiding")
        boolean readLinkTargets)
        {
            this.readLinkTargets = readLinkTargets;
            return this;
        }

        /**
         * Perform an integrity test of the archive, i.e. see whether the index and the content of
         * the archive match with respect to types, sizes and checksums.
         */
        public ListParametersBuilder testArchive()
        {
            this.testArchive = true;
            return this;
        }

        /**
         * If <var>testArchive</var> is <code>true</code>, perform an integrity test of the archive,
         * i.e. see whether the index and the content of the archive match with respect to types,
         * sizes and checksums, if it is <code>false</code>, do not perform an integrity check.
         */
        public ListParametersBuilder testArchive(@SuppressWarnings("hiding")
        boolean testArchive)
        {
            this.testArchive = testArchive;
            return this;
        }

        /**
         * Suppress directory entries from being listed. Only files and links will be listed.
         */
        public ListParametersBuilder suppressDirectoryEntries()
        {
            this.suppressDirectoryEntries = true;
            return this;
        }

        /**
         * If <var>suppressDirectoryEntries</var> is <code>true</code>, suppress directory entries
         * from being listed. Only files and links will be listed, if it is <code>false</code>, list
         * also directories.
         */
        public ListParametersBuilder suppressDirectoryEntries(@SuppressWarnings("hiding")
        boolean suppressDirectoryEntries)
        {
            this.suppressDirectoryEntries = suppressDirectoryEntries;
            return this;
        }

        /**
         * Includes the top-level (or starting) directory into the listing.
         * <p>
         * Note that the root directory "/" will never be listed, so this parameter is only
         * effective when the top-level directory of the listing is <i>not</i> the root directory.
         */
        public ListParametersBuilder includeTopLevelDirectoryEntry()
        {
            this.includeTopLevelDirectoryEntry = true;
            return this;
        }

        /**
         * If <var>includeTopLevelDirectoryEntry</var> is <code>true</code>, includes the top-level
         * directory into the listing, if it is <code>false</code>, exclude the top-level directory
         * from the listing.
         * <p>
         * Note that the root directory "/" will never be listed, so this parameter is only
         * effective when the top-level directory of the listing is <i>not</i> the root directory.
         */
        public ListParametersBuilder includeTopLevelDirectoryEntry(@SuppressWarnings("hiding")
        boolean includeTopLevelDirectoryEntry)
        {
            this.includeTopLevelDirectoryEntry = includeTopLevelDirectoryEntry;
            return this;
        }

        /**
         * Resolve symbolic links to their link targets.
         * <p>
         * This makes symbolic links kind of appear like hard links in the listing. Note, however,
         * that symbolic links to directories being resolved do not lead to the directory being
         * traversed expect if also {@link #followSymbolicLinks()} is given.
         */
        public ListParametersBuilder resolveSymbolicLinks()
        {
            this.resolveSymbolicLinks = true;
            return this;
        }

        /**
         * If <var>resolveSymbolicLinks</var> is <code>true</code>, resolve symbolic links to their
         * link targets, if it is <code>false</code>, do not resolve symbolic links to their link
         * targets.
         * <p>
         * If set to <code>true</code>, this makes symbolic links kind of appear like hard links in
         * the listing. Note, however, that symbolic links to directories being resolved do not lead
         * to the directory being traversed expect if also {@link #followSymbolicLinks()} is given.
         */
        public ListParametersBuilder resolveSymbolicLinks(@SuppressWarnings("hiding")
        boolean resolveSymbolicLinks)
        {
            this.resolveSymbolicLinks = resolveSymbolicLinks;
            return this;
        }

        /**
         * Traverse a directory that was resolved from a symbolic link.
         * <p>
         * Only effective if recursive listing is enabled.
         */
        public ListParametersBuilder followSymbolicLinks()
        {
            this.followSymbolicLinks = true;
            return this;
        }

        /**
         * If <var>followSymbolicLinks</var> is set to <code>true</code>, traverse a directory that
         * was resolved from a symbolic link, if it is <code>false</code>, do not traverse a
         * directory when it was resolved from a symbolic link.
         * <p>
         * Only effective if recursive listing is enabled.
         */
        public ListParametersBuilder followSymbolicLinks(@SuppressWarnings("hiding")
        boolean followSymbolicLinks)
        {
            this.followSymbolicLinks = followSymbolicLinks;
            return this;
        }

        /**
         * Returns the {@link ListParameters} object constructed.
         */
        public ListParameters get()
        {
            return new ListParameters(recursive, readLinkTargets, testArchive,
                    suppressDirectoryEntries, includeTopLevelDirectoryEntry, resolveSymbolicLinks,
                    followSymbolicLinks);
        }
    }

    /**
     * Starts building new list parameters.
     * 
     * @return A new {@link ListParametersBuilder}.
     */
    public static ListParametersBuilder build()
    {
        return new ListParametersBuilder();
    }

    private ListParameters(boolean recursive, boolean readLinkTargets, boolean testArchive,
            boolean suppressDirectoryEntries, boolean includeTopLevelDirectoryEntry,
            boolean resolveSymbolicLinks, boolean followSymbolicLinks)
    {
        this.recursive = recursive;
        this.readLinkTargets = readLinkTargets || resolveSymbolicLinks;
        this.testArchive = testArchive;
        this.suppressDirectoryEntries = suppressDirectoryEntries;
        this.includeTopLevelDirectoryEntry = includeTopLevelDirectoryEntry;
        this.resolveSymbolicLinks = resolveSymbolicLinks;
        this.followSymbolicLinks = followSymbolicLinks;
    }

    /**
     * Returns if recursive listing is enabled, i.e. if the listing will traverse into
     * sub-directories.
     * 
     * @see ListParametersBuilder#recursive(boolean)
     */
    public boolean isRecursive()
    {
        return recursive;
    }

    /**
     * Returns if symbolic link targets should be read.
     * 
     * @see ListParametersBuilder#readLinkTargets(boolean)
     */
    public boolean isReadLinkTargets()
    {
        return readLinkTargets;
    }

    /**
     * Returns if the archive should be tested for integrity.
     * 
     * @see ListParametersBuilder#testArchive(boolean)
     */
    public boolean isTestArchive()
    {
        return testArchive;
    }

    /**
     * Returns if directory entries should be suppressed from being listed.
     * 
     * @see ListParametersBuilder#suppressDirectoryEntries(boolean)
     */
    public boolean isSuppressDirectoryEntries()
    {
        return suppressDirectoryEntries;
    }

    /**
     * Returns if the top-level directory entry should be listed as well.
     * 
     * @see ListParametersBuilder#includeTopLevelDirectoryEntry(boolean)
     */
    public boolean isIncludeTopLevelDirectoryEntry()
    {
        return includeTopLevelDirectoryEntry;
    }

    /**
     * Returns if symbolic links should be resolved.
     * 
     * @see ListParametersBuilder#resolveSymbolicLinks(boolean)
     */
    public boolean isResolveSymbolicLinks()
    {
        return resolveSymbolicLinks;
    }

    /**
     * Returns if directories resolved from symbolic links should be traversed.
     * 
     * @see ListParametersBuilder#followSymbolicLinks(boolean)
     */
    public boolean isFollowSymbolicLinks()
    {
        return followSymbolicLinks;
    }
}
