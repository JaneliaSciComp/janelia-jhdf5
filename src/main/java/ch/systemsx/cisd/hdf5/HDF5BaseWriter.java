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
import static ch.systemsx.cisd.hdf5.HDF5Utils.getDataTypeGroup;
import static ch.systemsx.cisd.hdf5.HDF5Utils.getTypeVariantDataTypePath;
import static ch.systemsx.cisd.hdf5.HDF5Utils.isEmpty;
import static ch.systemsx.cisd.hdf5.HDF5Utils.isNonPositive;
import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_SCALAR;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT32;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT8;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I16LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I32LE;
import static hdf.hdf5lib.HDF5Constants.H5T_STD_I8LE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import hdf.hdf5lib.exceptions.HDF5DatasetInterfaceException;
import hdf.hdf5lib.exceptions.HDF5JavaException;
import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.base.exceptions.IErrorStrategy;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.namedthread.NamingThreadPoolExecutor;
import ch.systemsx.cisd.hdf5.IHDF5CompoundInformationRetriever.IByteArrayInspector;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.FileFormatVersionBounds;
import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.SyncMode;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;
import ch.systemsx.cisd.hdf5.exceptions.HDF5FileNotFoundException;

/**
 * Class that provides base methods for reading and writing HDF5 files.
 * 
 * @author Bernd Rinn
 */
final class HDF5BaseWriter extends HDF5BaseReader
{

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

    private static final int MAX_TYPE_VARIANT_TYPES = 1024;

    private final static EnumSet<SyncMode> BLOCKING_SYNC_MODES = EnumSet.of(SyncMode.SYNC_BLOCK,
            SyncMode.SYNC_ON_FLUSH_BLOCK);

    private final static EnumSet<SyncMode> NON_BLOCKING_SYNC_MODES = EnumSet.of(SyncMode.SYNC,
            SyncMode.SYNC_ON_FLUSH);

    private final static EnumSet<SyncMode> SYNC_ON_CLOSE_MODES = EnumSet.of(SyncMode.SYNC_BLOCK,
            SyncMode.SYNC);

    /**
     * The size threshold for the COMPACT storage layout.
     */
    final static int COMPACT_LAYOUT_THRESHOLD = 256;
    
    /**
     * ExecutorService for calling <code>fsync(2)</code> in a non-blocking way.
     */
    private final static ExecutorService syncExecutor = new NamingThreadPoolExecutor("HDF5 Sync")
            .corePoolSize(3).daemonize();

