package com.galdiuz.manga.classes;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class Manga implements Serializable {
    public String id;
    public String artist;
    public String author;
    @SerializedName("categories")
    public String[] genres;
    public ArrayList<Chapter> chapters;
    public String description;
    public int status;
    public String title;
    @SerializedName("last_chapter_date")
    public double lastChapterDate;
    public String image;
    //public String imageURL; // Mangaupdates image
}
