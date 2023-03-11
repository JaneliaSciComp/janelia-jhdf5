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

/**
 * An interface for writing HDF5 files (HDF5 1.6.x, HDF5 1.8.x or HDF5 1.10.x).
 * <p>
 * Obtain an object implementing this interface by calling {@link HDF5Factory#open(String)} or 
 * {@link IHDF5WriterConfigurator#writer()}.
 * <p>
 * The interface focuses on ease of use instead of completeness. As a consequence not all features
 * of HDF5 are supported by this class, however it covers a large subset.
 * <p>
 * The functionality is being made available in two ways:
 * <ol>
 * <li>{@link IHDF5SimpleWriter} contains the most important methods in one interface. If you are
 * new to the library, this is a good starting point, see the example code below.</li>
 * <li>The hierarchical ("quasi-fluent") API provides the full functionality. It is designed along
 * the data types supported by JHDF5.
 * <ul>
 * <li>{@link #file()}: File-level information and operations, has e.g. the
 * {@link IHDF5FileLevelReadWriteHandler#close()} and {@link IHDF5FileLevelReadWriteHandler#flush()}
 * methods.</li>
 * <li>{@link #object()}: Object-level information, where "objects" can be data sets, links, groups
 * or data types, following the concept of an HDF5 object. Here you can find methods like
 * {@link IHDF5ObjectReadWriteInfoProviderHandler#createGroup(String)} for creating a new group, or
 * {@link IHDF5ObjectReadWriteInfoProviderHandler#createSoftLink(String, String)} for creating a
 * symbolic link.</li>
 * <li>{@link #bool()}: Writer methods for boolean data sets, including bit fields.</li>
 * <li>{@link #int8()} / {@link #int16()} / {@link #int16()} / {@link #int32()} / {@link #int64()}:
 * Writer methods for signed integer data sets, where the number as part of the method name denotes
 * the size of the integer type.</li>
 * <li>{@link #uint8()} / {@link #uint16()} / {@link #uint16()} / {@link #uint32()} /
 * {@link #int64()}: Writer methods for unsigned integer data sets, where the number as part of the
 * name sets the size of the integer type. While the data sets take signed integer values due to
 * Java's lack of unsigned integer types, they <i>represent</i> them as unsigned values in the HDF5
 * file. See {@link UnsignedIntUtils} for conversion methods, e.g.
 * <code>uint32().write("myint", UnsignedIntUtils.toInt16(50000))</code> will write a 16-bit
 * unsigned integer with value 50000.</li>
 * <li>{@link #float32()} / {@link #float64()}: Writer methods for float data sets, where the number
 * as part of the name sets the size of the float type.</li>
 * <li>{@link #time()} / {@link #duration()}: Writer methods for time stamp (or date) and for time
 * duration data sets.</li>
 * <li>{@link #string()}: Writer methods for string data sets.</li>
 * <li>{@link #enumeration()}: Writer methods for enumeration data sets.</li>
 * <li>{@link #compound()}: Writer methods for compound data sets.</li>
 * <li>{@link #opaque()}: Writer methods for data sets that are "black boxes" to HDF5 which are
 * called "opaque data sets" in HDF5 jargon. Here you can also find methods of reading arbitrary
 * data sets as byte arrays.</li>
 * <li>{@link #reference()}: Writer methods for HDF5 object references. Note that object references,
 * though similar to hard links and symbolic links on the first glance, are quite different for
 * HDF5.</li>
 * </ul>
 * </li>
 * </ol>
 * <p>
 * Simple usage example:
 * 
 * <pre>
 * float[] f = new float[100];
 * ...
 * IHDF5Writer writer = HDF5FactoryProvider.get().open(new File(&quot;test.h5&quot;));
 * writer.writeFloatArray(&quot;/some/path/dataset&quot;, f);
 * writer.setStringAttribute(&quot;some key&quot;, &quot;some value&quot;);
 * writer.close();
 * </pre>
 * 
 * @author Bernd Rinn
 */
public interface IHDF5Writer extends IHDF5Reader, IHDF5SimpleWriter
{
    // /////////////////////
    // File
    // /////////////////////

    /**
     * Returns the handler for file-level information and status.
     */
    @Override
    public IHDF5FileLevelReadWriteHandler file();

    // /////////////////////////////////
    // Objects, links, groups and types
    // /////////////////////////////////

    /**
     * Returns an info provider and handler for HDF5 objects like links, groups, data sets and data
     * types.
     */
    @Override
    public IHDF5ObjectReadWriteInfoProviderHandler object();

    // /////////////////////
    // Opaque
    // /////////////////////

    /**
     * Returns the full writer for opaque values.
     */
    @Override
    public IHDF5OpaqueWriter opaque();

    // /////////////////////
    // Boolean
    // /////////////////////

    /**
     * Returns the full writer for boolean values.
     */
    @Override
    public IHDF5BooleanWriter bool();

    // /////////////////////
    // Bytes
    // /////////////////////

    /**
     * Returns the full writer for byte / int8.
     */
    @Override
    public IHDF5ByteWriter int8();

    /**
     * Returns the full writer for unsigned byte / uint8.
     */
    @Override
    public IHDF5ByteWriter uint8();

    // /////////////////////
    // Short
    // /////////////////////

    /**
     * Returns the full writer for short / int16.
     */
    @Override
    public IHDF5ShortWriter int16();

    /**
     * Returns the full writer for unsigned short / uint16.
     */
    @Override
    public IHDF5ShortWriter uint16();

    // /////////////////////
    // Int
    // /////////////////////

    /**
     * Returns the full writer for int / int32.
     */
    @Override
    public IHDF5IntWriter int32();

    /**
     * Returns the full writer for unsigned int / uint32.
     */
    @Override
    public IHDF5IntWriter uint32();

    // /////////////////////
    // Long
    // /////////////////////

    /**
     * Returns the full writer for long / int64.
     */
    @Override
    public IHDF5LongWriter int64();

    /**
     * Returns the full writer for unsigned long / uint64.
     */
    @Override
    public IHDF5LongWriter uint64();

    // /////////////////////
    // Float
    // /////////////////////

    /**
     * Returns the full writer for float / float32.
     */
    @Override
    public IHDF5FloatWriter float32();

    // /////////////////////
    // Double
    // /////////////////////

    /**
     * Returns the full writer for long / float64.
     */
    @Override
    public IHDF5DoubleWriter float64();

    // /////////////////////
    // Enums
    // /////////////////////

    /**
     * Returns the full writer for enumerations.
     */
    @Override
    public IHDF5EnumWriter enumeration();

    // /////////////////////
    // Compounds
    // /////////////////////

    /**
     * Returns the full reader for compounds.
     */
    @Override
    public IHDF5CompoundWriter compound();

    // /////////////////////
    // Strings
    // /////////////////////

    /**
     * Returns the full writer for strings.
     */
    @Override
    public IHDF5StringWriter string();

    // /////////////////////
    // Date & Time
    // /////////////////////

    /**
     * Returns the full writer for date and times.
     */
    @Override
    public IHDF5DateTimeWriter time();

    /**
     * Returns the full writer for time durations.
     */
    @Override
    public IHDF5TimeDurationWriter duration();

    // /////////////////////
    // Object references
    // /////////////////////

    /**
     * Returns the full reader for object references.
     */
    @Override
    public IHDF5ReferenceWriter reference();

}
