package com.galdiuz.manga;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.galdiuz.manga.classes.Chapter;
import com.galdiuz.manga.classes.Favorite;
import com.galdiuz.manga.classes.Manga;
import com.galdiuz.manga.other.CustomOnDoubleTapListener;
import com.galdiuz.manga.other.CustomOnLongClickListener;
import com.galdiuz.manga.other.CustomPhotoViewAttacher;
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
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageViewerActivity extends Activity implements CustomPhotoViewAttacher.OnDragInterceptor {

    // TODO: Set progress here
    // TODO: Reverse volume button
    // TODO: Reorder menu

    private ImageView imageViewCurrent;
    private ImageView imageViewOther;
    private CustomPhotoViewAttacher attacherCurrent;
    private CustomPhotoViewAttacher attacherOther;
    private ImageLoader imageLoader; // TODO: Replace with App.getImageLoader()
    private int currentPage;
    private Chapter currentChapter;
    private Manga manga;
    private AnimatorSet titleAnimatorSet;
    private ObjectAnimator loadingAnimator;
    private HashMap<Double, Page[]> chapterMap;
    private RetainedFragment retainedFragment;
    private Bitmap brokenBitmap; // TODO: Replace with SoftReference
    private int retries = 0;
    private int statusBarHeight = 0;

    // Preference related
    private float tapZoom;
    private int preloadPageCount;
    private boolean rememberZoom;
    private boolean animatePageSwitch;
    private int readingDirection;
    private int systemUIMode;
    private int hideUIDelay;

    private static final float MAX_ZOOM = 5f;
    private static final int MAX_RETRIES = 1;

    /*@Override
    public boolean onTouchEvent(MotionEvent event){

        int action = event.getActionMasked();

        switch (action) {
            case (MotionEvent.ACTION_DOWN) :
                Log.d("ImageViewer","Action was DOWN");
                return true;
            case (MotionEvent.ACTION_MOVE) :
                Log.d("ImageViewer","Action was MOVE");
                return true;
            case (MotionEvent.ACTION_UP) :
                Log.d("ImageViewer","Action was UP");
                return true;
            case (MotionEvent.ACTION_CANCEL) :
                Log.d("ImageViewer","Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE) :
                Log.d("ImageViewer","Movement occurred outside bounds " +
                        "of current screen element");
                return true;
            default :
                return super.onTouchEvent(event);
        }
    }*/

    float prevy = 0;
    boolean ignore = false;



    @Override
    public boolean OnDragIntercept(CustomPhotoViewAttacher attacher, float dx, float dy) {
        Log.i("OnDragIntercept", attacher.getDisplayRect().toShortString() + ", " + dy);
        float top = attacher.getDisplayRect().top;
        float bottom = attacher.getDisplayRect().bottom;


        if(ignore) {
            ignore = false;
            return true;
        }
        else if(top == 0 && dy > 0) {
            ImageView im = attacher.getImageView();

            im.setTranslationY(im.getTranslationY() + dy);
            prevy = dy;
            ignore = true;
            return true;
        }
        else if(bottom == attacher.getImageView().getHeight() && dy < 0) {
            ImageView im = attacher.getImageView();
            im.setTranslationY(im.getTranslationY() + dy);
            prevy = dy;
            ignore = true;
            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ActionBar ab = getActionBar();
        if(ab != null) ab.setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            currentChapter = (Chapter)savedInstanceState.getSerializable("chapter");
            currentPage = savedInstanceState.getInt("page");
            manga = (Manga)savedInstanceState.getSerializable("manga");
        } else {
            Intent intent = getIntent();
            currentChapter = (Chapter)intent.getSerializableExtra("chapter");
            currentPage = intent.getIntExtra("page", -1);
            manga = (Manga)intent.getSerializableExtra("manga");
        }

        loadPreferences();

        imageLoader = App.getImageLoader();
        chapterMap = new HashMap<>();

        imageViewCurrent = (ImageView)findViewById(R.id.imageView);
        imageViewOther = (ImageView)findViewById(R.id.imageView2);
        attacherCurrent = new CustomPhotoViewAttacher(imageViewCurrent);
        attacherCurrent.setMediumScale(tapZoom);
        attacherCurrent.setMaximumScale(MAX_ZOOM);
        attacherCurrent.setOnDoubleTapListener(new CustomOnDoubleTapListener(attacherCurrent));
        attacherCurrent.setOnLongClickListener(new CustomOnLongClickListener(attacherCurrent));
        //attacherCurrent.setOnDragInterceptor(this);
        /*attacherCurrent.setOnMatrixChangeListener(new PhotoViewAttacher.OnMatrixChangedListener() {
            @Override
            public void onMatrixChanged(RectF rect) {
                Log.d("onMatrixChanged", rect.toString());
            }
        });*/
        attacherOther = new CustomPhotoViewAttacher(imageViewOther);
        attacherOther.setMediumScale(tapZoom);
        attacherOther.setMaximumScale(MAX_ZOOM);
        attacherOther.setOnDoubleTapListener(new CustomOnDoubleTapListener(attacherOther));
        attacherOther.setOnLongClickListener(new CustomOnLongClickListener(attacherOther));
        //attacherOther.setOnDragInterceptor(this);
        /*attacherOther.setOnMatrixChangeListener(new PhotoViewAttacher.OnMatrixChangedListener() {
            @Override
            public void onMatrixChanged(RectF rect) {
                Log.d("onMatrixChanged", rect.toString());
            }
        });*/

        TextViewAlpha loadingTextView = (TextViewAlpha)findViewById(R.id.textView);
        loadingAnimator = ObjectAnimator.ofInt(loadingTextView, "textAlpha", 255, 0);
        loadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loadingAnimator.setRepeatMode(ValueAnimator.REVERSE);
        loadingAnimator.setDuration(500);

        FragmentManager fm = getFragmentManager();
        retainedFragment = (RetainedFragment) fm.findFragmentByTag("retainedfragment");

        View decor = getWindow().getDecorView();
        decor.setOnSystemUiVisibilityChangeListener(mOnSystemUiVisibilityChangeListener);

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(metrics.widthPixels, metrics.heightPixels + statusBarHeight);
            params.setMargins(0, statusBarHeight, 0, 0);
            imageViewCurrent.setLayoutParams(params);
            imageViewOther.setLayoutParams(params);
        }

        if(retainedFragment == null) {
            retainedFragment = new RetainedFragment();
            fm.beginTransaction().add(retainedFragment, "retainedfragment").commit();
        }

        if(retainedFragment.bitmap == null) {
            loadPage(currentChapter, currentPage, 0);
        }
        else {
            chapterMap.put(retainedFragment.chapter, retainedFragment.pages);

            final View content = findViewById(android.R.id.content);
            content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if(retainedFragment.bitmap != null) {
                        String key = MemoryCacheUtils.generateKey(retainedFragment.uri, new ImageSize(content.getWidth(), content.getHeight()));
                        imageLoader.getMemoryCache().put(key, retainedFragment.bitmap);
                    }
                    loadPage(currentChapter, currentPage, 0);
                }
            });
        }
    }



    @Override
    public void onStop() {
        super.onStop();
        App.getImageLoader().clearMemoryCache();
        if(Favorite.getFavorites().get(manga.id) != null) {
            Favorite.saveFavorites();
        }
        if(brokenBitmap != null) {
            brokenBitmap.recycle();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadPreferences();
        handler.postDelayed(runnable, hideUIDelay);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("chapter", currentChapter);
        outState.putInt("page", currentPage);
        outState.putSerializable("manga", manga);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_image_viewer, menu);
        return true;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {

        if(menu != null) {
            if(Favorite.getFavorites().get(manga.id) == null) {
                menu.findItem(R.id.favorite).setTitle(R.string.action_favorite_add);
            }
            else {
                menu.findItem(R.id.favorite).setTitle(R.string.action_favorite_remove);
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.direction) {
            DirectionDialog.showDialog(this);
        }
        else if(id == R.id.favorite) {
            Favorite favorite = Favorite.getFavorites().get(manga.id);
            if (favorite == null) {
                favorite = new Favorite(manga);
                favorite.progressChapter = currentChapter;
                favorite.progressPage = currentPage;
                Favorite.getFavorites().put(manga.id, favorite);
                Favorite.saveFavorites();
                MessageDialog.showDialog(this, getString(R.string.favorite_added, manga.title));
            }
            else {
                Favorite.getFavorites().remove(manga.id);
                Favorite.saveFavorites();
                MessageDialog.showDialog(this, getString(R.string.favorite_removed, manga.title));
            }
            return true;
        }
        else if(id == R.id.jump) {
            MessageDialog.showDialog(this, "Not yet implemented");
            // TODO: Implement
            return true;
        }
        else if(id == R.id.reload) {
            loadPage(currentChapter, currentPage, 0);
            return true;
        }
        else if(id == R.id.chapter_list) {
            Intent intent = new Intent(this, ChapterListActivity.class);
            intent.putExtra("title", manga.title);
            intent.putExtra("id", manga.id);
            startActivity(intent);
            return true;
        }
        else if(id == R.id.prev_chapter) {
            Chapter prev = getPrevChapter(currentChapter);
            if(prev != null) {
                loadPage(prev, 1, 0);
            }
            else {
                MessageDialog.showDialog(this, getString(R.string.viewer_first_chapter, manga.title));
            }
            return true;
        }
        else if(id == R.id.next_chapter) {
            Chapter next = getNextChapter(currentChapter);
            if(next != null) {
                loadPage(next, 1, 0);
            }
            else {
                MessageDialog.showDialog(this, getString(R.string.viewer_last_chapter, manga.title));
            }
            return true;
        }
        else if(id == R.id.settings) {
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            nextPage();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            prevPage();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || super.onKeyUp(keyCode, event);
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences(App.PREFSNAME, 0);
        rememberZoom = preferences.getBoolean("rememberZoom", true);
        animatePageSwitch = preferences.getBoolean("animatePageSwitch", true);
        systemUIMode = preferences.getInt("systemUIMode", 1);
        preloadPageCount = preferences.getInt("preloadPageCount", 3);
        tapZoom = preferences.getFloat("tapZoom", 1.5f);
        Favorite favorite = Favorite.getFavorites().get(manga.id);
        readingDirection = favorite != null ? favorite.direction : 0;
        hideUIDelay = preferences.getInt("hideUIDelay", 3000);
    }

    private void nextPage() {
        Pair<Chapter, Integer> page = getNextPage(currentChapter, currentPage);
        if(page.first != null) {
            loadPage(page.first, page.second, 1);
        }
        else {
            MessageDialog.showDialog(this, getString(R.string.viewer_last_page, manga.title));
        }
    }

    private void prevPage() {
        Pair<Chapter, Integer> page = getPrevPage(currentChapter, currentPage);
        if(page.first != null) {
            loadPage(page.first, page.second, -1);
        }
        else {
            MessageDialog.showDialog(this, getString(R.string.viewer_first_page, manga.title));
        }

    }

    private Pair<Chapter, Integer> getNextPage(Chapter chapter, int page) {
        Page[] pages = chapterMap.get(chapter.number);
        if(pages != null) {
            if(page + 1 > pages.length) {
                return new Pair<>(getNextChapter(chapter), 1);
            }
            else if (page + 1 > 0) {
                return new Pair<>(chapter, page + 1);
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        throw new RuntimeException();
    }

    private Pair<Chapter, Integer> getPrevPage(Chapter chapter, int page) {
        Page[] pages = chapterMap.get(chapter.number);
        if(pages != null) {
            if(page - 1 == 0) {
                return new Pair<>(getPrevChapter(chapter), -1);
            }
            else if (page - 1 < pages.length) {
                return new Pair<>(chapter, page - 1);
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        throw new RuntimeException();
    }

    private Chapter getNextChapter(Chapter chapter) {
        int cindex = manga.chapters.indexOf(chapter);
        if(cindex < manga.chapters.size() - 1) {
            return manga.chapters.get(cindex + 1);
        }
        else {
            return null; // Last chapter
        }
    }

    private Chapter getPrevChapter(Chapter chapter) {
        int cindex = manga.chapters.indexOf(chapter);
        if(cindex > 0) {
            return manga.chapters.get(cindex - 1);
        }
        else {
            return null; // First chapter
        }
    }

    private void loadBrokenBitmap() {
        if(brokenBitmap == null || brokenBitmap.isRecycled()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            brokenBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.broken, options);
        }

    }

    private void loadPage(final Chapter chapter, final int page, final int animType) {
        final TextView loadingTextView = (TextView)findViewById(R.id.textView);
        final Page[] pages = chapterMap.get(chapter.number);

        if(pages == null) {
            loadingTextView.setText(getString(R.string.viewer_loading_bar_chapter));
            loadingTextView.setVisibility(View.VISIBLE);
            loadingAnimator.start();
            new DownloadPages(chapter, page, animType, false).execute();
            return;
        }
        if(page <= -1) {
            loadPage(chapter, pages.length, animType);
            return;
        }
        else if (page > pages.length) {
            // TODO: Invalid page
            return;
        }

        loadingTextView.setText(getString(R.string.viewer_loading_bar_page, page));
        loadingTextView.setVisibility(View.VISIBLE);
        loadingAnimator.start();

        final String uri = "http://cdn.mangaeden.com/mangasimg/" + pages[page - 1].url;

        imageLoader.displayImage(uri, imageViewOther, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {}

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                // TODO: Try again?
                // TODO: Temporary solution
                if(failReason.getType() == FailReason.FailType.OUT_OF_MEMORY) {
                    MessageDialog.showDialog(ImageViewerActivity.this, "Out of memory\nImages in memory: " + imageLoader.getMemoryCache().keys().size());
                    imageLoader.getMemoryCache().clear();
                    retries++;
                    if(retries <= MAX_RETRIES) {
                        loadPage(chapter, page, animType);
                    }
                    else {
                        retries = 0;
                    }
                }
                else {
                    // TODO: Replace with string resource
                    MessageDialog.showDialog(ImageViewerActivity.this, "Unable to load image\n" + failReason.getType().name());
                    loadBrokenBitmap();
                    imageViewOther.setImageBitmap(brokenBitmap);
                    switchPage(chapter, page, animType, imageUri, brokenBitmap);
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                switchPage(chapter, page, animType, imageUri, loadedImage);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {}
        });

    }

    private void preloadPage(final Chapter chapter, final int page, final int remaining) {
        if(remaining <= 0) return;
        final Page[] pages = chapterMap.get(chapter.number);

        if(pages == null) {
            new DownloadPages(chapter, page, remaining, true).execute();
        }
        else {
            String uri = "http://cdn.mangaeden.com/mangasimg/" + pages[page - 1].url;
            List<String> keys = MemoryCacheUtils.findCacheKeysForImageUri(uri, imageLoader.getMemoryCache());
            if(keys.size() > 0) {
                // Image already in memory, skip
                Pair<Chapter, Integer> nextPage = getNextPage(chapter, page);
                if (nextPage.first != null) {
                    preloadPage(nextPage.first, nextPage.second, remaining - 1);
                }
            }
            else {
                imageLoader.loadImage(uri, new ImageSize(pages[page - 1].width, pages[page - 1].height), new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {}

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        // TODO: Retry?
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        Log.d("preloadPage", "Preloaded " + chapter.number + ":" + page);
                        Pair<Chapter, Integer> nextPage = getNextPage(chapter, page);
                        if (nextPage.first != null) {
                            preloadPage(nextPage.first, nextPage.second, remaining - 1);
                        }
                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {}
                });
            }
        }
    }

    private View.OnSystemUiVisibilityChangeListener mOnSystemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            int i = visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == View.VISIBLE) {
                ActionBar ab = getActionBar();
                if(ab != null) {
                    ab.show();
                }
                handler.postDelayed(runnable, hideUIDelay);
            }
            else {
                ActionBar ab = getActionBar();
                if(ab != null) {
                    ab.hide();
                }
            }
        }
    };


    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            hideUI();
        }
    };


    private void hideUI() {
        systemUIMode = 1;
        View decor = getWindow().getDecorView();

        if(systemUIMode == 1) {
            if(Build.VERSION.SDK_INT >= 19) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE);
            }
            else {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            }
        }
        else if(systemUIMode == 2) {
            if(Build.VERSION.SDK_INT >= 19) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
            else {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
        }
        else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void switchPage(Chapter chapter, int page, int animType, String uri, Bitmap loadedImage) {
        int[] maxTextureSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

        // Swap imageViews and attachers
        ImageView t1 = imageViewCurrent;
        imageViewCurrent = imageViewOther;
        imageViewOther = t1;

        CustomPhotoViewAttacher t2 = attacherCurrent;
        attacherCurrent = attacherOther;
        attacherOther = t2;

        attacherCurrent.update();
        currentChapter = chapter;
        currentPage = page;

        // Update titleTextView
        TextView titleTextView = (TextView)findViewById(R.id.textView2);
        NumberFormat nf = Chapter.getChapterFormatter();
        String title = getString(R.string.viewer_title_bar, manga.title, nf.format(currentChapter.number), page, chapterMap.get(chapter.number).length);
        titleTextView.setText(title);
        if(titleAnimatorSet != null) {
            titleAnimatorSet.cancel();
        }
        titleAnimatorSet = new AnimatorSet(); // TODO: Fix multirow issue
        ObjectAnimator o1 = ObjectAnimator.ofFloat(titleTextView, "translationY", titleTextView.getTranslationY(), statusBarHeight);
        ObjectAnimator o2 = ObjectAnimator.ofFloat(titleTextView, "translationY", statusBarHeight, -titleTextView.getHeight());
        o2.setStartDelay(2000);
        titleAnimatorSet.playSequentially(o1, o2);
        titleAnimatorSet.start();

        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setTitle(title);
        }

        // Update loadingTextView
        loadingAnimator.cancel();
        TextView loadingTextView = (TextView)findViewById(R.id.textView);
        loadingTextView.setVisibility(View.INVISIBLE);

        // TODO: If far ahead, check if progress should be saved
        // Save favorite progress
        Favorite favorite = Favorite.getFavorites().get(manga.id);
        if(favorite != null && (favorite.progressChapter == null || currentChapter.number > favorite.progressChapter.number ||
                (currentPage > favorite.progressPage && currentChapter.number == favorite.progressChapter.number))) {
            favorite.progressPage = currentPage;
            favorite.progressChapter = currentChapter;
            Favorite.saveFavorites();
        }

        if(brokenBitmap == null || loadedImage != brokenBitmap) {
            retainedFragment.bitmap = loadedImage;
        }
        else {
            retainedFragment.bitmap = null;
        }
        retainedFragment.pages = chapterMap.get(chapter.number);
        retainedFragment.chapter = chapter.number;
        retainedFragment.uri = uri;

        // TODO: Implement mode for webtoons?
        // If landscape, fit to width
        if(readingDirection == 2 || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            float scale = (float)loadedImage.getHeight() / imageViewCurrent.getMeasuredHeight();
            float widthScale = (float)loadedImage.getWidth() / imageViewCurrent.getMeasuredWidth();
            if(widthScale < 1) scale /= widthScale;

            if(scale < 1) scale = 1;
            if(scale > attacherCurrent.getMinimumScale()) {
                attacherCurrent.setMaximumScale(MAX_ZOOM * scale);
                attacherCurrent.setMediumScale(tapZoom * scale);
                attacherCurrent.setMinimumScale(scale);
            }
            else {
                attacherCurrent.setMinimumScale(scale);
                attacherCurrent.setMediumScale(tapZoom * scale);
                attacherCurrent.setMaximumScale(MAX_ZOOM * scale);
            }
            attacherCurrent.setScale(scale);
        }

        if(rememberZoom) {
            int x = 0;
            if(readingDirection == 0) {
                x = loadedImage.getWidth();
            }
            float scale = (attacherOther.getScale() / attacherOther.getMinimumScale()) * attacherCurrent.getMinimumScale();
            attacherCurrent.setScale(scale, x, 0, false);
        }

        // Handle status bar
        hideUI();

        // Page switch animation
        if(animatePageSwitch && animType != 0) {
            if (animType == 1) {
                imageViewCurrent.bringToFront();
                Animation a;
                if(readingDirection == 2) {
                    int y = imageViewCurrent.getHeight();
                    a = new TranslateAnimation(0, 0, y ,0);
                }
                else if(readingDirection == 1) {
                    int x = imageViewCurrent.getWidth();
                    a = new TranslateAnimation(x, 0, 0, 0);
                }
                else {
                    int x = -imageViewCurrent.getWidth();
                    a = new TranslateAnimation(x, 0, 0, 0);
                }
                a.setDuration(200);
                imageViewCurrent.startAnimation(a);
            } else if (animType == -1) {
                imageViewOther.bringToFront();
                Animation a;
                if(readingDirection == 2) {
                    int y = imageViewCurrent.getHeight();
                    a = new TranslateAnimation(0, 0, 0, y);
                }
                else if (readingDirection == 1) {
                    int x = imageViewCurrent.getWidth();
                    a = new TranslateAnimation(0, x, 0, 0);
                }
                else {
                    int x = -imageViewCurrent.getWidth();
                    a = new TranslateAnimation(0, x, 0, 0);
                }
                a.setDuration(200);
                a.setFillAfter(true);
                a.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        imageViewCurrent.bringToFront();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                imageViewOther.startAnimation(a);
                imageViewCurrent.clearAnimation();
            }
        }
        else {
            imageViewCurrent.clearAnimation();
            imageViewCurrent.bringToFront();
        }

        // Preload pages
        Pair<Chapter, Integer> nextPage = getNextPage(currentChapter, currentPage);
        if(nextPage.first != null) {
            preloadPage(nextPage.first, nextPage.second, preloadPageCount);
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
            catch(IOException ex) {
                Log.e("DownloadChapterData",  "Exception");
            }
            finally {
                if(con != null)
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

                if(!preload) {
                    loadPage(chapter, page, param);
                }
                else {
                    preloadPage(chapter, page, param);
                }
            }
            else {
                if(!preload) {
                    MessageDialog.showDialog(ImageViewerActivity.this, getString(R.string.viewer_load_chapter_failed));
                    loadingAnimator.cancel();
                    TextView loadingTextView = (TextView)findViewById(R.id.textView);
                    loadingTextView.setVisibility(View.INVISIBLE);
                }
            }
        }
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

    public static class DirectionDialog extends DialogFragment {
        public static void showDialog(Activity activity) {
            new DirectionDialog().show(activity.getFragmentManager(), "directiondialog");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // TODO: Replace with string resources
            String[] items = {"Right-to-left (Manga)", "Left-to-right (Manhwa)", "Up-down (Webtoon)"};
            int selected = ((ImageViewerActivity)getActivity()).readingDirection;
            builder.setTitle("Reading direction")
                    .setSingleChoiceItems(items, selected, null)
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ImageViewerActivity activity = (ImageViewerActivity) getActivity();
                            int direction = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                            activity.readingDirection = direction;
                            Favorite favorite = Favorite.getFavorites().get(activity.manga.id);
                            if(favorite != null) {
                                favorite.direction = direction;
                                Favorite.saveFavorites();
                            }
                        }
                    });
            return builder.create();
        }
    }
}
