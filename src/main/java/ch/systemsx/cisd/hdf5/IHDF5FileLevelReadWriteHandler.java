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

import java.io.Flushable;

import ch.systemsx.cisd.hdf5.IHDF5WriterConfigurator.FileFormatVersionBounds;

/**
 * An interface for handling file-level information and status of the writer. 
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Writer#file()}.
 *
 * @author Bernd Rinn
 */
public interface IHDF5FileLevelReadWriteHandler extends IHDF5FileLevelReadOnlyHandler
{

    // /////////////////////
    // Configuration
    // /////////////////////

    /**
     * Returns <code>true</code>, if the {@link IHDF5WriterConfigurator} was <em>not</em> configured
     * with {@link IHDF5WriterConfigurator#dontUseExtendableDataTypes()}, that is if extendable data
     * types are used for new data sets.
     */
    public boolean isUseExtendableDataTypes();

    /**
     * Returns the {@link FileFormatVersionBounds} compatibility setting for this writer.
     */
    public FileFormatVersionBounds getFileFormatVersionBounds();

    // /////////////////////
    // Flushing and Syncing
    // /////////////////////

    /**
     * Flushes the cache to disk (without discarding it). Note that this may or may not trigger a
     * <code>fsync(2)</code>, depending on the {@link IHDF5WriterConfigurator.SyncMode} used.
     */
    public void flush();

    /**
     * Flushes the cache to disk (without discarding it) and synchronizes the file with the
     * underlying storage using a method like <code>fsync(2)</code>, regardless of what
     * {@link IHDF5WriterConfigurator.SyncMode} has been set for this file.
     * <p>
     * This method blocks until <code>fsync(2)</code> has returned.
     */
    public void flushSyncBlocking();

    /**
     * Adds a {@link Flushable} to the set of flushables. This set is flushed when {@link #flush()}
     * or {@link #flushSyncBlocking()} are called and before the writer is closed.
     * <p>
     * This function is supposed to be used for in-memory caching structures that need to make it
     * into the HDF5 file.
     * <p>
     * If the <var>flushable</var> implements
     * {@link ch.systemsx.cisd.base.exceptions.IErrorStrategy}, in case of an exception in
     * {@link Flushable#flush()}, the method
     * {@link ch.systemsx.cisd.base.exceptions.IErrorStrategy#dealWithError(Throwable)} will be
     * called to decide how do deal with the exception.
     * 
     * @param flushable The {@link Flushable} to add. Needs to fulfill the {@link Object#hashCode()}
     *            contract.
     * @return <code>true</code> if the set of flushables did not already contain the specified
     *         element.
     */
    public boolean addFlushable(Flushable flushable);

    /**
     * Removes a {@link Flushable} from the set of flushables.
     * 
     * @param flushable The {@link Flushable} to remove. Needs to fulfill the
     *            {@link Object#hashCode()} contract.
     * @return <code>true</code> if the set of flushables contained the specified element.
     */
    public boolean removeFlushable(Flushable flushable);

}
