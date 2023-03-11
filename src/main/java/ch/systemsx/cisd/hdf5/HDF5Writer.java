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

import hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * A class for writing HDF5 files (HDF5 1.6.x or HDF5 1.8.x).
 * <p>
 * The class focuses on ease of use instead of completeness. As a consequence not all valid HDF5
 * files can be generated using this class, but only a subset.
 * <p>
 * Usage:
 * 
 * <pre>
 * float[] f = new float[100];
 * ...
 * HDF5Writer writer = new HDF5WriterConfig(&quot;test.h5&quot;).writer();
 * writer.writeFloatArray(&quot;/some/path/dataset&quot;, f);
 * writer.addAttribute(&quot;some key&quot;, &quot;some value&quot;);
 * writer.close();
 * </pre>
 * 
 * @author Bernd Rinn
 */
final class HDF5Writer extends HDF5Reader implements IHDF5Writer
{
    private final HDF5BaseWriter baseWriter;

    private final IHDF5FileLevelReadWriteHandler fileHandler;

    private final IHDF5ObjectReadWriteInfoProviderHandler objectHandler;

    private final IHDF5ByteWriter byteWriter;

    private final IHDF5ByteWriter ubyteWriter;

    private final IHDF5ShortWriter shortWriter;

    private final IHDF5ShortWriter ushortWriter;

    private final IHDF5IntWriter intWriter;

    private final IHDF5IntWriter uintWriter;

    private final IHDF5LongWriter longWriter;

    private final IHDF5LongWriter ulongWriter;

    private final IHDF5FloatWriter floatWriter;

    private final IHDF5DoubleWriter doubleWriter;

    private final IHDF5BooleanWriter booleanWriter;

    private final IHDF5StringWriter stringWriter;

    private final IHDF5EnumWriter enumWriter;

    private final IHDF5CompoundWriter compoundWriter;

    private final IHDF5DateTimeWriter dateTimeWriter;

    private final HDF5TimeDurationWriter timeDurationWriter;

    private final IHDF5ReferenceWriter referenceWriter;

    private final IHDF5OpaqueWriter opaqueWriter;

    HDF5Writer(HDF5BaseWriter baseWriter)
    {
        super(baseWriter);
        this.baseWriter = baseWriter;
        // Ensure the finalizer of this HDF5Writer doesn't close the file behind the back of the 
        // specialized writers when they are still in operation.
        baseWriter.setMyReader(this);
        this.fileHandler = new HDF5FileLevelReadWriteHandler(baseWriter);
        this.objectHandler = new HDF5ObjectReadWriteInfoProviderHandler(baseWriter);
        this.byteWriter = new HDF5ByteWriter(baseWriter);
        this.ubyteWriter = new HDF5UnsignedByteWriter(baseWriter);
        this.shortWriter = new HDF5ShortWriter(baseWriter);
        this.ushortWriter = new HDF5UnsignedShortWriter(baseWriter);
        this.intWriter = new HDF5IntWriter(baseWriter);
        this.uintWriter = new HDF5UnsignedIntWriter(baseWriter);
        this.longWriter = new HDF5LongWriter(baseWriter);
        this.ulongWriter = new HDF5UnsignedLongWriter(baseWriter);
        this.floatWriter = new HDF5FloatWriter(baseWriter);
        this.doubleWriter = new HDF5DoubleWriter(baseWriter);
        this.booleanWriter = new HDF5BooleanWriter(baseWriter);
        this.stringWriter = new HDF5StringWriter(baseWriter);
        this.enumWriter = new HDF5EnumWriter(baseWriter);
        this.compoundWriter = new HDF5CompoundWriter(baseWriter, enumWriter);
        this.dateTimeWriter = new HDF5DateTimeWriter(baseWriter, (HDF5LongReader) longReader);
        this.timeDurationWriter =
                new HDF5TimeDurationWriter(baseWriter, (HDF5LongReader) longReader);
        this.referenceWriter = new HDF5ReferenceWriter(baseWriter);
        this.opaqueWriter = new HDF5OpaqueWriter(baseWriter);
    }

    HDF5BaseWriter getBaseWriter()
    {
        return baseWriter;
    }

    // /////////////////////
    // File
    // /////////////////////

    @Override
    public IHDF5FileLevelReadWriteHandler file()
    {
        return fileHandler;
    }

    // /////////////////////////////////
    // Objects, links, groups and types
    // /////////////////////////////////

