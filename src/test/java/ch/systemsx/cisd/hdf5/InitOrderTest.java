package ch.systemsx.cisd.hdf5;

import static org.testng.AssertJUnit.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Checks that HDF5 keeps working no matter which class of this library a program touches first.
 * <p>
 * Several public enums initialize their constants from {@code hdf.hdf5lib.HDF5Constants}. If one
 * of them is the first HDF5-related class a program uses, {@code HDF5Constants} initializes
 * {@code hdf.hdf5lib.H5} before {@link ch.systemsx.cisd.hdf5.hdf5lib.HDF5NativeLibrary} has
 * loaded the native library. {@code H5}'s class initialization then fails, which Java never
 * retries, so every later HDF5 call in the process fails too. Since class initialization happens
 * once per JVM, each case runs in a child JVM ({@link InitOrderCheck}).
 */
public class InitOrderTest
{
    @DataProvider
    public Object[][] firstClasses()
    {
        return new Object[][]
            {
                { CharacterEncoding.class.getName() },
                { HDF5DataClass.class.getName() },
                { HDF5StorageLayout.class.getName() },
                { HDF5EnumerationType.EnumStorageForm.class.getName() },
                { IHDF5WriterConfigurator.FileFormatVersion.class.getName() },
                { IHDF5WriterConfigurator.FileFormatVersionBounds.class.getName() },
                { HDF5IntStorageFeatures.class.getName() },
                { HDF5FloatStorageFeatures.class.getName() },
                { HDF5GenericStorageFeatures.class.getName() },
                // Control: the normal entry point.
                { HDF5Factory.class.getName() },
            };
    }

    @Test(dataProvider = "firstClasses")
    public void testHDF5WorksWhenClassIsTouchedFirst(String className) throws Exception
    {
        final List<String> command = new ArrayList<>();
        final boolean windows = System.getProperty("os.name", "").startsWith("Windows");
        command.add(new File(new File(System.getProperty("java.home"), "bin"),
                windows ? "java.exe" : "java").getPath());
        command.add("-cp");
        // Under Surefire this is its booter jar, whose manifest Class-Path holds the real class
        // path, so the child JVM sees the same classes as this one.
        command.add(System.getProperty("java.class.path"));
        command.add(InitOrderCheck.class.getName());
        command.add(className);
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final String output = readFully(process.getInputStream());
        final int exitCode = process.waitFor();
        assertEquals("Touching " + className + " first broke HDF5:\n" + output, 0, exitCode);
    }

    private static String readFully(InputStream in) throws Exception
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) >= 0)
        {
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
