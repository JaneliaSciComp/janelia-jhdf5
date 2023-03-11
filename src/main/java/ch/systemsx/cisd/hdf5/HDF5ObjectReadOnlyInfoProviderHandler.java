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

import static ch.systemsx.cisd.hdf5.HDF5Utils.removeInternalNames;

import java.util.List;

import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation.DataTypeInfoOptions;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * Implementation of {@link IHDF5ObjectReadOnlyInfoProviderHandler}
 * 
 * @author Bernd Rinn
 */
class HDF5ObjectReadOnlyInfoProviderHandler implements IHDF5ObjectReadOnlyInfoProviderHandler
{
    private final HDF5BaseReader baseReader;

    HDF5ObjectReadOnlyInfoProviderHandler(HDF5BaseReader baseReader)
    {
        assert baseReader != null;

        this.baseReader = baseReader;
    }

    // /////////////////////
    // Objects & Links
    // /////////////////////

    @Override
    public HDF5LinkInformation getLinkInformation(final String objectPath)
    {
        baseReader.checkOpen();
        return baseReader.h5.getLinkInfo(baseReader.fileId, objectPath, false);
    }

    @Override
    public HDF5ObjectInformation getObjectInformation(final String objectPath)
    {
        baseReader.checkOpen();
        return baseReader.h5.getObjectInfo(baseReader.fileId, objectPath, false);
    }

    @Override
    public HDF5ObjectType getObjectType(final String objectPath, boolean followLink)
    {
        baseReader.checkOpen();
        if (followLink)
        {
            return baseReader.h5.getObjectTypeInfo(baseReader.fileId, objectPath, false);
        } else
        {
            return baseReader.h5.getLinkTypeInfo(baseReader.fileId, objectPath, false);
        }
    }

    @Override
    public HDF5ObjectType getObjectType(final String objectPath)
    {
        return getObjectType(objectPath, true);
    }

    @Override
    public boolean exists(final String objectPath, boolean followLink)
    {
        if (followLink == false)
        {
            // Optimization
            baseReader.checkOpen();
            if ("/".equals(objectPath))
            {
                return true;
            }
            return baseReader.h5.exists(baseReader.fileId, objectPath);
        } else
        {
            return exists(objectPath);
        }
    }

    @Override
    public boolean exists(final String objectPath)
    {
        baseReader.checkOpen();
        if ("/".equals(objectPath))
        {
            return true;
        }
        return baseReader.h5.getObjectTypeId(baseReader.fileId, objectPath, false) >= 0;
    }

    @Override
    public HDF5DataSet openDataSet(final String objectPath)
    {
        baseReader.checkOpen();
        return baseReader.openDataSet(objectPath);
    }

    @Override
    public String toHouseKeepingPath(String objectPath)
    {
        return HDF5Utils.toHouseKeepingPath(objectPath, baseReader.houseKeepingNameSuffix);
    }

    @Override
    public boolean isHouseKeepingObject(String objectPath)
    {
        return HDF5Utils.isInternalName(objectPath, baseReader.houseKeepingNameSuffix);
    }

    @Override
    public boolean isGroup(final String objectPath, boolean followLink)
    {
        return HDF5ObjectType.isGroup(getObjectType(objectPath, followLink));
    }

    @Override
    public boolean isGroup(final String objectPath)
    {
        return HDF5ObjectType.isGroup(getObjectType(objectPath));
    }

    @Override
    public boolean isDataSet(final String objectPath, boolean followLink)
    {
        return HDF5ObjectType.isDataSet(getObjectType(objectPath, followLink));
    }

    @Override
    public boolean isDataSet(final String objectPath)
    {
        return HDF5ObjectType.isDataSet(getObjectType(objectPath));
    }

    @Override
    public boolean isDataType(final String objectPath, boolean followLink)
    {
        return HDF5ObjectType.isDataType(getObjectType(objectPath, followLink));
    }

    @Override
    public boolean isDataType(final String objectPath)
    {
        return HDF5ObjectType.isDataType(getObjectType(objectPath));
    }

    @Override
    public boolean isSoftLink(final String objectPath)
    {
        return HDF5ObjectType.isSoftLink(getObjectType(objectPath, false));
    }

    @Override
    public boolean isExternalLink(final String objectPath)
    {
        return HDF5ObjectType.isExternalLink(getObjectType(objectPath, false));
    }

    @Override
    public boolean isSymbolicLink(final String objectPath)
    {
        return HDF5ObjectType.isSymbolicLink(getObjectType(objectPath, false));
    }

    @Override
    public String tryGetSymbolicLinkTarget(final String objectPath)
    {
        return getLinkInformation(objectPath).tryGetSymbolicLinkTarget();
    }

    @Override
    public String tryGetExternalLinkFilename(final String objectPath)
    {
        return getLinkInformation(objectPath).tryGetExternalLinkFilename();
    }

