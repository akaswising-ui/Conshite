cmake_minimum_required(VERSION 3.22.1)

project(conshite_runtime LANGUAGES C CXX)

add_library(
    conshite_runtime
    SHARED
    native_runtime.cpp
)

target_compile_features(conshite_runtime PRIVATE cxx_std_20)
set_target_properties(
    conshite_runtime
    PROPERTIES
        CXX_STANDARD 20
        CXX_STANDARD_REQUIRED YES
        CXX_EXTENSIONS NO
)
