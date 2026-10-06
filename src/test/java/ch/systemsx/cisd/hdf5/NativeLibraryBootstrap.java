package ch.systemsx.cisd.hdf5;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.hdf5.hdf5_java;
import org.testng.annotations.BeforeSuite;

/**
 * Loads the native HDF5 JNI library before any test runs.
 * <p>
 * The upstream test suite this package was imported from predates this project's switch to
 * org.bytedeco:hdf5 and never needed to call {@link Loader#load} itself: the official
 * {@code hdf.hdf5lib.H5} class simply called {@code System.loadLibrary("hdf5_java")} and relied on
 * it already being on {@code java.library.path}. Now the native library is bundled inside the
 * org.bytedeco:hdf5 jar instead, extracted on demand by {@link Loader}. Loader.load's own
 * extraction does not reliably make a later System.loadLibrary call find it (java.library.path is
 * effectively cached by the JVM the first time it's consulted), so this resolves the extracted
 * library's absolute path and points hdf.hdf5lib.H5 at it directly via its own documented escape
 * hatch instead.
 */
public class NativeLibraryBootstrap
{
    @BeforeSuite(alwaysRun = true)
    public static void loadNativeLibrary()
    {
        String path = Loader.load(hdf5_java.class);
        System.setProperty("hdf.hdf5lib.H5.hdf5lib", path);
    }
}
