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

import hdf.hdf5lib.HDF5Constants;

/**
 * The configuration of the writer is done by chaining calls to configuration methods before calling {@link #writer()}.
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Factory#configure(java.io.File)}.
 * 
 * @author Bernd Rinn
 */
public interface IHDF5WriterConfigurator extends IHDF5ReaderConfigurator
{
    /**
     * The mode of synchronizing changes (using a method like <code>fsync(2)</code>) to the HDF5 file with the underlying storage. As
     * <code>fsync(2)</code> is blocking, the synchonization is by default performed in a separate thread to minimize latency effects on the
     * application. In order to ensure that <code>fsync(2)</code> is called in the same thread, use one of the <code>*_BLOCK</code> modes.
     * <p>
     * Note that non-blocking modes can have unexpected interactions with mandatory locks on Windows. The symptom of that will be that the program
     * holds a lock to the HDF5 file for some (short) time even after the file has been closed. Thus, on Windows by default a blocking mode is chosen.
     */
    public enum SyncMode
    {
    /**
     * Do not synchronize at all.
     */
    NO_SYNC,
    /**
     * Synchronize whenever {@link IHDF5FileLevelReadWriteHandler#flush()} or {@link IHDF5FileLevelReadWriteHandler#close()} are called.
     */
    SYNC,
    /**
     * Synchronize whenever {@link IHDF5FileLevelReadWriteHandler#flush()} or {@link IHDF5FileLevelReadWriteHandler#close()} are called. Block until
     * synchronize is finished.
     */
    SYNC_BLOCK,
    /**
     * Synchronize whenever {@link IHDF5FileLevelReadWriteHandler#flush()} is called. <i>Default on Unix</i>
     */
    SYNC_ON_FLUSH,
    /**
     * Synchronize whenever {@link IHDF5FileLevelReadWriteHandler#flush()} is called. Block until synchronize is finished. <i>Default on Windows</i>.
     */
    SYNC_ON_FLUSH_BLOCK,
    }

    /**
     * Enum for specifying a file format version.
     * <p>
     * For details, see
     * https://www.hdfgroup.org/wp-content/uploads/2018/04/RFC-Setting-Bounds-for-Object-Creation-in-HDF5-1-10-0.pdf
     */
    public enum FileFormatVersion
    {
        /**
         * The library will create objects with the earliest possible format versions.
         */
        EARLIEST(HDF5Constants.H5F_LIBVER_EARLIEST, false),

        /**
         * The library will allow objects to be created with the latest format versions available to the current library release version.
         */
        LATEST(HDF5Constants.H5F_LIBVER_LATEST, true),

        /**
         * The library will allow objects to be created with the latest format versions available to HDF5 v1.8.
         */
        V1_8(HDF5Constants.H5F_LIBVER_V18, false),

        /**
         * The library will allow objects to be created with the latest format versions available to HDF5 v1.10.
         */
        V1_10(HDF5Constants.H5F_LIBVER_V110, true);

        private final int hdf5Constant;
        
        private final boolean supportsMDCCache;

        private FileFormatVersion(int hdf5Constant, boolean supportsMDCCache)
        {
            this.hdf5Constant = hdf5Constant;
            this.supportsMDCCache = supportsMDCCache;
        }

        public boolean supportsMDCCache()
        {
            return supportsMDCCache;
        }

        int getHdf5Constant()
        {
            return hdf5Constant;
        }
    }

    /**
     * Enum for specifying the file format version boundaries.
     * <p>
     * For details, see
     * https://www.hdfgroup.org/wp-content/uploads/2018/04/RFC-Setting-Bounds-for-Object-Creation-in-HDF5-1-10-0.pdf
     */
    public enum FileFormatVersionBounds
    {
        /**
         * <ul>
         * <li>The library will create objects with the earliest possible format versions.</li>
         * <li>The library will allow objects to be created with the latest format versions available to library release version 1.8.</li>
         * <li>API calls that create objects or features that is available to versions of the library greater than release version 1.8 will fail.<.li>
         * </ul>
         */
        EARLIEST_V1_8(FileFormatVersion.EARLIEST, FileFormatVersion.V1_8),

