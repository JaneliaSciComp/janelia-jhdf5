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

import static ch.systemsx.cisd.hdf5.HDF5Utils.createAttributeTypeVariantAttributeName;
import static ch.systemsx.cisd.hdf5.HDF5Utils.createObjectTypeVariantAttributeName;

import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * Implementation of {@link HDF5ObjectReadWriteInfoProviderHandler}.
 * 
 * @author Bernd Rinn
 */
final class HDF5ObjectReadWriteInfoProviderHandler extends HDF5ObjectReadOnlyInfoProviderHandler
        implements IHDF5ObjectReadWriteInfoProviderHandler
{
    private final HDF5BaseWriter baseWriter;

    HDF5ObjectReadWriteInfoProviderHandler(HDF5BaseWriter baseWriter)
    {
        super(baseWriter);

        assert baseWriter != null;
        this.baseWriter = baseWriter;
    }

    @Override
    public void createHardLink(String currentPath, String newPath)
    {
        assert currentPath != null;
        assert newPath != null;

        baseWriter.checkOpen();
        baseWriter.h5.createHardLink(baseWriter.fileId, currentPath, newPath);
    }

    @Override
    public void createSoftLink(String targetPath, String linkPath)
    {
        assert targetPath != null;
        assert linkPath != null;

        baseWriter.checkOpen();
        baseWriter.h5.createSoftLink(baseWriter.fileId, linkPath, targetPath);
    }

    @Override
    public void createOrUpdateSoftLink(String targetPath, String linkPath)
    {
        assert targetPath != null;
        assert linkPath != null;

        baseWriter.checkOpen();
        if (isSymbolicLink(linkPath))
        {
            delete(linkPath);
        }
        baseWriter.h5.createSoftLink(baseWriter.fileId, linkPath, targetPath);
    }

    @Override
    public void createExternalLink(String targetFileName, String targetPath, String linkPath)
            throws IllegalStateException
    {
        assert targetFileName != null;
        assert targetPath != null;
        assert linkPath != null;

        baseWriter.checkOpen();
        baseWriter.h5.createExternalLink(baseWriter.fileId, linkPath, targetFileName, targetPath);
    }

    @Override
    public void createOrUpdateExternalLink(String targetFileName, String targetPath, String linkPath)
            throws IllegalStateException
    {
        assert targetFileName != null;
        assert targetPath != null;
        assert linkPath != null;

        baseWriter.checkOpen();
        if (isSymbolicLink(linkPath))
        {
            delete(linkPath);
        }
        baseWriter.h5.createExternalLink(baseWriter.fileId, linkPath, targetFileName, targetPath);
    }

    @Override
    public void delete(String objectPath)
    {
        baseWriter.checkOpen();
        if (isGroup(objectPath, false))
        {
            for (String path : getGroupMemberPaths(objectPath))
            {
                delete(path);
            }
        }
        baseWriter.h5.deleteObject(baseWriter.fileId, objectPath);
    }

    @Override
    public void move(String oldLinkPath, String newLinkPath)
    {
        baseWriter.checkOpen();
        baseWriter.h5.moveLink(baseWriter.fileId, oldLinkPath, newLinkPath);
    }

    // /////////////////////
    // Group
    // /////////////////////

    @Override
    public void createGroup(final String groupPath)
    {
        baseWriter.checkOpen();
        baseWriter.h5.createGroup(baseWriter.fileId, groupPath);
    }

    @Override
    public void createGroup(final String groupPath, final int sizeHint)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createGroupRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    baseWriter.h5.createOldStyleGroup(baseWriter.fileId, groupPath, sizeHint,
                            registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createGroupRunnable);
    }

    @Override
    public void createGroup(final String groupPath, final int maxCompact, final int minDense)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> createGroupRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    baseWriter.h5.createNewStyleGroup(baseWriter.fileId, groupPath, maxCompact,
                            minDense, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(createGroupRunnable);
    }

    // /////////////////////
    // Attributes
    // /////////////////////

    @Override
    public void deleteAttribute(final String objectPath, final String name)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> deleteAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long objectId =
                            baseWriter.h5.openObject(baseWriter.fileId, objectPath, registry);
                    baseWriter.h5.deleteAttribute(objectId, name);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(deleteAttributeRunnable);
    }

    // /////////////////////
    // Data Sets
    // /////////////////////

    @Override
    public void setDataSetSize(final String objectPath, final long newSize)
    {
        setDataSetDimensions(objectPath, new long[]
            { newSize });
    }

    @Override
    public void setDataSetDimensions(final String objectPath, final long[] newDimensions)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> writeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    baseWriter.setDataSetDimensions(objectPath, newDimensions, registry);
                    return null; // Nothing to return.
                }
            };
        baseWriter.runner.call(writeRunnable);
    }

    // /////////////////////
    // Types
    // /////////////////////

    @Override
    public void setTypeVariant(final String objectPath, final HDF5DataTypeVariant typeVariant)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Void> addAttributeRunnable = new ICallableWithCleanUp<Void>()
                {
                    @Override
                    public Void call(ICleanUpRegistry registry)
                    {
                        if (baseWriter.useSimpleDataSpaceForAttributes)
                        {
                            final long dataSpaceId = baseWriter.h5.createSimpleDataSpace(new long[]
                                { 1 }, registry);
                            baseWriter.setAttribute(
                                    objectPath,
                                    createObjectTypeVariantAttributeName(baseWriter.houseKeepingNameSuffix),
                                    baseWriter.typeVariantDataType.getStorageTypeId(),
                                    baseWriter.typeVariantDataType.getNativeTypeId(),
                                    dataSpaceId,
                                    baseWriter.typeVariantDataType.getEnumType().toStorageForm(
                                            typeVariant.ordinal()), registry);
                        } else
                        {
                            baseWriter.setAttribute(
                                    objectPath,
                                    createObjectTypeVariantAttributeName(baseWriter.houseKeepingNameSuffix),
                                    baseWriter.typeVariantDataType.getStorageTypeId(),
                                    baseWriter.typeVariantDataType.getNativeTypeId(),
                                    -1,
                                    baseWriter.typeVariantDataType.getEnumType().toStorageForm(
                                            typeVariant.ordinal()), registry);
                        }
                        return null; // Nothing to return.
                    }
                };
                baseWriter.runner.call(addAttributeRunnable);
    }

    @Override
    public void setTypeVariant(final String objectPath, final String attributeName,
            final HDF5DataTypeVariant typeVariant)
    {
        baseWriter.checkOpen();
        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            if (baseWriter.useSimpleDataSpaceForAttributes)
                            {
                                final long dataSpaceId =
                                        baseWriter.h5.createSimpleDataSpace(new long[]
                                            { 1 }, registry);
                                baseWriter.setAttribute(
                                        objectPath,
                                        createAttributeTypeVariantAttributeName(attributeName,
                                                baseWriter.houseKeepingNameSuffix),
                                        baseWriter.typeVariantDataType.getStorageTypeId(),
                                        baseWriter.typeVariantDataType.getNativeTypeId(),
                                        dataSpaceId, baseWriter.typeVariantDataType.getEnumType()
                                                .toStorageForm(typeVariant.ordinal()), registry);
                            } else
                            {
                                baseWriter.setAttribute(
                                        objectPath,
                                        createAttributeTypeVariantAttributeName(attributeName,
                                                baseWriter.houseKeepingNameSuffix),
                                        baseWriter.typeVariantDataType.getStorageTypeId(),
                                        baseWriter.typeVariantDataType.getNativeTypeId(),
                                        -1,
                                        baseWriter.typeVariantDataType.getEnumType().toStorageForm(
                                                typeVariant.ordinal()), registry);
                            }
                            return null; // Nothing to return.
                        }
                    };
        baseWriter.runner.call(addAttributeRunnable);
    }

    @Override
    public void deleteTypeVariant(String objectPath)
    {
        deleteAttribute(objectPath,
                createObjectTypeVariantAttributeName(baseWriter.houseKeepingNameSuffix));
    }

    @Override
    public void deleteTypeVariant(String objectPath, String attributeName)
    {
        deleteAttribute(
                objectPath,
                createAttributeTypeVariantAttributeName(attributeName,
                        baseWriter.houseKeepingNameSuffix));
    }
}
