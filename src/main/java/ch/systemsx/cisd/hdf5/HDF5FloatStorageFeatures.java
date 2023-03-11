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

import static hdf.hdf5lib.HDF5Constants.H5T_FLOAT;

/**
 * An object representing the storage features that are to be used for a float data set.
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
 * Two types of compressions are supported: <i>deflation</i> (the method used by <code>gzip</code>)
 * and <i>scaling</i>, which can be used if the accuracy of the values are smaller than what the
 * atomic data type can store. <b>Note that <i>scaling</i> in general is a lossy compression</b>
 * while <i>deflation</i> is always lossless. <i>Scaling</i> compression is only available with HDF5
 * 1.8 and newer. Trying to use <i>scaling</i> in strict HDF5 1.6 compatibility mode will throw an
 * {@link IllegalStateException}.
 * <p>
 * For <i>deflation</i> the deflation level can be chosen to get the right balance between speed of
 * compression and compression ratio. Often the {@link #DEFAULT_DEFLATION_LEVEL} will be the right
 * choice.
 * <p>
 * For <i>scaling</i>, the scaling factor can be chosen that determines the accuracy of the values
 * saved. For float values, the scaling factor determines the number of significant digits of the
 * numbers. It is guaranteed that <code>{@literal |f_real - f_saved| < 10^(-scalingFactor)}</code>.
 * The algorithm used for scale compression is:
 * <ol>
 * <li>Calculate the minimum value of all values</li>
 * <li>Subtract the minimum value from all values</li>
 * <li>Multiply all values obtained in step 2 with <code>{@literal 10^scalingFactor}</code></li>
 * <li>Round the values obtained in step 3 to the nearest integer value</li>
 * <li>Store the minimum found in step 1 and the values obtained in step 4</li>
 * </ol>
 * This algorithm is known as GRIB D-scaling.
 * 
 * @author Bernd Rinn
 */
public final class HDF5FloatStorageFeatures extends HDF5AbstractStorageFeatures
{