        /**
         * <ul>
         * <li>The library will create objects with the earliest possible format versions.</li>
         * <li>The library will allow objects to be created with the latest format versions available to library release version 1.10.</li>
         * </ul>
         */
        EARLIEST_V1_10(FileFormatVersion.EARLIEST, FileFormatVersion.V1_10),

        /**
         * <ul>
         * <li>The library will create objects with the earliest possible format versions.</li>
         * <li>The library will allow objects to be created with the latest format versions available to the current library release version.</li>
         * <li>With this setting, there is no upper limit on the format version to use.</li>
         * <li>This is the library default setting and provides the greatest format compatibility.</li>
         * </ul>
         */
        EARLIEST_LATEST(FileFormatVersion.EARLIEST, FileFormatVersion.LATEST),

        /**
         * <ul>
         * <li>The library will create objects with the latest format versions available to library release version 1.8.</li>
         * <li>The library will allow objects to be created with the latest format versions available to library release version 1.8.</li>
         * <li>Earlier versions of the library than 1.8 may not be able to access objects created with this setting.</li>
         * <li>API calls that create objects or features that is available to versions of the library greater than release version 1.8 will fail.<.li>
         * </ul>
         */
        V1_8_V1_8(FileFormatVersion.V1_8, FileFormatVersion.V1_8),

        /**
         * <ul>
         * <li>The library will create objects with the latest format versions available to library release version 1.8.</li>
         * <li>The library will allow objects to be created with the latest format versions available to library release version 1.10.</li>
         * <li>Earlier versions of the library than 1.8 may not be able to access objects created with this setting.</li>
         * </ul>
         */
        V1_8_V1_10(FileFormatVersion.V1_8, FileFormatVersion.V1_10),

        /**
         * <ul>
         * <li>The library will create objects with the latest format versions available to library release version 1.8.</li>
         * <li>The library will allow objects to be created with the latest format versions available to the current library release version.</li>
         * <li>Earlier versions of the library than 1.8 may not be able to access objects created with this setting.</li>
         * </ul>
         */
        V1_8_LATEST(FileFormatVersion.V1_8, FileFormatVersion.LATEST),

        /**
         * <ul>
         * <li>The library will create objects with the latest format versions available to library release version 1.10.</li>
         * <li>This setting allows users to take advantage of the latest features and performance enhancements in the library version 1.10.</li>
         * <li>Earlier versions of the library than 1.10 may not be able to access objects created with this setting.</li>
         * </ul>
         */
        V1_10_V1_10(FileFormatVersion.V1_10, FileFormatVersion.V1_10),

        /**
         * <ul>
         * <li>The library will create objects with the latest format versions available to the current library release version.</li>
         * <li>This setting allows users to take advantage of the latest features and performance enhancements in the current library.</li>
         * <li>Earlier versions of the library than the current one may not be able to access objects created with this setting.</li>
         * </ul>
         */
        V1_10_LATEST(FileFormatVersion.V1_10, FileFormatVersion.LATEST),

        /**
         * <ul>
         * <li>The library will create objects with the latest format versions available to the current library release version.</li>
         * <li>This setting allows users to take advantage of the latest features and performance enhancements in the current library.</li>
         * <li>Earlier versions of the library than the current one may not be able to access objects created with this setting.</li>
         * </ul>
         */
        LATEST_LATEST(FileFormatVersion.LATEST, FileFormatVersion.LATEST);

        private final FileFormatVersion low;

        private final FileFormatVersion high;

        private FileFormatVersionBounds(FileFormatVersion low, FileFormatVersion high)
        {
            this.low = low;
            this.high = high;
        }
        
        /**
         * @return The lower boundary of the file format version.
         */
        public FileFormatVersion getLowBound()
        {
            return low;
        }
        
