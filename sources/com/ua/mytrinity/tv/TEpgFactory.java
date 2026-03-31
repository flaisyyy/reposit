package com.ua.mytrinity.tv;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import com.ua.mytrinity.AppConfig;
import com.ua.mytrinity.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TEpgFactory {
    private static final String TAG = "TEpgFactory";

    private static class TEpgXmlReader {
        Element channelElement;
        Integer channel_id;
        HashMap<Integer, ArrayList<TEpgItem>> data;
        ArrayList<TEpgItem> epgItems;
        String hash;
        Element hashElement;
        Element itemElement;
        RootElement root;
        Date t_start;
        Date t_stop;

        public TEpgXmlReader() {
            this.channel_id = -1;
            this.epgItems = null;
            this.t_start = null;
            this.t_stop = null;
            this.data = null;
            this.hash = null;
            this.root = new RootElement("epg");
            this.channelElement = this.root.getChild("channel");
            this.itemElement = this.channelElement.getChild("item");
            this.hashElement = this.root.getChild("hash");
            this.data = new HashMap<>();
        }

        public TEpg parse(InputStream is) {
            this.channelElement.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    TEpgXmlReader.this.channel_id = Integer.valueOf(Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID)));
                    TEpgXmlReader.this.epgItems = new ArrayList<>();
                }
            });
            this.channelElement.setEndElementListener(new EndElementListener() {
                public void end() {
                    TEpgXmlReader.this.data.put(TEpgXmlReader.this.channel_id, TEpgXmlReader.this.epgItems);
                    TEpgXmlReader.this.epgItems = null;
                }
            });
            this.itemElement.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    TEpgXmlReader.this.t_start = new Date(Long.parseLong(attributes.getValue("t_start")));
                    TEpgXmlReader.this.t_stop = new Date(Long.parseLong(attributes.getValue("t_stop")));
                }
            });
            this.itemElement.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TEpgXmlReader.this.epgItems.add(new TEpgItem(TEpgXmlReader.this.channel_id.intValue(), body, TEpgXmlReader.this.t_start, TEpgXmlReader.this.t_stop));
                }
            });
            this.hashElement.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TEpgXmlReader.this.hash = body;
                }
            });
            TEpg result = new TEpg();
            try {
                Xml.parse(is, Xml.Encoding.UTF_8, this.root.getContentHandler());
                result.setData(this.data);
                result.setHash(this.hash);
            } catch (IOException e) {
                Log.e(TEpgFactory.TAG, "IOException", e);
            } catch (SAXException e2) {
                Log.e(TEpgFactory.TAG, "SAXException", e2);
            }
            return result;
        }

        public String parseHash(InputStream is) {
            this.hashElement.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TEpgXmlReader.this.hash = body;
                }
            });
            try {
                Xml.parse(is, Xml.Encoding.UTF_8, this.root.getContentHandler());
                return this.hash;
            } catch (IOException e) {
                Log.e(TEpgFactory.TAG, "IOException", e);
            } catch (SAXException e2) {
                Log.e(TEpgFactory.TAG, "SAXException", e2);
            }
            return null;
        }
    }

    public static TEpg loadFromXmlStream(InputStream is) {
        return new TEpgXmlReader().parse(is);
    }

    public static String loadHashFromXmlStream(InputStream is) {
        return new TEpgXmlReader().parseHash(is);
    }

    public static TEpg loadFromFile(String fileName) {
        try {
            if (new File(fileName).exists()) {
                return loadFromXmlStream(new FileInputStream(fileName));
            }
            return null;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException", e);
            return null;
        }
    }

    public static String loadHashFromFile(String fileName) {
        try {
            if (new File(fileName).exists()) {
                return loadHashFromXmlStream(new FileInputStream(fileName));
            }
            return null;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException", e);
            return null;
        }
    }

    public static TEpg loadFromHttp(String uri) {
        try {
            HttpURLConnection get = (HttpURLConnection) new URL(uri).openConnection();
            if (get.getResponseCode() != 200) {
                Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
            }
            return loadFromXmlStream(get.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            return null;
        } catch (IllegalStateException e2) {
            Log.e(TAG, "IllegalStateException", e2);
            return null;
        }
    }

    public static String loadHashFromHttp(String uri) {
        try {
            HttpURLConnection get = (HttpURLConnection) new URL(uri).openConnection();
            if (get.getResponseCode() != 200) {
                Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
            }
            return loadHashFromXmlStream(get.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            return null;
        } catch (IllegalStateException e2) {
            Log.e(TAG, "IllegalStateException", e2);
            return null;
        }
    }

    public static TEpg load(int channel_id, boolean today) {
        String url = AppConfig.webApi() + "/epg.php?channel=" + channel_id + "&today=" + (today ? "1" : "0");
        String cacheFile = AppConfig.epgCacheDir() + "/" + (channel_id == -1 ? "all" : Integer.valueOf(channel_id)) + (today ? "_t" : "") + ".xml";
        String cacheHash = loadHashFromFile(cacheFile);
        String currentHash = loadHashFromHttp(url + "&hash=1");
        if (cacheHash == null || !(cacheHash == null || currentHash == null || cacheHash.compareTo(currentHash) == 0)) {
            Utils.loadHttpToFile(url, cacheFile);
        }
        return loadFromFile(cacheFile);
    }
}
