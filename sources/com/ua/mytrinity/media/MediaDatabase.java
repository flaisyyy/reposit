package com.ua.mytrinity.media;

import android.annotation.SuppressLint;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.sax.TextElementListener;
import android.util.Log;
import android.util.Xml;
import com.google.android.exoplayer2.util.MimeTypes;
import com.ua.mytrinity.AppConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressLint({"UseSparseArrays"})
public class MediaDatabase {
    private static final String TAG = "MediaDatabase";
    private static final MediaDatabase m_instance = new MediaDatabase();
    private HashMap<Integer, Company> m_companies = new HashMap<>();
    private HashMap<Integer, Country> m_countries = new HashMap<>();
    private HashMap<Integer, Genre> m_genres = new HashMap<>();
    private HashMap<Integer, Movie> m_movies = new HashMap<>();
    private HashMap<Integer, Person> m_persons = new HashMap<>();

    private MediaDatabase() {
    }

    public static MediaDatabase getInstance() {
        return m_instance;
    }

    public Genre getGenre(int id) {
        Integer key = Integer.valueOf(id);
        if (this.m_genres.containsKey(key)) {
            return this.m_genres.get(key);
        }
        Genre value = new Genre(id);
        this.m_genres.put(key, value);
        return value;
    }

    public Person getPerson(int id) {
        Integer key = Integer.valueOf(id);
        if (this.m_persons.containsKey(key)) {
            return this.m_persons.get(key);
        }
        Person value = new Person(id);
        this.m_persons.put(key, value);
        return value;
    }

    public Country getCounry(int id) {
        Integer key = Integer.valueOf(id);
        if (this.m_countries.containsKey(key)) {
            return this.m_countries.get(key);
        }
        Country value = new Country(id);
        this.m_countries.put(key, value);
        return value;
    }

    public Movie getMovie(int id) {
        Integer key = Integer.valueOf(id);
        if (this.m_movies.containsKey(key)) {
            return this.m_movies.get(key);
        }
        Movie value = new Movie(id);
        this.m_movies.put(key, value);
        return value;
    }

    public Company getCompany(int id) {
        Integer key = Integer.valueOf(id);
        if (this.m_companies.containsKey(key)) {
            return this.m_companies.get(key);
        }
        Company value = new Company(id);
        this.m_companies.put(key, value);
        return value;
    }

    private static class XmlReader {
        Element companies = this.root.getChild("companies");
        Element company = this.companies.getChild("company");
        Element countries = this.root.getChild("countries");
        Element country = this.countries.getChild("country");
        MediaDatabase db = MediaDatabase.getInstance();
        Element genre = this.genres.getChild("genre");
        Element genres = this.root.getChild("genres");
        Element movie = this.movies.getChild("movie");
        Element movie_about = this.movie.getChild("about");
        Element movie_actor = this.movie_actors.getChild("person");
        Element movie_actors = this.movie.getChild("actors");
        Element movie_companies = this.movie.getChild("companies");
        Element movie_company = this.movie_companies.getChild("company");
        Element movie_countries = this.movie.getChild("countries");
        Element movie_country = this.movie_countries.getChild("country");
        Element movie_director = this.movie.getChild("director");
        Element movie_genre = this.movie_genres.getChild("genre");
        Element movie_genres = this.movie.getChild("genres");
        Element movie_poster = this.movie.getChild("poster");
        Element movie_release = this.movie_releases.getChild("release");
        Element movie_release_audio = this.movie_release.getChild(MimeTypes.BASE_TYPE_AUDIO);
        Element movie_release_id = this.movie_release.getChild(TtmlNode.ATTR_ID);
        Element movie_release_link = this.movie_release_links.getChild("link");
        Element movie_release_links = this.movie_release.getChild("links");
        Element movie_release_updated = this.movie_release.getChild("updated");
        Element movie_release_video = this.movie_release.getChild(MimeTypes.BASE_TYPE_VIDEO);
        Element movie_releases = this.movie.getChild("releases");
        Element movie_runtime = this.movie.getChild("runtime");
        Element movie_title = this.movie.getChild("title");
        Element movie_title_en = this.movie.getChild("title_en");
        Element movie_updated = this.movie.getChild("updated");
        Element movie_year = this.movie.getChild("year");
        Element movies = this.root.getChild("movies");
        Movie new_movie;
        Release new_release;
        Element person = this.persons.getChild("person");
        Element persons = this.root.getChild("persons");
        RootElement root = new RootElement("root");

