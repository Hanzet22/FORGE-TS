#include <benchmark/benchmark.h>

#include "pty_helpers.hpp"

#include <signal.h>

#include <cstdint>
#include <string>
#include <vector>

namespace {

const char* const kCwd = "/data/user/0/com.HG.Forge.Ts/files/home";

std::vector<std::string> prootArgv() {
    return {
        "/data/user/0/com.HG.Forge.Ts/files/usr/bin/proot",
        "--link2symlink",
        "-0",
        "-r",
        "/data/user/0/com.HG.Forge.Ts/files/usr",
        "/usr/bin/env"
    };
}

// Base terminal environment plus `extra` synthetic entries, mirroring sessions
// that forward additional variables to the guest shell.
std::vector<std::string> terminalEnvironment(int64_t extra) {
    std::vector<std::string> environment{
        "HOME=/root",
        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
        "TMPDIR=/tmp",
        "TERM=xterm-256color"
    };
    for (int64_t index = 0; index < extra; ++index) {
        environment.push_back("FORGE_VAR_" + std::to_string(index) +
                              "=/data/user/0/com.HG.Forge.Ts/files/usr/lib/value_" +
                              std::to_string(index));
    }
    return environment;
}

static void BM_PtyDimensionsValidation(benchmark::State& state) {
    for (auto _ : state) {
        (void)_;
        bool valid = forge::pty::validDimensions(24, 80);
        benchmark::DoNotOptimize(valid);
    }
}
BENCHMARK(BM_PtyDimensionsValidation);

// Sweep of resize requests, including out-of-range values rejected by the JNI
// start/resize entry points.
static void BM_PtyResizeSweepValidation(benchmark::State& state) {
    std::vector<int> sizes;
    for (int value = -8; value <= 1032; value += 8) sizes.push_back(value);

    for (auto _ : state) {
        (void)_;
        int accepted = 0;
        for (int rows : sizes) {
            for (int columns : sizes) {
                accepted += forge::pty::validDimensions(rows, columns) ? 1 : 0;
            }
        }
        benchmark::DoNotOptimize(accepted);
    }
    state.SetItemsProcessed(state.iterations() *
                            static_cast<int64_t>(sizes.size() * sizes.size()));
}
BENCHMARK(BM_PtyResizeSweepValidation);

static void BM_PtyArgvEnvPointerTable(benchmark::State& state) {
    std::vector<std::string> argv = prootArgv();
    std::vector<std::string> environment = terminalEnvironment(state.range(0));

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
BENCHMARK(BM_PtyArgvEnvPointerTable)->Arg(0)->Arg(32)->Arg(250);

static void BM_PtyStringArgumentValidation(benchmark::State& state) {
    std::vector<std::string> values = prootArgv();
    for (auto& entry : terminalEnvironment(state.range(0))) values.push_back(entry);
    values.push_back(std::string(8192, 'x'));

    for (auto _ : state) {
        (void)_;
        bool valid = true;
        for (const auto& value : values) {
            valid &= forge::pty::validStringArgument(value);
        }
        benchmark::DoNotOptimize(valid);
    }
    state.SetItemsProcessed(state.iterations() * static_cast<int64_t>(values.size()));
}
BENCHMARK(BM_PtyStringArgumentValidation)->Arg(0)->Arg(32)->Arg(250);

// Pre-fork launch preparation done by nativeStart: dimension, argv, env and
// working-directory validation followed by the execve pointer tables.
static void BM_PtyLaunchPreparation(benchmark::State& state) {
    for (auto _ : state) {
        (void)_;
        std::vector<std::string> argv = prootArgv();
        std::vector<std::string> environment = terminalEnvironment(state.range(0));
        std::vector<char*> argvPointers;
        std::vector<char*> envPointers;
        const std::string cwd(kCwd);

        bool valid = forge::pty::validDimensions(24, 80) &&
                     forge::pty::validWorkingDirectory(cwd) &&
                     argv.front().front() == '/';
        for (const auto& value : argv) valid &= forge::pty::validStringArgument(value);
        for (const auto& value : environment) valid &= forge::pty::validStringArgument(value);
        forge::pty::buildCStringPointers(argv, argvPointers);
        forge::pty::buildCStringPointers(environment, envPointers);

        benchmark::DoNotOptimize(valid);
        benchmark::DoNotOptimize(argvPointers.data());
        benchmark::DoNotOptimize(envPointers.data());
        benchmark::ClobberMemory();
    }
}
BENCHMARK(BM_PtyLaunchPreparation)->Arg(0)->Arg(32);

static void BM_PtySignalFilter(benchmark::State& state) {
    std::vector<int> signals;
    for (int value = 0; value <= 64; ++value) signals.push_back(value);

    for (auto _ : state) {
        (void)_;
        int allowed = 0;
        for (int value : signals) allowed += forge::pty::allowedSignal(value) ? 1 : 0;
        benchmark::DoNotOptimize(allowed);
    }
    state.SetItemsProcessed(state.iterations() * static_cast<int64_t>(signals.size()));
}
BENCHMARK(BM_PtySignalFilter);

static void BM_PtyWaitStatusEncoding(benchmark::State& state) {
    // Raw waitpid() statuses: normal exits (code << 8) and signalled children.
    std::vector<int> statuses;
    for (int code = 0; code < 256; ++code) statuses.push_back(code << 8);
    for (int signalValue : {SIGHUP, SIGINT, SIGKILL, SIGSEGV, SIGTERM}) {
        statuses.push_back(signalValue);
    }

    for (auto _ : state) {
        (void)_;
        int64_t checksum = 0;
        for (int status : statuses) checksum += forge::pty::encodeWaitStatus(status);
        benchmark::DoNotOptimize(checksum);
    }
    state.SetItemsProcessed(state.iterations() * static_cast<int64_t>(statuses.size()));
}
BENCHMARK(BM_PtyWaitStatusEncoding);

}  // namespace

BENCHMARK_MAIN();
