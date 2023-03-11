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

import java.util.BitSet;
import java.util.Date;
import java.util.List;

import hdf.hdf5lib.exceptions.HDF5DatatypeInterfaceException;
import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * A class for reading HDF5 files (HDF5 1.8.x and older).
 * <p>
 * The class focuses on ease of use instead of completeness. As a consequence not all features of a
 * valid HDF5 files can be read using this class, but only a subset. (All information written by
 * {@link HDF5Writer} can be read by this class.)
 * <p>
 * Usage:
 * 
 * <pre>
 * HDF5Reader reader = new HDF5ReaderConfig(&quot;test.h5&quot;).reader();
 * float[] f = reader.readFloatArray(&quot;/some/path/dataset&quot;);
 * String s = reader.getAttributeString(&quot;/some/path/dataset&quot;, &quot;some key&quot;);
 * reader.close();
 * </pre>
 * 
 * @author Bernd Rinn
 */
class HDF5Reader implements IHDF5Reader
{
    private final HDF5BaseReader baseReader;
    
    private final IHDF5FileLevelReadOnlyHandler fileHandler;
    
    private final IHDF5ObjectReadOnlyInfoProviderHandler objectHandler;

    private final IHDF5ByteReader byteReader;

    private final IHDF5ByteReader ubyteReader;

    private final IHDF5ShortReader shortReader;

    private final IHDF5ShortReader ushortReader;

    private final IHDF5IntReader intReader;

    private final IHDF5IntReader uintReader;

    protected final IHDF5LongReader longReader;

    private final IHDF5LongReader ulongReader;

    private final IHDF5FloatReader floatReader;

    private final IHDF5DoubleReader doubleReader;

    private final IHDF5BooleanReader booleanReader;

    private final IHDF5StringReader stringReader;

    private final IHDF5EnumReader enumReader;

    private final IHDF5CompoundReader compoundReader;

    private final IHDF5DateTimeReader dateTimeReader;

    private final HDF5TimeDurationReader timeDurationReader;

    private final IHDF5ReferenceReader referenceReader;

    private final IHDF5OpaqueReader opaqueReader;

    HDF5Reader(final HDF5BaseReader baseReader)
    {
        assert baseReader != null;

        this.baseReader = baseReader;
        // Ensure the finalizer of this HDF5Reader doesn't close the file behind the back of the 
        // specialized readers when they are still in operation.
        baseReader.setMyReader(this);
        this.fileHandler = new HDF5FileLevelReadOnlyHandler(baseReader);
        this.objectHandler = new HDF5ObjectReadOnlyInfoProviderHandler(baseReader);
        this.byteReader = new HDF5ByteReader(baseReader);
        this.ubyteReader = new HDF5UnsignedByteReader(baseReader);
        this.shortReader = new HDF5ShortReader(baseReader);
        this.ushortReader = new HDF5UnsignedShortReader(baseReader);
        this.intReader = new HDF5IntReader(baseReader);
        this.uintReader = new HDF5UnsignedIntReader(baseReader);
        this.longReader = new HDF5LongReader(baseReader);
        this.ulongReader = new HDF5UnsignedLongReader(baseReader);
        this.floatReader = new HDF5FloatReader(baseReader);
        this.doubleReader = new HDF5DoubleReader(baseReader);
        this.booleanReader = new HDF5BooleanReader(baseReader);
        this.stringReader = new HDF5StringReader(baseReader);
        this.enumReader = new HDF5EnumReader(baseReader);
        this.compoundReader = new HDF5CompoundReader(baseReader, enumReader);
        this.dateTimeReader = new HDF5DateTimeReader(baseReader, (HDF5LongReader) longReader);
        this.timeDurationReader = new HDF5TimeDurationReader(baseReader, (HDF5LongReader) longReader);
        this.referenceReader = new HDF5ReferenceReader(baseReader);
        this.opaqueReader = new HDF5OpaqueReader(baseReader);
    }

    void checkOpen()
    {
        baseReader.checkOpen();
    }

    long getFileId()
    {
        return baseReader.fileId;
    }

    // /////////////////////
    // File
    // /////////////////////
    
    @Override
    public IHDF5FileLevelReadOnlyHandler file()
    {
        return fileHandler;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        close();
    }

    @Override
    public void close()
    {
        baseReader.close();
    }

    // /////////////////////////////////
    // Objects, links, groups and types
    // /////////////////////////////////

    @Override
    public IHDF5ObjectReadOnlyInfoProviderHandler object()
    {
        return objectHandler;
    }

    @Override
    public boolean exists(String objectPath)
    {
        return objectHandler.exists(objectPath);
    }

    @Override
    public boolean isGroup(String objectPath)
    {
        return objectHandler.isGroup(objectPath);
    }

    @Override
    public HDF5DataSetInformation getDataSetInformation(String dataSetPath)
    {
        return objectHandler.getDataSetInformation(dataSetPath);
    }

    @Override
    public List<String> getGroupMembers(String groupPath)
    {
        return objectHandler.getGroupMembers(groupPath);
    }

    // /////////////////////
    // Data Sets reading
    // /////////////////////

    //
    // Opaque
    //

    @Override
    public IHDF5OpaqueReader opaque()
    {
        return opaqueReader;
    }

    @Override
    public byte[] readAsByteArray(String objectPath)
    {
        return opaqueReader.readArray(objectPath);
    }

    //
    // Boolean
    //

    @Override
    public IHDF5BooleanReader bool()
    {
        return booleanReader;
    }

    @Override
    public BitSet readBitField(String objectPath) throws HDF5DatatypeInterfaceException
    {
        return booleanReader.readBitField(objectPath);
    }

