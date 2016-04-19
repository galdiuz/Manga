package com.galdiuz.manga.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.galdiuz.manga.App;
import com.galdiuz.manga.R;

public class TagAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private String[] tagNames;
    private boolean[] checked;
    private boolean showCheckboxes;
    private boolean showNone;

    public TagAdapter(Context context, String[] tagNames) {
        this(context, tagNames, true, null);
    }

    public TagAdapter(Context context, String[] tagNames, boolean showNone) {
        this(context, tagNames, showNone, null);
    }

    public TagAdapter(Context context, String[] tagNames, boolean[] checked) {
        this(context, tagNames, true, checked);
    }

    public TagAdapter(Context context, String[] tagNames, boolean showNone, boolean[] checked) {
        inflater = LayoutInflater.from(context);
        this.tagNames = tagNames;
        if(checked != null) {
            this.checked = checked;
            showCheckboxes = true;
        }
        else {
            showCheckboxes = false;
        }
        this.showNone = showNone;
    }

    @Override
    public int getCount() {
        return tagNames.length;
    }

    @Override
    public Object getItem(int position) {
        return tagNames[position];
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
            view = inflater.inflate(R.layout.favorite_filter_tags_item, parent, false);
            holder.imageView = (ImageView)view.findViewById(R.id.imageView);
            holder.textView = (TextView)view.findViewById(R.id.textView);
            holder.checkBox = (CheckBox)view.findViewById(R.id.checkBox);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder)view.getTag();
        }

        if(!showNone) {
            holder.imageView.setImageResource(App.TAG_IMAGES[position]);
        }
        else if(position > 0) {
            holder.imageView.setImageResource(App.TAG_IMAGES[position - 1]);
            holder.imageView.setVisibility(View.VISIBLE);
        }
        else {
            holder.imageView.setImageResource(App.TAG_IMAGES[0]);
            holder.imageView.setVisibility(View.INVISIBLE);
        }
        holder.textView.setText(tagNames[position]);
        if(showCheckboxes) {
            holder.checkBox.setChecked(checked[position]);
        }
        else {
            holder.checkBox.setVisibility(View.GONE);
        }

        return view;
    }

    private class ViewHolder {
        ImageView imageView;
        TextView textView;
        CheckBox checkBox;
    }
}
