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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.systemsx.cisd.hdf5.HDF5ValueObjectByteifyer.IFileAccessProvider;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

/**
 * A factory for {@link HDF5MemberByteifyer}s.
 * 
 * @author Bernd Rinn
 */
class HDF5CompoundByteifyerFactory
{

    private static List<IHDF5CompoundMemberBytifyerFactory> memberFactories =
            new ArrayList<IHDF5CompoundMemberBytifyerFactory>(14);

    static
    {
        memberFactories.add(new HDF5CompoundMemberByteifyerBooleanFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerIntFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerLongFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerShortFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerByteFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerFloatFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerDoubleFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerStringFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerBitSetFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerDateFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerHDF5TimeDurationFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerEnumFactory());
        memberFactories.add(new HDF5CompoundMemberByteifyerEnumArrayFactory());
    }

    /**
     * The type of access to the information.
     */
    enum AccessType
    {
        FIELD, MAP, LIST, ARRAY
    }

    /**
     * The interface for member factories.
     */
    interface IHDF5CompoundMemberBytifyerFactory
    {
        /**
         * Returns <code>true</code> if this factory can handle a member of type <code>clazz</code>.
         */
        boolean canHandle(Class<?> clazz, HDF5CompoundMemberInformation memberInfoOrNull);

        /**
         * Creates a byteifyer.
         */
        HDF5MemberByteifyer createBytifyer(AccessType accessType, Field fieldOrNull,
                HDF5CompoundMemberMapping member,
                HDF5CompoundMemberInformation compoundMemberInfoOrNull,
                HDF5EnumerationType enumTypeOrNull, Class<?> memberClazz, int index, int offset,
                int memOffset, IFileAccessProvider fileInfoProvider);

        /**
         * Returns a suitable Java type, if this factory has one, or <code>null</code> otherwise.
         */
        Class<?> tryGetOverrideJavaType(HDF5DataClass dataClass, int rank, int elementSize,
                HDF5DataTypeVariant typeVariantOrNull);
    }

    /**
     * Returns a Java type overriding the one given by {@link HDF5DataClass}, if the factories have
     * one, or <code>null</code> otherwise.
     */
    static Class<?> tryGetOverrideJavaType(HDF5DataClass dataClass, int rank, int elementSize,
            HDF5DataTypeVariant typeVariantOrNull)
    {
        for (IHDF5CompoundMemberBytifyerFactory factory : memberFactories)
        {
            final Class<?> javaClassOrNull =
                    factory.tryGetOverrideJavaType(dataClass, rank, elementSize, typeVariantOrNull);
            if (javaClassOrNull != null)
            {
                return javaClassOrNull;
            }
        }
        return null;
    }

    static HDF5MemberByteifyer[] createMemberByteifyers(Class<?> clazz,
            IFileAccessProvider fileInfoProvider, CompoundTypeInformation compoundTypeInfoOrNull,
            HDF5CompoundMemberMapping[] members)
    {
        final HDF5MemberByteifyer[] result = new HDF5MemberByteifyer[members.length];
        int offsetOnDisk = 0;
        int offsetInMemory = 0;
        for (int i = 0; i < result.length; ++i)
        {
            final AccessType accessType = getAccessType(clazz);
            final HDF5CompoundMemberInformation compoundMemberInfoOrNull =
                    (compoundTypeInfoOrNull == null) ? null : compoundTypeInfoOrNull.getMember(i);
            final Field fieldOrNull =
                    (accessType == AccessType.FIELD) ? members[i].tryGetField(clazz,
                            (compoundMemberInfoOrNull != null)) : null;
            final Class<?> memberClazzOrNull =
                    (fieldOrNull != null) ? fieldOrNull.getType() : members[i].tryGetMemberClass();
            final IHDF5CompoundMemberBytifyerFactory factory =
                    findFactory(memberClazzOrNull, compoundMemberInfoOrNull,
                            members[i].getMemberName());
            final HDF5EnumerationType enumTypeOrNullOrNull =
                    (compoundTypeInfoOrNull == null) ? null : compoundTypeInfoOrNull.enumTypes[i];
            if (compoundMemberInfoOrNull != null)
            {
                offsetOnDisk = compoundMemberInfoOrNull.getOffsetOnDisk();
                offsetInMemory = compoundMemberInfoOrNull.getOffsetInMemory();
            }
            if (isDummy(accessType, fieldOrNull))
            {
                result[i] =
                        new HDF5DummyMemberByteifyer(factory.createBytifyer(accessType,
                                fieldOrNull, members[i], compoundMemberInfoOrNull,
                                enumTypeOrNullOrNull, memberClazzOrNull, i, offsetOnDisk,
                                offsetInMemory, fileInfoProvider));
            } else
            {
                result[i] =
                        factory.createBytifyer(accessType, fieldOrNull, members[i],
                                compoundMemberInfoOrNull, enumTypeOrNullOrNull, memberClazzOrNull,
                                i, offsetOnDisk, offsetInMemory, fileInfoProvider);
            }
            if (compoundMemberInfoOrNull == null)
            {
                final int size = result[i].getSize();
                final int elementSize = result[i].getElementSize();
                offsetOnDisk += size;
                offsetInMemory = PaddingUtils.padOffset(offsetInMemory + size, elementSize);
            }
        }
        return result;
    }

