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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract of all classes providing build and environment information.
 * <p>
 * Does <em>not</em> depend on any library jar files.
 * </p>
 * 
 * @author Franz-Josef Elmer
 */
public abstract class AbstractBuildAndEnvironmentInfo
{
    private static final String UNKNOWN = "UNKNOWN";

    private final String version;

    private final String revision;

    private final boolean cleanSources;

    private final String applicationName;

    protected AbstractBuildAndEnvironmentInfo(String applicationName)
    {
        this.applicationName = applicationName;
        String extractedVersion = UNKNOWN;
        String extractedRevision = UNKNOWN;
        boolean extractedCleanFlag = false;
        final InputStream stream =
                AbstractBuildAndEnvironmentInfo.class.getResourceAsStream("/BUILD-" + applicationName
                        + ".INFO");
        if (stream != null)
        {
            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new InputStreamReader(stream));
                final String line = reader.readLine();
                if (line != null)
                {
                    final String[] rev = line.split("::");
                    if (rev.length == 3)
                    {
                        extractedVersion = rev[0];
                        extractedRevision = rev[1];
                        extractedCleanFlag = "clean".equals(rev[2]);
                    } else
                    {
                        // Backward compatibility: test for the old format.
                        final String[] rev2 = line.split(":");
                        if (rev2.length == 3)
                        {
                            extractedVersion = rev2[0];
                            extractedRevision = rev2[1];
                            extractedCleanFlag = "clean".equals(rev2[2]);
                        } else
                        {
                            extractedVersion = line;
                            extractedRevision = "?";
                            extractedCleanFlag = false;
                        }
                    }
                }
            } catch (IOException ex)
            {
                // ignored
            } finally
            {
                try
                {
                    if (reader != null)
                    {
                        reader.close();
                    }
                } catch (IOException ioe)
                {
                    // ignore
                }
            }
        }
        this.version = extractedVersion;
        this.revision = extractedRevision;
        this.cleanSources = extractedCleanFlag;
    }

    private final static String getProperty(final String property)
    {
        return System.getProperty(property, UNKNOWN);
    }

    private final static boolean isUnknown(final String property)
    {
        return property.equals(UNKNOWN);
    }

    /**
     * @return Name of the CPU architecture.
     */
    public final String getCPUArchitecture()
    {
        return getProperty("os.arch");
    }

    /**
     * @return Name and version of the operating system.
     */
    public final String getOS()
    {
        final String osName = getProperty("os.name");
        final String osVersion = getProperty("os.version");
        if (isUnknown(osName) || isUnknown(osVersion))
        {
            return osName;
        }
        return osName + " (v" + osVersion + ")";
    }

    /**
     * @return Name and version of the Java Virtual Machine.
     */
    public final String getJavaVM()
    {
        final String vmName = getProperty("java.vm.name");
        final String vmVersion = getProperty("java.vm.version");
        if (isUnknown(vmName) || isUnknown(vmVersion))
        {
            return vmName;
        }
        return vmName + " (v" + vmVersion + ")";
    }

    /**
     * @return The version of the software.
     */
    public final String getVersion()
    {
        return version;
    }

    /**
     * @return <code>true</code> if the versioned entities of the working copy have been clean when
     *         this build has been made, in other words, whether the revision given by
     *         {@link #getRevision()} does really identify the source that is build has been
     *         produced from.
     */
    public final boolean isCleanSources()
    {
        return cleanSources;
    }

    /**
     * @return The revision number.
     */
    public final String getRevision()
    {
        return revision;
    }

    /**
     * Returns the version accompanied by the build number of the software (if known).
     */
    public final String getFullVersion()
    {
        final StringBuilder builder = new StringBuilder();
        final String rev = getRevision();
        final boolean isDirty = isCleanSources() == false;
        builder.append(getVersion());
        if (isUnknown(rev) == false)
        {
            builder.append(" (").append(rev);
            if (isDirty)
            {
                builder.append("*");
            }
            builder.append(")");
        } else
        {
            if (isDirty)
            {
                builder.append("*");
            }
        }
        return builder.toString();
    }

    public String getApplicationName()
    {
        return applicationName;
    }

    /**
     * Returns version, build number, Java VM, and OS as a {@link List} with four entries.
     */
    public final List<String> getEnvironmentInfo()
    {
        final List<String> environmentInfo = new ArrayList<String>();
        environmentInfo.add("Application: " + getApplicationName());
        environmentInfo.add("Version: " + getFullVersion());
        environmentInfo.add("Java VM: " + getJavaVM());
        environmentInfo.add("CPU Architecture: " + getCPUArchitecture());
        environmentInfo.add("OS: " + getOS());
        return environmentInfo;
    }

    /**
     * Returns version, build number, Java VM, and OS in a four-liner as one {@link String}.
     */
    @Override
    public final String toString()
    {
        final StringBuilder builder = new StringBuilder();
        final List<String> environmentInfo = getEnvironmentInfo();
        final int n = environmentInfo.size();
        for (int i = 0; i < n; i++)
        {
            builder.append(environmentInfo.get(i));
            if (i < n - 1)
            {
                builder.append(System.getProperty("line.separator"));
            }
        }
        return builder.toString();
    }

}
