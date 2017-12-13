cmake_minimum_required(VERSION 3.4.1)


# add fdk
add_library( # Specifies the name of the library.
             FdkAac-jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/FdkAac_jni.cc )

include_directories(libs/FdkAac/include/)
add_library( FraunhoferAAC
             SHARED
             IMPORTED )

set_target_properties( # Specifies the target library.
                       FraunhoferAAC

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       ../../../../libs/FdkAac/lib/${ANDROID_ABI}/libFraunhoferAAC.so )

# add openh264
add_library( # Specifies the name of the library.
             OpenH264-jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/OpenH264_jni.cc )
include_directories(libs/openh264/include/)
add_library( OpenH264
             SHARED
             IMPORTED )

set_target_properties( # Specifies the target library.
                       OpenH264

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       ../../../../libs/openh264/lib/${ANDROID_ABI}/libopenh264.so )

# add rtmpsession
add_library( # Specifies the name of the library.
             RtmpSession-jni

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/RtmpSession_jni.cc
             src/main/cpp/RtmpSession.h
             src/main/cpp/RtmpSession.cc)

include_directories(libs/rtmp/include/)
add_library( Rtmp
             SHARED
             IMPORTED )

set_target_properties( # Specifies the target library.
                       Rtmp

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       ../../../../libs/rtmp/lib/${ANDROID_ABI}/librtmp.so )

find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              log-lib

              # Specifies the name of the NDK library that
              # CMake needs to locate.
              log )

# Links your native library against one or more other native libraries.
target_link_libraries( # Specifies the target library.
                       FdkAac-jni

                       FraunhoferAAC

                       # Links the log library to the target library.
                       ${log-lib} )

# Links your native library against one or more other native libraries.
target_link_libraries( # Specifies the target library.
                       OpenH264-jni

                       OpenH264

                       # Links the log library to the target library.
                       ${log-lib} )

# Links your native library against one or more other native libraries.
target_link_libraries( # Specifies the target library.
                       RtmpSession-jni

                       Rtmp

                       # Links the log library to the target library.
                       ${log-lib} )