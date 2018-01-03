cmake_minimum_required(VERSION 3.4.1)

# find android log lib to name log-lib.
find_library(log-lib log)

# add srs-rtmp
add_library(SrsRtmp-jni SHARED
            src/main/cpp/srs_librtmp.h
            src/main/cpp/srs_librtmp.cpp
            src/main/cpp/SrsRtmp_jni.cc
            src/main/cpp/SrsRtmp.h
            src/main/cpp/SrsRtmp.cpp
            )

target_link_libraries(SrsRtmp-jni ${log-lib})
