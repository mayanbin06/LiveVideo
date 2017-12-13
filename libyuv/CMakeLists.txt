cmake_minimum_required(VERSION 3.4.1)

include_directories(third_party/libyuv/include)

# add cmkae sub build project(libyuv), output to build direct.
add_subdirectory(third_party/libyuv ./build)

# find all files in the direct.
aux_source_directory(src/main/cpp SRC_FILE)

# build jni lib.
add_library(yuv-jni SHARED ${SRC_FILE})

find_library(log-lib log)
target_link_libraries(yuv-jni yuv ${log-lib} )