    /**
     * Represents 'no compression', use default storage layout.
     */
    public static final HDF5FloatStorageFeatures FLOAT_NO_COMPRESSION =
            new HDF5FloatStorageFeatures(null, NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'no compression', use default storage layout.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_NO_COMPRESSION_KEEP =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'no compression', use default storage layout.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_NO_COMPRESSION_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents a compact storage layout.
     */
    public static final HDF5FloatStorageFeatures FLOAT_COMPACT = new HDF5FloatStorageFeatures(
            HDF5StorageLayout.COMPACT, NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents a compact storage layout.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_COMPACT_KEEP = new HDF5FloatStorageFeatures(
            HDF5StorageLayout.COMPACT, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
            NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents a compact storage layout.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_COMPACT_DELETE =
            new HDF5FloatStorageFeatures(HDF5StorageLayout.COMPACT,
                    DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a contiguous storage layout.
     */
    public static final HDF5FloatStorageFeatures FLOAT_CONTIGUOUS = new HDF5FloatStorageFeatures(
            HDF5StorageLayout.CONTIGUOUS, NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents a contiguous storage layout.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_CONTIGUOUS_KEEP =
            new HDF5FloatStorageFeatures(HDF5StorageLayout.CONTIGUOUS,
                    DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a contiguous storage layout.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_CONTIGUOUS_DELETE =
            new HDF5FloatStorageFeatures(HDF5StorageLayout.CONTIGUOUS,
                    DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents a chunked storage layout.
     */
    public static final HDF5FloatStorageFeatures FLOAT_CHUNKED = new HDF5FloatStorageFeatures(
            HDF5StorageLayout.CHUNKED, NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents a chunked storage layout.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_CHUNKED_KEEP = new HDF5FloatStorageFeatures(
            HDF5StorageLayout.CHUNKED, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
            NO_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents a chunked storage layout.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_CHUNKED_DELETE =
            new HDF5FloatStorageFeatures(HDF5StorageLayout.CHUNKED,
                    DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW, NO_DEFLATION_LEVEL,
                    NO_SCALING_FACTOR);

    /**
     * Represents 'standard compression', that is deflation with the default deflation level.
     */
    public static final HDF5FloatStorageFeatures FLOAT_DEFLATE = new HDF5FloatStorageFeatures(null,
            DEFAULT_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'standard compression' with a pre-filter shuffle, that is deflation with the
     * default deflation level.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SHUFFLE_DEFLATE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.USE_WRITER_DEFAULT, true,
                    DEFAULT_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'standard compression', that is deflation with the default deflation level.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_DEFLATE_KEEP = new HDF5FloatStorageFeatures(
            null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING, DEFAULT_DEFLATION_LEVEL,
            NO_SCALING_FACTOR);

    /**
     * Represents 'standard compression', that is deflation with the default deflation level.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_DEFLATE_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    DEFAULT_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'maximal compression', that is deflation with the maximal deflation level.
     */
    public static final HDF5FloatStorageFeatures FLOAT_DEFLATE_MAX = new HDF5FloatStorageFeatures(
            null, MAX_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'maximal compression', that is deflation with the maximal deflation level.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_DEFLATE_MAX_KEEP =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    MAX_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents 'maximal compression', that is deflation with the maximal deflation level.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_DEFLATE_MAX_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    MAX_DEFLATION_LEVEL, NO_SCALING_FACTOR);

    /**
     * Represents scaling with scaling factor 1 for float values.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING1 = new HDF5FloatStorageFeatures(
            null, NO_DEFLATION_LEVEL, (byte) 1);

    /**
     * Represents scaling with scaling factor 1 for float values.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING1_KEEP =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    NO_DEFLATION_LEVEL, (byte) 1);

    /**
     * Represents scaling with scaling factor 1 for float values.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING1_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    NO_DEFLATION_LEVEL, (byte) 1);

    /**
     * Represents scaling with scaling factor 1 for float values combined with deflation using the
     * default deflation level.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING1_DEFLATE =
            new HDF5FloatStorageFeatures(null, DEFAULT_DEFLATION_LEVEL, (byte) 1);

    /**
     * Represents scaling with scaling factor 1 for float values combined with deflation using the
     * default deflation level.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING1_DEFLATE_KEEP =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    DEFAULT_DEFLATION_LEVEL, (byte) 1);

    /**
     * Represents scaling with scaling factor 1 for float values combined with deflation using the
     * default deflation level.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING1_DEFLATE_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    DEFAULT_DEFLATION_LEVEL, (byte) 1);

    /**
     * Represents scaling with scaling factor 2 for float values.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING2 = new HDF5FloatStorageFeatures(
            null, NO_DEFLATION_LEVEL, (byte) 2);

    /**
     * Represents scaling with scaling factor 2 for float values.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING2_KEEP =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    NO_DEFLATION_LEVEL, (byte) 2);

    /**
     * Represents scaling with scaling factor 2 for float values.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING2_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    NO_DEFLATION_LEVEL, (byte) 2);

    /**
     * Represents scaling with scaling factor 2 for float values combined with deflation using the
     * default deflation level.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING2_DEFLATE =
            new HDF5FloatStorageFeatures(null, DEFAULT_DEFLATION_LEVEL, (byte) 2);

    /**
     * Represents scaling with scaling factor 2 for float values combined with deflation using the
     * default deflation level.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING2_DEFLATE_KEEP =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    DEFAULT_DEFLATION_LEVEL, (byte) 2);

    /**
     * Represents scaling with scaling factor 2 for float values combined with deflation using the
     * default deflation level.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING2_DEFLATE_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    DEFAULT_DEFLATION_LEVEL, (byte) 2);

    /**
     * Represents scaling with scaling factor 3 for float values.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING3 = new HDF5FloatStorageFeatures(
            null, NO_DEFLATION_LEVEL, (byte) 3);

    /**
     * Represents scaling with scaling factor 3 for float values.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING3_KEEP =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    NO_DEFLATION_LEVEL, (byte) 3);

    /**
     * Represents scaling with scaling factor 3 for float values.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING3_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    true, NO_DEFLATION_LEVEL, (byte) 3);

    /**
     * Represents scaling with scaling factor 3 for float values combined with deflation using the
     * default deflation level.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING3_DEFLATE =
            new HDF5FloatStorageFeatures(null, DEFAULT_DEFLATION_LEVEL, (byte) 3);

    /**
     * Represents scaling with scaling factor 3 for float values combined with deflation using the
     * default deflation level.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING3_DEFLATE_KEEP =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                    DEFAULT_DEFLATION_LEVEL, (byte) 3);

    /**
     * Represents scaling with scaling factor 3 for float values combined with deflation using the
     * default deflation level.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static final HDF5FloatStorageFeatures FLOAT_SCALING3_DEFLATE_DELETE =
            new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW,
                    DEFAULT_DEFLATION_LEVEL, (byte) 3);

    /**
     * A builder for storage features.
     */
    public static final class HDF5FloatStorageFeatureBuilder extends
            HDF5AbstractStorageFeatureBuilder
    {
        public HDF5FloatStorageFeatureBuilder()
        {
        }

        public HDF5FloatStorageFeatureBuilder(HDF5AbstractStorageFeatures template)
        {
            super(template);
        }

        /**
         * Compresses the dataset with default deflation level, if <code>compress==true</code>, do
         * not compress if <code>compress==false</code>.
         * 
         * @return This builder.
         */
        @Override
        public HDF5FloatStorageFeatureBuilder compress(boolean compress)
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
        public HDF5FloatStorageFeatureBuilder compress()
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
        public HDF5FloatStorageFeatureBuilder deflateLevel(byte deflateLevel)
        {
            super.deflateLevel(deflateLevel);
            return this;
        }

        /**
         * Sets the scaling factor for an integer scaling pre-filter.
         * 
         * @return This builder.
         */
        @Override
        public HDF5FloatStorageFeatureBuilder scalingFactor(byte scalingFactor)
        {
            super.scalingFactor(scalingFactor);
            return this;
        }
        
        /**
         * Disables the scaling pre-filter.
         * 
         * @return This builder.
         */
        @Override
        public HDF5FloatStorageFeatureBuilder noScaling()
        {
            super.noScaling();
            return this;
        }

        /**
         * Sets a shuffling pre-filter for deflation if <code>shuffleBeforeDeflate==true</code> and
         * disables it if <code>shuffleBeforeDeflate==false</code>. Theshuffling pre-filter may
         * improve the compression level but may also increase the compression time.
         * <p>
         * Only takes effect if compression is switched on.
         * 
         * @return This builder.
         */
        @Override
        public HDF5FloatStorageFeatureBuilder shuffleBeforeDeflate(boolean shuffleBeforeDeflate)
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
        public HDF5FloatStorageFeatureBuilder storageLayout(HDF5StorageLayout proposedLayout)
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
        public HDF5FloatStorageFeatureBuilder datasetReplacementPolicy(
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
        public HDF5FloatStorageFeatureBuilder datasetReplacementUseWriterDefault()
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
        public HDF5FloatStorageFeatureBuilder datasetReplacementEnforceKeepExisting()
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
        public HDF5FloatStorageFeatureBuilder datasetReplacementEnforceReplaceWithNew()
        {
            super.datasetReplacementEnforceReplaceWithNew();
            return this;
        }

        /**
         * Returns the storage features corresponding to this builder's values.
         */
        @Override
        public HDF5FloatStorageFeatures features()
        {
            return new HDF5FloatStorageFeatures(this);
        }
    }

    /**
     * Returns a new storage feature builder.
     */
    public static HDF5FloatStorageFeatureBuilder build()
    {
        return new HDF5FloatStorageFeatureBuilder();
    }

    /**
     * Returns a new storage feature builder, initializing from <var>template</var>.
     */
    public static HDF5FloatStorageFeatureBuilder build(HDF5AbstractStorageFeatures template)
    {
        return new HDF5FloatStorageFeatureBuilder(template);
    }

    /**
     * Create a corresponding {@link HDF5FloatStorageFeatures} for the given
     * {@link HDF5GenericStorageFeatures}.
     */
    public static HDF5FloatStorageFeatures createFromGeneric(
            HDF5GenericStorageFeatures storageFeatures)
    {
        if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_CHUNKED)
        {
            return HDF5FloatStorageFeatures.FLOAT_CHUNKED;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_CHUNKED_DELETE)
        {
            return HDF5FloatStorageFeatures.FLOAT_CHUNKED_DELETE;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_CHUNKED_KEEP)
        {
            return HDF5FloatStorageFeatures.FLOAT_CHUNKED_KEEP;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_COMPACT)
        {
            return HDF5FloatStorageFeatures.FLOAT_COMPACT;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_COMPACT_DELETE)
        {
            return HDF5FloatStorageFeatures.FLOAT_COMPACT_DELETE;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_COMPACT_KEEP)
        {
            return HDF5FloatStorageFeatures.FLOAT_COMPACT_KEEP;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_CONTIGUOUS)
        {
            return HDF5FloatStorageFeatures.FLOAT_CONTIGUOUS;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_CONTIGUOUS_DELETE)
        {
            return HDF5FloatStorageFeatures.FLOAT_CONTIGUOUS_DELETE;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_CONTIGUOUS_KEEP)
        {
            return HDF5FloatStorageFeatures.FLOAT_CONTIGUOUS_KEEP;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION)
        {
            return HDF5FloatStorageFeatures.FLOAT_NO_COMPRESSION;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION_DELETE)
        {
            return HDF5FloatStorageFeatures.FLOAT_NO_COMPRESSION_DELETE;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION_KEEP)
        {
            return HDF5FloatStorageFeatures.FLOAT_NO_COMPRESSION_KEEP;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_DEFLATE)
        {
            return HDF5FloatStorageFeatures.FLOAT_DEFLATE;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_DEFLATE_DELETE)
        {
            return HDF5FloatStorageFeatures.FLOAT_DEFLATE_DELETE;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_DEFLATE_KEEP)
        {
            return HDF5FloatStorageFeatures.FLOAT_DEFLATE_KEEP;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_DEFLATE_MAX)
        {
            return HDF5FloatStorageFeatures.FLOAT_DEFLATE_MAX;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_DEFLATE_MAX_DELETE)
        {
            return HDF5FloatStorageFeatures.FLOAT_DEFLATE_MAX_DELETE;
        } else if (storageFeatures == HDF5GenericStorageFeatures.GENERIC_DEFLATE_MAX_KEEP)
        {
            return HDF5FloatStorageFeatures.FLOAT_DEFLATE_MAX_KEEP;
        } else
        {
            return new HDF5FloatStorageFeatures(storageFeatures.tryGetProposedLayout(),
                    storageFeatures.getDatasetReplacementPolicy(),
                    storageFeatures.getDeflateLevel(), NO_SCALING_FACTOR);
        }
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents deflation with the given
     * <var>deflationLevel</var>.
     */
    public static HDF5FloatStorageFeatures createDeflation(int deflationLevel)
    {
        return createDeflation(deflationLevel, DataSetReplacementPolicy.USE_WRITER_DEFAULT);
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents deflation with the given
     * <var>deflationLevel</var>.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static HDF5FloatStorageFeatures createDeflationKeep(int deflationLevel)
    {
        return createDeflation(deflationLevel, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING);
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents deflation with the given
     * <var>deflationLevel</var>.
     * <p>
     * Delete an existing data set before writing the new one. Always apply the chosen settings.
     * This allows to overwrite the {@link IHDF5WriterConfigurator#keepDataSetsIfTheyExist()}
     * setting.
     */
    public static HDF5FloatStorageFeatures createDeflationDelete(int deflationLevel)
    {
        return createDeflation(deflationLevel, DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW);
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents deflation with the given
     * <var>deflationLevel</var>.
     */
    private static HDF5FloatStorageFeatures createDeflation(int deflationLevel,
            DataSetReplacementPolicy dataSetReplacementPolicy)
    {
        return new HDF5FloatStorageFeatures(null, dataSetReplacementPolicy, toByte(deflationLevel),
                NO_SCALING_FACTOR);
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents float scaling with the
     * given <var>scalingFactor</var>.
     */
    public static HDF5FloatStorageFeatures createFloatScaling(int scalingFactor)
    {
        return new HDF5FloatStorageFeatures(null, NO_DEFLATION_LEVEL, toByte(scalingFactor));
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents float scaling with the
     * given <var>scalingFactor</var>.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static HDF5FloatStorageFeatures createFloatScalingKeep(int scalingFactor)
    {
        return new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                NO_DEFLATION_LEVEL, toByte(scalingFactor));
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents deflation with the default
     * deflation level and float scaling with the given <var>scalingFactor</var>.
     */
    public static HDF5FloatStorageFeatures createDeflateAndFloatScaling(int scalingFactor)
    {
        return new HDF5FloatStorageFeatures(null, DEFAULT_DEFLATION_LEVEL, toByte(scalingFactor));
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents deflation with the default
     * deflation level and float scaling with the given <var>scalingFactor</var>.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static HDF5FloatStorageFeatures createDeflateAndFloatScalingKeep(int scalingFactor)
    {
        return new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                DEFAULT_DEFLATION_LEVEL, toByte(scalingFactor));
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents deflation with the given
     * <var>deflateLevel</var> and float scaling with the given <var>scalingFactor</var>.
     */
    public static HDF5FloatStorageFeatures createDeflateAndFloatScaling(int deflateLevel,
            int scalingFactor)
    {
        return new HDF5FloatStorageFeatures(null, toByte(deflateLevel), toByte(scalingFactor));
    }

    /**
     * Creates a {@link HDF5FloatStorageFeatures} object that represents deflation with the given
     * <var>deflateLevel</var> and float scaling with the given <var>scalingFactor</var>.
     * <p>
     * Keep existing data set and apply only if a new data set has to be created.
     */
    public static HDF5FloatStorageFeatures createDeflateAndFloatScalingKeep(int deflateLevel,
            int scalingFactor)
    {
        return new HDF5FloatStorageFeatures(null, DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING,
                toByte(deflateLevel), toByte(scalingFactor));
    }

    HDF5FloatStorageFeatures(HDF5StorageLayout proposedLayoutOrNull, byte deflateLevel,
            byte scalingFactor)
    {
        this(proposedLayoutOrNull, DataSetReplacementPolicy.USE_WRITER_DEFAULT, deflateLevel,
                scalingFactor);
    }

    HDF5FloatStorageFeatures(HDF5StorageLayout proposedLayoutOrNull,
            DataSetReplacementPolicy dataSetReplacementPolicy, byte deflateLevel, byte scalingFactor)
    {
        super(proposedLayoutOrNull, dataSetReplacementPolicy, deflateLevel, scalingFactor);
    }

    HDF5FloatStorageFeatures(HDF5FloatStorageFeatureBuilder builder)
    {
        super(builder.getStorageLayout(), builder.getDatasetReplacementPolicy(), builder
                .isShuffleBeforeDeflate(), builder.getDeflateLevel(), builder.getScalingFactor());
    }

    HDF5FloatStorageFeatures(HDF5StorageLayout proposedLayoutOrNull,
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
        return (dataClassId == H5T_FLOAT);
    }

}
