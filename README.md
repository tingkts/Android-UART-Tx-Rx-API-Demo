android-serialport-api-demo
-------------
It's the original reference project from [Android 模拟器串口与PC虚拟串口通讯 - CSDN博客](https://blog.csdn.net/gd6321374/article/details/74779770 "Android 模拟器串口与PC虚拟串口通讯 - CSDN博客")

<br />

android-stm32-uart-command
-------------

Fork project android-serialport-api-demo to android-stm32-uart-command and add CommandActivity.java to implement STM32 bootloader USART command protocol.

![](https://github.com/tingkts/android_uart_tx_rx_api_demo/blob/master/android-stm32-uart-command/CommandActivity.jpg?raw=true)

> 图为：CommandActivity UI

<br />

- put STM32_Robot.bin to /sdcard

  `adb push STM32_Robot.bin /sdcard/STM32_Robot.bin`

- app sign platform key
1. put "android.stm32.uart" folder into AOSP/packages/app/
2. entry path "AOSP/package/app/android.stm32.uart", then run command `mm -j4`
3. app signed platform key will be built in "out/target/product/*your_device_name*/system/app/android.smt32.uart/android.smt32.uart.apk"
4. install output android.smt32.uart.apk
