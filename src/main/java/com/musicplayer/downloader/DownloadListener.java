package com.musicplayer.downloader;

import java.io.File;

/**
 * Callback listener for download progress and completion.
 */
public interface DownloadListener {
    void onDownloadStarted(String songTitle, String url);
    void onDownloadProgress(String songTitle, int percentage, long bytesRead, long totalBytes);
    void onDownloadCompleted(String songTitle, File localFile);
    void onDownloadFailed(String songTitle, String errorMessage);
}
