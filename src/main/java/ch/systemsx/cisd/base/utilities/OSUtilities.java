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

package ch.systemsx.cisd.base.utilities;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Some useful methods related to the operating system.
 * <p>
 * Does <em>not</em> depend on any library jar files. But before using or extending this class and
 * if you do not mind using <a href="http://jakarta.apache.org/commons/lang/">commons lang</a>, then
 * have a look on <code>SystemUtils</code>.
 * </p>
 * 
 * @author Bernd Rinn
 */
public class OSUtilities
{

    /** Platform specific line separator. */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * @return <code>true</code> if the operating system is UNIX like.
     */
    public static boolean isUnix()
    {
        return (File.separatorChar == '/');
    }

    /**
     * @return <code>true</code> if the operating system is a MS Windows type.
     */
    public static boolean isWindows()
    {
        return (File.separatorChar == '\\');
    }

    /**
     * @return <code>true</code> if the the operating system is a flavor of Mac OS X.
     */
    public static boolean isMacOS()
    {
        return "Mac OS X".equals(System.getProperty("os.name"));
    }

    /**
     * @return The name of the computer platform that is compatible with respect to executables (CPU
     *         architecture and OS name, both as precise as possible to be able to share libraries
     *         and binaries).
     */
    public static String getCompatibleComputerPlatform()
    {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows"))
        {
            osName = "Windows";
        }
        return System.getProperty("os.arch") + "-" + osName;
    }

    /**
     * @return The name of the CPU architecture.
     */
    public static String getCPUArchitecture()
    {
        return System.getProperty("os.arch");
    }

    /**
     * @return The name of the operating system.
     */
    public static String getOSName()
    {
        return System.getProperty("os.name");
    }

    /**
     * @return The name of the computer platform (CPU architecture and OS name).
     */
    public static String getComputerPlatform()
    {
        return System.getProperty("os.arch") + "-" + System.getProperty("os.name");
    }

    /**
     * @return The name of user that runs this program.
     */
    public static String getUsername()
    {
        return System.getProperty("user.name");
    }

    /**
     * @return <code>true</code> if the user that runs this program is known to have root privileges
     *         (based on his name).
     */
    public static boolean isRoot()
    {
        if (isUnix())
        {
            return "root".equals(getUsername());
        } else
        {
            return "Administrator".equals(getUsername());
        }
    }

    /**
     * @return The <var>PATH</var> as provided by the operating system.
     */
    public static Set<String> getOSPath()
    {
        final String[] pathEntries =
                System.getenv("PATH").split(Pattern.quote(System.getProperty("path.separator")));
        return new LinkedHashSet<String>(Arrays.asList(pathEntries));
    }

    /**
     * @param root Whether the path should be prepared for root or not.
     * @return The path as provided by the operating system plus some path entries that should
     *         always be available.
     * @see #getOSPath()
     */
    public static Set<String> getSafeOSPath(boolean root)
    {
        final Set<String> pathEntries = getOSPath();
        if (isUnix())
        {
            if (isMacOS())
            {
                pathEntries.add("/opt/local/bin"); // MacPorts
                pathEntries.add("/sw/bin"); // Fink
                if (root)
                {
                    pathEntries.add("/opt/local/sbin");
                    pathEntries.add("/sw/sbin");
                }
            }
            pathEntries.add("/usr/local/bin");
            pathEntries.add("/usr/bin");
            pathEntries.add("/bin");
            if (root)
            {
                pathEntries.add("/usr/local/sbin");
                pathEntries.add("/usr/sbin");
                pathEntries.add("/sbin");
            }
        }
        return pathEntries;
    }

    /**
     * Convenience method for {@link #getSafeOSPath(boolean)} with <code>root=false</code>.
     * 
     * @return The path as provided by the operating system plus some path entries that should
     *         always be available.
     * @see #getSafeOSPath(boolean)
     */
    public static Set<String> getSafeOSPath()
    {
        return getSafeOSPath(false);
    }

    /**
     * Search for the binary program with name <code>binaryName</code> in the operating system
     * path..
     * 
     * @param executableName The name of the executable to search for. Under Windows, a name with
     *            and without <code>.exe</code> appended will work, but the executable found needs
     *            to have the .exe extension.
     * @return The binary file that has been found in the path, or <code>null</code>, if no binary
     *         file could be found.
     */
    public static File findExecutable(String executableName)
    {
        return OSUtilities.findExecutable(executableName, getSafeOSPath());
    }

    /**
     * Search for the binary program with name <code>binaryName</code> in the set of paths denoted
     * by <code>pathSet</code>.
     * 
     * @param executableName The name of the executable to search for. Under Windows, a name with
     *            and without <code>.exe</code> appended will work, but the executable found needs
     *            to have the .exe extension.
     * @param pathSet The set of paths to search for. It is recommended to use an ordered set like
     *            the {@link LinkedHashSet} here in order to get results that are independent of the
     *            JRE implementation.
     * @return The binary file that has been found in the path, or <code>null</code>, if no binary
     *         file could be found.
     */
    public static File findExecutable(String executableName, Set<String> pathSet)
    {
        final String executableNameWithExtension =
                addWindowsExecutableExtensionIfNecessary(executableName);
        for (String dir : pathSet)
        {
            final File fileToCheck = new File(dir, executableNameWithExtension);
            if (fileToCheck.exists())
            {
                return fileToCheck;
            }
        }
        return null;
    }

    /**
     * @return <code>true</code> if and only if an executable of name <var>executableName</var>
     *         exists.
     */
    public static boolean executableExists(String executableName)
    {
        return (new File(OSUtilities.addWindowsExecutableExtensionIfNecessary(executableName)))
                .exists();
    }

    /**
     * @return <code>true</code> if and only if an executable of name <var>executableName</var>
     *         exists.
     */
    public static boolean executableExists(File executable)
    {
        return (new File(OSUtilities.addWindowsExecutableExtensionIfNecessary(executable.getPath())))
                .exists();
    }

    private static String addWindowsExecutableExtensionIfNecessary(String executableName)
    {
        if (isWindows() && executableName.indexOf('.') < 0)
        {
            return executableName + ".exe";
        } else
        {
            return executableName;
        }
    }

}
