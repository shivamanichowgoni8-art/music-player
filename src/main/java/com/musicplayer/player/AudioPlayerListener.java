package com.musicplayer.player;

import com.musicplayer.model.Track;

/**
 * Observer interface for decoupled audio playback events.
 * Listeners receive notifications on the Swing EDT to update UI smoothly.
 */
public interface AudioPlayerListener {
    /**
     * Called when a track begins playback.
     */
    void onTrackStarted(Track track);

    /**
     * Called periodically with playback progress.
     */
    void onProgressUpdate(long currentSeconds, long totalSeconds);

    /**
     * Called when playback state changes (PLAYING, PAUSED, STOPPED, etc.).
     */
    void onStateChanged(PlaybackState newState);

    /**
     * Called when the current track finishes naturally.
     */
    void onTrackFinished(Track track);

    /**
     * Called when an error occurs during playback.
     */
    void onError(String message, Throwable exception);
}
