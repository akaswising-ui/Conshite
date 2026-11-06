cmake_minimum_required(VERSION 3.22.1)
project(conshite_native_runtime LANGUAGES CXX)

add_library(conshite_native SHARED
    jni_bridge.cpp
    display_session.cpp
    image_probe.cpp
)

target_include_directories(conshite_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_features(conshite_native PRIVATE cxx_std_17)
target_compile_options(conshite_native PRIVATE
    -Wall -Wextra -Wpedantic -fvisibility=hidden
)

find_library(android-log log)
target_link_libraries(conshite_native PRIVATE ${android-log})
