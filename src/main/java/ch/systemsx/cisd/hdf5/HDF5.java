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

package ch.systemsx.cisd.hdf5;

import static hdf.hdf5lib.H5.*;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper.H5Pset_mdc_image_config;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper.H5Pget_mdc_image_enabled;
import static hdf.hdf5lib.HDF5Constants.H5_INDEX_NAME;
import static hdf.hdf5lib.HDF5Constants.H5_ITER_NATIVE;
import static hdf.hdf5lib.HDF5Constants.H5D_CHUNKED;
import static hdf.hdf5lib.HDF5Constants.H5D_COMPACT;
import static hdf.hdf5lib.HDF5Constants.H5D_FILL_TIME_ALLOC;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_RDONLY;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_RDWR;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_TRUNC;
import static hdf.hdf5lib.HDF5Constants.H5F_SCOPE_GLOBAL;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_GROUP;
import static hdf.hdf5lib.HDF5Constants.H5P_ATTRIBUTE_CREATE;
import static hdf.hdf5lib.HDF5Constants.H5P_DATASET_CREATE;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5P_FILE_ACCESS;
import static hdf.hdf5lib.HDF5Constants.H5P_GROUP_CREATE;
import static hdf.hdf5lib.HDF5Constants.H5P_LINK_CREATE;
import static hdf.hdf5lib.HDF5Constants.H5R_OBJECT;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;
import static hdf.hdf5lib.HDF5Constants.H5S_MAX_RANK;
import static hdf.hdf5lib.HDF5Constants.H5S_SCALAR;
import static hdf.hdf5lib.HDF5Constants.H5S_SELECT_SET;
import static hdf.hdf5lib.HDF5Constants.H5S_UNLIMITED;
import static hdf.hdf5lib.HDF5Constants.H5T_ARRAY;
import static hdf.hdf5lib.HDF5Constants.H5T_COMPOUND;
import static hdf.hdf5lib.HDF5Constants.H5T_C_S1;
import static hdf.hdf5lib.HDF5Constants.H5T_ENUM;
import static hdf.hdf5lib.HDF5Constants.H5T_FLOAT;
import static hdf.hdf5lib.HDF5Constants.H5T_INTEGER;
import static hdf.hdf5lib.HDF5Constants.H5T_OPAQUE;
import static hdf.hdf5lib.HDF5Constants.H5T_OPAQUE_TAG_MAX;
import static hdf.hdf5lib.HDF5Constants.H5T_SGN_NONE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I16LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I32LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I8LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STR_NULLPAD;
import static hdf.hdf5lib.HDF5Constants.H5T_VARIABLE;
import static hdf.hdf5lib.HDF5Constants.H5Z_SO_FLOAT_DSCALE;
import static hdf.hdf5lib.HDF5Constants.H5Z_SO_INT;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5JavaException;

import hdf.hdf5lib.structs.H5O_info_t;
import ch.systemsx.cisd.base.convert.NativeData;
import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.FileFormatVersion;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.FileFormatVersionBounds;
import ch.systemsx.cisd.hdf5.cleanup.CleanUpCallable;
import ch.systemsx.cisd.hdf5.cleanup.CleanUpRegistry;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import ch.systemsx.cisd.hdf5.exceptions.HDF5SpaceRankMismatch;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;

/**
 * A wrapper around {@link hdf.hdf5lib.H5General} that handles closing of resources automatically by means of registering clean-up {@link Runnable}s.
 * 
 * @author Bernd Rinn
 */
class HDF5
{

    private final static int MAX_PATH_LENGTH = 16384;

    private final CleanUpCallable runner;

    private final long dataSetCreationPropertyListCompactStorageLayoutFileTimeAlloc;

    private final long dataSetCreationPropertyListFillTimeAlloc;

    private final long numericConversionXferPropertyListID;

    private final long lcplCreateIntermediateGroups;

    private final boolean useUTF8CharEncoding;

    private final boolean autoDereference;

    public HDF5(final CleanUpRegistry fileRegistry, final CleanUpCallable runner,
            final boolean performNumericConversions, final boolean useUTF8CharEncoding,
            final boolean autoDereference)
    {
        this.runner = runner;
        this.useUTF8CharEncoding = useUTF8CharEncoding;
        this.autoDereference = autoDereference;
        this.dataSetCreationPropertyListCompactStorageLayoutFileTimeAlloc =
                createDataSetCreationPropertyList(fileRegistry);
        H5Pset_layout(dataSetCreationPropertyListCompactStorageLayoutFileTimeAlloc, H5D_COMPACT);
        this.dataSetCreationPropertyListFillTimeAlloc =
                createDataSetCreationPropertyList(fileRegistry);
        if (performNumericConversions)
        {
            this.numericConversionXferPropertyListID =
                    createDataSetXferPropertyListAbortOverflow(fileRegistry);
        } else
        {
            this.numericConversionXferPropertyListID =
                    createDataSetXferPropertyListAbort(fileRegistry);
        }
        this.lcplCreateIntermediateGroups = createLinkCreationPropertyList(true, fileRegistry);

    }

    private static void checkMaxLength(String path) throws HDF5JavaException
    {
        if (path.length() > MAX_PATH_LENGTH)
        {
            throw new HDF5JavaException("Path too long (length=" + path.length() + ")");
        }
    }
    
    //
    // Library
    //
    
    public static void resetLibrary()
    {
        synchronized (H5.class)
        {
            H5.H5close();
            H5.H5open();
            H5.H5error_off();
        }
    }

    //
    // File
    //

