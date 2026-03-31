package com.google.android.exoplayer2.upstream;

import android.net.Uri;
import android.util.Base64;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import java.io.IOException;
import java.net.URLDecoder;

public final class DataSchemeDataSource implements DataSource {
    public static final String SCHEME_DATA = "data";
    private int bytesRead;
    private byte[] data;
    private DataSpec dataSpec;

    public long open(DataSpec dataSpec2) throws IOException {
        this.dataSpec = dataSpec2;
        Uri uri = dataSpec2.uri;
        String scheme = uri.getScheme();
        if (!SCHEME_DATA.equals(scheme)) {
            throw new ParserException("Unsupported scheme: " + scheme);
        }
        String[] uriParts = uri.getSchemeSpecificPart().split(",");
        if (uriParts.length > 2) {
            throw new ParserException("Unexpected URI format: " + uri);
        }
        String dataString = uriParts[1];
        if (uriParts[0].contains(";base64")) {
            try {
                this.data = Base64.decode(dataString, 0);
            } catch (IllegalArgumentException e) {
                throw new ParserException("Error while parsing Base64 encoded string: " + dataString, e);
            }
        } else {
            this.data = URLDecoder.decode(dataString, C.ASCII_NAME).getBytes();
        }
        return (long) this.data.length;
    }

    public int read(byte[] buffer, int offset, int readLength) {
        if (readLength == 0) {
            return 0;
        }
        int remainingBytes = this.data.length - this.bytesRead;
        if (remainingBytes == 0) {
            return -1;
        }
        int readLength2 = Math.min(readLength, remainingBytes);
        System.arraycopy(this.data, this.bytesRead, buffer, offset, readLength2);
        this.bytesRead += readLength2;
        return readLength2;
    }

    public Uri getUri() {
        if (this.dataSpec != null) {
            return this.dataSpec.uri;
        }
        return null;
    }

    public void close() throws IOException {
        this.dataSpec = null;
        this.data = null;
    }
}