    @Override
    public String tryGetExternalLinkTarget(final String objectPath)
    {
        return getLinkInformation(objectPath).tryGetExternalLinkTarget();
    }

    @Override
    public String tryGetDataTypePath(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String> dataTypeNameCallable =
                new ICallableWithCleanUp<String>()
                    {
                        @Override
                        public String call(ICleanUpRegistry registry)
                        {
                            final long dataSetId =
                                    baseReader.h5.openDataSet(baseReader.fileId, objectPath,
                                            registry);
                            final long dataTypeId =
                                    baseReader.h5.getDataTypeForDataSet(dataSetId, registry);
                            return baseReader.tryGetDataTypePath(dataTypeId);
                        }
                    };
        return baseReader.runner.call(dataTypeNameCallable);
    }

    @Override
    public String tryGetDataTypePath(HDF5DataType type)
    {
        assert type != null;

        baseReader.checkOpen();
        type.check(baseReader.fileId);
        return baseReader.tryGetDataTypePath(type.getStorageTypeId());
    }

    @Override
    public List<String> getAttributeNames(final String objectPath)
    {
        assert objectPath != null;
        baseReader.checkOpen();
        return removeInternalNames(getAllAttributeNames(objectPath),
                baseReader.houseKeepingNameSuffix, "/".equals(objectPath));
    }

