package com.ua.mytrinity.tv;

import android.net.Uri;
import android.sax.Element;
import android.sax.RootElement;
import android.sax.TextElementListener;
import android.util.Log;
import android.util.Xml;
import com.ua.mytrinity.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Updates {
    private static final String TAG = "Updates";
    /* access modifiers changed from: private */
    public Uri m_uri;
    /* access modifiers changed from: private */
    public int m_versionCode;
    /* access modifiers changed from: private */
    public String m_versionName;

    private static class UpdatesXmlReader {
        Updates result;
        RootElement root = new RootElement("root");
        Element update = this.root.requireChild("update");

        public UpdatesXmlReader() {
            this.update.setTextElementListener(new TextElementListener() {
                public void end(String body) {
                    Uri unused = UpdatesXmlReader.this.result.m_uri = Uri.parse(body);
                }

                public void start(Attributes attributes) {
                    String unused = UpdatesXmlReader.this.result.m_versionName = attributes.getValue("versionName");
                    int unused2 = UpdatesXmlReader.this.result.m_versionCode = Integer.parseInt(attributes.getValue("versionCode"));
                }
            });
        }

        public void parse(InputStream is, Updates result2) throws IOException, SAXException {
            this.result = result2;
            Xml.parse(is, Xml.Encoding.UTF_8, this.root.getContentHandler());
        }
    }

    public static Updates load() {
        try {
            HttpURLConnection get = (HttpURLConnection) new URL(AppConfig.webApi() + "/update.php").openConnection();
            if (get.getResponseCode() != 200) {
                Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
            }
            UpdatesXmlReader reader = new UpdatesXmlReader();
            Updates result = new Updates();
            reader.parse(get.getInputStream(), result);
            return result;
        } catch (IOException | IllegalStateException | SAXException e) {
            return null;
        }
    }

    public String versionName() {
        return this.m_versionName;
    }

    public int versionCode() {
        return this.m_versionCode;
    }

    public Uri uri() {
        return this.m_uri;
    }
}
