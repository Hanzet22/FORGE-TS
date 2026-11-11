#include <benchmark/benchmark.h>

#include "pty_helpers.hpp"

#include <string>
#include <vector>

namespace {

static void BM_PtyDimensionsValidation(benchmark::State& state) {
    for (auto _ : state) {
        (void)_;
        const bool valid = forge::pty::validDimensions(24, 80);
        benchmark::DoNotOptimize(valid);
    }
}
BENCHMARK(BM_PtyDimensionsValidation);

static void BM_PtyArgvEnvPointerTable(benchmark::State& state) {
    std::vector<std::string> argv{
        "/data/user/0/com.HG.Forge.Ts/files/usr/bin/proot",
        "--link2symlink",
        "-0",
        "-r",
        "/data/user/0/com.HG.Forge.Ts/files/usr",
        "/usr/bin/env"
    };
    std::vector<std::string> environment{
        "HOME=/root",
        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
        "TMPDIR=/tmp",
        "TERM=xterm-256color"
    };

    std::vector<char*> argvPointers;
    std::vector<char*> envPointers;
    argvPointers.reserve(argv.size() + 1);
    envPointers.reserve(environment.size() + 1);

    for (auto _ : state) {
        (void)_;
        forge::pty::buildCStringPointers(argv, argvPointers);
        forge::pty::buildCStringPointers(environment, envPointers);
        benchmark::DoNotOptimize(argvPointers.data());
        benchmark::DoNotOptimize(envPointers.data());
        benchmark::ClobberMemory();
    }

    state.SetItemsProcessed(state.iterations() *
                            static_cast<int64_t>(argv.size() + environment.size()));
}
BENCHMARK(BM_PtyArgvEnvPointerTable);

}  // namespace

BENCHMARK_MAIN();
