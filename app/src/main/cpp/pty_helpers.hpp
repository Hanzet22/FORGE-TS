#pragma once

#include <signal.h>
#include <sys/wait.h>

#include <cstddef>
#include <string>
#include <vector>

namespace forge::pty {

inline bool validDimensions(int rows, int columns) {
    return rows > 0 && rows <= 1000 && columns > 0 && columns <= 1000;
}

// Arguments and environment entries forwarded to execve must be non-empty,
// bounded, and free of embedded NUL bytes.
inline bool validStringArgument(const std::string& value) {
    return !value.empty() && value.size() <= 8192 &&
           value.find('\0') == std::string::npos;
}

inline bool validWorkingDirectory(const std::string& cwd) {
    return !cwd.empty() && cwd.front() == '/' && cwd.size() <= 4096;
}

inline bool allowedSignal(int signalValue) {
    return signalValue == SIGINT || signalValue == SIGTERM ||
           signalValue == SIGHUP || signalValue == SIGWINCH ||
           signalValue == SIGTSTP || signalValue == SIGCONT;
}

// Encodes a waitpid() status for the Kotlin side: exit code + 1 for a normal
// exit, 256 + signal number for a signalled child, and 1 otherwise.
inline int encodeWaitStatus(int status) {
    return WIFEXITED(status) ? WEXITSTATUS(status) + 1
         : WIFSIGNALED(status) ? 256 + WTERMSIG(status) : 1;
}

inline void buildCStringPointers(std::vector<std::string>& values,
                                 std::vector<char*>& pointers) {
    pointers.clear();
    pointers.reserve(values.size() + 1);
    for (auto& value : values) pointers.push_back(value.data());
    pointers.push_back(nullptr);
}

}  // namespace forge::pty
