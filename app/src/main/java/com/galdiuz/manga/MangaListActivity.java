package com.galdiuz.manga;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.galdiuz.manga.adapters.MangaListAdapter;
import com.galdiuz.manga.classes.Favorite;
import com.galdiuz.manga.classes.Manga;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MangaListActivity extends Activity implements
        TaskFragment.TaskCallbacks<Void, MangaListActivity.MangaListItem[]>,
        LoadingDialog.LoadingDialogCallbacks,
        MessageDialog.MessageDialogCallbacks {

    ListView listView;
    MangaListAdapter adapter;
    EditText search;

    private static final String TASKTAG = "download";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manga_list);

        listView = (ListView)findViewById(R.id.listView);
        listView.setFastScrollEnabled(true);

        search = (EditText)findViewById(R.id.editText);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s); // TODO: Optimize
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ArrayList<MangaListItem> arr = new ArrayList<>();
        adapter = new MangaListAdapter(this);
        listView.setAdapter(adapter);

        // TODO: Cache data on disk for faster retrieval
        // TODO: Doesn't load properly on configchange
        TaskFragment f = (TaskFragment)getFragmentManager().findFragmentByTag(TASKTAG);
        if(f == null) {
            TaskFragment.<String, Manga>createTask(this, null, TASKTAG);
        }
        else {
            Object result = f.getResult();
            if(result != null) {
                MangaListItem[] manga = ((MangaListHolder)result).manga;
                adapter.setMangaList(Arrays.asList(manga));
            }
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final MangaListItem item = (MangaListItem)parent.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), ChapterListActivity.class);
                intent.putExtra("title", item.title);
                intent.putExtra("id", item.id);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onPreExecute() {
        LoadingDialog.showDialog(this, getString(R.string.manga_load_list));
    }

    @Override
    public MangaListItem[] doInBackground(Void param) {
        HttpURLConnection con = null;
        try {
            Log.i("DownloadMangaList", "Open Connection");

            URL url = new URL("https://www.mangaeden.com/api/list/0/");
            con = (HttpURLConnection) url.openConnection();

            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));

            MangaListHolder mangaListHolder = gson.fromJson(reader, MangaListHolder.class);

            reader.close();

            MangaListItem[] manga = mangaListHolder.manga;

            for (MangaListItem m : manga) {
                m.title = StringEscapeUtils.unescapeHtml4(m.title);
            }
            Arrays.sort(manga);

            HashMap<String, Favorite> favorites = Favorite.getFavorites();

            for(MangaListItem item : manga) {
                Favorite f = favorites.get(item.id);
                if(f != null) {
                    item.favorite = true;
                }
            }

            return manga;
        }
        catch(IOException ex) {
            Log.e("DownloadMangaList",  "Exception");
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
    public void onPostExecute(MangaListItem[] mangaList) {
        Log.i("DownloadMangaList", "Post Execute");

        if(mangaList != null) {
            adapter.setMangaList(Arrays.asList(mangaList));
        }
        else {
            MessageDialog.showDialog(this, true, "Unable to retrieve manga list");
        }
        DialogFragment frag = (DialogFragment)getFragmentManager().findFragmentByTag(LoadingDialog.TAG);
        frag.dismiss();
    }

    @Override
    public void onLoadingDialogCancel() {
        TaskFragment f = (TaskFragment)getFragmentManager().findFragmentByTag(TASKTAG);
        f.cancel();
        finish();
    }

    @Override
    public void onMessageDialogDismiss() {
        finish();
    }

    class MangaListHolder {
        public int total;
        public MangaListItem[] manga;

        public MangaListHolder() {}
    }

    public class MangaListItem implements Comparable<MangaListItem> {
        @SerializedName("t")
        public String title;
        @SerializedName("s")
        public int status;
        @SerializedName("i")
        public String id;
        //@SerializedName("c")
        //public String[] genres;
        //@SerializedName("im")
        //public String imageURL;
        public boolean favorite = false;

        public MangaListItem() {}

        @Override
        public String toString() {
            return title;
        }

        @Override
        public int compareTo(MangaListItem o) {
            return toString().compareToIgnoreCase(o.toString());
        }
    }
}
