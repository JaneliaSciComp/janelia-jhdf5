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
 * Exposes the native HDF5 handles behind a reader, for callers that make calls on
 * {@link hdf.hdf5lib.H5} directly (for example to keep their own cache of open data sets).
 * <p>
 * All handles are owned by the reader. Do not close them, and do not use them after the reader has
 * been closed. HDF5 reuses ids, so a stale id may refer to an unrelated object by then, and a raw
 * call with it can act on that object instead of failing. The getters throw once the reader is
 * closed, so call them again rather than caching the result across a close. Close every object that
 * you opened with a handle (data sets, data spaces, ...) before closing the reader: an object left
 * open keeps the file open, which on Windows also prevents deleting or renaming it.
 * <p>
 * The property list is the one that the reader itself uses for all its reads, in every thread. If
 * you change it, you change how the reader behaves from then on, so treat it as read-only: to
 * customize it, make a copy with {@link hdf.hdf5lib.H5#H5Pcopy(long)} and close the copy yourself.
 * The creation property lists are on the writer, in {@link IHDF5WriterNativeHandles}.
 */
public interface IHDF5NativeHandles
{
    /**
     * Returns the HDF5 file id (<code>hid_t</code>) of the open file.
     * 
     * @throws ch.systemsx.cisd.hdf5.exceptions.HDF5JavaException if the reader has been closed.
     */
    public long getFileId();

    /**
     * Returns the id of the data set transfer property list that this reader uses for reads. It
     * has a conversion exception handler that aborts the read. HDF5 only calls the handler when a
     * conversion raises an exception, so a read whose memory type matches the type in the file is
     * not affected. If {@link IHDF5ReaderConfigurator#performNumericConversions()} was requested,
     * the read aborts only when a value overflows the memory type; otherwise it aborts on every
     * conversion exception, not only on overflow. A raw read that converts to a type that cannot
     * hold every value, such as an <code>int32</code> data set into <code>H5T_NATIVE_SHORT</code>,
     * can therefore throw. To avoid that, read with the memory type of the data set, or copy the
     * list and set your own handler.
     *
     * @throws ch.systemsx.cisd.hdf5.exceptions.HDF5JavaException if the reader has been closed.
     */
    public long getDataTransferPropertyListId();
}
