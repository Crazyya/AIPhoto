package com.cstore.aiphoto;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by zhiya.zhang
 * on 2018/2/27 10:40.
 */

public class MyJavaFun {
    /**
     * 获取txt文件内容
     *
     * @param file 要获取内容的文件
     * @return 以字符串显示的文件内容
     */
    public static String getTxtFileMessage(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 获取txt文件
     */
    public static String getErrorTxtMessage() {
        String result = "";
        try {
            FileInputStream inputStream = MyApplication.instance().getApplicationContext().openFileInput(GlobalException.getCrashFileName());
            byte[] bytes = new byte[1024];
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            while (inputStream.read(bytes) != -1) {
                arrayOutputStream.write(bytes, 0, bytes.length);
            }
            inputStream.close();
            arrayOutputStream.close();
            result = new String(arrayOutputStream.toByteArray());
        } catch (Exception ignored) {
        }
        return result;
    }

    /**
     * 清空txt文件内容
     */
    public static void clearErrorTxtMessage() {
        try {
            FileOutputStream outputStream = MyApplication.instance().getApplicationContext().openFileOutput(GlobalException.getCrashFileName(), Activity.MODE_PRIVATE);
            outputStream.write("".getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception ignored) {
        }
    }
}
