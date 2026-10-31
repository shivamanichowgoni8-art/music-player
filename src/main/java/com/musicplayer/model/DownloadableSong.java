package com.musicplayer.model;

/**
 * Represents a song available for online download.
 */
public class DownloadableSong {
    private final String id;
    private final String title;
    private final String artist;
    private final String album;
    private final String duration;
    private final String downloadUrl;
    private final String genre;
    private final String format;

    public DownloadableSong(String id, String title, String artist, String album, String duration, String downloadUrl, String genre, String format) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.downloadUrl = downloadUrl;
        this.genre = genre;
        this.format = format;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getDuration() { return duration; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getGenre() { return genre; }
    public String getFormat() { return format; }

    @Override
    public String toString() {
        return title + " - " + artist + " (" + duration + ")";
    }
}
