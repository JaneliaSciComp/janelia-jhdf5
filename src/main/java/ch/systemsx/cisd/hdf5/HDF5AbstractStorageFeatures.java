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

import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * An object representing the storage features that are to be used for a data set.
 * <p>
 * The available storage layouts are {@link HDF5StorageLayout#COMPACT},
 * {@link HDF5StorageLayout#CONTIGUOUS} or {@link HDF5StorageLayout#CHUNKED} can be chosen. Only
 * {@link HDF5StorageLayout#CHUNKED} is extendable and can be compressed.
 * <p>
 * Two types of compressions are supported: <i>deflation</i> (the method used by <code>gzip</code>)
 * and <i>scaling</i>, which can be used if the accuracy of the values are smaller than what the
 * atomic data type can store. Note that <i>scaling</i> in general can be a lossy compression while
 * <i>deflation</i> is always lossless. <i>Scaling</i> compression is only available with HDF5 1.8
 * and newer. Trying to use <i>scaling</i> in strict HDF5 1.6 compatibility mode will throw an
 * {@link IllegalStateException}.
 * <p>
 * For <i>deflation</i> the deflation level can be chosen to get the right balance between speed of
 * compression and compression ratio. Often the {@link #DEFAULT_DEFLATION_LEVEL} will be the right
 * choice.
 * <p>
 * For <i>scaling</i>, the scaling factor can be chosen that determines the accuracy of the values
 * saved. What exactly the scaling factor means, differs between float and integer values.
 * 
 * @author Bernd Rinn
 */
abstract class HDF5AbstractStorageFeatures
{
    /**
     * A constant that specifies that no deflation should be used.
     */
    public final static byte NO_DEFLATION_LEVEL = 0;

    /**
     * A constant that specifies the default deflation level (gzip compression).
     */
    public final static byte DEFAULT_DEFLATION_LEVEL = 6;

    /**
     * A constant that specifies the maximal deflation level (gzip compression).
     */
    public final static byte MAX_DEFLATION_LEVEL = 9;

    /**
     * The policy on how to deal with write access to existing datasets. "Keeping the dataset" means
     * to overwrite the content of the dataset, while "replacing the dataset" refers to deleting the
     * existing dataset and create a new one.
     */
    public enum DataSetReplacementPolicy
    {
        /** Use the default behavior as specified when the writer was created. */
        USE_WRITER_DEFAULT,
        /** Enforce to keep the existing dataset, overwriting the writer's default. */
        ENFORCE_KEEP_EXISTING,
        /** Enforce to replace the existing dataset, overwriting the writer's default. */
        ENFORCE_REPLACE_WITH_NEW
    }

    /**
     * Do not perform any scaling on the data.
     */
    final static byte NO_SCALING_FACTOR = -1;

    static byte toByte(int i)
    {
        final byte b = (byte) i;
        if (b != i)
        {
            throw new HDF5JavaException("Value " + i + " cannot be casted to type byte");
        }
        return b;
    }

    private final byte deflateLevel;

    private final byte scalingFactor;

    private final DataSetReplacementPolicy datasetReplacementPolicy;

    private final HDF5StorageLayout proposedLayoutOrNull;

    private final boolean shuffleBeforeDeflate;

    public abstract static class HDF5AbstractStorageFeatureBuilder
    {
        private byte deflateLevel;

        private byte scalingFactor;

        private HDF5StorageLayout storageLayout;

        private DataSetReplacementPolicy datasetReplacementPolicy =
                DataSetReplacementPolicy.USE_WRITER_DEFAULT;

        private boolean shuffleBeforeDeflate;

        HDF5AbstractStorageFeatureBuilder()
        {
        }

        public HDF5AbstractStorageFeatureBuilder(HDF5AbstractStorageFeatures template)
        {
            deflateLevel(template.getDeflateLevel());
            scalingFactor(template.getScalingFactor());
            storageLayout(template.tryGetProposedLayout());
            datasetReplacementPolicy(template.getDatasetReplacementPolicy());
            shuffleBeforeDeflate(template.isShuffleBeforeDeflate());
        }

        byte getDeflateLevel()
        {
            return deflateLevel;
        }

        byte getScalingFactor()
        {
            return scalingFactor;
        }

        HDF5StorageLayout getStorageLayout()
        {
            return storageLayout;
        }

        DataSetReplacementPolicy getDatasetReplacementPolicy()
        {
            return datasetReplacementPolicy;
        }

        boolean isShuffleBeforeDeflate()
        {
            return shuffleBeforeDeflate;
        }

        public HDF5AbstractStorageFeatureBuilder compress(boolean compress)
        {
            this.deflateLevel = compress ? DEFAULT_DEFLATION_LEVEL : NO_DEFLATION_LEVEL;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder compress()
        {
            this.deflateLevel = DEFAULT_DEFLATION_LEVEL;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder deflateLevel(@SuppressWarnings("hiding")
        byte deflateLevel)
        {
            this.deflateLevel = deflateLevel;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder scalingFactor(@SuppressWarnings("hiding")
        byte scalingFactor)
        {
            this.scalingFactor = scalingFactor;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder noScaling()
        {
            this.scalingFactor = (byte) -1;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder shuffleBeforeDeflate(@SuppressWarnings("hiding")
        boolean shuffleBeforeDeflate)
        {
            this.shuffleBeforeDeflate = shuffleBeforeDeflate;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder shuffleBeforeDeflate()
        {
            this.shuffleBeforeDeflate = true;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder noShuffleBeforeDeflate()
        {
            this.shuffleBeforeDeflate = true;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder storageLayout(@SuppressWarnings("hiding")
        HDF5StorageLayout storageLayout)
        {
            this.storageLayout = storageLayout;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder compactStorageLayout()
        {
            this.storageLayout = HDF5StorageLayout.COMPACT;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder contiguousStorageLayout()
        {
            this.storageLayout = HDF5StorageLayout.CONTIGUOUS;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder chunkedStorageLayout()
        {
            this.storageLayout = HDF5StorageLayout.CHUNKED;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder defaultStorageLayout()
        {
            this.storageLayout = null;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder datasetReplacementPolicy(
                @SuppressWarnings("hiding")
                DataSetReplacementPolicy datasetReplacementPolicy)
        {
            this.datasetReplacementPolicy = datasetReplacementPolicy;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder datasetReplacementUseWriterDefault()
        {
            this.datasetReplacementPolicy = DataSetReplacementPolicy.USE_WRITER_DEFAULT;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder datasetReplacementEnforceKeepExisting()
        {
            this.datasetReplacementPolicy = DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING;
            return this;
        }

        public HDF5AbstractStorageFeatureBuilder datasetReplacementEnforceReplaceWithNew()
        {
            this.datasetReplacementPolicy = DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW;
            return this;
        }

        abstract public HDF5AbstractStorageFeatures features();
    }

    HDF5AbstractStorageFeatures(final HDF5StorageLayout proposedLayoutOrNull,
            final DataSetReplacementPolicy datasetReplacementPolicy, final byte deflateLevel,
            final byte scalingFactor)
    {
        this(proposedLayoutOrNull, datasetReplacementPolicy, false, deflateLevel, scalingFactor);
    }

    HDF5AbstractStorageFeatures(final HDF5StorageLayout proposedLayoutOrNull,
            final DataSetReplacementPolicy datasetReplacementPolicy,
            final boolean shuffleBeforeDeflate, final byte deflateLevel, final byte scalingFactor)
    {
        if (deflateLevel < 0)
        {
            throw new IllegalArgumentException("Invalid deflateLevel " + deflateLevel);
        }
        this.proposedLayoutOrNull = proposedLayoutOrNull;
        this.datasetReplacementPolicy = datasetReplacementPolicy;
        this.shuffleBeforeDeflate = shuffleBeforeDeflate;
        this.deflateLevel = deflateLevel;
        this.scalingFactor = scalingFactor;
    }

    /**
     * Returns true, if this compression setting can be applied on the given <var>dataClassId</var>.
     */
    abstract boolean isCompatibleWithDataClass(int dataClassId);

    /**
     * Returns the proposed storage layout, or <code>null</code>, if no particular storage layout
     * should be proposed.
     */
    public HDF5StorageLayout tryGetProposedLayout()
    {
        return proposedLayoutOrNull;
    }

    /**
     * Returns the policy of this storage feature object regarding replacing or keeping already
     * existing datasets.
     */
    public DataSetReplacementPolicy getDatasetReplacementPolicy()
    {
        return datasetReplacementPolicy;
    }

    boolean requiresChunking()
    {
        return isDeflating() || isScaling() || proposedLayoutOrNull == HDF5StorageLayout.CHUNKED;
    }

    boolean allowsCompact()
    {
        return proposedLayoutOrNull == null || proposedLayoutOrNull == HDF5StorageLayout.COMPACT;
    }

    /**
     * Returns <code>true</code>, if this storage feature object deflates data.
     */
    public boolean isDeflating()
    {
        return (deflateLevel != NO_DEFLATION_LEVEL);
    }

    /**
     * Returns <code>true</code>, if this storage feature object scales data.
     */
    public boolean isScaling()
    {
        return scalingFactor >= 0;
    }

    /**
     * Returns <code>true</code>, if this storage feature object performs shuffling before deflating
     * the data.
     */
    public boolean isShuffleBeforeDeflate()
    {
        return shuffleBeforeDeflate;
    }

    /**
     * Returns the deflate level of this storage feature object. 0 means no deflate.
     */
    public byte getDeflateLevel()
    {
        return deflateLevel;
    }

    /**
     * Returns the scaling factor of this storage feature object. -1 means no scaling, 0 means
     * auto-scaling.
     */
    public byte getScalingFactor()
    {
        return scalingFactor;
    }

    static DataSetReplacementPolicy getDataSetReplacementPolicy(boolean keepDataSetIfExists,
            boolean deleteDataSetIfExists)
    {
        return keepDataSetIfExists ? DataSetReplacementPolicy.ENFORCE_KEEP_EXISTING
                : (deleteDataSetIfExists ? DataSetReplacementPolicy.ENFORCE_REPLACE_WITH_NEW
                        : DataSetReplacementPolicy.USE_WRITER_DEFAULT);
    }
}
