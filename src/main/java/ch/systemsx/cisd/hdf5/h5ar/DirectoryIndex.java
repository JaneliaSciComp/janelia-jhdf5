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

import static ch.systemsx.cisd.hdf5.HDF5CompoundMemberMapping.mapping;

import java.io.File;
import java.io.Flushable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

import hdf.hdf5lib.exceptions.HDF5Exception;

import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.base.unix.FileLinkType;
import ch.systemsx.cisd.hdf5.CharacterEncoding;
import ch.systemsx.cisd.hdf5.HDF5CompoundMemberMapping;
import ch.systemsx.cisd.hdf5.HDF5CompoundType;
import ch.systemsx.cisd.hdf5.HDF5EnumerationType;
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5LinkInformation;
import ch.systemsx.cisd.hdf5.IHDF5CompoundInformationRetriever;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ch.systemsx.cisd.hdf5.StringUtils;

/**
 * Memory representation of the directory index stored in an HDF5 archive.
 * <p>
 * Can operate in read-only or read-write mode. The mode is automatically determined by the
 * <var>hdf5Reader</var> provided the constructor: If this is an instance of {@link IHDF5Writer},
 * the directory index will be read-write, otherwise read-only.
 * 
 * @author Bernd Rinn
 */
class DirectoryIndex implements IDirectoryIndex
{
    private static final String CRC32_ATTRIBUTE_NAME = "CRC32";

    private final IHDF5Reader hdf5Reader;

    private final IHDF5Writer hdf5WriterOrNull;

    private final String groupPath;

    private final IErrorStrategy errorStrategy;

    private final Set<Flushable> flushables;

    /**
     * The list of all links in this directory.
     * <p>
     * The order is to have all directories (in alphabetical order) before all files (in
     * alphabetical order).
     */
    private LinkStore links;

    private boolean readLinkTargets;

    private boolean dirty;

    /**
     * Converts an array of {@link File}s into a list of {@link LinkRecord}s. The list is optimized
     * for iterating through it and removing single entries during the iteration.
     * <p>
     * Note that the length of the list will always be the same as the length of <var>entries</var>.
     * If some <code>stat</code> call failed on an entry, this entry will be <code>null</code>, so
     * code using the returned list of this method needs to be prepared that this list may contain
     * <code>null</code> values!
     * 
     * @return A list of {@link LinkRecord}s in the same order as <var>entries</var>.
     */
    public static List<LinkRecord> convertFilesToLinks(File[] files, IErrorStrategy errorStrategy)
    {
        final List<LinkRecord> list = new LinkedList<LinkRecord>();
        for (File file : files)
        {
            list.add(LinkRecord.tryCreate(file, errorStrategy));
        }
        return list;
    }

    private static HDF5EnumerationType getHDF5LinkTypeEnumeration(IHDF5Reader reader)
    {
        return reader.enumeration().getType("linkType", getFileLinkTypeValues());
    }

    private static HDF5CompoundType<LinkRecord> getHDF5LinkCompoundType(IHDF5Reader reader)
    {
        return getHDF5LinkCompoundType(reader, getHDF5LinkTypeEnumeration(reader));
    }

    private static HDF5CompoundType<LinkRecord> getHDF5LinkCompoundType(IHDF5Reader reader,
            HDF5EnumerationType hdf5LinkTypeEnumeration)
    {
        return reader.compound().getType(LinkRecord.class, getMapping(hdf5LinkTypeEnumeration));
    }

    private static String[] getFileLinkTypeValues()
    {
        final FileLinkType[] fileLinkTypes = FileLinkType.values();
        final String[] values = new String[fileLinkTypes.length];
        for (int i = 0; i < values.length; ++i)
        {
            values[i] = fileLinkTypes[i].name();
        }
        return values;
    }

    private static HDF5CompoundMemberMapping[] getMapping(HDF5EnumerationType linkEnumerationType)
    {
        return new HDF5CompoundMemberMapping[]
            { mapping("linkNameLength"), mapping("linkType").enumType(linkEnumerationType),
                    mapping("size"), mapping("lastModified"), mapping("uid"), mapping("gid"),
                    mapping("permissions"), mapping("checksum").fieldName("crc32") };
    }

