package com.ua.mytrinity.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.ua.mytrinity.AppConfig;
import com.ua.mytrinity.media.Country;
import com.ua.mytrinity.media.Genre;
import com.ua.mytrinity.media.Link;
import com.ua.mytrinity.media.MediaDatabase;
import com.ua.mytrinity.media.Movie;
import com.ua.mytrinity.media.Person;
import com.ua.mytrinity.media.Release;
import com.ua.mytrinity.player.R;
import java.util.Iterator;

public class MovieInfoActivity extends Activity {
    public static final String MOVIE_ID_EXTRA = "com.ua.mytrinity.movie_id";
    private static final String MXVP = "com.mxtech.videoplayer.ad";
    private static final String MXVP_PRO = "com.mxtech.videoplayer.pro";
    private static final String TAG = "MovieInfoActivity";
    TextView m_actors;
    AppConfig m_config;
    TextView m_country;
    TextView m_descr;
    TextView m_director;
    TextView m_genre;
    Movie m_movie;
    ImageView m_poster;
    ProgressBar m_poster_spinner;
    TextView m_runtime;
    ProgressBar m_spinner;
    TextView m_title;
    TextView m_year;

    private class LoadPosterTask extends AsyncTask<Void, Void, Void> {
        private LoadPosterTask() {
        }

        /* access modifiers changed from: protected */
        public void onPreExecute() {
            MovieInfoActivity.this.m_poster_spinner.setVisibility(0);
        }

        /* access modifiers changed from: protected */
        public Void doInBackground(Void... params) {
            MovieInfoActivity.this.m_movie.loadPosterBitmap();
            return null;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Void result) {
            MovieInfoActivity.this.m_poster.setImageBitmap(MovieInfoActivity.this.m_movie.posterBitmap());
            MovieInfoActivity.this.m_poster_spinner.setVisibility(8);
        }
    }

    private class LoadInfoTask extends AsyncTask<Void, Void, Void> {
        private LoadInfoTask() {
        }

        /* access modifiers changed from: protected */
        public void onPreExecute() {
            MovieInfoActivity.this.m_spinner.setVisibility(0);
        }

        /* access modifiers changed from: protected */
        public Void doInBackground(Void... params) {
            MediaDatabase.getInstance().loadMovie(MovieInfoActivity.this.m_movie.id());
            return null;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Void result) {
            MovieInfoActivity.this.updateInfo();
            MovieInfoActivity.this.m_spinner.setVisibility(8);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movie_info);
        this.m_poster = (ImageView) findViewById(R.id.movie_poster);
        this.m_title = (TextView) findViewById(R.id.movie_title);
        this.m_descr = (TextView) findViewById(R.id.movie_descr);
        this.m_poster_spinner = (ProgressBar) findViewById(R.id.movie_poster_spinner);
        this.m_spinner = (ProgressBar) findViewById(R.id.movie_info_spinner);
        this.m_runtime = (TextView) findViewById(R.id.movie_info_runtime);
        this.m_year = (TextView) findViewById(R.id.movie_info_year);
        this.m_genre = (TextView) findViewById(R.id.movie_info_genre);
        this.m_country = (TextView) findViewById(R.id.movie_info_country);
        this.m_director = (TextView) findViewById(R.id.movie_info_director);
        this.m_actors = (TextView) findViewById(R.id.movie_info_actors);
        this.m_config = AppConfig.getAppConfig(this);
        int movie_id = getIntent().getIntExtra(MOVIE_ID_EXTRA, -1);
        if (movie_id == -1) {
            finish();
        }
        this.m_movie = MediaDatabase.getInstance().getMovie(movie_id);
        if (this.m_movie.posterBitmap() != null) {
            this.m_poster.setImageBitmap(this.m_movie.posterBitmap());
        } else {
            new LoadPosterTask().execute(new Void[0]);
        }
        updateInfo();
        new LoadInfoTask().execute(new Void[0]);
    }

    /* access modifiers changed from: private */
    public void updateInfo() {
        this.m_title.setText(this.m_movie.titleAll());
        this.m_descr.setText(this.m_movie.about());
        this.m_runtime.setText(this.m_movie.runtime());
        if (this.m_movie.director() != null) {
            this.m_director.setText(this.m_movie.director().title());
        } else {
            this.m_director.setText((CharSequence) null);
        }
        this.m_year.setText(Integer.toString(this.m_movie.year()));
        StringBuilder genres = new StringBuilder();
        Iterator<Genre> it = this.m_movie.genres().iterator();
        while (it.hasNext()) {
            Genre g = it.next();
            if (genres.length() > 0) {
                genres.append(", ");
            }
            genres.append(g.title());
        }
        this.m_genre.setText(genres.toString());
        StringBuilder actors = new StringBuilder();
        Iterator<Person> it2 = this.m_movie.actors().iterator();
        while (it2.hasNext()) {
            Person p = it2.next();
            if (actors.length() > 0) {
                actors.append(", ");
            }
            actors.append(p.title());
        }
        this.m_actors.setText(actors.toString());
        StringBuilder countries = new StringBuilder();
        Iterator<Country> it3 = this.m_movie.counries().iterator();
        while (it3.hasNext()) {
            Country c = it3.next();
            if (countries.length() > 0) {
                countries.append(", ");
            }
            countries.append(c.title());
        }
        this.m_country.setText(countries.toString());
    }

    public void play() {
        Log.i(TAG, "play movie_id " + this.m_movie.id());
        final Release r = this.m_movie.releases().get(0);
        if (r.links().size() > 1) {
            CharSequence[] items = new CharSequence[r.links().size()];
            int i = 0;
            Iterator<Link> it = r.links().iterator();
            while (it.hasNext()) {
                items[i] = it.next().title();
                i++;
            }
            new AlertDialog.Builder(this).setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MovieInfoActivity.this.playLink(r.links().get(which));
                }
            }).show();
            return;
        }
        playLink(r.links().get(0));
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, event.toString());
        if (event.getKeyCode() != 126 && event.getKeyCode() != 85 && event.getKeyCode() != 66 && event.getKeyCode() != 23) {
            return super.onKeyDown(keyCode, event);
        }
        play();
        return true;
    }

    /* access modifiers changed from: private */
    public void playLink(Link l) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(Uri.parse(l.url()), "application/x-mpegurl");
        intent.putExtra("title", this.m_movie.title());
        intent.setPackage(MXVP_PRO);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "mx player pro not found", e);
            intent.setPackage(MXVP);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e2) {
                Log.d(TAG, "mx player ad not found", e2);
                new AlertDialog.Builder(this).setMessage(R.string.mx_player_not_found).setPositiveButton(R.string.download_mx_player, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent("android.intent.action.VIEW");
                            intent.setData(Uri.parse("market://details?id=com.mxtech.videoplayer.ad"));
                            MovieInfoActivity.this.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            try {
                                Intent intent2 = new Intent("android.intent.action.VIEW");
                                intent2.setData(Uri.parse("http://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad"));
                                MovieInfoActivity.this.startActivity(intent2);
                            } catch (ActivityNotFoundException e2) {
                            }
                        }
                    }
                }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
    }

    public void onPlay(View v) {
        play();
    }
}
