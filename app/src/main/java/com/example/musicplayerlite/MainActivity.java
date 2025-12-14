package com.example.musicplayerlite;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.Manifest;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener{
    private static final int MY_PERMISSION_REQUEST = 100;
    final String musicFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
    private MusicService musicService;
    private boolean isBound = false;
    private ArrayList<Song> songsList = new ArrayList<Song>();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            // Service đã sẵn sàng để được sử dụng
            Log.d("MainActivity", "MusicService đã kết nối thành công.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
            Log.d("MainActivity", "MusicService đã bị ngắt kết nối.");
        }
    };

    // Logic kiểm tra quyền trong MainActivity.java
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU = API 33
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO}, MY_PERMISSION_REQUEST);
            } else {
                loadMusicFiles();
            }
        } else {
            // Logic cho Android < 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            } else {
                loadMusicFiles();
            }
        }
    }

    @Override
    public void onSongClick(Song song) {
        // tìm vị trí của bài hát được click
        int songIndex = songsList.indexOf(song);

        if (songIndex == -1) {
            // Trường hợp không tìm thấy bài hát trong danh sách (Lỗi dữ liệu)
            Log.e("MainActivity", "Lỗi: Không tìm thấy bài hát trong danh sách đã tải.");
            return;
        }

        Intent serviceIntent = new Intent(this, MusicService.class);

        serviceIntent.putParcelableArrayListExtra("SONGS_LIST", (ArrayList<? extends Parcelable>) songsList);
        serviceIntent.putExtra("SONG_INDEX", songIndex);

        // Gửi thông tin để PlaybackActivity hiển thị ban đầu
        serviceIntent.putExtra("CURRENT_TITLE", song.getName());
        serviceIntent.putExtra("CURRENT_ARTIST", song.getArtist());

        serviceIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Sử dụng startForegroundService() mới
        ContextCompat.startForegroundService(this, serviceIntent);

        // 3. KHỞI CHẠY PLAYBACK ACTIVITY MỚI
        Intent playbackIntent = new Intent(this, PlaybackActivity.class);
        startActivity(playbackIntent);
    }

    // Phương thức load nhạc sử dụng MediaStore
    private void loadMusicFiles() {
        songsList.clear();

        // 1. URI chuẩn để truy vấn các file Audio
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // 2. Các cột dữ liệu bạn muốn truy vấn
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,       // Đường dẫn file (Dùng để lọc thư mục)
                MediaStore.Audio.Media.TITLE,      // Tên bài hát (ID3 tag)
                MediaStore.Audio.Media.ARTIST,     // Tên ca sĩ (ID3 tag)
                MediaStore.Audio.Media.DURATION    // Thời lượng (milliseconds)
        };

        // 3. Điều kiện lọc: Chỉ lấy các file được đánh dấu là nhạc
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC"; // Sắp xếp theo tên bài hát

        // Thực hiện truy vấn
        Cursor cursor = getContentResolver().query(uri, projection, selection, null, sortOrder);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String path = cursor.getString(1);
                String title = cursor.getString(2);
                String artist = cursor.getString(3);
                long durationMs = cursor.getLong(4);

                // Lọc để chỉ lấy các file nằm trong thư mục /Music/
                // Điều kiện này giúp thu hẹp phạm vi, mặc dù MediaStore đã làm rất tốt.
                // Lưu ý: path.contains("/Music/") có thể không chính xác 100% trên mọi thiết bị.
                if (path != null && path.startsWith(musicFolderPath)) {

                    String duration = formatDuration(durationMs);

                    // Xử lý trường hợp metadata bị thiếu
                    if (title == null || title.isEmpty()) {
                        title = new File(path).getName().replace(".mp3", "");
                    }
                    if (artist == null || artist.isEmpty() || artist.equals("<unknown>")) {
                        artist = "<Unknown>";
                    }

                    Song song = new Song(id, path, title, artist, duration);
                    songsList.add(song);
                }
            }
            cursor.close();

            // <<< BƯỚC 4: KÍCH HOẠT HIỂN THỊ RECYCLERVIEW >>>
            setupRecyclerView(songsList);
        } else {
            Toast.makeText(this, "Không tìm thấy file nhạc nào.", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm hỗ trợ chuyển đổi thời gian (đã khắc phục cảnh báo Locale)
    private String formatDuration(long durationMs) {
        long minutes = (durationMs / 1000) / 60;
        long seconds = (durationMs / 1000) % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Người dùng chấp nhận quyền
                loadMusicFiles();
            } else {
                // Người dùng từ chối quyền
                Toast.makeText(this, "Bạn cần cấp quyền truy cập bộ nhớ để tải nhạc.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupRecyclerView(ArrayList<Song> songsList) {
        // 1. Tìm RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        // 2. Thiết lập LayoutManager (Cuộn dọc)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. Thiết lập Adapter
        SongAdapter adapter = new SongAdapter(songsList,this);
        recyclerView.setAdapter(adapter);

        Toast.makeText(this, "Đã tải " + songsList.size() + " bài hát.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Toolbar appbar = findViewById(R.id.toolbar);
        setSupportActionBar(appbar);
        appbar.setBackgroundColor(Color.BLUE);
        appbar.setLogo(R.drawable.music);
        appbar.setTitle("Music Player Lite");
        appbar.setTitleTextColor(Color.WHITE);
        appbar.setSubtitleTextColor(Color.WHITE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // <<< BƯỚC 5: GỌI HÀM KIỂM TRA QUYỀN VÀ TẢI NHẠC >>>
        checkPermissions();
    }

    // Trong MainActivity.java
    @Override
    protected void onStart() {
        super.onStart();
        // Bắt đầu liên kết Service khi Activity hiển thị
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Ngắt liên kết Service khi Activity không hiển thị
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}