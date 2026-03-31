package com.ua.mytrinity.ui.task;

import android.os.AsyncTask;
import android.util.Log;
import com.ua.mytrinity.tv.AuthorizationException;
import com.ua.mytrinity.tv.UserInfo;

public class LoadUserInfoTask extends AsyncTask<Void, Void, UserInfo> {
    private static final String TAG = "LoadUserInfoTask";
    private boolean auth_error;
    private OnUserInfoLoadedListener m_listener;

    public interface OnUserInfoLoadedListener {
        void OnUserInfoLoadError(boolean z);

        void OnUserInfoLoaded(UserInfo userInfo);
    }

    public LoadUserInfoTask() {
    }

    public LoadUserInfoTask(OnUserInfoLoadedListener listener) {
        this.m_listener = listener;
    }

    /* access modifiers changed from: protected */
    public UserInfo doInBackground(Void... params) {
        try {
            this.auth_error = false;
            return UserInfo.load();
        } catch (AuthorizationException e) {
            this.auth_error = true;
        } catch (Exception e2) {
            Log.e(TAG, "load error", e2);
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(UserInfo result) {
        if (this.m_listener == null) {
            return;
        }
        if (result != null) {
            this.m_listener.OnUserInfoLoaded(result);
        } else {
            this.m_listener.OnUserInfoLoadError(this.auth_error);
        }
    }
}
