cmake_minimum_required(VERSION 3.31)

project("shizuku")

set(CMAKE_CXX_STANDARD 17)

# Global compilation flags for safety, stability, and code density
add_compile_options(
    -Werror=format
    -fdata-sections
    -ffunction-sections
    -fno-exceptions
    -fno-rtti
    -fno-threadsafe-statics
    -fstack-protector-strong
    -D_FORTIFY_SOURCE=2
)

# Global link options for page alignment and memory hardening
add_link_options(
    -Wl,-z,max-page-size=16384
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,noexecstack
)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    message("Building Release with high-performance optimizations...")

    add_definitions(-DNDEBUG)
    add_compile_options(
        -O3
        -flto
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -fomit-frame-pointer
        -fno-semantic-interposition
    )
    add_link_options(
        -flto
        -Wl,--exclude-libs,ALL
        -Wl,--gc-sections
        -Wl,--icf=all
        -Wl,--strip-all
    )
else ()
    message("Building Debug...")

    add_definitions(-DDEBUG)
endif ()

find_package(boringssl REQUIRED CONFIG)
find_package(cxx REQUIRED CONFIG)

add_executable(libshizuku.so
        starter.cpp misc.cpp selinux.cpp cgroup.cpp)

target_link_libraries(libshizuku.so log cxx::cxx)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(TARGET libshizuku.so POST_BUILD
            COMMAND ${CMAKE_STRIP} --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libshizuku.so")
endif ()

add_library(adb SHARED
        adb_pairing.cpp misc.cpp)

target_link_libraries(adb log boringssl::crypto_static cxx::cxx)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(TARGET adb POST_BUILD
            COMMAND ${CMAKE_STRIP} --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libadb.so")
endif ()
