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
import static hdf.hdf5lib.HDF5Constants.H5P_DATASET_XFER;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5_INDEX_NAME;
import static hdf.hdf5lib.HDF5Constants.H5_ITER_INC;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.hdf5lib.structs.H5L_info_t;
import hdf.hdf5lib.structs.H5O_info_t;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.hdf5.H5T_conv_except_func_t;
import org.bytedeco.hdf5.H5AC_cache_image_config_t;
import org.bytedeco.hdf5.global.hdf5;

/**
 * Helper methods that used to be implemented by a bespoke native library ("jhdf5"/"libjhdf5")
 * built from this project's own JNI C sources. That native library was never actually part of
 * this repository (no C sources, no build scripts, no bundled binaries for any platform), so
 * every one of these methods was broken out of the box here regardless of platform.
 * <p>
 * All of them turn out to be thin wrappers around plain public HDF5 C API calls, which the
 * {@code org.bytedeco:hdf5} JavaCPP preset (see bytedeco/javacpp-presets#1808) already binds.
 * This class now implements them in pure Java on top of that preset instead of requiring its
 * own native library, inheriting every platform the preset supports.
 */
public class HDFHelper
{

    static
    {
        // HDFHelper calls into org.bytedeco.hdf5.global.hdf5 below; make sure its native
        // library is loaded regardless of what the caller has already triggered.
        Loader.load(hdf5.class);
    }

    // H5Pcreate_xfer_abort(_overflow) below use this statically-imported H5P_DATASET_XFER (the
    // official hdf.hdf5lib API's own already-cached constant) rather than
    // org.bytedeco.hdf5.global.hdf5.H5P_DATASET_XFER: that preset's own constant resolves to 11,
    // an unrelated HDF5-internal array index (H5P_CLS_DXFR's position in H5Pint.c's static
    // init_class[] table), not the real property-list-class hid_t.

    /**
     * compoundCpyVLStr/createVLStrFromCompound/freeCompoundVLStr below read and write a raw C
     * pointer's worth of bytes using this value, so it has to match the actual native ABI
     * JavaCPP loaded -- not just assume 64-bit -- since getting it wrong would silently corrupt
     * memory (a 4-byte native char* read or written as 8 bytes) rather than fail loudly.
     * Loader.sizeof(Pointer.class) is JavaCPP's own authoritative answer for sizeof(void*) on
     * whatever platform it just loaded, the same way javacpp-generated code itself would ask.
     */
    static final int pointerSize = Loader.sizeof(Pointer.class);

    // ////////////////////////////////////////////////////////////
    // //
    // Functions for link and object information //
    // //
    // ////////////////////////////////////////////////////////////

