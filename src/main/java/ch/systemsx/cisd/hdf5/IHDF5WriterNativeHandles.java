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
 * Exposes the native HDF5 creation property lists behind a writer, for callers that create objects
 * with calls on {@link hdf.hdf5lib.H5} directly. The handles of the reader side, such as the file
 * id, are in {@link IHDF5NativeHandles}.
 * <p>
 * The lists are owned by the writer. Do not close them, and do not use them after the writer has
 * been closed. HDF5 reuses ids, so a stale id may refer to an unrelated object by then. The getters
 * throw once the writer is closed, so call them again rather than caching the result across a
 * close.
 * <p>
 * The lists are the ones that the writer itself uses when it creates objects, in every thread. If
 * you change one, you change how the writer creates objects from then on, so treat them as
 * read-only: to customize one (for example to set a chunk layout or a filter), make a copy with
 * {@link hdf.hdf5lib.H5#H5Pcopy(long)} and close the copy yourself.
 * <p>
 * Objects that you create and write with raw {@link hdf.hdf5lib.H5} calls bypass the writer's own
 * bookkeeping. In particular, the writer's {@link IHDF5WriterConfigurator.SyncMode} only applies to
 * what it writes itself: it does not know about your raw writes, so it does not schedule a flush or
 * sync for them. Call {@link IHDF5FileLevelReadWriteHandler#flush()} (or
 * {@link IHDF5FileLevelReadWriteHandler#flushSyncBlocking()}) on the writer's
 * {@link IHDF5Writer#file()} after raw writes if they have to reach the file at a given point. Close
 * every object that you created or opened with the file id before closing the writer.
 */
public interface IHDF5WriterNativeHandles
{
    /**
     * Returns the id of the data set creation property list that JHDF5 uses for contiguous data
     * sets: default layout, fill time set to allocation time.
     * 
     * @throws ch.systemsx.cisd.hdf5.exceptions.HDF5JavaException if the writer has been closed.
     */
    public long getDataSetCreationPropertyListId();

    /**
     * Returns the id of the data set creation property list that JHDF5 uses for compact data sets:
     * compact layout, fill time set to allocation time. HDF5 stores a compact data set in the
     * object header, which limits it to 64 KiB of data, so creating a larger data set with this
     * list fails.
     *
     * @throws ch.systemsx.cisd.hdf5.exceptions.HDF5JavaException if the writer has been closed.
     */
    public long getCompactDataSetCreationPropertyListId();

    /**
     * Returns the id of the link creation property list that JHDF5 uses when creating groups, data
     * sets and links: intermediate groups are created as needed, and names are UTF-8 encoded if the
     * file was opened with UTF-8 character encoding.
     * 
     * @throws ch.systemsx.cisd.hdf5.exceptions.HDF5JavaException if the writer has been closed.
     */
    public long getLinkCreationPropertyListId();
}