    @Override
    public List<String> getAllAttributeNames(final String objectPath)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<List<String>> attributeNameReaderRunnable =
                new ICallableWithCleanUp<List<String>>()
                    {
                        @Override
                        public List<String> call(ICleanUpRegistry registry)
                        {
                            final long objectId =
                                    baseReader.h5.openObject(baseReader.fileId, objectPath,
                                            registry);
                            return baseReader.h5.getAttributeNames(objectId, registry);
                        }
                    };
        return baseReader.runner.call(attributeNameReaderRunnable);
    }

    @Override
    public HDF5DataTypeInformation getAttributeInformation(final String dataSetPath,
            final String attributeName)
    {
        return getAttributeInformation(dataSetPath, attributeName, DataTypeInfoOptions.DEFAULT);
    }

    @Override
    public HDF5DataTypeInformation getAttributeInformation(final String dataSetPath,
            final String attributeName, final DataTypeInfoOptions dataTypeInfoOptions)
    {
        assert dataSetPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<HDF5DataTypeInformation> informationDeterminationRunnable =
                new ICallableWithCleanUp<HDF5DataTypeInformation>()
                    {
                        @Override
                        public HDF5DataTypeInformation call(ICleanUpRegistry registry)
                        {
                            try
                            {
                                final long objectId =
                                        baseReader.h5.openObject(baseReader.fileId, dataSetPath,
                                                registry);
                                final long attributeId =
                                        baseReader.h5.openAttribute(objectId, attributeName,
                                                registry);
                                final long dataTypeId =
                                        baseReader.h5
                                                .getDataTypeForAttribute(attributeId, registry);
                                final HDF5DataTypeInformation dataTypeInformation =
                                        baseReader.getDataTypeInformation(dataTypeId,
                                                dataTypeInfoOptions, registry);
                                if (dataTypeInformation.isArrayType() == false)
                                {
                                    final int[] dimensions =
                                            MDAbstractArray.toInt(baseReader.h5
                                                    .getDataDimensionsForAttribute(attributeId,
                                                            registry));
                                    if (dimensions.length > 0)
                                    {
                                        dataTypeInformation.setDimensions(dimensions);
                                    }
                                }
                                return dataTypeInformation;
                            } catch (RuntimeException ex)
                            {
                                throw ex;
                            }
                        }
                    };
        return baseReader.runner.call(informationDeterminationRunnable);
    }

    @Override
    public HDF5DataSetInformation getDataSetInformation(final String dataSetPath)
    {
        return getDataSetInformation(dataSetPath, DataTypeInfoOptions.DEFAULT);
    }

    @Override
    public HDF5DataSetInformation getDataSetInformation(final String dataSetPath,
            final DataTypeInfoOptions dataTypeInfoOptions)
    {
        return getDataSetInformation(dataSetPath, dataTypeInfoOptions, true);
    }

    HDF5DataSetInformation getDataSetInformation(final String dataSetPath,
            final DataTypeInfoOptions dataTypeInfoOptions, final boolean fillInDimensions)
    {
        assert dataSetPath != null;

        baseReader.checkOpen();
        return baseReader.getDataSetInformation(dataSetPath, dataTypeInfoOptions, fillInDimensions);
    }

    @Override
    public long getSize(final String objectPath)
    {
        return getDataSetInformation(objectPath, DataTypeInfoOptions.MINIMAL).getSize();
    }

    @Override
    public long getNumberOfElements(final String objectPath)
    {
        return getDataSetInformation(objectPath, DataTypeInfoOptions.MINIMAL).getNumberOfElements();
    }

    @Override
    public int getElementSize(final String objectPath)
    {
        return getDataSetInformation(objectPath, DataTypeInfoOptions.MINIMAL, false)
                .getTypeInformation().getElementSize();
    }

    @Override
    public int getSpaceRank(String objectPath)
    {
        baseReader.checkOpen();
        return baseReader.getSpaceRank(objectPath);
    }

    @Override
    public long[] getSpaceDimensions(String objectPath)
    {
        baseReader.checkOpen();
        return baseReader.getSpaceDimensions(objectPath);
    }

    @Override
    public int getArrayRank(String objectPath)
    {
        final HDF5DataSetInformation info =
                getDataSetInformation(objectPath, DataTypeInfoOptions.MINIMAL, false);
        return info.getTypeInformation().getRank();
    }

    @Override
    public int[] getArrayDimensions(String objectPath)
    {
        final HDF5DataSetInformation info =
                getDataSetInformation(objectPath, DataTypeInfoOptions.MINIMAL, false);
        return info.getTypeInformation().getDimensions();
    }

    @Override
    public int getRank(String objectPath)
    {
        baseReader.checkOpen();
        return baseReader.getRank(objectPath);
    }

    @Override
    public long[] getDimensions(String objectPath)
    {
        baseReader.checkOpen();
        return baseReader.getDimensions(objectPath);
    }

    // /////////////////////
    // Copies
    // /////////////////////

    @Override
    public void copy(final String sourceObject, final IHDF5Writer destinationWriter,
            final String destinationObject)
    {
        baseReader.checkOpen();
        final HDF5Writer dwriter = (HDF5Writer) destinationWriter;
        if (dwriter.object() != this)
        {
            dwriter.checkOpen();
        }
        baseReader.copyObject(sourceObject, dwriter.getFileId(), destinationObject);
    }

    @Override
    public void copy(String sourceObject, IHDF5Writer destinationWriter)
    {
        copy(sourceObject, destinationWriter, "/");
    }

    @Override
    public void copyAll(IHDF5Writer destinationWriter)
    {
        copy("/", destinationWriter, "/");
    }

    // /////////////////////
    // Group
    // /////////////////////

    @Override
    public List<String> getGroupMembers(final String groupPath)
    {
        assert groupPath != null;

        baseReader.checkOpen();
        return baseReader.getGroupMembers(groupPath);
    }

    @Override
    public List<String> getAllGroupMembers(final String groupPath)
    {
        assert groupPath != null;

        baseReader.checkOpen();
        return baseReader.getAllGroupMembers(groupPath);
    }

    @Override
    public List<String> getGroupMemberPaths(final String groupPath)
    {
        assert groupPath != null;

        baseReader.checkOpen();
        return baseReader.getGroupMemberPaths(groupPath);
    }

    @Override
    public List<HDF5LinkInformation> getGroupMemberInformation(final String groupPath,
            boolean readLinkTargets)
    {
        baseReader.checkOpen();
        if (readLinkTargets)
        {
            return baseReader.h5.getGroupMemberLinkInfo(baseReader.fileId, groupPath, false,
                    baseReader.houseKeepingNameSuffix);
        } else
        {
            return baseReader.h5.getGroupMemberTypeInfo(baseReader.fileId, groupPath, false,
                    baseReader.houseKeepingNameSuffix);
        }
    }

    @Override
    public List<HDF5LinkInformation> getAllGroupMemberInformation(final String groupPath,
            boolean readLinkTargets)
    {
        baseReader.checkOpen();
        if (readLinkTargets)
        {
            return baseReader.h5.getGroupMemberLinkInfo(baseReader.fileId, groupPath, true,
                    baseReader.houseKeepingNameSuffix);
        } else
        {
            return baseReader.h5.getGroupMemberTypeInfo(baseReader.fileId, groupPath, true,
                    baseReader.houseKeepingNameSuffix);
        }
    }

    // /////////////////////
    // Types
    // /////////////////////

    @Override
    public HDF5DataTypeVariant tryGetTypeVariant(final String objectPath)
    {
        baseReader.checkOpen();
        return baseReader.tryGetTypeVariant(objectPath);
    }

    @Override
    public HDF5DataTypeVariant tryGetTypeVariant(String objectPath, String attributeName)
    {
        baseReader.checkOpen();
        return baseReader.tryGetTypeVariant(objectPath, attributeName);
    }

    // /////////////////////
    // Attributes
    // /////////////////////

    @Override
    public boolean hasAttribute(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<Boolean> writeRunnable = new ICallableWithCleanUp<Boolean>()
            {
                @Override
                public Boolean call(ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    return baseReader.h5.existsAttribute(objectId, attributeName);
                }
            };
        return baseReader.runner.call(writeRunnable);
    }

}
