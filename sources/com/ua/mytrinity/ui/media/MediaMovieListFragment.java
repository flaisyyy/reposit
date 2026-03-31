package com.ua.mytrinity.ui.media;

import android.app.ListFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.ua.mytrinity.media.Genre;
import com.ua.mytrinity.media.MediaDatabase;
import com.ua.mytrinity.media.Movie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

public class MediaMovieListFragment extends ListFragment implements AdapterView.OnItemClickListener {
    MovieAdapter m_adapter;
    String m_filter = "";
    HashSet<Genre> m_loaded_genres = new HashSet<>();
    OnMovieChangeListener m_onmovie_change_listener;
    SortOrder m_order = SortOrder.Alpha;

    public interface OnMovieChangeListener {
        void onMovieChange(Movie movie);
    }

    public enum SortOrder {
        Alpha,
        Year,
        Updated
    }

    class MovieAdapter extends BaseAdapter {
        ArrayList<Movie> m_data = new ArrayList<>();
        Genre m_genre;
        private LayoutInflater m_inflater;

        public MovieAdapter(Context context) {
            this.m_inflater = LayoutInflater.from(context);
            update((Genre) null);
        }

        public void update(Genre genre) {
            this.m_data.clear();
            if (genre != null) {
                this.m_data.addAll(genre.movies());
            }
            this.m_genre = genre;
            notifyDataSetChanged();
        }

        public int getCount() {
            return this.m_data.size();
        }

        public Object getItem(int pos) {
            return this.m_data.get(pos);
        }

        public long getItemId(int position) {
            return (long) this.m_data.get(position).id();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (view == null) {
                view = (TextView) this.m_inflater.inflate(17367043, parent, false);
            }
            Movie movie = this.m_data.get(position);
            view.setText(Integer.toString(position + 1) + ". [" + movie.year() + "] " + movie.titleAll());
            return view;
        }

        public void sort(Comparator<Movie> comparator) {
            Collections.sort(this.m_data, comparator);
            notifyDataSetChanged();
        }

        public void filter(String search) {
            if (search == null || search.isEmpty()) {
                update(this.m_genre);
                return;
            }
            this.m_data.clear();
            if (this.m_genre != null) {
                Pattern pattern = Pattern.compile(Pattern.quote(search), 2);
                Iterator<Movie> it = this.m_genre.movies().iterator();
                while (it.hasNext()) {
                    Movie movie = it.next();
                    if (pattern.matcher(movie.title()).find() || (movie.titleEn() != null && pattern.matcher(movie.titleEn()).find())) {
                        this.m_data.add(movie);
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    private class LoadMediaTask extends AsyncTask<Genre, Float, Genre> {
        private LoadMediaTask() {
        }

        /* access modifiers changed from: protected */
        public void onPreExecute() {
            MediaMovieListFragment.this.setListShown(false);
        }

        /* access modifiers changed from: protected */
        public Genre doInBackground(Genre... arg0) {
            MediaDatabase.getInstance().loadGenreMovies(arg0[0].id());
            return arg0[0];
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Genre result) {
            MediaMovieListFragment.this.m_loaded_genres.add(result);
            MediaMovieListFragment.this.m_adapter.update(result);
            MediaMovieListFragment.this.sort(MediaMovieListFragment.this.m_order);
            MediaMovieListFragment.this.setListShown(true);
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MovieAdapter movieAdapter = new MovieAdapter(getActivity());
        this.m_adapter = movieAdapter;
        setListAdapter(movieAdapter);
        getListView().setChoiceMode(1);
        getListView().setOnItemClickListener(this);
    }

    public void setGenre(Genre genre) {
        if (!this.m_loaded_genres.contains(genre)) {
            new LoadMediaTask().execute(new Genre[]{genre});
            return;
        }
        this.m_adapter.update(genre);
        sort(this.m_order);
    }

    public void setOnMovieChangeListener(OnMovieChangeListener listener) {
        this.m_onmovie_change_listener = listener;
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
        if (this.m_onmovie_change_listener != null) {
            this.m_onmovie_change_listener.onMovieChange((Movie) this.m_adapter.getItem(pos));
        }
        getListView().setItemChecked(pos, true);
    }

    private class MovieAlphaComparator implements Comparator<Movie> {
        private MovieAlphaComparator() {
        }

        public int compare(Movie lhs, Movie rhs) {
            return lhs.title().compareTo(rhs.title());
        }
    }

    private class MovieYearComparator implements Comparator<Movie> {
        private MovieYearComparator() {
        }

        public int compare(Movie lhs, Movie rhs) {
            if (lhs.year() < rhs.year()) {
                return 1;
            }
            if (lhs.year() > rhs.year()) {
                return -1;
            }
            return lhs.title().compareTo(rhs.title());
        }
    }

    private class MovieUpdateComparator implements Comparator<Movie> {
        private MovieUpdateComparator() {
        }

        public int compare(Movie lhs, Movie rhs) {
            return -lhs.updated().compareTo(rhs.updated());
        }
    }

    /* access modifiers changed from: private */
    public void sort(SortOrder order) {
        switch (order) {
            case Alpha:
                this.m_adapter.sort(new MovieAlphaComparator());
                return;
            case Year:
                this.m_adapter.sort(new MovieYearComparator());
                return;
            case Updated:
                this.m_adapter.sort(new MovieUpdateComparator());
                return;
            default:
                return;
        }
    }

    public void setSort(SortOrder order) {
        if (order != this.m_order) {
            this.m_order = order;
            sort(order);
        }
    }

    public void setFilter(String filter) {
        if (!filter.equals(this.m_filter)) {
            this.m_filter = filter;
            this.m_adapter.filter(filter);
        }
    }
}
