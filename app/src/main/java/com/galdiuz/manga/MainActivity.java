package com.galdiuz.manga;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.galdiuz.manga.classes.Chapter;
import com.galdiuz.manga.classes.Manga;
import com.galdiuz.manga.other.CustomExceptionHandler;

import java.util.ArrayList;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);

        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(App.FOLDER + "exceptions/"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
        //return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_preferences) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void test3(View view) {
        Intent intent = new Intent(this, MangaListActivity.class);
        startActivity(intent);
    }

    public void testFavorite(View view) {
        Intent intent = new Intent(this, FavoriteActivity.class);
        startActivity(intent);
    }

    public void settings(View view) {

    }

    public void testImage(View view) {
        //Chapter chapter = new Chapter(254, 1446447326, "254", "563708de719a162df6a3a353");
        Chapter chapter = new Chapter(106, 1446747077, "106", "563b9bc5719a166ad94db60f");
        Manga manga = new Manga();
        //manga.title = "Tower of God";
        manga.title = "The Gamer";
        //manga.id = "4e70ea03c092255ef70046e8";
        manga.id = "522e4b1d45b9ef7927d18ba4";
        ArrayList<Chapter> chapters = new ArrayList<>();
        chapters.add(chapter);
        manga.chapters = chapters;
        Intent intent = new Intent(this, InfiniteImageViewerActivity.class);
        intent.putExtra("chapter", chapter);
        intent.putExtra("page", 21);
        intent.putExtra("manga", manga);
        startActivity(intent);
    }
}
