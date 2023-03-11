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
 * Exception thrown when archiving a file / directory fails.
 *
 * @author Bernd Rinn
 */
public class ArchivingException extends ArchiverException
{
    private static final long serialVersionUID = 1L;
    
    private static final String OPERATION_NAME = "archiving";
    
    public ArchivingException(String msg)
    {
        super("GENERAL", OPERATION_NAME, msg);
    }
    
    public ArchivingException(String objectPath, HDF5Exception cause)
    {
        super(objectPath, OPERATION_NAME, cause);
    }
    
    public ArchivingException(File file, IOException cause)
    {
        super(file, OPERATION_NAME, cause);
    }

    public ArchivingException(String filePath, IOException cause)
    {
        super(filePath, OPERATION_NAME, cause);
    }

    public ArchivingException(String objectPath, String detailedMsg)
    {
        super(objectPath, OPERATION_NAME, detailedMsg);
    }

}
