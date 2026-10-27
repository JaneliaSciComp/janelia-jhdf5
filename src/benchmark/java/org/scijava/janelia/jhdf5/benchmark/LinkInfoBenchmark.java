/*
 * Copyright 2026 Howard Hughes Medical Institute
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

package org.scijava.janelia.jhdf5.benchmark;

import static hdf.hdf5lib.HDF5Constants.H5F_ACC_TRUNC;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import ch.systemsx.cisd.hdf5.hdf5lib.HDF5NativeLibrary;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;
import hdf.hdf5lib.H5;

/**
 * The link-listing helpers of {@link HDFHelper}, which used to be native: getting the names and
 * types (and soft-link targets) of all members of a group. This is what listing a group through
 * {@code IHDF5Reader.object().getGroupMemberInformation()} costs. The group holds
 * {@code links} members, alternating hard links (sub-groups) and soft links.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class LinkInfoBenchmark
{
    @Param({ "10", "1000", "10000" })
    public int links;

    private File file;

    private long fileId = -1;

    private String[] names;

    private int[] types;

    private String[] linkFiles;

    private String[] linkTargets;

    @Setup(Level.Trial)
    public void setUp() throws Exception
    {
        HDF5NativeLibrary.load();
        file = File.createTempFile("link-info-" + links + "-", ".h5");
        fileId = H5.H5Fcreate(file.getPath(), H5F_ACC_TRUNC, H5P_DEFAULT, H5P_DEFAULT);
        final long group = H5.H5Gcreate(fileId, "g", H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT);
        for (int i = 0; i < links; ++i)
        {
            if (i % 2 == 0)
            {
                H5.H5Gclose(H5.H5Gcreate(group, "m" + i, H5P_DEFAULT, H5P_DEFAULT, H5P_DEFAULT));
            } else
            {
                H5.H5Lcreate_soft("/g/m" + (i - 1), group, "m" + i, H5P_DEFAULT, H5P_DEFAULT);
            }
        }
        H5.H5Gclose(group);
        names = new String[links];
        types = new int[links];
        linkFiles = new String[links];
        linkTargets = new String[links];
    }

    @TearDown(Level.Trial)
    public void tearDown()
    {
        H5.H5Fclose(fileId);
        file.delete();
    }

    /** Names and types of all members, with soft-link targets, in one pass. */
    @Benchmark
    public String[] linkInfoAll()
    {
        HDFHelper.H5Lget_link_info_all(fileId, "/g", names, types, linkFiles, linkTargets);
        return linkTargets;
    }

    /** Names only. */
    @Benchmark
    public String[] linkNamesAll()
    {
        HDFHelper.H5Lget_link_names_all(fileId, "/g", names);
        return names;
    }

    /** Names in one pass, then the type and target of each member by name. */
    @Benchmark
    public int linkInfoEachByName()
    {
        HDFHelper.H5Lget_link_names_all(fileId, "/g", names);
        final String[] target = new String[2];
        int sum = 0;
        for (final String name : names)
        {
            sum += HDFHelper.H5Lget_link_info(fileId, "/g/" + name, target, false);
        }
        return sum;
    }
}
