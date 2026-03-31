package com.ua.mytrinity.ui.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.tv.AuthorizationException;
import com.ua.mytrinity.tv.TChannelList;

public class LoadChannelListTask extends AsyncTask<Void, Void, TChannelList> {
    private static final String TAG = "LoadChannelListTask";
    private boolean auth_error;
    private ProgressDialog m_channel_loading_dlg;
    private Context m_context;
    private OnChannelListLoadListener m_listener;

    public interface OnChannelListLoadListener {
        void onChannelListLoadError(boolean z);

        void onChannelListLoaded(TChannelList tChannelList);
    }

    public LoadChannelListTask(Context context) {
        this.m_context = context;
    }

    public LoadChannelListTask(Context context, OnChannelListLoadListener listener) {
        this.m_context = context;
        this.m_listener = listener;
    }

    public void setOnChannelListLoadListener(OnChannelListLoadListener listener) {
        this.m_listener = listener;
    }

    /* access modifiers changed from: protected */
    public void onPreExecute() {
        this.m_channel_loading_dlg = ProgressDialog.show(this.m_context, "", this.m_context.getText(R.string.loading), true, false);
    }

    /* access modifiers changed from: protected */
    public TChannelList doInBackground(Void... arg0) {
        Log.i(TAG, "LoadChannelListTask start");
        try {
            this.auth_error = false;
            return TChannelList.load();
        } catch (AuthorizationException e) {
            this.auth_error = true;
        } catch (Exception e2) {
            Log.e(TAG, TAG, e2);
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(TChannelList result) {
        Log.i(TAG, "LoadChannelListTask done");
        this.m_channel_loading_dlg.dismiss();
        if (result != null) {
            if (this.m_listener != null) {
                this.m_listener.onChannelListLoaded(result);
            }
        } else if (this.m_listener != null) {
            this.m_listener.onChannelListLoadError(this.auth_error);
        }
    }
}
