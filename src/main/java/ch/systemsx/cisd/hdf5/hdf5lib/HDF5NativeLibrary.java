package ch.systemsx.cisd.hdf5.hdf5lib;

import java.io.IOException;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.bytedeco.hdf5.HDF5Plugins;
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
 * <p>
 * Enum constants that are initialized from {@code HDF5Constants} need the same guarantee, but they
 * are created before any static initializer of their enum runs, and their constructor arguments
 * are evaluated before their constructor. They read the constant through {@link #intConstant} or
 * {@link #longConstant} instead, which load the library first.
 * <p>
 * Right after the library is loaded, the filter plugins bundled with {@code org.bytedeco:hdf5}
 * (bzip2, lzf, blosc, blosc2, lz4, bitshuffle, zfp, zstd, ...) are extracted and registered through
 * {@link HDF5Plugins#load()}. They live in the natives jar's {@code lib/plugin} directory, which
 * JavaCPP does not extract on its own, so without this HDF5 cannot find them. {@code H5close()}
 * discards that registration again, so code that re-initializes the library (see
 * {@code HDF5.resetLibrary()}) has to call {@link #reloadPlugins()} afterwards.
 */
public final class HDF5NativeLibrary
{
    private static final String H5_PATH_PROPERTY = "hdf.hdf5lib.H5.hdf5lib";

    private static final String H5_NAME_PROPERTY = "hdf.hdf5lib.H5.loadLibraryName";

    private static boolean loaded = false;

    /** Whether {@link #load()} registered the bundled plugins (and so a reset has to redo it). */
    private static boolean bundledPlugins = false;

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
        if (isSet(H5_PATH_PROPERTY) == false && isSet(H5_NAME_PROPERTY) == false)
        {
            // Loader.load() returns null when JavaCPP's library loading is disabled.
            final String path = Loader.load(hdf5_java.class);
            if (path != null)
            {
                System.setProperty(H5_PATH_PROPERTY, path);
                bundledPlugins = true;
                loadPlugins();
            }
        }
        // Only now, so that a failed attempt (e.g. no natives for this platform) is retried and
        // reported again by the next caller, instead of surfacing later as an obscure
        // UnsatisfiedLinkError from H5's static initializer.
        loaded = true;
    }

    /**
     * Returns {@code constant.getAsInt()}, after {@link #load()}. For initializing enum constants
     * from {@code HDF5Constants}, e.g. {@code COMPACT(intConstant(() -> H5D_COMPACT))}.
     */
    public static int intConstant(IntSupplier constant)
    {
        load();
        return constant.getAsInt();
    }

    /**
     * Returns {@code constant.getAsLong()}, after {@link #load()}. See {@link #intConstant}.
     */
    public static long longConstant(LongSupplier constant)
    {
        load();
        return constant.getAsLong();
    }

    /**
     * Registers the bundled filter plugins again. {@code H5close()} resets HDF5's plugin search
     * path, so this has to run after every {@code H5open()} that follows one. Does nothing if
     * {@link #load()} did not register the plugins in the first place (user-chosen library).
     */
    public static synchronized void reloadPlugins()
    {
        if (bundledPlugins)
        {
            loadPlugins();
        }
    }

    /**
     * Registers the bundled filter plugins. They are optional: if they are missing for this
     * platform, files that use only HDF5's built-in filters still work, so this does not fail.
     * Filters that are needed and missing are reported by HDF5 when a dataset that uses them is
     * created or read.
     */
    private static void loadPlugins()
    {
        try
        {
            HDF5Plugins.load();
        } catch (IOException ex)
        {
            System.err.println("Warning: could not load the bundled HDF5 filter plugins: " + ex);
        }
    }

    private static boolean isSet(String key)
    {
        final String value = System.getProperty(key);
        return value != null && value.length() > 0;
    }
}
