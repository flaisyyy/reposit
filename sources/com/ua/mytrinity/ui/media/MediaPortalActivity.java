package com.ua.mytrinity.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import com.ua.mytrinity.AppConfig;
import com.ua.mytrinity.media.Genre;
import com.ua.mytrinity.media.Movie;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.tv.UserInfo;
import com.ua.mytrinity.ui.media.MediaGenreListFragment;
import com.ua.mytrinity.ui.media.MediaMovieListFragment;
import com.ua.mytrinity.ui.task.AuthorizeTask;
import com.ua.mytrinity.ui.task.CheckUpdateTask;
import com.ua.mytrinity.ui.task.LoadUserInfoTask;

public class MediaPortalActivity extends Activity implements MediaGenreListFragment.OnGenreChangeListener, MediaGenreListFragment.OnGenreListLoadedListener, MediaMovieListFragment.OnMovieChangeListener, SearchView.OnQueryTextListener, LoadUserInfoTask.OnUserInfoLoadedListener, AuthorizeTask.OnAuthorizeListener {
    private static final String TAG = "MediaPortalActivity";
    MediaGenreListFragment m_genres;
    MediaMovieListFragment m_movies;
    MenuItem m_search_menu;
    MediaMovieListFragment.SortOrder m_sort = MediaMovieListFragment.SortOrder.Alpha;
    MediaMovieListFragment.SortOrder m_sort_user = MediaMovieListFragment.SortOrder.Alpha;
    boolean m_sort_user_override = false;
    private Runnable m_update_filter = new Runnable() {
        public void run() {
            MediaPortalActivity.this.m_movies.setFilter(MediaPortalActivity.this.searchView.getQuery().toString());
        }
    };
    SearchView searchView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_portal);
        AppConfig.getAppConfig(this);
        FragmentManager fm = getFragmentManager();
        this.m_genres = (MediaGenreListFragment) fm.findFragmentById(R.id.media_genre_list);
        this.m_genres.setOnGenreChangeListener(this);
        this.m_genres.setOnGenreListLoadedListener(this);
        this.m_movies = (MediaMovieListFragment) fm.findFragmentById(R.id.media_movie_list);
        this.m_movies.setOnMovieChangeListener(this);
    }

    public void onStart() {
        super.onStart();
        new LoadUserInfoTask(this).execute(new Void[0]);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.media_portal_options_menu, menu);
        this.m_search_menu = menu.findItem(R.id.action_search);
        this.searchView = (SearchView) this.m_search_menu.getActionView();
        this.searchView.setOnQueryTextListener(this);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        int id = -1;
        switch (this.m_sort) {
            case Alpha:
                id = R.id.action_sort_alpha;
                break;
            case Updated:
                id = R.id.action_sort_date_add;
                break;
            case Year:
                id = R.id.action_sort_year;
                break;
        }
        if (id != -1) {
            menu.findItem(R.id.action_sort).setIcon(menu.findItem(id).getIcon());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public void onGenreChange(Genre g) {
        Log.i(TAG, "Selected " + g.id() + " - '" + g.title() + "'");
        this.m_movies.setGenre(g);
        if (g.id() == 31) {
            this.m_sort = MediaMovieListFragment.SortOrder.Updated;
        } else {
            this.m_sort = this.m_sort_user;
        }
        this.m_movies.setSort(this.m_sort);
        invalidateOptionsMenu();
    }

    public void OnGenreListLoaded(MediaGenreListFragment list) {
        this.m_genres.setItemIndex(0);
    }

    public void onMovieChange(Movie m) {
        Intent movie_info = new Intent(this, MovieInfoActivity.class);
        movie_info.putExtra(MovieInfoActivity.MOVIE_ID_EXTRA, m.id());
        startActivity(movie_info);
    }

    public void onSort(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_alpha:
                this.m_sort = MediaMovieListFragment.SortOrder.Alpha;
                break;
            case R.id.action_sort_date_add:
                this.m_sort = MediaMovieListFragment.SortOrder.Updated;
                break;
            case R.id.action_sort_year:
                this.m_sort = MediaMovieListFragment.SortOrder.Year;
                break;
            default:
                return;
        }
        this.m_sort_user = this.m_sort;
        this.m_movies.setSort(this.m_sort);
        if (this.m_genres.selectedGenre() != null && this.m_genres.selectedGenre().id() == 31) {
            this.m_sort_user_override = true;
        }
        invalidateOptionsMenu();
    }

    public boolean onQueryTextChange(String newText) {
        this.m_movies.getView().removeCallbacks(this.m_update_filter);
        this.m_movies.getView().postDelayed(this.m_update_filter, 200);
        Log.i(TAG, "onQueryTextChange: " + newText);
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        this.m_movies.setFilter(query);
        this.searchView.clearFocus();
        Log.i(TAG, "onQueryTextSubmit: " + query);
        return true;
    }

    public void OnUserInfoLoaded(UserInfo info) {
        if (!info.isVOD()) {
            new AlertDialog.Builder(this).setIconAttribute(16843605).setMessage(R.string.account_has_no_vod).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MediaPortalActivity.this.finish();
                }
            }).setCancelable(false).show();
        } else if (info.isBlocked()) {
            new AlertDialog.Builder(this).setIconAttribute(16843605).setMessage(R.string.account_is_blocked).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MediaPortalActivity.this.finish();
                }
            }).setCancelable(false).show();
        } else {
            new CheckUpdateTask(this).execute(new Void[0]);
        }
    }

    public void OnUserInfoLoadError(boolean auth_error) {
        if (auth_error) {
            new AuthorizeTask(this, this).start();
        } else {
            new AlertDialog.Builder(this).setIconAttribute(16843605).setMessage(R.string.loading_userinfo_error).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MediaPortalActivity.this.finish();
                }
            }).setCancelable(false).show();
        }
    }

    public void onAuthorizeDone(boolean success) {
        if (success) {
            new LoadUserInfoTask(this).execute(new Void[0]);
        } else {
            finish();
        }
    }
}