    /**
     * Version of {@link H5#H5Lexists(long, String, long)} that never throws an exception when
     * {@code name} does not exist.
     */
    public static boolean H5Lexists(long loc_id, String name, long lapl_id)
            throws HDF5LibraryException, NullPointerException
    {
        try
        {
            return H5.H5Lexists(loc_id, name, lapl_id);
        } catch (HDF5LibraryException e)
        {
            if (e.getMinorErrorNumber() == HDF5Constants.H5E_NOTFOUND)
            {
                return false;
            }
            throw e;
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

        return new H5O_info_t(-1, HDF5Constants.H5O_TOKEN_UNDEF, -1, -1, -1, -1, -1, -1, -1);
    }

    /**
     * H5Lget_link_info returns the type of the link. If <code>lname != null</code> and <var>name</var> is a symbolic link, <code>lname[0]</code> will
     * contain the target of the link. If <var>exception_when_non_existent</var> is <code>true</code>, the method will throw an exception when the
     * link does not exist, otherwise -1 will be returned.
     */
    public static int H5Lget_link_info(
            final long fileId,
            final String objectName,
            final String[] linkTargetOrNull,
            boolean exceptionIfNonExistent)
    {
        int result = -1;
        try
        {
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

    public static void H5Lget_link_names_all(
            final long locId,
            final String groupName,
            final String[] objectNames)
    {
        for (int i = 0; i < objectNames.length; ++i)
        {
            objectNames[i] = H5Lget_name_by_idx(locId, groupName, H5_INDEX_NAME, H5_ITER_INC, i, H5P_DEFAULT);
        }
        return;
    }

    public static void H5Lget_link_info_all(
            final long locId,
            final String groupName,
            final String[] objectNames,
            final int[] objectTypes,
            final String[] linkFilenamesOrNull,
            final String[] linkTargetsOrNull)
    {
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
     * A {@link BytePointer} wrapping an address that was not allocated by this process (e.g. one
     * read back out of a compound buffer). It is never responsible for freeing that memory via a
     * JavaCPP-attached deallocator -- callers free it explicitly via {@link Pointer#free} instead,
     * matching the malloc/free pairing the original C implementation used.
     */
    private static final class ForeignBytePointer extends BytePointer
    {
        ForeignBytePointer(long rawAddress)
        {
            address = rawAddress;
        }
    }

    /**
     * Returns the size of a pointer on this platform.
     */
    public static int getPointerSize()
    {
        return pointerSize;
    }

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
    public static int compoundCpyVLStr(String str, byte[] buf, int bufOfs)
    {
        if (str == null)
        {
            throw new NullPointerException("compoundCpyVLStr: str is null");
        }
        if (buf == null)
        {
            throw new NullPointerException("compoundCpyVLStr: buf is null");
        }
        final byte[] utf8 = str.getBytes(StandardCharsets.UTF_8);
        // calloc, not malloc: the trailing byte is left zeroed as the string's null terminator,
        // exactly like the original C calloc(1, numberOfBytes + 1) did.
        final Pointer raw = Pointer.calloc(1, utf8.length + 1);
        new BytePointer(raw).capacity(utf8.length + 1).put(utf8);
        ByteBuffer.wrap(buf).order(ByteOrder.nativeOrder()).putLong(bufOfs, raw.address());
        return 0;
    }

    /**
     * Creates a Java copy from a C char* pointer in the buf at bufOfs.
     */
    public static String createVLStrFromCompound(byte[] buf, int offset)
    {
        if (buf == null)
        {
            throw new NullPointerException("createVLStrFromCompound: buf is null");
        }
        final long address = ByteBuffer.wrap(buf).order(ByteOrder.nativeOrder()).getLong(offset);
        return new ForeignBytePointer(address).getString(StandardCharsets.UTF_8);
    }

    /**
     * Frees the variable-length strings in compound buf, where one compound has size recordSize and the variable-length members can be found at
     * byte-offsets vlIndices.
     */
    public static int freeCompoundVLStr(byte[] buf, int recordSize, int[] vlIndices)
    {
        if (buf == null)
        {
            throw new NullPointerException("freeCompoundVLStr: buf is null");
        }
        if (vlIndices == null)
        {
            throw new NullPointerException("freeCompoundVLStr: vlIndices is null");
        }
        final ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.nativeOrder());
        for (int recordOffset = 0; recordOffset < buf.length; recordOffset += recordSize)
        {
            for (int idx : vlIndices)
            {
                final long address = bb.getLong(recordOffset + idx);
                if (address != 0)
                {
                    Pointer.free(new ForeignBytePointer(address));
                }
            }
        }
        return 0;
    }

    // ////////////////////////////////////////////////////////////
    // //
    // Functions related to numeric value conversion features //
    // //
    // ////////////////////////////////////////////////////////////

    /**
     * Aborts conversions that trigger overflows (range-hi/range-low exceptions); any other
     * conversion exception is left unhandled (HDF5's default behavior applies). Allocated once:
     * the JNI callback it wraps is only ever invoked by HDF5 on the (rare) exception path during
     * a type conversion, never per element on the hot path, so a single shared instance is both
     * correct and cheap to reuse across every property list that registers it.
     */
    private static final H5T_conv_except_func_t ABORT_ON_OVERFLOW_CALLBACK = new H5T_conv_except_func_t()
    {
        @Override
        public int call(int exceptType, long srcId, long dstId, Pointer srcBuf, Pointer dstBuf, Pointer opData)
        {
            if (exceptType == hdf5.H5T_CONV_EXCEPT_RANGE_HI || exceptType == hdf5.H5T_CONV_EXCEPT_RANGE_LOW)
            {
                return hdf5.H5T_CONV_ABORT;
            }
            return hdf5.H5T_CONV_UNHANDLED;
        }
    };

    /**
     * Aborts every conversion exception unconditionally. See {@link #ABORT_ON_OVERFLOW_CALLBACK}
     * for why a single shared instance is appropriate here too.
     */
    private static final H5T_conv_except_func_t ABORT_ALWAYS_CALLBACK = new H5T_conv_except_func_t()
    {
        @Override
        public int call(int exceptType, long srcId, long dstId, Pointer srcBuf, Pointer dstBuf, Pointer opData)
        {
            return hdf5.H5T_CONV_ABORT;
        }
    };

    /**
     * Returns a dataset transfer property list (<code>H5P_DATASET_XFER</code>) that has a conversion exception handler set which abort conversions
     * that triggers overflows.
     */
    public static long H5Pcreate_xfer_abort_overflow()
    {
        final long plist = H5.H5Pcreate(H5P_DATASET_XFER);
        if (plist < 0)
        {
            throw new HDF5LibraryException("H5Pcreate(H5P_DATASET_XFER) failed");
        }
        if (hdf5.H5Pset_type_conv_cb(plist, ABORT_ON_OVERFLOW_CALLBACK, null) < 0)
        {
            throw new HDF5LibraryException("H5Pset_type_conv_cb failed");
        }
        return plist;
    }

    /**
     * Returns a dataset transfer property list (<code>H5P_DATASET_XFER</code>) that has a conversion exception handler set which aborts all
     * conversions.
     */
    public static long H5Pcreate_xfer_abort()
    {
        final long plist = H5.H5Pcreate(H5P_DATASET_XFER);
        if (plist < 0)
        {
            throw new HDF5LibraryException("H5Pcreate(H5P_DATASET_XFER) failed");
        }
        if (hdf5.H5Pset_type_conv_cb(plist, ABORT_ALWAYS_CALLBACK, null) < 0)
        {
            throw new HDF5LibraryException("H5Pset_type_conv_cb failed");
        }
        return plist;
    }

    // ////////////////////////////////////////////////////////////
    // //
    // Functions for controlling the metadata cache configuration //
    // //
    // ////////////////////////////////////////////////////////////

    /**
     * Sets whether a metadata cache image should be generated for an HDF5 file.
     *
     * @param fapl The file access property list of the file.
     * @param generate_image If a metadata cache image should be generated for the file.
     * @return 0 for successfull completion.
     */
    public static long H5Pset_mdc_image_config(long fapl, boolean generate_image) throws HDF5LibraryException
    {
        try (H5AC_cache_image_config_t config = new H5AC_cache_image_config_t())
        {
            config.version(hdf5.H5AC__CURR_CACHE_IMAGE_CONFIG_VERSION);
            config.generate_image(generate_image);
            config.save_resize_status(false);
            config.entry_ageout(hdf5.H5AC__CACHE_IMAGE__ENTRY_AGEOUT__NONE);

            final int status = hdf5.H5Pset_mdc_image_config(fapl, config);
            if (status < 0)
            {
                throw new HDF5LibraryException("H5Pset_mdc_image_config failed");
            }
            return status;
        }
    }

    /**
     * Determines whether the metadata cache image generation is enabled for an HDF5 file.
     *
     * @param fapl The file access property list of the file.
     * @return <code>true</code> if a metadata cache image will ge generated on file close for this file.
     */
    public static boolean H5Pget_mdc_image_enabled(long fapl)
    {
        try (H5AC_cache_image_config_t config = new H5AC_cache_image_config_t())
        {
            config.version(hdf5.H5AC__CURR_CACHE_IMAGE_CONFIG_VERSION);
            final int status = hdf5.H5Pget_mdc_image_config(fapl, config);
            if (status < 0)
            {
                throw new HDF5LibraryException("H5Pget_mdc_image_config failed");
            }
            return config.generate_image();
        }
    }

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
        final long[] imageAddr = new long[1];
        final long[] imageLen = new long[1];
        final int status = hdf5.H5Fget_mdc_image_info(file_id, imageAddr, imageLen);
        if (status < 0)
        {
            throw new HDF5LibraryException("H5Fget_mdc_image_info failed");
        }
        // HADDR_UNDEF is all-ones (UINT64_MAX), i.e. -1 as a signed 64-bit value.
        return imageAddr[0] != -1L && imageLen[0] > 0;
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