    static
    {
        // Ensure all sync() calls are finished.
        Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    syncExecutor.shutdownNow();
                    try
                    {
                        syncExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (InterruptedException ex)
                    {
                        // Unexpected
                        ex.printStackTrace();
                    }
                }
            });
    }

    private final RandomAccessFile fileForSyncing;

    private enum Command
    {
        SYNC, CLOSE_ON_EXIT, CLOSE_SYNC, EXIT
    }

    private final BlockingQueue<Command> commandQueue;

    private final Set<Flushable> flushables = new LinkedHashSet<Flushable>();

    final boolean useExtentableDataTypes;

    final boolean overwriteFile;

    final boolean keepDataSetIfExists;

    final boolean useSimpleDataSpaceForAttributes;

    final SyncMode syncMode;

    final FileFormatVersionBounds fileFormat;

    HDF5BaseWriter(File hdf5File, boolean performNumericConversions, boolean useUTF8CharEncoding,
            boolean autoDereference, FileFormatVersionBounds fileFormat, MDCImageGeneration mdcGenerateImage, 
            boolean useExtentableDataTypes, boolean overwriteFile, boolean keepDataSetIfExists,
            boolean useSimpleDataSpaceForAttributes, String preferredHouseKeepingNameSuffix,
            SyncMode syncMode)
    {
        super(hdf5File, performNumericConversions, useUTF8CharEncoding, autoDereference,
                fileFormat, mdcGenerateImage, overwriteFile, preferredHouseKeepingNameSuffix);
        this.readOnly = false;
        try
        {
            this.fileForSyncing = new RandomAccessFile(hdf5File, "rw");
        } catch (FileNotFoundException ex)
        {
            // Should not be happening as openFile() was called in super()
            throw new HDF5JavaException("Cannot open RandomAccessFile: " + ex.getMessage());
        }
        this.fileFormat = fileFormat;
        this.useExtentableDataTypes = useExtentableDataTypes;
        this.overwriteFile = overwriteFile;
        this.keepDataSetIfExists = keepDataSetIfExists;
        this.useSimpleDataSpaceForAttributes = useSimpleDataSpaceForAttributes;
        this.syncMode = syncMode;
        readNamedDataTypes();
        saveNonDefaultHouseKeepingNameSuffix();
        commandQueue = new LinkedBlockingQueue<Command>();
        setupSyncThread();
    }

    private void setupSyncThread()
    {
        syncExecutor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    while (true)
                    {
                        try
                        {
                            switch (commandQueue.take())
                            {
                                case SYNC:
                                    syncNow();
                                    break;
                                case CLOSE_ON_EXIT:
                                    closeNow();
                                    return;
                                case CLOSE_SYNC:
                                    closeSync();
                                    return;
                                case EXIT:
                                    return;
                            }
                        } catch (InterruptedException ex)
                        {
                            // Shutdown has been triggered by showdownNow(), add
                            // <code>CLOSEHDF</code> to queue.
                            // (Note that a close() on a closed RandomAccessFile is harmless.)
                            commandQueue.add(Command.CLOSE_ON_EXIT);
                        }
                    }
                }
            });
    }

    @Override
    long openFile(FileFormatVersionBounds fileFormatInit, MDCImageGeneration mdcGenerateImage, boolean overwriteInit)
    {
        boolean generateMDCImage = mdcGenerateImage.isGenerateImageForNewFile();
        if (hdf5File.exists() && overwriteInit == false)
        {
            if (hdf5File.canWrite() == false)
            {
                throw new HDF5FileNotFoundException(hdf5File, "File is not writable.");
            }
            if (hdf5File.length() > 0 && mdcGenerateImage == MDCImageGeneration.KEEP_MDC_IMAGE)
            {
                generateMDCImage = HDF5Factory.hasMDCImage(hdf5File);
            }
            return h5.openFileReadWrite(hdf5File.getPath(), fileFormatInit, generateMDCImage, fileRegistry);
        } else
        {
            final File directory = hdf5File.getParentFile();
            if (directory.exists() == false)
            {
                throw new HDF5FileNotFoundException(directory, "Directory does not exist.");
            }
            if (directory.canWrite() == false)
            {
                throw new HDF5FileNotFoundException(directory, "Directory is not writable.");
            }
            return h5.createFile(hdf5File.getPath(), fileFormatInit, generateMDCImage, fileRegistry);
        }
    }

    /**
     * Calls <code>fsync(2)</code> in the current thread.
     */
    private void syncNow()
    {
        try
        {
            // Implementation note 1: Unix will call fsync(), , Windows: FlushFileBuffers()
            // Implementation note 2: We do not call fileForSyncing.getChannel().force(false) which
            // might be better in terms of performance as if shutdownNow() already has been
            // triggered on the syncExecutor and thus this thread has already been interrupted,
            // channel methods would throw a ClosedByInterruptException at us no matter what we do.
            fileForSyncing.getFD().sync();
        } catch (IOException ex)
        {
            final String msg =
                    (ex.getMessage() == null) ? ex.getClass().getSimpleName() : ex.getMessage();
            throw new HDF5JavaException("Error syncing file: " + msg);
        }
    }

    /**
     * Closes and, depending on the sync mode, syncs the HDF5 file in the current thread.
     * <p>
     * To be called from the syncer thread only.
     */
    private void closeNow()
    {
        synchronized (fileRegistry)
        {
            if (state == State.OPEN)
            {
                flushExternals();
                flushables.clear();
                super.close();
                if (SYNC_ON_CLOSE_MODES.contains(syncMode))
                {
                    syncNow();
                }
                closeSync();
            }
        }
    }

    private void closeSync()
    {
        try
        {
            fileForSyncing.close();
        } catch (IOException ex)
        {
            throw new HDF5JavaException("Error closing file: " + ex.getMessage());
        }
    }

    boolean addFlushable(Flushable flushable)
    {
        return flushables.add(flushable);
    }

    boolean removeFlushable(Flushable flushable)
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
            } catch (Throwable ex)
            {
                if (f instanceof IErrorStrategy)
                {
                    ((IErrorStrategy) f).dealWithError(ex);
                } else
                {
                    throw CheckedExceptionTunnel.wrapIfNecessary(ex);
                }
            }
        }
    }

    void flush()
    {
        synchronized (fileRegistry)
        {
            flushExternals();
            h5.flushFile(fileId);
            if (NON_BLOCKING_SYNC_MODES.contains(syncMode))
            {
                commandQueue.add(Command.SYNC);
            } else if (BLOCKING_SYNC_MODES.contains(syncMode))
            {
                syncNow();
            }
        }
    }

    void flushSyncBlocking()
    {
        synchronized (fileRegistry)
        {
            flushExternals();
            h5.flushFile(fileId);
            syncNow();
        }
    }

    @Override
    void close()
    {
        synchronized (fileRegistry)
        {
            if (state == State.OPEN)
            {
                flushExternals();
                flushables.clear();
                super.close();
                if (SyncMode.SYNC == syncMode)
                {
                    commandQueue.add(Command.SYNC);
                } else if (SyncMode.SYNC_BLOCK == syncMode)
                {
                    syncNow();
                }

                if (EnumSet.complementOf(NON_BLOCKING_SYNC_MODES).contains(syncMode))
                {
                    closeSync();
                    commandQueue.add(Command.EXIT);
                } else
                {
                    // End syncer thread and avoid a race condition for non-blocking sync modes as
                    // the
                    // syncer thread still may want to use the fileForSynching
                    commandQueue.add(Command.CLOSE_SYNC);
                }
            }
        }
    }

    void saveNonDefaultHouseKeepingNameSuffix()
    {
        // If it is empty, then there is nothing to save.
        if ("".equals(houseKeepingNameSuffix))
        {
            return;
        }
        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            final long objectId = h5.openObject(fileId, "/", registry);
                            setStringAttribute(objectId,
                                    HDF5Utils.HOUSEKEEPING_NAME_SUFFIX_ATTRIBUTE_NAME,
                                    houseKeepingNameSuffix, houseKeepingNameSuffix.length(), false,
                                    registry);
                            setIntAttributeAutoSize(objectId,
                                    HOUSEKEEPING_NAME_SUFFIX_STRINGLENGTH_ATTRIBUTE_NAME,
                                    houseKeepingNameSuffix.length(), registry);
                            return null; // Nothing to return.
                        }
                    };
        runner.call(addAttributeRunnable);
    }

    /**
     * Saves the <var>value</var> as integer attribute <var>attributeName</var> of
     * <var>objectId</var>, choosing the size of the integer type automatically based on the
     * <var>value</var>.
     * 
     * @param objectId The id of the data set object in the file.
     */
    private void setIntAttributeAutoSize(final long objectId, final String attributeName,
            final int value, ICleanUpRegistry registry)
    {
        if (value > Short.MAX_VALUE)
        {
            setAttribute(objectId, attributeName, H5T_STD_I32LE, H5T_NATIVE_INT32, -1, new int[]
                { value }, registry);
        } else if (value > Byte.MAX_VALUE)
        {
            setAttribute(objectId, attributeName, H5T_STD_I16LE, H5T_NATIVE_INT16, -1, new int[]
                { value }, registry);
        } else
        {
            setAttribute(objectId, attributeName, H5T_STD_I8LE, H5T_NATIVE_INT8, -1, new byte[]
                { (byte) value }, registry);
        }
    }

    @Override
    void commitDataType(final String dataTypePath, final long dataTypeId)
    {
        h5.commitDataType(fileId, dataTypePath, dataTypeId);
    }

    HDF5EnumerationType openOrCreateTypeVariantDataType(final HDF5Writer writer)
    {
        final String typeVariantTypePath = getTypeVariantDataTypePath(houseKeepingNameSuffix);
        final HDF5EnumerationType dataType;
        long dataTypeId = getDataTypeId(typeVariantTypePath);
        if (dataTypeId < 0
                || h5.getNumberOfMembers(dataTypeId) < HDF5DataTypeVariant.values().length)
        {
            final String typeVariantPath = findFirstUnusedTypeVariantPath(writer);
            dataType = createTypeVariantDataType();
            commitDataType(typeVariantPath, dataType.getStorageTypeId());
            writer.object().createOrUpdateSoftLink(typeVariantPath.substring(getDataTypeGroup(
                    houseKeepingNameSuffix).length() + 1), typeVariantTypePath);
        } else
        {
            final long nativeDataTypeId = h5.getNativeDataType(dataTypeId, fileRegistry);
            final String[] typeVariantNames = h5.getNamesForEnumOrCompoundMembers(dataTypeId);
            dataType =
                    new HDF5EnumerationType(fileId, dataTypeId, nativeDataTypeId,
                            typeVariantTypePath, typeVariantNames, this);

        }
        return dataType;
    }

    void setEnumArrayAttribute(final String objectPath, final String name,
            final HDF5EnumerationValueArray value)
    {
        assert objectPath != null;
        assert name != null;
        assert value != null;

        checkOpen();
        final ICallableWithCleanUp<Void> setAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long baseMemoryTypeId = value.getType().getNativeTypeId();
                    final long memoryTypeId =
                            h5.createArrayType(baseMemoryTypeId, value.getLength(), registry);
                    final long baseStorageTypeId = value.getType().getStorageTypeId();
                    final long storageTypeId =
                            h5.createArrayType(baseStorageTypeId, value.getLength(), registry);
                    setAttribute(objectPath, name, storageTypeId, memoryTypeId, -1,
                            value.toStorageForm(), registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(setAttributeRunnable);
    }

    void setEnumMDArrayAttribute(final String objectPath, final String name,
            final HDF5EnumerationValueMDArray value)
    {
        assert objectPath != null;
        assert name != null;
        assert value != null;

        checkOpen();
        final ICallableWithCleanUp<Void> setAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final long baseMemoryTypeId = value.getType().getNativeTypeId();
                    final long memoryTypeId =
                            h5.createArrayType(baseMemoryTypeId, value.dimensions(), registry);
                    final long baseStorageTypeId = value.getType().getStorageTypeId();
                    final long storageTypeId =
                            h5.createArrayType(baseStorageTypeId, value.dimensions(), registry);
                    setAttribute(objectPath, name, storageTypeId, memoryTypeId, -1,
                            value.toStorageForm(), registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(setAttributeRunnable);
    }

    <T> void setCompoundArrayAttribute(final String objectPath, final String attributeName,
            final HDF5CompoundType<T> type, final T[] data,
            final IByteArrayInspector inspectorOrNull)
    {
        assert objectPath != null;
        assert attributeName != null;
        assert data != null;

        checkOpen();
        type.check(fileId);
        final ICallableWithCleanUp<Void> setAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final byte[] byteArray =
                            type.getObjectByteifyer().byteify(type.getStorageTypeId(), data);
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArray);
                    }
                    final long baseMemoryTypeId = type.getNativeTypeId();
                    final long memoryTypeId =
                            h5.createArrayType(baseMemoryTypeId, data.length, registry);
                    final long baseStorageTypeId = type.getStorageTypeId();
                    final long storageTypeId =
                            h5.createArrayType(baseStorageTypeId, data.length, registry);
                    setAttribute(objectPath, attributeName, storageTypeId, memoryTypeId, -1,
                            byteArray, registry);
                    h5.reclaimCompoundVL(type, byteArray);

                    return null; // Nothing to return.
                }
            };
        runner.call(setAttributeRunnable);
    }

    <T> void setCompoundMDArrayAttribute(final String objectPath, final String attributeName,
            final HDF5CompoundType<T> type, final MDArray<T> data,
            final IByteArrayInspector inspectorOrNull)
    {
        assert objectPath != null;
        assert attributeName != null;
        assert data != null;

        checkOpen();
        type.check(fileId);
        final ICallableWithCleanUp<Void> setAttributeRunnable = new ICallableWithCleanUp<Void>()
            {
                @Override
                public Void call(ICleanUpRegistry registry)
                {
                    final byte[] byteArray =
                            type.getObjectByteifyer().byteify(type.getStorageTypeId(),
                                    data.getAsFlatArray());
                    if (inspectorOrNull != null)
                    {
                        inspectorOrNull.inspect(byteArray);
                    }
                    final long baseMemoryTypeId = type.getNativeTypeId();
                    final long memoryTypeId =
                            h5.createArrayType(baseMemoryTypeId, data.dimensions(), registry);
                    final long baseStorageTypeId = type.getStorageTypeId();
                    final long storageTypeId =
                            h5.createArrayType(baseStorageTypeId, data.dimensions(), registry);
                    setAttribute(objectPath, attributeName, storageTypeId, memoryTypeId, -1,
                            byteArray, registry);
                    h5.reclaimCompoundVL(type, byteArray);

                    return null; // Nothing to return.
                }
            };
        runner.call(setAttributeRunnable);
    }

    private String findFirstUnusedTypeVariantPath(final HDF5Reader reader)
    {
        int number = 0;
        String path;
        do
        {
            path = getTypeVariantDataTypePath(houseKeepingNameSuffix) + "." + (number++);
        } while (reader.object().exists(path, false) && number < MAX_TYPE_VARIANT_TYPES);
        return path;
    }

    /**
     * Write a scalar value provided as <code>byte[]</code>.
     */
    void writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final byte[] value)
    {
        assert dataSetPath != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;
        assert value != null;

        final ICallableWithCleanUp<Object> writeScalarRunnable = new ICallableWithCleanUp<Object>()
            {
                @Override
                public Object call(ICleanUpRegistry registry)
                {
                    writeScalar(dataSetPath, storageDataTypeId, nativeDataTypeId, value, true,
                            keepDataSetIfExists, registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(writeScalarRunnable);
    }

    /**
     * Internal method for writing a scalar value provided as <code>byte[]</code>.
     */
    long writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final byte[] value, final boolean compactLayout,
            final boolean keepDatasetIfExists, ICleanUpRegistry registry)
    {
        final long dataSetId;
        boolean exists = h5.exists(fileId, dataSetPath);
        if (exists && keepDatasetIfExists == false)
        {
            h5.deleteObject(fileId, dataSetPath);
            exists = false;
        }
        if (exists)
        {
            dataSetId = h5.openObject(fileId, dataSetPath, registry);
        } else
        {
            dataSetId =
                    h5.createScalarDataSet(fileId, storageDataTypeId, dataSetPath, compactLayout,
                            registry);
        }
        H5Dwrite(dataSetId, nativeDataTypeId, H5S_SCALAR, H5S_SCALAR, H5P_DEFAULT, value);
        return dataSetId;
    }

    /**
     * Write a scalar value provided as <code>byte</code>.
     */
    void writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final byte value)
    {
        assert dataSetPath != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;

        final ICallableWithCleanUp<Object> writeScalarRunnable = new ICallableWithCleanUp<Object>()
            {
                @Override
                public Object call(ICleanUpRegistry registry)
                {
                    writeScalar(dataSetPath, storageDataTypeId, nativeDataTypeId, value, true,
                            true, registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(writeScalarRunnable);
    }

    /**
     * Internal method for writing a scalar value provided as <code>byte</code>.
     */
    long writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final byte value, final boolean compactLayout,
            final boolean keepDatasetIfExists, ICleanUpRegistry registry)
    {
        final long dataSetId;
        boolean exists = h5.exists(fileId, dataSetPath);
        if (exists && keepDatasetIfExists == false)
        {
            h5.deleteObject(fileId, dataSetPath);
            exists = false;
        }
        if (exists)
        {
            dataSetId = h5.openObject(fileId, dataSetPath, registry);
        } else
        {
            dataSetId =
                    h5.createScalarDataSet(fileId, storageDataTypeId, dataSetPath, compactLayout,
                            registry);
        }
        H5Dwrite(dataSetId, nativeDataTypeId, H5S_SCALAR, H5S_SCALAR, H5P_DEFAULT, new byte[]
            { value });
        return dataSetId;
    }

    /**
     * Write a scalar value provided as <code>short</code>.
     */
    void writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final short value)
    {
        assert dataSetPath != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;

        final ICallableWithCleanUp<Object> writeScalarRunnable = new ICallableWithCleanUp<Object>()
            {
                @Override
                public Object call(ICleanUpRegistry registry)
                {
                    writeScalar(dataSetPath, storageDataTypeId, nativeDataTypeId, value, true,
                            true, registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(writeScalarRunnable);
    }

    /**
     * Internal method for writing a scalar value provided as <code>short</code>.
     */
    long writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final short value, final boolean compactLayout,
            final boolean keepDatasetIfExists, ICleanUpRegistry registry)
    {
        final long dataSetId;
        boolean exists = h5.exists(fileId, dataSetPath);
        if (exists && keepDatasetIfExists == false)
        {
            h5.deleteObject(fileId, dataSetPath);
            exists = false;
        }
        if (exists)
        {
            dataSetId = h5.openObject(fileId, dataSetPath, registry);
        } else
        {
            dataSetId =
                    h5.createScalarDataSet(fileId, storageDataTypeId, dataSetPath, compactLayout,
                            registry);
        }
        H5Dwrite(dataSetId, nativeDataTypeId, H5S_SCALAR, H5S_SCALAR, H5P_DEFAULT, new short[]
            { value });
        return dataSetId;
    }

    /**
     * Write a scalar value provided as <code>int</code>.
     */
    void writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final int value)
    {
        assert dataSetPath != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;

        final ICallableWithCleanUp<Object> writeScalarRunnable = new ICallableWithCleanUp<Object>()
            {
                @Override
                public Object call(ICleanUpRegistry registry)
                {
                    writeScalar(dataSetPath, storageDataTypeId, nativeDataTypeId, value, true,
                            true, registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(writeScalarRunnable);
    }

    /**
     * Internal method for writing a scalar value provided as <code>int</code>.
     */
    long writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final int value, final boolean compactLayout,
            final boolean keepDatasetIfExists, ICleanUpRegistry registry)
    {
        final long dataSetId;
        boolean exists = h5.exists(fileId, dataSetPath);
        if (exists && keepDatasetIfExists == false)
        {
            h5.deleteObject(fileId, dataSetPath);
            exists = false;
        }
        if (exists)
        {
            dataSetId = h5.openObject(fileId, dataSetPath, registry);
        } else
        {
            dataSetId =
                    h5.createScalarDataSet(fileId, storageDataTypeId, dataSetPath, compactLayout,
                            registry);
        }
        H5Dwrite(dataSetId, nativeDataTypeId, H5S_SCALAR, H5S_SCALAR, H5P_DEFAULT, new int[]
            { value });
        return dataSetId;
    }

    /**
     * Write a scalar value provided as <code>long</code>.
     */
    void writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final long value)
    {
        assert dataSetPath != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;

        final ICallableWithCleanUp<Object> writeScalarRunnable = new ICallableWithCleanUp<Object>()
            {
                @Override
                public Object call(ICleanUpRegistry registry)
                {
                    writeScalar(dataSetPath, storageDataTypeId, nativeDataTypeId, value, true,
                            true, registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(writeScalarRunnable);
    }

    /**
     * Internal method for writing a scalar value provided as <code>long</code>.
     */
    long writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final long value, final boolean compactLayout,
            final boolean keepDatasetIfExists, ICleanUpRegistry registry)
    {
        final long dataSetId;
        boolean exists = h5.exists(fileId, dataSetPath);
        if (exists && keepDatasetIfExists == false)
        {
            h5.deleteObject(fileId, dataSetPath);
            exists = false;
        }
        if (exists)
        {
            dataSetId = h5.openObject(fileId, dataSetPath, registry);
        } else
        {
            dataSetId =
                    h5.createScalarDataSet(fileId, storageDataTypeId, dataSetPath, compactLayout,
                            registry);
        }
        H5Dwrite(dataSetId, nativeDataTypeId, H5S_SCALAR, H5S_SCALAR, H5P_DEFAULT, new long[]
            { value });
        return dataSetId;
    }

    /**
     * Write a scalar value provided as <code>float</code>.
     */
    void writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final float value)
    {
        assert dataSetPath != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;

        final ICallableWithCleanUp<Object> writeScalarRunnable = new ICallableWithCleanUp<Object>()
            {
                @Override
                public Object call(ICleanUpRegistry registry)
                {
                    writeScalar(dataSetPath, storageDataTypeId, nativeDataTypeId, value, true,
                            keepDataSetIfExists, registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(writeScalarRunnable);
    }

    /**
     * Internal method for writing a scalar value provided as <code>float</code>.
     */
    long writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final float value, final boolean compactLayout,
            final boolean keepDatasetIfExists, ICleanUpRegistry registry)
    {
        final long dataSetId;
        boolean exists = h5.exists(fileId, dataSetPath);
        if (exists && keepDatasetIfExists == false)
        {
            h5.deleteObject(fileId, dataSetPath);
            exists = false;
        }
        if (exists)
        {
            dataSetId = h5.openObject(fileId, dataSetPath, registry);
        } else
        {
            dataSetId =
                    h5.createScalarDataSet(fileId, storageDataTypeId, dataSetPath, compactLayout,
                            registry);
        }
        H5Dwrite(dataSetId, nativeDataTypeId, H5S_SCALAR, H5S_SCALAR, H5P_DEFAULT, new float[]
            { value });
        return dataSetId;
    }

    /**
     * Write a scalar value provided as <code>double</code>.
     */
    void writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final double value)
    {
        assert dataSetPath != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;

        final ICallableWithCleanUp<Object> writeScalarRunnable = new ICallableWithCleanUp<Object>()
            {
                @Override
                public Object call(ICleanUpRegistry registry)
                {
                    writeScalar(dataSetPath, storageDataTypeId, nativeDataTypeId, value, true,
                            true, registry);
                    return null; // Nothing to return.
                }
            };
        runner.call(writeScalarRunnable);
    }

    /**
     * Internal method for writing a scalar value provided as <code>double</code>.
     */
    long writeScalar(final String dataSetPath, final long storageDataTypeId,
            final long nativeDataTypeId, final double value, final boolean compactLayout,
            final boolean keepDatasetIfExists, ICleanUpRegistry registry)
    {
        final long dataSetId;
        boolean exists = h5.exists(fileId, dataSetPath);
        if (exists && keepDatasetIfExists == false)
        {
            h5.deleteObject(fileId, dataSetPath);
            exists = false;
        }
        if (exists)
        {
            dataSetId = h5.openObject(fileId, dataSetPath, registry);
        } else
        {
            dataSetId =
                    h5.createScalarDataSet(fileId, storageDataTypeId, dataSetPath, compactLayout,
                            registry);
        }
        H5Dwrite(dataSetId, nativeDataTypeId, H5S_SCALAR, H5S_SCALAR, H5P_DEFAULT, new double[]
            { value });
        return dataSetId;
    }

    /**
     * Writes a variable-length string array data set.
     */
    void writeStringVL(long dataSetId, long memorySpaceId, long fileSpaceId, String[] value)
    {
        h5.writeStringVL(dataSetId, variableLengthStringDataTypeId, memorySpaceId, fileSpaceId,
                value);
    }

    /**
     * Writes a variable-length string array data set.
     */
    void writeStringVL(long dataSetId, String[] value)
    {
        h5.writeStringVL(dataSetId, variableLengthStringDataTypeId, value);
    }

    /**
     * Writes a variable-length string array attribute.
     */
    void writeAttributeStringVL(long attributeId, String[] value)
    {
        h5.writeAttributeStringVL(attributeId, variableLengthStringDataTypeId, value);
    }

    /**
     * Creates a data set.
     */
    long createDataSet(final String objectPath, final long storageDataTypeId,
            final HDF5AbstractStorageFeatures features, final long[] dimensions,
            final long[] chunkSizeOrNull, int elementLength, final ICleanUpRegistry registry)
    {
        final long dataSetId;
        final boolean empty = isEmpty(dimensions);
        final boolean chunkSizeProvided =
                (chunkSizeOrNull != null && isNonPositive(chunkSizeOrNull) == false);
        final long[] definitiveChunkSizeOrNull;
        if (h5.exists(fileId, objectPath))
        {
            if (keepDataIfExists(features))
            {
                return h5.openDataSet(fileId, objectPath, registry);
            }
            h5.deleteObject(fileId, objectPath);
        }
        if (empty)
        {
            definitiveChunkSizeOrNull =
                    chunkSizeProvided ? chunkSizeOrNull : HDF5Utils.tryGetChunkSize(dimensions,
                            elementLength, features.requiresChunking(), true);
        } else if (features.tryGetProposedLayout() == HDF5StorageLayout.COMPACT
                || features.tryGetProposedLayout() == HDF5StorageLayout.CONTIGUOUS
                || (useExtentableDataTypes == false) && features.requiresChunking() == false)
        {
            definitiveChunkSizeOrNull = null;
        } else if (chunkSizeProvided)
        {
            definitiveChunkSizeOrNull = chunkSizeOrNull;
        } else
        {
            definitiveChunkSizeOrNull =
                    HDF5Utils
                            .tryGetChunkSize(
                                    dimensions,
                                    elementLength,
                                    features.requiresChunking(),
                                    useExtentableDataTypes
                                            || features.tryGetProposedLayout() == HDF5StorageLayout.CHUNKED);
        }
        final HDF5StorageLayout layout =
                determineLayout(storageDataTypeId, dimensions, definitiveChunkSizeOrNull,
                        features.tryGetProposedLayout());
        dataSetId =
                h5.createDataSet(fileId, dimensions, definitiveChunkSizeOrNull, storageDataTypeId,
                        features, objectPath, layout, registry);
        return dataSetId;
    }

    /**
     * Creates a detached data set.
     */
    HDF5DataSet createDataSet(final String objectPath, final long storageDataTypeId,
            final HDF5AbstractStorageFeatures features, final long[] dimensions,
            final long[] chunkSizeOrNull, final int elementLength)
    {
        final ICallableWithCleanUp<HDF5DataSet> openDataSetCallable =
                new ICallableWithCleanUp<HDF5DataSet>()
                    {
                        @Override
                        public HDF5DataSet call(ICleanUpRegistry registry)
                        {
                            final boolean empty = isEmpty(dimensions);
                            final boolean chunkSizeProvided =
                                    (chunkSizeOrNull != null && isNonPositive(chunkSizeOrNull) == false);
                            final long[] definitiveChunkSizeOrNull;
                            if (h5.exists(fileId, objectPath))
                            {
                                if (keepDataIfExists(features))
                                {
                                    return openDataSet(objectPath);
                                }
                                h5.deleteObject(fileId, objectPath);
                            }
                            if (empty)
                            {
                                definitiveChunkSizeOrNull =
                                        chunkSizeProvided ? chunkSizeOrNull : HDF5Utils.tryGetChunkSize(dimensions,
                                                elementLength, features.requiresChunking(), true);
                            } else if (features.tryGetProposedLayout() == HDF5StorageLayout.COMPACT
                                    || features.tryGetProposedLayout() == HDF5StorageLayout.CONTIGUOUS
                                    || (useExtentableDataTypes == false) && features.requiresChunking() == false)
                            {
                                definitiveChunkSizeOrNull = null;
                            } else if (chunkSizeProvided)
                            {
                                definitiveChunkSizeOrNull = chunkSizeOrNull;
                            } else
                            {
                                definitiveChunkSizeOrNull =
                                        HDF5Utils
                                                .tryGetChunkSize(
                                                        dimensions,
                                                        elementLength,
                                                        features.requiresChunking(),
                                                        useExtentableDataTypes
                                                                || features.tryGetProposedLayout() == HDF5StorageLayout.CHUNKED);
                            }
                            final HDF5StorageLayout layout =
                                    determineLayout(storageDataTypeId, dimensions, definitiveChunkSizeOrNull,
                                            features.tryGetProposedLayout());
                            final HDF5DataSet dataSet = h5.createDataSetDetached(HDF5BaseWriter.this, dimensions, 
                                            definitiveChunkSizeOrNull, storageDataTypeId, features, objectPath, layout, registry);
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

    /**
     * Creates a data set template.
     */
    HDF5DataSetTemplate createDataSetTemplate(final long storageDataTypeId,
            final HDF5AbstractStorageFeatures features, final long[] dimensions,
            final long[] chunkSizeOrNull, int elementLength)
    {
        final boolean empty = isEmpty(dimensions);
        final boolean chunkSizeProvided =
                (chunkSizeOrNull != null && isNonPositive(chunkSizeOrNull) == false);
        final long[] definitiveChunkSizeOrNull;
        if (empty)
        {
            definitiveChunkSizeOrNull =
                    chunkSizeProvided ? chunkSizeOrNull : HDF5Utils.tryGetChunkSize(dimensions,
                            elementLength, features.requiresChunking(), true);
        } else if (features.tryGetProposedLayout() == HDF5StorageLayout.COMPACT
                || features.tryGetProposedLayout() == HDF5StorageLayout.CONTIGUOUS
                || (useExtentableDataTypes == false) && features.requiresChunking() == false)
        {
            definitiveChunkSizeOrNull = null;
        } else if (chunkSizeProvided)
        {
            definitiveChunkSizeOrNull = chunkSizeOrNull;
        } else
        {
            definitiveChunkSizeOrNull =
                    HDF5Utils
                            .tryGetChunkSize(
                                    dimensions,
                                    elementLength,
                                    features.requiresChunking(),
                                    useExtentableDataTypes
                                            || features.tryGetProposedLayout() == HDF5StorageLayout.CHUNKED);
        }
        final HDF5StorageLayout layout =
                determineLayout(storageDataTypeId, dimensions, definitiveChunkSizeOrNull,
                        features.tryGetProposedLayout());
        return h5.createDataSetTemplateLowLevel(fileId, dimensions, definitiveChunkSizeOrNull,
                storageDataTypeId, features, layout, fileFormat);
    }

    /**
     * Creates a new data set from a template. The data set must not yet exist or else an exception
     * from the HDF5 library is thrown.
     */
    long createDataSetFromTemplate(final String objectPath,
            final HDF5DataSetTemplate dataSetTemplate, final ICleanUpRegistry registry)
    {
        return h5.createDataSetSimple(fileId, dataSetTemplate.getStorageDataTypeId(),
                dataSetTemplate.getDataspaceId(), dataSetTemplate.getDataSetCreationPropertyListId(), 
                objectPath, registry);
    }

    boolean keepDataIfExists(final HDF5AbstractStorageFeatures features)
    {
        switch (features.getDatasetReplacementPolicy())
        {
            case ENFORCE_KEEP_EXISTING:
                return true;
            case ENFORCE_REPLACE_WITH_NEW:
                return false;
            case USE_WRITER_DEFAULT:
            default:
                return keepDataSetIfExists;
        }
    }

    /**
     * Determine which {@link HDF5StorageLayout} to use for the given <var>storageDataTypeId</var>.
     */
    HDF5StorageLayout determineLayout(final long storageDataTypeId, final long[] dimensions,
            final long[] chunkSizeOrNull, final HDF5StorageLayout proposedLayoutOrNull)
    {
        if (chunkSizeOrNull != null)
        {
            return HDF5StorageLayout.CHUNKED;
        }
        if (proposedLayoutOrNull != null)
        {
            return proposedLayoutOrNull;
        }
        if (computeSizeForDimensions(storageDataTypeId, dimensions) < HDF5BaseWriter.COMPACT_LAYOUT_THRESHOLD)
        {
            return HDF5StorageLayout.COMPACT;
        }
        return HDF5StorageLayout.CONTIGUOUS;
    }

    private int computeSizeForDimensions(long dataTypeId, long[] dimensions)
    {
        int size = h5.getDataTypeSize(dataTypeId);
        for (long d : dimensions)
        {
            size *= d;
        }
        return size;
    }

    /**
     * Returns the data set id for the given <var>objectPath</var>. If the data sets exists, it
     * depends on the <code>features</code> and on the status of <code>keepDataSetIfExists</code>
     * whether the existing data set will be opened or whether the data set will be deleted and
     * re-created.
     */
    long getOrCreateDataSetId(final String objectPath, final long storageDataTypeId,
            final long[] dimensions, int elementLength, final HDF5AbstractStorageFeatures features,
            ICleanUpRegistry registry)
    {
        final long dataSetId;
        boolean exists = h5.exists(fileId, objectPath);
        final boolean isRef = h5.isReference(objectPath);
        if (exists && isRef == false && keepDataIfExists(features) == false)
        {
            h5.deleteObject(fileId, objectPath);
            exists = false;
        }
        if (exists || isRef)
        {
            dataSetId =
                    h5.openAndExtendDataSet(fileId, objectPath, fileFormat, dimensions,
                            true, registry);
        } else
        {
            dataSetId =
                    createDataSet(objectPath, storageDataTypeId, features, dimensions, null,
                            elementLength, registry);
        }
        return dataSetId;
    }

    void setDataSetDimensions(final String objectPath, final long[] newDimensions,
            ICleanUpRegistry registry)
    {
        assert newDimensions != null;

        final long dataSetId = h5.openDataSet(fileId, objectPath, registry);
        try
        {
            h5.setDataSetExtentChunked(dataSetId, newDimensions);
        } catch (HDF5DatasetInterfaceException ex)
        {
            if (HDF5StorageLayout.CHUNKED != h5.getLayout(dataSetId, registry))
            {
                throw new HDF5JavaException("Cannot change dimensions of non-extendable data set.");
            } else
            {
                throw ex;
            }
        }
    }

    //
    // Attributes
    //

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final byte[] value,
            ICleanUpRegistry registry)
    {
        assert objectPath != null;
        assert name != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;
        assert value != null;

        final long objectId = h5.openObject(fileId, objectPath, registry);
        setAttribute(objectId, name, storageDataTypeId, nativeDataTypeId, dataSpaceId, value,
                registry);
    }

    void setAttribute(final long objectId, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final byte[] value,
            ICleanUpRegistry registry)
    {
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId =
                        h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
            }
        } else
        {
            attributeId =
                    h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
        }
        h5.writeAttribute(attributeId, nativeDataTypeId, value);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final short[] value)
    {
        assert objectPath != null;
        assert name != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;
        assert value != null;

        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            setAttribute(objectPath, name, storageDataTypeId, nativeDataTypeId,
                                    dataSpaceId, value, registry);
                            return null; // Nothing to return.
                        }
                    };
        runner.call(addAttributeRunnable);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final short[] value,
            ICleanUpRegistry registry)
    {
        final long objectId = h5.openObject(fileId, objectPath, registry);
        setAttribute(objectId, name, storageDataTypeId, nativeDataTypeId, dataSpaceId, value,
                registry);
    }

    void setAttribute(final long objectId, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final short[] value,
            ICleanUpRegistry registry)
    {
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId =
                        h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
            }
        } else
        {
            attributeId =
                    h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
        }
        h5.writeAttribute(attributeId, nativeDataTypeId, value);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final int[] value)
    {
        assert objectPath != null;
        assert name != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;
        assert value != null;

        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            setAttribute(objectPath, name, storageDataTypeId, nativeDataTypeId,
                                    dataSpaceId, value, registry);
                            return null; // Nothing to return.
                        }
                    };
        runner.call(addAttributeRunnable);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final int[] value,
            ICleanUpRegistry registry)
    {
        final long objectId = h5.openObject(fileId, objectPath, registry);
        setAttribute(objectId, name, storageDataTypeId, nativeDataTypeId, dataSpaceId, value,
                registry);
    }

    void setAttribute(final long objectId, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final int[] value,
            ICleanUpRegistry registry)
    {
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId =
                        h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
            }
        } else
        {
            attributeId =
                    h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
        }
        h5.writeAttribute(attributeId, nativeDataTypeId, value);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final long[] value)
    {
        assert objectPath != null;
        assert name != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;
        assert value != null;

        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            setAttribute(objectPath, name, storageDataTypeId, nativeDataTypeId,
                                    dataSpaceId, value, registry);
                            return null; // Nothing to return.
                        }
                    };
        runner.call(addAttributeRunnable);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final long[] value,
            ICleanUpRegistry registry)
    {
        final long objectId = h5.openObject(fileId, objectPath, registry);
        setAttribute(objectId, name, storageDataTypeId, nativeDataTypeId, dataSpaceId, value,
                registry);
    }

    void setAttribute(final String objectPath, final String name,
            final HDF5DataTypeVariant typeVariant, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final long[] value,
            ICleanUpRegistry registry)
    {
        assert objectPath != null;
        assert name != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;
        assert value != null;

        final long objectId = h5.openObject(fileId, objectPath, registry);
        setAttribute(objectPath, name, storageDataTypeId, nativeDataTypeId, dataSpaceId, value,
                registry);
        setTypeVariant(objectId, name, (dataSpaceId != -1), typeVariant, registry);
    }

    void setAttribute(final long objectId, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final long[] value,
            ICleanUpRegistry registry)
    {
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId =
                        h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
            }
        } else
        {
            attributeId =
                    h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
        }
        h5.writeAttribute(attributeId, nativeDataTypeId, value);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final float[] value)
    {
        assert objectPath != null;
        assert name != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;
        assert value != null;

        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            setAttribute(objectPath, name, storageDataTypeId, nativeDataTypeId,
                                    dataSpaceId, value, registry);
                            return null; // Nothing to return.
                        }
                    };
        runner.call(addAttributeRunnable);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final float[] value,
            ICleanUpRegistry registry)
    {
        final long objectId = h5.openObject(fileId, objectPath, registry);
        setAttribute(objectId, name, storageDataTypeId, nativeDataTypeId, dataSpaceId, value,
                registry);
    }

    void setAttribute(final long objectId, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final float[] value,
            ICleanUpRegistry registry)
    {
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId =
                        h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
            }
        } else
        {
            attributeId =
                    h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
        }
        h5.writeAttribute(attributeId, nativeDataTypeId, value);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final double[] value)
    {
        assert objectPath != null;
        assert name != null;
        assert storageDataTypeId >= 0;
        assert nativeDataTypeId >= 0;
        assert value != null;

        final ICallableWithCleanUp<Object> addAttributeRunnable =
                new ICallableWithCleanUp<Object>()
                    {
                        @Override
                        public Object call(ICleanUpRegistry registry)
                        {
                            setAttribute(objectPath, name, storageDataTypeId, nativeDataTypeId,
                                    dataSpaceId, value, registry);
                            return null; // Nothing to return.
                        }
                    };
        runner.call(addAttributeRunnable);
    }

    void setAttribute(final String objectPath, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final double[] value,
            ICleanUpRegistry registry)
    {
        final long objectId = h5.openObject(fileId, objectPath, registry);
        setAttribute(objectId, name, storageDataTypeId, nativeDataTypeId, dataSpaceId, value,
                registry);
    }

    void setAttribute(final long objectId, final String name, final long storageDataTypeId,
            final long nativeDataTypeId, final long dataSpaceId, final double[] value,
            ICleanUpRegistry registry)
    {
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId =
                        h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
            }
        } else
        {
            attributeId =
                    h5.createAttribute(objectId, name, storageDataTypeId, dataSpaceId, registry);
        }
        h5.writeAttribute(attributeId, nativeDataTypeId, value);
    }

    void setTypeVariant(final long objectId, final HDF5DataTypeVariant typeVariant,
            ICleanUpRegistry registry)
    {
        setAttribute(objectId, createObjectTypeVariantAttributeName(houseKeepingNameSuffix),
                typeVariantDataType.getStorageTypeId(), typeVariantDataType.getNativeTypeId(), -1,
                typeVariantDataType.getEnumType().toStorageForm(typeVariant.ordinal()), registry);
    }

    void setTypeVariant(final long objectId, final String attributeName,
            final boolean enforceSimpleDataSpace, final HDF5DataTypeVariant typeVariant,
            ICleanUpRegistry registry)
    {
        final long dataSpaceId = enforceSimpleDataSpace ? h5.createSimpleDataSpace(new long[]
            { 1 }, registry) : -1;
        setAttribute(objectId,
                createAttributeTypeVariantAttributeName(attributeName, houseKeepingNameSuffix),
                typeVariantDataType.getStorageTypeId(), typeVariantDataType.getNativeTypeId(),
                dataSpaceId,
                typeVariantDataType.getEnumType().toStorageForm(typeVariant.ordinal()), registry);
    }

    void setStringAttribute(final long objectId, final String name, final String value,
            final int maxLength, final boolean lengthFitsValue, ICleanUpRegistry registry)
    {
        final byte[] bytes;
        final int realMaxLengthInBytes;
        if (lengthFitsValue)
        {
            bytes = StringUtils.toBytes(value, encodingForNewDataSets);
            realMaxLengthInBytes = (bytes.length == 0) ? 1 : bytes.length;
        } else
        {
            bytes = StringUtils.toBytes(value, maxLength, encodingForNewDataSets);
            realMaxLengthInBytes =
                    encodingForNewDataSets.getMaxBytesPerChar()
                            * ((maxLength == 0) ? 1 : maxLength);
        }
        final long storageDataTypeId = h5.createDataTypeString(realMaxLengthInBytes, registry);
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId = h5.createAttribute(objectId, name, storageDataTypeId, -1, registry);
            }
        } else
        {
            attributeId = h5.createAttribute(objectId, name, storageDataTypeId, -1, registry);
        }
        h5.writeAttribute(attributeId, storageDataTypeId,
                StringUtils.cutOrPadBytes(bytes, realMaxLengthInBytes));
    }

    class StringArrayBuffer
    {
        private byte[] buf;

        private int len;

        private int realMaxLengthPerString;

        private boolean valueContainsChar0;

        private int[] lengths;

        private final int maxLengthPerString;

        private final boolean lengthFitsValue;

        StringArrayBuffer(int maxLengthPerString, boolean lengthFitsValue)
        {
            this.maxLengthPerString = maxLengthPerString;
            this.lengthFitsValue = lengthFitsValue;
        }

        void addAll(String[] array)
        {
            if (lengthFitsValue)
            {
                addAllLengthFitsValue(array);
            } else
            {
                addAllLengthFixedLength(array);
            }
        }

        private void addAllLengthFixedLength(String[] array)
        {
            this.realMaxLengthPerString =
                    encodingForNewDataSets.getMaxBytesPerChar() * maxLengthPerString;
            this.buf = new byte[realMaxLengthPerString * array.length];
            this.lengths = new int[array.length];
            int idx = 0;
            for (String s : array)
            {
                final byte[] data =
                        StringUtils.toBytes(s, maxLengthPerString, encodingForNewDataSets);
                final int dataLen = Math.min(data.length, realMaxLengthPerString);
                final int newLen = len + realMaxLengthPerString;
                System.arraycopy(data, 0, buf, len, dataLen);
                len = newLen;
                if (valueContainsChar0 == false)
                {
                    valueContainsChar0 |= s.contains("\0");
                }
                lengths[idx++] = dataLen;
            }
        }

        private void addAllLengthFitsValue(String[] array)
        {
            final byte[][] data = new byte[array.length][];
            this.lengths = new int[array.length];
            int idx = 0;
            for (String s : array)
            {
                final byte[] bytes = StringUtils.toBytes(s, encodingForNewDataSets);
                realMaxLengthPerString = Math.max(realMaxLengthPerString, bytes.length);
                data[idx] = bytes;
                lengths[idx] = bytes.length;
                if (valueContainsChar0 == false)
                {
                    valueContainsChar0 |= s.contains("\0");
                }
                ++idx;
            }
            this.buf = new byte[realMaxLengthPerString * array.length];
            for (byte[] bytes : data)
            {
                System.arraycopy(bytes, 0, buf, len, bytes.length);
                len = len + realMaxLengthPerString;
            }
        }

        byte[] toArray()
        {
            return StringUtils.cutOrPadBytes(buf, len);
        }

        int getMaxLengthInByte()
        {
            return (realMaxLengthPerString == 0) ? 1 : realMaxLengthPerString;
        }

        boolean shouldSaveExplicitLength()
        {
            return valueContainsChar0 || (realMaxLengthPerString == 0);
        }

        int[] getLengths()
        {
            return lengths;
        }
    }

    void setStringArrayAttribute(final long objectId, final String name, final String[] value,
            final int maxLength, final boolean lengthFitsValue, ICleanUpRegistry registry)
    {
        final StringArrayBuffer array = new StringArrayBuffer(maxLength, lengthFitsValue);
        array.addAll(value);
        final byte[] arrData = array.toArray();
        final long stringDataTypeId = h5.createDataTypeString(array.getMaxLengthInByte(), registry);
        final long storageDataTypeId = h5.createArrayType(stringDataTypeId, value.length, registry);
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId = h5.createAttribute(objectId, name, storageDataTypeId, -1, registry);
            }
        } else
        {
            attributeId = h5.createAttribute(objectId, name, storageDataTypeId, -1, registry);
        }
        h5.writeAttribute(attributeId, storageDataTypeId, arrData);
    }

    void setStringArrayAttribute(final long objectId, final String name,
            final MDArray<String> value, final int maxLength, final boolean lengthFitsValue,
            ICleanUpRegistry registry)
    {
        final StringArrayBuffer array = new StringArrayBuffer(maxLength, lengthFitsValue);
        array.addAll(value.getAsFlatArray());
        final byte[] arrData = array.toArray();
        final long stringDataTypeId = h5.createDataTypeString(array.getMaxLengthInByte(), registry);
        final long storageDataTypeId =
                h5.createArrayType(stringDataTypeId, value.dimensions(), registry);
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, storageDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId = h5.createAttribute(objectId, name, storageDataTypeId, -1, registry);
            }
        } else
        {
            attributeId = h5.createAttribute(objectId, name, storageDataTypeId, -1, registry);
        }
        h5.writeAttribute(attributeId, storageDataTypeId, arrData);
    }

    void setStringAttributeVariableLength(final long objectId, final String name,
            final String value, ICleanUpRegistry registry)
    {
        long attributeId;
        if (h5.existsAttribute(objectId, name))
        {
            attributeId = h5.openAttribute(objectId, name, registry);
            final long oldStorageDataTypeId = h5.getDataTypeForAttribute(attributeId, registry);
            if (h5.dataTypesAreEqual(oldStorageDataTypeId, variableLengthStringDataTypeId) == false)
            {
                h5.deleteAttribute(objectId, name);
                attributeId =
                        h5.createAttribute(objectId, name, variableLengthStringDataTypeId, -1,
                                registry);
            }
        } else
        {
            attributeId =
                    h5.createAttribute(objectId, name, variableLengthStringDataTypeId, -1, registry);
        }
        writeAttributeStringVL(attributeId, new String[]
            { value });
    }

    String moveLinkOutOfTheWay(String linkPath)
    {
        final String newLinkPath = createNonExistentReplacementLinkPath(linkPath);
        h5.moveLink(fileId, linkPath, newLinkPath);
        return newLinkPath;
    }

    private String createNonExistentReplacementLinkPath(final String dataTypePath)
    {
        final String dstLinkPath = dataTypePath + "__REPLACED_";
        int idx = 1;
        while (h5.exists(fileId, dstLinkPath + idx))
        {
            ++idx;
        }
        return dstLinkPath + idx;
    }

}
