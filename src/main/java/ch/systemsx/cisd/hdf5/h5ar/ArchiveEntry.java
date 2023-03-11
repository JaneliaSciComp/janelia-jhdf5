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

import ch.systemsx.cisd.base.unix.FileLinkType;
import ch.systemsx.cisd.hdf5.h5ar.HDF5ArchiveUpdater.DataSetInfo;

/**
 * An entry of an archive listing.
 * 
 * @author Bernd Rinn
 */
public final class ArchiveEntry
{
    private final String path;

    private final String parentPath;

    private final String name;

    private final String realPath;

    private final String realParentPath;

    private final String realName;

    private final ArchiveEntryCompleteness completeness;

    private final boolean hasLinkTarget;

    private final String linkTarget;

    private final FileLinkType linkType;

    private final FileLinkType verifiedLinkType;

    private final long size;

    private long verifiedSize;

    private final long lastModified;

    private final long verifiedLastModified;

    private int crc32;

    private boolean knowsChecksum;

    private int verifiedCrc32;

    private final int uid;

    private final int gid;

    private final IdCache idCache;

    private final short permissions;

    private final String errorLineOrNull;

    ArchiveEntry(String dir, String path, LinkRecord link, IdCache idCache)
    {
        this(dir, path, link, idCache, null);
    }

    ArchiveEntry(String dir, String path, LinkRecord link, IdCache idCache, String errorLineOrNull)
    {
        this.parentPath = (dir != null) ? dir : Utils.getParentPath(path);
        this.realParentPath = parentPath;
        this.path = path;
        this.realPath = path;
        this.name = link.getLinkName();
        this.realName = name;
        this.idCache = idCache;
        this.completeness = link.getCompleteness();
        this.hasLinkTarget = (link.tryGetLinkTarget() != null);
        this.linkTarget = hasLinkTarget ? link.tryGetLinkTarget() : "?";
        this.linkType = link.getLinkType();
        this.verifiedLinkType = link.getVerifiedType();
        this.size = link.getSize();
        this.verifiedSize = link.getVerifiedSize();
        this.lastModified = link.getLastModified();
        this.verifiedLastModified = link.getVerifiedLastModified();
        this.crc32 = link.getCrc32();
        this.knowsChecksum = link.hasCRC32Checksum();
        this.verifiedCrc32 = link.getVerifiedCrc32();
        this.uid = link.getUid();
        this.gid = link.getGid();
        this.permissions = link.getPermissions();
        this.errorLineOrNull = errorLineOrNull;
    }

    ArchiveEntry(ArchiveEntry pathInfo, ArchiveEntry linkInfo)
    {
        this.parentPath = pathInfo.parentPath;
        this.path = pathInfo.path;
        this.name = pathInfo.name;
        this.realParentPath = linkInfo.parentPath;
        this.realPath = linkInfo.realPath;
        this.realName = linkInfo.name;
        this.idCache = pathInfo.idCache;
        this.completeness = linkInfo.completeness;
        this.hasLinkTarget = linkInfo.hasLinkTarget;
        this.linkTarget = linkInfo.linkTarget;
        this.linkType = linkInfo.linkType;
        this.verifiedLinkType = linkInfo.verifiedLinkType;
        this.size = linkInfo.size;
        this.verifiedSize = linkInfo.verifiedSize;
        this.lastModified = Math.max(pathInfo.lastModified, linkInfo.lastModified);
        this.verifiedLastModified =
                Math.max(pathInfo.verifiedLastModified, linkInfo.verifiedLastModified);
        this.crc32 = linkInfo.crc32;
        this.knowsChecksum = linkInfo.knowsChecksum;
        this.verifiedCrc32 = linkInfo.verifiedCrc32;
        this.uid = linkInfo.uid;
        this.gid = linkInfo.gid;
        this.permissions = linkInfo.permissions;
        this.errorLineOrNull = null;
    }

