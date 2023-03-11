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

import static ch.systemsx.cisd.hdf5.HDF5Utils.HOUSEKEEPING_NAME_SUFFIX_STRINGLENGTH_ATTRIBUTE_NAME;
import static ch.systemsx.cisd.hdf5.HDF5Utils.createAttributeTypeVariantAttributeName;
import static ch.systemsx.cisd.hdf5.HDF5Utils.createObjectTypeVariantAttributeName;
import static ch.systemsx.cisd.hdf5.HDF5Utils.getBooleanDataTypePath;
import static ch.systemsx.cisd.hdf5.HDF5Utils.getDataTypeGroup;
import static ch.systemsx.cisd.hdf5.HDF5Utils.getOneDimensionalArraySize;
import static ch.systemsx.cisd.hdf5.HDF5Utils.getTypeVariantDataTypePath;
import static ch.systemsx.cisd.hdf5.HDF5Utils.getTypeVariantMembersAttributeName;
import static ch.systemsx.cisd.hdf5.HDF5Utils.getVariableLengthStringDataTypePath;
import static ch.systemsx.cisd.hdf5.HDF5Utils.removeInternalNames;
import static hdf.hdf5lib.HDF5Constants.H5S_ALL;
import static hdf.hdf5lib.HDF5Constants.H5T_ARRAY;
import static hdf.hdf5lib.HDF5Constants.H5T_ENUM;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT32;
import static hdf.hdf5lib.HDF5Constants.H5T_STRING;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import hdf.hdf5lib.exceptions.HDF5JavaException;
import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.FileFormatVersionBounds;
import ch.systemsx.cisd.hdf5.cleanup.CleanUpCallable;
import ch.systemsx.cisd.hdf5.cleanup.CleanUpRegistry;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import ch.systemsx.cisd.hdf5.exceptions.HDF5FileNotFoundException;
import ch.systemsx.cisd.hdf5.exceptions.HDF5SpaceRankMismatch;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;
import hdf.hdf5lib.HDF5Constants;

/**
 * Class that provides base methods for reading HDF5 files.
 * 
 * @author Bernd Rinn
 */
class HDF5BaseReader
{

    /** State that this reader / writer is currently in. */
    protected enum State
    {
        CONFIG, OPEN, CLOSED
    }

    /** Control of metadate image generation. */
    static enum MDCImageGeneration 
    { 
        /** Do not generate an MDC image. */
        NO_GENERATE_MDC_IMAGE(false), 
        /** Generate an MDC image if the file already has one. */
        KEEP_MDC_IMAGE(false), 
        /** Generate an MDC image. */
        GENERATE_MDC_IMAGE(true);
        
        final boolean generateImageForNewFile;
        
        public boolean isGenerateImageForNewFile()
        {
            return generateImageForNewFile;
        }

        MDCImageGeneration(boolean generateImageForNewFile)
        {
            this.generateImageForNewFile = generateImageForNewFile;
        }
    };

    /** The size of a reference in bytes. */
    static final int REFERENCE_SIZE_IN_BYTES = 8;

    protected final File hdf5File;

    protected final CleanUpCallable runner;

    protected final CleanUpRegistry fileRegistry;

    protected final boolean performNumericConversions;
    
    protected final boolean hasMDCImage;
    
    protected boolean readOnly;

    /** Map from named data types to ids. */
    private final Map<String, Long> namedDataTypeMap;

    private class DataTypeContainer
    {
        final long typeId;

        final String typePath;

        DataTypeContainer(long typeId, String typePath)
        {
            this.typeId = typeId;
            this.typePath = typePath;
        }
    }

    private final List<DataTypeContainer> namedDataTypeList;

    protected final HDF5 h5;

    protected final long fileId;

    protected final long booleanDataTypeId;

    protected final long variableLengthStringDataTypeId;

    protected final HDF5EnumerationType typeVariantDataType;

    protected State state;

    final String houseKeepingNameSuffix;

    final CharacterEncoding encodingForNewDataSets;
    
    // We keep this reference in order to not have the reader garbage collected and thus
    // closing the file when specialized readers are still open and need access to this base 
    // reader.
    @SuppressWarnings("unused")
    private HDF5Reader myReader;

    HDF5BaseReader(File hdf5File, boolean performNumericConversions, boolean autoDereference,
            FileFormatVersionBounds fileFormat, MDCImageGeneration mdcGenerateImage, boolean overwrite, 
            String preferredHouseKeepingNameSuffix)
    {
        this(hdf5File, performNumericConversions, false, autoDereference, fileFormat, mdcGenerateImage, 
                overwrite,
                preferredHouseKeepingNameSuffix);
    }

    HDF5BaseReader(File hdf5File, boolean performNumericConversions, boolean useUTF8CharEncoding,
            boolean autoDereference, FileFormatVersionBounds fileFormat, MDCImageGeneration mdcGenerateImage, 
            boolean overwrite, String preferredHouseKeepingNameSuffix)
    {
        assert hdf5File != null;
        assert preferredHouseKeepingNameSuffix != null;

        this.readOnly = true;
        this.performNumericConversions = performNumericConversions;
        this.hdf5File = hdf5File.getAbsoluteFile();
        this.runner = new CleanUpCallable();
        this.fileRegistry = CleanUpRegistry.createSynchonized();
        this.namedDataTypeMap = new HashMap<String, Long>();
        this.namedDataTypeList = new ArrayList<DataTypeContainer>();
        this.encodingForNewDataSets =
                useUTF8CharEncoding ? CharacterEncoding.UTF8 : CharacterEncoding.ASCII;
        this.h5 =
                new HDF5(fileRegistry, runner, performNumericConversions, useUTF8CharEncoding,
                        autoDereference);
        this.fileId = openFile(fileFormat, mdcGenerateImage, overwrite);
        this.hasMDCImage = HDFHelper.H5Fhas_mdc_image(fileId);
        this.state = State.OPEN;

        final String houseKeepingNameSuffixFromFileOrNull = tryGetHouseKeepingNameSuffix();
        this.houseKeepingNameSuffix =
                (houseKeepingNameSuffixFromFileOrNull == null) ? preferredHouseKeepingNameSuffix
                        : houseKeepingNameSuffixFromFileOrNull;
        readNamedDataTypes();
        variableLengthStringDataTypeId = openOrCreateVLStringType();
        booleanDataTypeId = openOrCreateBooleanDataType();
        typeVariantDataType = openOrCreateTypeVariantDataType();
    }

    void setMyReader(HDF5Reader myReader)
    {
        this.myReader = myReader;
    }

    void copyObject(String srcPath, long dstFileId, String dstPath)
    {
        final boolean dstIsDir = dstPath.endsWith("/");
        if ("/".equals(srcPath))
        {
            final String dstDir = dstIsDir ? dstPath : dstPath + "/";
            for (String object : getGroupMembers("/"))
            {
                h5.copyObject(fileId, object, dstFileId, dstDir + object);
            }
        } else if (dstIsDir)
        {
            final int idx = srcPath.lastIndexOf('/');
            final String sourceObjectName = srcPath.substring(idx < 0 ? 0 : idx);
            h5.copyObject(fileId, srcPath, dstFileId, dstPath + sourceObjectName);
        } else
        {
            h5.copyObject(fileId, srcPath, dstFileId, dstPath);
        }
    }

    long openFile(FileFormatVersionBounds fileFormat, MDCImageGeneration mdcGenerateImage, boolean overwrite)
    {
        if (hdf5File.exists() == false)
        {
            throw new HDF5FileNotFoundException(hdf5File, "Path does not exit.");
        }
        if (hdf5File.canRead() == false)
        {
            throw new HDF5FileNotFoundException(hdf5File, "Path is not readable.");
        }
        if (hdf5File.isFile() == false)
        {
            throw new HDF5FileNotFoundException(hdf5File, "Path is not a file.");
        }
        if (HDF5Factory.isHDF5File(hdf5File) == false)
        {
            throw new HDF5FileNotFoundException(hdf5File, "Path is not a valid HDF5 file.");
        }
        return h5.openFileReadOnly(hdf5File.getPath(), fileRegistry);
    }

    void checkOpen() throws HDF5JavaException
    {
        if (state != State.OPEN)
        {
            final String msg =
                    "HDF5 file '" + hdf5File.getPath() + "' is "
                            + (state == State.CLOSED ? "closed." : "not opened yet.");
            throw new HDF5JavaException(msg);
        }
    }

    /**
     * Closes this object and the file referenced by this object. This object must not be used after
     * being closed.
     */
    void close()
    {
        synchronized (fileRegistry)
        {
            if (state == State.OPEN)
            {
                fileRegistry.cleanUp(false);
            }
            state = State.CLOSED;
        }
    }

    boolean isClosed()
    {
        return state == State.CLOSED;
    }

