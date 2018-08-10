package tpv.smt32.uart.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import tpv.smt32.uart.R;
import tpv.smt32.uart.SerialPort;
import tpv.smt32.uart.sample.Application;

public class CommandActivity extends Activity {
    private static final String TAG_UART = CommandActivity.class.getPackage().getName() + "_tag_uart";
    private static final String TAG_THREAD = CommandActivity.class.getPackage().getName() + "_tag_thread";

    private static final byte ACK = (byte)0x79;
    private static final byte NACK = (byte)0x1F;

    private Application mApplication;

    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private UiControl uiCtrl;

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 42; // any random variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            return;
        }
        initAfterPermissionGranted();
    }

    @Override
    protected void onDestroy() {
        mApplication.closeSerialPort();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length == 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                    return;
                }
                initAfterPermissionGranted();
            }
        }
    }

    private void initAfterPermissionGranted() {
        setContentView(R.layout.command);

        uiCtrl = new UiControl();
        uiCtrl.init();

        mApplication = (Application) getApplication();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void displayError(String msg) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Error");
        b.setMessage(msg);
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                CommandActivity.this.finish();
            }
        });
        b.show();
    }

    private void command_init() {
        try {
            mSerialPort = mApplication.getSerialPort();
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
        } catch (Exception e) {
            displayError(e.getMessage());
            e.printStackTrace();
            return;
        }
        if (mSerialPort == null || mOutputStream == null || mInputStream == null) {
            displayError("COM port open fail!");
            return;
        }
        new Thread("thread: command_init") {
            @Override
            public void run() {
                for (int i = 0 ; i < 10 ; i++) {
                    if (i >= 1) waiting(100);
                    try {
                        mOutputStream.write(0x7F);
                        Log.i(TAG_UART, "send command: 0x" + Integer.toHexString(0x7F)
                                + "  <" + (i+1) + ">");
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (isAck()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uiCtrl.btnInit.setEnabled(false);
                                uiCtrl.textInit.setText("init ok");
                                uiCtrl.textInfo.setText("");
                                command_get_id();
                            }
                        });
                        break;
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uiCtrl.btnInit.setEnabled(true);
                                uiCtrl.textInit.setText("init fail!");
                                uiCtrl.textInfo.setText("");
                            }
                        });
                        break;
                    }
                }
                Log.i(TAG_THREAD, getName() + " is end!");
            }
        }.start();
    }

    private void command_get_id() {
        new Thread("thread: command_get_id") {
            @Override
            public void run() {
                for (int i = 0 ; i < 3 ; i++) {
                    if (i >= 1) waiting(100);

                    // send command id
                    try {
                        mOutputStream.write(new byte[]{(byte)0x02, (byte)0xFD});
                        Log.i(TAG_UART, "send command: 0x02, 0xFD  <" + (i+1) + ">");
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }

                    // wait for ACK
                    if (!isAck()) continue;

                    // receive byte2,3,4
                    final byte[] byte234 = new byte[3];
                    final int read;
                    try {
                        read = mInputStream.read(byte234);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (read == -1) continue;
                    final StringBuilder receivedBytes = new StringBuilder("0x");
                    for (int j = 0 ; j < 3 ; j++) {
                        receivedBytes.append(String.format("%02X", byte234[j]));
                    }
                    Log.i(TAG_UART, "received: " + receivedBytes.toString() + " <" + (i+1) + ">");

                    // wait for ACK
                    if (!isAck()) continue;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            uiCtrl.textChipId.setText("chip id: " + receivedBytes.toString());
                            command_get_version_and_read_protection_status();
                        }
                    });
                    break;
                }
                Log.i(TAG_THREAD, getName() + " is end!");
            }
        }.start();
    }

    private void command_get_version_and_read_protection_status() {
        new Thread("thread: command_get_version_and_read_protection_status") {
            @Override
            public void run() {
                for (int i = 0 ; i < 3 ; i++) {
                    if (i >= 1) waiting(100);

                    // send command id
                    try {
                        mOutputStream.write(new byte[]{(byte)0x01, (byte)0xFE});
                        Log.i(TAG_UART, "send command: 0x01, 0xFE");
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }

                    // wait for ACK
                    if (!isAck()) continue;

                    // receive byte2,3,4
                    final byte[] byte234 = new byte[3];
                    final int read;
                    try {
                        read = mInputStream.read(byte234);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (read == -1) continue;
                    final StringBuilder receivedBytes = new StringBuilder("0x");
                    for (int j = 0 ; j < 3 ; j++) {
                        receivedBytes.append(String.format("%02X", byte234[j]));
                    }
                    Log.i(TAG_UART, "received: " + receivedBytes.toString() + " <" + (i+1) + ">");

                    // wait for ACK
                    if (!isAck()) continue;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            uiCtrl.textBootloaderVersion.setText("bootloader version: " + receivedBytes.toString());
                            command_get();
                        }
                    });
                    break;
                }
                Log.i(TAG_THREAD, getName() + " is end!");
            }
        }.start();
    }

    private void command_get() {
        new Thread("thread: command_get") {
            @Override
            public void run() {
                for (int i = 0 ; i < 3 ; i++) {
                    if (i >= 1) waiting(100);

                    // send command id
                    try {
                        mOutputStream.write(new byte[]{(byte)0x00, (byte)0xFF});
                        Log.i(TAG_UART, "send command: 0x00, 0xFF  <" + (i+1) + ">");
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }

                    // wait for ACK
                    if (!isAck()) continue;

                    // read N is the following bytes
                    final int num;
                    try {
                        num = mInputStream.read();
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (num == -1 || num != 11) continue;

                    // receive bytes 12
                    final int N = num+1; // should be 12
                    final byte[] byteN = new byte[N];
                    final int read;
                    try {
                        read = mInputStream.read(byteN);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (read == -1 || read != N) continue;

                    final StringBuilder receivedBytes = new StringBuilder("0x");
                    for (int j = 0 ; j < N ; j++) {
                        receivedBytes.append(String.format("%02X", byteN[j]));
                    }
                    Log.i(TAG_UART, "received: " + receivedBytes.toString() + " <" + (i+1) + ">");

                    // wait for ACK
                    if (!isAck()) continue;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            uiCtrl.textSupportedCommands.setText("supported commands: " + receivedBytes.toString());
                            uiCtrl.btnWriteImage.setEnabled(true);
                        }
                    });
                    break;
                }
                Log.i(TAG_THREAD, getName() + " is end!");
            }
        }.start();
    }

    private void command_erase() {
        new Thread("thread: command_erase") {
            @Override
            public void run() {
                for (int i = 0 ; i < 5 ; i++) {
                    if (i >= 1) waiting(100);

                    // send command id
                    try {
                        mOutputStream.write(new byte[]{(byte)0x44, (byte)0xBB});
                        Log.i(TAG_UART, "send command: 0x44, 0xBB  <" + (i+1) + ">");
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }

                    // wait for ACK
                    if (!isAck()) continue;

                    // write 0xFFFF to mass erase
                    try {
                        mOutputStream.write(new byte[]{(byte)0xFF, (byte)0xFF});
                        Log.i(TAG_UART, "send command: 0xFF, 0xFF  <" + (i+1) + ">");
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }

                    // write 0x00
                    try {
                        mOutputStream.write(new byte[]{(byte)0x00});
                        Log.i(TAG_UART, "send command: 0x00  <" + (i+1) + ">");
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }

                    // wait for ACK
                    if (!isAck()) continue;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            uiCtrl.textInfo.setText("erase success");
                            command_write();
                        }
                    });
                    break;
                }
                Log.i(TAG_THREAD, getName() + " is end!");
            }
        }.start();
    }

    private void command_write() {
        new Thread("thread: command_write") {
            @Override
            public void run() {
                File imageFile = new File("/sdcard/TPV_Robot.bin");
                long imageFileSize = imageFile.length();
                Log.i(TAG_UART, "read: " + imageFile + ", file size: " + imageFileSize + " bytes");

                FileInputStream imageFileInputStream;
                try {
                    imageFileInputStream = new FileInputStream(imageFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return; // 不會找不到檔案
                }

                int n = (int) (imageFileSize/256);
                int lastLoopWriteBytes = (int) (imageFileSize - n*256);
                final int loops = n + 1;
                Log.i(TAG_UART, "loops: " + loops + ", lastLoopWriteBytes: " + lastLoopWriteBytes);

                long address = 0x08000000;
                for (int i = 0 ; i < loops ; i++) {
                    int N = 256;
                    if (i > 0) {
                        address += N;
                    }
                    if (i == loops-1) {
                        N = lastLoopWriteBytes;
                    }
                    Log.i(TAG_UART, "loop: " + (i+1) +", address: "
                            + String.format("0x%08X", address) + ", write " + N + " bytes");
                    byte[] readN = new byte[N];
                    int read;
                    try {
                        read = imageFileInputStream.read(readN);
                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uiCtrl.textInfo.setText("write fail!");
                                uiCtrl.btnWriteImage.setEnabled(true);
                            }
                        });
                        try {
                            imageFileInputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if (read == -1 || read != N) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uiCtrl.textInfo.setText("write fail!");
                                uiCtrl.btnWriteImage.setEnabled(true);
                            }
                        });
                        try {
                            imageFileInputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        return;
                    }

                    boolean isWriteNSuccess = false;
                    for (int j = 0 ; j < 5 ; j++) { // try write cmd to STM 5 times
                        // send command id
                        try {
                            Log.i(TAG_UART, "send command: 0x31, 0xCE  <" + (j+1) + ">");
                            mOutputStream.write(new byte[]{(byte)0x31, (byte)0xCE});
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }

                        // wait for ACK
                        if (!isAck()) continue;

                        // write 4 bytes address & 1 byte checksum
                        byte[] byteAddress8 = ByteBuffer.allocate(8).putLong(address).array();
                        byte[] byteAddress = {byteAddress8[4], byteAddress8[5], byteAddress8[6], byteAddress8[7]};
                        StringBuilder tmp = new StringBuilder("0x");
                        for (byte b : byteAddress) {
                            tmp.append(String.format("%02X", b));
                        }
                        Log.i(TAG_UART, "send address: " + tmp + "  <" + (j+1) + ">");
                        try {
                            mOutputStream.write(byteAddress);
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                            // write checksum
                        byte checksum = (byte) (byteAddress[0] ^ byteAddress[1] ^ byteAddress[2] ^ byteAddress[3]);
                        try {
                            Log.i(TAG_UART, "send address checksum: " + String.format("0x%02X", checksum) + "  <" + (j+1) + ">");
                            mOutputStream.write(new byte[]{checksum});
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }

                        // wait for ACK
                        if (!isAck()) continue;

                        // write 1 byte: n=N-1, N bytes, their checksum
                        n = N-1;
                        try {
                            Log.i(TAG_UART, "send n=N-1: " + n + "  <" + (j+1) + ">");
                            mOutputStream.write(new byte[]{(byte)n});
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                            // write N bytes
                        checksum = (byte)n;
                        StringBuilder sendBytes = new StringBuilder("0x");
                        for (byte b : readN) {
                            checksum ^= b;
                            sendBytes.append(String.format("%02X", b));
                        }
                        Log.i(TAG_UART, "send N bytes: " + sendBytes + "  <" + (j+1) + ">");
                        try {
                            mOutputStream.write(readN);
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                            // write checksum
                        try {
                            Log.i(TAG_UART, "send N bytes checksum: " + String.format("0x%02X", checksum) + "  <" + (j+1) + ">");
                            mOutputStream.write(new byte[]{checksum});
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }

                        // wait for ACK
                        if (!isAck()) continue;

                        final int thisI = i;
                        final long thisAddress = address;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (thisI != loops-1) {
                                    uiCtrl.textInfo.setText("write address " + String.format("0x%08X", thisAddress) + " success");
                                } else {
                                    uiCtrl.textInfo.setText("write success");
                                }
                            }
                        });
                        isWriteNSuccess = true;
                        break;
                    }

                    if (!isWriteNSuccess) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uiCtrl.textInfo.setText("write fail!");
                                uiCtrl.btnWriteImage.setEnabled(true);
                            }
                        });
                        try {
                            imageFileInputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        return;
                    }
                }
                try {
                    imageFileInputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                Log.i(TAG_THREAD, getName() + " is end!");
            }
        }.start();
    }

    boolean isAck() {
        for (int i = 0 ; i < 10 ; i++) {
            if (i == 0) waiting(100);
            else waiting(20);
            int read;
            try {
                read = mInputStream.read();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            if (read == -1) continue;
            Log.i(TAG_UART, "   received: 0x" + Integer.toHexString(read) + "  <" + (i+1) +">");
            if ((byte)read == ACK) {
                return true;
            } else if ((byte)read == NACK) {
                return false;
            }
        }
        return false;
    }

    void waiting(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class UiControl {
        Button btnInit;
        TextView textInit;
        TextView textChipId;
        TextView textBootloaderVersion;
        TextView textSupportedCommands;
        Button btnWriteImage;
        TextView textInfo;

        void init() {
            btnInit = findViewById(R.id.btn_init);
            textInit = findViewById(R.id.text_init);
            textChipId = findViewById(R.id.text_chip_id);
            textBootloaderVersion = findViewById(R.id.text_bootloader_version);
            textSupportedCommands = findViewById(R.id.text_get);
            btnWriteImage = findViewById(R.id.btn_write_img);
            textInfo = findViewById(R.id.text_info);

            textSupportedCommands.setMovementMethod(new ScrollingMovementMethod());

            btnInit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    command_init();
                }
            });

            btnWriteImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    btnWriteImage.setEnabled(false);
                    command_erase();
                }
            });

            btnWriteImage.setEnabled(false);
        }
    }
}