    ArchiveEntry(String errorLineOrNull)
    {
        this.errorLineOrNull = errorLineOrNull;
        this.path = null;
        this.parentPath = null;
        this.name = null;
        this.realPath = null;
        this.realParentPath = null;
        this.realName = null;
        this.idCache = null;
        this.completeness = null;
        this.linkTarget = null;
        this.hasLinkTarget = false;
        this.linkType = null;
        this.verifiedLinkType = null;
        this.size = Utils.UNKNOWN;
        this.verifiedSize = Utils.UNKNOWN;
        this.lastModified = Utils.UNKNOWN;
        this.verifiedLastModified = Utils.UNKNOWN;
        this.crc32 = 0;
        this.verifiedCrc32 = 0;
        this.uid = Utils.UNKNOWN;
        this.gid = Utils.UNKNOWN;
        this.permissions = Utils.UNKNOWN_S;
    }

    void setDataSetInfo(DataSetInfo dataSetInfo)
    {
        this.verifiedSize = dataSetInfo.size;
        this.crc32 = dataSetInfo.crc32;
        this.verifiedCrc32 = crc32;
        this.knowsChecksum = true;
    }

    /**
     * Returns the full path of this entry.
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Returns the parent directory of the path of this entry.
     */
    public String getParentPath()
    {
        return parentPath;
    }

    /**
     * Returns the name of the path this entry.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the real full path of this entry.
     * <p>
     * This will be the same as {@link #getPath()}, except when it originates from a call to
     * {@link IHDF5ArchiveInfoProvider#tryGetResolvedEntry(String, boolean)} with
     * <code>keepPath=true</code> where it will be the path of the link target.
     */
    public String getRealPath()
    {
        return realPath;
    }

    /**
     * Returns the real parent directory of the path of this entry.
     * <p>
     * This will be the same as {@link #getPath()}, except when it originates from a call to
     * {@link IHDF5ArchiveInfoProvider#tryGetResolvedEntry(String, boolean)} with
     * <code>keepPath=true</code> where it will be the parent path of the link target.
     */
    public String getRealParentPath()
    {
        return realParentPath;
    }

    /**
     * Returns the name of the path this entry.
     * <p>
     * This will be the same as {@link #getPath()}, except when it originates from a call to
     * {@link IHDF5ArchiveInfoProvider#tryGetResolvedEntry(String, boolean)} with
     * <code>keepPath=true</code> where it will be the name of the link target.
     */
    public String getRealName()
    {
        return realName;
    }

    /**
     * Returns how complete this entry is.
     * <p>
     * {@link ArchiveEntryCompleteness#BASE} entries can occur if the archive does not contain valid
     * file attributes, {@link ArchiveEntryCompleteness#LAST_MODIFIED} entries can occur if the
     * archive has been created or updated on a non-POSIX (read: Microsoft Windows) machine.
     */
    public ArchiveEntryCompleteness getCompleteness()
    {
        return completeness;
    }

    /**
     * Returns the link target. May be "?" if the link target has not been readm or if this entry
     * does not represent a link.
     * 
     * @see #hasLinkTarget()
     */
    public String getLinkTarget()
    {
        return linkTarget;
    }

    /**
     * Returns <code>true</code>, if this entry has a meaningful link target.
     * 
     * @see #getLinkTarget()
     */
    public boolean hasLinkTarget()
    {
        return hasLinkTarget;
    }

    /**
     * Returns the type of this entry.
     */
    public FileLinkType getLinkType()
    {
        return linkType;
    }

    /**
     * Returns if this entry is of type {@link FileLinkType#DIRECTORY}.
     */
    public boolean isDirectory()
    {
        return linkType == FileLinkType.DIRECTORY;
    }

    /**
     * Returns if this entry is of type {@link FileLinkType#SYMLINK}.
     */
    public boolean isSymLink()
    {
        return linkType == FileLinkType.SYMLINK;
    }

    /**
     * Returns if this entry is of type {@link FileLinkType#REGULAR_FILE}.
     */
    public boolean isRegularFile()
    {
        return linkType == FileLinkType.REGULAR_FILE;
    }

    /**
     * Returns the size of this entry, if this entry is a regular file, or 0 otherwise.
     * 
     * @see #isRegularFile()
     */
    public long getSize()
    {
        return size;
    }

    /**
     * Returns the date and time of last modification of this entry, measured in seconds since the
     * epoch (00:00:00 GMT, January 1, 1970), or -1, if this information is not available.
     */
    public long getLastModified()
    {
        return lastModified;
    }

    /**
     * Returns a string representation of the date and time of last modification of this entry, or
     * "?", if this information is not available.
     */
    public String getLastModifiedStr()
    {
        return getLastModifiedStr(lastModified);
    }

