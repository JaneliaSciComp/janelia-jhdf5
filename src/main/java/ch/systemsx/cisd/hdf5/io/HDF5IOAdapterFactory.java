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

package ch.systemsx.cisd.hdf5.io;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import ch.systemsx.cisd.base.io.AdapterIInputStreamToInputStream;
import ch.systemsx.cisd.base.io.AdapterIOutputStreamToOutputStream;
import ch.systemsx.cisd.base.io.IInputStream;
import ch.systemsx.cisd.base.io.IOutputStream;
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

/**
 * A factory of I/O adapter for HDF5 data sets.
 * 
 * @author Bernd Rinn
 */
public class HDF5IOAdapterFactory
{

    private static final String OPAQUE_TAG_FILE = "FILE";

    private final static int BUFFER_SIZE = 1024 * 1024;

    //
    // File methods
    //

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link IOutputStream}.
     * <p>
     * If the dataset does not yet exist, it will create a chunked opaque dataset with a chunk size
     * of 1MB and an opaque tag <code>FILE</code>.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link IOutputStream}.
     */
    public static IOutputStream asIOutputStream(File hdf5File, String dataSetPath)
    {
        return new HDF5DataSetRandomAccessFile(hdf5File, dataSetPath,
                HDF5GenericStorageFeatures.GENERIC_CHUNKED, BUFFER_SIZE, OPAQUE_TAG_FILE, false);
    }

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link IOutputStream}.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @param creationStorageFeature If the dataset does not yet exist, use this value as the
     *            storage features when creating it.
     * @param chunkSize If the dataset does not yet exist, use this value as the chunk size.
     * @param opaqueTagOrNull If the dataset does not yet exist and this value is not
     *            <code>null</code>, then an opaque dataset will be created using this value will be
     *            used as opaque tag.
     * @return The {@link IOutputStream}.
     */
    public static IOutputStream asIOutputStream(File hdf5File, String dataSetPath,
            HDF5GenericStorageFeatures creationStorageFeature, int chunkSize, String opaqueTagOrNull)
    {
        return new HDF5DataSetRandomAccessFile(hdf5File, dataSetPath, creationStorageFeature,
                chunkSize, opaqueTagOrNull, false);
    }

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link HDF5DataSetRandomAccessFile} in
     * read/write mode.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link HDF5DataSetRandomAccessFile}.
     */
    public static HDF5DataSetRandomAccessFile asRandomAccessFileReadWrite(File hdf5File,
            String dataSetPath)
    {
        return new HDF5DataSetRandomAccessFile(hdf5File, dataSetPath,
                HDF5GenericStorageFeatures.GENERIC_CHUNKED, BUFFER_SIZE, OPAQUE_TAG_FILE, false);
    }

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link OutputStream}.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @param creationStorageFeature If the dataset does not yet exist, use this value as the
     *            storage features when creating it.
     * @param chunkSize If the dataset does not yet exist, use this value as the chunk size.
     * @param opaqueTagOrNull If the dataset does not yet exist and this value is not
     *            <code>null</code>, then an opaque dataset will be created using this value will be
     *            used as opaque tag.
     * @return The {@link OutputStream}.
     */
    public static OutputStream asOutputStream(File hdf5File, String dataSetPath,
            HDF5GenericStorageFeatures creationStorageFeature, int chunkSize, String opaqueTagOrNull)
    {
        return new AdapterIOutputStreamToOutputStream(asIOutputStream(hdf5File, dataSetPath,
                creationStorageFeature, chunkSize, opaqueTagOrNull));
    }

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link IOutputStream}.
     * <p>
     * If the dataset does not yet exist, it will create a chunked opaque dataset with a chunk size
     * of 1MB and an opaque tag <code>FILE</code>.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link OutputStream}.
     */
    public static OutputStream asOutputStream(File hdf5File, String dataSetPath)
    {
        return new AdapterIOutputStreamToOutputStream(asIOutputStream(hdf5File, dataSetPath,
                HDF5GenericStorageFeatures.GENERIC_CHUNKED, BUFFER_SIZE, OPAQUE_TAG_FILE));
    }

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link HDF5DataSetRandomAccessFile} in
     * read/write mode.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @param creationStorageFeature If the dataset does not yet exist, use this value as the
     *            storage features when creating it.
     * @param chunkSize If the dataset does not yet exist, use this value as the chunk size.
     * @param opaqueTagOrNull If the dataset does not yet exist and this value is not
     *            <code>null</code>, then an opaque dataset will be created using this value will be
     *            used as opaque tag.
     * @return The {@link HDF5DataSetRandomAccessFile}.
     */
    public static HDF5DataSetRandomAccessFile asRandomAccessFile(File hdf5File, String dataSetPath,
            HDF5GenericStorageFeatures creationStorageFeature, int chunkSize, String opaqueTagOrNull)
    {
        return new HDF5DataSetRandomAccessFile(hdf5File, dataSetPath, creationStorageFeature,
                chunkSize, opaqueTagOrNull, false);
    }

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link IInputStream}.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link IInputStream}.
     */
    public static IInputStream asIInputStream(File hdf5File, String dataSetPath)
    {
        return new HDF5DataSetRandomAccessFile(hdf5File, dataSetPath, null, 0, null, true);
    }

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link InputStream}.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link InputStream}.
     */
    public static InputStream asInputStream(File hdf5File, String dataSetPath)
    {
        return new AdapterIInputStreamToInputStream(asIInputStream(hdf5File, dataSetPath));
    }

    /**
     * Creates an adapter of the <var>hdf5File</var> as an {@link HDF5DataSetRandomAccessFile} in
     * read-only mode.
     * 
     * @param hdf5File The HDF5 file to create the adapter for.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link HDF5DataSetRandomAccessFile}.
     */
    public static HDF5DataSetRandomAccessFile asRandomAccessFileReadOnly(File hdf5File,
            String dataSetPath)
    {
        return new HDF5DataSetRandomAccessFile(hdf5File, dataSetPath, null, 0, null, true);
    }

    //
    // Writer methods
    //

    /**
     * Creates an adapter of the <var>writer</var> as an {@link IOutputStream}.
     * <p>
     * If the dataset does not yet exist, it will create a chunked opaque dataset with a chunk size
     * of 1MB and an opaque tag <code>FILE</code>.
     * <p>
     * <b>Note that returned object is buffered. Do not access <var>dataSetPath</var> by directly
     * accessing <var>writer</var> while this object is used or else the behavior is undefined!</b>
     * 
     * @param writer The HDF5 writer to create the adapter for. The writer will <i>not be closed
     *            when the returned object is closed.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link IOutputStream}.
     */
    public static IOutputStream asIOutputStream(IHDF5Writer writer, String dataSetPath)
    {
        return new HDF5DataSetRandomAccessFile(writer, dataSetPath,
                HDF5GenericStorageFeatures.GENERIC_CHUNKED, BUFFER_SIZE, OPAQUE_TAG_FILE, false);
    }

    /**
     * Creates an adapter of the <var>writer</var> as an {@link IOutputStream}.
     * <p>
     * <b>Note that returned object is buffered. Do not access <var>dataSetPath</var> by directly
     * accessing <var>writer</var> while this object is used or else the behavior is undefined!</b>
     * 
     * @param writer The HDF5 writer to create the adapter for. The writer will <i>not be closed
     *            when the returned object is closed.

     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @param creationStorageFeature If the dataset does not yet exist, use this value as the
     *            storage features when creating it.
     * @param chunkSize If the dataset does not yet exist, use this value as the chunk size.
     * @param opaqueTagOrNull If the dataset does not yet exist and this value is not
     *            <code>null</code>, then an opaque dataset will be created using this value will be
     *            used as opaque tag.
     * @return The {@link IOutputStream}.
     */
    public static IOutputStream asIOutputStream(IHDF5Writer writer, String dataSetPath,
            HDF5GenericStorageFeatures creationStorageFeature, int chunkSize, String opaqueTagOrNull)
    {
        return new HDF5DataSetRandomAccessFile(writer, dataSetPath, creationStorageFeature,
                chunkSize, opaqueTagOrNull, false);
    }

    /**
     * Creates an adapter of the <var>writer</var> as an {@link IOutputStream}.
     * <p>
     * If the dataset does not yet exist, it will create a chunked opaque dataset with a chunk size
     * of 1MB and an opaque tag <code>FILE</code>.
     * <p>
     * <b>Note that returned object is buffered. Do not access <var>dataSetPath</var> by directly
     * accessing <var>writer</var> while this object is used or else the behavior is undefined!</b>
     * 
     * @param writer The HDF5 writer to create the adapter for. The writer will <i>not be closed
     *            when the returned object is closed.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link OutputStream}.
     */
    public static OutputStream asOutputStream(IHDF5Writer writer, String dataSetPath)
    {
        return new AdapterIOutputStreamToOutputStream(asIOutputStream(writer, dataSetPath,
                HDF5GenericStorageFeatures.GENERIC_CHUNKED, BUFFER_SIZE, OPAQUE_TAG_FILE));
    }

    /**
     * Creates an adapter of the <var>writer</var> as an {@link OutputStream}.
     * <p>
     * <b>Note that returned object is buffered. Do not access <var>dataSetPath</var> by directly
     * accessing <var>writer</var> while this object is used or else the behavior is undefined!</b>
     * 
     * @param writer The HDF5 writer to create the adapter for. The writer will <i>not be closed
     *            when the returned object is closed.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @param creationStorageFeature If the dataset does not yet exist, use this value as the
     *            storage features when creating it.
     * @param chunkSize If the dataset does not yet exist, use this value as the chunk size.
     * @param opaqueTagOrNull If the dataset does not yet exist and this value is not
     *            <code>null</code>, then an opaque dataset will be created using this value will be
     *            used as opaque tag.
     * @return The {@link OutputStream}.
     */
    public static OutputStream asOutputStream(IHDF5Writer writer, String dataSetPath,
            HDF5GenericStorageFeatures creationStorageFeature, int chunkSize, String opaqueTagOrNull)
    {
        return new AdapterIOutputStreamToOutputStream(asIOutputStream(writer, dataSetPath,
                creationStorageFeature, chunkSize, opaqueTagOrNull));
    }

    /**
     * Creates an adapter of the <var>writer</var> as an {@link HDF5DataSetRandomAccessFile}.
     * <p>
     * <b>Note that returned object is buffered. Do not access <var>dataSetPath</var> by directly
     * accessing <var>writer</var> while this object is used or else the behavior is undefined!</b>
     * 
     * @param writer The HDF5 writer to create the adapter for. The writer will <i>not be closed
     *            when the returned object is closed.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @param creationStorageFeature If the dataset does not yet exist, use this value as the
     *            storage features when creating it.
     * @param chunkSize If the dataset does not yet exist, use this value as the chunk size.
     * @param opaqueTagOrNull If the dataset does not yet exist and this value is not
     *            <code>null</code>, then an opaque dataset will be created using this value will be
     *            used as opaque tag.
     * @return The {@link HDF5DataSetRandomAccessFile}.
     */
    public static HDF5DataSetRandomAccessFile asRandomAccessFile(IHDF5Writer writer,
            String dataSetPath, HDF5GenericStorageFeatures creationStorageFeature, int chunkSize,
            String opaqueTagOrNull)
    {
        return new HDF5DataSetRandomAccessFile(writer, dataSetPath, creationStorageFeature,
                chunkSize, opaqueTagOrNull, false);
    }

    /**
     * Creates an adapter of the <var>writer</var> as an {@link HDF5DataSetRandomAccessFile}.
     * <p>
     * <b>Note that returned object is buffered. Do not access <var>dataSetPath</var> by directly
     * accessing <var>writer</var> while this object is used or else the behavior is undefined!</b>
     * 
     * @param writer The HDF5 writer to create the adapter for. The writer will <i>not be closed
     *            when the returned object is closed.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link HDF5DataSetRandomAccessFile}.
     */
    public static HDF5DataSetRandomAccessFile asRandomAccessFile(IHDF5Writer writer,
            String dataSetPath)
    {
        return new HDF5DataSetRandomAccessFile(writer, dataSetPath,
                HDF5GenericStorageFeatures.GENERIC_CHUNKED, BUFFER_SIZE, OPAQUE_TAG_FILE, false);
    }

    //
    // Reader methods
    //

    /**
     * Creates an adapter of the <var>reader</var> as an {@link IInputStream}.
     * 
     * @param reader The HDF5 reader to create the adapter for. The reader will <i>not be closed
     *            when the returned object is closed.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link IInputStream}.
     */
    public static IInputStream asIInputStream(IHDF5Reader reader, String dataSetPath)
    {
        return new HDF5DataSetRandomAccessFile(reader, dataSetPath, null, 0, null, false);
    }

    /**
     * Creates an adapter of the <var>reader</var> as an {@link InputStream}.
     * 
     * @param reader The HDF5 reader to create the adapter for. The reader will <i>not be closed
     *            when the returned object is closed.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link InputStream}.
     */
    public static InputStream asInputStream(IHDF5Reader reader, String dataSetPath)
    {
        return new AdapterIInputStreamToInputStream(asIInputStream(reader, dataSetPath));
    }

    /**
     * Creates an adapter of the <var>reader</var> as an {@link HDF5DataSetRandomAccessFile}.
     * 
     * @param reader The HDF5 reader to create the adapter for. The reader will <i>not be closed
     *            when the returned object is closed.
     * @param dataSetPath The path of the HDF5 dataset in the HDF5 container to use as a file.
     * @return The {@link HDF5DataSetRandomAccessFile}.
     */
    public static HDF5DataSetRandomAccessFile asRandomAccessFile(IHDF5Reader reader,
            String dataSetPath)
    {
        return new HDF5DataSetRandomAccessFile(reader, dataSetPath, null, 0, null, false);
    }

}
