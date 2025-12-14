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
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SongAdapter(List<Song> songList,OnSongClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle;
        public TextView tvArtist;
        public TextView tvDuration;

        public SongViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            tvDuration = itemView.findViewById(R.id.tv_duration);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_song.xml
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        // Lấy đối tượng Song tại vị trí hiện tại
        Song currentSong = songList.get(position);

        holder.tvTitle.setText(currentSong.getName());

        holder.tvArtist.setText(currentSong.getArtist());
        holder.tvDuration.setText(currentSong.getDuration());

        holder.itemView.setOnClickListener(v -> {
            listener.onSongClick(songList.get(position));
        });
    }

    // 3. getItemCount: Trả về tổng số mục trong danh sách
    @Override
    public int getItemCount() {
        return songList.size();
    }
}