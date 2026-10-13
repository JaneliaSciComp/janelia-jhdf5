/*
 * Pure-C baseline for ChunkIterBenchmark: the same dataset and the same three ways of
 * enumerating its chunks, with no JVM involved. The difference to the Java numbers is the cost
 * of the JavaCPP binding, which for H5Dchunk_iter is one JNI upcall per chunk.
 *
 * Build against the libhdf5 that the JavaCPP natives jar extracts (so it is the same library
 * the Java benchmarks use), e.g. on macOS arm64:
 *
 *   H5=~/.javacpp/cache/hdf5-2.2.0-1.5.15-SNAPSHOT-macosx-arm64.jar/org/bytedeco/hdf5/macosx-arm64
 *   cc -O2 -I$H5/include -L$H5 -lhdf5.320 -Wl,-rpath,$H5 -o chunk_iter_baseline chunk_iter_baseline.c
 *   ./chunk_iter_baseline
 *
 * Reports the best of 20 runs (3 for the by-index loop) per case.
 */
#include "hdf5.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

static double now(void) { struct timespec t; clock_gettime(CLOCK_MONOTONIC, &t); return t.tv_sec + t.tv_nsec * 1e-9; }

static long count; static unsigned long long sum;
static int cb(const hsize_t *offset, unsigned filter_mask, haddr_t addr, hsize_t size, void *op_data)
{ (void)offset; (void)filter_mask; (void)op_data; count++; sum += addr + size; return H5_ITER_CONT; }
static int cb_noop(const hsize_t *offset, unsigned filter_mask, haddr_t addr, hsize_t size, void *op_data)
{ (void)offset; (void)filter_mask; (void)addr; (void)size; (void)op_data; return H5_ITER_CONT; }

static hid_t make(int size, int latest, char *path)
{
    hid_t fapl = H5Pcreate(H5P_FILE_ACCESS);
    if (latest) H5Pset_libver_bounds(fapl, H5F_LIBVER_LATEST, H5F_LIBVER_LATEST);
    else H5Pset_libver_bounds(fapl, H5F_LIBVER_EARLIEST, H5F_LIBVER_V18);
    H5Pset_meta_block_size(fapl, 4096);
    hid_t f = H5Fcreate(path, H5F_ACC_TRUNC, H5P_DEFAULT, fapl); H5Pclose(fapl);
    hid_t dcpl = H5Pcreate(H5P_DATASET_CREATE); hsize_t ch[2] = {16, 16}; H5Pset_chunk(dcpl, 2, ch);
    H5Pset_alloc_time(dcpl, H5D_ALLOC_TIME_EARLY);
    hsize_t dims[2] = {(hsize_t)size, (hsize_t)size}; hid_t sp = H5Screate_simple(2, dims, NULL);
    hid_t d = H5Dcreate2(f, "test", H5T_NATIVE_UINT8, sp, H5P_DEFAULT, dcpl, H5P_DEFAULT);
    unsigned char *buf = malloc((size_t)size * size); memset(buf, 2, (size_t)size * size);
    H5Dwrite(d, H5T_NATIVE_UINT8, H5S_ALL, H5S_ALL, H5P_DEFAULT, buf); free(buf);
    H5Fflush(f, H5F_SCOPE_GLOBAL); H5Sclose(sp); H5Pclose(dcpl); H5Fclose(f);
    return d;
}

int main(void)
{
    int sizes[] = {512, 2048};
    printf("%-9s %7s  %14s %14s %14s   per chunk (us): noop / counting / by-index\n", "libver", "chunks", "iter noop", "iter counting", "by index");
    for (int lv = 0; lv < 2; ++lv) for (int si = 0; si < 2; ++si) {
        char path[64]; snprintf(path, sizeof path, "/tmp/cbench-%d-%d.h5", lv, sizes[si]);
        hid_t d = make(sizes[si], lv, path);
        long n = (sizes[si] / 16) * (long)(sizes[si] / 16);
        int reps = 20; double best_noop = 1e9, best_cnt = 1e9, best_idx = 1e9;
        for (int r = 0; r < reps; ++r) { double t = now(); H5Dchunk_iter(d, H5P_DEFAULT, cb_noop, NULL); t = now() - t; if (t < best_noop) best_noop = t; }
        for (int r = 0; r < reps; ++r) { count = 0; sum = 0; double t = now(); H5Dchunk_iter(d, H5P_DEFAULT, cb, NULL); t = now() - t; if (count != n) { fprintf(stderr, "count %ld != %ld\n", count, n); return 1; } if (t < best_cnt) best_cnt = t; }
        for (int r = 0; r < 3; ++r) { double t = now(); for (hsize_t i = 0; i < (hsize_t)n; ++i) { hsize_t off[2]; unsigned fm; haddr_t a; hsize_t s; H5Dget_chunk_info(d, H5S_ALL, i, off, &fm, &a, &s); sum += a + s; } t = now() - t; if (t < best_idx) best_idx = t; }
        printf("%-9s %7ld  %11.3f ms %11.3f ms %11.3f ms   %6.3f / %6.3f / %8.3f\n", lv ? "latest" : "earliest", n, best_noop*1e3, best_cnt*1e3, best_idx*1e3, best_noop/n*1e6, best_cnt/n*1e6, best_idx/n*1e6);
        H5Dclose(d); remove(path);
    }
    return 0;
}
