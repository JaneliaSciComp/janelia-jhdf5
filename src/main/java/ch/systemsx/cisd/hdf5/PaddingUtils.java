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

import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;

/**
 * A class with methods for padding of memory structures.
 * <p>
 * <i>This is an internal API that should not be expected to be stable between releases!</i>
 * 
 * @author Bernd Rinn
 */
class PaddingUtils
{
    private static final int machineWordSize = HDFHelper.getMachineWordSize(); 
    
    private PaddingUtils()
    {
        // Cannot be instantiated
    }

    /**
     * Compute the padded <code>offset</code> to have aligned access to variables of
     * <code>elementSize</code>, or the size of the machine word, whatever is smaller.
     */
    static int padOffset(int offset, int elementSize)
    {
        if (elementSize > 0)
        {
            final int actualElementSize = Math.min(elementSize, machineWordSize);
            int mod = offset % actualElementSize;
            return (mod > 0) ? offset + actualElementSize - mod : offset;
        } else
        {
            return offset;
        }
    }

    /**
     * Compute the maximal element size (in bytes). If the maximal element size is larger than the
     * size of a machine word on this platform, return the size of a machine word instead.
     */
    static int findMaxElementSize(HDF5MemberByteifyer[] byteifyers)
    {
        int maxElementSize = 0;
        for (HDF5MemberByteifyer b : byteifyers)
        {
            maxElementSize = Math.max(maxElementSize, b.getElementSize());
            if (maxElementSize >= machineWordSize)
            {
                return machineWordSize;
            }
        }
        return maxElementSize;
    }

}
