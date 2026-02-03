package com.ytmusic.pro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "YTMusicProPlayback";
    private static final int NOTIFICATION_ID = 888;
    private static final long IDLE_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    
    private MediaSessionCompat mediaSession;
    private android.os.Handler idleHandler = new android.os.Handler();
    private Runnable idleRunnable;
    private boolean isPaused = true;

    public static final String ACTION_PLAY = "com.ytmusic.pro.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.ytmusic.pro.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.ytmusic.pro.ACTION_NEXT";
    public static final String ACTION_PREV = "com.ytmusic.pro.ACTION_PREV";
    public static final String ACTION_SEEK = "com.ytmusic.pro.ACTION_SEEK";
    public static final String ACTION_STOP = "com.ytmusic.pro.ACTION_STOP";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "YTMusicProSession");

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onSeekTo(long pos) {
                Intent intent = new Intent("YTPRO_CONTROL");
                intent.putExtra("action", ACTION_SEEK);
                intent.putExtra("position", pos / 1000);
                sendBroadcast(intent);
            }

            @Override
            public void onPlay() {
                handleAction(ACTION_PLAY);
            }

            @Override
            public void onPause() {
                handleAction(ACTION_PAUSE);
            }

            @Override
            public void onSkipToNext() {
                handleAction(ACTION_NEXT);
            }

            @Override
            public void onSkipToPrevious() {
                handleAction(ACTION_PREV);
            }
        });

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SEEK_TO)
                .build());
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_NOT_STICKY;

        String action = intent.getAction();
        if (action != null) {
            handleAction(action);
        }

        String title = intent.getStringExtra("title");
        String artist = intent.getStringExtra("artist");
        String albumArtUrl = intent.getStringExtra("albumArt");
        boolean isPlaying = intent.getBooleanExtra("isPlaying", true);
        long position = intent.getLongExtra("position", 0);
        long duration = intent.getLongExtra("duration", 0);
        
        // Manage idle timer based on playback state
        manageIdleTimer(isPlaying);

        // Update MediaSession Position and Meta
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        position, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SEEK_TO)
                .build());

        android.support.v4.media.MediaMetadataCompat.Builder metaBuilder = new android.support.v4.media.MediaMetadataCompat.Builder()
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, duration);

        new Thread(() -> {
            Bitmap art = null;
            if (albumArtUrl != null && !albumArtUrl.isEmpty()) {
                art = downloadBitmap(albumArtUrl);
                if (art != null) {
                    metaBuilder.putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);
                }
            }
            mediaSession.setMetadata(metaBuilder.build());
            showNotification(title, artist, art, isPlaying);
        }).start();

        return START_NOT_STICKY;
    }

    private void handleAction(String action) {
        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            return;
        }
        
        Intent intent = new Intent("YTPRO_CONTROL");
        intent.putExtra("action", action);
        sendBroadcast(intent);
    }
    
    private void manageIdleTimer(boolean isPlaying) {
        isPaused = !isPlaying;
        
        // Cancel existing timer
        if (idleRunnable != null) {
            idleHandler.removeCallbacks(idleRunnable);
        }
        
        // Start timer only when paused
        if (isPaused) {
            idleRunnable = new Runnable() {
                @Override
                public void run() {
                    // Stop service after idle timeout
                    stopForeground(true);
                    stopSelf();
                }
            };
            idleHandler.postDelayed(idleRunnable, IDLE_TIMEOUT);
        }
    }

    private void showNotification(String title, String artist, Bitmap albumArt, boolean isPlaying) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        // Actions
        NotificationCompat.Action playPauseAction = isPlaying
                ? new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause",
                        getActionPendingIntent(ACTION_PAUSE))
                : new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play",
                        getActionPendingIntent(ACTION_PLAY));

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title != null ? title : "YTMusic Pro")
                .setContentText(artist != null ? artist : "Playing...")
                .setLargeIcon(albumArt)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying)
                .addAction(android.R.drawable.ic_media_previous, "Previous", getActionPendingIntent(ACTION_PREV))
                .addAction(playPauseAction)
                .addAction(android.R.drawable.ic_media_next, "Next", getActionPendingIntent(ACTION_NEXT))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private PendingIntent getActionPendingIntent(String action) {
        Intent intent = new Intent(this, ForegroundService.class);
        intent.setAction(action);
        // Use unique request codes for each action to prevent PendingIntent collision
        int requestCode = 0;
        switch (action) {
            case ACTION_PLAY:
                requestCode = 1;
                break;
            case ACTION_PAUSE:
                requestCode = 2;
                break;
            case ACTION_NEXT:
                requestCode = 3;
                break;
            case ACTION_PREV:
                requestCode = 4;
                break;
        }
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private Bitmap downloadBitmap(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            android.util.Log.e("YTMusicPro", "Error downloading bitmap: " + e.getMessage());
            return null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "YTMusic Pro Playback",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (idleRunnable != null) {
            idleHandler.removeCallbacks(idleRunnable);
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
    }
}
