cmake_minimum_required(VERSION 3.22.1)
project(nexshell_pty)

add_library(nexshell_pty SHARED pty_native.cpp)

find_library(log-lib log)

target_link_libraries(nexshell_pty ${log-lib})