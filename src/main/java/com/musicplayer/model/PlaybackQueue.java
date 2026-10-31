package com.musicplayer.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the playback queue, tracks list, shuffle, repeat, and history.
 */
public class PlaybackQueue {

    public enum RepeatMode {
        OFF,
        REPEAT_ALL,
        REPEAT_ONE
    }

    private final List<Track> originalList = new CopyOnWriteArrayList<>();
    private final List<Track> activeQueue = new CopyOnWriteArrayList<>();
    private final Deque<Track> history = new ArrayDeque<>();

    private int currentIndex = -1;
    private boolean shuffle = false;
    private RepeatMode repeatMode = RepeatMode.OFF;

    public PlaybackQueue() {}

    /**
     * Sets the library/playlist tracks into the queue.
     */
    public synchronized void setTracks(List<Track> tracks) {
        originalList.clear();
        activeQueue.clear();
        history.clear();
        currentIndex = -1;

        if (tracks != null) {
            originalList.addAll(tracks);
            activeQueue.addAll(tracks);
            if (shuffle) {
                applyShuffle();
            }
        }
    }

    /**
     * Adds a track to the end of the queue.
     */
    public synchronized void addTrack(Track track) {
        if (track == null) return;
        originalList.add(track);
        activeQueue.add(track);
    }

    /**
     * Inserts a track immediately next in the queue ("Play Next").
     */
    public synchronized void playNext(Track track) {
        if (track == null) return;
        if (currentIndex >= 0 && currentIndex < activeQueue.size() - 1) {
            activeQueue.add(currentIndex + 1, track);
        } else {
            activeQueue.add(track);
        }
        originalList.add(track);
    }

    /**
     * Removes a track from the queue at a specific index.
     */
    public synchronized Track removeTrack(int index) {
        if (index >= 0 && index < activeQueue.size()) {
            Track removed = activeQueue.remove(index);
            originalList.remove(removed);
            if (index < currentIndex) {
                currentIndex--;
            } else if (index == currentIndex) {
                // If removing the currently playing track, adjust index
                if (currentIndex >= activeQueue.size()) {
                    currentIndex = activeQueue.size() - 1;
                }
            }
            return removed;
        }
        return null;
    }

    /**
     * Clears all queued tracks.
     */
    public synchronized void clear() {
        originalList.clear();
        activeQueue.clear();
        history.clear();
        currentIndex = -1;
    }

    /**
     * Plays a specific track by its reference or index.
     */
    public synchronized Track playTrack(Track track) {
        if (track == null) return null;
        int idx = activeQueue.indexOf(track);
        if (idx == -1) {
            // Add and play
            activeQueue.add(track);
            originalList.add(track);
            idx = activeQueue.size() - 1;
        }
        return playIndex(idx);
    }

    public synchronized Track playIndex(int index) {
        if (index >= 0 && index < activeQueue.size()) {
            Track current = getCurrentTrack();
            if (current != null) {
                history.push(current);
            }
            currentIndex = index;
            return activeQueue.get(currentIndex);
        }
        return null;
    }

    /**
     * Advances to the next track according to repeat and queue state.
     */
    public synchronized Track next() {
        if (activeQueue.isEmpty()) return null;

        if (repeatMode == RepeatMode.REPEAT_ONE && currentIndex >= 0) {
            return activeQueue.get(currentIndex);
        }

        if (currentIndex < activeQueue.size() - 1) {
            Track current = getCurrentTrack();
            if (current != null) history.push(current);
            currentIndex++;
            return activeQueue.get(currentIndex);
        } else if (repeatMode == RepeatMode.REPEAT_ALL) {
            Track current = getCurrentTrack();
            if (current != null) history.push(current);
            currentIndex = 0;
            return activeQueue.get(currentIndex);
        }

        return null; // End of queue
    }

    /**
     * Moves to the previous track in history, or restarts track.
     */
    public synchronized Track previous() {
        if (!history.isEmpty()) {
            Track prev = history.pop();
            int idx = activeQueue.indexOf(prev);
            if (idx != -1) {
                currentIndex = idx;
                return prev;
            }
        }

        if (currentIndex > 0) {
            currentIndex--;
            return activeQueue.get(currentIndex);
        } else if (currentIndex == 0 && repeatMode == RepeatMode.REPEAT_ALL) {
            currentIndex = activeQueue.size() - 1;
            return activeQueue.get(currentIndex);
        }

        return getCurrentTrack();
    }

    public synchronized Track getCurrentTrack() {
        if (currentIndex >= 0 && currentIndex < activeQueue.size()) {
            return activeQueue.get(currentIndex);
        }
        return null;
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized List<Track> getActiveQueue() {
        return Collections.unmodifiableList(new ArrayList<>(activeQueue));
    }

    public synchronized boolean isShuffle() {
        return shuffle;
    }

    public synchronized void setShuffle(boolean enable) {
        if (this.shuffle == enable) return;
        this.shuffle = enable;

        Track current = getCurrentTrack();
        activeQueue.clear();

        if (shuffle) {
            List<Track> shuffled = new ArrayList<>(originalList);
            if (current != null) {
                shuffled.remove(current);
                Collections.shuffle(shuffled);
                activeQueue.add(current);
                activeQueue.addAll(shuffled);
                currentIndex = 0;
            } else {
                Collections.shuffle(shuffled);
                activeQueue.addAll(shuffled);
                currentIndex = -1;
            }
        } else {
            activeQueue.addAll(originalList);
            if (current != null) {
                currentIndex = activeQueue.indexOf(current);
            }
        }
    }

    private void applyShuffle() {
        Track current = getCurrentTrack();
        activeQueue.clear();
        List<Track> copy = new ArrayList<>(originalList);
        if (current != null) {
            copy.remove(current);
            Collections.shuffle(copy);
            activeQueue.add(current);
            activeQueue.addAll(copy);
            currentIndex = 0;
        } else {
            Collections.shuffle(copy);
            activeQueue.addAll(copy);
        }
    }

    public synchronized RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public synchronized void cycleRepeatMode() {
        switch (repeatMode) {
            case OFF:
                repeatMode = RepeatMode.REPEAT_ALL;
                break;
            case REPEAT_ALL:
                repeatMode = RepeatMode.REPEAT_ONE;
                break;
            case REPEAT_ONE:
                repeatMode = RepeatMode.OFF;
                break;
        }
    }

    public synchronized void setRepeatMode(RepeatMode mode) {
        this.repeatMode = mode;
    }

    public synchronized int size() {
        return activeQueue.size();
    }
}
