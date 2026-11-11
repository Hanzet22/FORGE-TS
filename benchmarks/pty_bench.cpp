#include <benchmark/benchmark.h>

#include <pty.h>
#include <poll.h>
#include <signal.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#include <cerrno>
#include <cstring>
#include <string>

namespace {

bool waitForChild(pid_t child, int* status) {
    pid_t result;
    do {
        result = waitpid(child, status, 0);
    } while (result < 0 && errno == EINTR);
    return result == child;
}

void stopChild(pid_t child, int master) {
    if (child > 0) {
        kill(-child, SIGTERM);
        kill(child, SIGTERM);
        int status = 0;
        waitForChild(child, &status);
    }
    if (master >= 0) close(master);
}

static void BM_PtySpawnAndExit(benchmark::State& state) {
    for (auto _ : state) {
        (void)_;
        int master = -1;
        struct winsize window {};
        window.ws_row = 24;
        window.ws_col = 80;

        const pid_t child = forkpty(&master, nullptr, nullptr, &window);
        if (child < 0) {
            state.SkipWithError(std::strerror(errno));
            break;
        }
        if (child == 0) {
            execl("/bin/true", "true", static_cast<char*>(nullptr));
            _exit(127);
        }

        int status = 0;
        const bool reaped = waitForChild(child, &status);
        close(master);
        if (!reaped || !WIFEXITED(status) || WEXITSTATUS(status) != 0) {
            state.SkipWithError("PTY child did not exit successfully");
            break;
        }
        benchmark::DoNotOptimize(status);
    }
}
BENCHMARK(BM_PtySpawnAndExit);

static void BM_PtyEchoRoundTrip(benchmark::State& state) {
    int master = -1;
    struct winsize window {};
    window.ws_row = 24;
    window.ws_col = 80;

    struct termios raw {};
    cfmakeraw(&raw);

    const pid_t child = forkpty(&master, nullptr, &raw, &window);
    if (child < 0) {
        state.SkipWithError(std::strerror(errno));
        return;
    }
    if (child == 0) {
        execl("/bin/cat", "cat", static_cast<char*>(nullptr));
        _exit(127);
    }

    const std::string payload(64, 'p');
    std::string received(payload.size(), '\0');

    for (auto _ : state) {
        (void)_;
        size_t sent = 0;
        while (sent < payload.size()) {
            const ssize_t count = write(master, payload.data() + sent,
                                        payload.size() - sent);
            if (count < 0 && errno == EINTR) continue;
            if (count <= 0) {
                state.SkipWithError("PTY write failed");
                break;
            }
            sent += static_cast<size_t>(count);
        }
        if (state.error_occurred()) break;

        size_t totalRead = 0;
        while (totalRead < received.size()) {
            struct pollfd descriptor {};
            descriptor.fd = master;
            descriptor.events = POLLIN;
            const int ready = poll(&descriptor, 1, 2000);
            if (ready < 0 && errno == EINTR) continue;
            if (ready <= 0) {
                state.SkipWithError("Timed out waiting for PTY echo");
                break;
            }

            const ssize_t count = read(master, received.data() + totalRead,
                                       received.size() - totalRead);
            if (count < 0 && errno == EINTR) continue;
            if (count <= 0) {
                state.SkipWithError("PTY read failed");
                break;
            }
            totalRead += static_cast<size_t>(count);
        }
        if (state.error_occurred()) break;
        benchmark::DoNotOptimize(received.data());
    }

    state.SetBytesProcessed(state.iterations() *
                             static_cast<int64_t>(payload.size()));
    state.PauseTiming();
    stopChild(child, master);
}
BENCHMARK(BM_PtyEchoRoundTrip);

static void BM_PtyResize(benchmark::State& state) {
    int master = -1;
    int slave = -1;
    struct winsize initial {};
    initial.ws_row = 24;
    initial.ws_col = 80;
    if (openpty(&master, &slave, nullptr, nullptr, &initial) != 0) {
        state.SkipWithError(std::strerror(errno));
        return;
    }

    struct winsize resized {};
    resized.ws_row = 42;
    resized.ws_col = 132;
    for (auto _ : state) {
        (void)_;
        if (ioctl(master, TIOCSWINSZ, &resized) != 0) {
            state.SkipWithError(std::strerror(errno));
            break;
        }
        benchmark::DoNotOptimize(resized);
    }

    state.PauseTiming();
    close(slave);
    close(master);
}
BENCHMARK(BM_PtyResize);

}  // namespace

BENCHMARK_MAIN();
