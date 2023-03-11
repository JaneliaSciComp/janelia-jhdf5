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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import hdf.hdf5lib.exceptions.HDF5JavaException;

import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * A class that byteifies Java value objects. The fields have to be specified by name. This class
 * can handle all primitive types and Strings.
 * 
 * @author Bernd Rinn
 */
class HDF5ValueObjectByteifyer<T>
{

    private final HDF5MemberByteifyer[] byteifyers;

    private final int recordSizeInMemory;

    private final int recordSizeOnDisk;

    private final int[] vlMemberIndices;

    private Class<?> cachedRecordClass;

    private Constructor<?> cachedDefaultConstructor;

    @SuppressWarnings("unchecked")
    private static <T> T newMap(int size)
    {
        return (T) new HDF5CompoundDataMap(size);
    }

    @SuppressWarnings("unchecked")
    private static <T> T newList(int size)
    {
        return (T) new HDF5CompoundDataList(Collections.nCopies(size, null));
    }

    @SuppressWarnings("unchecked")
    private static <T> T newArray(int size)
    {
        return (T) new Object[size];
    }

    /** A role that provides direct access to the HDF5 file to this byteifyer. */
    interface IFileAccessProvider
    {
        public long getBooleanDataTypeId();

        public long getStringDataTypeId(int maxLength);

        public long getVariableLengthStringDataTypeId();

        public long getArrayTypeId(long baseTypeId, int length);

        public long getArrayTypeId(long baseTypeId, int[] dimensions);

        public HDF5EnumerationType getEnumType(String[] options);

        public CharacterEncoding getCharacterEncoding(long dataTypeId);
        
        public byte[] createObjectReference(String referencedObjectPath);
    }

    HDF5ValueObjectByteifyer(Class<T> clazz, IFileAccessProvider fileInfoProvider,
            CompoundTypeInformation compoundTypeInfoOrNull, HDF5CompoundMemberMapping... members)
    {
        byteifyers =
                HDF5CompoundByteifyerFactory.createMemberByteifyers(clazz, fileInfoProvider,
                        compoundTypeInfoOrNull, members);
        int numberOfVLMembers = 0;
        if (compoundTypeInfoOrNull != null)
        {
            recordSizeOnDisk = compoundTypeInfoOrNull.recordSizeOnDisk;
            recordSizeInMemory = compoundTypeInfoOrNull.recordSizeInMemory;
            numberOfVLMembers = compoundTypeInfoOrNull.getNumberOfVLMembers();
        } else if (byteifyers.length > 0)
        {
            recordSizeOnDisk = byteifyers[byteifyers.length - 1].getTotalSizeOnDisk();
            recordSizeInMemory =
                    PaddingUtils.padOffset(
                            byteifyers[byteifyers.length - 1].getTotalSizeInMemory(),
                            PaddingUtils.findMaxElementSize(byteifyers));
            for (HDF5MemberByteifyer byteifyer : byteifyers)
            {
                if (byteifyer.isVariableLengthType())
                {
                    ++numberOfVLMembers;
                }
            }
        } else
        {
            recordSizeOnDisk = 0;
            recordSizeInMemory = 0;
        }
        vlMemberIndices = new int[numberOfVLMembers];
        int idx = 0;
        for (HDF5MemberByteifyer byteifyer : byteifyers)
        {
            if (byteifyer.isVariableLengthType())
            {
                vlMemberIndices[idx++] = byteifyer.getOffsetInMemory();
            }
        }
    }

    public long insertMemberTypes(long dataTypeId)
    {
        for (HDF5MemberByteifyer byteifyer : byteifyers)
        {
            byteifyer.insertType(dataTypeId);
        }
        return dataTypeId;
    }

    public long insertNativeMemberTypes(long dataTypeId, HDF5 h5, ICleanUpRegistry registry)
    {
        for (HDF5MemberByteifyer byteifyer : byteifyers)
        {
            byteifyer.insertNativeType(dataTypeId, h5, registry);
        }
        return dataTypeId;
    }

    /**
     * @throw {@link HDF5JavaException} if one of the elements in <var>arr</var> exceeding its
     *        pre-defined size.
     */
    public byte[] byteify(long compoundDataTypeId, T[] arr) throws HDF5JavaException
    {
        final byte[] barray = new byte[arr.length * recordSizeInMemory];
        int offset = 0;
        int counter = 0;
        for (Object obj : arr)
        {
            for (HDF5MemberByteifyer byteifyer : byteifyers)
            {
                try
                {
                    final byte[] b = byteifyer.byteify(compoundDataTypeId, obj);
                    if (b.length > byteifyer.getSize() && byteifyer.mayBeCut() == false)
                    {
                        throw new HDF5JavaException("Compound " + byteifyer.describe()
                                + " of array element " + counter + " must not exceed "
                                + byteifyer.getSize() + " bytes, but is of size " + b.length
                                + " bytes.");
                    }
                    System.arraycopy(b, 0, barray, offset + byteifyer.getOffsetInMemory(),
                            Math.min(b.length, byteifyer.getSize()));
                } catch (IllegalAccessException ex)
                {
                    throw new HDF5JavaException("Error accessing " + byteifyer.describe());
                }
            }
            offset += recordSizeInMemory;
            ++counter;
        }
        return barray;
    }

