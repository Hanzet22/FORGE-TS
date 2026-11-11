cmake_minimum_required(VERSION 3.22)
project(forge_ts_pty_benchmarks LANGUAGES CXX)

include(FetchContent)

set(CMAKE_BUILD_TYPE RelWithDebInfo CACHE STRING "Benchmark build type" FORCE)
set(BENCHMARK_ENABLE_TESTING OFF CACHE BOOL "Disable upstream benchmark tests" FORCE)
set(BENCHMARK_DOWNLOAD_DEPENDENCIES ON CACHE BOOL "Fetch benchmark dependencies" FORCE)

FetchContent_Declare(
    google_benchmark
    GIT_REPOSITORY https://github.com/CodSpeedHQ/codspeed-cpp
    SOURCE_SUBDIR google_benchmark
    GIT_TAG f5a917fdd14db7293bd37acb682873fec19f8b6c # v2.4.0
)

FetchContent_MakeAvailable(google_benchmark)

add_executable(forge_pty_bench pty_bench.cpp)
target_compile_features(forge_pty_bench PRIVATE cxx_std_17)
target_include_directories(forge_pty_bench PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/../app/src/main/cpp")
target_compile_options(forge_pty_bench PRIVATE -Wall -Wextra -Werror)
target_link_libraries(forge_pty_bench PRIVATE benchmark::benchmark)
