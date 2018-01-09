cmake_minimum_required(VERSION 3.4.1)

# find android log lib to name log-lib.
find_library(log-lib log)

# add rtmpsession
add_library(RtmpSession-jni SHARED
            src/main/cpp/RtmpSession_jni.cc
            src/main/cpp/RtmpSession.h
            src/main/cpp/RtmpSession.cc)

include_directories(${PROJECT_SOURCE_DIR}/third_party/rtmp/include/)
add_library(Rtmp SHARED IMPORTED)

set_target_properties(
    Rtmp
    PROPERTIES IMPORTED_LOCATION
    ${PROJECT_SOURCE_DIR}/third_party/rtmp/lib/${ANDROID_ABI}/librtmp.so
)

target_link_libraries(RtmpSession-jni Rtmp ${log-lib})
