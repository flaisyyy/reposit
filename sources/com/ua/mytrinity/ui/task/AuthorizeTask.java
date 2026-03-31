package com.ua.mytrinity.ui.task;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.tv.Authorization;

public class AuthorizeTask extends AsyncTask<String, Void, Boolean> {
    private String def_login;
    private String def_password;
    private AlertDialog dialog;
    /* access modifiers changed from: private */
    public EditText login;
    /* access modifiers changed from: private */
    public Context m_context;
    /* access modifiers changed from: private */
    public OnAuthorizeListener m_listener;
    private ProgressDialog m_loading;
    private boolean m_try_null;
    /* access modifiers changed from: private */
    public EditText password;

    public interface OnAuthorizeListener {
        void onAuthorizeDone(boolean z);
    }

    public AuthorizeTask(Context context) {
        this.m_context = context;
    }

    public AuthorizeTask(Context context, OnAuthorizeListener listener) {
        this.m_context = context;
        this.m_listener = listener;
    }

    public void setAuthorizeListener(OnAuthorizeListener listener) {
        this.m_listener = listener;
    }

    public void start() {
        start(true);
    }

    public void start(boolean try_null) {
        this.m_try_null = try_null;
        if (try_null) {
            execute(new String[0]);
            return;
        }
        this.dialog = new AlertDialog.Builder(this.m_context).setTitle(R.string.authorization).setView(((LayoutInflater) this.m_context.getSystemService("layout_inflater")).inflate(R.layout.login_dialog, (ViewGroup) null)).setCancelable(false).setPositiveButton(R.string.login_dialog_submit, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                AuthorizeTask.this.execute(new String[]{AuthorizeTask.this.login.getText().toString(), AuthorizeTask.this.password.getText().toString()});
            }
        }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (AuthorizeTask.this.m_listener != null) {
                    AuthorizeTask.this.m_listener.onAuthorizeDone(false);
                }
            }
        }).create();
        this.dialog.show();
        this.login = (EditText) this.dialog.findViewById(R.id.login_dialog_username);
        this.password = (EditText) this.dialog.findViewById(R.id.login_dialog_password);
        if (this.def_login != null) {
            this.login.setText(this.def_login);
        }
        if (this.def_password != null) {
            this.password.setText(this.def_password);
        }
    }

    public void setLogin(String login2) {
        this.def_login = login2;
    }

    public void setPassword(String password2) {
        this.def_password = password2;
    }

    /* access modifiers changed from: protected */
    public void onPreExecute() {
        if (this.dialog != null) {
            this.dialog.dismiss();
        }
        this.m_loading = ProgressDialog.show(this.m_context, "", this.m_context.getText(R.string.authorization_process), true, false);
    }

    /* access modifiers changed from: protected */
    public Boolean doInBackground(String... params) {
        try {
            if (params.length == 2) {
                return Boolean.valueOf(Authorization.authorize(params[0], params[1]));
            }
            return Boolean.valueOf(Authorization.authorize());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(Boolean result) {
        this.m_loading.dismiss();
        if (result.booleanValue()) {
            if (this.m_listener != null) {
                this.m_listener.onAuthorizeDone(true);
            }
        } else if (this.m_try_null) {
            new AuthorizeTask(this.m_context, this.m_listener).start(false);
        } else {
            new AlertDialog.Builder(this.m_context).setIconAttribute(16843605).setMessage(R.string.authorization_error).setCancelable(false).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (AuthorizeTask.this.m_listener != null) {
                        AuthorizeTask.this.m_listener.onAuthorizeDone(false);
                    }
                }
            }).setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    AuthorizeTask t = new AuthorizeTask(AuthorizeTask.this.m_context, AuthorizeTask.this.m_listener);
                    t.setLogin(AuthorizeTask.this.login.getText().toString());
                    t.setPassword(AuthorizeTask.this.password.getText().toString());
                    t.start(false);
                }
            }).show();
        }
    }
}
