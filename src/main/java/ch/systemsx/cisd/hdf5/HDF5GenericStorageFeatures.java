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


/**
 * An object representing the storage features that are to be used for a data set.
 * <p>
 * The <code>..._KEEP</code> variants denote that the specified storage features should only be
 * applied if a new data set has to be created. If the data set already exists, it will be kept with
 * whatever storage features it has.
 * <em>Note that this may lead to an exception if the existing data set is non-extendable and the 
 * dimensions of the new data set differ from the dimensions of the existing data set.</em>
 * <p>
 * The <code>..._DELETE</code> variants denote that the specified storage features should always be
 * applied. If the data set already exists, it will be deleted before the new data set is written.
 * This is the default behavior. However, if the
 * {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()} setting is given, the
 * <code>..._DELETE</code> variant can be used to override this setting on a case-by-case basis.
 * <p>
 * The available storage layouts are {@link HDF5StorageLayout#COMPACT},
 * {@link HDF5StorageLayout#CONTIGUOUS} or {@link HDF5StorageLayout#CHUNKED} can be chosen. Only
 * {@link HDF5StorageLayout#CHUNKED} is extendable and can be compressed.
 * <p>
 * For generic (that is non-integer and non-float) data sets only one type of compression is
 * supported, which is <i>deflation</i>, the method used by <code>gzip</code>. The deflation level
 * can be chosen to get the right balance between speed of compression and compression ratio. Often
 * the {@link #DEFAULT_DEFLATION_LEVEL} will be the right choice.
 * 
 * @author Bernd Rinn
 */
