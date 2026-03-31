package com.ua.mytrinity.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class Movie {
    private static final String TAG = "Movie";
    private String m_about;
    private ArrayList<Person> m_actors = new ArrayList<>();
    private ArrayList<Country> m_counries = new ArrayList<>();
    private Person m_director;
    private ArrayList<Genre> m_genres = new ArrayList<>();
    private int m_id;
    private Bitmap m_poster;
    private String m_poster_url;
    private ArrayList<Release> m_releases = new ArrayList<>();
    private String m_runtime;
    private String m_title;
    private String m_title_en;
    private Date m_updated;
    private int m_year;

    public Movie(int id) {
        this.m_id = id;
    }

    public int id() {
        return this.m_id;
    }

    public String title() {
        return this.m_title;
    }

    public void setTitle(String title) {
        this.m_title = title;
    }

    public String titleEn() {
        return this.m_title_en;
    }

    public void setTitleEn(String title_en) {
        this.m_title_en = title_en;
    }

    public String titleAll() {
        StringBuilder builder = new StringBuilder(this.m_title);
        if (!(this.m_title_en == null || this.m_title.compareTo(this.m_title_en) == 0)) {
            builder.append(" / ").append(this.m_title_en);
        }
        return builder.toString();
    }

    public int year() {
        return this.m_year;
    }

    public void setYear(int year) {
        this.m_year = year;
    }

    public String runtime() {
        return this.m_runtime;
    }

    public void setRuntime(String runtime) {
        this.m_runtime = runtime;
    }

    public String about() {
        return this.m_about;
    }

    public void setAbout(String about) {
        this.m_about = about;
    }

    public Person director() {
        return this.m_director;
    }

    public void setDirectorId(int director_id) {
        this.m_director = MediaDatabase.getInstance().getPerson(director_id);
    }

    public ArrayList<Release> releases() {
        return this.m_releases;
    }

    public void addRelease(Release release) {
        if (release != null && !this.m_releases.contains(release)) {
            this.m_releases.add(release);
        }
    }

    public ArrayList<Genre> genres() {
        return this.m_genres;
    }

    public void addGenre(Genre genre) {
        if (genre != null && !this.m_genres.contains(genre)) {
            this.m_genres.add(genre);
            genre.movies().add(this);
        }
    }

    public void addGenreId(int genre_id) {
        addGenre(MediaDatabase.getInstance().getGenre(genre_id));
    }

    public void addActor(Person actor) {
        if (actor != null && !this.m_actors.contains(actor)) {
            this.m_actors.add(actor);
        }
    }

    public void addActorId(int person_id) {
        addActor(MediaDatabase.getInstance().getPerson(person_id));
    }

    public ArrayList<Person> actors() {
        return this.m_actors;
    }

    public void setPoster(String url) {
        this.m_poster_url = url;
    }

    public String poster() {
        return this.m_poster_url;
    }

    public void loadPosterBitmap() {
        if (this.m_poster == null) {
            try {
                HttpURLConnection get = (HttpURLConnection) new URL(this.m_poster_url).openConnection();
                if (get.getResponseCode() != 200) {
                    Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
                }
                this.m_poster = BitmapFactory.decodeStream(get.getInputStream());
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
            } catch (IllegalStateException e2) {
                Log.e(TAG, "IllegalStateException", e2);
            }
        }
    }

    public Bitmap posterBitmap() {
        return this.m_poster;
    }

    public void addCountry(Country c) {
        if (c != null && !this.m_counries.contains(c)) {
            this.m_counries.add(c);
        }
    }

    public void addCountryId(int id) {
        addCountry(MediaDatabase.getInstance().getCounry(id));
    }

    public ArrayList<Country> counries() {
        return this.m_counries;
    }

    public Date updated() {
        return this.m_updated;
    }

    public void setUpdated(Date date) {
        this.m_updated = date;
    }
}
