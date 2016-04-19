package com.galdiuz.manga;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.galdiuz.manga.adapters.ChapterListAdapter;
import com.galdiuz.manga.classes.Chapter;
import com.galdiuz.manga.classes.ChapterDeserializer;
import com.galdiuz.manga.classes.Favorite;
import com.galdiuz.manga.classes.Manga;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;


public class ChapterListActivity extends Activity implements
        TaskFragment.TaskCallbacks<String, Manga>,
        LoadingDialog.LoadingDialogCallbacks,
        MessageDialog.MessageDialogCallbacks {

    ListView listView;
    ChapterListAdapter adapter;
    Manga manga;

    // TODO: Set progress here, context menu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_list);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        String title = intent.getStringExtra("title");

        ActionBar ab = getActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(title);

        listView = (ListView)findViewById(R.id.listView);
        listView.setFastScrollEnabled(true);

        adapter = new ChapterListAdapter(this);
        listView.setAdapter(adapter);

        TaskFragment f = (TaskFragment)getFragmentManager().findFragmentByTag("download");
        if(f == null) {
            TaskFragment.<String, Manga>createTask(this, id, "download");
        }
        else {
            Object result = f.getResult();
            if(result != null) {
                manga = (Manga)result;
                adapter.setManga(manga);
                invalidateOptionsMenu();
            }
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                if(position == 0) {
                    ShowMangaInfoFragment frag = new ShowMangaInfoFragment();
                    Bundle b = new Bundle();
                    b.putSerializable("manga", manga);
                    frag.setArguments(b);
                    frag.show(getFragmentManager(), "mangainfo");
                }
                else {
                    final Chapter item = (Chapter) parent.getItemAtPosition(position);
                    Intent intent = new Intent(getApplicationContext(), ImageViewerActivity.class);
                    intent.putExtra("chapter", item);
                    intent.putExtra("page", 1);
                    intent.putExtra("manga", manga);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chapter_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(manga != null && Favorite.getFavorites().containsKey(manga.id)) {
            menu.findItem(R.id.favorite).setIcon(R.drawable.ic_remove).setTitle(getString(R.string.action_favorite_remove));
        }
        else {
            menu.findItem(R.id.favorite).setIcon(R.drawable.ic_star).setTitle(getString(R.string.action_favorite_add));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.favorite) {
            if(manga != null) {
                Favorite favorite = Favorite.getFavorites().get(manga.id);
                if (favorite == null) {
                    Favorite.getFavorites().put(manga.id, new Favorite(manga));
                    Favorite.saveFavorites();
                    invalidateOptionsMenu();
                    MessageDialog.showDialog(this, getString(R.string.favorite_added, manga.title));
                }
                else {
                    Favorite.getFavorites().remove(manga.id);
                    Favorite.saveFavorites();
                    invalidateOptionsMenu();
                    MessageDialog.showDialog(this, getString(R.string.favorite_removed, manga.title));
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestart() {
        invalidateOptionsMenu();
        super.onRestart();
    }

    @Override
    public Intent getParentActivityIntent() {
        //Intent intent = new Intent(this, MangaListActivity.class);

        //return intent;
        return super.getParentActivityIntent();
    }

    @Override
    public void onPreExecute() {
        LoadingDialog.showDialog(this, getString(R.string.chapter_load_list));
    }

    @Override
    public Manga doInBackground(String id) {
        HttpURLConnection con = null;
        try {
            Log.i("DownloadChapterList", "Open Connection");

            URL url = new URL("https://www.mangaeden.com/api/manga/" + id);
            con = (HttpURLConnection) url.openConnection();

            Gson gson = new GsonBuilder().registerTypeAdapter(Chapter.class, new ChapterDeserializer()).create();
            JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));

            manga = gson.fromJson(reader, Manga.class);
            manga.id = id;
            manga.title = StringEscapeUtils.unescapeHtml4(manga.title);
            manga.description = StringEscapeUtils.unescapeHtml4(manga.description);

            reader.close();

            return manga;
        }
        catch(IOException ex) {
            Log.e("DownloadChapterList",  "Exception");
        }
        finally {
            if(con != null)
                con.disconnect();
        }
        return null;
    }

    @Override
    public void onCancelled() {}

    @Override
    public void onPostExecute(Manga manga) {
        Log.i("DownloadChapterList", "Post Execute");

        DialogFragment frag = (DialogFragment)getFragmentManager().findFragmentByTag("loadingdialog");
        frag.dismiss();

        if(manga != null) {
            adapter.setManga(manga);

            invalidateOptionsMenu();

            Collections.reverse(manga.chapters);
        }
        else {
            MessageDialog.showDialog(this, true, "Unable to retrieve chapter list");
        }
    }

    @Override
    public void onLoadingDialogCancel() {
        TaskFragment f = (TaskFragment)getFragmentManager().findFragmentByTag("download");
        f.cancel();
        finish();
    }

    @Override
    public void onMessageDialogDismiss() {
        finish();
    }

    public static class ShowMangaInfoFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.manga_info, null);
            builder.setView(view);

            Manga manga = (Manga)getArguments().getSerializable("manga");
            assert manga != null;

            ((TextView)view.findViewById(R.id.title)).setText(manga.title);
            if(manga.author.equals(manga.artist)) {
                ((TextView)view.findViewById(R.id.author)).setText(getString(R.string.manga_info_authorartist, manga.author));
                view.findViewById(R.id.artist).setVisibility(View.GONE);
            }
            else {
                ((TextView)view.findViewById(R.id.author)).setText(getString(R.string.manga_info_author, manga.author));
                ((TextView)view.findViewById(R.id.artist)).setText(getString(R.string.manga_info_artist, manga.artist));
            }
            StringBuilder sb = new StringBuilder();
            for(String g : manga.genres) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(g);
            }
            ((TextView)view.findViewById(R.id.genres)).setText(sb.toString());
            String[] status = {"Suspended", "Ongoing" , "Completed"};
            ((TextView)view.findViewById(R.id.chapters)).setText(getString(R.string.manga_info_status_text, manga.chapters.size(), status[manga.status]));
            ((TextView)view.findViewById(R.id.description)).setText(manga.description);

            ImageView image = (ImageView)view.findViewById(R.id.image);
            if(manga.image != null) {
                App.getImageLoader().displayImage("https://cdn.mangaeden.com/mangasimg/" + manga.image, image);
            }

            return builder.create();
        }
    }

    private class BatotoChapterList extends AsyncTask<String, Void, Document> {
        @Override
        protected Document doInBackground(String... urls) {
            try {
                Log.i("Manga", "Success");
                Document doc = Jsoup.connect(urls[0]).get();
                return doc;
            }
            catch(IOException e) {
                Log.e("Manga", "IOException");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Document doc) {
            Element content = doc.getElementById("content");
            Element table = content.getElementsByClass("chapters_list").first();
            Elements chapters = table.getElementsByClass("lang_English");
            for(Element chapterrow : chapters) {
                String name = chapterrow.getElementsByTag("a").first().text();
                String url = chapterrow.getElementsByTag("a").first().attr("href");
                //Chapter chapter = new Chapter(name, url);
                //adapter.add(chapter);
                //Log.i("List", name);
            }
        }
    }
}
