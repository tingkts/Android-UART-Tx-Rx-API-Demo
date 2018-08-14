android-stm32-uart-command
-------------

Fork project android-serialport-api-demo to android-stm32-uart-command and add CommandActivity.java to implement STM32 bootloader USART command protocol.

![](https://github.com/tingkts/android_uart_tx_rx_api_demo/blob/master/android-stm32-uart-command/CommandActivity.jpg?raw=true)

> 图为：CommandActivity UI

<br />

- put STM32_Robot.bin to /sdcard
执行命令：`adb push STM32_Robot.bin /sdcard/STM32_Robot.bin`

- app sign platform key
1. put "android.stm32.uart" folder into AOSP/packages/app/
2. enter path "AOSP/package/app/android.stm32.uart", then run command `mm -j4`
3. sign platform key apk will be built in "out/target/product/*your_device_name*/system/app/android.smt32.uart/android.smt32.uart.apk"
4. install output android.smt32.uart.apk
