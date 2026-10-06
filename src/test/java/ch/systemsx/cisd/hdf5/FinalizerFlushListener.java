package ch.systemsx.cisd.hdf5;

import org.testng.IClassListener;
import org.testng.ITestClass;

/**
 * Forces pending finalizers to run to completion after every test class, synchronously on the
 * main test thread.
 * <p>
 * HDF5 is built here without thread-safety support (the default), so any HDF5Reader/Writer
 * instance left for the GC to clean up via {@code finalize()} is a hazard: if the JVM's own
 * Finalizer thread happens to run that cleanup (closing native HDF5 handles) while the main test
 * thread is concurrently making its own HDF5 calls, both threads are inside non-thread-safe
 * libhdf5 at once, which corrupts its internal state and crashes the JVM natively (observed:
 * SIGBUS inside H5CX_pop, HDF5's internal per-call context stack, invoked from the Finalizer
 * thread). Running the accumulated backlog of finalizers to completion between test classes,
 * while the main thread is idle and not itself touching HDF5, removes the race.
 */
public class FinalizerFlushListener implements IClassListener
{
    @Override
    public void onAfterClass(ITestClass testClass)
    {
        System.gc();
        System.runFinalization();
    }
}
