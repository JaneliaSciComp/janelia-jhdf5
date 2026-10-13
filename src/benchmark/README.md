# Benchmarks

[JMH](https://github.com/openjdk/jmh) benchmarks for the parts of janelia-jhdf5 that replaced
native code with Java, and for chunk enumeration through the JavaCPP binding. They are not part
of the build or the test run; the `benchmarks` Maven profile compiles them from this directory.

## Running

```sh
# everything (about 15 minutes)
mvn -Pbenchmarks test-compile exec:exec

# one class, one parameter value, JSON output
mvn -Pbenchmarks test-compile exec:exec \
    -Dbenchmarks.args="ChunkIter -p size=256 -p libver=earliest -rf json -rff chunk-iter.json"

# JMH's own options
mvn -Pbenchmarks test-compile exec:exec -Dbenchmarks.args=-h
```

`benchmarks.args` is passed to `org.openjdk.jmh.Main` as is. Each class carries its own
defaults for forks, warm-up and measurement, chosen so that the whole set finishes in a
reasonable time; override them on the command line (`-f`, `-wi`, `-i`, `-w`, `-r`) when you
need tighter numbers.

## What is measured

| Class | Replaces | Measures | Compared with |
|---|---|---|---|
| `ChunkIterBenchmark` | – | Enumerating every chunk of a 2-D `uint8` dataset with 16×16 chunks, edge length 32 … 2048 (4 … 16,384 chunks), with the v1 B-tree (`libver=earliest`) or fixed-array (`latest`) chunk index. Same setup as the Julia benchmarks in [JuliaIO/HDF5.jl#1031](https://github.com/JuliaIO/HDF5.jl/pull/1031#issuecomment-1407749686). | `H5Dchunk_iter` (one call, a JavaCPP callback per chunk) against a loop of `H5Dget_chunk_info` by index and a loop of `H5Dget_chunk_info_by_coord`. All through `org.bytedeco.hdf5`; `hdf.hdf5lib.H5` has none of these. |
| `NativeDataBenchmark` | cisd:base `libnativedata` (#6) | Converting 1,024 and 1,048,576 values between `byte[]` and `int[]`/`double[]`, in native and swapped byte order. | HDF5's JNI `HDFNativeData` and `java.nio.ByteBuffer`. |
| `VLStringBenchmark` | libjhdf5's compound VL-string helpers (#2) | Writing a Java string to a C string referenced from a compound buffer, reading it back, freeing it; 8, 256 and 8,192 characters. | – (absolute cost per member per record) |
| `LinkInfoBenchmark` | libjhdf5's link listing (#2) | Names, types and soft-link targets of all members of a group with 10, 1,000 and 10,000 links. | The one-pass helper against per-member lookups. |
| `UnixBenchmark` | cisd:base `libunix` (#7) | `lstat`/`stat` of a file and a uid → user-name lookup through jnr-posix. | `java.nio.file.Files.readAttributes`, `File.lastModified`. Skipped where `Unix.isOperational()` is false. |

## Results

Measured on 2026-10-13 on an Apple M1 Max (32 GB), macOS 26.6.2, Zulu OpenJDK 21.0.3,
`org.bytedeco:hdf5` 2.2.0-1.5.15-SNAPSHOT (build 19, HDF5 2.2.0), each benchmark in one forked
JVM with the class's default warm-up and iterations. Times are averages per operation.

### `H5Dchunk_iter` vs `H5Dget_chunk_info`, libver = earliest

| Dataset | Chunks | `H5Dchunk_iter` | `H5Dget_chunk_info` (by index) | `H5Dget_chunk_info_by_coord` | index / iter | coord / iter |
|---|---|---|---|---|---|---|
| 32×32 | 4 | 0.002 ms/op | 0.001 ms/op | 0.001 ms/op | 0.7 | 0.5 |
| 64×64 | 16 | 0.007 ms/op | 0.009 ms/op | 0.005 ms/op | 1.3 | 0.7 |
| 128×128 | 64 | 0.029 ms/op | 0.096 ms/op | 0.045 ms/op | 3.3 | 1.6 |
| 256×256 | 256 | 0.117 ms/op | 1.69 ms/op | 0.683 ms/op | 14.4 | 5.8 |
| 512×512 | 1,024 | 0.466 ms/op | 28.1 ms/op | 12.3 ms/op | 60.1 | 26.4 |
| 1024×1024 | 4,096 | 1.89 ms/op | 474 ms/op | 200 ms/op | 251.4 | 106.0 |
| 2048×2048 | 16,384 | 7.48 ms/op | 5,894 ms/op | 2,317 ms/op | 788.0 | 309.8 |

### `H5Dchunk_iter` vs `H5Dget_chunk_info`, libver = latest

| Dataset | Chunks | `H5Dchunk_iter` | `H5Dget_chunk_info` (by index) | `H5Dget_chunk_info_by_coord` | index / iter | coord / iter |
|---|---|---|---|---|---|---|
| 32×32 | 4 | 0.002 ms/op | 0.001 ms/op | 0.001 ms/op | 0.5 | 0.4 |
| 64×64 | 16 | 0.007 ms/op | 0.007 ms/op | 0.004 ms/op | 1.0 | 0.6 |
| 128×128 | 64 | 0.029 ms/op | 0.072 ms/op | 0.036 ms/op | 2.5 | 1.2 |
| 256×256 | 256 | 0.114 ms/op | 1.03 ms/op | 0.461 ms/op | 9.0 | 4.0 |
| 512×512 | 1,024 | 0.471 ms/op | 20.8 ms/op | 9.41 ms/op | 44.1 | 20.0 |
| 1024×1024 | 4,096 | 1.88 ms/op | 327 ms/op | 148 ms/op | 173.7 | 78.8 |
| 2048×2048 | 16,384 | 7.39 ms/op | 3,409 ms/op | 1,433 ms/op | 461.0 | 193.8 |

### NativeData, n = 1,024 values, per call

| Operation | `NativeData` (pure Java) | `NativeData`, byte-swapped | `HDFNativeData` (HDF5 JNI) | `ByteBuffer` |
|---|---|---|---|---|
| `byteToInt` | 0.195 us/op | 0.243 us/op | 0.939 us/op | 0.192 us/op |
| `intToByte` | 0.191 us/op | 0.235 us/op | 0.810 us/op | 0.210 us/op |
| `byteToDouble` | 0.480 us/op | 0.506 us/op | 1.20 us/op | 0.543 us/op |
| `doubleToByte` | 0.477 us/op | – | 1.47 us/op | 0.478 us/op |

### NativeData, n = 1,048,576 values, per call

| Operation | `NativeData` (pure Java) | `NativeData`, byte-swapped | `HDFNativeData` (HDF5 JNI) | `ByteBuffer` |
|---|---|---|---|---|
| `byteToInt` | 120 us/op | 124 us/op | 665 us/op | 121 us/op |
| `intToByte` | 118 us/op | 123 us/op | 829 us/op | 117 us/op |
| `byteToDouble` | 265 us/op | 251 us/op | 1,092 us/op | 257 us/op |
| `doubleToByte` | 261 us/op | – | 1,175 us/op | 255 us/op |

### VLString, per call

| Benchmark | length = 8 | length = 256 | length = 8,192 |
|---|---|---|---|
| `copyAndFree` | 269 ns/op | 299 ns/op | 1,077 ns/op |
| `roundTrip` | 405 ns/op | 462 ns/op | 1,750 ns/op |

### LinkInfo, per call

| Benchmark | links = 10 | links = 1,000 | links = 10,000 |
|---|---|---|---|
| `linkInfoAll` | 130 us/op | 1,099 ms/op | 133 s/op |
| `linkInfoEachByName` | 93.4 us/op | 504 ms/op | 59.2 s/op |
| `linkNamesAll` | 47.7 us/op | 488 ms/op | 59.6 s/op |

### Unix, per call

| Benchmark | Time |
|---|---|
| `fileLastModified` | 1,945 ns/op |
| `nioReadAttributes` | 2,375 ns/op |
| `unixGetFileInfo` | 2,495 ns/op |
| `unixGetLinkInfo` | 2,524 ns/op |
| `unixUserNameForUid` | 275 ns/op |

### What they show

- **`H5Dchunk_iter` scales linearly, the indexed routes quadratically**, as in the Julia
  measurements. Per chunk, `H5Dchunk_iter` costs about 0.45 µs from Java, independent of the
  chunk count and of the chunk index. `H5Dget_chunk_info` by index walks the chunk index from
  the start for every call, so a full enumeration grows with the square of the chunk count: at
  16,384 chunks it is 788× slower with the v1 B-tree index and 461× slower with the fixed array.
  `H5Dget_chunk_info_by_coord` is about 2.5× faster than by index but has the same shape.
  Anything that needs every chunk's address (kerchunk-style offset tables, archive manifests,
  chunk-level integrity checks) should use `H5Dchunk_iter`.
- **Of those 0.45 µs per chunk, about 0.43 µs is the JNI upcall.** `c/chunk_iter_baseline.c`
  runs the same enumerations in C against the same `libhdf5`:

  | | C, no-op callback | C, by index | Java callback | Java, by index |
  |---|---|---|---|---|
  | 1,024 chunks, v1 B-tree | 0.037 ms | 28.6 ms | 0.463 ms | 28.1 ms |
  | 16,384 chunks, v1 B-tree | 0.36 ms | 5,928 ms | 7.47 ms | 5,894 ms |
  | 16,384 chunks, fixed array | 0.29 ms | 3,408 ms | 7.36 ms | 3,409 ms |

  HDF5 itself visits a chunk in 0.02 µs; the JavaCPP callback (a JNI call back into the JVM per
  chunk) adds 0.43 µs, and an empty Java callback costs the same as one that records the chunk,
  so the body is free. The by-index times are the same from C and from Java: that route is bound
  by HDF5, not by the binding. So `H5Dchunk_iter` from Java is 20× slower than from C, yet still
  800× faster than the indexed alternative at 16,384 chunks. Collecting the chunk records in
  native code and crossing into Java once would remove most of the remaining cost; that would be
  a helper in the preset rather than in this project.
- **The pure-Java `NativeData` is faster than the native code it replaced the need for.** It
  matches `ByteBuffer` within noise and is 4–7× faster than HDF5's own JNI converters in
  `HDFNativeData`, for 1 K and 1 M values alike. Byte swapping costs at most 25% on small arrays
  and nothing measurable on large ones. The JNI copies pay for the array pinning and the extra
  copy across the boundary; the Java loops vectorize. A follow-up could make
  `HDFHelper.byteToInt` and friends, which still delegate to `HDFNativeData`, use `NativeData`.
- **Variable-length strings cost 0.3–0.5 µs per member and record** for typical lengths,
  dominated by the `calloc`/`free` pair rather than by string length until a few kilobytes.
- **Listing a group is quadratic in its size.** Both `H5Lget_link_info_all` and the plain
  `H5Lget_link_names_all` take 0.5–1.1 s for 1,000 members and 60–133 s for 10,000. The time is
  in HDF5's `H5Lget_name_by_idx`, which for a symbol-table group walks the B-tree to the
  requested index on every call, so looking up all `n` names by index costs O(n²). The original
  native `libjhdf5` made the same per-index calls in C, so this is inherited rather than a cost
  of the Java port, but it is the one place where a different HDF5 API would change the picture:
  `H5Literate2`, with a callback like `H5Dchunk_iter`'s, visits all links in one pass.
- **jnr-posix `lstat` costs the same as `java.nio.file.Files.readAttributes`** (2.5 µs against
  2.4 µs), and `File.lastModified` is only 20% cheaper for far less information. The uid → name
  lookup is 0.3 µs. `h5ar` makes one of these calls per archived file, so none of this is
  visible next to the file I/O.

