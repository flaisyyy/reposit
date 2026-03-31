package com.ua.mytrinity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Utils {
    private static final String TAG = "Utils";

    public static Document loadXmlFromHttp(String uri) {
        Log.i(TAG, "loadXmlFromHttp " + uri);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            if (connection.getResponseCode() != 200) {
                Log.d(TAG, "HTTP error, invalid server status code: " + connection.getResponseMessage());
            }
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            return null;
        } catch (ParserConfigurationException e2) {
            Log.e(TAG, "ParserConfigurationException", e2);
            return null;
        } catch (IllegalStateException e3) {
            Log.e(TAG, "IllegalStateException", e3);
            return null;
        } catch (SAXException e4) {
            Log.e(TAG, "SAXException", e4);
            return null;
        }
    }

    public static Document loadXmlFromFile(String fileName) {
        Log.i(TAG, "loadXmlFromFile " + fileName);
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(fileName));
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (ParserConfigurationException e2) {
            Log.e(TAG, "ParserConfigurationException", e2);
        } catch (IllegalStateException e3) {
            Log.e(TAG, "IllegalStateException", e3);
        } catch (SAXException e4) {
            Log.e(TAG, "SAXException", e4);
        }
        return null;
    }

    public static boolean loadHttpToFile(String uri, String fileName) {
        OutputStream os;
        boolean result = false;
        Log.i(TAG, "loadHttpToFile " + uri + " -> " + fileName);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            if (connection.getResponseCode() != 200) {
                Log.d(TAG, "HTTP error, invalid server status code: " + connection.getResponseMessage());
            }
            InputStream is = connection.getInputStream();
            try {
                os = new FileOutputStream(fileName);
                byte[] buffer = new byte[4096];
                while (true) {
                    int bytesRead = is.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    os.write(buffer, 0, bytesRead);
                }
                result = true;
                os.close();
                is.close();
                return result;
            } catch (Throwable th) {
                is.close();
                throw th;
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
    }

    public static boolean saveExe(InputStream is, String fileName) {
        boolean result = false;
        try {
            OutputStream os = new FileOutputStream(fileName);
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    int bytesRead = is.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    os.write(buffer, 0, bytesRead);
                }
                new File(fileName).setExecutable(true);
                result = true;
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            } catch (IOException e2) {
                Log.e(TAG, e2.toString());
                try {
                    os.close();
                } catch (IOException e3) {
                    Log.e(TAG, e3.toString());
                }
            } catch (Throwable th) {
                try {
                    os.close();
                } catch (IOException e4) {
                    Log.e(TAG, e4.toString());
                }
                throw th;
            }
            try {
                is.close();
            } catch (IOException e5) {
                Log.e(TAG, e5.toString());
            }
        } catch (FileNotFoundException e6) {
            Log.e(TAG, e6.toString());
            try {
                is.close();
            } catch (IOException e7) {
                Log.e(TAG, e7.toString());
            }
        } catch (Throwable th2) {
            try {
                is.close();
            } catch (IOException e8) {
                Log.e(TAG, e8.toString());
            }
            throw th2;
        }
        return result;
    }

    public static String md5(String s) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(s.getBytes(), 0, s.length());
            return String.format("%1$032x", new Object[]{new BigInteger(1, m.digest())});
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isWiFiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getNetworkInfo(1);
        }
        if (networkInfo == null || !networkInfo.isConnected()) {
            return false;
        }
        return true;
    }

    public static boolean isEthernetConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getNetworkInfo(9);
        }
        return networkInfo != null && networkInfo.isConnected();
    }
}