    /**
     * @throw {@link HDF5JavaException} if <var>obj</var> exceeding its pre-defined size.
     */
    public byte[] byteify(long compoundDataTypeId, T obj) throws HDF5JavaException
    {
        final byte[] barray = new byte[recordSizeInMemory];
        for (HDF5MemberByteifyer byteifyer : byteifyers)
        {
            try
            {
                final byte[] b = byteifyer.byteify(compoundDataTypeId, obj);
                if (b.length > byteifyer.getSize() && byteifyer.mayBeCut() == false)
                {
                    throw new HDF5JavaException("Compound " + byteifyer.describe()
                            + " must not exceed " + byteifyer.getSize() + " bytes, but is of size "
                            + b.length + " bytes.");
                }
                System.arraycopy(b, 0, barray, byteifyer.getOffsetInMemory(),
                        Math.min(b.length, byteifyer.getSize()));
            } catch (IllegalAccessException ex)
            {
                throw new HDF5JavaException("Error accessing " + byteifyer.describe());
            }
        }
        return barray;
    }

    public T[] arrayify(long compoundDataTypeId, byte[] byteArr, Class<T> recordClass)
    {
        final int length = byteArr.length / recordSizeInMemory;
        if (length * recordSizeInMemory != byteArr.length)
        {
            throw new HDF5JavaException("Illegal byte array for compound type (length "
                    + byteArr.length + " is not a multiple of record size " + recordSizeInMemory
                    + ")");
        }
        final T[] result = HDF5Utils.createArray(recordClass, length);
        int offset = 0;
        for (int i = 0; i < length; ++i)
        {
            result[i] = primArrayifyScalar(compoundDataTypeId, byteArr, recordClass, offset);
            offset += recordSizeInMemory;
        }
        return result;
    }

    public T arrayifyScalar(long compoundDataTypeId, byte[] byteArr, Class<T> recordClass)
    {
        if (byteArr.length < recordSizeInMemory)
        {
            throw new HDF5JavaException("Illegal byte array for scalar compound type (length "
                    + byteArr.length + " is smaller than record size " + recordSizeInMemory + ")");
        }
        return primArrayifyScalar(compoundDataTypeId, byteArr, recordClass, 0);
    }

    private T primArrayifyScalar(long compoundDataTypeId, byte[] byteArr, Class<T> recordClass,
            int offset)
    {
        T result = newInstance(recordClass);
        for (HDF5MemberByteifyer byteifyer : byteifyers)
        {
            try
            {
                byteifyer.setFromByteArray(compoundDataTypeId, result, byteArr, offset);
            } catch (IllegalAccessException ex)
            {
                throw new HDF5JavaException("Error accessing " + byteifyer.describe());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private T newInstance(Class<?> recordClass) throws HDF5JavaException
    {
        if (Map.class.isAssignableFrom(recordClass))
        {
            return newMap(byteifyers.length);
        }
        if (List.class.isAssignableFrom(recordClass))
        {
            return newList(byteifyers.length);
        }
        if (recordClass == Object[].class)
        {
            return newArray(byteifyers.length);
        }
        try
        {
            if (recordClass != cachedRecordClass)
            {
                cachedRecordClass = recordClass;
                cachedDefaultConstructor = ReflectionUtils.getDefaultConstructor(recordClass);
            }
            return (T) cachedDefaultConstructor.newInstance();
        } catch (Exception ex)
        {
            throw new HDF5JavaException("Creation of new object of class "
                    + recordClass.getCanonicalName() + " by default constructor failed: "
                    + ex.toString());
        }
    }

    public int getRecordSizeOnDisk()
    {
        return recordSizeOnDisk;
    }

    public int getRecordSizeInMemory()
    {
        return recordSizeInMemory;
    }

    public HDF5MemberByteifyer[] getByteifyers()
    {
        return byteifyers;
    }

    /**
     * Returns <code>true</code> if the value object byteifyer has any members that cannot be mapped
     * to the in-memory representation.
     */
    public boolean hasUnmappedMembers()
    {
        for (HDF5MemberByteifyer memberByteifyer : byteifyers)
        {
            if (memberByteifyer.isDummy())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list with the names of all members that cannot be mapped to the in-memory
     * representation.
     */
    public String[] getUnmappedMembers()
    {
        if (hasUnmappedMembers())
        {
            final List<String> unmappedMembers = new ArrayList<String>();
            for (HDF5MemberByteifyer memberByteifyer : byteifyers)
            {
                if (memberByteifyer.isDummy())
                {
                    unmappedMembers.add(memberByteifyer.getMemberName());
                }
            }
            return unmappedMembers.toArray(new String[unmappedMembers.size()]);
        } else
        {
            return new String[0];
        }
    }

    boolean hasVLMembers()
    {
        return vlMemberIndices.length > 0;
    }

    int[] getVLMemberIndices()
    {
        return vlMemberIndices;
    }

    //
    // Object
    //

    @Override
    public String toString()
    {
        return "HDF5ValueObjectByteifyer [byteifyers=" + Arrays.toString(byteifyers) + "]";
    }

}
