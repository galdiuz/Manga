package com.galdiuz.manga.classes;

import android.util.Log;

import com.galdiuz.manga.App;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;

public class Favorite implements Serializable {
    @SerializedName("i")
    public String id;
    @SerializedName("t")
    public String title;
    @SerializedName("c")
    public Chapter progressChapter;
    @SerializedName("p")
    public int progressPage;
    @SerializedName("im")
    public String image;
    @SerializedName("ld")
    public double lastChapterDate;
    @SerializedName("ln")
    public double lastChapterNumber;
    @SerializedName("tg")
    public int tag;
    @SerializedName("n")
    public boolean markAsNew;
    @SerializedName("u")
    public boolean updated;
    @SerializedName("d")
    public int direction;
    @SerializedName("cu")
    public boolean checkForUpdates;

    public Favorite(Manga manga) {
        id = manga.id;
        title = manga.title;
        image = manga.image;
        progressChapter = null;
        progressPage = -1;
        lastChapterDate = 0;
        lastChapterNumber = -1;
        tag = 0;
        markAsNew = false;
        updated = false;
        direction = 0;
        checkForUpdates = false;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass() && id.equals(((Favorite) o).id);
    }

    public String getFormattedLastChapterNumber() {
        return Chapter.getChapterFormatter().format(lastChapterNumber);
    }

    private static HashMap<String, Favorite> favorites;

    public static HashMap<String, Favorite> getFavorites() {
        if(favorites == null) {
            favorites = new HashMap<>();
            Gson gson = new Gson();
            File file = new File(App.getContext().getFilesDir(), "favorites.json");
            if(file.exists()) {
                try {
                    FileReader fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr);
                    Favorite[] array = gson.fromJson(br, Favorite[].class);
                    for (Favorite f : array) {
                        favorites.put(f.id, f);
                    }
                }
                catch (FileNotFoundException e) {
                    Log.e("GetFavorites", "favorites.json not found", e);
                }
                catch (JsonSyntaxException e) {
                    Log.e("GetFavorites", "favorites.json malformed", e);
                    // TODO: Deal with corrupted file
                }
            }
        }
        return favorites;
    }

    public static void saveFavorites() {
        saveFavorites(true);
    }

    public static void saveFavorites(boolean saveToDrive) {
        if (favorites == null) throw new NullPointerException("Get favorites first");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(App.getContext().getFilesDir(), "favorites.json");
        try {
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            String wah = gson.toJson(favorites.values());
            bw.write(wah);
            bw.close();
            FileUtils.copyFile(file, new File(App.getContext().getFilesDir(), "favorites-backup.json"));

            if (saveToDrive) {

            }
        }
        catch (IOException e) {
            Log.e("SaveFavorites", "IOException", e);
        }
    }

    /*public static boolean isNull() {
        return favorites == null;
    }*/

    public static class SortAlphabetically implements Comparator<Favorite> {
        @Override
        public int compare(Favorite f1, Favorite f2) {
            return f1.title.compareToIgnoreCase(f2.title);
        }
    }

    public static class SortLastUpdated implements Comparator<Favorite> {
        @Override
        public int compare(Favorite f1, Favorite f2) {
            return f1.lastChapterDate < f2.lastChapterDate ? 1 : f1.lastChapterDate > f2.lastChapterDate ? -1 : f1.title.compareToIgnoreCase(f2.title);
        }
    }
}