        public XmlReader() {
            this.genre.setTextElementListener(new TextElementListener() {
                Genre m_genre;

                public void end(String body) {
                    this.m_genre.setTitle(body);
                }

                public void start(Attributes attributes) {
                    this.m_genre = XmlReader.this.db.getGenre(Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID)));
                    this.m_genre.setOrder(Integer.parseInt(attributes.getValue("order")));
                }
            });
            this.person.setTextElementListener(new TextElementListener() {
                private Person m_person;

                public void end(String body) {
                    this.m_person.setTitle(body);
                }

                public void start(Attributes attributes) {
                    this.m_person = XmlReader.this.db.getPerson(Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID)));
                }
            });
            this.company.setTextElementListener(new TextElementListener() {
                private Company m_company;

                public void end(String body) {
                    this.m_company.setTitle(body);
                }

                public void start(Attributes attributes) {
                    this.m_company = XmlReader.this.db.getCompany(Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID)));
                }
            });
            this.country.setTextElementListener(new TextElementListener() {
                private Country m_counry;

                public void end(String body) {
                    this.m_counry.setTitle(body);
                }

                public void start(Attributes attributes) {
                    this.m_counry = XmlReader.this.db.getCounry(Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID)));
                }
            });
            this.movie.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    XmlReader.this.new_movie = XmlReader.this.db.getMovie(Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID)));
                }
            });
            this.movie_title.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.setTitle(body);
                }
            });
            this.movie_title_en.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.setTitleEn(body);
                }
            });
            this.movie_year.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.setYear(Integer.parseInt(body));
                }
            });
            this.movie_runtime.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.setRuntime(body);
                }
            });
            this.movie_about.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.setAbout(body);
                }
            });
            this.movie_director.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.setDirectorId(Integer.parseInt(body));
                }
            });
            this.movie_poster.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.setPoster(body);
                }
            });
            this.movie_genre.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.addGenreId(Integer.parseInt(body));
                }
            });
            this.movie_actor.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.addActorId(Integer.parseInt(body));
                }
            });
            this.movie_company.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                }
            });
            this.movie_country.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.addCountryId(Integer.parseInt(body));
                }
            });
            this.movie_updated.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_movie.setUpdated(new Date(Long.parseLong(body)));
                }
            });
            this.movie_release.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    XmlReader.this.new_release = new Release();
                }
            });
            this.movie_release_id.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_release.setId(Integer.parseInt(body));
                }
            });
            this.movie_release_video.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_release.setVideo(body);
                }
            });
            this.movie_release_audio.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_release.setAudio(body);
                }
            });
            this.movie_release_updated.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    XmlReader.this.new_release.setUpdated(new Date(Long.parseLong(body)));
                }
            });
            this.movie_release_link.setTextElementListener(new TextElementListener() {
                int id;
                String title;

                public void end(String body) {
                    XmlReader.this.new_release.addLink(new Link(this.id, this.title, body));
                }

                public void start(Attributes attributes) {
                    this.id = Integer.parseInt(attributes.getValue(TtmlNode.ATTR_ID));
                    this.title = attributes.getValue("title");
                }
            });
            this.movie_release.setEndElementListener(new EndElementListener() {
                public void end() {
                    XmlReader.this.new_movie.addRelease(XmlReader.this.new_release);
                    XmlReader.this.new_release = null;
                }
            });
            this.movie.setEndElementListener(new EndElementListener() {
                public void end() {
                    XmlReader.this.new_movie = null;
                }
            });
        }

        public void parse(InputStream is) {
            try {
                Xml.parse(is, Xml.Encoding.UTF_8, this.root.getContentHandler());
            } catch (IOException e) {
                Log.e(MediaDatabase.TAG, "IOException", e);
            } catch (SAXException e2) {
                Log.e(MediaDatabase.TAG, "SAXException", e2);
            }
        }
    }

    public void loadFromStream(InputStream is) {
        new XmlReader().parse(is);
    }

    public void loadFromFile(String fileName) {
        try {
            if (new File(fileName).exists()) {
                loadFromStream(new FileInputStream(fileName));
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException", e);
        }
    }

    public void loadFromHttp(String uri) {
        try {
            HttpURLConnection get = (HttpURLConnection) new URL(uri).openConnection();
            if (get.getResponseCode() != 200) {
                Log.d(TAG, "HTTP error, invalid server status code: " + get.getResponseMessage());
            }
            loadFromStream(get.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (IllegalStateException e2) {
            Log.e(TAG, "IllegalStateException", e2);
        }
    }

    public void loadGenreList() {
        loadFromHttp(AppConfig.webApi() + "/genres.php");
    }

    public void loadGenreMovies(int genre_id) {
        loadFromHttp(AppConfig.webApi() + "/genre.php?genre_id=" + genre_id);
    }

    public void loadMovie(int movie_id) {
        loadFromHttp(AppConfig.webApi() + "/movie.php?movie_id=" + movie_id);
    }

    public void loadMovies(int[] list) {
        for (int loadMovie : list) {
            loadMovie(loadMovie);
        }
    }

    public void loadMovies(List<Integer> list) {
        for (Integer intValue : list) {
            loadMovie(intValue.intValue());
        }
    }

    public void loadCountries() {
        loadFromHttp(AppConfig.webApi() + "/countries.php");
    }

    public void loadPersons() {
        loadFromHttp(AppConfig.webApi() + "/persons.php");
    }

    public Collection<Genre> genres() {
        return this.m_genres.values();
    }

    public Collection<Person> persons() {
        return this.m_persons.values();
    }

    public Collection<Country> contries() {
        return this.m_countries.values();
    }

    public Collection<Company> companies() {
        return this.m_companies.values();
    }

    public Collection<Movie> movies() {
        return this.m_movies.values();
    }
}
