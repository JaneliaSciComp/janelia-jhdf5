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

package ch.systemsx.cisd.hdf5.hdf5lib;

import static hdf.hdf5lib.H5.H5Gclose;
import static hdf.hdf5lib.H5.H5Gopen;
import static hdf.hdf5lib.H5.H5Lget_info;
import static hdf.hdf5lib.H5.H5Lget_info_by_idx;
import static hdf.hdf5lib.H5.H5Lget_name_by_idx;
import static hdf.hdf5lib.H5.H5Lget_value;
import static hdf.hdf5lib.H5.H5Lget_value_by_idx;
import static hdf.hdf5lib.H5.H5Oget_info_by_idx;
import static hdf.hdf5lib.HDF5Constants.H5L_TYPE_EXTERNAL;
import static hdf.hdf5lib.HDF5Constants.H5L_TYPE_HARD;
import static hdf.hdf5lib.HDF5Constants.H5L_TYPE_SOFT;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_NTYPES;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5_INDEX_NAME;
import static hdf.hdf5lib.HDF5Constants.H5_ITER_INC;

import com.sun.xml.internal.bind.v2.runtime.Name;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.hdf5lib.structs.H5L_info_t;
import hdf.hdf5lib.structs.H5O_info_t;

public class HDFHelper
{

    static final boolean USE_NATIVE_METHODS = true;

    static final int pointerSize;

    static
    {
        pointerSize = getPointerSize();
    }

    // ////////////////////////////////////////////////////////////
    // //
    // Functions for link and object information //
    // //
    // ////////////////////////////////////////////////////////////

    private static native boolean _H5Lexists(long loc_id, String name, long lapl_id)
            throws HDF5LibraryException, NullPointerException;

    /**
     * Version of {@link H5#H5Lexists(long, String, long)} that never throws an exception when {@link Name} does not exist. 
     */
    public static boolean H5Lexists(long loc_id, String name, long lapl_id)
            throws HDF5LibraryException, NullPointerException
    {
        synchronized (H5.class)
        {
            return _H5Lexists(loc_id, name, lapl_id);
        }
    }
    
    public static H5O_info_t H5Oget_info_by_name(
            long loc_id,
            String object_name,
            boolean exceptionIfNonExistent)
    {
        try
        {
            return hdf.hdf5lib.H5.H5Oget_info_by_name(loc_id, object_name, H5P_DEFAULT);
        } catch (HDF5LibraryException e)
        {
            /*
             * Note: H5E_CANTINSERT is thrown by the dense group lookup. That is probably a wrong error code, but we have to deal with it here anyway.
             */
            if (e.getMinorErrorNumber() == HDF5Constants.H5E_NOTFOUND
                    || e.getMinorErrorNumber() == HDF5Constants.H5E_CANTINSERT)
            {
                if (exceptionIfNonExistent)
                {
                    throw e;
                }
            } else
            {
                throw e;
            }
        }

        return new H5O_info_t(-1, -1, -1, -1, -1, -1, -1, -1, -1, null, null, null);
    }

    /**
     * H5Lget_link_info returns the type of the link. If <code>lname != null</code> and <var>name</var> is a symbolic link, <code>lname[0]</code> will
     * contain the target of the link. If <var>exception_when_non_existent</var> is <code>true</code>, the method will throw an exception when the
     * link does not exist, otherwise -1 will be returned.
     */
    private static native int _H5Lget_link_info(long locId, String name, 
                                    String[] lname) throws HDF5LibraryException, NullPointerException;

    public static int H5Lget_link_info(
            final long fileId,
            final String objectName,
            final String[] linkTargetOrNull,
            boolean exceptionIfNonExistent)
    {
        int result = -1;
        try
        {
            if (USE_NATIVE_METHODS)
            {
                synchronized (H5.class)
                {
                    return _H5Lget_link_info(fileId, objectName, linkTargetOrNull);
                }
            }

            final H5L_info_t info = H5Lget_info(fileId, objectName, H5P_DEFAULT);
            if (info.type == H5L_TYPE_HARD)
            {
                result = H5Oget_info_by_name(fileId, objectName, exceptionIfNonExistent).type;
            } else
            {
                result = H5O_TYPE_NTYPES + info.type;
                if (linkTargetOrNull != null && linkTargetOrNull.length > 1)
                {
                    final String[] linkTarget = getLinkTarget(fileId, objectName, info.type);
                    linkTargetOrNull[0] = linkTarget[0];
                    linkTargetOrNull[1] = linkTarget[1];
                }
            }
        } catch (HDF5LibraryException e)
        {
            /*
             * Note: H5E_CANTINSERT is thrown by the dense group lookup. That is probably a wrong error code, but we have to deal with it here anyway.
             */
            if (e.getMinorErrorNumber() == HDF5Constants.H5E_NOTFOUND
                    || e.getMinorErrorNumber() == HDF5Constants.H5E_CANTINSERT)
            {
                if (exceptionIfNonExistent)
                {
                    System.err.println("Ups, throwing exception " + e + " anyway");
                    throw e;
                }
            } else
            {
                throw e;
            }
        }
        return result;
    }

