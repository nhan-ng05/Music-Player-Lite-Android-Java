package com.example.musicplayerlite;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    public static final String ACTION_PLAY = "com.example.musicplayerlite.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.musicplayerlite.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.musicplayerlite.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.musicplayerlite.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.musicplayerlite.ACTION_STOP";

    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private List<Song> songsList = new ArrayList<>();
    private int currentSongIndex = -1;
    private Song currentSong;

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    private final IBinder musicBinder = new MusicBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();   // *** FIX QUAN TRỌNG ***

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setWakeMode(getApplicationContext(), android.os.PowerManager.PARTIAL_WAKE_LOCK);
    }

    // --- Notification Channel ---
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Kênh Phát Nhạc",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Kênh thông báo cho ứng dụng phát nhạc.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // --- Nhận danh sách bài hát và chỉ số ---
        if (intent != null && intent.hasExtra("SONGS_LIST") && intent.hasExtra("SONG_INDEX")) {

            int newIndex = intent.getIntExtra("SONG_INDEX", 0);

            List<Song> newSongsList = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                newSongsList = intent.getParcelableArrayListExtra("SONGS_LIST", Song.class);
            } else {
                ArrayList<?> raw = intent.getParcelableArrayListExtra("SONGS_LIST");
                if (raw != null) {
                    newSongsList = new ArrayList<>();
                    for (Object obj : raw) {
                        if (obj instanceof Song) {
                            newSongsList.add((Song) obj);
                        }
                    }
                }
            }


            if (newSongsList != null && !newSongsList.isEmpty()) {
                songsList = newSongsList;
            }

            if (!songsList.isEmpty() && newIndex != currentSongIndex) {
                currentSongIndex = newIndex;
                Song newSong = songsList.get(currentSongIndex);
                playNewSongFromUri(newSong.getContentUri().toString());
            }
        }

        // --- StartForeground CHỈ GỌI 1 LẦN ---
        startForeground(NOTIFICATION_ID, buildNotification());

        // --- Xử lý Action ---
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    startPlayback();
                    break;
                case ACTION_PAUSE:
                    pausePlayback();
                    break;
                case ACTION_PREVIOUS:
                    playPreviousSong();
                    break;
                case ACTION_NEXT:
                    playNextSong();
                    break;
                case ACTION_STOP:
                    stopSelf();
                    break;
            }
        } else if (intent != null && intent.hasExtra("SONG_URI")) {
            playNewSongFromUri(intent.getStringExtra("SONG_URI"));
        }

        // --- Cập nhật notification ---
        updateNotification();

        return START_STICKY;
    }

    // --- Build Notification ---
    private Notification buildNotification() {

        if (songsList.isEmpty() || currentSongIndex < 0 || currentSongIndex >= songsList.size()) {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Music Player")
                    .setContentText("No song playing")
                    .setSmallIcon(R.drawable.note)   // *** icon của bạn ***
                    .build();
        }

        Song song = songsList.get(currentSongIndex);

        boolean isPlaying = mediaPlayer != null && mediaPlayer.isPlaying();

        PendingIntent prevIntent = createPendingIntent(ACTION_PREVIOUS);
        PendingIntent playIntent = createPendingIntent(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent nextIntent = createPendingIntent(ACTION_NEXT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.note)   // *** icon của bạn, không sửa ***
                .setContentTitle(song.getName())
                .setContentText(song.getArtist())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)

                .addAction(R.drawable.previous, "Previous", prevIntent)
                .addAction(isPlaying ? R.drawable.pause : R.drawable.play,
                        isPlaying ? "Pause" : "Play",
                        playIntent)
                .addAction(R.drawable.next, "Next", nextIntent)

                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))

                .build();
    }

    // --- updateNotification KHÔNG ĐƯỢC GỌI startForeground() ---
    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, buildNotification());
    }

    private PendingIntent createPendingIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
    }

    // --- Media Control ---
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        updateNotification();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("MusicService", "MediaPlayer error: " + what);
        return false;
    }

    public void startPlayback() {
        if (mediaPlayer != null) mediaPlayer.start();
        updateNotification();
    }

    public void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
        updateNotification();
    }

    public void playNextSong() {
        if (songsList.isEmpty()) return;

        currentSongIndex = (currentSongIndex + 1) % songsList.size();
        playNewSongFromUri(songsList.get(currentSongIndex).getContentUri().toString());
    }

    public void playPreviousSong() {
        if (songsList.isEmpty()) return;

        currentSongIndex--;
        if (currentSongIndex < 0) currentSongIndex = songsList.size() - 1;

        playNewSongFromUri(songsList.get(currentSongIndex).getContentUri().toString());
    }

    private void playNewSongFromUri(String uriString) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );

            Uri uri = Uri.parse(uriString);
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e("MusicService", "Lỗi phát nhạc: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBinder;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
        }
        super.onDestroy();
    }

    // --- Helper ---
    public int getCurrentPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void seekTo(int pos) {
        if (mediaPlayer != null) mediaPlayer.seekTo(pos);
    }
    public String getCurrentSongTitle() {
        if (songsList == null || songsList.isEmpty() ||
                currentSongIndex < 0 || currentSongIndex >= songsList.size()) {
            return "";
        }
        return songsList.get(currentSongIndex).getName();
    }

    public String getCurrentSongArtist() {
        if (songsList == null || songsList.isEmpty() ||
                currentSongIndex < 0 || currentSongIndex >= songsList.size()) {
            return "";
        }
        return songsList.get(currentSongIndex).getArtist();
    }

}
