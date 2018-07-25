cmake_minimum_required(VERSION 3.4.1)

add_library(serial_port SHARED
            android_serialport_api_SerialPort.h
            android_serialport_api_SerialPort.c)

# Include libraries needed for hello-jni lib
target_link_libraries(serial_port
                      android
                      log
                      jnigraphics
                      m
                      z
                      dl
                      EGL)