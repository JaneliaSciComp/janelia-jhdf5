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

import java.io.File;
import java.io.IOException;

import hdf.hdf5lib.exceptions.HDF5Exception;

/**
 * Exception thrown when listing a file / directory in an archive fails.
 *
 * @author Bernd Rinn
 */
public class ListArchiveException extends ArchiverException
{

    private static final long serialVersionUID = 1L;
    
    private static final String OPERATION_NAME = "listing";
    
    public ListArchiveException(String objectPath, String detailedMsg)
    {
        super(objectPath, OPERATION_NAME, detailedMsg);
    }
    
    public ListArchiveException(String objectPath, HDF5Exception cause)
    {
        super(objectPath, OPERATION_NAME, cause);
    }
    
    public ListArchiveException(String objectPath, RuntimeException cause)
    {
        super(objectPath, OPERATION_NAME, cause);
    }
    
    public ListArchiveException(File file, IOException cause)
    {
        super(file, OPERATION_NAME, cause);
    }

}
