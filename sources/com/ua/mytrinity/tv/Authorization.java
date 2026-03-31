package com.ua.mytrinity.tv;

import android.net.Uri;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;
import com.ua.mytrinity.AppConfig;
import com.ua.mytrinity.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Authorization {
    private static final String TAG = "Authorization";

    private static class AuthXmlReader {
        Element error = this.root.getChild("error");
        int error_no;
        String error_text;
        int result;
        RootElement root = new RootElement("root");
        Element status = this.root.getChild(NotificationCompat.CATEGORY_STATUS);

        public AuthXmlReader() {
            this.status.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    AuthXmlReader.this.result = Integer.parseInt(body);
                }
            });
            this.error.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    AuthXmlReader.this.error_no = Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID));
                }
            });
            this.error.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    AuthXmlReader.this.error_text = body;
                }
            });
        }

        public boolean parse(InputStream is) throws IOException, SAXException, ProtocolException {
            this.result = 0;
            this.error_no = -1;
            Xml.parse(is, Xml.Encoding.UTF_8, this.root.getContentHandler());
            if (this.error_no != -1) {
                throw ProtocolException.create(this.error_no, this.error_text);
            } else if (this.result == 1) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean authorize() throws IllegalStateException, IOException, SAXException, ProtocolException {
        return authorize((String) null, (String) null);
    }

    public static boolean authorize(String login, String password) throws IOException, IllegalStateException, SAXException, ProtocolException {
        String uri;
        if (login == null || password == null) {
            uri = AppConfig.webApi() + "/authorize.php?mac=" + AppConfig.mac();
        } else {
            uri = AppConfig.webApi() + "/authorize.php?login=" + Uri.encode(login) + "&password=" + Utils.md5(password) + "&mac=" + AppConfig.mac();
        }
        HttpURLConnection get = (HttpURLConnection) new URL(uri).openConnection();
        if (get.getResponseCode() != 200) {
            Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
        }
        return new AuthXmlReader().parse(get.getInputStream());
    }
}
