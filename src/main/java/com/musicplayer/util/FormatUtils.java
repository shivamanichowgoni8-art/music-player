package com.musicplayer.util;

import java.io.File;

/**
 * Utility functions for formatting durations, file sizes, and strings.
 */
public class FormatUtils {

    private FormatUtils() {}

    /**
     * Formats seconds into MM:SS or HH:MM:SS
     */
    public static String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats bytes into a human-readable size (e.g. 4.2 MB)
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Extracts a clean song title from filename if tags are unavailable
     */
    public static String cleanTitleFromFileName(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        // Remove track numbers like "01 - " or "01. "
        name = name.replaceAll("^[0-9]+[\\s.\\-_]+", "");
        return name.trim();
    }
}
