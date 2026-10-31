package com.musicplayer.model;

import com.musicplayer.util.FormatUtils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an audio track with metadata (title, artist, album, duration, artwork, file path).
 */
public class Track implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final File file;
    private String title;
    private String artist;
    private String album;
    private long durationSeconds;
    private String format;
    private transient BufferedImage albumArt;
    private boolean isFavorite;

    public Track(File file) {
        this.file = Objects.requireNonNull(file, "Audio file cannot be null");
        this.id = file.getAbsolutePath();
        this.format = getFileExtension(file).toUpperCase();
        this.isFavorite = false;
        extractMetadata();
    }

    public Track(String id, String title, String artist, String album, long durationSeconds, File file) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationSeconds = durationSeconds;
        this.file = file;
        this.format = file != null ? getFileExtension(file).toUpperCase() : "MP3";
        this.isFavorite = false;
    }

    /**
     * Attempts to read metadata from the file, falling back cleanly if not available.
     */
    private void extractMetadata() {
        this.title = FormatUtils.cleanTitleFromFileName(file);
        this.artist = "Unknown Artist";
        this.album = "Unknown Album";
        this.durationSeconds = 0;

        try {
            // Attempt standard Java Sound SPI inspection for WAV/AIFF
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            Map<String, Object> properties = fileFormat.properties();
            if (properties != null) {
                if (properties.containsKey("title")) {
                    this.title = properties.get("title").toString();
                }
                if (properties.containsKey("author")) {
                    this.artist = properties.get("author").toString();
                }
                if (properties.containsKey("album")) {
                    this.album = properties.get("album").toString();
                }
                if (properties.containsKey("duration")) {
                    long microSeconds = ((Number) properties.get("duration")).longValue();
                    this.durationSeconds = microSeconds / 1_000_000;
                }
            }

            if (this.durationSeconds <= 0 && fileFormat.getFrameLength() > 0 && fileFormat.getFormat().getFrameRate() > 0) {
                this.durationSeconds = (long) (fileFormat.getFrameLength() / fileFormat.getFormat().getFrameRate());
            }
        } catch (Exception ignored) {
            // File may be MP3 or metadata unavailable; fallback estimates will be used
        }

        // Estimate duration if unknown (assuming ~128-192 kbps average for MP3/compressed audio)
        if (this.durationSeconds <= 0 && file.length() > 0) {
            long estimatedSeconds = file.length() / (160 * 1024 / 8); // approximate ~160kbps
            if (estimatedSeconds > 0 && estimatedSeconds < 3600) {
                this.durationSeconds = estimatedSeconds;
            } else {
                this.durationSeconds = 180; // default standard 3 min
            }
        }
    }

    private String getFileExtension(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1).toLowerCase() : "";
    }

    // Getters and Setters
    public String getId() { return id; }
    public File getFile() { return file; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getFormattedDuration() { return FormatUtils.formatDuration(durationSeconds); }
    public String getFormat() { return format; }
    public BufferedImage getAlbumArt() { return albumArt; }
    public void setAlbumArt(BufferedImage albumArt) { this.albumArt = albumArt; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track)) return false;
        Track track = (Track) o;
        return Objects.equals(id, track.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title + " - " + artist + " (" + getFormattedDuration() + ")";
    }
}
