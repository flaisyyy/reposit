package com.ua.mytrinity.tv;

import android.annotation.SuppressLint;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import com.ua.mytrinity.AppConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressLint({"UseSparseArrays"})
public class TChannelList extends Vector<TChannel> {
    private static final String TAG = "TChannelList";
    private static final long serialVersionUID = -7599915840398601189L;
    HashMap<Integer, TTimeOffset> m_offsets = new HashMap<>();
    String m_streamer;

    private static class TChannelXmlReader {
        Element channelElement = this.list.requireChild("channel");
        Element channelId = this.channelElement.requireChild(TtmlNode.ATTR_ID);
        Element channelOffset = this.channelElement.getChild("offset");
        Element channelTitle = this.channelElement.requireChild("title");
        Element channelUrl = this.channelElement.requireChild("url");
        HashMap<TTimeOffset, String> channel_offsets = new HashMap<>();
        Element error = this.root.getChild("error");
        int error_no = -1;
        String error_text;
        String id;
        Element list = this.root.getChild("list");
        String offset;
        Element offsetElement = this.list.getChild("offset");
        Element offsetIndex = this.offsetElement.requireChild("index");
        Element offsetOffsetTime = this.offsetElement.requireChild("time");
        Integer offset_id;
        TChannelList result;
        RootElement root = new RootElement("root");
        Element streamer = this.list.requireChild("streamer");
        String title;
        String url;

        public TChannelXmlReader() {
            this.error.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    TChannelXmlReader.this.error_no = Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID));
                }
            });
            this.error.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TChannelXmlReader.this.error_text = body;
                }
            });
            this.streamer.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TChannelXmlReader.this.result.m_streamer = body;
                }
            });
            this.channelElement.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    TChannelXmlReader.this.channel_offsets.clear();
                }
            });
            this.channelId.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TChannelXmlReader.this.id = body;
                }
            });
            this.channelTitle.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TChannelXmlReader.this.title = body;
                }
            });
            this.channelUrl.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TChannelXmlReader.this.url = body;
                }
            });
            this.channelOffset.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    TChannelXmlReader.this.offset_id = Integer.valueOf(Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID)));
                }
            });
            this.channelOffset.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TChannelXmlReader.this.channel_offsets.put(TChannelXmlReader.this.result.m_offsets.get(TChannelXmlReader.this.offset_id), body);
                }
            });
            this.channelElement.setEndElementListener(new EndElementListener() {
                public void end() {
                    TChannel channel = new TChannel(Integer.parseInt(TChannelXmlReader.this.id), TChannelXmlReader.this.result.size() + 1, TChannelXmlReader.this.title, TChannelXmlReader.this.url);
                    channel.offsets().putAll(TChannelXmlReader.this.channel_offsets);
                    channel.offset_list().addAll(TChannelXmlReader.this.channel_offsets.keySet());
                    Collections.sort(channel.offset_list());
                    TChannelXmlReader.this.result.add(channel);
                }
            });
            this.offsetIndex.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TChannelXmlReader.this.id = body;
                }
            });
            this.offsetOffsetTime.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    TChannelXmlReader.this.offset = body;
                }
            });
            this.offsetElement.setEndElementListener(new EndElementListener() {
                public void end() {
                    TChannelXmlReader.this.result.m_offsets.put(Integer.valueOf(Integer.parseInt(TChannelXmlReader.this.id)), new TTimeOffset(Integer.parseInt(TChannelXmlReader.this.id), Integer.parseInt(TChannelXmlReader.this.offset)));
                }
            });
        }

        public void parse(InputStream is, TChannelList result2) throws IOException, SAXException, ProtocolException {
            this.result = result2;
            Xml.parse(is, Xml.Encoding.UTF_8, this.root.getContentHandler());
            if (this.error_no != -1) {
                throw ProtocolException.create(this.error_no, this.error_text);
            }
        }
    }

    public static TChannelList loadFromXmlStream(InputStream is) throws IOException, SAXException, ProtocolException {
        TChannelXmlReader reader = new TChannelXmlReader();
        TChannelList result = new TChannelList();
        reader.parse(is, result);
        return result;
    }

    public static TChannelList loadFromFile(String fileName) {
        try {
            if (new File(fileName).exists()) {
                return loadFromXmlStream(new FileInputStream(fileName));
            }
            return null;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException", e);
            return null;
        } catch (IOException e2) {
            Log.e(TAG, "IOException", e2);
            return null;
        } catch (SAXException e3) {
            Log.e(TAG, "SAXException", e3);
            return null;
        } catch (ProtocolException e4) {
            Log.e(TAG, "ProtocolException", e4);
            return null;
        }
    }

    public static TChannelList loadFromHttp(String uri) throws IOException, IllegalStateException, SAXException, ProtocolException {
        HttpURLConnection get = (HttpURLConnection) new URL(uri).openConnection();
        if (get.getResponseCode() != 200) {
            Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
        }
        return loadFromXmlStream(get.getInputStream());
    }

    public static TChannelList load() throws IllegalStateException, IOException, SAXException, ProtocolException {
        return loadFromHttp(AppConfig.webApi() + "/channels.php?mac=" + AppConfig.mac() + "&ver=" + AppConfig.appVersion());
    }

    public Collection<TTimeOffset> offsets() {
        return this.m_offsets.values();
    }

    public TTimeOffset offset(int index) {
        return this.m_offsets.get(Integer.valueOf(index));
    }

    public String streamer() {
        return this.m_streamer;
    }
}
