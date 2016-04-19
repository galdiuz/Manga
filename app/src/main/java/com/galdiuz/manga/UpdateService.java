package com.galdiuz.manga;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.galdiuz.manga.classes.Chapter;
import com.galdiuz.manga.classes.ChapterDeserializer;
import com.galdiuz.manga.classes.Favorite;
import com.galdiuz.manga.classes.Manga;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

public class UpdateService extends Service {
    public static final int NOTIFICATION_ONGOING_ID = 1;
    public static final int NOTIFICATION_NEW_ID = 2;
    public static final String UPDATE = "com.galdiuz.manga.UpdateService.UPDATE";
    public static final int PROGRESS = 0;
    public static final int DONE = 1;

    private LocalBroadcastManager broadcaster;
    private NotificationManager manager;
    private Notification.Builder builder;
    private ServiceHandler serviceHandler;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO: Ability to choose which favorites to check
            ArrayList<Favorite> all = new ArrayList<>(Favorite.getFavorites().values());
            ArrayList<Favorite> list = new ArrayList<>();
            ArrayList<Favorite> updated = new ArrayList<>();

            for(Favorite f : all) {
                if(f.checkForUpdates) {
                    list.add(f);
                }
            }
            int i = 0;
            for(Favorite f : list) {
                i++;
                broadcastProgress(i, list.size(), f.title);
                HttpURLConnection con = null;
                try {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    URL url = new URL("https://www.mangaeden.com/api/manga/" + f.id);
                    con = (HttpURLConnection) url.openConnection();

                    Gson gson = new GsonBuilder().registerTypeAdapter(Chapter.class, new ChapterDeserializer()).create();
                    JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));

                    Manga manga = gson.fromJson(reader, Manga.class);
                    if(manga.lastChapterDate > f.lastChapterDate) {
                        for(Chapter c : manga.chapters) {
                            if(c.date == manga.lastChapterDate) {
                                f.lastChapterNumber = c.number;
                                break;
                            }
                        }
                        if(f.progressChapter != null && f.lastChapterNumber != f.progressChapter.number) {
                            updated.add(f);
                            f.markAsNew = true;
                        }
                        f.lastChapterDate = manga.lastChapterDate;
                    }
                }
                catch(IOException ex) {
                    Log.e("UpdateTask", "Exception", ex);
                }
                finally {
                    if(con != null) {
                        con.disconnect();
                    }
                }
            }
            manager.cancel(NOTIFICATION_ONGOING_ID);
            if(updated.size() > 0) {
                builder.setContentTitle("Manga")
                        .setContentText(getString(R.string.favorite_notification_new_chapters, updated.size()))
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                manager.notify(NOTIFICATION_NEW_ID, builder.build());

                for(Favorite f : updated) {
                    f.updated = true;
                }
                Favorite.saveFavorites();
            }
            Intent broadcastIntent = new Intent(UPDATE);
            broadcastIntent.putExtra("type", DONE);
            broadcastIntent.putExtra("updated", updated.size());
            broadcaster.sendBroadcast(broadcastIntent);

            removeCallbacksAndMessages(null);
            stopSelf();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
        manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new Notification.Builder(UpdateService.this);

        builder.setContentTitle(getString(R.string.favorite_notification_checking_title))
                .setSmallIcon(R.mipmap.ic_launcher);
        startForeground(NOTIFICATION_ONGOING_ID, builder.build());

        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        serviceHandler = new ServiceHandler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceHandler.sendEmptyMessage(1);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        manager.cancel(NOTIFICATION_ONGOING_ID);
    }

    private void broadcastProgress(int current, int total, String title) {
        builder.setProgress(total, current, false)
                .setContentText(getString(R.string.favorite_notification_checking_text, current, total, title));
        manager.notify(NOTIFICATION_ONGOING_ID, builder.build());

        Intent broadcastIntent = new Intent(UPDATE);
        broadcastIntent.putExtra("type", PROGRESS);
        broadcastIntent.putExtra("current", current);
        broadcastIntent.putExtra("total", total);
        broadcastIntent.putExtra("title", title);
        broadcaster.sendBroadcast(broadcastIntent);
    }
}