    String tryGetHouseKeepingNameSuffix()
    {
        final ICallableWithCleanUp<String> readRunnable = new ICallableWithCleanUp<String>()
            {
                @Override
                public String call(ICleanUpRegistry registry)
                {
                    final long objectId = h5.openObject(fileId, "/", registry);
                    if (h5.existsAttribute(objectId,
                            HDF5Utils.HOUSEKEEPING_NAME_SUFFIX_ATTRIBUTE_NAME))
                    {
                        final int suffixLen =
                                getHousekeepingAttributeExplicitStringLength(objectId, registry);
                        final boolean explicitLengthStored = (suffixLen >= 0);
                        final String rawSuffix =
                                getStringAttribute(objectId, "/",
                                        HDF5Utils.HOUSEKEEPING_NAME_SUFFIX_ATTRIBUTE_NAME,
                                        explicitLengthStored, registry);
                        return explicitLengthStored ? rawSuffix.substring(0, suffixLen) : rawSuffix;
                    } else
                    {
                        return null;
                    }
                }
            };
        return runner.call(readRunnable);
    }
    
    boolean isMDCImageGenerationEnabled()
    {
        return readOnly ? hasMDCImage : h5.isMDCImageGenerationEnabled(fileId);
    }

    public HDF5DataSet openDataSet(final String objectPath)
    {
        final ICallableWithCleanUp<HDF5DataSet> openDataSetCallable =
                new ICallableWithCleanUp<HDF5DataSet>()
                    {
                        @Override
                        public HDF5DataSet call(ICleanUpRegistry registry)
                        {
                            final long dataSetId = h5.openDataSet(fileId, objectPath, null);
                            final HDF5StorageLayout layout = h5.getLayout(dataSetId, registry);
                            final long dataSpaceId = h5.getDataSpaceForDataSet(dataSetId, null);
                            final long[] dimensions = h5.getDataSpaceDimensions(dataSpaceId);
                            final HDF5DataSet dataSet =
                                    new HDF5DataSet(HDF5BaseReader.this, objectPath, dataSetId, dataSpaceId, 
                                            dimensions, null, layout, true);
                            fileRegistry.registerCleanUp(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        dataSet.close();
                                    }
                                });
                            return dataSet;
                        }
                    };
        return runner.call(openDataSetCallable);
    }

    byte[] getAttributeAsByteArray(final long objectId, final String attributeName,
            ICleanUpRegistry registry)
    {
        final long attributeId = h5.openAttribute(objectId, attributeName, registry);
        final long nativeDataTypeId = h5.getNativeDataTypeForAttribute(attributeId, registry);
        final int dataClass = h5.getClassType(nativeDataTypeId);
        final int size;
        if (dataClass == H5T_ARRAY)
        {
            final int numberOfElements =
                    MDAbstractArray.getLength(h5.getArrayDimensions(nativeDataTypeId));
            final long baseDataType = h5.getBaseDataType(nativeDataTypeId, registry);
            final int elementSize = h5.getDataTypeSize(baseDataType);
            size = numberOfElements * elementSize;
        } else if (dataClass == H5T_STRING)
        {
            final long stringDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            size = h5.getDataTypeSize(stringDataTypeId);
            if (h5.isVariableLengthString(stringDataTypeId))
            {
                String[] data = new String[1];
                h5.readAttributeVL(attributeId, stringDataTypeId, data);
                return data[0].getBytes();
            }
        } else
        {
            final int numberOfElements =
                    MDAbstractArray.getLength(h5.getDataDimensionsForAttribute(attributeId,
                            registry));
            final int elementSize = h5.getDataTypeSize(nativeDataTypeId);
            size = numberOfElements * elementSize;
        }
        return h5.readAttributeAsByteArray(attributeId, nativeDataTypeId, size);
    }

    long openOrCreateBooleanDataType()
    {
        final String booleanDataTypePath = getBooleanDataTypePath(houseKeepingNameSuffix);
        long dataTypeId = getDataTypeId(booleanDataTypePath);
        if (dataTypeId < 0)
        {
            dataTypeId = createBooleanDataType();
            commitDataType(booleanDataTypePath, dataTypeId);
        }
        return dataTypeId;
    }

    String tryGetDataTypePath(long dataTypeId)
    {
        for (DataTypeContainer namedDataType : namedDataTypeList)
        {
            if (h5.dataTypesAreEqual(dataTypeId, namedDataType.typeId))
            {
                return namedDataType.typePath;
            }
        }
        return h5.tryGetDataTypePath(dataTypeId);
    }

    void renameNamedDataType(String oldPath, String newPath)
    {
        final Long typeIdOrNull = namedDataTypeMap.remove(oldPath);
        if (typeIdOrNull != null)
        {
            namedDataTypeMap.put(newPath, typeIdOrNull);
        }
        for (int i = 0; i < namedDataTypeList.size(); ++i)
        {
            final DataTypeContainer c = namedDataTypeList.get(i);
            if (c.typePath.equals(oldPath))
            {
                namedDataTypeList.set(i, new DataTypeContainer(c.typeId, newPath));
            }
        }
    }

    String tryGetDataTypeName(long dataTypeId, HDF5DataClass dataClass)
    {
        final String dataTypePathOrNull = tryGetDataTypePath(dataTypeId);
        return HDF5Utils.tryGetDataTypeNameFromPath(dataTypePathOrNull, houseKeepingNameSuffix,
                dataClass);
    }

    long getDataTypeId(final String dataTypePath)
    {
        final Long dataTypeIdOrNull = namedDataTypeMap.get(dataTypePath);
        if (dataTypeIdOrNull == null)
        {
            // Just in case of data types added to other groups than HDF5Utils.DATATYPE_GROUP
            if (h5.exists(fileId, dataTypePath))
            {
                final long dataTypeId = h5.openDataType(fileId, dataTypePath, fileRegistry);
                namedDataTypeMap.put(dataTypePath, dataTypeId);
                return dataTypeId;
            } else
            {
                return -1;
            }
        } else
        {
            return dataTypeIdOrNull;
        }
    }

    long createBooleanDataType()
    {
        return h5.createDataTypeEnum(new String[]
            { "FALSE", "TRUE" }, fileRegistry);
    }

    HDF5EnumerationType openOrCreateTypeVariantDataType()
    {
        final String typeVariantTypePath = getTypeVariantDataTypePath(houseKeepingNameSuffix);
        long dataTypeId = getDataTypeId(typeVariantTypePath);
        if (dataTypeId < 0)
        {
            return createTypeVariantDataType();
        }
        final long nativeDataTypeId = h5.getNativeDataType(dataTypeId, fileRegistry);
        final String[] typeVariantNames = h5.getNamesForEnumOrCompoundMembers(dataTypeId);
        return new HDF5EnumerationType(fileId, dataTypeId, nativeDataTypeId, typeVariantTypePath,
                typeVariantNames, this);
    }

    HDF5EnumerationType createTypeVariantDataType()
    {
        final HDF5DataTypeVariant[] typeVariants = HDF5DataTypeVariant.values();
        final String[] typeVariantNames = new String[typeVariants.length];
        for (int i = 0; i < typeVariants.length; ++i)
        {
            typeVariantNames[i] = typeVariants[i].name();
        }
        final long dataTypeId = h5.createDataTypeEnum(typeVariantNames, fileRegistry);
        final long nativeDataTypeId = h5.getNativeDataType(dataTypeId, fileRegistry);
        return new HDF5EnumerationType(fileId, dataTypeId, nativeDataTypeId,
                getTypeVariantDataTypePath(houseKeepingNameSuffix), typeVariantNames, this);
    }

    void readNamedDataTypes()
    {
        final String typeGroup = getDataTypeGroup(houseKeepingNameSuffix);
        if (h5.exists(fileId, typeGroup) == false)
        {
            return;
        }
        readNamedDataTypes(typeGroup);
    }

    private void readNamedDataTypes(String dataTypePath)
    {
        for (String dataTypeSubPath : getGroupMemberPaths(dataTypePath))
        {
            final HDF5ObjectType type = h5.getObjectTypeInfo(fileId, dataTypeSubPath, false);
            if (HDF5ObjectType.isGroup(type))
            {
                readNamedDataTypes(dataTypeSubPath);
            } else if (HDF5ObjectType.isDataType(type))
            {
                final long dataTypeId = h5.openDataType(fileId, dataTypeSubPath, fileRegistry);
                namedDataTypeMap.put(dataTypeSubPath, dataTypeId);
                namedDataTypeList.add(new DataTypeContainer(dataTypeId, dataTypeSubPath));
            }
        }
    }

    void commitDataType(final String dataTypePath, final long dataTypeId)
    {
        // Overwrite this method in writer.
    }

    /**
     * Class to store the parameters of a 1d data space.
     */
    static class DataSpaceParameters
    {
        final long memorySpaceId;

        final long dataSpaceId;

        final int blockSize;

        final long[] dimensions;

        DataSpaceParameters(long memorySpaceId, long dataSpaceId, int blockSize, long[] dimensions)
        {
            this.memorySpaceId = memorySpaceId;
            this.dataSpaceId = dataSpaceId;
            this.blockSize = blockSize;
            this.dimensions = dimensions;
        }
    }

    /**
     * Returns the {@link DataSpaceParameters} for the given <var>dataSetId</var>.
     */
    DataSpaceParameters getSpaceParameters(final long dataSetId, ICleanUpRegistry registry)
    {
        long[] dimensions = h5.getDataDimensions(dataSetId, registry);
        return new DataSpaceParameters(H5S_ALL, H5S_ALL, MDAbstractArray.getLength(dimensions),
                dimensions);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a 1d block of the given <var>dataSetId</var>, or
     * <code>null</code>, if the offset is outside of the dataset and
     * <code>nullWhenOutside == true</code>.
     */
    DataSpaceParameters tryGetSpaceParameters(final long dataSetId, final long offset,
            final int blockSize, boolean nullWhenOutside, ICleanUpRegistry registry)
    {
        return tryGetSpaceParameters(dataSetId, 0, offset, blockSize, nullWhenOutside, registry);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a 1d block of the given <var>dataSetId</var>.
     */
    DataSpaceParameters getSpaceParameters(final long dataSetId, final long offset,
            final int blockSize, ICleanUpRegistry registry)
    {
        return tryGetSpaceParameters(dataSetId, 0, offset, blockSize, false, registry);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a 1d block of the given <var>dataSet</var>.
     */
    DataSpaceParameters getSpaceParameters(final HDF5DataSet dataSet, final long offset,
            final int blockSize)
    {
        final long memorySpaceId;
        final long dataSpaceId;
        final int effectiveBlockSize;
        final long[] dimensions;
        if (blockSize > 0)
        {
            dataSpaceId = dataSet.getDataSpaceId();
            dimensions = dataSet.getDimensions();
            if (dimensions.length != 1)
            {
                throw new HDF5JavaException("Data Set is expected to be of rank 1 (rank="
                        + dimensions.length + ")");
            }
            final long size = dimensions[0];
            final long maxFileBlockSize = size - offset;
            if (maxFileBlockSize <= 0)
            {
                throw new HDF5JavaException("Offset " + offset + " >= Size " + size);
            }
            effectiveBlockSize = (int) Math.min(blockSize, Math.min(size, maxFileBlockSize));
            final long[] blockShape = new long[]
                { effectiveBlockSize };
            h5.setHyperslabBlock(dataSpaceId, new long[]
                { offset }, blockShape);
            memorySpaceId = dataSet.getMemorySpaceId(blockShape);
            h5.setHyperslabBlock(memorySpaceId, new long[]
                { 0 }, blockShape);
        } else
        {
            memorySpaceId = HDF5Constants.H5S_ALL;
            dataSpaceId = HDF5Constants.H5S_ALL;
            dimensions = dataSet.getDimensions();
            effectiveBlockSize = getOneDimensionalArraySize(dimensions);
        }
        return new DataSpaceParameters(memorySpaceId, dataSpaceId, effectiveBlockSize, dimensions);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a 1d block of the given <var>dataSetId</var>.
     */
    DataSpaceParameters getSpaceParameters(final long dataSetId, final long memoryOffset,
            final long offset, final int blockSize, ICleanUpRegistry registry)
    {
        return tryGetSpaceParameters(dataSetId, memoryOffset, offset, blockSize, false, registry);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a 1d block of the given <var>dataSetId</var>.
     */
    DataSpaceParameters tryGetSpaceParameters(final long dataSetId, final long memoryOffset,
            final long offset, final int blockSize, boolean nullWhenOutside,
            ICleanUpRegistry registry)
    {
        final long memorySpaceId;
        final long dataSpaceId;
        final int effectiveBlockSize;
        final long[] dimensions;
        if (blockSize > 0)
        {
            dataSpaceId = h5.getDataSpaceForDataSet(dataSetId, registry);
            dimensions = h5.getDataSpaceDimensions(dataSpaceId);
            if (dimensions.length != 1)
            {
                throw new HDF5JavaException("Data Set is expected to be of rank 1 (rank="
                        + dimensions.length + ")");
            }
            final long size = dimensions[0];
            final long maxFileBlockSize = size - offset;
            if (maxFileBlockSize <= 0)
            {
                if (nullWhenOutside)
                {
                    return null;
                }
                throw new HDF5JavaException("Offset " + offset + " >= Size " + size);
            }
            final long maxMemoryBlockSize = size - memoryOffset;
            if (maxMemoryBlockSize <= 0)
            {
                if (nullWhenOutside)
                {
                    return null;
                }
                throw new HDF5JavaException("Memory offset " + memoryOffset + " >= Size " + size);
            }
            effectiveBlockSize =
                    (int) Math.min(blockSize, Math.min(maxMemoryBlockSize, maxFileBlockSize));
            final long[] blockShape = new long[]
                { effectiveBlockSize };
            h5.setHyperslabBlock(dataSpaceId, new long[]
                { offset }, blockShape);
            memorySpaceId = h5.createSimpleDataSpace(blockShape, registry);
            h5.setHyperslabBlock(memorySpaceId, new long[]
                { memoryOffset }, blockShape);
        } else
        {
            memorySpaceId = HDF5Constants.H5S_ALL;
            dataSpaceId = HDF5Constants.H5S_ALL;
            dimensions = h5.getDataDimensions(dataSetId, registry);
            effectiveBlockSize = getOneDimensionalArraySize(dimensions);
        }
        return new DataSpaceParameters(memorySpaceId, dataSpaceId, effectiveBlockSize, dimensions);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a multi-dimensional block of the given
     * <var>dataSetId</var>.
     */
    DataSpaceParameters getSpaceParameters(final long dataSetId, final long[] offset,
            final int[] blockDimensionsOrNull, ICleanUpRegistry registry)
    {
        return tryGetSpaceParameters(dataSetId, offset, blockDimensionsOrNull, false, registry);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a multi-dimensional block of the given
     * <var>dataSetId</var>.
     */
    DataSpaceParameters tryGetSpaceParameters(final long dataSetId, final long[] offset,
            final int[] blockDimensionsOrNull, boolean nullWhenOutside, ICleanUpRegistry registry)
    {
        final long memorySpaceId;
        final long dataSpaceId;
        final long[] effectiveBlockDimensions;
        if (blockDimensionsOrNull != null)
        {
            assert offset != null;
            assert blockDimensionsOrNull.length == offset.length;

            dataSpaceId = h5.getDataSpaceForDataSet(dataSetId, registry);
            final long[] dimensions = h5.getDataSpaceDimensions(dataSpaceId);
            if (blockDimensionsOrNull.length != offset.length)
            {
                throw new HDF5SpaceRankMismatch(blockDimensionsOrNull.length, offset.length);
            }
            if (blockDimensionsOrNull.length != dimensions.length)
            {
                throw new HDF5SpaceRankMismatch(blockDimensionsOrNull.length, dimensions.length);
            }
            effectiveBlockDimensions = new long[blockDimensionsOrNull.length];
            for (int i = 0; i < offset.length; ++i)
            {
                final long maxBlockSize = dimensions[i] - offset[i];
                if (maxBlockSize <= 0)
                {
                    if (nullWhenOutside)
                    {
                        return null;
                    }
                    throw new HDF5JavaException("Offset " + offset[i] + " >= Size " + dimensions[i]);
                }
                effectiveBlockDimensions[i] =
                        (blockDimensionsOrNull[i] < 0) ? (int) maxBlockSize : Math.min(
                                blockDimensionsOrNull[i], maxBlockSize);
            }
            h5.setHyperslabBlock(dataSpaceId, offset, effectiveBlockDimensions);
            memorySpaceId = h5.createSimpleDataSpace(effectiveBlockDimensions, registry);
        } else
        {
            memorySpaceId = H5S_ALL;
            dataSpaceId = H5S_ALL;
            effectiveBlockDimensions = h5.getDataDimensions(dataSetId, registry);
        }
        return new DataSpaceParameters(memorySpaceId, dataSpaceId,
                MDAbstractArray.getLength(effectiveBlockDimensions), effectiveBlockDimensions);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a multi-dimensional block of the given
     * <var>dataSet</var>.
     */
    DataSpaceParameters getSpaceParameters(final HDF5DataSet dataSet, final long[] offset,
            final int[] blockDimensionsOrNull)
    {
        final long memorySpaceId;
        final long dataSpaceId;
        final long[] effectiveBlockDimensions;
        if (blockDimensionsOrNull != null)
        {
            assert offset != null;
            assert blockDimensionsOrNull.length == offset.length;

            dataSpaceId = dataSet.getDataSpaceId();
            final long[] dimensions = dataSet.getDimensions();
            if (dimensions.length != blockDimensionsOrNull.length)
            {
                throw new HDF5SpaceRankMismatch(blockDimensionsOrNull.length, dimensions.length);
            }
            effectiveBlockDimensions = new long[blockDimensionsOrNull.length];
            for (int i = 0; i < offset.length; ++i)
            {
                final long maxBlockSize = dimensions[i] - offset[i];
                if (maxBlockSize <= 0)
                {
                    throw new HDF5JavaException("Offset " + offset[i] + " >= Size " + dimensions[i]);
                }
                effectiveBlockDimensions[i] =
                        (blockDimensionsOrNull[i] < 0) ? (int) maxBlockSize : Math.min(
                                blockDimensionsOrNull[i], maxBlockSize);
            }
            h5.setHyperslabBlock(dataSpaceId, offset, effectiveBlockDimensions);
            memorySpaceId = dataSet.getMemorySpaceId(effectiveBlockDimensions);
        } else
        {
            memorySpaceId = H5S_ALL;
            dataSpaceId = H5S_ALL;
            effectiveBlockDimensions = dataSet.getDimensions();
        }
        return new DataSpaceParameters(memorySpaceId, dataSpaceId,
                MDAbstractArray.getLength(effectiveBlockDimensions), effectiveBlockDimensions);
    }

    /**
     * Returns the {@link DataSpaceParameters} for the given <var>dataSetId</var> when they are
     * mapped to a block in memory.
     */
    DataSpaceParameters getBlockSpaceParameters(final long dataSetId, final int[] memoryOffset,
            final int[] memoryDimensions, ICleanUpRegistry registry)
    {
        return tryGetBlockSpaceParameters(dataSetId, memoryOffset, memoryDimensions, false,
                registry);
    }

    /**
     * Returns the {@link DataSpaceParameters} for the given <var>dataSet</var> when they are
     * mapped to a block in memory.
     */
    DataSpaceParameters getBlockSpaceParameters(final HDF5DataSet dataSet, final int[] memoryOffset,
            final int[] memoryDimensions)
    {
        return tryGetBlockSpaceParameters(dataSet, memoryOffset, memoryDimensions, false);
    }

    /**
     * Returns the {@link DataSpaceParameters} for the given <var>dataSetId</var> when they are
     * mapped to a block in memory.
     */
    DataSpaceParameters tryGetBlockSpaceParameters(final long dataSetId, final int[] memoryOffset,
            final int[] memoryDimensions, final boolean nullWhenOutside, ICleanUpRegistry registry)
    {
        assert memoryOffset != null;
        assert memoryDimensions != null;
        assert memoryDimensions.length == memoryOffset.length;

        final long[] dimensions = h5.getDataDimensions(dataSetId, registry);
        final long memorySpaceId =
                h5.createSimpleDataSpace(MDAbstractArray.toLong(memoryDimensions), registry);
        for (int i = 0; i < dimensions.length; ++i)
        {
            if (dimensions[i] + memoryOffset[i] > memoryDimensions[i])
            {
                if (nullWhenOutside)
                {
                    return null;
                }
                throw new HDF5JavaException("Dimensions " + dimensions[i] + " + memory offset "
                        + memoryOffset[i] + " >= memory buffer " + memoryDimensions[i]);
            }
        }
        h5.setHyperslabBlock(memorySpaceId, MDAbstractArray.toLong(memoryOffset), dimensions);
        return new DataSpaceParameters(memorySpaceId, H5S_ALL,
                MDAbstractArray.getLength(dimensions), dimensions);
    }

    /**
     * Returns the {@link DataSpaceParameters} for the given <var>dataSetId</var> when they are
     * mapped to a block in memory.
     */
    DataSpaceParameters tryGetBlockSpaceParameters(final HDF5DataSet dataSet, final int[] memoryOffset,
            final int[] memoryDimensions, final boolean nullWhenOutside)
    {
        assert memoryOffset != null;
        assert memoryDimensions != null;
        assert memoryDimensions.length == memoryOffset.length;

        final long[] dimensions = dataSet.getDimensions();
        final long memorySpaceId = dataSet.getMemorySpaceId(MDArray.toLong(memoryDimensions));
        for (int i = 0; i < dimensions.length; ++i)
        {
            if (dimensions[i] + memoryOffset[i] > memoryDimensions[i])
            {
                if (nullWhenOutside)
                {
                    return null;
                }
                throw new HDF5JavaException("Dimensions " + dimensions[i] + " + memory offset "
                        + memoryOffset[i] + " >= memory buffer " + memoryDimensions[i]);
            }
        }
        h5.setHyperslabBlock(memorySpaceId, MDAbstractArray.toLong(memoryOffset), dimensions);
        return new DataSpaceParameters(memorySpaceId, H5S_ALL,
                MDAbstractArray.getLength(dimensions), dimensions);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a block of the given <var>dataSetId</var> when
     * they are mapped to a block in memory.
     */
    DataSpaceParameters getBlockSpaceParameters(final long dataSetId, final int[] memoryOffset,
            final int[] memoryDimensions, final long[] offset, final int[] blockDimensions,
            ICleanUpRegistry registry)
    {
        assert memoryOffset != null;
        assert memoryDimensions != null;
        assert offset != null;
        assert blockDimensions != null;
        assert memoryOffset.length == offset.length;
        assert memoryDimensions.length == memoryOffset.length;
        assert blockDimensions.length == offset.length;

        final long memorySpaceId;
        final long dataSpaceId;
        final long[] effectiveBlockDimensions;

        dataSpaceId = h5.getDataSpaceForDataSet(dataSetId, registry);
        final long[] dimensions = h5.getDataSpaceDimensions(dataSpaceId);
        if (dimensions.length != blockDimensions.length)
        {
            throw new HDF5JavaException("Data Set is expected to be of rank "
                    + blockDimensions.length + " (rank=" + dimensions.length + ")");
        }
        effectiveBlockDimensions = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            final long maxFileBlockSize = dimensions[i] - offset[i];
            if (maxFileBlockSize <= 0)
            {
                throw new HDF5JavaException("Offset " + offset[i] + " >= Size " + dimensions[i]);
            }
            final long maxMemoryBlockSize = memoryDimensions[i] - memoryOffset[i];
            if (maxMemoryBlockSize <= 0)
            {
                throw new HDF5JavaException("Memory offset " + memoryOffset[i] + " >= Size "
                        + memoryDimensions[i]);
            }
            effectiveBlockDimensions[i] =
                    Math.min(blockDimensions[i], Math.min(maxMemoryBlockSize, maxFileBlockSize));
        }
        h5.setHyperslabBlock(dataSpaceId, offset, effectiveBlockDimensions);
        memorySpaceId =
                h5.createSimpleDataSpace(MDAbstractArray.toLong(memoryDimensions), registry);
        h5.setHyperslabBlock(memorySpaceId, MDAbstractArray.toLong(memoryOffset),
                effectiveBlockDimensions);
        return new DataSpaceParameters(memorySpaceId, dataSpaceId,
                MDAbstractArray.getLength(effectiveBlockDimensions), effectiveBlockDimensions);
    }

    /**
     * Returns the {@link DataSpaceParameters} for a block of the given <var>dataSet</var> when
     * they are mapped to a block in memory.
     */
    DataSpaceParameters getBlockSpaceParameters(final HDF5DataSet dataSet, final int[] memoryOffset,
            final int[] memoryDimensions, final long[] offset, final int[] blockDimensions)
    {
        assert dataSet != null;
        assert memoryOffset != null;
        assert memoryDimensions != null;
        assert offset != null;
        assert blockDimensions != null;
        assert memoryOffset.length == offset.length;
        assert memoryDimensions.length == memoryOffset.length;
        assert blockDimensions.length == offset.length;

        final long memorySpaceId;
        final long dataSpaceId;
        final long[] effectiveBlockDimensions;

        dataSpaceId = dataSet.getDataSpaceId();
        final long[] dimensions = dataSet.getDimensions();
        if (dimensions.length != blockDimensions.length)
        {
            throw new HDF5JavaException("Data Set is expected to be of rank "
                    + blockDimensions.length + " (rank=" + dimensions.length + ")");
        }
        effectiveBlockDimensions = new long[blockDimensions.length];
        for (int i = 0; i < offset.length; ++i)
        {
            final long maxFileBlockSize = dimensions[i] - offset[i];
            if (maxFileBlockSize <= 0)
            {
                throw new HDF5JavaException("Offset " + offset[i] + " >= Size " + dimensions[i]);
            }
            final long maxMemoryBlockSize = memoryDimensions[i] - memoryOffset[i];
            if (maxMemoryBlockSize <= 0)
            {
                throw new HDF5JavaException("Memory offset " + memoryOffset[i] + " >= Size "
                        + memoryDimensions[i]);
            }
            effectiveBlockDimensions[i] =
                    Math.min(blockDimensions[i], Math.min(maxMemoryBlockSize, maxFileBlockSize));
        }
        h5.setHyperslabBlock(dataSpaceId, offset, effectiveBlockDimensions);
        memorySpaceId = dataSet.getMemorySpaceId(MDArray.toLong(memoryDimensions));
        h5.setHyperslabBlock(memorySpaceId, MDAbstractArray.toLong(memoryOffset),
                effectiveBlockDimensions);
        return new DataSpaceParameters(memorySpaceId, dataSpaceId,
                MDAbstractArray.getLength(effectiveBlockDimensions), effectiveBlockDimensions);
    }

    /**
     * Returns the native data type for the given <var>dataSetId</var>, or
     * <var>overrideDataTypeId</var>, if it is not negative.
     */
    long getNativeDataTypeId(final long dataSetId, final long overrideDataTypeId,
            ICleanUpRegistry registry)
    {
        final long nativeDataTypeId;
        if (overrideDataTypeId < 0)
        {
            nativeDataTypeId = h5.getNativeDataTypeForDataSet(dataSetId, registry);
        } else
        {
            nativeDataTypeId = overrideDataTypeId;
        }
        return nativeDataTypeId;
    }

    /**
     * Returns the members of <var>groupPath</var>. The order is <i>not</i> well defined.
     * 
     * @param groupPath The path of the group to get the members for.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    List<String> getGroupMembers(final String groupPath)
    {
        assert groupPath != null;
        return removeInternalNames(getAllGroupMembers(groupPath), houseKeepingNameSuffix, false);
    }

    /**
     * Returns all members of <var>groupPath</var>, including internal groups that may be used by
     * the library to do house-keeping. The order is <i>not</i> well defined.
     * 
     * @param groupPath The path of the group to get the members for.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    List<String> getAllGroupMembers(final String groupPath)
    {
        final String[] groupMemberArray = h5.getGroupMembers(fileId, groupPath);
        return new LinkedList<String>(Arrays.asList(groupMemberArray));
    }

    /**
     * Returns the paths of the members of <var>groupPath</var> (including the parent). The order is
     * <i>not</i> well defined.
     * 
     * @param groupPath The path of the group to get the member paths for.
     * @throws IllegalArgumentException If <var>groupPath</var> is not a group.
     */
    List<String> getGroupMemberPaths(final String groupPath)
    {
        final String superGroupName = (groupPath.equals("/") ? "/" : groupPath + "/");
        final List<String> memberNames = getGroupMembers(groupPath);
        for (int i = 0; i < memberNames.size(); ++i)
        {
            memberNames.set(i, superGroupName + memberNames.get(i));
        }
        return memberNames;
    }

    /**
     * Returns the information about a data set as a {@link HDF5DataTypeInformation} object. It is a
     * failure condition if the <var>dataSetPath</var> does not exist or does not identify a data
     * set. <br>
     * <i>Does not read the data type path of a committed data type.</i>
     * 
     * @param dataSetPath The name (including path information) of the data set to return
     *            information about.
     */
    HDF5DataSetInformation getDataSetInformation(final String dataSetPath)
    {
        return getDataSetInformation(dataSetPath, DataTypeInfoOptions.DEFAULT, true);
    }

    /**
     * Returns the information about a data set as a {@link HDF5DataTypeInformation} object. It is a
     * failure condition if the <var>dataSetPath</var> does not exist or does not identify a data
     * set.
     * 
     * @param dataSetPath The name (including path information) of the data set to return
     *            information about.
     * @param options What information to obtain about the data type.
     * @param fillDimensions If <code>true</code>, fill in the dimensions of the dataset
     */
    HDF5DataSetInformation getDataSetInformation(final String dataSetPath,
            final DataTypeInfoOptions options, final boolean fillDimensions)
    {
        assert dataSetPath != null;

        final ICallableWithCleanUp<HDF5DataSetInformation> informationDeterminationRunnable =
                new ICallableWithCleanUp<HDF5DataSetInformation>()
                    {
                        @Override
                        public HDF5DataSetInformation call(ICleanUpRegistry registry)
                        {
                            final long dataSetId = h5.openDataSet(fileId, dataSetPath, registry);
                            final long dataTypeId = h5.getDataTypeForDataSet(dataSetId, registry);
                            final HDF5DataTypeInformation dataTypeInfo =
                                    getDataTypeInformation(dataTypeId, options, registry);
                            final HDF5DataTypeVariant variantOrNull =
                                    options.knowsDataTypeVariant() ? tryGetTypeVariant(dataSetId,
                                            registry) : null;
                            final HDF5DataSetInformation dataSetInfo =
                                    new HDF5DataSetInformation(dataTypeInfo, variantOrNull);
                            // Is it a variable-length string?
                            final boolean vlString =
                                    (dataTypeInfo.getDataClass() == HDF5DataClass.STRING && h5
                                            .isVariableLengthString(dataTypeId));
                            if (vlString)
                            {
                                dataTypeInfo.setElementSize(-1);
                            }
                            if (fillDimensions)
                            {
                                h5.fillDataDimensions(dataSetId, false, dataSetInfo, registry);
                            }
                            return dataSetInfo;
                        }
                    };
        return runner.call(informationDeterminationRunnable);
    }

    /**
     * Returns the dimensions of the data set. It is a failure condition if the
     * <var>dataSetPath</var> does not exist or does not identify a data set.
     * 
     * @param dataSetPath The name (including path information) of the data set to return
     *            information about.
     */
    long[] getSpaceDimensions(final String dataSetPath)
    {
        assert dataSetPath != null;

        final ICallableWithCleanUp<long[]> informationDeterminationRunnable =
                new ICallableWithCleanUp<long[]>()
                    {
                        @Override
                        public long[] call(ICleanUpRegistry registry)
                        {
                            final long dataSetId = h5.openDataSet(fileId, dataSetPath, registry);
                            return h5.getDimensions(dataSetId, false, registry);
                        }
                    };
        return runner.call(informationDeterminationRunnable);
    }

    /**
     * Returns the rank of this data set of <var>objectPath</var>. This combines the space rank and
     * the array rank into one rank. It is a failure condition if the <var>objectPath</var> does not
     * exist or does not identify a data set. This method follows symbolic links.
     */
    int getRank(String dataSetPath)
    {
        final HDF5DataSetInformation info =
                getDataSetInformation(dataSetPath, DataTypeInfoOptions.MINIMAL, true);
        return info.getRank() + info.getTypeInformation().getRank();
    }
    
    /**
     * Returns the dimensions of the space of <var>objectPath</var> (empty if this is a scalar
     * space). It is a failure condition if the <var>objectPath</var> does not exist or does not
     * identify a data set. This method follows symbolic links.
     */
    long[] getDimensions(String dataSetPath)
    {
        assert dataSetPath != null;

        final HDF5DataSetInformation info =
                getDataSetInformation(dataSetPath, DataTypeInfoOptions.MINIMAL, true);
        return MatrixUtils.concat(info.getDimensions(), info.getTypeInformation().getDimensions());
    }

    /**
     * Returns the rank of the data set. It is a failure condition if the <var>dataSetPath</var>
     * does not exist or does not identify a data set.
     * 
     * @param dataSetPath The name (including path information) of the data set to return
     *            information about.
     */
    int getSpaceRank(final String dataSetPath)
    {
        assert dataSetPath != null;

        final ICallableWithCleanUp<Integer> informationDeterminationRunnable =
                new ICallableWithCleanUp<Integer>()
                    {
                        @Override
                        public Integer call(ICleanUpRegistry registry)
                        {
                            final long dataSetId = h5.openDataSet(fileId, dataSetPath, registry);
                            return h5.getRank(dataSetId, false, registry);
                        }
                    };
        return runner.call(informationDeterminationRunnable);
    }

    HDF5DataTypeVariant tryGetTypeVariant(final String objectPath)
    {
        assert objectPath != null;

        final ICallableWithCleanUp<HDF5DataTypeVariant> readRunnable =
                new ICallableWithCleanUp<HDF5DataTypeVariant>()
                    {
                        @Override
                        public HDF5DataTypeVariant call(ICleanUpRegistry registry)
                        {
                            final long objectId = h5.openObject(fileId, objectPath, registry);
                            return tryGetTypeVariant(objectId, registry);
                        }
                    };

        return runner.call(readRunnable);
    }

    HDF5DataTypeVariant tryGetTypeVariant(final String objectPath, final String attributeName)
    {
        assert objectPath != null;

        final ICallableWithCleanUp<HDF5DataTypeVariant> readRunnable =
                new ICallableWithCleanUp<HDF5DataTypeVariant>()
                    {
                        @Override
                        public HDF5DataTypeVariant call(ICleanUpRegistry registry)
                        {
                            final long objectId = h5.openObject(fileId, objectPath, registry);
                            return tryGetTypeVariant(objectId, attributeName, registry);
                        }
                    };

        return runner.call(readRunnable);
    }

    HDF5EnumerationValueArray getEnumValueArray(final long attributeId, final String objectPath,
            final String attributeName, ICleanUpRegistry registry)
    {
        final long storageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
        final long nativeDataTypeId = h5.getNativeDataType(storageDataTypeId, registry);
        final int len;
        final long enumTypeId;
        if (h5.getClassType(storageDataTypeId) == H5T_ARRAY)
        {
            final int[] arrayDimensions = h5.getArrayDimensions(storageDataTypeId);
            if (arrayDimensions.length != 1)
            {
                throw new HDF5JavaException("Attribute '" + attributeName + "' of object '"
                        + objectPath + "' is not an array of rank 1, but is of rank "
                        + arrayDimensions.length);
            }
            len = arrayDimensions[0];
            enumTypeId = h5.getBaseDataType(storageDataTypeId, registry);
            if (h5.getClassType(enumTypeId) != H5T_ENUM)
            {
                throw new HDF5JavaException("Attribute '" + attributeName + "' of object '"
                        + objectPath + "' is not of type enumeration array.");
            }
        } else
        {
            if (h5.getClassType(storageDataTypeId) != H5T_ENUM)
            {
                throw new HDF5JavaException("Attribute '" + attributeName + "' of object '"
                        + objectPath + "' is not of type enumeration array.");
            }
            enumTypeId = storageDataTypeId;
            final long[] arrayDimensions = h5.getDataDimensionsForAttribute(attributeId, registry);
            len = HDF5Utils.getOneDimensionalArraySize(arrayDimensions);
        }
        final HDF5EnumerationType enumType =
                getEnumTypeForEnumDataType(null, enumTypeId, true, fileRegistry);
        final byte[] data =
                h5.readAttributeAsByteArray(attributeId, nativeDataTypeId, len
                        * enumType.getStorageForm().getStorageSize());
        final HDF5EnumerationValueArray value =
                new HDF5EnumerationValueArray(enumType, EnumerationType.fromStorageForm(data,
                        enumType.getStorageForm()));
        return value;
    }

    HDF5EnumerationValueMDArray getEnumValueMDArray(final long attributeId, final String objectPath,
            final String attributeName, ICleanUpRegistry registry)
    {
        final long storageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
        final long nativeDataTypeId = h5.getNativeDataType(storageDataTypeId, registry);
        final int len;
        final long enumTypeId;
        final int[] arrayDimensions;
        if (h5.getClassType(storageDataTypeId) == H5T_ARRAY)
        {
            arrayDimensions = h5.getArrayDimensions(storageDataTypeId);
            len = MDAbstractArray.getLength(arrayDimensions);
            enumTypeId = h5.getBaseDataType(storageDataTypeId, registry);
            if (h5.getClassType(enumTypeId) != H5T_ENUM)
            {
                throw new HDF5JavaException("Attribute '" + attributeName + "' of object '"
                        + objectPath + "' is not of type enumeration array.");
            }
        } else
        {
            if (h5.getClassType(storageDataTypeId) != H5T_ENUM)
            {
                throw new HDF5JavaException("Attribute '" + attributeName + "' of object '"
                        + objectPath + "' is not of type enumeration array.");
            }
            enumTypeId = storageDataTypeId;
            arrayDimensions =
                    MDAbstractArray.toInt(h5.getDataDimensionsForAttribute(attributeId, registry));
            len = MDAbstractArray.getLength(arrayDimensions);
        }
        final HDF5EnumerationType enumType =
                getEnumTypeForEnumDataType(null, enumTypeId, true, fileRegistry);
        final byte[] data =
                h5.readAttributeAsByteArray(attributeId, nativeDataTypeId, len
                        * enumType.getStorageForm().getStorageSize());
        final HDF5EnumerationValueMDArray value =
                new HDF5EnumerationValueMDArray(enumType, EnumerationType.fromStorageForm(data,
                        arrayDimensions, enumType.getStorageForm()));
        return value;
    }

    long getEnumDataTypeId(final long storageDataTypeId, ICleanUpRegistry registry)
    {
        final long enumDataTypeId;
        if (h5.getClassType(storageDataTypeId) == H5T_ARRAY)
        {
            enumDataTypeId = h5.getBaseDataType(storageDataTypeId, registry);
        } else
        {
            enumDataTypeId = storageDataTypeId;
        }
        return enumDataTypeId;
    }

    HDF5DataTypeVariant[] tryGetTypeVariantForCompoundMembers(String dataTypePathOrNull,
            ICleanUpRegistry registry)
    {
        if (dataTypePathOrNull == null)
        {
            return null;
        }
        checkOpen();
        final long objectId = h5.openObject(fileId, dataTypePathOrNull, registry);
        final String typeVariantMembersAttributeName =
                getTypeVariantMembersAttributeName(houseKeepingNameSuffix);
        if (h5.existsAttribute(objectId, typeVariantMembersAttributeName) == false)
        {
            return null;
        }
        final long attributeId =
                h5.openAttribute(objectId, typeVariantMembersAttributeName, registry);
        final HDF5EnumerationValueArray valueArray =
                getEnumValueArray(attributeId, dataTypePathOrNull, typeVariantMembersAttributeName,
                        registry);
        final HDF5DataTypeVariant[] variants = new HDF5DataTypeVariant[valueArray.getLength()];
        boolean hasVariants = false;
        for (int i = 0; i < variants.length; ++i)
        {
            variants[i] = HDF5DataTypeVariant.values()[valueArray.getOrdinal(i)];
            hasVariants |= variants[i].isTypeVariant();
        }
        if (hasVariants)
        {
            return variants;
        } else
        {
            return null;
        }
    }

    HDF5DataTypeVariant tryGetTypeVariant(final long objectId, ICleanUpRegistry registry)
    {
        final int typeVariantOrdinal = getAttributeTypeVariant(objectId, registry);
        return typeVariantOrdinal < 0 ? null : HDF5DataTypeVariant.values()[typeVariantOrdinal];
    }

    HDF5DataTypeVariant tryGetTypeVariant(final long objectId, String attributeName,
            ICleanUpRegistry registry)
    {
        final int typeVariantOrdinal = getAttributeTypeVariant(objectId, attributeName, registry);
        return typeVariantOrdinal < 0 ? null : HDF5DataTypeVariant.values()[typeVariantOrdinal];
    }

    /**
     * Returns the ordinal for the type variant of <var>objectPath</var>, or <code>-1</code>, if no
     * type variant is defined for this <var>objectPath</var>.
     * 
     * @param objectId The id of the data set object in the file.
     * @return The ordinal of the type variant or <code>null</code>.
     */
    int getAttributeTypeVariant(final long objectId, ICleanUpRegistry registry)
    {
        checkOpen();
        final String dataTypeVariantAttributeName =
                createObjectTypeVariantAttributeName(houseKeepingNameSuffix);
        if (h5.existsAttribute(objectId, dataTypeVariantAttributeName) == false)
        {
            return -1;
        }
        final long attributeId = h5.openAttribute(objectId, dataTypeVariantAttributeName, registry);
        return getEnumOrdinal(attributeId, -1, typeVariantDataType);
    }

    /**
     * Returns the ordinal for the type variant of <var>objectPath</var>, or <code>-1</code>, if no
     * type variant is defined for this <var>objectPath</var>.
     * 
     * @param objectId The id of the data set object in the file.
     * @param attributeName The name of the attribute to get the type variant for.
     * @return The ordinal of the type variant or <code>null</code>.
     */
    int getAttributeTypeVariant(final long objectId, String attributeName, ICleanUpRegistry registry)
    {
        checkOpen();
        final String typeVariantAttrName =
                createAttributeTypeVariantAttributeName(attributeName, houseKeepingNameSuffix);
        if (h5.existsAttribute(objectId, typeVariantAttrName) == false)
        {
            return -1;
        }
        final long attributeId = h5.openAttribute(objectId, typeVariantAttrName, registry);
        return getEnumOrdinal(attributeId, -1, typeVariantDataType);
    }

    int getEnumOrdinal(final long attributeId, long nativeDataTypeId,
            final HDF5EnumerationType enumType)
    {
        final byte[] data =
                h5.readAttributeAsByteArray(attributeId,
                        (nativeDataTypeId < 0) ? enumType.getNativeTypeId() : nativeDataTypeId,
                        enumType.getStorageForm().getStorageSize());
        return EnumerationType.fromStorageForm(data);
    }

    /**
     * Returns the explicitly saved string length for attribute
     * <var>HOUSEKEEPING_NAME_SUFFIX_ATTRIBUTE_NAME</var> of <var>objectId</var>, or <code>-1</code>
     * , if no explicit string length is defined for this attribute.
     * 
     * @param objectId The id of the data set object in the file.
     * @return The length of the string attribute or -1.
     */
    private int getHousekeepingAttributeExplicitStringLength(final long objectId,
            ICleanUpRegistry registry)
    {
        if (h5.existsAttribute(objectId, HOUSEKEEPING_NAME_SUFFIX_STRINGLENGTH_ATTRIBUTE_NAME) == false)
        {
            return -1;
        }
        final long attributeId =
                h5.openAttribute(objectId, HOUSEKEEPING_NAME_SUFFIX_STRINGLENGTH_ATTRIBUTE_NAME,
                        registry);
        final int[] data = h5.readAttributeAsIntArray(attributeId, H5T_NATIVE_INT32, 1);
        return data[0];
    }

    HDF5DataTypeInformation getDataTypeInformation(final long dataTypeId,
            final DataTypeInfoOptions options)
    {
        final ICallableWithCleanUp<HDF5DataTypeInformation> informationDeterminationRunnable =
                new ICallableWithCleanUp<HDF5DataTypeInformation>()
                    {
                        @Override
                        public HDF5DataTypeInformation call(ICleanUpRegistry registry)
                        {
                            final HDF5DataTypeInformation dataTypeInfo =
                                    getDataTypeInformation(dataTypeId, options, registry);
                            // Is it a variable-length string?
                            final boolean vlString =
                                    (dataTypeInfo.getDataClass() == HDF5DataClass.STRING && h5
                                            .isVariableLengthString(dataTypeId));
                            if (vlString)
                            {
                                dataTypeInfo.setElementSize(-1);
                            }
                            return dataTypeInfo;
                        }
                    };
        return runner.call(informationDeterminationRunnable);
    }

    HDF5DataTypeInformation getDataTypeInformation(final long dataTypeId,
            final DataTypeInfoOptions options, final ICleanUpRegistry registry)
    {
        final int classTypeId = h5.getClassType(dataTypeId);
        final HDF5DataClass dataClass;
        final int totalSize = h5.getDataTypeSize(dataTypeId);
        if (classTypeId == H5T_ARRAY)
        {
            dataClass = getElementClassForArrayDataType(dataTypeId);
            final int[] arrayDimensions = h5.getArrayDimensions(dataTypeId);
            final int numberOfElements = MDAbstractArray.getLength(arrayDimensions);
            final int size = totalSize / numberOfElements;
            final long baseTypeId = h5.getBaseDataType(dataTypeId, registry);
            final boolean signed =
                    (dataClass == HDF5DataClass.INTEGER) ? h5.getSigned(baseTypeId)
                            : (dataClass == HDF5DataClass.FLOAT);
            final String dataTypePathOrNull =
                    options.knowsDataTypePath() ? tryGetDataTypePath(baseTypeId) : null;
            final CharacterEncoding dataSetEncoding =
                    (dataClass == HDF5DataClass.STRING) ? h5.getCharacterEncoding(baseTypeId)
                            : CharacterEncoding.ASCII;
            final boolean variableLengthString =
                    (dataClass == HDF5DataClass.STRING) ? h5.isVariableLengthString(baseTypeId)
                            : false;
            return new HDF5DataTypeInformation(dataTypePathOrNull, options, dataClass,
                    dataSetEncoding, houseKeepingNameSuffix, size, arrayDimensions, true, signed,
                    variableLengthString);
        } else
        {
            dataClass = getDataClassForClassType(classTypeId, dataTypeId);
            final String opaqueTagOrNull;
            if (dataClass == HDF5DataClass.OPAQUE)
            {
                opaqueTagOrNull = h5.tryGetOpaqueTag(dataTypeId);
            } else
            {
                opaqueTagOrNull = null;
            }
            final String dataTypePathOrNull =
                    options.knowsDataTypePath() ? tryGetDataTypePath(dataTypeId) : null;
            final boolean signed =
                    (dataClass == HDF5DataClass.INTEGER) ? h5.getSigned(dataTypeId)
                            : (dataClass == HDF5DataClass.FLOAT);
            final CharacterEncoding dataSetEncoding =
                    (dataClass == HDF5DataClass.STRING) ? h5.getCharacterEncoding(dataTypeId)
                            : CharacterEncoding.ASCII;
            final boolean variableLengthString =
                    (dataClass == HDF5DataClass.STRING) ? h5.isVariableLengthString(dataTypeId)
                            : false;
            return new HDF5DataTypeInformation(dataTypePathOrNull, options, dataClass,
                    dataSetEncoding, houseKeepingNameSuffix, totalSize, signed,
                    variableLengthString, opaqueTagOrNull);
        }
    }

    private HDF5DataClass getDataClassForClassType(final int classTypeId, final long dataTypeId)
    {
        HDF5DataClass dataClass = HDF5DataClass.classIdToDataClass(classTypeId);
        // Is it a boolean?
        if (dataClass == HDF5DataClass.ENUM && h5.dataTypesAreEqual(dataTypeId, booleanDataTypeId))
        {
            dataClass = HDF5DataClass.BOOLEAN;
        }
        return dataClass;
    }

    private HDF5DataClass getElementClassForArrayDataType(final long arrayDataTypeId)
    {
        for (HDF5DataClass eClass : HDF5DataClass.values())
        {
            if (h5.hasClassType(arrayDataTypeId, eClass.getId()))
            {
                return eClass;
            }
        }
        return HDF5DataClass.OTHER;
    }

    //
    // Compound
    //

    String getCompoundDataTypeName(final String nameOrNull, final long dataTypeId)
    {
        return getDataTypeName(nameOrNull, HDF5DataClass.COMPOUND, dataTypeId);
    }

    <T> HDF5ValueObjectByteifyer<T> createCompoundByteifyers(final Class<T> compoundClazz,
            final HDF5CompoundMemberMapping[] compoundMembers,
            final CompoundTypeInformation compoundTypeInfoOrNull)
    {
        final HDF5ValueObjectByteifyer<T> objectByteifyer =
                new HDF5ValueObjectByteifyer<T>(compoundClazz,
                        new HDF5ValueObjectByteifyer.IFileAccessProvider()
                            {
                                @Override
                                public long getBooleanDataTypeId()
                                {
                                    return booleanDataTypeId;
                                }

                                @Override
                                public long getStringDataTypeId(int maxLength)
                                {
                                    final long typeId =
                                            h5.createDataTypeString(maxLength, fileRegistry);
                                    return typeId;
                                }

                                @Override
                                public long getArrayTypeId(long baseTypeId, int length)
                                {
                                    final long typeId =
                                            h5.createArrayType(baseTypeId, length, fileRegistry);
                                    return typeId;
                                }

                                @Override
                                public long getArrayTypeId(long baseTypeId, int[] dimensions)
                                {
                                    final long typeId =
                                            h5.createArrayType(baseTypeId, dimensions, fileRegistry);
                                    return typeId;
                                }

                                @Override
                                public CharacterEncoding getCharacterEncoding(long dataTypeId)
                                {
                                    return (dataTypeId < 0) ? encodingForNewDataSets : h5
                                            .getCharacterEncoding(dataTypeId);
                                }

                                @Override
                                public HDF5EnumerationType getEnumType(String[] options)
                                {
                                    final long storageDataTypeId =
                                            h5.createDataTypeEnum(options, fileRegistry);
                                    final long nativeDataTypeId =
                                            h5.getNativeDataType(storageDataTypeId, fileRegistry);
                                    return new HDF5EnumerationType(fileId, storageDataTypeId,
                                            nativeDataTypeId, null, options, HDF5BaseReader.this);
                                }

                                @Override
                                public long getVariableLengthStringDataTypeId()
                                {
                                    return variableLengthStringDataTypeId;
                                }

                                @Override
                                public byte[] createObjectReference(String referencedObjectPath)
                                {
                                    return h5.createObjectReference(fileId, referencedObjectPath);
                                }
                            }, compoundTypeInfoOrNull, compoundMembers);
        return objectByteifyer;
    }

    long createStorageCompoundDataType(HDF5ValueObjectByteifyer<?> objectArrayifyer)
    {
        final long storageDataTypeId =
                h5.createDataTypeCompound(objectArrayifyer.getRecordSizeOnDisk(), fileRegistry);
        objectArrayifyer.insertMemberTypes(storageDataTypeId);
        return storageDataTypeId;
    }

    long createNativeCompoundDataType(HDF5ValueObjectByteifyer<?> objectArrayifyer)
    {
        final long nativeDataTypeId =
                h5.createDataTypeCompound(objectArrayifyer.getRecordSizeInMemory(), fileRegistry);
        objectArrayifyer.insertNativeMemberTypes(nativeDataTypeId, h5, fileRegistry);
        return nativeDataTypeId;
    }

    //
    // Enum
    //

    HDF5EnumerationType getEnumTypeForStorageDataType(final String nameOrNull,
            final long storageDataTypeId, final boolean resolveName, final String objectPathOrNull,
            final String attributeNameOrNull, final ICleanUpRegistry registry)
    {
        int classType = h5.getClassType(storageDataTypeId);
        final boolean isArray = (classType == H5T_ARRAY);
        final long enumStoreDataTypeId;
        if (isArray)
        {
            enumStoreDataTypeId = h5.getBaseDataType(storageDataTypeId, registry);
            classType = h5.getClassType(enumStoreDataTypeId);
        } else
        {
            enumStoreDataTypeId = storageDataTypeId;
        }
        if (classType != H5T_ENUM)
        {
            if (attributeNameOrNull != null)
            {
                throw new HDF5JavaException("Attribute '" + attributeNameOrNull + "' of object '"
                        + objectPathOrNull + "' is not of enum type.");
            } else if (objectPathOrNull != null)
            {
                throw new HDF5JavaException("Object '" + objectPathOrNull
                        + "' is not of enum type.");
            } else
            {
                throw new HDF5JavaException("Type '" + (nameOrNull != null ? nameOrNull : "???")
                        + "' is not of enum type.");
            }
        }
        return getEnumTypeForEnumDataType(nameOrNull, enumStoreDataTypeId, resolveName, registry);
    }

    HDF5EnumerationType getEnumTypeForEnumDataType(final String nameOrNull,
            final long enumStoreDataTypeId, final boolean resolveName,
            final ICleanUpRegistry registry)
    {
        final long nativeDataTypeId = h5.getNativeDataType(enumStoreDataTypeId, registry);
        final String[] values = h5.getNamesForEnumOrCompoundMembers(enumStoreDataTypeId);
        return new HDF5EnumerationType(fileId, enumStoreDataTypeId, nativeDataTypeId,
                resolveName ? getEnumDataTypeName(nameOrNull, enumStoreDataTypeId) : nameOrNull,
                values, this);
    }

    void checkEnumValues(long dataTypeId, final String[] values, final String nameOrNull)throws HDF5JavaException
    {
        final String[] valuesStored = h5.getNamesForEnumOrCompoundMembers(dataTypeId);
        if (valuesStored.length != values.length)
        {
            throw new HDF5JavaException("Enum " + getEnumDataTypeName(nameOrNull, dataTypeId)
                    + " has " + valuesStored.length + " members, but should have " + values.length);
        }
        for (int i = 0; i < values.length; ++i)
        {
            if (values[i].equals(valuesStored[i]) == false)
            {
                throw new HDF5JavaException("Enum member index " + i + " of enum "
                        + getEnumDataTypeName(nameOrNull, dataTypeId) + " is '" + valuesStored[i]
                        + "', but should be '" + values[i] + "'");
            }
        }
    }

    String getEnumDataTypeName(final String nameOrNull, final long dataTypeId)
    {
        return getDataTypeName(nameOrNull, HDF5DataClass.ENUM, dataTypeId);
    }

    private String getDataTypeName(final String nameOrNull, final HDF5DataClass dataClass,
            final long dataTypeId)
    {
        if (nameOrNull != null)
        {
            return nameOrNull;
        } else
        {
            final String nameFromPathOrNull =
                    HDF5Utils.tryGetDataTypeNameFromPath(tryGetDataTypePath(dataTypeId),
                            houseKeepingNameSuffix, dataClass);
            return (nameFromPathOrNull == null) ? "UNKNOWN" : nameFromPathOrNull;
        }
    }

    boolean isScaledEnum(final long objectId, final ICleanUpRegistry registry)
    {
        final HDF5DataTypeVariant typeVariantOrNull = tryGetTypeVariant(objectId, registry);
        return (HDF5DataTypeVariant.ENUM == typeVariantOrNull);
    }

    boolean isScaledBitField(final long objectId, final ICleanUpRegistry registry)
    {
        final HDF5DataTypeVariant typeVariantOrNull = tryGetTypeVariant(objectId, registry);
        return (HDF5DataTypeVariant.BITFIELD == typeVariantOrNull);
    }

    //
    // String
    //

    private long openOrCreateVLStringType()
    {
        final String variableLengthStringTypePath =
                getVariableLengthStringDataTypePath(houseKeepingNameSuffix);
        long dataTypeId = getDataTypeId(variableLengthStringTypePath);
        if (dataTypeId < 0)
        {
            dataTypeId = h5.createDataTypeVariableString(fileRegistry);
            commitDataType(variableLengthStringTypePath, dataTypeId);
        }
        return dataTypeId;
    }

    String getStringAttribute(final long objectId, final String objectPath,
            final String attributeName, final boolean readRaw, final ICleanUpRegistry registry)
    {
        final long attributeId = h5.openAttribute(objectId, attributeName, registry);
        final long stringDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
        final boolean isString = (h5.getClassType(stringDataTypeId) == H5T_STRING);
        if (isString == false)
        {
            throw new IllegalArgumentException("Attribute " + attributeName + " of object "
                    + objectPath + " needs to be a String.");
        }
        final int size = h5.getDataTypeSize(stringDataTypeId);
        if (h5.isVariableLengthString(stringDataTypeId))
        {
            String[] data = new String[1];
            h5.readAttributeVL(attributeId, stringDataTypeId, data);
            return data[0];
        } else
        {
            final CharacterEncoding dataSetEncoding = h5.getCharacterEncoding(stringDataTypeId);
            final byte[] data = h5.readAttributeAsByteArray(attributeId, stringDataTypeId, size);
            return (readRaw ? StringUtils.fromBytes(data, dataSetEncoding) : StringUtils
                    .fromBytes0Term(data, dataSetEncoding));
        }
    }

    String[] getStringArrayAttribute(final long objectId, final String objectPath,
            final String attributeName, final boolean readRaw, final ICleanUpRegistry registry)
    {
        final long attributeId = h5.openAttribute(objectId, attributeName, registry);
        final long stringArrayDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
        final boolean isArray = (h5.getClassType(stringArrayDataTypeId) == H5T_ARRAY);
        if (isArray == false)
        {
            throw new HDF5JavaException("Attribute " + attributeName + " of object " + objectPath
                    + " needs to be a String array of rank 1.");
        }
        final long stringDataTypeId = h5.getBaseDataType(stringArrayDataTypeId, registry);
        final boolean isStringArray = (h5.getClassType(stringDataTypeId) == H5T_STRING);
        if (isStringArray == false)
        {
            throw new HDF5JavaException("Attribute " + attributeName + " of object " + objectPath
                    + " needs to be a String array of rank 1.");
        }
        final int size = h5.getDataTypeSize(stringArrayDataTypeId);
        if (h5.isVariableLengthString(stringDataTypeId))
        {
            String[] data = new String[1];
            h5.readAttributeVL(attributeId, stringDataTypeId, data);
            return data;
        } else
        {
            final CharacterEncoding dataSetEncoding =
                    h5.getCharacterEncoding(stringArrayDataTypeId);
            byte[] data = h5.readAttributeAsByteArray(attributeId, stringArrayDataTypeId, size);
            final int[] arrayDimensions = h5.getArrayDimensions(stringArrayDataTypeId);
            if (arrayDimensions.length != 1)
            {
                throw new HDF5JavaException("Attribute " + attributeName + " of object "
                        + objectPath + " needs to be a String array of rank 1.");
            }
            final int lengthPerElement = h5.getDataTypeSize(stringDataTypeId);
            final int numberOfElements = arrayDimensions[0];
            final String[] result = new String[numberOfElements];
            for (int i = 0, startIdx = 0, endIdx = lengthPerElement; i < numberOfElements; ++i, startIdx +=
                    lengthPerElement, endIdx += lengthPerElement)
            {
                result[i] =
                        readRaw ? StringUtils.fromBytes(data, startIdx, endIdx, dataSetEncoding)
                                : StringUtils.fromBytes0Term(data, startIdx, endIdx,
                                        dataSetEncoding);
            }
            return result;
        }
    }

    MDArray<String> getStringMDArrayAttribute(final long objectId, final String objectPath,
            final String attributeName, final boolean readRaw, final ICleanUpRegistry registry)
    {
        final long attributeId = h5.openAttribute(objectId, attributeName, registry);
        final long stringArrayDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
        final boolean isArray = (h5.getClassType(stringArrayDataTypeId) == H5T_ARRAY);
        if (isArray == false)
        {
            throw new HDF5JavaException("Attribute " + attributeName + " of object " + objectPath
                    + " needs to be a String array.");
        }
        final long stringDataTypeId = h5.getBaseDataType(stringArrayDataTypeId, registry);
        final boolean isStringArray = (h5.getClassType(stringDataTypeId) == H5T_STRING);
        if (isStringArray == false)
        {
            throw new HDF5JavaException("Attribute " + attributeName + " of object " + objectPath
                    + " needs to be a String array.");
        }
        final int size = h5.getDataTypeSize(stringArrayDataTypeId);
        if (h5.isVariableLengthString(stringDataTypeId))
        {
            String[] data = new String[1];
            h5.readAttributeVL(attributeId, stringDataTypeId, data);
            return new MDArray<String>(data, new int[]
                { 1 });
        } else
        {
            final byte[] data =
                    h5.readAttributeAsByteArray(attributeId, stringArrayDataTypeId, size);
            final CharacterEncoding dataSetEncoding =
                    h5.getCharacterEncoding(stringArrayDataTypeId);
            final int[] arrayDimensions = h5.getArrayDimensions(stringArrayDataTypeId);
            final int lengthPerElement = h5.getDataTypeSize(stringDataTypeId);
            final int numberOfElements = MDAbstractArray.getLength(arrayDimensions);
            final String[] result = new String[numberOfElements];
            for (int i = 0, startIdx = 0, endIdx = lengthPerElement; i < numberOfElements; ++i, startIdx +=
                    lengthPerElement, endIdx += lengthPerElement)
            {
                result[i] =
                        readRaw ? StringUtils.fromBytes(data, startIdx, endIdx, dataSetEncoding)
                                : StringUtils.fromBytes0Term(data, startIdx, endIdx,
                                        dataSetEncoding);
            }
            return new MDArray<String>(result, arrayDimensions);
        }
    }

    // Date & Time

    void checkIsTimeStamp(final String objectPath, final long dataSetId, ICleanUpRegistry registry)
            throws HDF5JavaException
    {
        final long typeVariantOrdinal = getAttributeTypeVariant(dataSetId, registry);
        if (typeVariantOrdinal != HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH
                .ordinal())
        {
            throw new HDF5JavaException("Data set '" + objectPath + "' is not a time stamp.");
        }
    }

    void checkIsTimeStamp(final String objectPath, final String attributeName, final long dataSetId,
            ICleanUpRegistry registry) throws HDF5JavaException
    {
        final long typeVariantOrdinal = getAttributeTypeVariant(dataSetId, attributeName, registry);
        if (typeVariantOrdinal != HDF5DataTypeVariant.TIMESTAMP_MILLISECONDS_SINCE_START_OF_THE_EPOCH
                .ordinal())
        {
            throw new HDF5JavaException("Attribute '" + attributeName + "' of data set '"
                    + objectPath + "' is not a time stamp.");
        }
    }

    HDF5TimeUnit checkIsTimeDuration(final String objectPath, final long dataSetId,
            ICleanUpRegistry registry) throws HDF5JavaException
    {
        final int typeVariantOrdinal = getAttributeTypeVariant(dataSetId, registry);
        if (HDF5DataTypeVariant.isTimeDuration(typeVariantOrdinal) == false)
        {
            throw new HDF5JavaException("Data set '" + objectPath + "' is not a time duration.");
        }
        return HDF5DataTypeVariant.getTimeUnit(typeVariantOrdinal);
    }

    HDF5TimeUnit checkIsTimeDuration(final String objectPath, final String attributeName,
            final long dataSetId, ICleanUpRegistry registry) throws HDF5JavaException
    {
        final int typeVariantOrdinal = getAttributeTypeVariant(dataSetId, attributeName, registry);
        if (HDF5DataTypeVariant.isTimeDuration(typeVariantOrdinal) == false)
        {
            throw new HDF5JavaException("Attribute '" + attributeName + "' of data set '"
                    + objectPath + "' is not a time duration.");
        }
        return HDF5DataTypeVariant.getTimeUnit(typeVariantOrdinal);
    }

}
