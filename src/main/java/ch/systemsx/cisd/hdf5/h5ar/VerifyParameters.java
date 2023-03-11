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


/**
 * A class that represents parameters for
 * {@link HDF5Archiver#verifyAgainstFilesystem(String, java.io.File, IArchiveEntryVisitor, VerifyParameters)
 * )} .
 * 
 * @author Bernd Rinn
 */
public final class VerifyParameters
{
    private final boolean recursive;

    private final boolean numeric;

    private final boolean verifyAttributes;

    public static final VerifyParameters DEFAULT = new VerifyParameters(true, false, false);

    /**
     * A class for constructing a new verify parameters object.
     */
    public static final class VerifyParametersBuilder
    {
        private boolean recursive = true;

        private boolean numeric = false;

        private boolean verifyAttributes = false;

        private VerifyParametersBuilder()
        {
        }

        /**
         * Perform a non-recursive verification, i.e. do not traverse sub-directories.
         */
        public VerifyParametersBuilder nonRecursive()
        {
            this.recursive = false;
            return this;
        }

        /**
         * If <var>recursive</var> is <code>true</code>, perform a recursive verification, if it is
         * <code>false</code>, perform a non-recursive listing, i.e. do not traverse
         * sub-directories.
         */
        public VerifyParametersBuilder recursive(@SuppressWarnings("hiding")
        boolean recursive)
        {
            this.recursive = recursive;
            return this;
        }

        /**
         * Reports user ids and permissions as numerical values.
         * <p>
         * This is a pure display parameter that is only relevant if {@link #verifyAttributes()} has
         * been set.
         */
        public VerifyParametersBuilder numeric()
        {
            this.numeric = true;
            return this;
        }

        /**
         * If <var>numeric</var> is <code>true</code>, reports user ids and permissions as numerical
         * values, if it is <code>false</code>, it reports user ids and permissions resolved to
         * strings. This is a pure display parameter.
         * <p>
         * This is a pure display parameter that is only relevant if {@link #verifyAttributes()} has
         * been set.
         */
        public VerifyParametersBuilder numeric(@SuppressWarnings("hiding")
        boolean numeric)
        {
            this.numeric = numeric;
            return this;
        }

        /**
         * Verifies also last modification time, file ownership and access permissions.
         */
        public VerifyParametersBuilder verifyAttributes()
        {
            this.verifyAttributes = true;
            return this;
        }

        /**
         * If <var>verifyAttributes</var> is <code>true</code>, verifies also last modification
         * time, file ownership and access permissions, if it is <code>false</code>, check only the
         * types and content of entries.
         */
        public VerifyParametersBuilder verifyAttributes(@SuppressWarnings("hiding")
        boolean verifyAttributes)
        {
            this.verifyAttributes = verifyAttributes;
            return this;
        }

        /**
         * Returns the {@link VerifyParameters} object constructed.
         */
        public VerifyParameters get()
        {
            return new VerifyParameters(recursive, numeric, verifyAttributes);
        }
    }

    /**
     * Starts building new verify parameters.
     * 
     * @return A new {@link VerifyParametersBuilder}.
     */
    public static VerifyParametersBuilder build()
    {
        return new VerifyParametersBuilder();
    }

    private VerifyParameters(boolean recursive, boolean numeric, boolean verifyAttributes)
    {
        this.recursive = recursive;
        this.numeric = numeric;
        this.verifyAttributes = verifyAttributes;
    }

    /**
     * Returns if recursive verification is enabled, i.e. if the verify process will traverse into
     * sub-directories.
     * 
     * @see VerifyParametersBuilder#recursive(boolean)
     */
    public boolean isRecursive()
    {
        return recursive;
    }

    /**
     * Returns if user id and permissions failures should be reported numerically.
     * 
     * @see VerifyParametersBuilder#numeric(boolean)
     */
    public boolean isNumeric()
    {
        return numeric;
    }

    /**
     * Returns if file attributes (last modification time, file ownerships and access permissions)
     * are checked, too.
     * 
     * @see VerifyParametersBuilder#verifyAttributes(boolean)
     */
    public boolean isVerifyAttributes()
    {
        return verifyAttributes;
    }

}