        /**
         * @return The upper boundary of the file format version.
         */
        public FileFormatVersion getHighBound()
        {
            return high;
        }
        
        /**
         * @return The default file format version boundaries used by the HDF5 library.
         */
        public static FileFormatVersionBounds getDefault()
        {
            return EARLIEST_LATEST;
        }
    }

    /**
     * The file will be truncated to length 0 if it already exists, that is its content will be deleted.
     */
    public IHDF5WriterConfigurator overwrite();

    /**
     * Use data types which can not be extended later on. This may reduce the initial size of the HDF5 file.
     */
    public IHDF5WriterConfigurator dontUseExtendableDataTypes();

    /**
     * Use simple data spaces for attributes.
     */
    public IHDF5WriterConfigurator useSimpleDataSpaceForAttributes();

    /**
     * On writing a data set, keep the data set if it exists and only write the new data. This is equivalent to the <code>_KEEP</code> variants of
     * {@link HDF5GenericStorageFeatures} and makes this behavior the default.
     * <p>
     * If this setting is not given, an existing data set will be deleted before the data set is written.
     * <p>
     * <i>Note:</i> If this configuration option is chosen, data types and storage features may only apply if the written data set does not yet exist.
     * For example, it may lead to a string value being truncated on write if a string dataset with the same name and shorter length already exists.
     */
    public IHDF5WriterConfigurator keepDataSetsIfTheyExist();

    /**
     * Sets the file format compatibility for the writer.
     */
    public IHDF5WriterConfigurator fileFormat(FileFormatVersionBounds newFileFormat);

    /**
     * Sets the {@link SyncMode}.
     */
    public IHDF5WriterConfigurator syncMode(SyncMode newSyncMode);

    /**
     * Will try to perform numeric conversions where appropriate if supported by the platform.
     * <p>
     * <strong>Numeric conversions can be platform dependent and are not available on all platforms. Be advised not to rely on numeric conversions if
     * you can help it!</strong>
     */
    @Override
    public IHDF5WriterConfigurator performNumericConversions();

    /**
     * Enables generation of a metadata cache image.
     * <p>
     * Use {#link {@link #keepMDCImage()} to only generate an MDC image if the file already has one. 
     */
    public IHDF5WriterConfigurator generateMDCImage();

    /**
     * Disables generation of a metadata cache image.
     * <p>
     * Use {#link {@link #keepMDCImage()} to keep an MDC image if the file already has one but disable it if it does not. 
     */
    public IHDF5WriterConfigurator noGenerateMDCImage();

    /**
     * Keep an existing metadata cache image, but do not generate an MDC if the file has none already. This is the default.
     * <p>
     * Use {#link {@link #generateMDCImage()} to generate an MDC image regardless of whether the file already has one.
     */
    public IHDF5WriterConfigurator keepMDCImage();

    /**
     * Sets UTF8 character encoding for all paths and all strings in this file. (The default is ASCII.)
     */
    public IHDF5WriterConfigurator useUTF8CharacterEncoding();

    /**
     * Switches off automatic dereferencing of unresolved references. Use this when you need to access file names that start with \0. The down-side of
     * switching off automatic dereferencing is that you can't provide references as obtained by {@link IHDF5ReferenceReader#read(String, boolean)}
     * with <code>resolveName=false</code> in places where a dataset path is required. <br>
     * <i>Note: automatic dereferencing is switched on by default.</i>
     */
    @Override
    public IHDF5WriterConfigurator noAutoDereference();

    /**
     * Sets the suffix that is used to mark and recognize house keeping files and groups. An empty string ("") encodes for the default, which is two
     * leading and two trailing underscores ("__NAME__").
     */
    public IHDF5WriterConfigurator houseKeepingNameSuffix(String houseKeepingNameSuffix);

    /**
     * Returns an {@link IHDF5Writer} based on this configuration.
     */
    public IHDF5Writer writer();

}
