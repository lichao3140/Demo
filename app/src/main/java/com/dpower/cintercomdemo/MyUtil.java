package com.dpower.cintercomdemo;

import android.content.Context;
import android.os.Environment;
import android.telephony.TelephonyManager;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyUtil {

    public static String getUsername(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String username = tm.getDeviceId();
        if (username != null) {
            username = "CI" + username;
            return username;
        }
        return null;
    }

    public static String getServerIP(String address) {
        String port = "";
        int index = address.indexOf(":");
        if (index != -1) {
            port = address.substring(index, address.length());
            address = address.substring(0, index);
        }
        try {
            InetAddress host = InetAddress.getByName(address);
            String ip;
            if (port.length() > 0) {
                ip = host.getHostAddress() + port;
                return ip;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getImageName(String key) {
        String ret  = null;
        if (key != null) {
            ret = getFilePath("d_images/" + key + "/") + GetCurrentTime() + ".jpg";
        } else {
            ret = getFilePath("d_images/") + GetCurrentTime() + ".jpg";
        }
        return ret;
    }

    public static String getFilePath(String folder) {
        String path = (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED))
                ?(Environment.getExternalStorageDirectory().getAbsolutePath()):"/mnt/sdcard";//保存到SD卡
        String filepath = path + "/CIntercomDemo/";
        if (folder != null)
            filepath = filepath + folder;
        File f = new File(filepath);
        if (!f.exists()) {
            f.mkdirs();
        }
        return filepath;
    }

    public static String GetCurrentTime(){
        String ret = null;
        SimpleDateFormat sdateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        ret = sdateFormat.format(new Date());
        return ret;
    }
}