    private static String getLastModifiedStr(long lastModified)
    {
        if (lastModified >= 0)
        {
            return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", lastModified
                    * Utils.MILLIS_PER_SECOND);
        } else
        {
            return "?";
        }

    }

    /**
     * Returns <code>true</code>, if this archive entry has a CRC32 checksum stored.
     */
    public boolean hasChecksum()
    {
        return knowsChecksum;
    }

    /**
     * Returns the CRC32 checksum of this entry, or 0, if this information is not available or if
     * this entry is not a regular file.
     * 
     * @see #isRegularFile()
     */
    public int getCrc32()
    {
        return crc32;
    }

    /**
     * Returns a string representation (using hexadecimal digits) of the CRC32 checksum of this
     * entry, or "00000000", if this information is not available or if this entry is not a regular
     * file.
     * 
     * @see #isRegularFile()
     */
    public String getCrc32Str()
    {
        return Utils.crc32ToString(crc32);
    }

    /**
     * Returns a string representation of the user owning this archive entry, or "?", if this
     * information is not available.
     * <p>
     * Note that the archive only stores the UID and it is the local system which is used to resolve
     * the UID to a user.
     */
    public String getUser(boolean numeric)
    {
        return (uid >= 0) ? idCache.getUser(uid, numeric) : "?";
    }

    /**
     * Returns the UID of the user owning this archive entry, or -1, if this information is not
     * available.
     */
    public int getUid()
    {
        return uid;
    }

    /**
     * Returns a string representation of the group owning this archive entry, or "?", if this
     * information is not available.
     * <p>
     * Note that the archive only stores the GID and it is the local system which is used to resolve
     * the GID to a group.
     */
    public String getGroup(boolean numeric)
    {
        return (gid >= 0) ? idCache.getGroup(gid, numeric) : "?";
    }

    /**
     * Returns the GID of the group owning this archive entry, or -1, if this information is not
     * available.
     */
    public int getGid()
    {
        return gid;
    }

    /**
     * Returns the access permissions of this archive entry, or -1, if this information is not
     * available.
     */
    public short getPermissions()
    {
        return permissions;
    }

    /**
     * Returns a string representation of the access permissions of this archive entry, or "?", if
     * this information is not available.
     */
    public String getPermissionsString(boolean numeric)
    {
        return (permissions >= 0) ? Utils.permissionsToString(permissions,
                linkType == FileLinkType.DIRECTORY, numeric) : "?";
    }

    /**
     * Returns the error line saved for this archive entry, or <code>null</code>, if no error line
     * is available. A non-null error line is one indication of a verification error.
     * <p>
     * Note that the error line may contain additional information when a verification step has
     * failed on the archive entry.
     */
    public String tryGetErrorLine()
    {
        return errorLineOrNull;
    }

    /**
     * Returns the verified type of this entry, or <code>null</code>, if no verification has been
     * performed on this entry.
     * <p>
     * This information may come from an internal test of the archive (see
     * {@link ListParameters#isTestArchive()}) or from a verification of the archive against the
     * filesystem (see {@link VerifyParameters}).
     */
    public FileLinkType tryGetVerifiedLinkType()
    {
        return verifiedLinkType;
    }

    /**
     * Returns the verified size of this archive entry, or -1, if this information is not available.
     */
    public long getVerifiedSize()
    {
        return verifiedSize;
    }

    /**
     * Returns the verified CRC32 checksum of this archive entry, or 0, if this information is not
     * available.
     */
    public int getVerifiedCrc32()
    {
        return verifiedCrc32;
    }

    /**
     * Returns a string representation (using hexadecimal digits) of the verified CRC32 checksum of
     * this entry, or "00000000", if this information is not available or if this entry is not a
     * regular file.
     * 
     * @see #isRegularFile()
     */
    public String getVerifiedCrc32Str()
    {
        return Utils.crc32ToString(verifiedCrc32);
    }

    /**
     * Returns the verified date and time of last modification of this entry, measured in seconds
     * since the epoch (00:00:00 GMT, January 1, 1970), or -1, if this information is not available.
     */
    public long getVerifiedLastModified()
    {
        return verifiedLastModified;
    }

    /**
     * Returns a string representation of the verified date and time of last modification of this
     * entry, or "?", if this information is not available.
     */
    public String getVerifiedLastModifiedStr()
    {
        return getLastModifiedStr(verifiedLastModified);
    }

    /**
     * Returns true, if this entry has verification information on archive integrity.
     */
    public boolean hasVerificationInfo()
    {
        return (verifiedLinkType != null || verifiedSize != -1 || verifiedCrc32 != 0
                || verifiedLastModified != -1 || errorLineOrNull != null);
    }

    /**
     * Returns <code>true</code> if this archive entry has been verified successfully (or if no
     * verification information is available).
     */
    public boolean isOK()
    {
        return (errorLineOrNull == null) && linkTypeOK() && sizeOK() && lastModifiedOK()
                && checksumOK();
    }

    /**
     * Returns <code>true</code> if this the type of this archive entry has been verified
     * successfully (or if no verification information for the type is available).
     */
    public boolean linkTypeOK()
    {
        return (verifiedLinkType == null) || (linkType == verifiedLinkType);
    }

    /**
     * Returns <code>true</code> if this the size of this archive entry has been verified
     * successfully (or if no verification information for the size is available).
     */
    public boolean sizeOK()
    {
        return (verifiedSize == Utils.UNKNOWN) || (size == verifiedSize);
    }

    /**
     * Returns <code>true</code> if this the last modification date of this archive entry has been
     * verified successfully (or if no verification information for the last modification date is
     * available).
     */
    public boolean lastModifiedOK()
    {
        return (verifiedLastModified == Utils.UNKNOWN) || (lastModified == Utils.UNKNOWN)
                || (lastModified == verifiedLastModified);
    }

    /**
     * Returns <code>true</code> if this the checksum of this archive entry has been verified
     * successfully (or if no verification information for the checksum is available).
     */
    public boolean checksumOK()
    {
        return (false == knowsChecksum) || (verifiedSize == Utils.UNKNOWN) ||(crc32 == verifiedCrc32);
    }

    /**
     * Returns a status string for this entry.
     * <p>
     * Note that the status will always be <code>OK</code> if no verification information is
     * available.
     * 
     * @see #hasVerificationInfo()
     */
    public String getStatus(boolean verbose)
    {
        if (isOK() == false)
        {
            if (errorLineOrNull != null)
            {
                return "ERROR: " + errorLineOrNull;
            } else if (linkTypeOK() == false)
            {
                return verbose ? String.format(
                        "ERROR: Entry '%s' failed link type test, expected: %s, found: %s.", path,
                        linkType, verifiedLinkType) : "WRONG TYPE";
            } else if (sizeOK() == false)
            {
                return verbose ? String.format(
                        "ERROR: Entry '%s' failed size test, expected: %d, found: %d.", path, size,
                        verifiedSize) : "WRONG SIZE";
            } else if (checksumOK() == false)
            {
                return verbose ? String.format(
                        "ERROR: Entry '%s' failed CRC checksum test, expected: %s, found: %s.",
                        path, Utils.crc32ToString(crc32), Utils.crc32ToString(verifiedCrc32))
                        : "WRONG CRC32";
            } else if (lastModifiedOK() == false)
            {
                return verbose ? String
                        .format("ERROR: Entry '%s' failed last modification test, expected: %s, found: %s.",
                                path, getLastModifiedStr(), getVerifiedLastModifiedStr())
                        : "WRONG LASTMODIFICATION";
            }
        }
        return "OK";
    }

    /**
     * Returns a (verbose) string description of this entry, including the (brief) verification
     * status, if available.
     */
    public String describeLink()
    {
        return describeLink(true, false, true);
    }

    /**
     * Returns a string description of this entry, including the (brief) verification status, if
     * available.
     * 
     * @param verbose If <code>true</code>, the link description will contain all information
     *            available, if <code>false</code>, it will only contain the path information.
     */
    public String describeLink(boolean verbose)
    {
        return describeLink(verbose, false, true);
    }

    /**
     * Returns a string description of this entry, including the (brief) verification status, if
     * available.
     * 
     * @param verbose If <code>true</code>, the link description will contain all information
     *            available, if <code>false</code>, it will only contain the path information.
     * @param numeric If <code>true</code>, file ownership and access permissions will be
     *            represented numerically, if <code>false</code>, they will be represented as
     *            strings. Only relevant if <var>verbose</var> is <code>true</code>.
     */
    public String describeLink(boolean verbose, boolean numeric)
    {
        return describeLink(verbose, numeric, true);
    }

    /**
     * Returns a string description of this entry.
     * 
     * @param verbose If <code>true</code>, the link description will contain all information
     *            available, if <code>false</code>, it will only contain the path information.
     * @param numeric If <code>true</code>, file ownership and access permissions will be
     *            represented numerically, if <code>false</code>, they will be represented as
     *            strings. Only relevant if <var>verbose</var> is <code>true</code>.
     * @param includeCheck If <code>true</code> (and if verification information is available for
     *            this entry), add a (brief) verification status string.
     * @see #hasVerificationInfo()
     */
    public String describeLink(boolean verbose, boolean numeric, boolean includeCheck)
    {
        final StringBuilder builder = new StringBuilder();
        if (verbose == false)
        {
            builder.append(path);
        } else
        {
            switch (completeness)
            {
                case BASE:
                    if (linkType == FileLinkType.SYMLINK)
                    {
                        builder.append(String.format("          \t%s -> %s", path, linkTarget));
                    } else if (linkType == FileLinkType.DIRECTORY)
                    {
                        builder.append(String.format("       DIR\t%s", path));
                    } else
                    {
                        builder.append(String.format("%10d\t%s\t%s%s", size,
                                Utils.crc32ToString(crc32), path,
                                (linkType == FileLinkType.REGULAR_FILE) ? "" : "\t*"));
                    }
                    break;
                case LAST_MODIFIED:
                    if (linkType == FileLinkType.SYMLINK)
                    {
                        builder.append(String.format(
                                "          \t%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS\t%2$s -> %3$s",
                                lastModified * Utils.MILLIS_PER_SECOND, path, linkTarget));
                    } else if (linkType == FileLinkType.DIRECTORY)
                    {
                        builder.append(String.format(
                                "       DIR\t%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS\t%2$s",
                                lastModified * Utils.MILLIS_PER_SECOND, path));
                    } else
                    {
                        builder.append(String.format(
                                "%10d\t%2$tY-%2$tm-%2$td %2$tH:%2$tM:%2$tS\t%3$s\t%4$s%5$s", size,
                                lastModified * Utils.MILLIS_PER_SECOND, Utils.crc32ToString(crc32),
                                path, (linkType == FileLinkType.REGULAR_FILE) ? "" : "\t*"));
                    }
                    break;
                case FULL:
                    if (linkType == FileLinkType.SYMLINK)
                    {
                        builder.append(String
                                .format("%s\t%s\t%s\t          \t%4$tY-%4$tm-%4$td %4$tH:%4$tM:%4$tS\t        \t%5$s -> %6$s",
                                        Utils.permissionsToString(permissions, false, numeric),
                                        getUser(numeric), getGroup(numeric), lastModified
                                                * Utils.MILLIS_PER_SECOND, path, linkTarget));
                    } else if (linkType == FileLinkType.DIRECTORY)
                    {
                        builder.append(String
                                .format("%s\t%s\t%s\t       DIR\t%4$tY-%4$tm-%4$td %4$tH:%4$tM:%4$tS\t        \t%5$s",
                                        Utils.permissionsToString(permissions, true, numeric),
                                        getUser(numeric), getGroup(numeric), lastModified
                                                * Utils.MILLIS_PER_SECOND, path));
                    } else
                    {
                        builder.append(String
                                .format("%s\t%s\t%s\t%10d\t%5$tY-%5$tm-%5$td %5$tH:%5$tM:%5$tS\t%6$s\t%7$s%8$s",
                                        Utils.permissionsToString(permissions, false, numeric),
                                        getUser(numeric), getGroup(numeric), size, lastModified
                                                * Utils.MILLIS_PER_SECOND,
                                        Utils.crc32ToString(crc32), path,
                                        (linkType == FileLinkType.REGULAR_FILE) ? "" : "\t*"));
                    }
                    break;
                default:
                    throw new Error("Unknown level of link completeness: " + completeness);
            }
        }
        if (includeCheck && hasVerificationInfo())
        {
            builder.append('\t');
            builder.append(getStatus(false));
        }
        return builder.toString();
    }

    @Override
    public String toString()
    {
        return describeLink();
    }

}