    private static String[] getLinkTarget(final long locId, final String objectName, int type)
    {
        final String[] linkTarget = new String[2];
        H5Lget_value(locId, objectName, linkTarget, H5P_DEFAULT);
        if (type == H5L_TYPE_SOFT || type == H5L_TYPE_EXTERNAL)
        {
            return linkTarget;
        } else
        {
            throw new HDF5Exception("No Link: " + objectName);
        }
    }

    private static void getLinkTargetByIdx(final long locId, final String objectName, final int idx, final int type, 
            final String[] linkTarget)
    {
        H5Lget_value_by_idx(locId, objectName, H5_INDEX_NAME, H5_ITER_INC, idx, linkTarget, H5P_DEFAULT);

        if (type != H5L_TYPE_SOFT && type != H5L_TYPE_EXTERNAL)
        {
            throw new HDF5Exception("No Link: " + objectName);
        }
    }

    private static native int _H5Lget_link_names_all(long loc_id, String name, String[] oname, int n)
            throws HDF5LibraryException, NullPointerException;

    public static void H5Lget_link_names_all(
            final long locId,
            final String groupName,
            final String[] objectNames)
    {
        if (USE_NATIVE_METHODS)
        {
            synchronized (H5.class)
            {
                _H5Lget_link_names_all(locId, groupName, objectNames, objectNames.length);
            }
            return;
        }

        for (int i = 0; i < objectNames.length; ++i)
        {
            objectNames[i] = H5Lget_name_by_idx(locId, groupName, H5_INDEX_NAME, H5_ITER_INC, i, H5P_DEFAULT);
        }
        return;
    }

    private static native int _H5Lget_link_info_all(long loc_id, String name, String[] oname,
            int[] type, String[] lname, String[] lfname, int n) throws HDF5LibraryException, NullPointerException;

    public static void H5Lget_link_info_all(
            final long locId,
            final String groupName,
            final String[] objectNames,
            final int[] objectTypes,
            final String[] linkFilenamesOrNull,
            final String[] linkTargetsOrNull)
    {
        if (USE_NATIVE_METHODS)
        {
            synchronized (H5.class)
            {
                _H5Lget_link_info_all(locId, groupName, objectNames, objectTypes, linkTargetsOrNull, linkFilenamesOrNull, 
                        objectNames.length);
            }
            return;
        }

        long groupId = -1;
        try
        {
            groupId = H5Gopen(locId, groupName, H5P_DEFAULT);
            if (objectNames.length == objectTypes.length)
            {
                for (int i = 0; i < objectNames.length; ++i)
                {
                    objectNames[i] = H5Lget_name_by_idx(locId, groupName, H5_INDEX_NAME, H5_ITER_INC, i, H5P_DEFAULT);
                    final H5L_info_t info = H5Lget_info_by_idx(locId, groupName, H5_INDEX_NAME, H5_ITER_INC, i, H5P_DEFAULT);
                    if (info.type == H5L_TYPE_HARD)
                    {
                        objectTypes[i] = H5Oget_info_by_idx(locId, groupName, H5_INDEX_NAME, H5_ITER_INC, i, H5P_DEFAULT).type;
                    } else
                    {
                        objectTypes[i] = H5O_TYPE_NTYPES + info.type;
                        if (linkTargetsOrNull != null && linkTargetsOrNull.length == objectNames.length
                                && linkFilenamesOrNull != null && linkFilenamesOrNull.length == objectNames.length)
                        {
                            final String[] linkTarget = new String[2];
                            getLinkTargetByIdx(locId, groupName, i, info.type, linkTarget);
                            linkTargetsOrNull[i] = linkTarget[0];
                            linkFilenamesOrNull[i] = linkTarget[1];
                        }
                    }
                }
            }
        } finally
        {
            if (groupId != -1)
            {
                H5Gclose(groupId);
            }
        }
        return;
    }

    // ////////////////////////////////////////////////////////////
    // //
    // Functions related to variable-length string copying //
    // //
    // ////////////////////////////////////////////////////////////

    /**
     * Returns the size of a pointer on this platform.
     */
    public static native int getPointerSize();

    /**
     * Returns the size of a machine word on this platform.
     */
    public static int getMachineWordSize()
    {
        return pointerSize;
    }

    /**
     * Creates a C copy of str (using calloc) and put the reference of it into buf at bufOfs.
     */
    public static native int compoundCpyVLStr(String str, byte[] buf, int bufOfs);

    /**
     * Creates a Java copy from a C char* pointer in the buf at bufOfs.
     */
    public static native String createVLStrFromCompound(byte[] buf, int bufOfs);

    /**
     * Frees the variable-length strings in compound buf, where one compound has size recordSize and the variable-length members can be found at
     * byte-offsets vlIndices.
     */
    public static native int freeCompoundVLStr(byte[] buf, int recordSize, int[] vlIndices);

