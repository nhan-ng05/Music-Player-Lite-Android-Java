package com.example.musicplayerlite;

public class Song {
    private final String path;
    private final String name;
    private final String artist;
    private final String duration;

    public Song(String path,String name,String artist,String duration) {
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
}