    //
    // Dummy helpers
    //

    private static boolean isDummy(AccessType accessType, Field fieldOrNull)
    {
        return (accessType == AccessType.FIELD) && (fieldOrNull == null);
    }

    private static class HDF5DummyMemberByteifyer extends HDF5MemberByteifyer
    {
        private final HDF5MemberByteifyer delegate;

        public HDF5DummyMemberByteifyer(HDF5MemberByteifyer delegate)
        {
            super(null, null, 0, 0, 0, false, null);
            this.delegate = delegate;
        }

        @Override
        int getElementSize()
        {
            return 0;
        }

        @Override
        public byte[] byteify(long compoundDataTypeId, Object obj) throws IllegalAccessException
        {
            // Dummy implementation
            return new byte[delegate.getSize()];
        }

        @Override
        public void setFromByteArray(long compoundDataTypeId, Object obj, byte[] byteArr,
                int arrayOffset) throws IllegalAccessException
        {
            // Dummy implementation
        }

        @Override
        protected long getMemberStorageTypeId()
        {
            return delegate.getMemberStorageTypeId();
        }

        @Override
        protected long getMemberNativeTypeId()
        {
            return delegate.getMemberNativeTypeId();
        }

        @Override
        public HDF5DataTypeVariant getTypeVariant()
        {
            return delegate.getTypeVariant();
        }

        @Override
        public void insertType(long dataTypeId)
        {
            delegate.insertType(dataTypeId);
        }

        @Override
        public void insertNativeType(long dataTypeId, HDF5 h5, ICleanUpRegistry registry)
        {
            delegate.insertNativeType(dataTypeId, h5, registry);
        }

        @Override
        public int getMaxCharacters()
        {
            return delegate.getMaxCharacters();
        }

        @Override
        public int getSize()
        {
            return delegate.getSize();
        }

        @Override
        public int getOffsetOnDisk()
        {
            return delegate.getOffsetOnDisk();
        }

        @Override
        public int getOffsetInMemory()
        {
            return delegate.getOffsetInMemory();
        }

        @Override
        public int getTotalSizeOnDisk()
        {
            return delegate.getTotalSizeOnDisk();
        }

        @Override
        public int getTotalSizeInMemory()
        {
            return delegate.getTotalSizeInMemory();
        }

        @Override
        public String getMemberName()
        {
            return delegate.getMemberName();
        }

        @Override
        public String describe()
        {
            return delegate.describe();
        }

        @Override
        public boolean isDummy()
        {
            return true;
        }

        @Override
        public String toString()
        {
            return delegate.toString();
        }

    }

    //
    // Auxiliary getter and setter methods.
    //

    private static IHDF5CompoundMemberBytifyerFactory findFactory(Class<?> memberClazz,
            HDF5CompoundMemberInformation memberInfoOrNull, String memberName)
    {
        if (memberClazz == null)
        {
            throw new IllegalArgumentException("No type given for member '" + memberName + "'.");
        }
        for (IHDF5CompoundMemberBytifyerFactory factory : memberFactories)
        {
            if (factory.canHandle(memberClazz, memberInfoOrNull))
            {
                return factory;
            }
        }
        if (memberInfoOrNull == null)
        {
            throw new IllegalArgumentException("The member '" + memberName + "' is of type '"
                    + memberClazz.getCanonicalName()
                    + "' which cannot be handled by any HDFMemberByteifyer.");
        } else
        {
            throw new IllegalArgumentException("The member '" + memberName + "' is of type '"
                    + memberClazz.getCanonicalName() + "' [memory] and '"
                    + memberInfoOrNull.getType()
                    + "' [disk] which cannot be handled by any HDFMemberByteifyer.");
        }
    }

    private static AccessType getAccessType(Class<?> clazz)
    {
        if (Map.class.isAssignableFrom(clazz))
        {
            return AccessType.MAP;
        } else if (List.class.isAssignableFrom(clazz))
        {
            return AccessType.LIST;
        } else if (Object[].class == clazz)
        {
            return AccessType.ARRAY;
        } else
        {
            return AccessType.FIELD;
        }
    }

    @SuppressWarnings("unchecked")
    static Object getMap(Object obj, final String name)
    {
        return ((Map<String, Object>) obj).get(name);
    }

    @SuppressWarnings("unchecked")
    static Object getList(Object obj, final int index)
    {
        return ((List<Object>) obj).get(index);
    }

    static Object getArray(Object obj, final int index)
    {
        return ((Object[]) obj)[index];
    }

    @SuppressWarnings("unchecked")
    static void putMap(final Object obj, final String memberName, final Object value)
    {
        ((Map<String, Object>) obj).put(memberName, value);
    }

    @SuppressWarnings("unchecked")
    static void setList(final Object obj, final int index, final Object value)
    {
        ((List<Object>) obj).set(index, value);
    }

    static void setArray(final Object obj, final int index, final Object value)
    {
        ((Object[]) obj)[index] = value;
    }

}
