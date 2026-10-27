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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import ch.systemsx.cisd.base.unix.Unix;

/**
 * The jnr-posix backed {@link Unix} that replaced cisd:base's native library, against what the
 * JDK offers for the same information. {@code h5ar} calls {@link Unix#getLinkInfo} once per
 * archived file, so its per-call cost is what matters.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class UnixBenchmark
{
    private File file;

    private String path;

    private Path nioPath;

    private int uid;

    @Setup
    public void setUp() throws IOException
    {
        if (Unix.isOperational() == false)
        {
            throw new IllegalStateException("Unix is not operational on this platform; nothing to benchmark.");
        }
        file = File.createTempFile("unix-bench-", ".txt");
        Files.write(file.toPath(), new byte[] { 1, 2, 3 });
        path = file.getAbsolutePath();
        nioPath = file.toPath();
        uid = Unix.getUid();
    }

    @TearDown
    public void tearDown()
    {
        file.delete();
    }

    @Benchmark
    public Unix.Stat unixGetLinkInfo()
    {
        return Unix.getLinkInfo(path);
    }

    @Benchmark
    public Unix.Stat unixGetFileInfo()
    {
        return Unix.getFileInfo(path);
    }

    @Benchmark
    public PosixFileAttributes nioReadAttributes() throws IOException
    {
        return Files.readAttributes(nioPath, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    }

    @Benchmark
    public long fileLastModified()
    {
        return file.lastModified();
    }

    @Benchmark
    public String unixUserNameForUid()
    {
        return Unix.tryGetUserNameForUid(uid);
    }
}
