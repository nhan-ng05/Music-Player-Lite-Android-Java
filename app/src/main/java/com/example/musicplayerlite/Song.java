package com.example.musicplayerlite;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

public class Song implements Parcelable {
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

    protected Song(Parcel in) {
        id = in.readLong();
        path = in.readString();
        name = in.readString();
        artist = in.readString();
        duration = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // 1. Ghi id (long)
        dest.writeLong(id);

        // 2. Ghi path (String)
        dest.writeString(path);

        // 3. Ghi name (String)
        dest.writeString(name);

        // 4. Ghi artist (String)
        dest.writeString(artist);

        // 5. Ghi duration (String)
        dest.writeString(duration);

        // Lưu ý: Thứ tự này phải KHỚP CHẶT CHẼ với thứ tự đọc trong Constructor Parcelable.
    }
}
