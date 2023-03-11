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

import java.io.File;

/**
 * An interface for handling file-level information and status of the reader. 
 * <p>
 * Obtain an object implementing this interface by calling {@link IHDF5Reader#file()}.
 *
 * @author Bernd Rinn
 */
public interface IHDF5FileLevelReadOnlyHandler
{

    // /////////////////////
    // Configuration
    // /////////////////////

    /**
     * Returns <code>true</code>, if numeric conversions should be performed automatically, e.g.
     * between <code>float</code> and <code>int</code>.
     */
    public boolean isPerformNumericConversions();

    /**
     * Returns <code>true</code>, if the generation of a metadata cache image is enabled for 
     * this file and <code>false</code> otherwise.
     */
    public boolean isMDCImageGenerationEnabled();
    
    /**
     * Returns <code>true</code>, if this file has an MDC image.
     */
    public boolean hasMDCImage();
    
    /**
     * Returns the suffix used to mark and recognize internal (house keeping) files and groups. An
     * empty string ("") encodes for the default, which is two leading and two trailing underscores
     * ("__NAME__")
     */
    public String getHouseKeepingNameSuffix();

    /**
     * Returns the HDF5 file that this class is reading.
     */
    public File getFile();

    // /////////////////////
    // Status
    // /////////////////////

    /**
     * Closes this object and the file referenced by this object. This object must not be used after
     * being closed. Calling this method for a second time is a no-op.
     */
    public void close();

    /**
     * Returns <code>true</code> if this reader has been already closed.
     */
    public boolean isClosed();

}
