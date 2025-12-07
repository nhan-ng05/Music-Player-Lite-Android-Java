package com.example.musicplayerlite;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class PlaybackActivity extends AppCompatActivity {
    private TextView tvTitle, tvArtist, tvCurrentTime, tvDuration;
    private ImageButton btnPlayPause, btnNext, btnPrevious;
    private MusicService musicService;
    private boolean isBound = false;
    private SeekBar seekBar;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable updateTimeTask = new Runnable() {
        public void run() {
            if (isBound && musicService != null) { // Chỉ kiểm tra binding và service

                // Xử lý khi đang phát
                if (musicService.isPlaying()) {
                    int currentPosition = musicService.getCurrentPosition();
                    int duration = musicService.getDuration();

                    seekBar.setMax(duration);
                    seekBar.setProgress(currentPosition);
                    tvCurrentTime.setText(formatDuration(currentPosition));
                    tvDuration.setText(formatDuration(duration));

                    btnPlayPause.setImageResource(R.drawable.pause);
                } else {
                    // Xử lý khi tạm dừng
                    btnPlayPause.setImageResource(R.drawable.play);
                }
            }

            // Lặp lại sau 100ms (Quan trọng: Đặt ngoài khối if/else bên trong)
            handler.postDelayed(this, 100);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Bắt đầu cập nhật SeekBar ngay sau khi kết nối
            handler.post(updateTimeTask); // Sử dụng Runnable

            // Thiết lập Listener cho các nút điều khiển
            setupControlListeners();

            // Cập nhật trạng thái UI ban đầu
            updateUIInitialState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        // Liên kết Service
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacks(updateTimeTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 1. Dừng nhạc và Service nếu Activity bị hủy
        if (isBound && musicService != null) {
            // Gọi phương thức dừng nhạc và Service mà ta vừa tạo
            musicService.stopAndRelease();

            // 2. Ngắt liên kết Service
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void updateUIInitialState() {
        Intent intent = getIntent();
        // Giả sử bạn truyền Title và Artist từ MainActivity
        String title = intent.getStringExtra("CURRENT_TITLE");
        String artist = intent.getStringExtra("CURRENT_ARTIST");

        if (title != null) tvTitle.setText(title);
        if (artist != null) tvArtist.setText(artist);

        // Cập nhật nút Play/Pause ban đầu
        if (isBound && musicService != null) {
            if (musicService.isPlaying()) {
                btnPlayPause.setImageResource(R.drawable.pause);
            } else {
                btnPlayPause.setImageResource(R.drawable.play);
            }
        }
    }

    private void updateCurrentSongUI() {
        if (isBound && musicService != null) {
            // Cần đảm bảo các hàm này tồn tại trong MusicService
            String title = musicService.getCurrentSongTitle();
            String artist = musicService.getCurrentSongArtist();

            if (title != null) tvTitle.setText(title);
            if (artist != null) tvArtist.setText(artist);

            // Bắt đầu cập nhật thời gian và nút Play/Pause
            handler.post(updateTimeTask);
        }
    }

    private void setupControlListeners() {
        // 1. Logic cho nút Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pausePlayback();
                    btnPlayPause.setImageResource(R.drawable.play);
                } else {
                    musicService.startPlayback();
                    btnPlayPause.setImageResource(R.drawable.pause);
                }
                // Khởi động lại vòng lặp cập nhật khi thay đổi trạng thái
                handler.post(updateTimeTask);
            }
        });

        // 2. Logic cho nút Next/Previous
        btnNext.setOnClickListener(v -> {
            if (isBound && musicService != null) musicService.playNextSong();
        });

        btnPrevious.setOnClickListener(v -> {
            if (isBound && musicService != null) musicService.playPreviousSong();
        });

        // 3. Seek Bar (Đã sửa lỗi biên dịch)
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && musicService != null) {
                    musicService.seekTo(progress);
                    tvCurrentTime.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Ngừng cập nhật tự động
                handler.removeCallbacks(updateTimeTask);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Tiếp tục vòng lặp cập nhật
                handler.post(updateTimeTask);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playback);

        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist); // Cần thêm vào layout và code
        tvCurrentTime = findViewById(R.id.tvCurrentTime); // Đã có trong layout
        tvDuration = findViewById(R.id.tvDuration); // Cần thêm vào layout và code
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);

        btnPlayPause.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pausePlayback();
                } else {
                    musicService.startPlayback();
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.playNextSong();
                updateCurrentSongUI(); // <-- Dùng hàm mới để cập nhật tên bài
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.playPreviousSong();
                updateCurrentSongUI(); // <-- Dùng hàm mới để cập nhật tên bài
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private String formatDuration(int durationMs) {
        long totalSeconds = durationMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}