    @Override
    public IHDF5ObjectReadWriteInfoProviderHandler object()
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
    public void delete(String objectPath)
    {
        objectHandler.delete(objectPath);
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

    // /////////////////////////////
    // Data Set Reading and Writing
    // /////////////////////////////

    //
    // Boolean
    //

    @Override
    public IHDF5BooleanWriter bool()
    {
        return booleanWriter;
    }

    @Override
    public void writeBitField(String objectPath, BitSet data)
    {
        booleanWriter.writeBitField(objectPath, data);
    }

    @Override
    public void writeBoolean(String objectPath, boolean value)
    {
        booleanWriter.write(objectPath, value);
    }

    //
    // Opaque
    //

    @Override
    public IHDF5OpaqueWriter opaque()
    {
        return opaqueWriter;
    }

    //
    // Date
    //

    @Override
    public IHDF5DateTimeWriter time()
    {
        return dateTimeWriter;
    }

    @Override
    public IHDF5TimeDurationWriter duration()
    {
        return timeDurationWriter;
    }

    @Override
    public void writeDate(String objectPath, Date date)
    {
        dateTimeWriter.write(objectPath, date);
    }

    @Override
    public void writeDateArray(String objectPath, Date[] dates)
    {
        dateTimeWriter.writeArray(objectPath, dates);
    }

    //
    // Duration
    //

    @Override
    public void writeTimeDuration(String objectPath, HDF5TimeDuration timeDuration)
    {
        timeDurationWriter.write(objectPath, timeDuration);
    }

    @Override
    public void writeTimeDurationArray(String objectPath, HDF5TimeDurationArray timeDurations)
    {
        timeDurationWriter.writeArray(objectPath, timeDurations);
    }

    //
    // References
    //

    @Override
    public IHDF5ReferenceWriter reference()
    {
        return referenceWriter;
    }

    //
    // String
    //

    @Override
    public IHDF5StringWriter string()
    {
        return stringWriter;
    }

    @Override
    public void writeString(String objectPath, String data)
    {
        stringWriter.write(objectPath, data);
    }

    @Override
    public void writeStringArray(String objectPath, String[] data)
    {
        stringWriter.writeArray(objectPath, data);
    }

    //
    // Enum
    //

    @Override
    public IHDF5EnumWriter enumeration()
    {
        return enumWriter;
    }

    @Override
    public <T extends Enum<T>> void writeEnum(String objectPath, Enum<T> value)
            throws HDF5JavaException
    {
        enumWriter.write(objectPath, value);
    }

    @Override
    public <T extends Enum<T>> void writeEnumArray(String objectPath, Enum<T>[] data)
    {
        enumWriter.writeArray(objectPath, enumWriter.newAnonArray(data));
    }

    @Override
    public void writeEnumArray(String objectPath, String[] options, String[] data)
    {
        enumWriter.writeArray(objectPath, enumWriter.newAnonArray(options, data));
    }

    //
    // Compound
    //

    @Override
    public IHDF5CompoundWriter compound()
    {
        return compoundWriter;
    }

    @Override
    public <T> void writeCompound(String objectPath, T data)
    {
        compoundWriter.write(objectPath, data);
    }

    @Override
    public <T> void writeCompoundArray(String objectPath, T[] data)
    {
        compoundWriter.writeArray(objectPath, data);
    }

    // ------------------------------------------------------------------------------
    // Primitive types - START
    // ------------------------------------------------------------------------------

    @Override
    public void writeByteArray(String objectPath, byte[] data)
    {
        byteWriter.writeArray(objectPath, data);
    }

    @Override
    public void writeDouble(String objectPath, double value)
    {
        doubleWriter.write(objectPath, value);
    }

    @Override
    public void writeDoubleArray(String objectPath, double[] data)
    {
        doubleWriter.writeArray(objectPath, data);
    }

    @Override
    public void writeDoubleMatrix(String objectPath, double[][] data)
    {
        doubleWriter.writeMatrix(objectPath, data);
    }

    @Override
    public void writeFloat(String objectPath, float value)
    {
        floatWriter.write(objectPath, value);
    }

    @Override
    public void writeFloatArray(String objectPath, float[] data)
    {
        floatWriter.writeArray(objectPath, data);
    }

    @Override
    public void writeFloatMatrix(String objectPath, float[][] data)
    {
        floatWriter.writeMatrix(objectPath, data);
    }

    @Override
    public void writeInt(String objectPath, int value)
    {
        intWriter.write(objectPath, value);
    }

    @Override
    public void writeIntArray(String objectPath, int[] data)
    {
        intWriter.writeArray(objectPath, data);
    }

    @Override
    public void writeIntMatrix(String objectPath, int[][] data)
    {
        intWriter.writeMatrix(objectPath, data);
    }

    @Override
    public void writeLong(String objectPath, long value)
    {
        longWriter.write(objectPath, value);
    }

    @Override
    public void writeLongArray(String objectPath, long[] data)
    {
        longWriter.writeArray(objectPath, data);
    }

    @Override
    public void writeLongMatrix(String objectPath, long[][] data)
    {
        longWriter.writeMatrix(objectPath, data);
    }

    @Override
    public IHDF5ByteWriter int8()
    {
        return byteWriter;
    }

    @Override
    public IHDF5ByteWriter uint8()
    {
        return ubyteWriter;
    }

    @Override
    public IHDF5ShortWriter int16()
    {
        return shortWriter;
    }

    @Override
    public IHDF5ShortWriter uint16()
    {
        return ushortWriter;
    }

    @Override
    public IHDF5IntWriter int32()
    {
        return intWriter;
    }

    @Override
    public IHDF5IntWriter uint32()
    {
        return uintWriter;
    }

    @Override
    public IHDF5LongWriter int64()
    {
        return longWriter;
    }

    @Override
    public IHDF5LongWriter uint64()
    {
        return ulongWriter;
    }

    @Override
    public IHDF5FloatWriter float32()
    {
        return floatWriter;
    }

    @Override
    public IHDF5DoubleWriter float64()
    {
        return doubleWriter;
    }

    // ------------------------------------------------------------------------------
    // Primitive Types - END
    // ------------------------------------------------------------------------------
}
