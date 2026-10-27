/*
 * Copyright 2026 Howard Hughes Medical Institute
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
 *
 * Modified in 2026 by the Howard Hughes Medical Institute for janelia-jhdf5.
 */

package ch.systemsx.cisd.hdf5;

import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5NativeLibrary.intConstant;
import hdf.hdf5lib.HDF5Constants;

/**
 * The storage layout of a data set in the HDF5 file. Not applicable for attributes.
 * 
 * @author Bernd Rinn
 */
public enum HDF5StorageLayout
{
    COMPACT(intConstant(() -> HDF5Constants.H5D_COMPACT)), CONTIGUOUS(intConstant(() -> HDF5Constants.H5D_CONTIGUOUS)), CHUNKED(
            intConstant(() -> HDF5Constants.H5D_CHUNKED)), NOT_APPLICABLE(-1);

    private int id;

    private HDF5StorageLayout(int id)
    {
        this.id = id;
    }

    static HDF5StorageLayout fromId(int id) throws IllegalArgumentException
    {
        for (HDF5StorageLayout layout : values())
        {
            if (layout.id == id)
            {
                return layout;
            }
        }
        throw new IllegalArgumentException("Illegal layout id " + id);
    }
}