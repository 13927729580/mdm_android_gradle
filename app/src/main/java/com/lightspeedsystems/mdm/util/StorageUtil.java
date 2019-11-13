package com.lightspeedsystems.mdm.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Created by Robert T. Wilson on 2019-11-04.
 */
public class StorageUtil {
    private final static String TAG = "StorageUtil";


    public static boolean isExternalMounted() {
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isExternalReadOnly() {
        if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
            return true;
        } else {
            return false;
        }
    }

    public static String getPublicExternalBaseDir() {
        String ret = "";
        if(isExternalMounted()) {
            File dirFile = Environment.getExternalStorageDirectory();
            ret = dirFile.getAbsolutePath();
        }
        return ret;
    }

    public static boolean saveSerialId(String serialId) {
        try {
            if (isExternalMounted()) {
                if (!isExternalReadOnly()) {
                    String dirPath = getPublicExternalBaseDir();
                    File serialFile = new File(dirPath,".serial.id");
                    FileWriter serialWriter = new FileWriter(serialFile);
                    serialWriter.write(serialId);
                    serialWriter.flush();
                    serialWriter.close();
                    LSLogger.info(TAG, "saveSerialId: "+serialId);
                    return true;
                }

            }
        }catch (Exception ex) {
            Log.e("saveSerialId error:", ex.getMessage(), ex);
        }
        return false;
    }

    public static String getSerialId() {
        String ret = "";
        try {
            if (isExternalMounted()) {
                String dirPath = getPublicExternalBaseDir();
                File serialFile = new File(dirPath,".serial.id");

                StringBuilder text = new StringBuilder();
                BufferedReader br = new BufferedReader(new FileReader(serialFile));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                }
                br.close();
                ret = text.toString();
                LSLogger.info(TAG, "getSerialId: "+ret);
            }
        }catch (Exception ex) {
            LSLogger.exception(TAG, "getSerialId error:", ex);
        }
        return ret;
    }

}
