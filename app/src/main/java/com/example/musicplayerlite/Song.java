package com.example.musicplayerlite;

import android.net.Uri;
import android.provider.MediaStore;

public class Song {
    private final long id;
    private final String path;
    private final String name;
    private final String artist;
    private final String duration;

    public Song(long id,String path,String name,String artist,String duration) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.artist = artist;
        this.duration = duration;
    }

    // getter
    public String getPath() {return this.path;}
    public String getName() {return this.name;}
    public String getArtist() {return this.artist;}
    public String getDuration() {return this.duration;}
    public long getId() { return this.id; }

    // Tạo phương thức lấy Content URI đầy đủ
    public Uri getContentUri() {
        return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(this.id));
    }
}
