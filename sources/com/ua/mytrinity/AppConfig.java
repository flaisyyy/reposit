package com.ua.mytrinity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import com.ua.mytrinity.tv.TChannelList;
import com.ua.mytrinity.tv.TEpg;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

public class AppConfig {
    private static final String APP_PREFERENCES = "app";
    private static final String APP_PREF_GENERATED_MAC = "generated_mac";
    private static AppConfig m_instance = null;
    private static final String m_web_api = "http://video.my-trinity.com/android";
    private String m_cache_dir;
    private TChannelList m_channels = null;
    private TEpg m_epg = new TEpg();
    private String m_epg_cache_dir;
    private String m_icon_cache_dir;
    private String m_mac;
    private String m_version;
    private int m_version_code;

    public static AppConfig instance() {
        return m_instance;
    }

    private void check_dir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    @SuppressLint({"HardwareIds"})
    private AppConfig(Context context) {
        this.m_cache_dir = context.getCacheDir().toString();
        this.m_icon_cache_dir = this.m_cache_dir + "/icons";
        this.m_epg_cache_dir = this.m_cache_dir + "/epg";
        check_dir(this.m_icon_cache_dir);
        check_dir(this.m_epg_cache_dir);
        try {
            PackageInfo info = context.getApplicationContext().getPackageManager().getPackageInfo(context.getPackageName(), 0);
            this.m_version = info.versionName;
            this.m_version_code = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        this.m_mac = null;
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService("wifi");
        if (wifiManager != null) {
            String mac = wifiManager.getConnectionInfo().getMacAddress();
            if (mac == null || mac.equals("02:00:00:00:00:00")) {
                this.m_mac = null;
            } else {
                this.m_mac = mac.replaceAll(":", "");
            }
        }
        if (this.m_mac == null) {
            this.m_mac = getMacFromSys();
        }
        if (this.m_mac == null) {
            this.m_mac = getGeneratedMac(context);
        }
    }

    public static AppConfig getAppConfig(Context context) {
        if (m_instance == null) {
            m_instance = new AppConfig(context);
        }
        return m_instance;
    }

    public static String cacheDir() {
        return instance().m_cache_dir;
    }

    public static String iconCacheDir() {
        return instance().m_icon_cache_dir;
    }

    public static String epgCacheDir() {
        return instance().m_epg_cache_dir;
    }

    public static String webApi() {
        return m_web_api;
    }

    public TChannelList channelList() {
        return this.m_channels;
    }

    public TEpg epg() {
        return this.m_epg;
    }

    public void setChannelList(TChannelList list) {
        this.m_channels = list;
    }

    public void setEpg(TEpg epg) {
        this.m_epg = epg;
    }

    public static String mac() {
        if (m_instance != null) {
            return m_instance.m_mac;
        }
        return null;
    }

    public static String appVersion() {
        return instance().m_version;
    }

    public static int appVersionCode() {
        return instance().m_version_code;
    }

    private static String getMacFromSys() {
        String[] names = {"wlan0", "ra0", "eth0"};
        int length = names.length;
        for (int i = 0; i < length; i++) {
            try {
                File address = new File("/sys/class/net/" + names[i] + "/address");
                if (address.exists() && address.canRead()) {
                    return new BufferedReader(new InputStreamReader(new FileInputStream(address))).readLine().replaceAll(":", "");
                }
            } catch (IOException e) {
            }
        }
        return null;
    }

    private String getGeneratedMac(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(APP_PREFERENCES, 0);
        String mac = preferences.getString(APP_PREF_GENERATED_MAC, (String) null);
        if (mac != null) {
            return mac;
        }
        SharedPreferences.Editor editor = preferences.edit();
        String mac2 = generateMac();
        editor.putString(APP_PREF_GENERATED_MAC, mac2);
        editor.apply();
        return mac2;
    }

    private String generateMac() {
        byte[] macAddr = new byte[6];
        new Random(System.currentTimeMillis()).nextBytes(macAddr);
        macAddr[0] = (byte) (macAddr[0] & -2);
        StringBuilder sb = new StringBuilder(18);
        int length = macAddr.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", new Object[]{Byte.valueOf(macAddr[i])}));
        }
        return sb.toString();
    }
}
