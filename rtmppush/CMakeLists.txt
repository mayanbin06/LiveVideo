cmake_minimum_required(VERSION 3.4.1)

# find android log lib to name log-lib.
find_library(log-lib log)

# add fdk-jni and lib fdk.
include_directories(${PROJECT_SOURCE_DIR}/third_party/FdkAac/include/)
add_library(FdkAac SHARED IMPORTED)
set_target_properties(
    FdkAac
    PROPERTIES IMPORTED_LOCATION
    ${PROJECT_SOURCE_DIR}/third_party/FdkAac/lib/${ANDROID_ABI}/libFraunhoferAAC.so
)

add_library(FdkAac-jni SHARED src/main/cpp/FdkAac_jni.cc)
target_link_libraries(FdkAac-jni FdkAac ${log-lib})

# add openh264
add_library(OpenH264-jni SHARED src/main/cpp/OpenH264_jni.cc)

include_directories(${PROJECT_SOURCE_DIR}/third_party/openh264/include/)
add_library(OpenH264 SHARED IMPORTED)

set_target_properties(
    OpenH264
    PROPERTIES IMPORTED_LOCATION
    ${PROJECT_SOURCE_DIR}/third_party/openh264/lib/${ANDROID_ABI}/libopenh264.so
)
target_link_libraries(OpenH264-jni OpenH264 ${log-lib})

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
