cmake_minimum_required(VERSION 3.22.1)
project(forge_pty LANGUAGES CXX)

add_library(forge_pty SHARED forge_pty.cpp)
target_compile_features(forge_pty PRIVATE cxx_std_17)
target_compile_options(forge_pty PRIVATE -Wall -Wextra -Werror -fvisibility=hidden)
target_link_libraries(forge_pty PRIVATE log)
