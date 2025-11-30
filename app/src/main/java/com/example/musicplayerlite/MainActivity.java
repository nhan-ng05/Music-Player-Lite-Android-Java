package com.example.musicplayerlite;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.Manifest;
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

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSION_REQUEST = 100;

    private void checkPermissions() {
        // Chỉ cần xin quyền READ_EXTERNAL_STORAGE cho Android 12 trở xuống.
        // Android 13+ không yêu cầu quyền này cho MediaStore nữa (chỉ cần READ_MEDIA_AUDIO)
        // nhưng khai báo READ_EXTERNAL_STORAGE vẫn hoạt động với tính tương thích ngược.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
        } else {
            loadMusicFiles();
        }
    }

    // Phương thức load nhạc sử dụng MediaStore
    private void loadMusicFiles() {
        ArrayList<Song> songsList = new ArrayList<>();

        // 1. URI chuẩn để truy vấn các file Audio
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // 2. Các cột dữ liệu bạn muốn truy vấn
        String[] projection = {
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
                String path = cursor.getString(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                long durationMs = cursor.getLong(3);

                // Lọc để chỉ lấy các file nằm trong thư mục /Music/
                // Điều kiện này giúp thu hẹp phạm vi, mặc dù MediaStore đã làm rất tốt.
                // Lưu ý: path.contains("/Music/") có thể không chính xác 100% trên mọi thiết bị.
                if (path != null && path.contains(Environment.DIRECTORY_MUSIC)) {

                    String duration = formatDuration(durationMs);

                    // Xử lý trường hợp metadata bị thiếu
                    if (title == null || title.isEmpty()) {
                        title = new File(path).getName().replace(".mp3", "");
                    }
                    if (artist == null || artist.isEmpty() || artist.equals("<unknown>")) {
                        artist = "<Unknown>";
                    }

                    Song song = new Song(path, title, artist, duration);
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
        SongAdapter adapter = new SongAdapter(songsList);
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
        appbar.setTitle("Music Player Lite");
        appbar.setTitleTextColor(Color.WHITE); // Đổi màu chữ Toolbar cho dễ nhìn trên nền tối (tùy chọn)
        appbar.setLogo(R.drawable.music); // Thay R.drawable.music bằng ID icon của bạn
        appbar.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_700)); // Thay màu Toolbar
        appbar.setSubtitleTextColor(Color.WHITE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // <<< BƯỚC 5: GỌI HÀM KIỂM TRA QUYỀN VÀ TẢI NHẠC >>>
        checkPermissions();
    }
}