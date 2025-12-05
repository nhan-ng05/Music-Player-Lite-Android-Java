package com.example.musicplayerlite;

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
    private List<Song> songsList = new ArrayList<>(); // Danh sách bài hát
    private int currentSongIndex = -1;

    // Song hiện tại đang phát
    private Song currentSong;

    // Trong MusicService.java (Thêm lớp nội bộ này)
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this; // Trả về đối tượng MusicService
        }
    }

    // Hàm tạo PendingIntent
    private PendingIntent createActionIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    // Thêm biến Binder
    private final IBinder musicBinder = new MusicBinder();

    public void playNewSong(String path) {
        try {
            mediaPlayer.reset();

            // 1. Chuyển đường dẫn thành Content URI (Cách tốt nhất cho MediaStore)
            Uri uri = Uri.parse(path);

            // 2. Thiết lập Data Source bằng URI
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("MusicService", "Lỗi IO khi đặt Data Source: " + e.getMessage());
            // Thường xảy ra nếu đường dẫn không thể truy cập
            stopSelf(); // Dừng Service nếu không thể phát
        } catch (IllegalArgumentException e) {
            Log.e("MusicService", "Lỗi đối số Data Source (URI không hợp lệ): " + e.getMessage());
            stopSelf();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Khởi tạo MediaPlayer
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setWakeMode(getApplicationContext(), android.os.PowerManager.PARTIAL_WAKE_LOCK);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // Chuẩn bị xong, bắt đầu phát nhạc
        mp.start();
        // Sau này bạn sẽ cập nhật Notification tại đây
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // Xử lý lỗi
        Log.e("MusicService", "MediaPlayer error: " + what);
        return false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Kênh Phát Nhạc",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Kênh thông báo cho ứng dụng phát nhạc.");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Luôn gọi startForeground() khi nhận lệnh để duy trì Service
        startForeground(NOTIFICATION_ID, buildNotification());

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_PLAY:
                    startPlayback();
                    break;
                case ACTION_PAUSE:
                    pausePlayback();
                    break;
                case ACTION_PREVIOUS:
                    playPreviousSong(); // Cần thêm hàm này
                    break;
                case ACTION_NEXT:
                    playNextSong(); // Cần thêm hàm này
                    break;
                case ACTION_STOP:
                    stopSelf(); // Dừng Service hoàn toàn
                    break;
            }
        } else if (intent != null && intent.hasExtra("SONG_URI")) {
            // Xử lý khi Intent có URI (tức là người dùng chọn bài mới)
            String songUri = intent.getStringExtra("SONG_URI");
            playNewSongFromUri(songUri);
        }

        // Luôn cập nhật Notification sau khi thực hiện bất kỳ lệnh nào
        updateNotification();

        return START_STICKY;
    }

    public String getCurrentSongTitle() {
        // Giả sử bạn có biến currentSong hoặc songsList.get(currentSongIndex)
        if (currentSongIndex >= 0 && currentSongIndex < songsList.size()) {
            return songsList.get(currentSongIndex).getName(); // Giả sử Song có hàm getTitle()
        }
        return "Unknown Title";
    }

    public String getCurrentSongArtist() {
        if (currentSongIndex >= 0 && currentSongIndex < songsList.size()) {
            return songsList.get(currentSongIndex).getArtist(); // Giả sử Song có hàm getArtist()
        }
        return "Unknown Artist";
    }

    public void playNextSong() {
        if (songsList.isEmpty()) return;

        currentSongIndex = (currentSongIndex + 1) % songsList.size();
        Song nextSong = songsList.get(currentSongIndex);

        // Giả sử playNewSongFromUri nhận URI String và cần cập nhật Notification
        playNewSongFromUri(nextSong.getContentUri().toString());
    }

    public void playPreviousSong() {
        if (songsList.isEmpty()) return;

        currentSongIndex--;
        if (currentSongIndex < 0) {
            currentSongIndex = songsList.size() - 1; // Phát lại bài cuối cùng
        }
        Song previousSong = songsList.get(currentSongIndex);

        playNewSongFromUri(previousSong.getContentUri().toString());
    }

    private void playNewSongFromUri(String uriString) {
        try {
            // 1. Dọn dẹp MediaPlayer cũ (nếu có)
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();

                // Thiết lập Audio Attributes để hệ thống biết đây là nhạc
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                );

                // Thiết lập Listener: Khi nhạc đã SẴN SÀNG để phát
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    updateNotification(); // Bắt đầu phát -> Cập nhật Notification
                });

                // Thiết lập Listener: Khi nhạc kết thúc -> Tự động chuyển bài
                mediaPlayer.setOnCompletionListener(mp -> playNextSong());

            } else {
                mediaPlayer.reset(); // Đặt lại MediaPlayer để tải nguồn nhạc mới
            }

            // 2. Thiết lập nguồn dữ liệu mới
            Uri uri = Uri.parse(uriString);
            mediaPlayer.setDataSource(getApplicationContext(), uri);

            // 3. Chuẩn bị phát nhạc (bất đồng bộ)
            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e("MusicService", "Lỗi IO khi đặt nguồn nhạc: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("MusicService", "Lỗi chung khi phát nhạc: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Notification buildNotification() {
        // Nếu danh sách rỗng hoặc index không hợp lệ, trả về một Notification cơ bản
        if (songsList.isEmpty() || currentSongIndex < 0 || currentSongIndex >= songsList.size()) {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Music Player")
                    .setContentText("No song playing")
                    .setSmallIcon(R.drawable.note) // Đảm bảo bạn có icon này
                    .build();
        }

        Song currentSong = songsList.get(currentSongIndex);

        // 1. Tạo các PendingIntent cho hành động điều khiển
        PendingIntent pIntentPause = createPendingIntent(ACTION_PAUSE);
        PendingIntent pIntentNext = createPendingIntent(ACTION_NEXT);
        PendingIntent pIntentPrevious = createPendingIntent(ACTION_PREVIOUS);

        // 2. Xác định trạng thái nút Play/Pause
        boolean isPlaying = mediaPlayer != null && mediaPlayer.isPlaying();
        int playPauseIcon = isPlaying ? R.drawable.pause : R.drawable.play; // Đảm bảo bạn có 2 icon này
        PendingIntent pIntentPlayPause = isPlaying ? pIntentPause : createPendingIntent(ACTION_PLAY);


        // 3. Xây dựng Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.note)
                .setContentTitle(currentSong.getName())
                .setContentText(currentSong.getArtist())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Thêm các nút điều khiển
                .addAction(R.drawable.previous, "Previous", pIntentPrevious)
                .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", pIntentPlayPause)
                .addAction(R.drawable.next, "Next", pIntentNext)
                // Thiết lập kiểu Notification Media
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2) // Hiển thị 3 nút Previous, Play/Pause, Next
                        .setMediaSession(null)); // Có thể dùng MediaSession, nhưng ta dùng cách đơn giản hơn ở đây

        return builder.build();
    }

    private void updateNotification() {
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    // Hàm trợ giúp để tạo PendingIntent (Cần thiết cho các action)
    private PendingIntent createPendingIntent(String action) {
        Intent intent = new Intent(action);
        intent.setClass(this, MusicService.class);
        // Lưu ý: FLAG_IMMUTABLE là yêu cầu bắt buộc từ Android 12 trở lên
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBinder;
    }

    // Yêu cầu 5: Dừng Service khi nhạc tắt và Service bị hủy
    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    // Trong MusicService.java (Thêm các phương thức public này)
    public void startPlayback() {
        if (mediaPlayer != null) mediaPlayer.start();
        // Cập nhật Notification và trạng thái UI
    }

    public void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
        // Cập nhật Notification và trạng thái UI
    }

    public int getCurrentPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) mediaPlayer.seekTo(position);
    }
}