    // ////////////////////////////////////////////////////////////
    // //
    // Functions related to numeric value conversion features //
    // //
    // ////////////////////////////////////////////////////////////

    private static native long _H5Pcreate_xfer_abort_overflow();

    /**
     * Returns a dataset transfer property list (<code>H5P_DATASET_XFER</code>) that has a conversion exception handler set which abort conversions
     * that triggers overflows.
     */
    public static long H5Pcreate_xfer_abort_overflow()
    {
        synchronized (H5.class)
        {
            return _H5Pcreate_xfer_abort_overflow();
        }
    }

    private static native long _H5Pcreate_xfer_abort();

    /**
     * Returns a dataset transfer property list (<code>H5P_DATASET_XFER</code>) that has a conversion exception handler set which aborts all
     * conversions.
     */
    public static long H5Pcreate_xfer_abort()
    {
        synchronized (H5.class)
        {
            return _H5Pcreate_xfer_abort();
        }
    }

    // ////////////////////////////////////////////////////////////
    // //
    // Functions for controlling the metadata cache configuration //
    // //
    // ////////////////////////////////////////////////////////////

    private static native long _H5Pset_mdc_image_config(long fapl, boolean generate_image) throws HDF5LibraryException;
    
    /**
     * Sets whether a metadata cache image should be generated for an HDF5 file.
     * 
     * @param fapl The file access property list of the file.
     * @param generate_image If a metadata cache image should be generated for the file. 
     * @return 0 for successfull completion.
     */
    public static long H5Pset_mdc_image_config(long fapl, boolean generate_image) throws HDF5LibraryException
    {
        synchronized (H5.class)
        {
            return _H5Pset_mdc_image_config(fapl, generate_image);
        }        
    }
    
    private static native boolean _H5Pget_mdc_image_enabled(long fapl);
    
    /**
     * Determines whether the metadata cache image generation is enabled for an HDF5 file.
     * 
     * @param fapl The file access property list of the file.
     * @return <code>true</code> if a metadata cache image will ge generated on file close for this file.
     */
    public static boolean H5Pget_mdc_image_enabled(long fapl)
    {
        synchronized (H5.class)
        {
            return _H5Pget_mdc_image_enabled(fapl);
        }        
    }
    
    private static native boolean _H5Fhas_mdc_image(long file_id);
    
    /**
     * Checks whether the file has an metadata cache image. 
     * <p>
     * On files open for read/write access this function needs to be called 
     * immediately after opening the file and before the first access to any metadata.
     * 
     * @param file_id The id of the file
     * @return <code>true</code> if the file has a metadata cache image.
     */
    public static boolean H5Fhas_mdc_image(long file_id)
    {
        synchronized (H5.class)
        {
            return _H5Fhas_mdc_image(file_id);
        }
    }
    
    /**
     * Checks whether the file has an metadata cache image. 
     * <p>
     * 
     * @param file_path The path of the file
     * @return <code>true</code> if the file has a metadata cache image.
     */
    public static boolean H5Fhas_mdc_image(String file_path)
    {
        long fileId = -1;
        try {
            fileId =
                hdf.hdf5lib.H5.H5Fopen(file_path,
                        hdf.hdf5lib.HDF5Constants.H5F_ACC_RDONLY,
                        hdf.hdf5lib.HDF5Constants.H5P_DEFAULT);
            return H5Fhas_mdc_image(fileId);
        } finally 
        {
            if (fileId != -1)
            {
                hdf.hdf5lib.H5.H5Fclose(fileId);
            }
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // //
    // Convenience functions for converting native data types. //
    // //
    // ////////////////////////////////////////////////////////////

    public static double[] byteToDouble(byte[] data, int start, int len)
    {
        return HDFNativeData.byteToDouble(start, len, data);
    }

    public static float[] byteToFloat(byte[] data, int start, int len)
    {
        return HDFNativeData.byteToFloat(start, len, data);
    }

    public static int[] byteToInt(byte[] data, int start, int len)
    {
        return HDFNativeData.byteToInt(start, len, data);
    }

    public static long[] byteToLong(byte[] data, int start, int len)
    {
        return HDFNativeData.byteToLong(start, len, data);
    }

    public static short[] byteToShort(byte[] data, int start, int len)
    {
        return HDFNativeData.byteToShort(start, len, data);
    }

    public static byte[] doubleToByte(double[] data)
    {
        return HDFNativeData.doubleToByte(0, data.length, data);
    }

    public static byte[] floatToByte(float[] data)
    {
        return HDFNativeData.floatToByte(0, data.length, data);
    }

    public static byte[] intToByte(int[] data)
    {
        return HDFNativeData.intToByte(0, data.length, data);
    }

    public static byte[] longToByte(long[] data)
    {
        return HDFNativeData.longToByte(0, data.length, data);
    }

    public static byte[] shortToByte(short[] data)
    {
        return HDFNativeData.shortToByte(0, data.length, data);
    }

}
