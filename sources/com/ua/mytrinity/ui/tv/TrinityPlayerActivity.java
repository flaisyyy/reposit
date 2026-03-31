package com.ua.mytrinity.ui.tv;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.ua.mytrinity.App;
import com.ua.mytrinity.AppConfig;
import com.ua.mytrinity.Utils;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.player.TapableSurfaceView;
import com.ua.mytrinity.player.TrinityPlayer;
import com.ua.mytrinity.tv.TChannel;
import com.ua.mytrinity.tv.TChannelList;
import com.ua.mytrinity.tv.TEpg;
import com.ua.mytrinity.tv.TEpgFactory;
import com.ua.mytrinity.tv.TEpgItem;
import com.ua.mytrinity.tv.TTimeOffset;
import com.ua.mytrinity.tv.UserInfo;
import com.ua.mytrinity.ui.media.MediaPortalActivity;
import com.ua.mytrinity.ui.settings.SettingsActivity;
import com.ua.mytrinity.ui.task.AuthorizeTask;
import com.ua.mytrinity.ui.task.CheckUpdateTask;
import com.ua.mytrinity.ui.task.LoadChannelListTask;
import com.ua.mytrinity.ui.task.LoadUserInfoTask;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class TrinityPlayerActivity extends Activity implements TapableSurfaceView.OnTapListener, TapableSurfaceView.OnScrollListener, TapableSurfaceView.OnEndScrollListener, AdapterView.OnItemClickListener, View.OnKeyListener, LoadChannelListTask.OnChannelListLoadListener, AuthorizeTask.OnAuthorizeListener, LoadUserInfoTask.OnUserInfoLoadedListener {
    private static final int BottomEpgHideTime = 8000;
    private static final int CenterTextHideTime = 8000;
    private static final int ChannelSwitchByNumInputTime = 2000;
    private static final int ChannelSwitchTime = 1000;
    private static final String PREF_BRIGHTNESS = "brightness";
    private static final String PREF_CHANNEL_INDEX = "channel_index";
    private static final String PREF_DISPLAY_MODE = "display_mode";
    private static final String PREF_VOLUME = "volume";
    private static final String TAG = "TrinityPlayerActivity";
    private static SimpleDateFormat m_time_format = new SimpleDateFormat("HH:mm", Locale.US);
    private Gallery m_act_gallery;
    private AdapterView.OnItemClickListener m_action_clicked_listener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            TrinityPlayerActivity.this.hideActions();
            switch (pos) {
                case 0:
                    TrinityPlayerActivity.this.realProgramMarker();
                    return;
                case 2:
                    TrinityPlayerActivity.this.toggleInterface();
                    return;
                case 3:
                    TrinityPlayerActivity.this.setCurrentOffset(TrinityPlayerActivity.this.searchMarkerType(MarkerProgramType.CURRENT, MarkerPositionType.BEGIN));
                    return;
                case 4:
                    TrinityPlayerActivity.this.startActivity(new Intent(TrinityPlayerActivity.this, MediaPortalActivity.class));
                    return;
                default:
                    return;
            }
        }
    };
    private ImageButton m_aspect_button;
    private AudioManager m_audio_manager;
    private float m_brigthness_margin = 0.1f;
    /* access modifiers changed from: private */
    public ProgressBar m_center_progress;
    /* access modifiers changed from: private */
    public TextView m_center_text;
    /* access modifiers changed from: private */
    public TChannelList m_channel_list;
    private Runnable m_channel_switch = new Runnable() {
        public void run() {
            TrinityPlayerActivity.this.playIndex(TrinityPlayerActivity.this.m_channel_switch_num - 1);
            int unused = TrinityPlayerActivity.this.m_channel_switch_num = 0;
        }
    };
    /* access modifiers changed from: private */
    public int m_channel_switch_num = 0;
    private AppConfig m_config;
    private ImageView m_current_channel_icon;
    private TextView m_current_channel_title;
    /* access modifiers changed from: private */
    public int m_current_index = 0;
    /* access modifiers changed from: private */
    public TTimeOffset m_current_offset;
    private Runnable m_do_play_index = new Runnable() {
        public void run() {
            TChannel channel = (TChannel) TrinityPlayerActivity.this.m_channel_list.elementAt(TrinityPlayerActivity.this.m_current_index);
            if (channel != null) {
                TrinityPlayerActivity.this.playUrl(TrinityPlayerActivity.this.m_channel_list.streamer() + channel.group(TrinityPlayerActivity.this.m_current_offset).toString());
            }
        }
    };
    private float m_dx = 0.0f;
    private float m_dy = 0.0f;
    /* access modifiers changed from: private */
    public TEpg m_epg;
    private TextView m_epg_current;
    /* access modifiers changed from: private */
    public RelativeLayout m_epg_layout;
    private Runnable m_epg_layout_hide = new Runnable() {
        public void run() {
            TrinityPlayerActivity.this.m_epg_layout.setVisibility(8);
        }
    };
    private TextView m_epg_next;
    private TextView m_epg_prev;
    private Runnable m_hide_center_text = new Runnable() {
        public void run() {
            TrinityPlayerActivity.this.m_center_text.setVisibility(8);
            TrinityPlayerActivity.this.m_center_progress.setVisibility(8);
        }
    };
    private boolean m_just_authorized = false;
    private TChannel m_last_channel = null;
    private LastOffsetMap m_last_offset;
    private ChannelListAdapter m_list_adapter;
    private ListView m_list_view;
    private RelativeLayout m_main_layout;
    private TrinityPlayer m_player;
    private ImageButton m_popup_button;
    private int m_start_progress = -1;
    private TextView m_time_now_label;
    private TextView m_time_offset_label;
    private View m_video_surface;
    private float m_volume_margin = 0.9f;

    private enum MarkerPositionType {
        BEGIN,
        MIDDLE,
        END
    }

    private enum MarkerProgramType {
        PREV,
        CURRENT,
        NEXT
    }

    /* access modifiers changed from: private */
    public void playUrl(String path) {
        this.m_player.stopPlayback();
        this.m_player.setVideoPath(path);
        this.m_player.start();
    }

    private class LoadChannelEpgTask extends AsyncTask<TChannel, Void, TEpg> {
        private LoadChannelEpgTask() {
        }

        /* access modifiers changed from: protected */
        public TEpg doInBackground(TChannel... params) {
            return TEpgFactory.load(params[0].id(), true);
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(TEpg result) {
            TrinityPlayerActivity.this.m_epg.merge(result);
            TrinityPlayerActivity.this.updateBottomView();
        }
    }

    private class LoadChannelIconTask extends AsyncTask<TChannel, Void, Void> {
        private LoadChannelIconTask() {
        }

        /* access modifiers changed from: protected */
        public Void doInBackground(TChannel... params) {
            params[0].loadIcon();
            return null;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Void result) {
            TrinityPlayerActivity.this.updateBottomView();
        }
    }

    private void playIndex(int index, boolean delayed) {
        boolean z = true;
        if (this.m_channel_list != null && index >= 0 && index < this.m_channel_list.size()) {
            this.m_list_view.setItemChecked(index, true);
            this.m_current_index = index;
            TChannel channel = (TChannel) this.m_channel_list.elementAt(index);
            if (this.m_last_channel != channel) {
                this.m_current_offset = this.m_last_offset.get(channel);
            }
            this.m_last_channel = channel;
            if (channel != null) {
                this.m_last_offset.put(channel, this.m_current_offset);
                if (channel.icon() == null) {
                    new LoadChannelIconTask().execute(new TChannel[]{channel});
                }
                if (!this.m_epg.hasTodayEpg(channel.id())) {
                    new LoadChannelEpgTask().execute(new TChannel[]{channel});
                }
                updateBottomView();
                if (this.m_list_view.getVisibility() == 0) {
                    z = false;
                }
                setBottomEpgVisible(z);
                this.m_video_surface.removeCallbacks(this.m_do_play_index);
                if (delayed) {
                    this.m_video_surface.postDelayed(this.m_do_play_index, 1000);
                } else {
                    this.m_video_surface.post(this.m_do_play_index);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateBottomView() {
        Date now;
        TChannel channel = (TChannel) this.m_channel_list.elementAt(this.m_current_index);
        if (channel != null) {
            this.m_current_channel_icon.setImageBitmap(channel.icon());
            if (channel.icon() == null) {
                new LoadChannelIconTask().execute(new TChannel[]{channel});
            }
            this.m_current_channel_title.setText((this.m_current_index + 1) + ". " + channel.title());
            if (this.m_epg.hasTodayEpg(channel.id())) {
                TEpgItem epg_prev = this.m_epg.getPrevForChannel(channel.id(), this.m_current_offset);
                TEpgItem epg_current = this.m_epg.getCurrentForChannel(channel.id(), this.m_current_offset);
                TEpgItem epg_next = this.m_epg.getNextForChannel(channel.id(), this.m_current_offset);
                this.m_epg_prev.setText(epg_prev == null ? "" : epg_prev.captionStart() + ": " + epg_prev.title());
                this.m_epg_current.setText(epg_current == null ? "" : epg_current.captionStart() + ": " + epg_current.title());
                this.m_epg_next.setText(epg_next == null ? "" : epg_next.captionStart() + ": " + epg_next.title());
            } else {
                this.m_epg_prev.setText("");
                this.m_epg_current.setText("");
                this.m_epg_next.setText("");
            }
            if (this.m_current_offset == null) {
                now = new Date();
            } else {
                now = new Date(System.currentTimeMillis() - ((long) (this.m_current_offset.offsetSec() * 1000)));
            }
            this.m_time_offset_label.setText(offsetLabelText(this.m_current_offset));
            this.m_time_now_label.setText(m_time_format.format(now));
        }
    }

    private CharSequence offsetLabelText(TTimeOffset offset) {
        if (offset == null) {
            return getText(R.string.now_label);
        }
        return String.valueOf((-offset.offsetSec()) / 60) + " " + getText(R.string.minutes);
    }

    /* access modifiers changed from: private */
    public void playIndex(int index) {
        playIndex(index, false);
    }

    private void setBottomEpgVisible(boolean visible) {
        if (visible) {
            this.m_epg_layout.setVisibility(0);
            this.m_epg_layout.removeCallbacks(this.m_epg_layout_hide);
            this.m_epg_layout.postDelayed(this.m_epg_layout_hide, 8000);
            return;
        }
        this.m_epg_layout.setVisibility(8);
        this.m_epg_layout.removeCallbacks(this.m_epg_layout_hide);
    }

    public void onAuthorizeDone(boolean success) {
        if (success) {
            this.m_just_authorized = true;
            new LoadUserInfoTask(this).execute(new Void[0]);
            return;
        }
        finish();
    }

    public void onChannelListLoadError(boolean auth_error) {
        if (auth_error) {
            new AuthorizeTask(this, this).start();
        } else {
            new AlertDialog.Builder(this).setIconAttribute(16843605).setMessage(R.string.loading_channels_error).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    TrinityPlayerActivity.this.finish();
                }
            }).setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    new LoadChannelListTask(TrinityPlayerActivity.this, TrinityPlayerActivity.this).execute(new Void[0]);
                }
            }).setCancelable(false).show();
        }
    }

    public void onChannelListLoaded(TChannelList list) {
        this.m_channel_list = list;
        this.m_config.setChannelList(this.m_channel_list);
        ListView listView = this.m_list_view;
        ChannelListAdapter channelListAdapter = new ChannelListAdapter(this, this.m_last_offset);
        this.m_list_adapter = channelListAdapter;
        listView.setAdapter(channelListAdapter);
        playIndex(this.m_current_index, true);
        this.m_list_view.setSelection(this.m_current_index);
        new CheckUpdateTask(this).execute(new Void[0]);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoview);
        this.m_config = AppConfig.getAppConfig(this);
        this.m_epg = this.m_config.epg();
        this.m_main_layout = (RelativeLayout) findViewById(R.id.TrinityPlayerActivityLayout);
        this.m_list_view = (ListView) findViewById(R.id.channels_list_view);
        this.m_list_view.setChoiceMode(1);
        this.m_list_view.setOnItemClickListener(this);
        this.m_popup_button = (ImageButton) findViewById(R.id.popup_button);
        this.m_aspect_button = (ImageButton) findViewById(R.id.aspect_btn);
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200);
        transition.setStartDelay(2, 0);
        transition.setStartDelay(3, 0);
        transition.setStartDelay(0, 0);
        transition.setStartDelay(1, 0);
        this.m_main_layout.setLayoutTransition(transition);
        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(50);
        set.addAnimation(animation);
        Animation animation2 = new TranslateAnimation(1, 0.0f, 1, 0.0f, 1, -1.0f, 1, 0.0f);
        animation2.setDuration(100);
        set.addAnimation(animation2);
        this.m_list_view.setLayoutAnimation(new LayoutAnimationController(set, 0.5f));
        createPlayer();
        this.m_epg_layout = (RelativeLayout) findViewById(R.id.epg_view);
        this.m_current_channel_icon = (ImageView) findViewById(R.id.channel_icon);
        this.m_current_channel_title = (TextView) findViewById(R.id.channel_title);
        this.m_epg_prev = (TextView) findViewById(R.id.epg_view_title_0);
        this.m_epg_current = (TextView) findViewById(R.id.epg_view_title_1);
        this.m_epg_next = (TextView) findViewById(R.id.epg_view_title_2);
        this.m_time_offset_label = (TextView) findViewById(R.id.time_offset);
        this.m_time_now_label = (TextView) findViewById(R.id.time_now);
        this.m_epg_layout.setBackgroundResource(R.color.channel_list_bg);
        this.m_list_view.setBackgroundResource(R.color.channel_list_bg);
        this.m_last_offset = new LastOffsetMap();
        this.m_center_text = (TextView) findViewById(R.id.center_text);
        this.m_center_text.setAnimation(new AlphaAnimation(0.0f, 1.0f));
        this.m_center_progress = (ProgressBar) findViewById(R.id.center_progress);
        this.m_act_gallery = (Gallery) findViewById(R.id.act_gallery);
        this.m_act_gallery.setAdapter(new ActionImageAdapter(this));
        this.m_act_gallery.setOnItemClickListener(this.m_action_clicked_listener);
        this.m_audio_manager = (AudioManager) getSystemService(MimeTypes.BASE_TYPE_AUDIO);
        this.m_main_layout.setOnKeyListener(this);
    }

    private void createPlayer() {
        this.m_video_surface = ((App) getApplication()).getPlayerBuilder().setActivity(this).build();
        this.m_player = (TrinityPlayer) this.m_video_surface;
        ((FrameLayout) findViewById(R.id.video_layout)).addView(this.m_video_surface, new FrameLayout.LayoutParams(-2, -2, 17));
        TapableSurfaceView player = (TapableSurfaceView) findViewById(R.id.tapable_surface);
        player.setOnTapListener(this);
        player.setOnScrollListener(this);
        player.setOnEndScrollListener(this);
        player.setOnKeyListener(this);
        TapableSurfaceView player2 = this.m_player;
        player2.setOnTapListener(this);
        player2.setOnScrollListener(this);
        player2.setOnEndScrollListener(this);
        player2.setOnKeyListener(this);
    }

    private void showActions() {
        this.m_act_gallery.setVisibility(0);
        this.m_act_gallery.setSelection(2);
        this.m_act_gallery.requestFocus();
    }

    /* access modifiers changed from: private */
    public void hideActions() {
        this.m_act_gallery.setVisibility(8);
        this.m_video_surface.requestFocus();
    }

    public void onPopupButtonClick(View button) {
        PopupMenu popup = new PopupMenu(this, button);
        popup.getMenuInflater().inflate(R.menu.player_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = null;
                switch (item.getItemId()) {
                    case R.id.show_all_tv_program:
                        intent = new Intent(TrinityPlayerActivity.this, TvProgramActivity.class);
                        intent.putExtra("channel", 1);
                        break;
                    case R.id.show_mediaportal:
                        intent = new Intent(TrinityPlayerActivity.this, MediaPortalActivity.class);
                        break;
                    case R.id.show_settings:
                        intent = new Intent(TrinityPlayerActivity.this, SettingsActivity.class);
                        break;
                }
                if (intent != null) {
                    TrinityPlayerActivity.this.startActivity(intent);
                }
                return true;
            }
        });
        popup.show();
    }

    public void onAspectButtonClick(View button) {
        TrinityPlayer.DisplayMode mode = this.m_player.getDisplayMode().getNext();
        this.m_player.setDisplayMode(mode);
        showCenterText(getText(new int[]{R.string.surface_best_fit, R.string.surface_fit_horizontal, R.string.surface_fit_vertical, R.string.surface_fill, R.string.surface_original}[mode.ordinal()]));
    }

    private void showCenterText(CharSequence text) {
        this.m_center_text.setText(text);
        this.m_center_text.setVisibility(0);
        this.m_center_text.removeCallbacks(this.m_hide_center_text);
        this.m_center_text.postDelayed(this.m_hide_center_text, 8000);
    }

    public void nextChannel() {
        nextChannel(false);
    }

    public void nextChannel(boolean delayed) {
        if (this.m_channel_list != null) {
            this.m_current_index = Math.max(this.m_current_index - 1, 0);
            playIndex(this.m_current_index, delayed);
        }
    }

    public void prevChannel() {
        prevChannel(false);
    }

    public void prevChannel(boolean delayed) {
        if (this.m_channel_list != null) {
            this.m_current_index = Math.min(this.m_current_index + 1, this.m_channel_list.size() - 1);
            playIndex(this.m_current_index, delayed);
        }
    }

    private void setCurrentOffset(TTimeOffset offset, boolean delayed) {
        if (offset != this.m_current_offset) {
            this.m_current_offset = offset;
            this.m_list_adapter.invalidate();
            this.m_list_view.invalidateViews();
            playIndex(this.m_current_index, delayed);
        }
    }

    /* access modifiers changed from: private */
    public void setCurrentOffset(TTimeOffset offset) {
        setCurrentOffset(offset, false);
    }

    public void nextTimeMarker() {
        nextTimeMarker(true);
    }

    public void nextTimeMarker(boolean delayed) {
        int index = this.m_current_offset == null ? 0 : ((TChannel) this.m_channel_list.elementAt(this.m_current_index)).offset_list().indexOf(this.m_current_offset) + 1;
        if (index >= 0 && index < ((TChannel) this.m_channel_list.elementAt(this.m_current_index)).offset_list().size()) {
            setCurrentOffset(((TChannel) this.m_channel_list.elementAt(this.m_current_index)).offset_list().get(index), delayed);
        }
    }

    public void prevTimeMarker() {
        prevTimeMarker(true);
    }

    public void prevTimeMarker(boolean delayed) {
        int index = this.m_current_offset == null ? -1 : ((TChannel) this.m_channel_list.elementAt(this.m_current_index)).offset_list().indexOf(this.m_current_offset) - 1;
        if (index >= 0 && index < ((TChannel) this.m_channel_list.elementAt(this.m_current_index)).offset_list().size()) {
            setCurrentOffset(((TChannel) this.m_channel_list.elementAt(this.m_current_index)).offset_list().get(index), delayed);
        } else if (index == -1) {
            setCurrentOffset((TTimeOffset) null, true);
        }
    }

    private TTimeOffset searchMarker(TEpgItem epg_item, float position, boolean only_between_start_stop) {
        float min_p = Math.abs(epg_item.progress() - position);
        TTimeOffset min_offset = null;
        Iterator<TTimeOffset> it = ((TChannel) this.m_channel_list.elementAt(this.m_current_index)).offset_list().iterator();
        while (it.hasNext()) {
            TTimeOffset offset = it.next();
            if ((only_between_start_stop && epg_item.isNow(offset)) || !only_between_start_stop) {
                float p = Math.abs(epg_item.progress(offset) - position);
                if (p < min_p) {
                    min_p = p;
                    min_offset = offset;
                }
            }
        }
        return min_offset;
    }

    /* access modifiers changed from: private */
    public TTimeOffset searchMarkerType(MarkerProgramType program, MarkerPositionType pos) {
        TChannel channel = (TChannel) this.m_channel_list.elementAt(this.m_current_index);
        TEpgItem epg_item = null;
        float p = 0.0f;
        switch (program) {
            case PREV:
                epg_item = this.m_epg.getPrevForChannel(channel.id(), this.m_current_offset);
                break;
            case CURRENT:
                epg_item = this.m_epg.getCurrentForChannel(channel.id(), this.m_current_offset);
                break;
            case NEXT:
                epg_item = this.m_epg.getNextForChannel(channel.id(), this.m_current_offset);
                break;
        }
        switch (pos) {
            case BEGIN:
                p = 0.0f;
                break;
            case MIDDLE:
                p = 0.5f;
                break;
            case END:
                p = 1.0f;
                break;
        }
        return searchMarker(epg_item, p, true);
    }

    public boolean isBeginProgramMarker() {
        if (searchMarker(this.m_epg.getCurrentForChannel(((TChannel) this.m_channel_list.elementAt(this.m_current_index)).id(), this.m_current_offset), 0.0f, true) == this.m_current_offset) {
            return true;
        }
        return false;
    }

    public boolean isEndProgramMarker() {
        if (searchMarker(this.m_epg.getCurrentForChannel(((TChannel) this.m_channel_list.elementAt(this.m_current_index)).id(), this.m_current_offset), 1.0f, true) == this.m_current_offset) {
            return true;
        }
        return false;
    }

    public void realProgramMarker() {
        setCurrentOffset((TTimeOffset) null);
    }

    private boolean interfaceShowed() {
        return this.m_list_view.getVisibility() == 0;
    }

    /* access modifiers changed from: private */
    public void toggleInterface() {
        this.m_list_view.setVisibility(this.m_list_view.getVisibility() == 0 ? 8 : 0);
        this.m_popup_button.setVisibility(this.m_list_view.getVisibility());
        this.m_aspect_button.setVisibility(this.m_list_view.getVisibility());
        if (this.m_list_view.getVisibility() == 0) {
            this.m_epg_layout.setVisibility(8);
            this.m_epg_layout.removeCallbacks(this.m_epg_layout_hide);
            this.m_list_view.requestFocus();
            this.m_list_view.setSelection(this.m_current_index);
            return;
        }
        this.m_video_surface.requestFocus();
    }

    public void onStart() {
        super.onStart();
        if (Utils.isWiFiConnected(this) || Utils.isEthernetConnected(this)) {
            startLoading();
        } else {
            new AlertDialog.Builder(this).setTitle(getString(R.string.no_network_title)).setMessage(R.string.no_wifi).setPositiveButton(17039379, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    TrinityPlayerActivity.this.finish();
                }
            }).setNegativeButton(R.string.no_network_continue, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    TrinityPlayerActivity.this.startLoading();
                }
            }).create().show();
        }
    }

    /* access modifiers changed from: private */
    public void startLoading() {
        loadSettings();
        if (this.m_channel_list == null) {
            new LoadUserInfoTask(this).execute(new Void[0]);
        } else {
            playIndex(this.m_current_index);
        }
        this.m_list_view.requestFocus();
    }

    public void onStop() {
        super.onStop();
        this.m_player.suspend();
        saveSettings();
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (e1.getX() < ((float) this.m_video_surface.getWidth()) * this.m_brigthness_margin) {
            BrightnessBar bar = getBrightnessBar();
            if (this.m_start_progress == -1) {
                this.m_start_progress = bar.getCurrent();
                this.m_dy = 0.0f;
                bar.disableHide();
            }
            this.m_dy += distanceY;
            bar.setCurrent(this.m_start_progress + ((int) ((((float) (bar.getMax() * 4)) * this.m_dy) / ((float) this.m_video_surface.getHeight()))));
        } else if (e1.getX() < ((float) this.m_video_surface.getWidth()) * this.m_volume_margin) {
            if (this.m_start_progress == -1) {
                this.m_start_progress = 0;
                this.m_dy = 0.0f;
                this.m_dx = 0.0f;
            }
            this.m_dx += distanceX;
            this.m_dy += distanceY;
            float ky = (float) (this.m_video_surface.getHeight() / 20);
            float kx = (float) (this.m_video_surface.getWidth() / 20);
            if (Math.abs(this.m_dy) > Math.abs(this.m_dx)) {
                if (Math.abs(this.m_dy) > ky) {
                    if (this.m_dy < 0.0f) {
                        prevChannel(true);
                        this.m_dy += ky;
                    } else {
                        nextChannel(true);
                        this.m_dy -= ky;
                    }
                    this.m_dx = 0.0f;
                    this.m_dy = 0.0f;
                    showCenterPorgress();
                }
            } else if (Math.abs(this.m_dx) > kx) {
                if (this.m_dx < 0.0f) {
                    prevTimeMarker(true);
                    this.m_dx += kx;
                } else {
                    nextTimeMarker(true);
                    this.m_dx -= kx;
                }
                this.m_dx = 0.0f;
                this.m_dy = 0.0f;
                showCenterPorgress();
            }
        } else {
            SoundBar bar2 = getSoundBar();
            if (this.m_start_progress == -1) {
                this.m_start_progress = bar2.getCurrent();
                this.m_dy = 0.0f;
                bar2.disableHide();
            }
            this.m_dy += distanceY;
            bar2.setCurrent(this.m_start_progress + ((int) ((((float) (bar2.getMax() * 4)) * this.m_dy) / ((float) this.m_video_surface.getHeight()))));
        }
    }

    public void onTap(MotionEvent event) {
        toggleInterface();
    }

    public void onScrollEnd(float startX, float startY, float distanceX, float distanceY) {
        if (startX < ((float) this.m_video_surface.getWidth()) * this.m_brigthness_margin) {
            getBrightnessBar().enableHide();
        } else if (startX >= ((float) this.m_video_surface.getWidth()) * this.m_volume_margin) {
            getSoundBar().enableHide();
        }
        this.m_start_progress = -1;
    }

    public void onItemClick(AdapterView<?> adapterView, View arg1, int pos, long id) {
        if (this.m_current_index != pos) {
            playIndex(pos);
        } else {
            toggleInterface();
        }
    }

    private void showCenterPorgress() {
        TEpgItem current = this.m_epg.getCurrentForChannel(((TChannel) this.m_channel_list.get(this.m_current_index)).id(), this.m_current_offset);
        StringBuilder label = new StringBuilder();
        if (current != null) {
            if (this.m_current_offset != null) {
                if (isBeginProgramMarker()) {
                    label.append(getText(R.string.caption_begin));
                } else if (isEndProgramMarker()) {
                    label.append(getText(R.string.caption_end));
                }
            }
            label.append(10);
            label.append(current.title());
            this.m_center_progress.setVisibility(0);
            this.m_center_progress.setProgress((int) (current.progress(this.m_current_offset) * 100.0f));
        } else {
            this.m_hide_center_text.run();
        }
        showCenterText(label.toString());
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.i(TAG, "onKey: " + event);
        if (event.getAction() == 0 && event.getRepeatCount() == 0) {
            boolean handled = true;
            switch (event.getKeyCode()) {
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    this.m_video_surface.removeCallbacks(this.m_channel_switch);
                    this.m_channel_switch_num = (this.m_channel_switch_num * 10) + (event.getKeyCode() - 7);
                    showCenterText(String.valueOf(this.m_channel_switch_num));
                    if (this.m_channel_switch_num * 10 <= this.m_channel_list.size()) {
                        this.m_video_surface.postDelayed(this.m_channel_switch, 2000);
                        break;
                    } else {
                        this.m_channel_switch.run();
                        break;
                    }
                case 21:
                    if (this.m_current_offset != null) {
                        if (!isBeginProgramMarker()) {
                            if (!isEndProgramMarker()) {
                                setCurrentOffset(searchMarkerType(MarkerProgramType.CURRENT, MarkerPositionType.BEGIN));
                                break;
                            } else {
                                setCurrentOffset(searchMarkerType(MarkerProgramType.CURRENT, MarkerPositionType.MIDDLE));
                                break;
                            }
                        } else {
                            setCurrentOffset(searchMarkerType(MarkerProgramType.PREV, MarkerPositionType.END));
                            break;
                        }
                    } else {
                        TTimeOffset off = searchMarkerType(MarkerProgramType.CURRENT, MarkerPositionType.BEGIN);
                        if (off == null) {
                            off = searchMarkerType(MarkerProgramType.PREV, MarkerPositionType.END);
                        }
                        setCurrentOffset(off);
                        break;
                    }
                case 22:
                    if (!isBeginProgramMarker()) {
                        if (!isEndProgramMarker()) {
                            setCurrentOffset(searchMarkerType(MarkerProgramType.CURRENT, MarkerPositionType.END));
                            break;
                        } else {
                            setCurrentOffset(searchMarkerType(MarkerProgramType.NEXT, MarkerPositionType.BEGIN));
                            break;
                        }
                    } else {
                        setCurrentOffset(searchMarkerType(MarkerProgramType.CURRENT, MarkerPositionType.MIDDLE));
                        break;
                    }
                case 23:
                    showActions();
                    break;
                case 82:
                    onPopupButtonClick(this.m_popup_button);
                    break;
                case 84:
                    setBottomEpgVisible(this.m_list_view.getVisibility() != 0);
                    break;
                case 85:
                    setCurrentOffset(searchMarkerType(MarkerProgramType.CURRENT, MarkerPositionType.BEGIN));
                    break;
                case 87:
                    realProgramMarker();
                    break;
                case 89:
                    nextTimeMarker();
                    break;
                case 90:
                    prevTimeMarker();
                    break;
                default:
                    handled = false;
                    break;
            }
            switch (event.getKeyCode()) {
                case 21:
                case 22:
                case 85:
                case 87:
                case 89:
                case 90:
                    showCenterPorgress();
                    break;
            }
            if (handled) {
                return true;
            }
        }
        if (event.getAction() == 0) {
            boolean handled2 = true;
            switch (event.getKeyCode()) {
                case 19:
                    if (!interfaceShowed()) {
                        playIndex(this.m_list_view.getCheckedItemPosition() - 1, true);
                        break;
                    }
                    break;
                case 20:
                    if (!interfaceShowed()) {
                        playIndex(this.m_list_view.getCheckedItemPosition() + 1, true);
                        break;
                    }
                    break;
                case 24:
                    getSoundBar().incrementProgressBy(1);
                    break;
                case 25:
                    getSoundBar().incrementProgressBy(-1);
                    break;
                default:
                    handled2 = false;
                    break;
            }
            if (handled2) {
                return true;
            }
        }
        if (event.getAction() == 1) {
            boolean handled3 = true;
            switch (event.getKeyCode()) {
                case 82:
                    onPopupButtonClick(this.m_popup_button);
                    break;
                default:
                    handled3 = false;
                    break;
            }
            if (handled3) {
                return true;
            }
        }
        return false;
    }

    public void OnUserInfoLoaded(UserInfo info) {
        if (info.isBlocked()) {
            new AlertDialog.Builder(this).setIconAttribute(16843605).setMessage(R.string.account_is_blocked).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    TrinityPlayerActivity.this.finish();
                }
            }).setCancelable(false).show();
        } else if (this.m_just_authorized) {
            new AlertDialog.Builder(this).setMessage(info.accountId() + ": " + info.fullName()).setPositiveButton(17039379, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    new LoadChannelListTask(TrinityPlayerActivity.this, TrinityPlayerActivity.this).execute(new Void[0]);
                }
            }).create().show();
        } else {
            new LoadChannelListTask(this, this).execute(new Void[0]);
        }
    }

    public void OnUserInfoLoadError(boolean auth_error) {
        if (auth_error) {
            new AuthorizeTask(this, this).start();
        } else {
            new AlertDialog.Builder(this).setIconAttribute(16843605).setMessage(R.string.loading_userinfo_error).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    TrinityPlayerActivity.this.finish();
                }
            }).setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    new LoadUserInfoTask(TrinityPlayerActivity.this).execute(new Void[0]);
                }
            }).setCancelable(false).show();
        }
    }

    private SoundBar getSoundBar() {
        SoundBar bar = (SoundBar) findViewById(R.id.soundBar);
        if (bar != null) {
            return bar;
        }
        SoundBar bar2 = new SoundBar(this);
        bar2.setId(R.id.soundBar);
        bar2.setAudioManager(this.m_audio_manager);
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(-2, -2);
        p.addRule(13, -1);
        ((RelativeLayout) findViewById(R.id.TrinityPlayerActivityLayout)).addView(bar2, p);
        return bar2;
    }

    private BrightnessBar getBrightnessBar() {
        BrightnessBar bar = (BrightnessBar) findViewById(R.id.brightnessBar);
        if (bar != null) {
            return bar;
        }
        BrightnessBar bar2 = new BrightnessBar(this);
        bar2.setId(R.id.brightnessBar);
        bar2.setWindow(getWindow());
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(-2, -2);
        p.addRule(13, -1);
        ((RelativeLayout) findViewById(R.id.TrinityPlayerActivityLayout)).addView(bar2, p);
        return bar2;
    }

    private void loadSettings() {
        SharedPreferences preferences = getPreferences(0);
        Window window = getWindow();
        WindowManager.LayoutParams attrs = window.getAttributes();
        this.m_audio_manager.setStreamVolume(3, preferences.getInt(PREF_VOLUME, this.m_audio_manager.getStreamVolume(3)), 0);
        attrs.screenBrightness = preferences.getFloat(PREF_BRIGHTNESS, attrs.screenBrightness);
        this.m_current_index = preferences.getInt(PREF_CHANNEL_INDEX, this.m_current_index);
        window.setAttributes(attrs);
        try {
            this.m_player.setDisplayMode(TrinityPlayer.DisplayMode.valueOf(preferences.getString(PREF_DISPLAY_MODE, TrinityPlayer.DisplayMode.BEST_FIT.toString())));
        } catch (Exception e) {
            this.m_player.setDisplayMode(TrinityPlayer.DisplayMode.BEST_FIT);
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor preferences = getPreferences(0).edit();
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        preferences.putInt(PREF_VOLUME, this.m_audio_manager.getStreamVolume(3));
        preferences.putFloat(PREF_BRIGHTNESS, attrs.screenBrightness);
        preferences.putInt(PREF_CHANNEL_INDEX, this.m_current_index);
        preferences.putString(PREF_DISPLAY_MODE, this.m_player.getDisplayMode().toString());
        preferences.commit();
    }
}
