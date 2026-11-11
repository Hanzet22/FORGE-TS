#include <jni.h>

#include "pty_helpers.hpp"

#include <pty.h>
#include <poll.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <unistd.h>
#include <errno.h>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstring>
#include <map>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

namespace {
struct Session {
    int master = -1;
    pid_t child = -1;
    bool exited = false;
    int waitStatus = 0;
    std::mutex ioMutex;
};

std::mutex registryMutex;
std::map<jlong, std::shared_ptr<Session>> sessions;
std::atomic<jlong> nextHandle{1};

std::shared_ptr<Session> findSession(jlong handle) {
    std::lock_guard<std::mutex> lock(registryMutex);
    const auto found = sessions.find(handle);
    return found == sessions.end() ? nullptr : found->second;
}

bool allowedSignal(jint signalValue) {
    return signalValue == SIGINT || signalValue == SIGTERM ||
           signalValue == SIGHUP || signalValue == SIGWINCH ||
           signalValue == SIGTSTP || signalValue == SIGCONT;
}

bool readStringArray(JNIEnv* env, jobjectArray input,
                     std::vector<std::string>& values,
                     std::vector<char*>& pointers) {
    if (input == nullptr) return false;
    const jsize count = env->GetArrayLength(input);
    if (count > 256) return false;
    values.reserve(static_cast<size_t>(count));
    pointers.reserve(static_cast<size_t>(count) + 1);
    for (jsize index = 0; index < count; ++index) {
        auto item = static_cast<jstring>(env->GetObjectArrayElement(input, index));
        if (item == nullptr) return false;
        const char* chars = env->GetStringUTFChars(item, nullptr);
        if (chars == nullptr) {
            env->DeleteLocalRef(item);
            return false;
        }
        values.emplace_back(chars);
        env->ReleaseStringUTFChars(item, chars);
        env->DeleteLocalRef(item);
        if (values.back().empty() || values.back().size() > 8192 ||
            values.back().find('\0') != std::string::npos) return false;
    }
    forge::pty::buildCStringPointers(values, pointers);
    return true;
}
}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_HG_Forge_Ts_data_simulation_NativePtyEngine_nativeStart(
    JNIEnv* env, jobject, jobjectArray argvArray, jobjectArray envArray,
    jstring cwdString, jint rows, jint columns) {
    if (!forge::pty::validDimensions(rows, columns) || cwdString == nullptr) return -EINVAL;

    std::vector<std::string> argv;
    std::vector<std::string> environment;
    std::vector<char*> argvPointers;
    std::vector<char*> envPointers;
    if (!readStringArray(env, argvArray, argv, argvPointers) ||
        argv.empty() || argv.front().front() != '/' ||
        !readStringArray(env, envArray, environment, envPointers)) return -EINVAL;

    const char* cwdChars = env->GetStringUTFChars(cwdString, nullptr);
    if (cwdChars == nullptr) return -ENOMEM;
    const std::string cwd(cwdChars);
    env->ReleaseStringUTFChars(cwdString, cwdChars);
    if (cwd.empty() || cwd.front() != '/' || cwd.size() > 4096) return -EINVAL;

    struct winsize size {};
    size.ws_row = static_cast<unsigned short>(rows);
    size.ws_col = static_cast<unsigned short>(columns);

    int master = -1;
    const pid_t child = forkpty(&master, nullptr, nullptr, &size);
    if (child < 0) return -errno;
    if (child == 0) {
        if (chdir(cwd.c_str()) != 0) _exit(126);
        execve(argvPointers.front() == nullptr ? "" : argv.front().c_str(),
               argvPointers.data(), envPointers.data());
        _exit(127);
    }

    const int flags = fcntl(master, F_GETFL, 0);
    if (flags < 0 || fcntl(master, F_SETFL, flags | O_NONBLOCK) < 0) {
        const int savedError = errno;
        kill(-child, SIGHUP);
        kill(child, SIGKILL);
        close(master);
        waitpid(child, nullptr, 0);
        return -savedError;
    }

    auto session = std::make_shared<Session>();
    session->master = master;
    session->child = child;
    const jlong handle = nextHandle.fetch_add(1);
    {
        std::lock_guard<std::mutex> lock(registryMutex);
        sessions.emplace(handle, std::move(session));
    }
    return handle;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_HG_Forge_Ts_data_simulation_NativePtyEngine_nativeRead(
    JNIEnv* env, jobject, jlong handle, jbyteArray destination, jint timeoutMillis) {
    if (destination == nullptr || timeoutMillis < 0 || timeoutMillis > 5000) return -EINVAL;
    auto session = findSession(handle);
    if (!session) return -EBADF;
    const jsize capacity = env->GetArrayLength(destination);
    if (capacity <= 0 || capacity > 65536) return -EINVAL;

    std::lock_guard<std::mutex> lock(session->ioMutex);
    // Keep reading after waitpid reports the leader exit: buffered PTY output
    // may still be pending, and Linux can briefly report EIO before its final
    // line-discipline data becomes readable.
    if (session->master < 0) return -EBADF;
    struct pollfd descriptor { session->master, POLLIN | POLLHUP | POLLERR, 0 };
    int ready;
    do { ready = poll(&descriptor, 1, timeoutMillis); } while (ready < 0 && errno == EINTR);
    if (ready == 0) return 0;
    if (ready < 0) return -errno;

    std::vector<jbyte> buffer(static_cast<size_t>(capacity));
    const ssize_t count = read(session->master, buffer.data(), buffer.size());
    if (count > 0) {
        env->SetByteArrayRegion(destination, 0, static_cast<jsize>(count), buffer.data());
        return static_cast<jint>(count);
    }
    if (count == 0 || (count < 0 && errno == EIO)) return -EIO;
    if (count < 0 && (errno == EAGAIN || errno == EINTR)) return 0;
    return -errno;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_HG_Forge_Ts_data_simulation_NativePtyEngine_nativeWrite(
    JNIEnv* env, jobject, jlong handle, jbyteArray source, jint offset, jint length) {
    if (source == nullptr || offset < 0 || length < 0) return -EINVAL;
    const jsize capacity = env->GetArrayLength(source);
    if (offset > capacity || length > capacity - offset || length > 65536) return -EINVAL;
    auto session = findSession(handle);
    if (!session) return -EBADF;
    if (length == 0) return 0;

    jbyte* bytes = env->GetByteArrayElements(source, nullptr);
    if (bytes == nullptr) return -ENOMEM;
    std::lock_guard<std::mutex> lock(session->ioMutex);
    if (session->master < 0 || session->exited) {
        env->ReleaseByteArrayElements(source, bytes, JNI_ABORT);
        return -EPIPE;
    }
    ssize_t written;
    do {
        written = write(session->master, bytes + offset, static_cast<size_t>(length));
    } while (written < 0 && errno == EINTR);
    const int savedError = errno;
    env->ReleaseByteArrayElements(source, bytes, JNI_ABORT);
    if (written >= 0) return static_cast<jint>(written);
    return -savedError;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_HG_Forge_Ts_data_simulation_NativePtyEngine_nativeResize(
    JNIEnv*, jobject, jlong handle, jint rows, jint columns) {
    if (!forge::pty::validDimensions(rows, columns)) return JNI_FALSE;
    auto session = findSession(handle);
    if (!session) return JNI_FALSE;
    std::lock_guard<std::mutex> lock(session->ioMutex);
    if (session->master < 0 || session->exited) return JNI_FALSE;
    struct winsize size {};
    size.ws_row = static_cast<unsigned short>(rows);
    size.ws_col = static_cast<unsigned short>(columns);
    return ioctl(session->master, TIOCSWINSZ, &size) == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_HG_Forge_Ts_data_simulation_NativePtyEngine_nativeWait(
    JNIEnv*, jobject, jlong handle) {
    auto session = findSession(handle);
    if (!session) return -EBADF;
    std::lock_guard<std::mutex> lock(session->ioMutex);
    if (session->exited) {
        return WIFEXITED(session->waitStatus) ? WEXITSTATUS(session->waitStatus) + 1
             : WIFSIGNALED(session->waitStatus) ? 256 + WTERMSIG(session->waitStatus) : 1;
    }
    int status = 0;
    const pid_t result = waitpid(session->child, &status, WNOHANG);
    if (result == 0) return 0;
    if (result < 0) {
        if (errno == EINTR) return 0;
        return -errno;
    }
    session->exited = true;
    session->waitStatus = status;
    return WIFEXITED(status) ? WEXITSTATUS(status) + 1
         : WIFSIGNALED(status) ? 256 + WTERMSIG(status) : 1;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_HG_Forge_Ts_data_simulation_NativePtyEngine_nativeSignal(
    JNIEnv*, jobject, jlong handle, jint signalValue) {
    if (!allowedSignal(signalValue)) return JNI_FALSE;
    auto session = findSession(handle);
    if (!session) return JNI_FALSE;
    std::lock_guard<std::mutex> lock(session->ioMutex);
    if (session->exited || session->child <= 0) return JNI_FALSE;
    return kill(-session->child, signalValue) == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_HG_Forge_Ts_data_simulation_NativePtyEngine_nativeClose(
    JNIEnv*, jobject, jlong handle, jboolean graceful) {
    std::shared_ptr<Session> session;
    {
        std::lock_guard<std::mutex> lock(registryMutex);
        const auto found = sessions.find(handle);
        if (found == sessions.end()) return JNI_FALSE;
        session = found->second;
        sessions.erase(found);
    }

    std::lock_guard<std::mutex> lock(session->ioMutex);
    if (session->child > 0) {
        // The PTY leader may already be reaped while descendants still hold
        // the slave side open. Signal the process group on every close path.
        kill(-session->child, graceful == JNI_TRUE ? SIGHUP : SIGKILL);
        if (!session->exited) {
            int status = 0;
            pid_t result = 0;
            if (graceful == JNI_TRUE) {
                for (int attempt = 0; attempt < 50; ++attempt) {
                    result = waitpid(session->child, &status, WNOHANG);
                    if (result == session->child || (result < 0 && errno == ECHILD)) break;
                    std::this_thread::sleep_for(std::chrono::milliseconds(10));
                }
            }
            if (result == 0) {
                kill(-session->child, SIGKILL);
                kill(session->child, SIGKILL);
                do { result = waitpid(session->child, &status, 0); } while (result < 0 && errno == EINTR);
            }
            if (result == session->child) {
                session->waitStatus = status;
            }
            session->exited = true;
        }
    }
    if (session->master >= 0) {
        close(session->master);
        session->master = -1;
    }
    return JNI_TRUE;
}
