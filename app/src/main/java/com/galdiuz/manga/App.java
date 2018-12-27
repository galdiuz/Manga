package com.galdiuz.manga;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.decode.BaseImageDecoder;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;

import java.io.IOException;
import java.io.InputStream;

public class App extends Application {

    public static final String PREFSNAME = "SharedPreferences";
    public static final String PREF_SORT = "sort";
    public static final String PREF_FILTERTAG = "filterTag";
    public static final String PREF_TAGNAME = "tagName";
    public static final String[] DEFAULT_TAG_NAMES = { "None", "Green", "Red", "Blue", "Yellow", "Purple" };
    public static final int NUMBER_OF_TAGS = 5; // Not including untagged/none
    public static final int[] TAG_IMAGES = {
            R.drawable.ic_tag_green,
            R.drawable.ic_tag_red,
            R.drawable.ic_tag_blue,
            R.drawable.ic_tag_yellow,
            R.drawable.ic_tag_purple
    };
    public static final String FOLDER = Environment.getExternalStorageDirectory() + "/Manga/";

    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    public static ImageLoader getImageLoader() {
        ImageLoader imageLoader = ImageLoader.getInstance();
        if(!imageLoader.isInited()) {
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getContext())
                    .defaultDisplayImageOptions(getDefaultDisplayImageOptions())
                    .denyCacheImageMultipleSizesInMemory()
                    .memoryCacheSizePercentage(50)
                    .diskCacheSize(104857600)
                    //.writeDebugLogs()
                    .build();
            imageLoader.init(config);
        }
        return imageLoader;
    }

    public static DisplayImageOptions getDefaultDisplayImageOptions() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                //.imageScaleType(ImageScaleType.EXACTLY)
                .imageScaleType(ImageScaleType.NONE)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }


}
