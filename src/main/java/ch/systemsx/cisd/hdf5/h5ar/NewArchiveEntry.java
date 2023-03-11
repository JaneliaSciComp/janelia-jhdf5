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

/**
 * A class to describe a new (yet to be created) archive entry.
 * 
 * @author Bernd Rinn
 */
public abstract class NewArchiveEntry
{
    private final String parentPath;

    private final String name;

    private final FileLinkType linkType;

    private final String linkTarget;

    private long lastModified;

    private int uid;

    private int gid;

    private short permissions;

    private long size;

    private int crc32;

    /**
     * A class to describe a new regular file archive entry.
     */
    public static final class NewFileArchiveEntry extends NewArchiveEntry
    {
        private boolean compress;

        private int chunkSize;

        private NewFileArchiveEntry(String parentPath, String name)
        {
            super(parentPath, name, FileLinkType.REGULAR_FILE, null);
        }

        @Override
        public NewFileArchiveEntry lastModified(long lastModified)
        {
            super.lastModified(lastModified);
            return this;
        }

        @Override
        public NewFileArchiveEntry uid(int uid)
        {
            super.uid(uid);
            return this;
        }

        @Override
        public NewFileArchiveEntry gid(int gid)
        {
            super.gid(gid);
            return this;
        }

        @Override
        public NewFileArchiveEntry permissions(short permissions)
        {
            super.permissions(permissions);
            return this;
        }

        public NewFileArchiveEntry compress()
        {
            this.compress = true;
            return this;
        }

        public NewFileArchiveEntry compress(@SuppressWarnings("hiding")
        boolean compress)
        {
            this.compress = compress;
            return this;
        }

        public boolean isCompress()
        {
            return compress;
        }

        /**
         * @param chunkSize The chunk size of the file in the archive. Will be capped to 10MB.
         */
        public NewFileArchiveEntry chunkSize(@SuppressWarnings("hiding")
        int chunkSize)
        {
            this.chunkSize = chunkSize;
            return this;
        }

        public int getChunkSize()
        {
            return chunkSize;
        }

    }

    /**
     * A class to describe a new symlink archive entry.
     */
    public static final class NewSymLinkArchiveEntry extends NewArchiveEntry
    {
        private NewSymLinkArchiveEntry(String parentPath, String name, String linkTarget)
        {
            super(parentPath, name, FileLinkType.SYMLINK, linkTarget);
        }

        @Override
        public NewSymLinkArchiveEntry lastModified(long lastModified)
        {
            super.lastModified(lastModified);
            return this;
        }

        @Override
        public NewSymLinkArchiveEntry uid(int uid)
        {
            super.uid(uid);
            return this;
        }

        @Override
        public NewSymLinkArchiveEntry gid(int gid)
        {
            super.gid(gid);
            return this;
        }

        @Override
        public NewSymLinkArchiveEntry permissions(short permissions)
        {
            super.permissions(permissions);
            return this;
        }
    }

    /**
     * A class to describe a new directory archive entry.
     */
    public static final class NewDirectoryArchiveEntry extends NewArchiveEntry
    {
        private NewDirectoryArchiveEntry(String parentPath, String name)
        {
            super(parentPath, name, FileLinkType.DIRECTORY, null);
        }

        @Override
        public NewDirectoryArchiveEntry lastModified(long lastModified)
        {
            super.lastModified(lastModified);
            return this;
        }

        @Override
        public NewDirectoryArchiveEntry uid(int uid)
        {
            super.uid(uid);
            return this;
        }

        @Override
        public NewDirectoryArchiveEntry gid(int gid)
        {
            super.gid(gid);
            return this;
        }

        @Override
        public NewDirectoryArchiveEntry permissions(short permissions)
        {
            super.permissions(permissions);
            return this;
        }
    }

    /**
     * @param path The path of the file in the archive.
     */
    public static NewFileArchiveEntry file(String path)
    {
        final String normalizedPath = Utils.normalizePath(path);
        final String parentPath = Utils.getParentPath(normalizedPath);
        final String name = Utils.getName(normalizedPath);
        if (name.length() == 0)
        {
            throw new ArchivingException(path, "Path does not contain a name.");
        }
        return new NewFileArchiveEntry(parentPath, name);
    }

    /**
     * @param parentPath The parent path of the file in the archive.
     * @param name The name of the file in the archive.
     */
    public static NewFileArchiveEntry file(String parentPath, String name)
    {
        return new NewFileArchiveEntry(parentPath, name);
    }

    /**
     * @param path The path of the symlink in the archive.
     * @param linkTarget the link target of the symlink.
     */
    public static NewSymLinkArchiveEntry symlink(String path, String linkTarget)
    {
        final String normalizedPath = Utils.normalizePath(path);
        final String parentPath = Utils.getParentPath(normalizedPath);
        final String name = Utils.getName(normalizedPath);
        if (name.length() == 0)
        {
            throw new ArchivingException(path, "Path does not contain a name.");
        }
        return new NewSymLinkArchiveEntry(parentPath, name, linkTarget);
    }

    public static NewSymLinkArchiveEntry symlink(String parentPath, String name, String linkTarget)
    {
        return new NewSymLinkArchiveEntry(parentPath, name, linkTarget);
    }

    public static NewDirectoryArchiveEntry directory(String path)
    {
        final String normalizedPath = Utils.normalizePath(path);
        final String parentPath = Utils.getParentPath(normalizedPath);
        final String name = Utils.getName(normalizedPath);
        if (name.length() == 0)
        {
            throw new ArchivingException(path, "Path does not contain a name.");
        }
        return new NewDirectoryArchiveEntry(parentPath, name);
    }

    public static NewDirectoryArchiveEntry directory(String parentPath, String name)
    {
        return new NewDirectoryArchiveEntry(parentPath, name);
    }

    private NewArchiveEntry(String parentPath, String name, FileLinkType linkType, String linkTarget)
    {
        this.parentPath = Utils.normalizePath(parentPath);
        this.name = name;
        this.linkType = linkType;
        this.linkTarget = linkTarget;
        this.size = Utils.UNKNOWN;
        this.lastModified = System.currentTimeMillis() / 1000;
        this.uid = Utils.getCurrentUid();
        this.gid = Utils.getCurrentGid();
        this.permissions = 0755;
    }

    public String getParentPath()
    {
        return parentPath;
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public NewArchiveEntry lastModified(@SuppressWarnings("hiding")
    long lastModified)
    {
        this.lastModified = lastModified;
        return this;
    }

    public int getUid()
    {
        return uid;
    }

    public NewArchiveEntry uid(@SuppressWarnings("hiding")
    int uid)
    {
        this.uid = uid;
        return this;
    }

    public int getGid()
    {
        return gid;
    }

    public NewArchiveEntry gid(@SuppressWarnings("hiding")
    int gid)
    {
        this.gid = gid;
        return this;
    }

    public short getPermissions()
    {
        return permissions;
    }

    public NewArchiveEntry permissions(@SuppressWarnings("hiding")
    short permissions)
    {
        this.permissions = permissions;
        return this;
    }

    public int getCrc32()
    {
        return crc32;
    }

    void setCrc32(int crc32)
    {
        this.crc32 = crc32;
    }

    public String getName()
    {
        return name;
    }

    public FileLinkType getLinkType()
    {
        return linkType;
    }

    public String getLinkTarget()
    {
        return linkTarget;
    }

    public long getSize()
    {
        return size;
    }

    void setSize(long size)
    {
        this.size = size;
    }

}
