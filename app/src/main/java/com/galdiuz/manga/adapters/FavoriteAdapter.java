package com.galdiuz.manga.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.galdiuz.manga.App;
import com.galdiuz.manga.classes.Chapter;
import com.galdiuz.manga.classes.Favorite;
import com.galdiuz.manga.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FavoriteAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List<Favorite> favorites;
    private boolean[] tags = new boolean[6];

    public FavoriteAdapter(Context context) {
        inflater = LayoutInflater.from(context);

        updateFavorites();
    }

    public void updateFavorites() {
        boolean save = false;
        favorites = new ArrayList<>(Favorite.getFavorites().values());

        SharedPreferences preferences = App.getContext().getSharedPreferences(App.PREFSNAME, 0);
        for(int i = 0; i < App.NUMBER_OF_TAGS + 1; i++) {
            tags[i] = preferences.getBoolean(App.PREF_FILTERTAG + i, true);
        }

        for(int i = favorites.size() - 1; i >= 0; i--) {
            Favorite f = favorites.get(i);
            if(f.tag >= tags.length || f.tag < 0) {
                f.tag = 0;
                save = true;
            }
            if(!tags[f.tag]) {
                favorites.remove(i);
            }
        }

        int sort = preferences.getInt(App.PREF_SORT, 0);
        if(sort == 0) {
            Collections.sort(favorites, new Favorite.SortAlphabetically());
        }
        else if (sort == 1) {
            Collections.sort(favorites, new Favorite.SortLastUpdated());
        }

        notifyDataSetChanged();

        if(save) {
            Favorite.saveFavorites();
        }
    }

    @Override
    public int getCount() {
        return favorites.size();
    }

    @Override
    public Object getItem(int position) {
        return favorites.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if(view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.favorite_list_item, parent, false);
            holder.layout = (RelativeLayout)view.findViewById(R.id.layout);
            holder.image = (ImageView)view.findViewById(R.id.image);
            holder.title = (TextView)view.findViewById(R.id.title);
            holder.progress = (TextView)view.findViewById(R.id.progress);
            holder.update = (TextView)view.findViewById(R.id.update);
            holder.tag = (ImageView)view.findViewById(R.id.tag);
            holder.updateicon = (ImageView)view.findViewById(R.id.updateicon);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder)view.getTag();
        }

        Favorite f = favorites.get(position);

        if(f.markAsNew) {
            holder.layout.setBackgroundResource(R.drawable.favorite_mark_new);
        }
        else {
            holder.layout.setBackgroundResource(android.R.color.transparent);

        }
        holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (f.image != null) {
            final File coversPath = new File(App.FOLDER + "cache/covers");
            final File coverImage = new File(coversPath + "/" + f.id);
            if(coverImage.exists()) {
                holder.image.setImageResource(android.R.color.transparent);
                String uri = Uri.fromFile(coverImage).toString();
                App.getImageLoader().displayImage(uri, holder.image);
            }
            else {
                DisplayImageOptions options = new DisplayImageOptions.Builder()
                        .cloneFrom(App.getDefaultDisplayImageOptions())
                        .showImageOnLoading(android.R.color.transparent)
                        .build();
                App.getImageLoader().displayImage("http://cdn.mangaeden.com/mangasimg/" + f.image, holder.image, options, new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {}

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {}

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        try {
                            // TODO: Move to background thread to reduce UI lag?
                            coversPath.mkdirs();
                            OutputStream os = new BufferedOutputStream(new FileOutputStream(coverImage));
                            loadedImage.compress(Bitmap.CompressFormat.PNG, 100, os);
                            os.flush();
                            os.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {}
                });
            }
        }
        else {
            // TODO: Load image missing image
            holder.image.setImageResource(android.R.color.transparent);
        }
        holder.title.setText(f.title);
        holder.title.setSelected(true);
        if(f.progressChapter != null) {
            NumberFormat nf = Chapter.getChapterFormatter();
            String progress = "Chapter " + nf.format(f.progressChapter.number) + ", page " + f.progressPage;
            holder.progress.setText(progress);
        }
        else {
            holder.progress.setText(view.getResources().getText(R.string.favorite_not_started));
        }
        if(f.lastChapterDate != 0) {
            // TODO: Replace with string resource
            Date date = new Date((long)f.lastChapterDate * 1000);
            holder.update.setText("Updated " + DateFormat.getDateInstance().format(date) + " (c" + f.getFormattedLastChapterNumber() + ")");
        }
        else {
            holder.update.setText("");
        }

        if(f.tag > 0 && f.tag < tags.length) {
            holder.tag.setImageResource(App.TAG_IMAGES[f.tag - 1]);
            holder.tag.setVisibility(View.VISIBLE);
        }
        else {
            holder.tag.setVisibility(View.GONE);
        }

        if(f.checkForUpdates) {
            holder.updateicon.setVisibility(View.VISIBLE);
        }
        else {
            holder.updateicon.setVisibility(View.GONE);
        }

        return view;
    }

    private class ViewHolder {
        RelativeLayout layout;
        ImageView image, tag, updateicon;
        TextView title, progress, update;
    }
}
