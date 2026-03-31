package com.ua.mytrinity.ui.task;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import com.ua.mytrinity.AppConfig;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.tv.Updates;
import java.text.MessageFormat;

public class CheckUpdateTask extends AsyncTask<Void, Void, Updates> {
    /* access modifiers changed from: private */
    public Context m_context;

    public CheckUpdateTask(Context context) {
        this.m_context = context;
    }

    /* access modifiers changed from: protected */
    public Updates doInBackground(Void... params) {
        return Updates.load();
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(final Updates result) {
        if (result != null && AppConfig.appVersionCode() < result.versionCode()) {
            Object[] args = {result.versionName()};
            new AlertDialog.Builder(this.m_context).setIconAttribute(16843605).setMessage(new MessageFormat(this.m_context.getText(R.string.update_message).toString()).format(args)).setNegativeButton(17039369, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setPositiveButton(17039379, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    new InstallUpdateTask(CheckUpdateTask.this.m_context).execute(new Uri[]{result.uri()});
                }
            }).show();
        }
    }
}
