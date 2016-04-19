package com.galdiuz.manga;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.galdiuz.manga.adapters.FavoriteAdapter;
import com.galdiuz.manga.adapters.TagAdapter;
import com.galdiuz.manga.classes.Chapter;
import com.galdiuz.manga.classes.ChapterDeserializer;
import com.galdiuz.manga.classes.Favorite;
import com.galdiuz.manga.classes.Manga;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

public class FavoriteActivity extends Activity implements
        TaskFragment.TaskCallbacks<Favorite, Manga>,
        LoadingDialog.LoadingDialogCallbacks {

    private FavoriteAdapter adapter;
    private BroadcastReceiver receiver;
    private boolean updatemode;

    private static final String TASKTAG = "download";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        ActionBar ab = getActionBar();
        if(ab != null) ab.setDisplayHomeAsUpEnabled(true);

        ListView listView = (ListView)findViewById(R.id.listView);
        listView.setFastScrollEnabled(true);

        adapter = new FavoriteAdapter(this);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);

        showUpdates();

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    App.getImageLoader().pause();
                } else {
                    App.getImageLoader().resume();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Favorite f = (Favorite) parent.getItemAtPosition(position);
                if(updatemode) {
                    f.checkForUpdates = !f.checkForUpdates;
                    Favorite.saveFavorites();
                    adapter.updateFavorites();
                }
                else {
                    if (f.progressChapter != null) {
                        read(f);
                    }
                    else {
                        viewChapterList(f);
                    }
                }
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int type = intent.getIntExtra("type", -1);
                if(type == UpdateService.PROGRESS) {
                    int current = intent.getIntExtra("current", -1);
                    int total = intent.getIntExtra("total", -1);
                    String title = intent.getStringExtra("title");
                    RelativeLayout layout = (RelativeLayout)findViewById(R.id.progessLayout);
                    layout.setVisibility(View.VISIBLE);
                    ProgressBar bar = (ProgressBar)findViewById(R.id.progressBar);
                    bar.setProgress(current);
                    bar.setMax(total);
                    TextView textView = (TextView)findViewById(R.id.progressText);
                    textView.setText(getString(R.string.favorite_checking_text, current, total, title));
                }
                else if(type == UpdateService.DONE) {
                    RelativeLayout layout = (RelativeLayout)findViewById(R.id.progessLayout);
                    layout.setVisibility(View.INVISIBLE);
                    if(intent.getIntExtra("updated", 0) > 0) {
                        showUpdates();
                    }
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(UpdateService.UPDATE));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    protected void onRestart() {
        adapter.updateFavorites();
        RelativeLayout layout = (RelativeLayout)findViewById(R.id.progessLayout);
        layout.setVisibility(View.INVISIBLE);
        showUpdates();
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        if(updatemode) {
            updatemode = false;
            Toast.makeText(this, "Updatemode disabled", Toast.LENGTH_SHORT).show();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_favorite, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.sort) {
            SortDialog.showDialog(this);
            return true;
        }
        else if (id == R.id.filter) {
            FilterTagDialog.showDialog(this);
            return true;
        }
        else if (id == R.id.search) {
            MessageDialog.showDialog(this, "Not yet implemented");
            // TODO: Implement
            return true;
        }
        else if (id == R.id.update) {
            Intent intent = new Intent(this, UpdateService.class);
            startService(intent);
        }
        else if (id == R.id.updatemode) {
            if(!updatemode) {
                // TODO: Replace with string resource, find better name
                updatemode = true;
                Toast.makeText(this, "Updatemode enabled", Toast.LENGTH_SHORT).show();
            }
            else {
                updatemode = false;
                Toast.makeText(this, "Updatemode disabled", Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.tag) {
            RenameTagsDialog.showDialog(this);
        }
        else if (id == R.id.backup) {

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            File folder = new File(App.FOLDER);
            boolean success = true;
            if(!folder.exists()) {
                success = folder.mkdir();
            }
            if(success) {
                // TODO: Custom name for file
                File file = new File(folder + "favorites.json");
                try {
                    FileWriter fw = new FileWriter(file);
                    BufferedWriter bw = new BufferedWriter(fw);
                    String wah = gson.toJson(Favorite.getFavorites().values());
                    bw.write(wah);
                    bw.close();
                    MessageDialog.showDialog(this, "Backup complete!");

                }
                catch (IOException e) {
                    Log.e("SaveFavorites", "IOException", e);
                }
                showUpdates();
            }

            return true;
        }
        else if (id == R.id.restore) {
            // TODO: Select file
            File file = new File(App.FOLDER + "favorites.json");
            if(file.exists()) {
                Gson gson = new Gson();
                Favorite.getFavorites().clear();
                try {
                    FileReader fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr);
                    Favorite[] array = gson.fromJson(br, Favorite[].class);
                    for (Favorite f : array) {
                        Favorite.getFavorites().put(f.id, f);
                    }
                    Favorite.saveFavorites();
                    MessageDialog.showDialog(this, "Restore complete!");
                }
                catch (FileNotFoundException e) {
                    Log.e("GetFavorites", "favorites.json not found", e);
                }
                catch (JsonSyntaxException e) {
                    Log.e("GetFavorites", "favorites.json malformed", e);
                }
            }

            return true;
        }
        else if (id == R.id.settings) {
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info;
        info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        Favorite f = (Favorite)adapter.getItem(info.position);
        menu.setHeaderTitle(f.title);
        getMenuInflater().inflate(R.menu.context_favorite, menu);
        if(f.progressChapter == null) {
            menu.removeItem(R.id.read);
            menu.removeItem(R.id.clear_progress);
        }
        if(f.markAsNew) {
            // TODO: Replace with string resource
            menu.findItem(R.id.new_chapter).setTitle("Unmark new chapter");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int id = item.getItemId();
        Favorite f = (Favorite)adapter.getItem(info.position);

        if (id == R.id.read) {
            read(f);
        }
        else if (id == R.id.chapter_list) {
            viewChapterList(f);
        }
        else if (id == R.id.tag) {
            SetTagDialog.showDialog(this, f);
        }
        else if (id == R.id.clear_image) {
            File image = new File(App.FOLDER + "cache/covers/" + f.id);
            if(image.exists()) {
                if(image.delete()) {
                    Toast.makeText(this, "Cover cache cleared", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Clear failed", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(this, "Cache empty", Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.clear_progress) {
            f.progressChapter = null;
            f.progressPage = -1;
            Favorite.saveFavorites();
            adapter.updateFavorites();
        }
        else if (id == R.id.remove) {
            Favorite.getFavorites().remove(f.id);
            Favorite.saveFavorites();
            adapter.updateFavorites();
        }
        else if (id == R.id.clear_latest) {
            Favorite u = Favorite.getFavorites().get(f.id);
            f.lastChapterNumber = -1;
            f.lastChapterDate = 0;
            f.markAsNew = false;
            Favorite.saveFavorites();
            adapter.updateFavorites();
        }
        else if (id == R.id.new_chapter) {
            f.markAsNew = !f.markAsNew;
            Favorite.saveFavorites();
            adapter.updateFavorites();
        }
        return super.onContextItemSelected(item);
    }

    private void read(Favorite f) {
        TaskFragment.<Favorite, Manga>createTask(this, f, TASKTAG);
        f.markAsNew = false;
    }

    private void viewChapterList(Favorite f) {
        f.markAsNew = false;
        Intent intent = new Intent(this, ChapterListActivity.class);
        intent.putExtra("title", f.title);
        intent.putExtra("id", f.id);
        startActivity(intent);
    }

    private void showUpdates() {
        ArrayList<Favorite> updated = new ArrayList<>();
        for(Favorite f : Favorite.getFavorites().values()) {
            if(f.updated) {
                updated.add(f);
            }
        }
        if(updated.size() > 0) {
            StringBuilder sb = new StringBuilder();
            String first = "";
            for (Favorite f : updated) {
                f.updated = false;
                sb.append(first).append(f.title).append(" (Chapter ").append(f.getFormattedLastChapterNumber()).append(")");
                first = "\n\n";
            }
            Favorite.saveFavorites();
            MessageDialog.showDialog(this, getString(R.string.favorite_new_chapters_title), sb.toString());
            adapter.updateFavorites();
            NotificationManager mgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            mgr.cancel(UpdateService.NOTIFICATION_NEW_ID);
        }
    }

    @Override
    public void onPreExecute() {
        LoadingDialog.showDialog(this, getString(R.string.favorite_load_manga));
    }

    @Override
    public Manga doInBackground(Favorite favorite) {
        HttpURLConnection con = null;
        try {
            Log.i("DownloadFavoriteManga", "Open Connection");

            URL url = new URL("https://www.mangaeden.com/api/manga/" + favorite.id);
            con = (HttpURLConnection) url.openConnection();

            Gson gson = new GsonBuilder().registerTypeAdapter(Chapter.class, new ChapterDeserializer()).create();
            JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));

            Manga manga = gson.fromJson(reader, Manga.class);
            manga.id = favorite.id;
            manga.title = StringEscapeUtils.unescapeHtml4(manga.title);

            reader.close();

            return manga;
        }
        catch(IOException ex) {
            Log.e("DownloadFavoriteManga",  "Exception");
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
        Log.i("DownloadFavoriteManga", "Post Execute");

        DialogFragment frag = (DialogFragment)getFragmentManager().findFragmentByTag(LoadingDialog.TAG);
        frag.dismiss();

        if(manga != null) {
            Collections.reverse(manga.chapters);

            TaskFragment f = (TaskFragment)getFragmentManager().findFragmentByTag(TASKTAG);
            Favorite favorite = (Favorite)f.getParam();

            Intent intent = new Intent(getApplicationContext(), ImageViewerActivity.class);
            intent.putExtra("chapter", favorite.progressChapter);
            intent.putExtra("page", favorite.progressPage);
            intent.putExtra("manga", manga);
            startActivity(intent);
        }
        else {
            MessageDialog.showDialog(this, getString(R.string.favorite_load_failed));
        }
    }

    @Override
    public void onLoadingDialogCancel() {
        TaskFragment f = (TaskFragment)getFragmentManager().findFragmentByTag(TASKTAG);
        f.cancel();
        adapter.updateFavorites();
    }

    public static class SetTagDialog extends DialogFragment {
        public static void showDialog(Activity activity, Favorite favorite) {
            Bundle b = new Bundle();
            b.putSerializable("favorite", favorite);
            SetTagDialog dialog = new SetTagDialog();
            dialog.setArguments(b);
            dialog.show(activity.getFragmentManager(), "tagdialog");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            SharedPreferences preferences = App.getContext().getSharedPreferences(App.PREFSNAME, 0);
            final Favorite f = (Favorite)getArguments().getSerializable("favorite");
            assert f != null;
            String title = f.title;
            String[] items = new String[App.NUMBER_OF_TAGS + 1];

            items[0] = App.DEFAULT_TAG_NAMES[0];
            for(int i = 1; i <= App.NUMBER_OF_TAGS; i++) {
                items[i] = preferences.getString(App.PREF_TAGNAME + i, App.DEFAULT_TAG_NAMES[i]);
            }

            final TagAdapter adapter = new TagAdapter(getActivity(), items);

            builder.setTitle("Set tag for " + title)
                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            f.tag = which;
                            Favorite.saveFavorites();
                            ((FavoriteActivity) getActivity()).adapter.updateFavorites();
                        }
                    });
            return builder.create();
        }
    }

    public static class RenameTagsDialog extends DialogFragment {
        public static void showDialog(Activity activity) {
            new RenameTagsDialog().show(activity.getFragmentManager(), "renametagsdialog");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final SharedPreferences preferences = App.getContext().getSharedPreferences(App.PREFSNAME, 0);
            String[] items = new String[App.NUMBER_OF_TAGS];

            for(int i = 1; i <= App.NUMBER_OF_TAGS; i++) {
                items[i - 1] = preferences.getString(App.PREF_TAGNAME + i, App.DEFAULT_TAG_NAMES[i]);
            }

            final TagAdapter adapter = new TagAdapter(getActivity(), items, false);

            builder.setTitle("Rename tags")
                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RenameDialog.showDialog(getActivity(), which + 1);
                        }
                    })
                    .setPositiveButton("Apply", null);

            AlertDialog dialog = builder.create();
            ListView listView = dialog.getListView();
            listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            return dialog;
        }
    }

    public static class RenameDialog extends DialogFragment {
        public static void showDialog(Activity activity, int tag) {
            Bundle b = new Bundle();
            b.putInt("tag", tag);
            RenameDialog dialog = new RenameDialog();
            dialog.setArguments(b);
            dialog.show(activity.getFragmentManager(), "renamedialog");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final SharedPreferences preferences = App.getContext().getSharedPreferences(App.PREFSNAME, 0);
            final EditText editText = new EditText(getActivity());
            final int tag = getArguments().getInt("tag");
            String name = preferences.getString(App.PREF_TAGNAME + tag, App.DEFAULT_TAG_NAMES[tag]);
            editText.setText(name);
            editText.setSelection(editText.getText().length());

            builder.setTitle("Change tag name")
                    .setIcon(App.TAG_IMAGES[tag - 1])
                    .setView(editText)
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preferences.edit().putString(App.PREF_TAGNAME + tag, editText.getText().toString()).apply();
                        }
                    })
                    .setNegativeButton("Cancel", null);

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            RenameTagsDialog.showDialog(getActivity());
        }
    }

    public static class FilterTagDialog extends DialogFragment {
        public static void showDialog(Activity activity) {
            new FilterTagDialog().show(activity.getFragmentManager(), "filterdialog");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final SharedPreferences preferences = App.getContext().getSharedPreferences(App.PREFSNAME, 0);
            String[] items = new String[App.NUMBER_OF_TAGS + 1];

            items[0] = App.DEFAULT_TAG_NAMES[0];
            for(int i = 1; i <= App.NUMBER_OF_TAGS; i++) {
                items[i] = preferences.getString(App.PREF_TAGNAME + i, App.DEFAULT_TAG_NAMES[i]);
            }

            final boolean[] checked = new boolean[App.NUMBER_OF_TAGS + 1];
            for(int i = 0; i < checked.length; i++) {
                checked[i] = preferences.getBoolean(App.PREF_FILTERTAG + i, true);
            }

            final TagAdapter adapter = new TagAdapter(getActivity(), items, checked);
            builder.setAdapter(adapter, null)
                    .setTitle("Filter tags")
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = preferences.edit();
                            for (int i = 0; i < checked.length; i++) {
                                editor.putBoolean(App.PREF_FILTERTAG + i, checked[i]);
                            }
                            editor.apply();
                            ((FavoriteActivity) getActivity()).adapter.updateFavorites();
                        }
                    });
            AlertDialog dialog = builder.create();
            ListView listView = dialog.getListView();
            listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            listView.setItemsCanFocus(false);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    checked[position] = !checked[position];
                    adapter.notifyDataSetChanged();
                }
            });

            return dialog;
        }
    }

    public static class SortDialog extends DialogFragment {
        public static void showDialog(Activity activity) {
            new SortDialog().show(activity.getFragmentManager(), "sortdialog");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final SharedPreferences preferences = App.getContext().getSharedPreferences(App.PREFSNAME, 0);
            String[] items = new String[2];

            items[0] = "Alphabetically";
            items[1] = "Last Updated";

            int selected = preferences.getInt(App.PREF_SORT, 0);

            builder.setTitle("Select sorting method")
                    .setSingleChoiceItems(items, selected, null)
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt(App.PREF_SORT, selectedPosition);
                            editor.apply();
                            ((FavoriteActivity) getActivity()).adapter.updateFavorites();
                        }
                    });

            return builder.create();
        }
    }


}
