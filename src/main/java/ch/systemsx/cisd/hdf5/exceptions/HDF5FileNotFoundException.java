/*
 * Copyright 2011 ETH Zuerich, CISD
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

package ch.systemsx.cisd.hdf5.exceptions;

import java.io.File;

import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * <p>
 * The class HDF5JavaException returns errors from the Java wrapper of theHDF5 library.
 * <p>
 * This exception communicates that a file is not found or cannot be opened.
 *
 * @author Bernd Rinn
 */
public class HDF5FileNotFoundException extends HDF5JavaException
{
    private static final long serialVersionUID = 1L;

    public HDF5FileNotFoundException(File file, String msg)
    {
        super(msg + " (" + file.getAbsolutePath() + ")");
    }

}
