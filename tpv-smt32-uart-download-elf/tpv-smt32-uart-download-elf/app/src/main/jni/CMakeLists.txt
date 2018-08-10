cmake_minimum_required(VERSION 3.4.1)

add_library(serial_port SHARED
            tpv_smt32_uart_SerialPort.h
            tpv_smt32_uart_SerialPort.c)

# Include libraries needed for hello-jni lib
target_link_libraries(serial_port
                      android
                      log
                      jnigraphics
                      m
                      z
                      dl
                      EGL)