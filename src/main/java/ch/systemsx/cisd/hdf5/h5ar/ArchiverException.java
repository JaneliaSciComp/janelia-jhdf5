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

import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;

/**
 * Base class of exceptions from the archiver.
 * 
 * @author Bernd Rinn
 */
public abstract class ArchiverException extends RuntimeException
{
    private final String fileOrObjectPath;

    private static final long serialVersionUID = 1L;

    protected ArchiverException(String objectPath, String operationName, String detailedMsg)
    {
        super("Error " + operationName + " object '" + objectPath + "': " + detailedMsg, null);
        this.fileOrObjectPath = objectPath;
    }

    protected ArchiverException(String objectPath, String operationName, HDF5Exception cause)
    {
        super("Error " + operationName + " object '" + objectPath + "' ["
                + cause.getClass().getSimpleName() + "]: " + cause.getMessage(), cause);
        this.fileOrObjectPath = objectPath;
    }

    protected ArchiverException(String objectPath, String operationName, RuntimeException cause)
    {
        super("Error " + operationName + " object '" + objectPath + "' ["
                + cause.getClass().getSimpleName() + "]: " + cause.getMessage(), cause);
        this.fileOrObjectPath = objectPath;
    }

    protected ArchiverException(File file, String operationName, IOExceptionUnchecked cause)
    {
        this(file, operationName, cause.getCause());
    }

    protected ArchiverException(File file, String operationName, IOException cause)
    {
        super("Error " + operationName + " file '" + file + "' [IO]: " + cause.getMessage(), cause);
        this.fileOrObjectPath = file.getAbsolutePath();
    }

    protected ArchiverException(String filePath, String operationName, IOException cause)
    {
        super("Error " + operationName + " on reading input stream for object  '" + filePath
                + "' [IO]: " + cause.getMessage(), cause);
        this.fileOrObjectPath = filePath;
    }

    public final String getFileOrObjectPath()
    {
        return fileOrObjectPath;
    }

}
