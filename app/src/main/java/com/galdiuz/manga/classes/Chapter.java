package com.galdiuz.manga.classes;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Chapter implements Serializable {
    private static NumberFormat chapterFormat;

    public double number;
    public double date;
    public String title;
    public String id;

    public Chapter(double number, double date, String title, String id)
    {
        this.number = number;
        this.date = date;
        this.title = title;
        this.id = id;
    }

    @Override
    public String toString() {
        NumberFormat nf = getChapterFormatter();
        if(nf.format(number).equals(title))
            return "Chapter " + title;
        else
            return "Chapter " + nf.format(number) + ": " + title;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass() && id.equals(((Chapter) o).id);
    }

    public static NumberFormat getChapterFormatter() {
        if(chapterFormat == null) {
            chapterFormat = new DecimalFormat("#.###");
        }
        return chapterFormat;
    }

    public String getformattedNumber() {
        return getChapterFormatter().format(number);
    }
}
