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

import static hdf.hdf5lib.HDF5Constants.H5T_CSET_ASCII;
import static hdf.hdf5lib.HDF5Constants.H5T_CSET_UTF8;

/**
 * An enum for character encodings of path names and strings in JHDF5. 
 *
 * @author Bernd Rinn
 */
public enum CharacterEncoding
{
    ASCII("ASCII", H5T_CSET_ASCII, 1), 
    UTF8("UTF8", H5T_CSET_UTF8, 4);

    private final String charSetName;
    
    private final int cValue;
    
    private final int maxBytesPerChar;
    
    private CharacterEncoding(String charSetName, int cValue, int maxBytesPerChar)
    {
        this.charSetName = charSetName;
        this.cValue = cValue;
        this.maxBytesPerChar = maxBytesPerChar;
    }
    
    int getCValue()
    {
        return cValue;
    }
    
    String getCharSetName()
    {
        return charSetName;
    }

    /**
     * Returns the maximum number of bytes per character.
     */
    int getMaxBytesPerChar()
    {
        return maxBytesPerChar;
    }

    static CharacterEncoding fromCValue(int cValue) throws IllegalArgumentException
    {
        if (cValue == H5T_CSET_ASCII)
        {
            return ASCII;
        } else if (cValue == H5T_CSET_UTF8)
        {
            return UTF8;
        } else
        {
            throw new IllegalArgumentException("Illegal character encoding id " + cValue);
        }
    }
}