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
 * been closed; close any objects opened with them first.
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
     * aborts on overflow if {@link IHDF5ReaderConfigurator#performNumericConversions()} was
     * requested and on any numeric conversion otherwise.
     * 
     * @throws ch.systemsx.cisd.hdf5.exceptions.HDF5JavaException if the reader has been closed.
     */
    public long getDataTransferPropertyListId();
}