public final class HDF5GenericStorageFeatures extends HDF5AbstractStorageFeatures
{
    /**
     * Represents 'no compression'.
     */
    public static final HDF5GenericStorageFeatures GENERIC_NO_COMPRESSION =
            new HDF5GenericStorageFeatures(null, NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'no compression'.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5GenericStorageFeatures GENERIC_NO_COMPRESSION_KEEP =
            new HDF5GenericStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'no compression'.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5GenericStorageFeatures GENERIC_NO_COMPRESSION_DELETE =
            new HDF5GenericStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents a compact storage layout.
     */
    public static final HDF5GenericStorageFeatures GENERIC_COMPACT =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.COMPACT, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a compact storage layout.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5GenericStorageFeatures GENERIC_COMPACT_KEEP =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.COMPACT,
                    DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a compact storage layout.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5GenericStorageFeatures GENERIC_COMPACT_DELETE =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.COMPACT,
                    DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a contiguous storage layout.
     */
    public static final HDF5GenericStorageFeatures GENERIC_CONTIGUOUS =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.CONTIGUOUS, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a contiguous storage layout.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5GenericStorageFeatures GENERIC_CONTIGUOUS_KEEP =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.CONTIGUOUS,
                    DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a contiguous storage layout.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5GenericStorageFeatures GENERIC_CONTIGUOUS_DELETE =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.CONTIGUOUS,
                    DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a chunked storage layout.
     */
    public static final HDF5GenericStorageFeatures GENERIC_CHUNKED =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.CHUNKED, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a chunked storage layout.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5GenericStorageFeatures GENERIC_CHUNKED_KEEP =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.CHUNKED,
                    DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a chunked storage layout.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5GenericStorageFeatures GENERIC_CHUNKED_DELETE =
            new HDF5GenericStorageFeatures(HDF5StorageLayout.CHUNKED,
                    DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents 'standard compression', that is deflation with the default deflation level.
     */
    public static final HDF5GenericStorageFeatures GENERIC_DEFLATE =
            new HDF5GenericStorageFeatures(null, DEFAULT_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'standard compression' with a pre-filter shuffle, that is deflation with the
     * default deflation level.
     */
    public static final HDF5GenericStorageFeatures GENERIC_SHUFFLE_DEFLATE =
            new HDF5GenericStorageFeatures(null, DataSetReplacementPolicy.USE_WRITER_DEFAULT, true,
                    DEFAULT_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'standard compression', that is deflation with the default deflation level.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5GenericStorageFeatures GENERIC_DEFLATE_KEEP =
            new HDF5GenericStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    DEFAULT_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'standard compression', that is deflation with the default deflation level.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5GenericStorageFeatures GENERIC_DEFLATE_DELETE =
            new HDF5GenericStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    DEFAULT_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'maximal compression', that is deflation with the maximal deflation level.
     */
    public static final HDF5GenericStorageFeatures GENERIC_DEFLATE_MAX =
            new HDF5GenericStorageFeatures(null, MAX_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'maximal compression' with a pre-filter shuffle, that is deflation with the
     * default deflation level.
     */
    public static final HDF5GenericStorageFeatures GENERIC_SHUFFLE_DEFLATE_MAX =
            new HDF5GenericStorageFeatures(null, DataSetReplacementPolicy.USE_WRITER_DEFAULT, true,
                    MAX_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'maximal compression', that is deflation with the maximal deflation level.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5GenericStorageFeatures GENERIC_DEFLATE_MAX_KEEP =
            new HDF5GenericStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    MAX_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'maximal compression', that is deflation with the maximal deflation level.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5GenericStorageFeatures GENERIC_DEFLATE_MAX_DELETE =
            new HDF5GenericStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    MAX_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Creates a {@link HDF5GenericStorageFeatures} object that represents deflation with the given
     * <var>deflationLevel</var>.
     */
    public static HDF5GenericStorageFeatures createDeflation(int deflationLevel)
    {
        return createDeflation(deflationLevel, false);
    }

    /**
     * Creates a {@link HDF5GenericStorageFeatures} object that represents deflation with the given
     * <var>deflationLevel</var>.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static HDF5GenericStorageFeatures createDeflationKeep(int deflationLevel)
    {
        return createDeflation(deflationLevel, true);
    }

    /**
     * Creates a {@link HDF5GenericStorageFeatures} object that represents deflation with the given
     * <var>deflationLevel</var>.
     */
    private static HDF5GenericStorageFeatures createDeflation(int deflationLevel,
            boolean keepDataSetIfExists)
    {
        if (deflationLevel == NO_DEFLATION_LEVEL)
        {
            return GENERIC_NO_COMPRESSION;
        } else if (deflationLevel == DEFAULT_DEFLATION_LEVEL)
        {
            return GENERIC_DEFLATE;
        } else if (deflationLevel == MAX_DEFLATION_LEVEL)
        {
            return GENERIC_DEFLATE_MAX;
        } else
        {
            return new HDF5GenericStorageFeatures(null, getDataSetReplacementPolicy(
                    keepDataSetIfExists, false), toByte(deflationLevel), NO_SCALING_FACTOR);
        }
    }

    /**
     * Legacy method for specifying the compression as a boolean value.
     */
    static HDF5GenericStorageFeatures getCompression(boolean deflate)
    {
        return deflate ? GENERIC_DEFLATE : GENERIC_NO_COMPRESSION;
    }

    /**
     * A builder for storage features.
     */
    public static final class HDF5GenericStorageFeatureBuilder extends
            HDF5AbstractStorageFeatureBuilder
    {
        public HDF5GenericStorageFeatureBuilder()
        {
        }

        public HDF5GenericStorageFeatureBuilder(HDF5AbstractStorageFeatures template)
        {
            super(template);
            noScaling();
        }

        /**
         * Compresses the dataset with default deflation level, if <code>compress==true</code>, do
         * not compress if <code>compress==false</code>.
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder compress(boolean compress)
        {
            super.compress(compress);
            return this;
        }

        /**
         * Compress the dataset with default deflation level.
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder compress()
        {
            super.compress();
            return this;
        }

        /**
         * Compresses this dataset with the given <var>deflateLevel</var>.
         * {@link #NO_DEFLATION_LEVEL} means: do not compress. A good default value is
         * {@link #DEFAULT_DEFLATION_LEVEL}, the maximum value supported is
         * {@link #MAX_DEFLATION_LEVEL}.
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder deflateLevel(byte deflateLevel)
        {
            super.deflateLevel(deflateLevel);
            return this;
        }

        /**
         * Sets a shuffling pre-filter for deflation if <code>shuffleBeforeDeflate==true</code> and
         * disables it if <code>shuffleBeforeDeflate==false</code>. The shuffling pre-filter may
         * improve the compression level but may also increase the compression time.
         * <p>
         * Only takes effect if compression is switched on.
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder shuffleBeforeDeflate(boolean shuffleBeforeDeflate)
        {
            super.shuffleBeforeDeflate(shuffleBeforeDeflate);
            return this;
        }

        /**
         * Sets a shuffling pre-filter for deflation. This may improve the compression level but may
         * also increase the compression time.
         * <p>
         * Only takes effect if compression is switched on.
         * 
         * @see #compress()
         * @see #deflateLevel(byte)
         * @return This builder.
         */
        @Override
        public HDF5AbstractStorageFeatureBuilder shuffleBeforeDeflate()
        {
            super.shuffleBeforeDeflate();
            return this;
        }

        /**
         * Set the layout for the dataset.
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder storageLayout(HDF5StorageLayout proposedLayout)
        {
            super.storageLayout(proposedLayout);
            return this;
        }

        /**
         * Set a compact layout for the dataset.
         * 
         * @return This builder.
         */
        @Override
        public HDF5AbstractStorageFeatureBuilder compactStorageLayout()
        {
            super.compactStorageLayout();
            return this;
        }

        /**
         * Set a contiguous layout for the dataset.
         * 
         * @return This builder.
         */
        @Override
        public HDF5AbstractStorageFeatureBuilder contiguousStorageLayout()
        {
            super.contiguousStorageLayout();
            return this;
        }

        /**
         * Set a chunked layout for the dataset.
         * 
         * @return This builder.
         */
        @Override
        public HDF5AbstractStorageFeatureBuilder chunkedStorageLayout()
        {
            super.chunkedStorageLayout();
            return this;
        }

        /**
         * Let a heuristic choose the right layout for the dataset.
         * 
         * @return This builder.
         */
        @Override
        public HDF5AbstractStorageFeatureBuilder defaultStorageLayout()
        {
            this.defaultStorageLayout();
            return this;
        }

        /**
         * Set the dataset replacement policy for existing datasets.
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder datasetReplacementPolicy(
                DataSetReplacementPolicy datasetReplacementPolicy)
        {
            super.datasetReplacementPolicy(datasetReplacementPolicy);
            return this;
        }

        /**
         * Set the dataset replacement policy for existing datasets to
         * {@link ch.systemsx.cisd.hdf5.HDF5AbstractStorageFeatures.DataSetReplacementPolicy#USE_WRITER_DEFAULT}
         * .
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder datasetReplacementUseWriterDefault()
        {
            super.datasetReplacementUseWriterDefault();
            return this;
        }

        /**
         * Set the dataset replacement policy for existing datasets to
         * {@link ch.systemsx.cisd.hdf5.HDF5AbstractStorageFeatures.DataSetReplacementPolicy#ENFORCE_KEEP_EXISTING}
         * .
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder datasetReplacementEnforceKeepExisting()
        {
            super.datasetReplacementEnforceKeepExisting();
            return this;
        }

        /**
         * Set the dataset replacement policy for existing datasets to
         * {@link ch.systemsx.cisd.hdf5.HDF5AbstractStorageFeatures.DataSetReplacementPolicy#ENFORCE_REPLACE_WITH_NEW}
         * .
         * 
         * @return This builder.
         */
        @Override
        public HDF5GenericStorageFeatureBuilder datasetReplacementEnforceReplaceWithNew()
        {
            super.datasetReplacementEnforceReplaceWithNew();
            return this;
        }

        /**
         * Returns the storage features corresponding to this builder's values.
         */
        @Override
        public HDF5GenericStorageFeatures features()
        {
            return new HDF5GenericStorageFeatures(this);
        }
    }

    /**
     * Returns a new storage feature builder.
     */
    public static HDF5GenericStorageFeatureBuilder build()
    {
        return new HDF5GenericStorageFeatureBuilder();
    }

    /**
     * Returns a new storage feature builder, initializing from <var>template</var>.
     */
    public static HDF5GenericStorageFeatureBuilder build(HDF5AbstractStorageFeatures template)
    {
        return new HDF5GenericStorageFeatureBuilder(template);
    }

    HDF5GenericStorageFeatures(HDF5GenericStorageFeatureBuilder builder)
    {
        super(builder.getStorageLayout(), builder.getDatasetReplacementPolicy(), builder
                .isShuffleBeforeDeflate(), builder.getDeflateLevel(), builder.getScalingFactor());
    }

    HDF5GenericStorageFeatures(HDF5StorageLayout proposedLayoutOrNull, byte deflateLevel,
            byte scalingFactor)
    {
        this(proposedLayoutOrNull, DataSetReplacementPolicy.USE_WRITER_DEFAULT, deflateLevel,
                scalingFactor);
    }

    HDF5GenericStorageFeatures(HDF5StorageLayout proposedLayoutOrNull,
            DataSetReplacementPolicy dataSetReplacementPolicy, byte deflateLevel, byte scalingFactor)
    {
        super(proposedLayoutOrNull, dataSetReplacementPolicy, deflateLevel, scalingFactor);
    }

    HDF5GenericStorageFeatures(HDF5StorageLayout proposedLayoutOrNull,
            DataSetReplacementPolicy dataSetReplacementPolicy, boolean shuffleBeforeDeflate,
            byte deflateLevel, byte scalingFactor)
    {
        super(proposedLayoutOrNull, dataSetReplacementPolicy, shuffleBeforeDeflate, deflateLevel,
                scalingFactor);
    }

    /**
     * Returns true, if this compression setting can be applied on the given <var>dataClassId</var>.
     */
    @Override
    boolean isCompatibleWithDataClass(int dataClassId)
    {
        return true;
    }

}
