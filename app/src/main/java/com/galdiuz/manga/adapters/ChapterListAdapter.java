package com.galdiuz.manga.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.galdiuz.manga.App;
import com.galdiuz.manga.classes.Chapter;
import com.galdiuz.manga.classes.Manga;
import com.galdiuz.manga.R;

import java.text.DateFormat;
import java.util.Date;

public class ChapterListAdapter extends BaseAdapter {
    private static final int TYPE_INFO = 0;
    private static final int TYPE_CHAPTER = 1;
    private static final int TYPE_COUNT = 2;
    private static final boolean NEWEST_FIRST = true;

    private LayoutInflater inflater;
    private Manga manga;

    public ChapterListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setManga(Manga manga) {
        this.manga = manga;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_INFO : TYPE_CHAPTER;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public int getCount() {
        return manga != null ? manga.chapters.size() + 1 : 0;
    }

    @Override
    public Object getItem(int position) {
        if(position != 0) {
            if(NEWEST_FIRST) {
                return manga.chapters.get(manga.chapters.size() - position);
            }
            else {
                return manga.chapters.get(position - 1);
            }
        }
        else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        int type = getItemViewType(position);
        if(view == null) {
            holder = new ViewHolder();

            if(type == TYPE_INFO) {
                view = inflater.inflate(R.layout.chapter_list_info, parent, false);
                holder.description = (TextView)view.findViewById(R.id.text6);
                holder.image = (ImageView)view.findViewById(R.id.imageView3);
                holder.textFrame = (LinearLayout)view.findViewById(R.id.textFrame);
            }
            else {
                view = inflater.inflate(R.layout.chapter_list_item, parent, false);
                holder.title = (TextView)view.findViewById(R.id.textView3);
                holder.date = (TextView)view.findViewById(R.id.textView4);
            }

            view.setTag(holder);
        }
        else {
            holder = (ViewHolder)view.getTag();
        }

        if(type == TYPE_INFO) {
            holder.description.setText(manga.description);
            holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if(manga.image != null) {
                App.getImageLoader().displayImage("https://cdn.mangaeden.com/mangasimg/" + manga.image, holder.image);
            }
            else {
                // TODO: Load image missing image
            }

            holder.description.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int lines = holder.description.getHeight() / holder.description.getLineHeight();
                    holder.description.setMaxLines(lines);
                    holder.description.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
        else {
            Chapter chapter;
            if(NEWEST_FIRST) {
                chapter = manga.chapters.get(manga.chapters.size() - position);
            }
            else {
                chapter = manga.chapters.get(position - 1);
            }
            holder.title.setText(chapter.toString());
            Date date = new Date((long)chapter.date * 1000);
            holder.date.setText(view.getResources().getString(R.string.chapter_date, DateFormat.getDateInstance().format(date)));
        }

        return view;
    }

    private class ViewHolder {
        public TextView title, date, description;
        public ImageView image;
        public LinearLayout textFrame;
    }
}
