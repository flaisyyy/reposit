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
import android.widget.ListView;
import android.widget.TextView;
import com.ua.mytrinity.media.Genre;
import com.ua.mytrinity.media.MediaDatabase;
import java.util.ArrayList;
import java.util.Collections;

public class MediaGenreListFragment extends ListFragment implements AdapterView.OnItemClickListener {
    GenreAdapter m_adapter;
    ListView m_list;
    OnGenreListLoadedListener m_list_loaded_listener;
    OnGenreChangeListener m_ongenre_change_listener;

    public interface OnGenreChangeListener {
        void onGenreChange(Genre genre);
    }

    public interface OnGenreListLoadedListener {
        void OnGenreListLoaded(MediaGenreListFragment mediaGenreListFragment);
    }

    class GenreAdapter extends BaseAdapter {
        ArrayList<Genre> m_data = new ArrayList<>();
        private LayoutInflater m_inflater;

        public GenreAdapter(Context context) {
            this.m_inflater = LayoutInflater.from(context);
            update();
        }

        public void update() {
            this.m_data.clear();
            this.m_data.addAll(MediaDatabase.getInstance().genres());
            Collections.sort(this.m_data);
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
            view.setText(this.m_data.get(position).title());
            return view;
        }
    }

    private class LoadMediaTask extends AsyncTask<Void, Float, Void> {
        private LoadMediaTask() {
        }

        /* access modifiers changed from: protected */
        public void onPreExecute() {
            MediaGenreListFragment.this.setListShown(false);
        }

        /* access modifiers changed from: protected */
        public Void doInBackground(Void... arg0) {
            MediaDatabase.getInstance().loadGenreList();
            return null;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Void result) {
            MediaGenreListFragment.this.m_adapter.update();
            MediaGenreListFragment.this.setListShown(true);
            if (MediaGenreListFragment.this.m_list_loaded_listener != null) {
                MediaGenreListFragment.this.m_list_loaded_listener.OnGenreListLoaded(MediaGenreListFragment.this);
            }
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        GenreAdapter genreAdapter = new GenreAdapter(getActivity());
        this.m_adapter = genreAdapter;
        setListAdapter(genreAdapter);
        this.m_list = getListView();
        this.m_list.setChoiceMode(1);
        this.m_list.setOnItemClickListener(this);
        new LoadMediaTask().execute(new Void[0]);
    }

    public void setOnGenreChangeListener(OnGenreChangeListener listener) {
        this.m_ongenre_change_listener = listener;
    }

    public void setOnGenreListLoadedListener(OnGenreListLoadedListener listener) {
        this.m_list_loaded_listener = listener;
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
        if (this.m_ongenre_change_listener != null) {
            this.m_ongenre_change_listener.onGenreChange((Genre) this.m_adapter.getItem(pos));
        }
        this.m_list.setItemChecked(pos, true);
    }

    public void setItemIndex(int index) {
        if (index >= 0 && index < this.m_adapter.getCount()) {
            this.m_list.setItemChecked(index, true);
            if (this.m_ongenre_change_listener != null) {
                this.m_ongenre_change_listener.onGenreChange((Genre) this.m_adapter.getItem(index));
            }
        }
    }

    public Genre selectedGenre() {
        int pos = this.m_list.getSelectedItemPosition();
        if (pos < 0 || pos >= this.m_adapter.getCount()) {
            return null;
        }
        return (Genre) this.m_adapter.getItem(pos);
    }
}
