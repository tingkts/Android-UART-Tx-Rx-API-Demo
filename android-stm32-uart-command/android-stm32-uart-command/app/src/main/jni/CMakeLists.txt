cmake_minimum_required(VERSION 3.4.1)

add_library(serial_port SHARED
            android_stm32_uart_SerialPort.h
            android_stm32_uart_SerialPort.c)

# Include libraries needed for hello-jni lib
target_link_libraries(serial_port
                      android
                      log
                      jnigraphics
                      m
                      z
                      dl
                      EGL)