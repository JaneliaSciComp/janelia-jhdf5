# janelia-jhdf5

[![Build](https://github.com/JaneliaSciComp/janelia-jhdf5/actions/workflows/build.yml/badge.svg)](https://github.com/JaneliaSciComp/janelia-jhdf5/actions/workflows/build.yml)
[![HDF5 2.2.0](https://img.shields.io/badge/HDF5-2.2.0-blue)](https://github.com/bytedeco/javacpp-presets/tree/master/hdf5)
[![Java 8+](https://img.shields.io/badge/Java-8%2B-blue)](pom.xml)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-green)](LICENSE)

A maintained fork of **JHDF5**, the high-level Java API for [HDF5](https://www.hdfgroup.org/solutions/hdf5/) written by Bernd Rinn at ETH Zürich's Scientific IT Services (SIS, formerly CISD). It keeps JHDF5's API (`ch.systemsx.cisd.hdf5.*`) and runs it on current HDF5 with native libraries for every major platform.

Maintained by [Mark Kittisopikul](https://www.janelia.org/people/mark-kittisopikul) at [Janelia Scientific Computing](https://www.janelia.org/support-team/scientific-computing-software), Howard Hughes Medical Institute. Issues and pull requests: [JaneliaSciComp/janelia-jhdf5](https://github.com/JaneliaSciComp/janelia-jhdf5).

## What this replaces

| Before | Now |
|---|---|
| `cisd:jhdf5` 19.04.1 | `org.scijava:janelia-jhdf5` |
| `cisd:base` (utility classes, native `libnativedata` and `libunix`) | Vendored into this artifact under the same package names (`ch.systemsx.cisd.base.*`), with the native parts rewritten in Java and [jnr-posix](https://github.com/jnr/jnr-posix) |
| HDF5 1.10 native libraries bundled by ETH for a few platforms | HDF5 2.2.0 from [`org.bytedeco:hdf5`](https://github.com/bytedeco/javacpp-presets/tree/master/hdf5), via `hdf5-platform` for Linux, macOS and Windows on x86-64 and arm64 |
| JHDF5's own JNI helper library (`libjhdf5`) | Pure Java on top of the bytedeco bindings (`HDFHelper`) |

Because the `cisd:base` classes keep their package names, code written against `cisd:jhdf5` compiles unchanged; only the Maven coordinates change. Do not put `cisd:base` on the same class path, as the classes would be duplicated.

## Using it

```xml
<dependency>
  <groupId>org.scijava</groupId>
  <artifactId>janelia-jhdf5</artifactId>
  <version>22.03.01-SNAPSHOT</version>
</dependency>
```

Until a release, the artifact is a SNAPSHOT that depends on the `org.bytedeco:hdf5` snapshot from [Central's snapshot repository](https://central.sonatype.com/repository/maven-snapshots/); the pom declares that repository. The native HDF5 library is loaded for you the first time a `ch.systemsx.cisd.hdf5` class is used.

```java
try (IHDF5Writer writer = HDF5Factory.open(new File("data.h5"))) {
    writer.int32().writeArray("counts", new int[] { 1, 2, 3 });
}
try (IHDF5Reader reader = HDF5Factory.openForReading(new File("data.h5"))) {
    int[] counts = reader.int32().readArray("counts");
}
```

The official HDF Group bindings (`hdf.hdf5lib.H5`) and the JavaCPP C/C++ bindings (`org.bytedeco.hdf5`) are on the class path too and can be mixed with JHDF5 calls on the same file handles.

## Differences from JHDF5 19.04.1

- **HDF5 2.2.0** instead of 1.10.x. Files written by either are interchangeable; see the HDF5 release notes for format details.
- **Native libraries** come from `org.bytedeco:hdf5` and are extracted at runtime by JavaCPP, so nothing has to be installed and `java.library.path` is not involved.
- **`ch.systemsx.cisd.base.unix.Unix`** covers the subset of the original API that JHDF5's archiver (`h5ar`) uses: file and link information, symbolic links, access modes, link ownership and timestamps, user and group lookup by id. Hard links, lookups by name, effective ids, process detection and sub-second timestamps are not implemented. On Windows `Unix.isOperational()` is `false`, as before.
- **`ch.systemsx.cisd.base.convert.NativeData`** is pure Java; `NativeData.isUseNativeLib()` always returns `false`.
- `HDF5Factory.getOpenHDF5FileCount()` works again (it returned 0 in an earlier version of this fork).

## Building

```sh
mvn test
```

The test suite is JHDF5's own (imported from `sourceTest/`), `cisd:base`'s tests for the vendored classes, and tests added here. CI runs it on Linux, macOS and Windows with Java 8 and 21, then builds [n5-hdf5](https://github.com/saalfeldlab/n5-hdf5) against the result as a downstream check.

## License

janelia-jhdf5 is licensed under the [Apache License, Version 2.0](LICENSE), the same license as the JHDF5 and `cisd:base` code it is based on. New code and modifications are © Howard Hughes Medical Institute; the code inherited from JHDF5 and `cisd:base` is © ETH Zürich, CISD and SIS, and keeps its original license headers; the files changed here say so in their headers. [NOTICE](NOTICE) lists the copyright notices that redistributions must carry, as section 4 of the license requires.

One file is under a different license: `ch.systemsx.cisd.base.convert.NativeData` is derived from NCSA's HDF Java library and is distributed under the NCSA HDF license in [licenses/COPYING_HDF](licenses/COPYING_HDF), as it was in `cisd:base`. The jar carries `LICENSE`, `NOTICE` and `licenses/COPYING_HDF` in `META-INF`.

The dependencies keep their own licenses: the HDF5 library and its Java bindings are © The HDF Group under the [HDF5 license](https://github.com/HDFGroup/hdf5/blob/develop/LICENSE); the JavaCPP presets are by Samuel Audet and contributors under Apache-2.0 or GPLv2 with Classpath exception.

## Acknowledgements

JHDF5 and `cisd:base` were written and maintained for many years by Bernd Rinn and colleagues at ETH Zürich SIS/CISD; this project exists because that code was worth keeping alive. The HDF5 2.2.0 bindings are made possible by the [bytedeco](https://github.com/bytedeco) project.