    public long createFile(String fileName, FileFormatVersionBounds fileFormatVersionBounds, 
            Boolean mdcGenerateImage, ICleanUpRegistry registry)
    {
        final long fileAccessPropertyListId =
                createFileAccessPropertyListId(fileFormatVersionBounds, mdcGenerateImage, registry);
        final long fileId =
                H5Fcreate(fileName, H5F_ACC_TRUNC, H5P_DEFAULT, fileAccessPropertyListId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Fclose(fileId);
                }
            });
        return fileId;
    }

    private long createFileAccessPropertyListId(FileFormatVersionBounds fileFormatVersionBounds, 
            boolean mdcGenerateImage, ICleanUpRegistry registry)
    {
        long fileAccessPropertyListId = H5P_DEFAULT;
        // MDC image generation is incompatible with low file format bound EARLIEST, thus raise it to V1_8.
        if (mdcGenerateImage && (fileFormatVersionBounds.getLowBound() == FileFormatVersion.EARLIEST))
        {
            switch (fileFormatVersionBounds)
            {
                case EARLIEST_LATEST:
                    fileFormatVersionBounds = FileFormatVersionBounds.V1_8_LATEST;
                    break;
                case EARLIEST_V1_10:
                    fileFormatVersionBounds = FileFormatVersionBounds.V1_8_V1_10;
                    break;
                case EARLIEST_V1_8:
                    throw new HDF5JavaException("Upper file version bound V1_8 is incompatible with MDC image generation.");
                default:
                    throw new IllegalStateException("Unhandled case switch");
            }
        }
        if (fileFormatVersionBounds != FileFormatVersionBounds.getDefault() || mdcGenerateImage)
        {
            final long fapl = H5Pcreate(H5P_FILE_ACCESS);
            registry.registerCleanUp(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        H5Pclose(fapl);
                    }
                });
            fileAccessPropertyListId = fapl;
            if (fileFormatVersionBounds != FileFormatVersionBounds.getDefault())
            {
                H5Pset_libver_bounds(fileAccessPropertyListId, 
                        fileFormatVersionBounds.getLowBound().getHdf5Constant(), 
                        fileFormatVersionBounds.getHighBound().getHdf5Constant());
            }
            if (mdcGenerateImage)
            {
                H5Pset_mdc_image_config(fileAccessPropertyListId, mdcGenerateImage);
            }
        }
        return fileAccessPropertyListId;
    }
    
    /**
     * @return if the generation of a metadata image is enabled for <code>fileId</code>.
     */
    public boolean isMDCImageGenerationEnabled(long fileId)
    {
        final long fapl = H5Fget_access_plist(fileId);
        return H5Pget_mdc_image_enabled(fapl);
        
    }

    public long openFileReadOnly(String fileName, ICleanUpRegistry registry)
    {
        final long fileId = H5Fopen(fileName, H5F_ACC_RDONLY, H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Fclose(fileId);
                }
            });
        return fileId;
    }

    public long openFileReadWrite(String fileName, FileFormatVersionBounds fileFormatVersionBounds, 
            Boolean mdcGenerateImage, ICleanUpRegistry registry)
    {
        final long fileAccessPropertyListId = createFileAccessPropertyListId(fileFormatVersionBounds, mdcGenerateImage, registry);
        final File f = new File(fileName);
        if (f.exists() && f.isFile() == false)
        {
            throw new HDF5Exception("An entry with name '" + fileName
                    + "' exists but is not a file.");
        }
        final long fileId = H5Fopen(fileName, H5F_ACC_RDWR, fileAccessPropertyListId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Fclose(fileId);
                }
            });
        return fileId;
    }

    public void flushFile(long fileId)
    {
        H5Fflush(fileId, H5F_SCOPE_GLOBAL);
    }

    //
    // Object
    //

    public long openObject(long fileId, String path, ICleanUpRegistry registry)
    {
        checkMaxLength(path);
        final long objectId =
                isReference(path) ? H5Oopen_by_addr(fileId, Long.parseLong(path.substring(1)))
                        : H5Oopen(fileId, path, H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Oclose(objectId);
                }
            });
        return objectId;
    }

    public int deleteObject(long fileId, String path)
    {
        checkMaxLength(path);
        H5Ldelete(fileId, path, H5P_DEFAULT);
        return 0;
    }

    public int copyObject(long srcFileId, String srcPath, long dstFileId, String dstPath)
    {
        checkMaxLength(srcPath);
        checkMaxLength(dstPath);
        final int success = 0;
        H5Ocopy(srcFileId, srcPath, dstFileId, dstPath, H5P_DEFAULT, lcplCreateIntermediateGroups);
        return success;
    }

    public int moveLink(long fileId, String srcLinkPath, String dstLinkPath)
    {
        checkMaxLength(srcLinkPath);
        checkMaxLength(dstLinkPath);
        final int success = 0;
        H5Lmove(fileId, srcLinkPath, fileId, dstLinkPath, lcplCreateIntermediateGroups,
                H5P_DEFAULT);
        return success;
    }

    //
    // Group
    //

    public void createGroup(long fileId, String groupName)
    {
        checkMaxLength(groupName);
        final long groupId =
                H5Gcreate(fileId, groupName, lcplCreateIntermediateGroups, H5P_DEFAULT, H5P_DEFAULT);
        H5Gclose(groupId);
    }

    public void createOldStyleGroup(long fileId, String groupName, int sizeHint,
            ICleanUpRegistry registry)
    {
        checkMaxLength(groupName);
        final long gcplId = H5Pcreate(H5P_GROUP_CREATE);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Pclose(gcplId);
                }
            });
        H5Pset_local_heap_size_hint(gcplId, sizeHint);
        final long groupId =
                H5Gcreate(fileId, groupName, lcplCreateIntermediateGroups, gcplId, H5P_DEFAULT);
        H5Gclose(groupId);
    }

    public void createNewStyleGroup(long fileId, String groupName, int maxCompact, int minDense,
            ICleanUpRegistry registry)
    {
        checkMaxLength(groupName);
        final long gcplId = H5Pcreate(H5P_GROUP_CREATE);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Pclose(gcplId);
                }
            });
        H5Pset_link_phase_change(gcplId, maxCompact, minDense);
        final long groupId =
                H5Gcreate(fileId, groupName, lcplCreateIntermediateGroups, gcplId, H5P_DEFAULT);
        H5Gclose(groupId);
    }

    public long openGroup(long fileId, String path, ICleanUpRegistry registry)
    {
        checkMaxLength(path);
        final long groupId = isReference(path) ? H5Oopen_by_addr(fileId, Long.parseLong(path.substring(1)))
                : H5Gopen(fileId, path, H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Gclose(groupId);
                }
            });
        return groupId;
    }

    public long getNumberOfGroupMembers(long fileId, String path, ICleanUpRegistry registry)
    {
        checkMaxLength(path);
        final long groupId = H5Gopen(fileId, path, H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Gclose(groupId);
                }
            });
        return H5Gget_info(groupId).nlinks;
    }

    public boolean existsAttribute(final long objectId, final String attributeName)
    {
        checkMaxLength(attributeName);
        return H5Aexists(objectId, attributeName);
    }

    public boolean exists(final long fileId, final String linkName)
    {
        checkMaxLength(linkName);
        return HDFHelper.H5Lexists(fileId, linkName, H5P_DEFAULT);
    }

    public HDF5LinkInformation getLinkInfo(final long fileId, final String objectName,
            boolean exceptionIfNonExistent)
    {
        checkMaxLength(objectName);
        if ("/".equals(objectName))
        {
            return HDF5LinkInformation.ROOT_LINK_INFO;
        }
        final String[] lname = new String[2];
        final int typeId = HDFHelper.H5Lget_link_info(fileId, objectName, lname, exceptionIfNonExistent);
        return HDF5LinkInformation.create(objectName, typeId, lname[1], lname[0]);
    }

    public HDF5ObjectType getLinkTypeInfo(final long fileId, final String objectName,
            boolean exceptionWhenNonExistent)
    {
        checkMaxLength(objectName);
        if ("/".equals(objectName))
        {
            return HDF5ObjectType.GROUP;
        }
        final int typeId = HDFHelper.H5Lget_link_info(fileId, objectName, null, exceptionWhenNonExistent);
        return HDF5CommonInformation.objectTypeIdToObjectType(typeId);
    }

    public HDF5ObjectInformation getObjectInfo(final long fileId, final String objectName,
            boolean exceptionWhenNonExistent)
    {
        checkMaxLength(objectName);
        final H5O_info_t info = HDFHelper.H5Oget_info_by_name(fileId, objectName, exceptionWhenNonExistent);
        return new HDF5ObjectInformation(objectName,
                HDF5CommonInformation.objectTypeIdToObjectType(info.type), info);
    }

    public int getObjectTypeId(final long fileId, final String objectName,
            boolean exceptionWhenNonExistent)
    {
        checkMaxLength(objectName);
        if ("/".equals(objectName))
        {
            return H5O_TYPE_GROUP;
        }
        return HDFHelper.H5Oget_info_by_name(fileId, objectName, exceptionWhenNonExistent).type;
    }

    public HDF5ObjectType getObjectTypeInfo(final long fileId, final String objectName,
            boolean exceptionWhenNonExistent)
    {
        return HDF5CommonInformation.objectTypeIdToObjectType(getObjectTypeId(fileId, objectName,
                exceptionWhenNonExistent));
    }

    public String[] getGroupMembers(final long fileId, final String groupName)
    {
        checkMaxLength(groupName);
        final ICallableWithCleanUp<String[]> dataDimensionRunnable =
                new ICallableWithCleanUp<String[]>()
                    {
                        @Override
                        public String[] call(ICleanUpRegistry registry)
                        {
                            final long groupId = openGroup(fileId, groupName, registry);
                            final long nLong = H5Gget_info(groupId).nlinks;
                            final int n = (int) nLong;
                            if (n != nLong)
                            {
                                throw new HDF5JavaException(
                                        "Number of group members is too large (n=" + nLong + ")");
                            }
                            final String[] names = new String[n];
                            HDFHelper.H5Lget_link_names_all(groupId, ".", names);
                            return names;
                        }
                    };
        return runner.call(dataDimensionRunnable);
    }

    public List<HDF5LinkInformation> getGroupMemberLinkInfo(final long fileId,
            final String groupName, final boolean includeInternal,
            final String houseKeepingNameSuffix)
    {
        checkMaxLength(groupName);
        final ICallableWithCleanUp<List<HDF5LinkInformation>> dataDimensionRunnable =
                new ICallableWithCleanUp<List<HDF5LinkInformation>>()
                    {
                        @Override
                        public List<HDF5LinkInformation> call(ICleanUpRegistry registry)
                        {
                            final long groupId = openGroup(fileId, groupName, registry);
                            final long nLong = H5Gget_info(groupId).nlinks;
                            final int n = (int) nLong;
                            if (n != nLong)
                            {
                                throw new HDF5JavaException(
                                        "Number of group members is too large (n=" + nLong + ")");
                            }
                            final String[] names = new String[n];
                            final String[] linkFilenames = new String[n];
                            final String[] linkTargets = new String[n];
                            final int[] types = new int[n];
                            HDFHelper.H5Lget_link_info_all(groupId, ".", names, types, linkFilenames, linkTargets);
                            final String superGroupName =
                                    (groupName.equals("/") ? "/" : groupName + "/");
                            final List<HDF5LinkInformation> info =
                                    new LinkedList<HDF5LinkInformation>();
                            for (int i = 0; i < n; ++i)
                            {
                                if (includeInternal
                                        || HDF5Utils.isInternalName(names[i],
                                                houseKeepingNameSuffix) == false)
                                {
                                    info.add(HDF5LinkInformation.create(superGroupName + names[i],
                                            types[i], linkFilenames[i], linkTargets[i]));
                                }
                            }
                            return info;
                        }
                    };
        return runner.call(dataDimensionRunnable);
    }

    public List<HDF5LinkInformation> getGroupMemberTypeInfo(final long fileId,
            final String groupName, final boolean includeInternal,
            final String houseKeepingNameSuffix)
    {
        checkMaxLength(groupName);
        final ICallableWithCleanUp<List<HDF5LinkInformation>> dataDimensionRunnable =
                new ICallableWithCleanUp<List<HDF5LinkInformation>>()
                    {
                        @Override
                        public List<HDF5LinkInformation> call(ICleanUpRegistry registry)
                        {
                            final long groupId = openGroup(fileId, groupName, registry);
                            final long nLong = H5Gget_info(groupId).nlinks;
                            final int n = (int) nLong;
                            if (n != nLong)
                            {
                                throw new HDF5JavaException(
                                        "Number of group members is too large (n=" + nLong + ")");
                            }
                            final String[] names = new String[n];
                            final int[] types = new int[n];
                            HDFHelper.H5Lget_link_info_all(groupId, ".", names, types, null, null);
                            final String superGroupName =
                                    (groupName.equals("/") ? "/" : groupName + "/");
                            final List<HDF5LinkInformation> info =
                                    new LinkedList<HDF5LinkInformation>();
                            for (int i = 0; i < n; ++i)
                            {
                                if (includeInternal
                                        || HDF5Utils.isInternalName(names[i],
                                                houseKeepingNameSuffix) == false)
                                {
                                    info.add(HDF5LinkInformation.create(superGroupName + names[i],
                                            types[i], null, null));
                                }
                            }
                            return info;
                        }
                    };
        return runner.call(dataDimensionRunnable);
    }

    //
    // Link
    //

    public void createHardLink(long fileId, String objectName, String linkName)
    {
        checkMaxLength(objectName);
        checkMaxLength(linkName);
        H5Lcreate_hard(fileId, objectName, fileId, linkName, lcplCreateIntermediateGroups,
                H5P_DEFAULT);
    }

    public void createSoftLink(long fileId, String linkName, String targetPath)
    {
        checkMaxLength(linkName);
        checkMaxLength(targetPath);
        H5Lcreate_soft(targetPath, fileId, linkName, lcplCreateIntermediateGroups, H5P_DEFAULT);
    }

    public void createExternalLink(long fileId, String linkName, String targetFileName,
            String targetPath)
    {
        checkMaxLength(linkName);
        checkMaxLength(targetFileName);
        checkMaxLength(targetPath);
        H5Lcreate_external(targetFileName, targetPath, fileId, linkName,
                lcplCreateIntermediateGroups, H5P_DEFAULT);
    }

    //
    // Data Set
    //

    public void writeStringVL(long dataSetId, long dataTypeId, String[] value)
    {
        H5DwriteVL(dataSetId, dataTypeId, H5S_ALL, H5S_ALL, H5P_DEFAULT, value);
    }

    public void writeStringVL(long dataSetId, long dataTypeId, long memorySpaceId, long fileSpaceId,
            String[] value)
    {
        H5DwriteVL(dataSetId, dataTypeId, memorySpaceId, fileSpaceId, H5P_DEFAULT, value);
    }

    public long createDataSet(long fileId, long[] dimensions, long[] chunkSizeOrNull, long dataTypeId,
            HDF5AbstractStorageFeatures compression, String dataSetName, HDF5StorageLayout layout,
            ICleanUpRegistry registry)
    {
        checkMaxLength(dataSetName);
        final long dataSpaceId =
                H5Screate_simple(dimensions.length, dimensions,
                        createMaxDimensions(dimensions, (layout == HDF5StorageLayout.CHUNKED)));
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Sclose(dataSpaceId);
                }
            });
        final long dataSetCreationPropertyListId;
        if (layout == HDF5StorageLayout.CHUNKED && chunkSizeOrNull != null)
        {
            dataSetCreationPropertyListId = createDataSetCreationPropertyList(registry);
            setChunkedLayout(dataSetCreationPropertyListId, chunkSizeOrNull);
            if (compression.isScaling())
            {
                final int classTypeId = getClassType(dataTypeId);
                assert compression.isCompatibleWithDataClass(classTypeId);
                if (classTypeId == H5T_INTEGER)
                {
                    H5Pset_scaleoffset(dataSetCreationPropertyListId, H5Z_SO_INT,
                            compression.getScalingFactor());
                } else if (classTypeId == H5T_FLOAT)
                {
                    H5Pset_scaleoffset(dataSetCreationPropertyListId, H5Z_SO_FLOAT_DSCALE,
                            compression.getScalingFactor());
                }
            }
            if (compression.isShuffleBeforeDeflate())
            {
                setShuffle(dataSetCreationPropertyListId);
            }
            if (compression.isDeflating())
            {
                setDeflate(dataSetCreationPropertyListId, compression.getDeflateLevel());
            }
        } else if (layout == HDF5StorageLayout.COMPACT)
        {
            dataSetCreationPropertyListId =
                    dataSetCreationPropertyListCompactStorageLayoutFileTimeAlloc;
        } else
        {
            dataSetCreationPropertyListId = dataSetCreationPropertyListFillTimeAlloc;
        }
        final long dataSetId =
                H5Dcreate(fileId, dataSetName, dataTypeId, dataSpaceId,
                        lcplCreateIntermediateGroups, dataSetCreationPropertyListId, H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Dclose(dataSetId);
                }
            });

        return dataSetId;
    }

    public HDF5DataSet createDataSetDetached(HDF5BaseWriter baseWriter, long[] dimensions, long[] chunkSizeOrNull, long dataTypeId,
            HDF5AbstractStorageFeatures compression, String dataSetName, HDF5StorageLayout layout,
            ICleanUpRegistry registry)
    {
        checkMaxLength(dataSetName);
        final long[] maxDimensions = createMaxDimensions(dimensions, (layout == HDF5StorageLayout.CHUNKED)); 
        final long dataSpaceId =
                H5Screate_simple(dimensions.length, dimensions, maxDimensions);
        final long dataSetCreationPropertyListId;
        if (layout == HDF5StorageLayout.CHUNKED && chunkSizeOrNull != null)
        {
            dataSetCreationPropertyListId = createDataSetCreationPropertyList(registry);
            setChunkedLayout(dataSetCreationPropertyListId, chunkSizeOrNull);
            if (compression.isScaling())
            {
                final int classTypeId = getClassType(dataTypeId);
                assert compression.isCompatibleWithDataClass(classTypeId);
                if (classTypeId == H5T_INTEGER)
                {
                    H5Pset_scaleoffset(dataSetCreationPropertyListId, H5Z_SO_INT,
                            compression.getScalingFactor());
                } else if (classTypeId == H5T_FLOAT)
                {
                    H5Pset_scaleoffset(dataSetCreationPropertyListId, H5Z_SO_FLOAT_DSCALE,
                            compression.getScalingFactor());
                }
            }
            if (compression.isShuffleBeforeDeflate())
            {
                setShuffle(dataSetCreationPropertyListId);
            }
            if (compression.isDeflating())
            {
                setDeflate(dataSetCreationPropertyListId, compression.getDeflateLevel());
            }
        } else if (layout == HDF5StorageLayout.COMPACT)
        {
            dataSetCreationPropertyListId =
                    dataSetCreationPropertyListCompactStorageLayoutFileTimeAlloc;
        } else
        {
            dataSetCreationPropertyListId = dataSetCreationPropertyListFillTimeAlloc;
        }
        final long dataSetId =
                H5Dcreate(baseWriter.fileId, dataSetName, dataTypeId, dataSpaceId,
                        lcplCreateIntermediateGroups, dataSetCreationPropertyListId, H5P_DEFAULT);

        return new HDF5DataSet(baseWriter, dataSetName, dataSetId, dataSpaceId, dimensions, 
                    maxDimensions, layout, true);
    }

    public HDF5DataSetTemplate createDataSetTemplateLowLevel(long fileId, long[] dimensions,
            long[] chunkSizeOrNull, long dataTypeId, HDF5AbstractStorageFeatures compression,
            HDF5StorageLayout layout, FileFormatVersionBounds fileFormat)
    {
        final long[] maxDimensions = createMaxDimensions(dimensions, (layout == HDF5StorageLayout.CHUNKED));
        final long dataSpaceId =
                H5Screate_simple(dimensions.length, dimensions, maxDimensions);
        final long dataSetCreationPropertyListId;
        final boolean closeCreationPropertyListId;
        if (layout == HDF5StorageLayout.CHUNKED && chunkSizeOrNull != null)
        {
            dataSetCreationPropertyListId = createDataSetCreationPropertyList(null);
            closeCreationPropertyListId = true;
            setChunkedLayout(dataSetCreationPropertyListId, chunkSizeOrNull);
            if (compression.isScaling())
            {
                final int classTypeId = getClassType(dataTypeId);
                assert compression.isCompatibleWithDataClass(classTypeId);
                if (classTypeId == H5T_INTEGER)
                {
                    H5Pset_scaleoffset(dataSetCreationPropertyListId, H5Z_SO_INT,
                            compression.getScalingFactor());
                } else if (classTypeId == H5T_FLOAT)
                {
                    H5Pset_scaleoffset(dataSetCreationPropertyListId, H5Z_SO_FLOAT_DSCALE,
                            compression.getScalingFactor());
                }
            }
            if (compression.isShuffleBeforeDeflate())
            {
                setShuffle(dataSetCreationPropertyListId);
            }
            if (compression.isDeflating())
            {
                setDeflate(dataSetCreationPropertyListId, compression.getDeflateLevel());
            }
        } else if (layout == HDF5StorageLayout.COMPACT)
        {
            dataSetCreationPropertyListId =
                    dataSetCreationPropertyListCompactStorageLayoutFileTimeAlloc;
            closeCreationPropertyListId = false;
        } else
        {
            dataSetCreationPropertyListId = dataSetCreationPropertyListFillTimeAlloc;
            closeCreationPropertyListId = false;
        }

        return new HDF5DataSetTemplate(dataSpaceId, dataSetCreationPropertyListId,
                closeCreationPropertyListId, dataTypeId, dimensions, maxDimensions, layout);
    }

    public long createDataSetSimple(long fileId, long dataTypeId, long dataSpaceId, 
            long dataSetCreationPropertyListId, 
            String dataSetName, ICleanUpRegistry registryOrNull)
    {
        final long dataSetId =
                H5Dcreate(fileId, dataSetName, dataTypeId, dataSpaceId,
                        lcplCreateIntermediateGroups, dataSetCreationPropertyListId,
                        H5P_DEFAULT);
        if (registryOrNull != null)
        {
            registryOrNull.registerCleanUp(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        H5Dclose(dataSetId);
                    }
                });
    
        }
        return dataSetId;
    }

    private long createDataSetCreationPropertyList(ICleanUpRegistry registry)
    {
        final long dataSetCreationPropertyListId = H5Pcreate(H5P_DATASET_CREATE);
        if (registry != null)
        {
            registry.registerCleanUp(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        H5Pclose(dataSetCreationPropertyListId);
                    }
                });
        }
        H5Pset_fill_time(dataSetCreationPropertyListId, H5D_FILL_TIME_ALLOC);
        return dataSetCreationPropertyListId;
    }

    /**
     * Returns one of: COMPACT, CHUNKED, CONTIGUOUS.
     */
    public HDF5StorageLayout getLayout(long dataSetId, ICleanUpRegistry registry)
    {
        final long dataSetCreationPropertyListId = getCreationPropertyList(dataSetId, registry);
        final int layoutId = H5Pget_layout(dataSetCreationPropertyListId);
        if (layoutId == H5D_COMPACT)
        {
            return HDF5StorageLayout.COMPACT;
        } else if (layoutId == H5D_CHUNKED)
        {
            return HDF5StorageLayout.CHUNKED;
        } else
        {
            return HDF5StorageLayout.CONTIGUOUS;
        }
    }

    private long getCreationPropertyList(long dataSetId, ICleanUpRegistry registry)
    {
        final long dataSetCreationPropertyListId = H5Dget_create_plist(dataSetId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Pclose(dataSetCreationPropertyListId);
                }
            });
        return dataSetCreationPropertyListId;
    }

    private static final long[] createMaxDimensions(long[] dimensions, boolean unlimited)
    {
        if (unlimited == false)
        {
            return dimensions;
        }
        final long[] maxDimensions = new long[dimensions.length];
        Arrays.fill(maxDimensions, H5S_UNLIMITED);
        return maxDimensions;
    }

    private void setChunkedLayout(long dscpId, long[] chunkSize)
    {
        assert dscpId >= 0;

        H5Pset_layout(dscpId, H5D_CHUNKED);
        H5Pset_chunk(dscpId, chunkSize.length, chunkSize);
    }

    private void setShuffle(long dscpId)
    {
        assert dscpId >= 0;

        H5Pset_shuffle(dscpId);
    }

    private void setDeflate(long dscpId, int deflateLevel)
    {
        assert dscpId >= 0;
        assert deflateLevel >= 0;

        H5Pset_deflate(dscpId, deflateLevel);
    }

    public long createScalarDataSet(long fileId, long dataTypeId, String dataSetName,
            boolean compactLayout, ICleanUpRegistry registry)
    {
        checkMaxLength(dataSetName);
        final long dataSpaceId = H5Screate(H5S_SCALAR);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Sclose(dataSpaceId);
                }
            });
        final long dataSetId =
                H5Dcreate(
                        fileId,
                        dataSetName,
                        dataTypeId,
                        dataSpaceId,
                        lcplCreateIntermediateGroups,
                        compactLayout ? dataSetCreationPropertyListCompactStorageLayoutFileTimeAlloc
                                : dataSetCreationPropertyListFillTimeAlloc,
                        H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Dclose(dataSetId);
                }
            });
        return dataSetId;
    }

    public long openDataSet(long fileId, String path, ICleanUpRegistry registry)
    {
        checkMaxLength(path);
        final long dataSetId = isReference(path) ? H5Oopen_by_addr(fileId, Long.parseLong(path.substring(1)))
                : H5Dopen(fileId, path, H5P_DEFAULT);
        if (registry != null)
        {
            registry.registerCleanUp(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        H5Dclose(dataSetId);
                    }
                });
        }
        return dataSetId;
    }

    boolean isReference(String path)
    {
        return autoDereference && (path.charAt(0) == '\0');
    }

    public long openAndExtendDataSet(long fileId, String path, FileFormatVersionBounds fileFormat,
            long[] newDimensions, boolean overwriteMode, ICleanUpRegistry registry)
            throws HDF5JavaException
    {
        checkMaxLength(path);
        final long dataSetId =
                isReference(path) ? H5Rdereference(fileId, H5P_DEFAULT, H5R_OBJECT, HDFNativeData.longToByte(Long.parseLong(path.substring(1))))
                        : H5Dopen(fileId, path, H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Dclose(dataSetId);
                }
            });
        final long dataSpaceId = getDataSpaceForDataSet(dataSetId, registry);
        final int rank = getDataSpaceRank(dataSpaceId);
        final long[][] dimsMaxDims = getDataSpaceDimensionsAndMaxDimensions(dataSpaceId, rank);
        final long[] dataDimensions = dimsMaxDims[0];
        final long[] maxDimensions = dimsMaxDims[1];
        final HDF5StorageLayout layout = getLayout(dataSetId, registry);
        extendDataSet(dataSetId, dataSpaceId, rank, layout, dataDimensions, newDimensions, maxDimensions,
                overwriteMode, registry);
        return dataSetId;
    }

    public boolean extendDataSet(HDF5DataSet dataSet, long[] newDimensions,
            boolean overwriteMode, ICleanUpRegistry registry)
                    throws HDF5SpaceRankMismatch, HDF5JavaException
    {
        return extendDataSet(dataSet.getDataSetId(), dataSet.getDataSpaceId(),
                dataSet.getRank(), dataSet.getLayout(), dataSet.getDimensions(), newDimensions,
                dataSet.getMaxDimensions(), overwriteMode, registry);
    }

    public boolean extendDataSet(long dataSetId, long dataSpaceId, int rank,
            HDF5StorageLayout layout, long[] oldDimensions, long[] newDimensions,
            long[] maxDimensions, boolean overwriteMode, ICleanUpRegistry registry)
                    throws HDF5SpaceRankMismatch, HDF5JavaException
    {
        checkRank(rank, newDimensions.length);
        if (Arrays.equals(oldDimensions, newDimensions) == false)
        {
            if (layout == HDF5StorageLayout.CHUNKED)
            {
                // Safety check. JHDF5 creates CHUNKED data sets always with unlimited max
                // dimensions but we may have to work on a file we haven't created.
                if (areDimensionsInBounds(newDimensions, maxDimensions))
                {
                    setDataSetExtentChunked(dataSetId,
                            computeNewDimensions(oldDimensions, newDimensions, overwriteMode));
                    return true;
                } else
                {
                    throw new HDF5JavaException("New data set dimensions are out of bounds.");
                }
            } else if (overwriteMode)
            {
                throw new HDF5JavaException("Cannot change dimensions on non-extendable data set.");
            } else
            {
                final long dataTypeId = getDataTypeForDataSet(dataSetId, registry);
                if (getClassType(dataTypeId) == H5T_ARRAY)
                {
                    throw new HDF5JavaException("Cannot partially overwrite array type.");
                }
                if (HDF5Utils.isInBounds(oldDimensions, newDimensions) == false)
                {
                    throw new HDF5JavaException("New data set dimensions are out of bounds.");
                }
            }
        }
        return false;
    }

    public boolean extendDataSet(HDF5DataSet dataSet, long[] newDimensions,
            boolean overwriteMode) throws HDF5JavaException
    {
        final long dataSetId = dataSet.getDataSetId();
        final long[] oldDimensions = dataSet.getDimensions();
        if (Arrays.equals(oldDimensions, newDimensions) == false)
        {
            final HDF5StorageLayout layout = dataSet.getLayout();
            final long[] maxDimensions = dataSet.getMaxDimensions();
            if (layout == HDF5StorageLayout.CHUNKED)
            {
                // Safety check. JHDF5 creates CHUNKED data sets always with unlimited max
                // dimensions but we may have to work on a file we haven't created.
                if (areDimensionsInBounds(newDimensions, maxDimensions))
                {
                    setDataSetExtentChunked(dataSetId,
                            computeNewDimensions(oldDimensions, newDimensions, overwriteMode));
                    return true;
                } else
                {
                    throw new HDF5JavaException("New data set dimensions are out of bounds.");
                }
            } else if (overwriteMode)
            {
                throw new HDF5JavaException("Cannot change dimensions on non-extendable data set.");
            } else
            {
                long dataTypeId = dataSet.getDataTypeId();
                if (getClassType(dataTypeId) == H5T_ARRAY)
                {
                    throw new HDF5JavaException("Cannot partially overwrite array type.");
                }
                if (HDF5Utils.isInBounds(oldDimensions, newDimensions) == false)
                {
                    throw new HDF5JavaException("New data set dimensions are out of bounds.");
                }
            }
        }
        return false;
    }

    long[] computeNewDimensions(long[] oldDimensions, long[] newDimensions,
            boolean cutDownExtendIfNecessary)
    {
        if (cutDownExtendIfNecessary)
        {
            return newDimensions;
        } else
        {
            final long[] newUncutDimensions = new long[oldDimensions.length];
            for (int i = 0; i < newUncutDimensions.length; ++i)
            {
                newUncutDimensions[i] = Math.max(oldDimensions[i], newDimensions[i]);
            }
            return newUncutDimensions;
        }
    }

    void checkRank(int rankExpected, int rankFound) throws HDF5SpaceRankMismatch
    {
        assert rankExpected >= 0;
        assert rankFound >= 0;

        if (rankExpected != rankFound)
        {
            throw new HDF5SpaceRankMismatch(rankExpected, rankFound);
        }
    }

    /**
     * Checks whether the given <var>dimensions</var> are in bounds for <var>dataSetId</var>.
     */
    private boolean areDimensionsInBounds(final long[] dimensions, final long[] maxDimensions)
    {
        if (dimensions.length != maxDimensions.length) // Actually an error condition
        {
            return false;
        }

        for (int i = 0; i < dimensions.length; ++i)
        {
            if (maxDimensions[i] != H5S_UNLIMITED && dimensions[i] > maxDimensions[i])
            {
                return false;
            }
        }
        return true;
    }

    public void setDataSetExtentChunked(long dataSetId, long[] dimensions)
    {
        assert dataSetId >= 0;
        assert dimensions != null;

        H5Dset_extent(dataSetId, dimensions);
    }

    public void readDataSetNonNumeric(long dataSetId, long nativeDataTypeId, byte[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
    }

    public void readDataSetNonNumeric(long dataSetId, long nativeDataTypeId, long memorySpaceId,
            long fileSpaceId, byte[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, memorySpaceId, fileSpaceId, H5P_DEFAULT, data);
    }

    public void readDataSetString(long dataSetId, long nativeDataTypeId, String[] data)
    {
        H5Dread_string(dataSetId, nativeDataTypeId, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
    }

    public void readDataSetString(long dataSetId, long nativeDataTypeId, long memorySpaceId,
            long fileSpaceId, String[] data)
    {
        H5Dread_string(dataSetId, nativeDataTypeId, memorySpaceId, fileSpaceId, H5P_DEFAULT, data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, byte[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, H5S_ALL, H5S_ALL, numericConversionXferPropertyListID,
                data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, short[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, H5S_ALL, H5S_ALL, numericConversionXferPropertyListID,
                data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, int[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, H5S_ALL, H5S_ALL, numericConversionXferPropertyListID,
                data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, long[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, H5S_ALL, H5S_ALL, numericConversionXferPropertyListID,
                data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, float[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, H5S_ALL, H5S_ALL, numericConversionXferPropertyListID,
                data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, double[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, H5S_ALL, H5S_ALL, numericConversionXferPropertyListID,
                data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, long memorySpaceId,
            long fileSpaceId, byte[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, memorySpaceId, fileSpaceId,
                numericConversionXferPropertyListID, data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, long memorySpaceId,
            long fileSpaceId, short[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, memorySpaceId, fileSpaceId,
                numericConversionXferPropertyListID, data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, long memorySpaceId,
            long fileSpaceId, int[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, memorySpaceId, fileSpaceId,
                numericConversionXferPropertyListID, data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, long memorySpaceId,
            long fileSpaceId, long[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, memorySpaceId, fileSpaceId,
                numericConversionXferPropertyListID, data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, long memorySpaceId,
            long fileSpaceId, float[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, memorySpaceId, fileSpaceId,
                numericConversionXferPropertyListID, data);
    }

    public void readDataSet(long dataSetId, long nativeDataTypeId, long memorySpaceId,
            long fileSpaceId, double[] data)
    {
        H5Dread(dataSetId, nativeDataTypeId, memorySpaceId, fileSpaceId,
                numericConversionXferPropertyListID, data);
    }

    public void readDataSetVL(long dataSetId, long dataTypeId, String[] data)
    {
        H5DreadVL(dataSetId, dataTypeId, H5S_ALL, H5S_ALL, H5P_DEFAULT, data);
        replaceNullWithEmptyString(data);
    }

    public void readDataSetVL(long dataSetId, long dataTypeId, long memorySpaceId, long fileSpaceId,
            String[] data)
    {
        H5DreadVL(dataSetId, dataTypeId, memorySpaceId, fileSpaceId, H5P_DEFAULT, data);
        replaceNullWithEmptyString(data);
    }

    // A fixed-length string array returns uninitialized strings as "", a variable-length string as
    // null. We don't want the application programmer to have to be aware of this difference,
    // thus we replace null with "" here.
    private void replaceNullWithEmptyString(String[] data)
    {
        for (int i = 0; i < data.length; ++i)
        {
            if (data[i] == null)
            {
                data[i] = "";
            }
        }
    }

    //
    // Attribute
    //

    public long createAttribute(long locationId, String attributeName, long dataTypeId,
            long dataSpaceIdOrMinusOne, ICleanUpRegistry registry)
    {
        checkMaxLength(attributeName);
        final long dataSpaceId =
                (dataSpaceIdOrMinusOne == -1) ? H5Screate(H5S_SCALAR) : dataSpaceIdOrMinusOne;
        if (dataSpaceIdOrMinusOne == -1)
        {
            registry.registerCleanUp(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        H5Sclose(dataSpaceId);
                    }
                });
        }
        final long attCreationPlistId;
        if (useUTF8CharEncoding)
        {
            attCreationPlistId = H5Pcreate(H5P_ATTRIBUTE_CREATE);
            setCharacterEncodingCreationPropertyList(attCreationPlistId, CharacterEncoding.UTF8);
        } else
        {
            attCreationPlistId = H5P_DEFAULT;
        }
        final long attributeId =
                H5Acreate(locationId, attributeName, dataTypeId, dataSpaceId, attCreationPlistId,
                        H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Aclose(attributeId);
                }
            });
        return attributeId;
    }

    public int deleteAttribute(long locationId, String attributeName)
    {
        checkMaxLength(attributeName);
        final int success = H5Adelete(locationId, attributeName);
        return success;
    }

    public long openAttribute(long locationId, String attributeName, ICleanUpRegistry registry)
    {
        checkMaxLength(attributeName);
        final long attributeId = H5Aopen(locationId, attributeName, H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Aclose(attributeId);
                }
            });
        return attributeId;
    }

    public List<String> getAttributeNames(long locationId, ICleanUpRegistry registry)
    {
        final H5O_info_t info = H5Oget_info(locationId);
        final int numberOfAttributes = (int) info.num_attrs;
        final List<String> attributeNames = new LinkedList<String>();
        for (int i = 0; i < numberOfAttributes; ++i)
        {
            final long attributeId =
                    H5Aopen_by_idx(locationId, ".", H5_INDEX_NAME, H5_ITER_NATIVE, (long) i, H5P_DEFAULT, H5P_DEFAULT);
            registry.registerCleanUp(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        H5Aclose(attributeId);
                    }
                });
            attributeNames.add(H5Aget_name(attributeId));
        }
        return attributeNames;
    }

    public byte[] readAttributeAsByteArray(long attributeId, long dataTypeId, int length)
    {
        final byte[] data = new byte[length];
        H5Aread(attributeId, dataTypeId, data);
        return data;
    }

    public short[] readAttributeAsShortArray(long attributeId, long dataTypeId, int length)
    {
        final short[] data = new short[length];
        H5Aread(attributeId, dataTypeId, data);
        return data;
    }

    public int[] readAttributeAsIntArray(long attributeId, long dataTypeId, int length)
    {
        final int[] data = new int[length];
        H5Aread(attributeId, dataTypeId, data);
        return data;
    }

    public long[] readAttributeAsLongArray(long attributeId, long dataTypeId, int length)
    {
        final long[] data = new long[length];
        H5Aread(attributeId, dataTypeId, data);
        return data;
    }

    public float[] readAttributeAsFloatArray(long attributeId, long dataTypeId, int length)
    {
        final float[] data = new float[length];
        H5Aread(attributeId, dataTypeId, data);
        return data;
    }

    public double[] readAttributeAsDoubleArray(long attributeId, long dataTypeId, int length)
    {
        final double[] data = new double[length];
        H5Aread(attributeId, dataTypeId, data);
        return data;
    }

    public void readAttributeVL(long attributeId, long dataTypeId, String[] data)
    {
        H5AreadVL(attributeId, dataTypeId, data);
    }

    public void writeAttribute(long attributeId, long dataTypeId, byte[] value)
    {
        H5Awrite(attributeId, dataTypeId, value);
    }

    public void writeAttribute(long attributeId, long dataTypeId, short[] value)
    {
        H5Awrite(attributeId, dataTypeId, value);
    }

    public void writeAttribute(long attributeId, long dataTypeId, int[] value)
    {
        H5Awrite(attributeId, dataTypeId, value);
    }

    public void writeAttribute(long attributeId, long dataTypeId, long[] value)
    {
        H5Awrite(attributeId, dataTypeId, value);
    }

    public void writeAttribute(long attributeId, long dataTypeId, float[] value)
    {
        H5Awrite(attributeId, dataTypeId, value);
    }

    public void writeAttribute(long attributeId, long dataTypeId, double[] value)
    {
        H5Awrite(attributeId, dataTypeId, value);
    }

    public void writeAttributeStringVL(long attributeId, long dataTypeId, String[] value)
    {
        H5AwriteVL(attributeId, dataTypeId, value);
    }

    //
    // Data Type
    //

    public long copyDataType(long dataTypeId, ICleanUpRegistry registry)
    {
        final long copiedDataTypeId = H5Tcopy(dataTypeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(copiedDataTypeId);
                }
            });
        return copiedDataTypeId;
    }

    public long createDataTypeVariableString(ICleanUpRegistry registry)
    {
        final long dataTypeId = createDataTypeStringVariableLength();
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        if (useUTF8CharEncoding)
        {
            setCharacterEncodingDataType(dataTypeId, CharacterEncoding.UTF8);
        }
        return dataTypeId;
    }

    private long createDataTypeStringVariableLength()
    {
        long dataTypeId = H5Tcopy(H5T_C_S1);
        H5Tset_size(dataTypeId, H5T_VARIABLE);
        return dataTypeId;
    }

    public long createDataTypeString(int length, ICleanUpRegistry registry)
    {
        assert length > 0;

        final long dataTypeId = H5Tcopy(H5T_C_S1);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        H5Tset_size(dataTypeId, length);
        H5Tset_strpad(dataTypeId, H5T_STR_NULLPAD);
        if (useUTF8CharEncoding)
        {
            setCharacterEncodingDataType(dataTypeId, CharacterEncoding.UTF8);
        }
        return dataTypeId;
    }

    private void setCharacterEncodingDataType(long dataTypeId, CharacterEncoding encoding)
    {
        H5Tset_cset(dataTypeId, encoding.getCValue());
    }

    public long createArrayType(long baseTypeId, int length, ICleanUpRegistry registry)
    {
        final long dataTypeId = H5Tarray_create(baseTypeId, 1, new long[] { length });
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        return dataTypeId;
    }

    public long createArrayType(long baseTypeId, int[] dimensions, ICleanUpRegistry registry)
    {
        final long[] ldims = new long[dimensions.length];
        for (int i = 0; i < ldims.length; ++i)
        {
            ldims[i] = (long) dimensions[i];
        }
        final long dataTypeId = H5Tarray_create(baseTypeId, ldims.length, ldims);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        return dataTypeId;
    }

    private enum EnumSize
    {
        BYTE8, SHORT16, INT32
    }

    public long createDataTypeEnum(String[] names, ICleanUpRegistry registry)
    {
        for (String name : names)
        {
            checkMaxLength(name);
        }
        final EnumSize size =
                (names.length < Byte.MAX_VALUE) ? EnumSize.BYTE8
                        : (names.length < Short.MAX_VALUE) ? EnumSize.SHORT16 : EnumSize.INT32;
        final long baseDataTypeId;
        switch (size)
        {
            case BYTE8:
                baseDataTypeId = H5T_STD_I8LE;
                break;
            case SHORT16:
                baseDataTypeId = H5T_STD_I16LE;
                break;
            case INT32:
                baseDataTypeId = H5T_STD_I32LE;
                break;
            default:
                throw new InternalError();
        }
        final long dataTypeId = H5Tenum_create(baseDataTypeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        switch (size)
        {
            case BYTE8:
                for (byte i = 0; i < names.length; ++i)
                {
                    insertMemberEnum(dataTypeId, names[i], i);
                }
                break;
            case SHORT16:
            {
                final short[] values = getLittleEndianSuccessiveShortValues(names);
                for (short i = 0; i < names.length; ++i)
                {
                    insertMemberEnum(dataTypeId, names[i], values[i]);
                }
                break;
            }
            case INT32:
            {
                final int[] values = getLittleEndianSuccessiveIntValues(names);
                for (int i = 0; i < names.length; ++i)
                {
                    insertMemberEnum(dataTypeId, names[i], values[i]);
                }
                break;
            }
        }
        return dataTypeId;
    }

    private short[] getLittleEndianSuccessiveShortValues(String[] names)
    {
        final short[] values = new short[names.length];
        final boolean swap = (NativeData.getNativeByteOrder() == NativeData.ByteOrder.BIG_ENDIAN);
        for (short i = 0; i < names.length; ++i)
        {
            values[i] = swap ? NativeData.changeByteOrder(i): i;
        }
        return values;
    }

    private int[] getLittleEndianSuccessiveIntValues(String[] names)
    {
        final int[] values = new int[names.length];
        final boolean swap = (NativeData.getNativeByteOrder() == NativeData.ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < names.length; ++i)
        {
            values[i] = swap ? NativeData.changeByteOrder(i): i;
        }
        return values;
    }

    private void insertMemberEnum(long dataTypeId, String name, byte value)
    {
        assert dataTypeId >= 0;
        assert name != null;

        H5Tenum_insert(dataTypeId, name, value);
    }

    private void insertMemberEnum(long dataTypeId, String name, short value)
    {
        assert dataTypeId >= 0;
        assert name != null;

        H5Tenum_insert(dataTypeId, name, value);
    }

    private void insertMemberEnum(long dataTypeId, String name, int value)
    {
        assert dataTypeId >= 0;
        assert name != null;

        H5Tenum_insert(dataTypeId, name, value);
    }

    /** Returns the number of members of an enum type or a compound type. */
    public int getNumberOfMembers(long dataTypeId)
    {
        return H5Tget_nmembers(dataTypeId);
    }

    /**
     * Returns the name of an enum value or compound member for the given <var>index</var>.
     * <p>
     * Must not be called on a <var>dateTypeId</var> that is not an enum or compound type.
     */
    public String getNameForEnumOrCompoundMemberIndex(long dataTypeId, int index)
    {
        return H5Tget_member_name(dataTypeId, index);
    }

    /**
     * Returns the offset of a compound member for the given <var>index</var>.
     * <p>
     * Must not be called on a <var>dateTypeId</var> that is not a compound type.
     */
    public int getOffsetForCompoundMemberIndex(long dataTypeId, int index)
    {
        return (int) H5Tget_member_offset(dataTypeId, index);
    }

    /**
     * Returns the names of an enum value or compound members.
     * <p>
     * Must not be called on a <var>dateTypeId</var> that is not an enum or compound type.
     */
    public String[] getNamesForEnumOrCompoundMembers(long dataTypeId)
    {
        final int len = getNumberOfMembers(dataTypeId);
        final String[] values = new String[len];
        for (int i = 0; i < len; ++i)
        {
            values[i] = H5Tget_member_name(dataTypeId, i);
        }
        return values;
    }

    /**
     * Returns the index of an enum value or compound member for the given <var>name</var>. Works on enum and compound data types.
     */
    public int getIndexForMemberName(long dataTypeId, String name)
    {
        checkMaxLength(name);
        return H5Tget_member_index(dataTypeId, name);
    }

    /**
     * Returns the data type id for a member of a compound data type, specified by index.
     */
    public long getDataTypeForIndex(long compoundDataTypeId, int index, ICleanUpRegistry registry)
    {
        final long memberTypeId = H5Tget_member_type(compoundDataTypeId, index);
        registry.registerCleanUp(new Runnable()
            {

                @Override
                public void run()
                {
                    H5Tclose(memberTypeId);
                }
            });
        return memberTypeId;
    }

    /**
     * Returns the data type id for a member of a compound data type, specified by name.
     */
    public long getDataTypeForMemberName(long compoundDataTypeId, String memberName)
    {
        checkMaxLength(memberName);
        final int index = H5Tget_member_index(compoundDataTypeId, memberName);
        return H5Tget_member_type(compoundDataTypeId, index);
    }

    public Boolean tryGetBooleanValue(final long dataTypeId, final int intValue)
    {
        if (getClassType(dataTypeId) != H5T_ENUM)
        {
            return null;
        }
        final String value = getNameForEnumOrCompoundMemberIndex(dataTypeId, intValue);
        if ("TRUE".equalsIgnoreCase(value))
        {
            return true;
        } else if ("FALSE".equalsIgnoreCase(value))
        {
            return false;
        } else
        {
            return null;
        }
    }

    public long createDataTypeCompound(int lengthInBytes, ICleanUpRegistry registry)
    {
        final long dataTypeId = H5Tcreate(H5T_COMPOUND, lengthInBytes);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        return dataTypeId;
    }

    public long createDataTypeOpaque(int lengthInBytes, String tag, ICleanUpRegistry registry)
    {
        checkMaxLength(tag);
        final long dataTypeId = H5Tcreate(H5T_OPAQUE, lengthInBytes);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        H5Tset_tag(dataTypeId,
                tag.length() > H5T_OPAQUE_TAG_MAX ? tag.substring(0, H5T_OPAQUE_TAG_MAX) : tag);
        return dataTypeId;
    }

    public void commitDataType(long fileId, String name, long dataTypeId)
    {
        checkMaxLength(name);
        H5Tcommit(fileId, name, dataTypeId, lcplCreateIntermediateGroups, H5P_DEFAULT, H5P_DEFAULT);
    }

    public long openDataType(long fileId, String name, ICleanUpRegistry registry)
    {
        checkMaxLength(name);
        final long dataTypeId = isReference(name) ? H5Oopen_by_addr(fileId, Long.parseLong(name.substring(1)))
                : H5Topen(fileId, name, H5P_DEFAULT);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        return dataTypeId;
    }

    public boolean dataTypesAreEqual(long dataTypeId1, long dataTypeId2)
    {
        return H5Tequal(dataTypeId1, dataTypeId2);
    }

    public long getDataTypeForDataSet(long dataSetId, ICleanUpRegistry registry)
    {
        final long dataTypeId = H5Dget_type(dataSetId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        return dataTypeId;
    }

    public long getDataTypeForAttribute(long attributeId, ICleanUpRegistry registry)
    {
        final long dataTypeId = H5Aget_type(attributeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        return dataTypeId;
    }

    public String tryGetOpaqueTag(long dataTypeId)
    {
        return H5Tget_tag(dataTypeId);
    }

    public long getNativeDataType(long dataTypeId, ICleanUpRegistry registry)
    {
        final long nativeDataTypeId = H5Tget_native_type(dataTypeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(nativeDataTypeId);
                }
            });
        return nativeDataTypeId;
    }

    public long getNativeDataTypeForDataSet(long dataSetId, ICleanUpRegistry registry)
    {
        final long dataTypeId = H5Dget_type(dataSetId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        return getNativeDataType(dataTypeId, registry);
    }

    public long getNativeDataTypeForAttribute(long attributeId, ICleanUpRegistry registry)
    {
        final long dataTypeId = H5Aget_type(attributeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(dataTypeId);
                }
            });
        return getNativeDataType(dataTypeId, registry);
    }

    public int getDataTypeSize(long dataTypeId)
    {
        return (int) H5Tget_size(dataTypeId);
    }

    public long getDataTypeSizeLong(long dataTypeId) throws HDF5JavaException
    {
        return H5Tget_size(dataTypeId);
    }

    public boolean isVariableLengthString(long dataTypeId)
    {
        return H5Tis_variable_str(dataTypeId);
    }

    public int getClassType(long dataTypeId)
    {
        return H5Tget_class(dataTypeId);
    }

    public CharacterEncoding getCharacterEncoding(long dataTypeId)
    {
        final int cValue = H5Tget_cset(dataTypeId);
        if (cValue == CharacterEncoding.ASCII.getCValue())
        {
            return CharacterEncoding.ASCII;
        } else if (cValue == CharacterEncoding.UTF8.getCValue())
        {
            return CharacterEncoding.UTF8;
        } else
        {
            throw new HDF5JavaException("Unknown character encoding cValue " + cValue);
        }
    }

    public boolean hasClassType(long dataTypeId, int classTypeId)
    {
        return H5Tdetect_class(dataTypeId, classTypeId);
    }

    public long getBaseDataType(long dataTypeId, ICleanUpRegistry registry)
    {
        final long baseDataTypeId = H5Tget_super(dataTypeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Tclose(baseDataTypeId);
                }
            });
        return baseDataTypeId;
    }

    public boolean getSigned(long dataTypeId)
    {
        return H5Tget_sign(dataTypeId) != H5T_SGN_NONE;
    }

    public String tryGetDataTypePath(long dataTypeId)
    {
        if (dataTypeId < 0 || H5Tcommitted(dataTypeId) == false)
        {
            return null;
        }
        return H5Iget_name(dataTypeId);
    }

    /**
     * Reclaims the variable-length data structures from a compound buffer, if any.
     */
    public void reclaimCompoundVL(HDF5CompoundType<?> type, byte[] buf)
    {
        int[] vlMemberIndices = type.getObjectByteifyer().getVLMemberIndices();
        if (vlMemberIndices.length > 0) // This type has variable-length data members
        {
            HDFHelper.freeCompoundVLStr(buf, type.getRecordSizeInMemory(), vlMemberIndices);
        }
    }

    //
    // Data Space
    //

    public long getDataSpaceForDataSet(long dataSetId, ICleanUpRegistry registry)
    {
        final long dataSpaceId = H5Dget_space(dataSetId);
        if (registry != null)
        {
            registry.registerCleanUp(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        H5Sclose(dataSpaceId);
                    }
                });
        }
        return dataSpaceId;
    }

    public long[] getDataDimensionsForAttribute(final long attributeId, ICleanUpRegistry registry)
    {
        final long dataSpaceId = H5Aget_space(attributeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Sclose(dataSpaceId);
                }
            });
        final long[] dimensions = getDataSpaceDimensions(dataSpaceId);
        return dimensions;
    }

    public long[] getDataDimensions(final long dataSetId, ICleanUpRegistry registry)
    {
        final long dataSpaceId = H5Dget_space(dataSetId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Sclose(dataSpaceId);
                }
            });
        long[] dimensions = getDataSpaceDimensions(dataSpaceId);
        // Ensure backward compatibility with 8.10
        if (HDF5Utils.mightBeEmptyInStorage(dimensions)
                && existsAttribute(dataSetId, HDF5Utils.DATASET_IS_EMPTY_LEGACY_ATTRIBUTE))
        {
            dimensions = new long[dimensions.length];
        }
        return dimensions;
    }

    public int getDataSpaceRank(long dataSpaceId)
    {
        return H5Sget_simple_extent_ndims(dataSpaceId);
    }

    public long[] getDataSpaceDimensions(long dataSpaceId)
    {
        final int rank = H5Sget_simple_extent_ndims(dataSpaceId);
        return getDataSpaceDimensions(dataSpaceId, rank);
    }

    public long[] getDataSpaceDimensions(long dataSpaceId, int rank)
    {
        assert dataSpaceId >= 0;
        assert rank >= 0;

        final long[] dimensions = new long[rank];
        H5Sget_simple_extent_dims(dataSpaceId, dimensions, null);
        return dimensions;
    }

    public long[] getDataSpaceMaxDimensions(long dataSpaceId)
    {
        final int rank = H5Sget_simple_extent_ndims(dataSpaceId);
        return getDataSpaceMaxDimensions(dataSpaceId, rank);
    }

    public long[] getDataSpaceMaxDimensions(long dataSpaceId, int rank)
    {
        assert dataSpaceId >= 0;
        assert rank >= 0;

        final long[] maxDimensions = new long[rank];
        H5Sget_simple_extent_dims(dataSpaceId, null, maxDimensions);
        return maxDimensions;
    }

    public long[][] getDataSpaceDimensionsAndMaxDimensions(long dataSpaceId, int rank)
    {
        final long[][] dimsMaxDims = new long[2][rank];
        H5Sget_simple_extent_dims(dataSpaceId, dimsMaxDims[0], dimsMaxDims[1]);
        return dimsMaxDims;
    }

    /**
     * @param dataSetOrAttributeId The id of either the data set or the attribute to get the rank for.
     * @param isAttribute If <code>true</code>, <var>dataSetOrAttributeId</var> will be interpreted as an attribute, otherwise as a data set.
     */
    public int getRank(final long dataSetOrAttributeId, final boolean isAttribute,
            ICleanUpRegistry registry)
    {
        final long dataSpaceId =
                isAttribute ? H5Aget_space(dataSetOrAttributeId)
                        : H5Dget_space(dataSetOrAttributeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Sclose(dataSpaceId);
                }
            });
        return H5Sget_simple_extent_ndims(dataSpaceId);
    }

    /**
     * @param dataSetOrAttributeId The id of either the data set or the attribute to get the rank for.
     * @param isAttribute If <code>true</code>, <var>dataSetOrAttributeId</var> will be interpreted as an attribute, otherwise as a data set.
     */
    public long[] getDimensions(final long dataSetOrAttributeId, final boolean isAttribute,
            ICleanUpRegistry registry)
    {
        final long dataSpaceId =
                isAttribute ? H5Aget_space(dataSetOrAttributeId)
                        : H5Dget_space(dataSetOrAttributeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Sclose(dataSpaceId);
                }
            });
        final long[] dimensions = new long[H5S_MAX_RANK];
        final int rank = H5Sget_simple_extent_dims(dataSpaceId, dimensions, null);
        final long[] realDimensions = new long[rank];
        System.arraycopy(dimensions, 0, realDimensions, 0, rank);
        return realDimensions;
    }

    /**
     * @param dataSetOrAttributeId The id of either the data set or the attribute to get the dimensions for.
     * @param isAttribute If <code>true</code>, <var>dataSetOrAttributeId</var> will be interpreted as an attribute, otherwise as a data set.
     * @param dataSetInfo The info object to fill.
     */
    public void fillDataDimensions(final long dataSetOrAttributeId, final boolean isAttribute,
            final HDF5DataSetInformation dataSetInfo, ICleanUpRegistry registry)
    {
        final long dataSpaceId =
                isAttribute ? H5Aget_space(dataSetOrAttributeId)
                        : H5Dget_space(dataSetOrAttributeId);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Sclose(dataSpaceId);
                }
            });
        final long[] dimensions = new long[H5S_MAX_RANK];
        final long[] maxDimensions = new long[H5S_MAX_RANK];
        final int rank = H5Sget_simple_extent_dims(dataSpaceId, dimensions, maxDimensions);
        final long[] realDimensions = new long[rank];
        System.arraycopy(dimensions, 0, realDimensions, 0, rank);
        final long[] realMaxDimensions = new long[rank];
        System.arraycopy(maxDimensions, 0, realMaxDimensions, 0, rank);
        dataSetInfo.setDimensions(realDimensions);
        dataSetInfo.setMaxDimensions(realMaxDimensions);
        if (isAttribute == false)
        {
            final long[] chunkSizes = new long[rank];
            final long creationPropertyList =
                    getCreationPropertyList(dataSetOrAttributeId, registry);
            final HDF5StorageLayout layout =
                    HDF5StorageLayout.fromId(H5Pget_layout(creationPropertyList));
            dataSetInfo.setStorageLayout(layout);
            if (layout == HDF5StorageLayout.CHUNKED)
            {
                H5Pget_chunk(creationPropertyList, rank, chunkSizes);
                dataSetInfo.setChunkSizes(MDAbstractArray.toInt(chunkSizes));
            }
        }
    }

    public int[] getArrayDimensions(long arrayTypeId)
    {
        final int rank = H5Tget_array_ndims(arrayTypeId);
        final long[] dims = new long[rank];
        H5Tget_array_dims(arrayTypeId, dims);
        final int[] result = new int[rank];
        for (int i = 0; i < rank; ++i)
        {
            result[i] = (int) dims[i];
        }
        return result;
    }

    public long createScalarDataSpace()
    {
        return H5Screate(H5S_SCALAR);
    }

    public long createSimpleDataSpace(long[] dimensions, ICleanUpRegistry registry)
    {
        final long dataSpaceId = H5Screate_simple(dimensions.length, dimensions, null);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Sclose(dataSpaceId);
                }
            });
        return dataSpaceId;
    }

    public void setHyperslabBlock(long dataSpaceId, long[] start, long[] count)
    {
        assert dataSpaceId >= 0;
        assert start != null;
        assert count != null;

        H5Sselect_hyperslab(dataSpaceId, H5S_SELECT_SET, start, null, count, null);
    }

    //
    // Properties
    //

    private long createLinkCreationPropertyList(boolean createIntermediateGroups,
            ICleanUpRegistry registry)
    {
        final long linkCreationPropertyList = H5Pcreate(H5P_LINK_CREATE);
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Pclose(linkCreationPropertyList);
                }
            });
        if (createIntermediateGroups)
        {
            H5Pset_create_intermediate_group(linkCreationPropertyList, true);
        }
        if (useUTF8CharEncoding)
        {
            setCharacterEncodingCreationPropertyList(linkCreationPropertyList,
                    CharacterEncoding.UTF8);
        }
        return linkCreationPropertyList;
    }

    // Only use with H5P_LINK_CREATE, H5P_ATTRIBUTE_CREATE and H5P_STRING_CREATE property list ids
    private void setCharacterEncodingCreationPropertyList(long creationPropertyList,
            CharacterEncoding encoding)
    {
        H5Pset_char_encoding(creationPropertyList, encoding.getCValue());
    }

    private long createDataSetXferPropertyListAbortOverflow(ICleanUpRegistry registry)
    {
        final long datasetXferPropertyList = HDFHelper.H5Pcreate_xfer_abort_overflow();
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Pclose(datasetXferPropertyList);
                }
            });
        return datasetXferPropertyList;
    }

    private long createDataSetXferPropertyListAbort(ICleanUpRegistry registry)
    {
        final long datasetXferPropertyList = HDFHelper.H5Pcreate_xfer_abort();
        registry.registerCleanUp(new Runnable()
            {
                @Override
                public void run()
                {
                    H5Pclose(datasetXferPropertyList);
                }
            });
        return datasetXferPropertyList;
    }

    //
    // References
    //
    
    private final int BUFLEN = 128;

    String getReferencedObjectName(long objectId, byte[] reference)
    {
        final String[] objectName = new String[1];
        H5Rget_name(objectId, HDF5Constants.H5R_OBJECT, reference, objectName, BUFLEN);
        return objectName[0];
    }

    String getReferencedObjectName(long objectId, long reference)
    {
        final String[] objectName = new String[1];
        H5Rget_name(objectId, HDF5Constants.H5R_OBJECT, 
                HDFHelper.longToByte(new long[] { reference }), objectName, BUFLEN);
        return objectName[0];
    }

    String[] getReferencedObjectNames(long objectId, long[] reference)
    {
        final String[] objectNames = new String[reference.length];
        for (int i = 0; i < reference.length; ++i)
        {
            objectNames[i] = getReferencedObjectName(objectId, reference[i]);
        }
        return objectNames;
    }

    String getReferencedObjectName(long objectId, byte[] references, int ofs)
    {
        final byte[] reference = new byte[HDF5BaseReader.REFERENCE_SIZE_IN_BYTES];
        System.arraycopy(references, ofs, reference, 0, HDF5BaseReader.REFERENCE_SIZE_IN_BYTES);
        return getReferencedObjectName(objectId, reference);
    }

    byte[] createObjectReference(long fileId, String objectPath)
    {
        return H5Rcreate(fileId, objectPath, H5R_OBJECT, -1);
    }

    long[] createObjectReferences(long fileId, String[] objectPaths)
    {
        final long[] references = new long[objectPaths.length];
        for (int i = 0; i < objectPaths.length; ++i)
        {
            references[i] = HDFNativeData.byteToLong(createObjectReference(fileId, objectPaths[i]), 0);
        }
        return references;
    }
}
