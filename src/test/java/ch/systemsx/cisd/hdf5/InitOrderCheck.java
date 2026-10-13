package ch.systemsx.cisd.hdf5;

import java.io.File;

/**
 * Child program of {@link InitOrderTest}: initializes the class named by the first argument,
 * then writes and reads an HDF5 file through {@link HDF5Factory}. Exits with 0 on success, 1 on
 * failure, printing the failure to stderr.
 * <p>
 * Run in a fresh JVM, because class initialization happens once per JVM and this is about
 * what happens when the named class is the <em>first</em> HDF5-related class touched.
 */
public class InitOrderCheck
{
    public static void main(String[] args)
    {
        try
        {
            Class.forName(args[0], true, InitOrderCheck.class.getClassLoader());
            final File file = File.createTempFile("init-order-", ".h5");
            try
            {
                try (IHDF5Writer writer = HDF5Factory.open(file))
                {
                    writer.int32().write("x", 42);
                }
                try (IHDF5Reader reader = HDF5Factory.openForReading(file))
                {
                    if (reader.int32().read("x") != 42)
                    {
                        throw new IllegalStateException("read back wrong value");
                    }
                }
            } finally
            {
                file.delete();
            }
            System.exit(0);
        } catch (Throwable t)
        {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
