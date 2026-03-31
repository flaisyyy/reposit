package com.ua.mytrinity.ui.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import com.ua.mytrinity.player.R;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class InstallUpdateTask extends AsyncTask<Uri, Integer, Uri> {
    private static final String TAG = "InstallUpdateTask";
    private Context m_context;
    private ProgressDialog m_progress;

    public InstallUpdateTask(Context context) {
        this.m_context = context;
    }

    /* access modifiers changed from: protected */
    public void onPreExecute() {
        this.m_progress = new ProgressDialog(this.m_context);
        this.m_progress.setMessage(this.m_context.getText(R.string.downloading_update));
        this.m_progress.setProgressStyle(1);
        this.m_progress.setCancelable(false);
        this.m_progress.show();
    }

    /* access modifiers changed from: protected */
    public Uri doInBackground(Uri... params) {
        if (params.length == 0) {
            return null;
        }
        try {
            URL url = new URL(params[0].toString());
            URLConnection connection = url.openConnection();
            connection.connect();
            long lenghtOfFile = (long) connection.getContentLength();
            File out = new File(this.m_context.getExternalCacheDir(), params[0].getLastPathSegment());
            if (out.exists()) {
                out.delete();
            }
            out.createNewFile();
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(out);
            byte[] data = new byte[1024];
            long total = 0;
            while (true) {
                int count = input.read(data);
                if (count != -1) {
                    total += (long) count;
                    publishProgress(new Integer[]{Integer.valueOf((int) ((100 * total) / lenghtOfFile))});
                    output.write(data, 0, count);
                } else {
                    output.close();
                    input.close();
                    return Uri.fromFile(out);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Download failed", e);
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public void onProgressUpdate(Integer... progress) {
        this.m_progress.setProgress(progress[0].intValue());
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(Uri result) {
        this.m_progress.dismiss();
        if (result != null) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setDataAndType(result, "application/vnd.android.package-archive");
            this.m_context.startActivity(intent);
        }
    }
}
