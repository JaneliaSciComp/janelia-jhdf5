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
 * An interface for reading HDF5 files (HDF5 1.10.x and older).
 * <p>
 * Obtain an object implementing this interface by calling {@link HDF5Factory#openForReading(String)} 
 * or {@link IHDF5ReaderConfigurator#reader()}.
 * <p>
 * The interface focuses on ease of use instead of completeness. As a consequence not all features
 * of HDF5 are supported by this class, however it covers a large subset. In particular all
 * information written by {@link IHDF5Writer} can be read by this class.
 * <p>
 * The functionality is being made available in two ways:
 * <ol>
 * <li>{@link IHDF5SimpleReader} contains the most important methods in one interface. If you are
 * new to the library, this is a good starting point, see the example code below.</li>
 * <li>The hierarchical ("quasi-fluent") API provides the full functionality. It is designed along
 * the data types supported by JHDF5.
 * <ul>
 * <li>{@link #file()}: File-level information and operations, has e.g. the
 * {@link IHDF5FileLevelReadOnlyHandler#close()} method.</li>
 * <li>{@link #object()}: Object-level information, where "objects" can be data sets, links, groups
 * or data types, following the concept of an HDF5 object. Here you can find for example the method
 * {@link IHDF5ObjectReadOnlyInfoProviderHandler#getGroupMemberInformation(String, boolean)} which
 * gives you information on the members of a group and the method
 * {@link IHDF5ObjectReadOnlyInfoProviderHandler#tryGetSymbolicLinkTarget(String)} for resolving a
 * symbolic link.</li>
 * <li>{@link #bool()}: Reader methods for boolean data sets, including bit fields.</li>
 * <li>{@link #int8()} / {@link #int16()} / {@link #int16()} / {@link #int32()} / {@link #int64()}:
 * Reader methods for integer data sets, where the number as part of the method name denotes the
 * size of the integer type. The methods will always read signed integers, if you need unsigned
 * integers, you need to convert them with one of the methods in {@link UnsignedIntUtils}.</li>
 * <li>{@link #float32()} / {@link #float64()}: Reader methods for float data sets, where the number
 * as part of the name sets the size of the float type.</li>
 * <li>{@link #time()} / {@link #duration()}: Reader methods for time stamp (or date) and for time
 * duration data sets.</li>
 * <li>{@link #string()}: Reader methods for string data sets.</li>
 * <li>{@link #enumeration()}: Reader methods for enumeration data sets.</li>
 * <li>{@link #compound()}: Reader methods for compound data sets.</li>
 * <li>{@link #opaque()}: Reader methods for data sets that are "black boxes" to HDF5 which are
 * called "opaque data sets" in HDF5 jargon. Here you can also find methods of reading arbitrary
 * data sets as byte arrays.</li>
 * <li>{@link #reference()}: Reader methods for HDF5 object references. Note that object references,
 * though similar to hard links and symbolic links on the first glance, are quite different for
 * HDF5.</li>
 * </ul>
 * </li>
 * </ol>
 * <p>
 * Usage example for {@link IHDF5SimpleReader}:
 * 
 * <pre>
 * IHDF5Reader reader = HDF5FactoryProvider.get().openForReading(new File(&quot;test.h5&quot;));
 * float[] f = reader.readFloatArray(&quot;/some/path/dataset&quot;);
 * String s = reader.getStringAttribute(&quot;/some/path/dataset&quot;, &quot;some key&quot;);
 * reader.close();
 * </pre>
 * 
 * @author Bernd Rinn
 */
public interface IHDF5Reader extends IHDF5SimpleReader
{

    // /////////////////////
    // File
    // /////////////////////

    /**
     * Returns the handler for file-level information and status.
     */
    public IHDF5FileLevelReadOnlyHandler file();

    // /////////////////////////////////
    // Objects, links, groups and types
    // /////////////////////////////////

    /**
     * Returns an info provider for HDF5 objects like links, groups, data sets and data types.
     */
    public IHDF5ObjectReadOnlyInfoProviderHandler object();

    // /////////////////////
    // Opaque
    // /////////////////////

    /**
     * Returns the full reader for reading data sets and attributes as byte arrays ('opaque') and
     * obtaining opaque types.
     */
    public IHDF5OpaqueReader opaque();

    // /////////////////////
    // Boolean
    // /////////////////////

    /**
     * Returns the full reader for boolean values.
     */
    public IHDF5BooleanReader bool();

    // /////////////////////
    // Bytes
    // /////////////////////

    /**
     * Returns the full reader for byte / int8.
     */
    public IHDF5ByteReader int8();

    /**
     * Returns the full reader for unsigned byte / uint8.
     */
    public IHDF5ByteReader uint8();

    // /////////////////////
    // Short
    // /////////////////////

    /**
     * Returns the full reader for short / int16.
     */
    public IHDF5ShortReader int16();

    /**
     * Returns the full reader for unsigned short / uint16.
     */
    public IHDF5ShortReader uint16();

    // /////////////////////
    // Int
    // /////////////////////

    /**
     * Returns the full reader for int / int32.
     */
    public IHDF5IntReader int32();

    /**
     * Returns the full reader for unsigned int / uint32.
     */
    public IHDF5IntReader uint32();

    // /////////////////////
    // Long
    // /////////////////////

    /**
     * Returns the full reader for long / int64.
     */
    public IHDF5LongReader int64();

    /**
     * Returns the full reader for unsigned long / uint64.
     */
    public IHDF5LongReader uint64();

    // /////////////////////
    // Float
    // /////////////////////

    /**
     * Returns the full reader for float / float32.
     */
    public IHDF5FloatReader float32();

    // /////////////////////
    // Double
    // /////////////////////

    /**
     * Returns the full reader for long / float64.
     */
    public IHDF5DoubleReader float64();

    // /////////////////////
    // Enums
    // /////////////////////

    /**
     * Returns the full reader for enumerations.
     */
    public IHDF5EnumReader enumeration();

    // /////////////////////
    // Compounds
    // /////////////////////

    /**
     * Returns the full reader for compounds.
     */
    public IHDF5CompoundReader compound();

    // /////////////////////
    // Strings
    // /////////////////////

    /**
     * Returns the full reader for strings.
     */
    public IHDF5StringReader string();

    // /////////////////////
    // Date & Time
    // /////////////////////

    /**
     * Returns the full reader for date and times.
     */
    public IHDF5DateTimeReader time();

    /**
     * Returns the full reader for time durations.
     */
    public IHDF5TimeDurationReader duration();

    // /////////////////////
    // Object references
    // /////////////////////

    /**
     * Returns the full reader for object references.
     */
    public IHDF5ReferenceReader reference();

}
