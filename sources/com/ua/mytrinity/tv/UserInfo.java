package com.ua.mytrinity.tv;

import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import com.ua.mytrinity.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class UserInfo {
    private static final String TAG = "Updates";
    /* access modifiers changed from: private */
    public int m_account_id;
    /* access modifiers changed from: private */
    public String m_address;
    /* access modifiers changed from: private */
    public float m_balance;
    /* access modifiers changed from: private */
    public float m_bonus_in_month;
    /* access modifiers changed from: private */
    public float m_cost;
    /* access modifiers changed from: private */
    public String m_full_name;
    /* access modifiers changed from: private */
    public int m_is_blocked;
    /* access modifiers changed from: private */
    public boolean m_is_vod;
    /* access modifiers changed from: private */
    public String m_tariff;

    private static class UserInfoXmlReader {
        Element account_id = this.info.requireChild("account_id");
        Element address = this.info.requireChild("address");
        Element balance = this.info.requireChild("balance");
        Element bonus_in_month = this.info.requireChild("bonus_in_month");
        Element cost = this.info.requireChild("cost");
        Element error = this.root.getChild("error");
        int error_no = -1;
        String error_text;
        Element full_name = this.info.requireChild("full_name");
        Element info = this.root.getChild("info");
        Element is_blocked = this.info.requireChild("is_blocked");
        Element is_vod = this.info.requireChild("is_vod");
        UserInfo result;
        RootElement root = new RootElement("root");
        Element tariff = this.info.requireChild("tariff");

        public UserInfoXmlReader() {
            this.error.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    UserInfoXmlReader.this.error_no = Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID));
                }
            });
            this.error.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    UserInfoXmlReader.this.error_text = body;
                }
            });
            this.account_id.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    int unused = UserInfoXmlReader.this.result.m_account_id = Integer.parseInt(body);
                }
            });
            this.full_name.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    String unused = UserInfoXmlReader.this.result.m_full_name = body;
                }
            });
            this.is_blocked.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    int unused = UserInfoXmlReader.this.result.m_is_blocked = Integer.parseInt(body);
                }
            });
            this.tariff.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    String unused = UserInfoXmlReader.this.result.m_tariff = body;
                }
            });
            this.address.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    String unused = UserInfoXmlReader.this.result.m_address = body;
                }
            });
            this.balance.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    float unused = UserInfoXmlReader.this.result.m_balance = Float.parseFloat(body);
                }
            });
            this.cost.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    float unused = UserInfoXmlReader.this.result.m_cost = Float.parseFloat(body);
                }
            });
            this.bonus_in_month.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    float unused = UserInfoXmlReader.this.result.m_bonus_in_month = Float.parseFloat(body);
                }
            });
            this.is_vod.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    boolean unused = UserInfoXmlReader.this.result.m_is_vod = Integer.parseInt(body) != 0;
                }
            });
        }

        public void parse(InputStream is, UserInfo result2) throws IOException, SAXException, ProtocolException {
            this.result = result2;
            Xml.parse(is, Xml.Encoding.UTF_8, this.root.getContentHandler());
            if (this.error_no != -1) {
                throw ProtocolException.create(this.error_no, this.error_text);
            }
        }
    }

    public static UserInfo load() throws ProtocolException, IOException, IllegalStateException, SAXException {
        HttpURLConnection get = (HttpURLConnection) new URL(AppConfig.webApi() + "/userinfo.php?mac=" + AppConfig.mac() + "&ver=" + AppConfig.appVersion()).openConnection();
        if (get.getResponseCode() != 200) {
            Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
        }
        UserInfoXmlReader reader = new UserInfoXmlReader();
        UserInfo result = new UserInfo();
        reader.parse(get.getInputStream(), result);
        return result;
    }

    public int accountId() {
        return this.m_account_id;
    }

    public String fullName() {
        return this.m_full_name;
    }

    public boolean isBlocked() {
        return this.m_is_blocked != 0;
    }

    public boolean isAdminBlocked() {
        return this.m_is_blocked == 256 || this.m_is_blocked == 768 || this.m_is_blocked == 1280 || this.m_is_blocked == 1792;
    }

    public boolean isSystemBlocked() {
        return this.m_is_blocked == 16 || this.m_is_blocked == 48 || this.m_is_blocked == 80 || this.m_is_blocked == 112;
    }

    public String tariff() {
        return this.m_tariff;
    }

    public String address() {
        return this.m_address;
    }

    public float balance() {
        return this.m_balance;
    }

    public float cost() {
        return this.m_cost;
    }

    public float bonus() {
        return this.m_bonus_in_month;
    }

    public boolean isVOD() {
        return this.m_is_vod;
    }
}
