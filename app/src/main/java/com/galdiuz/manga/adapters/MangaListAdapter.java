package com.galdiuz.manga.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.galdiuz.manga.MangaListActivity;
import com.galdiuz.manga.R;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MangaListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<MangaListActivity.MangaListItem> mangaList;
    private List<MangaListActivity.MangaListItem> filteredList;

    public MangaListAdapter(Activity activity) {
        this.inflater = activity.getLayoutInflater();
    }

    public void setMangaList(List<MangaListActivity.MangaListItem> list) {
        mangaList = list;
        filteredList = list;
        notifyDataSetChanged();
    }

    public void filter(CharSequence filter) {
        filteredList = new ArrayList<>();
        for(MangaListActivity.MangaListItem item : mangaList) {
            if(StringUtils.containsIgnoreCase(item.title, filter)) {
                filteredList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredList != null ? filteredList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return filteredList.get(position);
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
            view = inflater.inflate(R.layout.manga_list_item, parent, false);
            holder.status = (ImageView)view.findViewById(R.id.status);
            holder.favorite = (ImageView)view.findViewById(R.id.favorite);
            holder.title = (TextView)view.findViewById(R.id.title);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder)view.getTag();
        }

        MangaListActivity.MangaListItem item = filteredList.get(position);

        if(item.status == 0) {
            holder.status.setImageResource(R.drawable.book);
        }
        else {
            holder.status.setImageResource(R.drawable.book_open);
        }

        if(item.favorite) {
            holder.favorite.setImageResource(R.drawable.ic_star);
        }
        else {
            holder.favorite.setImageResource(android.R.color.transparent);
        }

        holder.title.setText(item.title);

        return view;
    }

    private class ViewHolder {
        ImageView status, favorite;
        TextView title;
    }
}
