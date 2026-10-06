package ch.systemsx.cisd.hdf5.hdf5lib;

import org.bytedeco.hdf5.hdf5_java;
import org.bytedeco.javacpp.Loader;

/**
 * Makes the official {@code hdf.hdf5lib.H5} class find its JNI library.
 * <p>
 * {@code H5}'s static initializer looks for {@code hdf5_java} only via its own system properties or
 * {@code System.loadLibrary} on {@code java.library.path}. With {@code org.bytedeco:hdf5} that
 * library is bundled inside the natives jar instead, and only JavaCPP's {@link Loader} knows how to
 * extract it (together with the libhdf5 it links against). Loader.load's own extraction does not
 * reliably make a later System.loadLibrary call find it (java.library.path is effectively cached by
 * the JVM the first time it's consulted), so this resolves the extracted library's absolute path
 * and points {@code H5} at it via its documented {@code hdf.hdf5lib.H5.hdf5lib} property.
 * <p>
 * {@link #load()} has to run before anything initializes {@code H5} or {@code HDF5Constants}; the
 * public entry points ({@code HDF5Factory}, {@code HDF5FactoryProvider}, {@link HDFHelper}) call it
 * from their static initializers. Code that touches {@code hdf.hdf5lib.H5} directly before any of
 * those should call {@link #load()} itself first.
 */
public final class HDF5NativeLibrary
{
    private static final String H5_PATH_PROPERTY = "hdf.hdf5lib.H5.hdf5lib";

    private static final String H5_NAME_PROPERTY = "hdf.hdf5lib.H5.loadLibraryName";

    private static boolean loaded = false;

    private HDF5NativeLibrary()
    {
        // Not to be instantiated.
    }

    /**
     * Extracts and loads the bundled HDF5 JNI library and points {@code hdf.hdf5lib.H5} at it.
     * Does nothing if already done, or if the user has chosen a library themselves via either of
     * {@code H5}'s own system properties.
     */
    public static synchronized void load()
    {
        if (loaded)
        {
            return;
        }
        loaded = true;
        if (isSet(H5_PATH_PROPERTY) || isSet(H5_NAME_PROPERTY))
        {
            return;
        }
        System.setProperty(H5_PATH_PROPERTY, Loader.load(hdf5_java.class));
    }

    private static boolean isSet(String key)
    {
        final String value = System.getProperty(key);
        return value != null && value.length() > 0;
    }
}
