package com.ua.mytrinity.tv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.ua.mytrinity.AppConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class TChannel {
    private static final String TAG = "TChannel";
    private String m_group;
    private Bitmap m_icon = null;
    private int m_id;
    private int m_index;
    private ArrayList<TTimeOffset> m_offset_list = new ArrayList<>();
    private HashMap<TTimeOffset, String> m_offsets = new HashMap<>();
    private String m_title;

    public TChannel(int id, int index, String title, String group) {
        this.m_id = id;
        this.m_index = index;
        this.m_title = title;
        this.m_group = group;
    }

    public void loadIcon() {
        OutputStream os;
        if (this.m_icon == null) {
            try {
                File icon_file = new File(AppConfig.iconCacheDir() + "/" + this.m_id + ".png");
                if (!icon_file.exists()) {
                    HttpURLConnection get = (HttpURLConnection) new URL(AppConfig.webApi() + "/channel_img.php?id=" + this.m_id).openConnection();
                    if (get.getResponseCode() != 200) {
                        Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
                    }
                    InputStream is = get.getInputStream();
                    try {
                        os = new FileOutputStream(icon_file);
                        byte[] buffer = new byte[4096];
                        while (true) {
                            int bytesRead = is.read(buffer);
                            if (bytesRead == -1) {
                                break;
                            }
                            os.write(buffer, 0, bytesRead);
                        }
                        os.close();
                        is.close();
                    } catch (Throwable th) {
                        is.close();
                        throw th;
                    }
                }
                this.m_icon = BitmapFactory.decodeFile(icon_file.toString());
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
            } catch (IllegalStateException e2) {
                Log.e(TAG, "IllegalStateException", e2);
            }
        }
    }

    public int id() {
        return this.m_id;
    }

    public int index() {
        return this.m_index;
    }

    public String title() {
        return this.m_title;
    }

    public String group() {
        return group((TTimeOffset) null);
    }

    public String group(TTimeOffset offset) {
        if (offset == null || !this.m_offsets.containsKey(offset)) {
            return this.m_group;
        }
        return this.m_offsets.get(offset);
    }

    public Bitmap icon() {
        return this.m_icon;
    }

    public HashMap<TTimeOffset, String> offsets() {
        return this.m_offsets;
    }

    public ArrayList<TTimeOffset> offset_list() {
        return this.m_offset_list;
    }
}
