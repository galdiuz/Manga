package com.galdiuz.manga;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.galdiuz.manga.classes.Chapter;
import com.galdiuz.manga.classes.Manga;
import com.galdiuz.manga.imageadapter.PhotoViewAttacher;
import com.galdiuz.manga.other.TextViewAlpha;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class InfiniteImageViewerActivity extends Activity {

    private ImageView imageViewPrev;
    private ImageView imageViewCurr;
    private ImageView imageViewNext;
    private PhotoViewAttacher attacherPrev;
    private PhotoViewAttacher attacherCurr;
    private PhotoViewAttacher attacherNext;
    private int currentPage;
    private Chapter currentChapter;
    private Manga manga;
    private AnimatorSet titleAnimatorSet;
    private ObjectAnimator loadingAnimator;
    private HashMap<Double, Page[]> chapterMap;
    private RetainedFragment retainedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infinite_image_viewer);

        if (savedInstanceState != null) {
            currentChapter = (Chapter) savedInstanceState.getSerializable("chapter");
            currentPage = savedInstanceState.getInt("page");
            manga = (Manga) savedInstanceState.getSerializable("manga");
        }
        else {
            Intent intent = getIntent();
            currentChapter = (Chapter) intent.getSerializableExtra("chapter");
            currentPage = intent.getIntExtra("page", -1);
            manga = (Manga) intent.getSerializableExtra("manga");
        }

        chapterMap = new HashMap<>();

        imageViewPrev = (ImageView) findViewById(R.id.imageView);
        imageViewCurr = (ImageView) findViewById(R.id.imageView2);
        imageViewNext = (ImageView) findViewById(R.id.imageView3);

        attacherPrev = new PhotoViewAttacher(imageViewPrev);
        attacherCurr = new PhotoViewAttacher(imageViewCurr);
        attacherNext = new PhotoViewAttacher(imageViewNext);

        attacherCurr.setOnDragListener(new PhotoViewAttacher.OnDragListener() {
            @Override
            public void onDrag(float dx, float dy) {
                dy = 0;
                imageViewCurr.setTranslationY(imageViewCurr.getTranslationY() - dy);
                attacherCurr.movedY = dy;
            }
        });

        TextViewAlpha loadingTextView = (TextViewAlpha) findViewById(R.id.textView);
        loadingAnimator = ObjectAnimator.ofInt(loadingTextView, "textAlpha", 255, 0);
        loadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loadingAnimator.setRepeatMode(ValueAnimator.REVERSE);
        loadingAnimator.setDuration(500);

        FragmentManager fm = getFragmentManager();
        retainedFragment = (RetainedFragment) fm.findFragmentByTag("retainedfragment");

        if (retainedFragment == null) {
            retainedFragment = new RetainedFragment();
            fm.beginTransaction().add(retainedFragment, "retainedfragment").commit();
        }

        if (retainedFragment.bitmap == null) {
            //loadPage(currentChapter, currentPage, 0);
        }
        else {
            chapterMap.put(retainedFragment.chapter, retainedFragment.pages);

            final View content = findViewById(android.R.id.content);
            content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (retainedFragment.bitmap != null) {
                        String key = MemoryCacheUtils.generateKey(retainedFragment.uri, new ImageSize(content.getWidth(), content.getHeight()));
                        //imageLoader.getMemoryCache().put(key, retainedFragment.bitmap);
                    }
                    //loadPage(currentChapter, currentPage, 0);
                }
            });
        }

        loadImages();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("chapter", currentChapter);
        outState.putInt("page", currentPage);
        outState.putSerializable("manga", manga);
        super.onSaveInstanceState(outState);
    }

    private void loadImages() {
        final Page[] pages = chapterMap.get(currentChapter.number);

        if (pages == null) {
            new DownloadPages(currentChapter, 1, 0, true).execute();
            return;
        }

        ImageLoader loader = App.getImageLoader();

        String[] uris = {
                "https://cdn.mangaeden.com/mangasimg/72/72afd09a9819f21ed8fc5dc919f1a081de32b32966c8999d80501666.jpg",
                //"https://cdn.mangaeden.com/mangasimg/f7/f77ed745f3423c91c226d60118fada1f43aaee1f8810b917540b9dd4.jpg",
                "https://cdn.mangaeden.com/mangasimg/30/303ef5d426e5445681cc42bcd92e5abfd1b044b674fb4fcf501d4f65.jpg",
                //"https://cdn.mangaeden.com/mangasimg/5d/5d087854b7dc4fa2a718d5c8392157310ebf4fabdb3dd5621253ce49.jpg",
                "https://cdn.mangaeden.com/mangasimg/54/543cfcd6edbf3663e3d9fccf2bc87ba467f5dcb645ae55d6d1ff421c.jpg"
        };

        //loader.displayImage(uris[0], imageViewPrev, new ILL(attacherPrev));
        loader.displayImage(uris[1], imageViewCurr, new ILL(attacherCurr));
        //loader.displayImage(uris[2], imageViewNext, new ILL(attacherNext));



        imageViewPrev.setTranslationY(-800);
        //imageViewCurr.setTranslationY(0);
        imageViewNext.setTranslationY(800);
    }

    private class ILL implements ImageLoadingListener {
        private PhotoViewAttacher attacher;

        ILL(PhotoViewAttacher attacher) {
            this.attacher = attacher;
        }

        @Override
        public void onLoadingStarted(String imageUri, View view) {
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            Log.d("loadComplete", loadedImage.getWidth() + "-" + loadedImage.getHeight());

            //RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.relative);
            //relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, loadedImage.getHeight()));
            //ViewGroup.LayoutParams params = relativeLayout.getLayoutParams();
            //params.height = loadedImage.getHeight();
            //relativeLayout.setLayoutParams(params);
            imageViewCurr.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, loadedImage.getHeight()));
            attacher.update();
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Log.i("Height", "" + imageViewCurr.getHeight());
            attacherCurr.update();
            //nextPage();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            //prevPage();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || super.onKeyUp(keyCode, event);
    }

    private class ChapterData {
        @SerializedName("images")
        Page[] pages;
        Double number;
    }

    private class Page {
        int number;
        String url;
        int width;
        int height;

        public Page(int number, String url, int width, int height) {
            this.number = number;
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }

    private class DownloadPages extends AsyncTask<Void, Void, ChapterData> {
        Chapter chapter;
        int page;
        int param;
        boolean preload;

        public DownloadPages(Chapter chapter, int page, int param, boolean preload) {
            this.chapter = chapter;
            this.page = page;
            this.param = param;
            this.preload = preload;
        }

        @Override
        protected ChapterData doInBackground(Void... p) {
            HttpURLConnection con = null;
            try {
                Log.i("DownloadChapterData", "Open Connection");

                URL url = new URL("https://www.mangaeden.com/api/chapter/" + chapter.id);
                con = (HttpURLConnection) url.openConnection();

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Page.class, new PageDeserializer());
                Gson gson = gsonBuilder.create();
                InputStream in = con.getInputStream();
                JsonReader reader = new JsonReader(new InputStreamReader(in));

                ChapterData chapterData = gson.fromJson(reader, ChapterData.class);
                chapterData.number = chapter.number;

                reader.close();

                return chapterData;
            }
            catch (IOException ex) {
                Log.e("DownloadChapterData", "Exception");
            }
            finally {
                if (con != null)
                    con.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ChapterData chapterData) {
            Log.i("DownloadChapterData", "Post Execute");

            if (chapterData != null) {
                chapterMap.put(chapterData.number, chapterData.pages);
                Page[] pages = chapterData.pages;
                Collections.reverse(Arrays.asList(pages));

                if (!preload) {
                    //loadPage(chapter, page, param);
                }
                else {
                    //preloadPage(chapter, page, param);
                }
                loadImages();
            }
            else {
                if (!preload) {
                    MessageDialog.showDialog(InfiniteImageViewerActivity.this, getString(R.string.viewer_load_chapter_failed));
                    loadingAnimator.cancel();
                    TextView loadingTextView = (TextView) findViewById(R.id.textView);
                    loadingTextView.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private class PageDeserializer implements JsonDeserializer<Page> {
        @Override
        public Page deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray jsonArray = json.getAsJsonArray();
            int number = jsonArray.get(0).getAsInt();
            String url = jsonArray.get(1).getAsString();
            int width = jsonArray.get(2).getAsInt();
            int height = jsonArray.get(3).getAsInt();
            return new Page(number, url, width, height);
        }
    }

    public static class RetainedFragment extends Fragment {
        private Bitmap bitmap;
        private Page[] pages;
        private Double chapter;
        private String uri;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }
}
