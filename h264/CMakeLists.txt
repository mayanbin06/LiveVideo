cmake_minimum_required(VERSION 3.4.1)

# find android log lib to name log-lib.
find_library(log-lib log)

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