    @Override
    public boolean readBoolean(String objectPath) throws HDF5JavaException
    {
        return booleanReader.read(objectPath);
    }

    //
    // Time & date
    //

    @Override
    public IHDF5DateTimeReader time()
    {
        return dateTimeReader;
    }

    @Override
    public IHDF5TimeDurationReader duration()
    {
        return timeDurationReader;
    }

    @Override
    public Date readDate(String objectPath) throws HDF5JavaException
    {
        return dateTimeReader.readDate(objectPath);
    }

    @Override
    public Date[] readDateArray(String objectPath) throws HDF5JavaException
    {
        return dateTimeReader.readDateArray(objectPath);
    }

    @Override
    public HDF5TimeDuration readTimeDuration(String objectPath) throws HDF5JavaException
    {
        return timeDurationReader.read(objectPath);
    }

    @Override
    public HDF5TimeDurationArray readTimeDurationArray(String objectPath) throws HDF5JavaException
    {
        return timeDurationReader.readArray(objectPath);
    }

    //
    // Reference
    //

    @Override
    public IHDF5ReferenceReader reference()
    {
        return referenceReader;
    }

    //
    // References
    //

    //
    // String
    //

    @Override
    public IHDF5StringReader string()
    {
        return stringReader;
    }

    @Override
    public String readString(String objectPath) throws HDF5JavaException
    {
        return stringReader.read(objectPath);
    }

    @Override
    public String[] readStringArray(String objectPath) throws HDF5JavaException
    {
        return stringReader.readArray(objectPath);
    }

    //
    // Enums
    //

    @Override
    public IHDF5EnumReader enumeration()
    {
        return enumReader;
    }

    @Override
    public <T extends Enum<T>> T readEnum(String objectPath, Class<T> enumClass)
            throws HDF5JavaException
    {
        return enumReader.read(objectPath, enumClass);
    }

    @Override
    public <T extends Enum<T>> T[] readEnumArray(String objectPath, Class<T> enumClass)
            throws HDF5JavaException
    {
        return enumReader.readArray(objectPath).toEnumArray(enumClass);
    }

    @Override
    public String[] readEnumArrayAsString(String objectPath) throws HDF5JavaException
    {
        return enumReader.readArray(objectPath).toStringArray();
    }

    @Override
    public String readEnumAsString(String objectPath) throws HDF5JavaException
    {
        return enumReader.readAsString(objectPath);
    }

    //
    // Compounds
    //

    @Override
    public IHDF5CompoundReader compound()
    {
        return compoundReader;
    }

    @Override
    public <T> T readCompound(String objectPath, Class<T> pojoClass) throws HDF5JavaException
    {
        return compoundReader.read(objectPath, pojoClass);
    }

    @Override
    public <T> T[] readCompoundArray(String objectPath, Class<T> pojoClass)
            throws HDF5JavaException
    {
        return compoundReader.readArray(objectPath, pojoClass);
    }

    // ------------------------------------------------------------------------------
    // Primite types - START
    // ------------------------------------------------------------------------------

    @Override
    public double readDouble(String objectPath)
    {
        return doubleReader.read(objectPath);
    }

    @Override
    public double[] readDoubleArray(String objectPath)
    {
        return doubleReader.readArray(objectPath);
    }

    @Override
    public double[][] readDoubleMatrix(String objectPath) throws HDF5JavaException
    {
        return doubleReader.readMatrix(objectPath);
    }

    @Override
    public float readFloat(String objectPath)
    {
        return floatReader.read(objectPath);
    }

    @Override
    public float[] readFloatArray(String objectPath)
    {
        return floatReader.readArray(objectPath);
    }

    @Override
    public float[][] readFloatMatrix(String objectPath) throws HDF5JavaException
    {
        return floatReader.readMatrix(objectPath);
    }

    @Override
    public int readInt(String objectPath)
    {
        return intReader.read(objectPath);
    }

    @Override
    public int[] readIntArray(String objectPath)
    {
        return intReader.readArray(objectPath);
    }

    @Override
    public int[][] readIntMatrix(String objectPath) throws HDF5JavaException
    {
        return intReader.readMatrix(objectPath);
    }

    @Override
    public long readLong(String objectPath)
    {
        return longReader.read(objectPath);
    }

    @Override
    public long[] readLongArray(String objectPath)
    {
        return longReader.readArray(objectPath);
    }

    @Override
    public long[][] readLongMatrix(String objectPath) throws HDF5JavaException
    {
        return longReader.readMatrix(objectPath);
    }

    @Override
    public IHDF5ByteReader int8()
    {
        return byteReader;
    }

    @Override
    public IHDF5ByteReader uint8()
    {
        return ubyteReader;
    }

    @Override
    public IHDF5ShortReader int16()
    {
        return shortReader;
    }

    @Override
    public IHDF5ShortReader uint16()
    {
        return ushortReader;
    }

    @Override
    public IHDF5IntReader int32()
    {
        return intReader;
    }

    @Override
    public IHDF5IntReader uint32()
    {
        return uintReader;
    }

    @Override
    public IHDF5LongReader int64()
    {
        return longReader;
    }

    @Override
    public IHDF5LongReader uint64()
    {
        return ulongReader;
    }

    @Override
    public IHDF5FloatReader float32()
    {
        return floatReader;
    }

    @Override
    public IHDF5DoubleReader float64()
    {
        return doubleReader;
    }

    // ------------------------------------------------------------------------------
    // Primitive types - END
    // ------------------------------------------------------------------------------

}
