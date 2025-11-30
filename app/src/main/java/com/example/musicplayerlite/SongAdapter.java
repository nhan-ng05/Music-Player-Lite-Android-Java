// SongAdapter.java
package com.example.musicplayerlite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    // final vì không cần thêm xoá bài
    private final List<Song> songList;

    // Constructor: Nhận danh sách dữ liệu để hiển thị
    public SongAdapter(List<Song> songList) {
        this.songList = songList;
    }

    // Lớp ViewHolder: Giữ tham chiếu đến các View con (TextView, ImageView...) trong một mục
    public static class SongViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle;
        public TextView tvArtist;
        public TextView tvDuration;

        public SongViewHolder(View itemView) {
            super(itemView);
            // Ánh xạ các View từ item_song.xml
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            tvDuration = itemView.findViewById(R.id.tv_duration);

            // Thiết lập sự kiện click (Tùy chọn)
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Xử lý khi người dùng click vào một bài hát
                    // Ví dụ: Bắt đầu chơi nhạc
                }
            });
        }
    }

    // 1. onCreateViewHolder: Tạo một ViewHolder mới (khi RecyclerView cần một View mới)
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_song.xml
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(v);
    }

    // 2. onBindViewHolder: Gắn dữ liệu vào các View của ViewHolder
    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        // Lấy đối tượng Song tại vị trí hiện tại
        Song currentSong = songList.get(position);

        // Đặt dữ liệu vào các TextView
        holder.tvTitle.setText(currentSong.getName());

        // Sử dụng định dạng: Tên ca sĩ - Thời lượng (ví dụ)
        // hoặc đặt chúng vào các TextView riêng biệt như trong item_song.xml
        holder.tvArtist.setText(currentSong.getArtist());
        holder.tvDuration.setText(currentSong.getDuration());
    }

    // 3. getItemCount: Trả về tổng số mục trong danh sách
    @Override
    public int getItemCount() {
        return songList.size();
    }
}