    /**
     * Creates a new directory (group) index. Note that <var>hdf5Reader</var> needs to be an
     * instance of {@link IHDF5Writer} if you intend to write the index to the archive.
     */
    DirectoryIndex(IHDF5Reader hdf5Reader, String groupPath, IErrorStrategy errorStrategy,
            boolean readLinkTargets)
    {
        assert hdf5Reader != null;
        assert groupPath != null;

        this.hdf5Reader = hdf5Reader;
        this.hdf5WriterOrNull =
                (hdf5Reader instanceof IHDF5Writer) ? (IHDF5Writer) hdf5Reader : null;
        if (hdf5WriterOrNull != null)
        {
            hdf5WriterOrNull.file().addFlushable(this);
        }
        this.groupPath = (groupPath.length() == 0) ? "/" : groupPath;
        this.errorStrategy = errorStrategy;
        this.flushables = new LinkedHashSet<Flushable>();
        readIndex(readLinkTargets);
    }

    @Override
    public boolean addFlushable(Flushable flushable)
    {
        return flushables.add(flushable);
    }

    @Override
    public boolean removeFlushable(Flushable flushable)
    {
        return flushables.remove(flushable);
    }

    void flushExternals()
    {
        for (Flushable f : flushables)
        {
            try
            {
                f.flush();
            } catch (Exception ex)
            {
                System.err.println("External flushable throws an exception:");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Amend the index with link targets. If the links targets have already been read, this method
     * is a noop.
     */
    @Override
    public void amendLinkTargets()
    {
        if (readLinkTargets)
        {
            return;
        }
        links.amendLinkTargets(hdf5Reader, groupPath);
        readLinkTargets = true;
    }

    private String getIndexDataSetName()
    {
        return groupPath + "/" + hdf5Reader.object().toHouseKeepingPath("INDEX");
    }

    private String getIndexNamesDataSetName()
    {
        return groupPath + "/" + hdf5Reader.object().toHouseKeepingPath("INDEXNAMES");
    }

    /**
     * (Re-)Reads the directory index from the archive represented by <var>hdf5Reader</var>.
     */
    private void readIndex(boolean withLinkTargets)
    {
        boolean readingH5ArIndexWorked = false;
        try
        {
            if (hdf5Reader.exists(getIndexDataSetName())
                    && hdf5Reader.exists(getIndexNamesDataSetName()))
            {
                final HDF5CompoundType<LinkRecord> linkCompoundType =
                        getHDF5LinkCompoundType(hdf5Reader);
                final CRC32 crc32Digester = new CRC32();
                final String indexDataSetName = getIndexDataSetName();
                final LinkRecord[] work =
                        hdf5Reader.compound().readArray(indexDataSetName, linkCompoundType,
                                new IHDF5CompoundInformationRetriever.IByteArrayInspector()
                                    {
                                        @Override
                                        public void inspect(byte[] byteArray)
                                        {
                                            updateCRC32(byteArray, linkCompoundType, crc32Digester);
                                        }
                                    });
                int crc32 = (int) crc32Digester.getValue();
                int crc32Stored =
                        hdf5Reader.int32().getAttr(indexDataSetName, CRC32_ATTRIBUTE_NAME);
                if (crc32 != crc32Stored)
                {
                    if (calcLegacy_14_12_0_Checksum(indexDataSetName, linkCompoundType) != crc32Stored)
                    {
                        throw new ListArchiveException(groupPath,
                                "CRC checksum mismatch on index (links). Expected: "
                                        + Utils.crc32ToString(crc32Stored) + ", found: "
                                        + Utils.crc32ToString(crc32));
                    }
                }
                final String indexNamesDataSetName = getIndexNamesDataSetName();
                final String concatenatedNames = hdf5Reader.readString(indexNamesDataSetName);
                crc32 = calcCrc32(concatenatedNames);
                crc32Stored =
                        hdf5Reader.int32().getAttr(indexNamesDataSetName, CRC32_ATTRIBUTE_NAME);
                if (crc32 != crc32Stored)
                {
                    throw new ListArchiveException(groupPath,
                            "CRC checksum mismatch on index (names). Expected: "
                                    + Utils.crc32ToString(crc32Stored) + ", found: "
                                    + Utils.crc32ToString(crc32));
                }
                initLinks(work, concatenatedNames, withLinkTargets);
                links = new LinkStore(work);
                readingH5ArIndexWorked = true;
            }
        } catch (RuntimeException ex)
        {
            errorStrategy.dealWithError(new ListArchiveException(groupPath, ex));
        }
        // Fallback: couldn't read the index, reconstructing it from the group information.
        if (readingH5ArIndexWorked == false)
        {
            if (hdf5Reader.object().isGroup(groupPath, false))
            {
                final List<HDF5LinkInformation> hdf5LinkInfos =
                        hdf5Reader.object().getGroupMemberInformation(groupPath, withLinkTargets);
                final LinkRecord[] work = new LinkRecord[hdf5LinkInfos.size()];
                int idx = 0;
                for (HDF5LinkInformation linfo : hdf5LinkInfos)
                {
                    final long size =
                            linfo.isDataSet() ? hdf5Reader.object().getSize(linfo.getPath())
                                    : Utils.UNKNOWN;
                    work[idx++] = new LinkRecord(linfo, size);
                }
                Arrays.sort(work);
                links = new LinkStore(work);
            } else
            {
                links = new LinkStore();
            }
        }
        readLinkTargets = withLinkTargets;
        dirty = false;
    }

    private int calcLegacy_14_12_0_Checksum(final String indexDataSetName,
            final HDF5CompoundType<LinkRecord> linkCompoundType)
    {
        final CRC32 crc32Digester = new CRC32();
                hdf5Reader.compound().readArray(indexDataSetName, linkCompoundType,
                        new IHDF5CompoundInformationRetriever.IByteArrayInspector()
                            {
                                @Override
                                public void inspect(byte[] byteArray)
                                {
                                    crc32Digester.update(byteArray);
                                }
                            });
        return (int) crc32Digester.getValue();
    }

    private void initLinks(final LinkRecord[] work, final String concatenatedNames,
            boolean withLinkTargets)
    {
        int namePos = 0;
        for (LinkRecord link : work)
        {
            namePos =
                    link.initAfterReading(concatenatedNames, namePos, hdf5Reader, groupPath,
                            withLinkTargets);
        }
    }

    @Override
    public boolean exists(String name)
    {
        return links.exists(name);
    }

    @Override
    public boolean isDirectory(String name)
    {
        final LinkRecord link = links.tryGetLink(name);
        return (link != null) && link.isDirectory();
    }

    /**
     * Returns the link with {@link LinkRecord#getLinkName()} equal to <var>name</var>, or
     * <code>null</code>, if there is no such link in the directory index.
     */
    @Override
    public LinkRecord tryGetLink(String name)
    {
        final LinkRecord linkOrNull = links.tryGetLink(name);
        if (linkOrNull != null)
        {
            linkOrNull.resetVerification();
        }
        return linkOrNull;
    }

    /**
     * Returns <code>true</code>, if this class has link targets read.
     */
    @Override
    public boolean hasLinkTargets()
    {
        return readLinkTargets;
    }

    //
    // Iterable
    //

    @Override
    public Iterator<LinkRecord> iterator()
    {
        return links.iterator();
    }

    //
    // Writing methods
    //

    /**
     * Writes the directory index to the archive represented by <var>hdf5Writer</var>.
     * <p>
     * Works on the list data structure.
     */
    @Override
    public void flush()
    {
        flushExternals();
        if (dirty == false)
        {
            return;
        }
        ensureWriteMode();
        try
        {
            final StringBuilder concatenatedNames = new StringBuilder();
            for (LinkRecord link : links)
            {
                link.prepareForWriting(concatenatedNames);
            }
            final String indexNamesDataSetName = getIndexNamesDataSetName();
            final String concatenatedNamesStr = concatenatedNames.toString();
            hdf5WriterOrNull.string().write(indexNamesDataSetName, concatenatedNamesStr,
                    HDF5GenericStorageFeatures.GENERIC_DEFLATE);
            hdf5WriterOrNull.int32().setAttr(indexNamesDataSetName, CRC32_ATTRIBUTE_NAME,
                    calcCrc32(concatenatedNamesStr));
            final String indexDataSetName = getIndexDataSetName();
            final CRC32 crc32Digester = new CRC32();
            final HDF5CompoundType<LinkRecord> linkCompoundType =
                    getHDF5LinkCompoundType(hdf5WriterOrNull);
            hdf5WriterOrNull.compound().writeArray(indexDataSetName, linkCompoundType,
                    links.getLinkArray(), HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION,
                    new IHDF5CompoundInformationRetriever.IByteArrayInspector()
                        {
                            @Override
                            public void inspect(byte[] byteArray)
                            {
                                updateCRC32(byteArray, linkCompoundType, crc32Digester);
                            }
                        });
            hdf5WriterOrNull.int32().setAttr(indexDataSetName, CRC32_ATTRIBUTE_NAME,
                    (int) crc32Digester.getValue());
        } catch (HDF5Exception ex)
        {
            errorStrategy.dealWithError(new ListArchiveException(groupPath, ex));
        }
        dirty = false;
    }

    /**
     * Add <var>entries</var> to the index. Any link that already exists in the index will be
     * replaced.
     */
    @Override
    public void updateIndex(LinkRecord[] entries)
    {
        ensureWriteMode();
        links.update(entries);
        dirty = true;
    }

    /**
     * Add <var>entries</var> to the index. Any link that already exists in the index will be
     * replaced.
     */
    @Override
    public void updateIndex(Collection<LinkRecord> entries)
    {
        ensureWriteMode();
        links.update(entries);
        dirty = true;
    }

    /**
     * Add <var>entry</var> to the index. If it already exists in the index, it will be replaced.
     */
    @Override
    public void updateIndex(LinkRecord entry)
    {
        ensureWriteMode();
        links.update(entry);
        dirty = true;
    }

    /**
     * Removes <var>linkName</var> from the index, if it is in.
     * 
     * @return <code>true</code>, if <var>linkName</var> was removed.
     */
    @Override
    public boolean remove(String linkName)
    {
        ensureWriteMode();
        final boolean storeChanged = links.remove(linkName);
        dirty |= storeChanged;
        return storeChanged;
    }

    private void ensureWriteMode()
    {
        if (hdf5WriterOrNull == null)
        {
            throw new IllegalStateException("Cannot write index in read-only mode.");
        }
    }

    private int calcCrc32(String names)
    {
        final CRC32 crc32 = new CRC32();
        crc32.update(StringUtils.toBytes0Term(names, names.length(), CharacterEncoding.UTF8));
        return (int) crc32.getValue();
    }

    private void updateCRC32(byte[] byteArray, final HDF5CompoundType<LinkRecord> linkCompoundType,
            final CRC32 crc32Digester)
    {
        final int numberOfRecords = byteArray.length / linkCompoundType.getRecordSizeInMemory();
        final int numberOfMembers = linkCompoundType.getNumberOfMembers();
        for (int i = 0; i < numberOfRecords; ++i)
        {
            final int recordOfs = i * linkCompoundType.getRecordSizeInMemory();
            for (int j = 0; j < numberOfMembers; ++j)
            {
                final int ofs = recordOfs + linkCompoundType.getMemberOffsetInMemory(j);
                final int sizeOnDisk = linkCompoundType.getMemberSize(j);
                crc32Digester.update(byteArray, ofs, sizeOnDisk);
            }
        }
    }

    //
    // Closeable
    //

    @Override
    public void close() throws IOExceptionUnchecked
    {
        flush();
        if (hdf5WriterOrNull != null)
        {
            hdf5WriterOrNull.file().removeFlushable(this);
        }
        flushables.clear();
    }

}
