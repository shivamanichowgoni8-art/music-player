package com.musicplayer.player;

import com.musicplayer.model.Track;

import javax.sound.sampled.*;
import javax.swing.SwingUtilities;
import java.io.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-blocking audio player executing audio decoding and playback on dedicated background threads.
 * Prevents UI freezes on the Event Dispatch Thread (EDT).
 */
public class AudioPlayer {

    private final List<AudioPlayerListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService progressScheduler = Executors.newSingleThreadScheduledExecutor();
    private ExecutorService playbackExecutor = Executors.newSingleThreadExecutor();

    private Track currentTrack;
    private volatile PlaybackState state = PlaybackState.STOPPED;

    // Playback state variables
    private volatile boolean isPaused = false;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile float volume = 0.85f; // 0.0 to 1.0

    // Audio stream handles
    private SourceDataLine sourceDataLine;
    private AudioInputStream audioInputStream;
    private FloatControl volumeControl;

    private volatile long currentMicroseconds = 0;
    private volatile long totalMicroseconds = 0;
    private volatile long pausedMicroseconds = 0;

    public AudioPlayer() {
        startProgressMonitor();
    }

    public void addListener(AudioPlayerListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(AudioPlayerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Plays the specified track on a background thread.
     */
    public synchronized void play(Track track) {
        if (track == null || track.getFile() == null) {
            notifyError("Cannot play track: File is null or not found.", null);
            return;
        }

        File file = track.getFile();
        if (!file.exists() || !file.canRead()) {
            notifyError("File does not exist or cannot be read: " + file.getAbsolutePath(), null);
            return;
        }

        // Stop any currently active playback
        stopPlaybackInternal();

        this.currentTrack = track;
        this.isPaused = false;
        this.stopRequested.set(false);
        this.currentMicroseconds = 0;
        this.pausedMicroseconds = 0;
        this.totalMicroseconds = track.getDurationSeconds() * 1_000_000L;

        setState(PlaybackState.BUFFERING);
        notifyTrackStarted(track);

        playbackExecutor.submit(() -> {
            try {
                playAudioFile(file);
            } catch (Throwable t) {
                if (!stopRequested.get()) {
                    setState(PlaybackState.ERROR);
                    notifyError("Failed to play audio file: " + t.getMessage(), t);
                }
            }
        });
    }

    /**
     * Internal method handling audio reading and feeding data line.
     */
    private void playAudioFile(File file) throws Exception {
        AudioInputStream rawStream;
        try {
            rawStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            // Attempt to check if MP3 or try JLayer reflection if on classpath
            if (tryPlayWithJLayer(file)) {
                return;
            }
            throw new UnsupportedAudioFileException("Audio format not supported natively by Java Sound: " + file.getName()
                    + " (Supported natively: WAV, AIFF, AU. Install MP3 SPI or JLayer for MP3).");
        }

        AudioFormat baseFormat = rawStream.getFormat();
        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
        );

        this.audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, rawStream);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Data line for decoded format is not supported: " + decodedFormat);
        }

        this.sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        this.sourceDataLine.open(decodedFormat);
        this.sourceDataLine.start();

        // Initialize volume control
        if (sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            this.volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            applyVolume(volume);
        }

        setState(PlaybackState.PLAYING);

        byte[] buffer = new byte[8192];
        int bytesRead;

        long bytesPerSecond = (long) (decodedFormat.getSampleRate() * decodedFormat.getFrameSize());
        long totalBytesStreamed = 0;

        while (!stopRequested.get() && (bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
            // Handle pause state loop
            while (isPaused && !stopRequested.get()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (stopRequested.get()) break;

            sourceDataLine.write(buffer, 0, bytesRead);
            totalBytesStreamed += bytesRead;

            if (bytesPerSecond > 0) {
                currentMicroseconds = (totalBytesStreamed * 1_000_000L) / bytesPerSecond;
            }
        }

        if (sourceDataLine != null && !stopRequested.get()) {
            sourceDataLine.drain();
            setState(PlaybackState.STOPPED);
            notifyTrackFinished(currentTrack);
        }

        closeStreams();
    }

    /**
     * Tries playing via JLayer if javazoom.jl.player.Player is on the classpath.
     */
    private boolean tryPlayWithJLayer(File file) {
        try {
            Class<?> playerClass = Class.forName("javazoom.jl.player.Player");
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            Object player = playerClass.getConstructor(InputStream.class).newInstance(is);

            setState(PlaybackState.PLAYING);

            // JLayer playback loop
            java.lang.reflect.Method playMethod = playerClass.getMethod("play", int.class);
            java.lang.reflect.Method isCompleteMethod = playerClass.getMethod("isComplete");
            java.lang.reflect.Method closeMethod = playerClass.getMethod("close");

            long startMillis = System.currentTimeMillis();

            while (!stopRequested.get() && !((Boolean) isCompleteMethod.invoke(player))) {
                while (isPaused && !stopRequested.get()) {
                    Thread.sleep(50);
                }
                if (stopRequested.get()) break;

                // Play in 100-frame chunks
                boolean more = (Boolean) playMethod.invoke(player, 100);
                currentMicroseconds = (System.currentTimeMillis() - startMillis) * 1000L;
                if (!more) break;
            }

            closeMethod.invoke(player);
            is.close();

            if (!stopRequested.get()) {
                setState(PlaybackState.STOPPED);
                notifyTrackFinished(currentTrack);
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void pause() {
        if (state == PlaybackState.PLAYING) {
            isPaused = true;
            if (sourceDataLine != null) {
                sourceDataLine.stop();
            }
            pausedMicroseconds = currentMicroseconds;
            setState(PlaybackState.PAUSED);
        }
    }

    public synchronized void resume() {
        if (state == PlaybackState.PAUSED) {
            isPaused = false;
            if (sourceDataLine != null) {
                sourceDataLine.start();
            }
            setState(PlaybackState.PLAYING);
        }
    }

    public synchronized void togglePlayPause() {
        if (state == PlaybackState.PLAYING) {
            pause();
        } else if (state == PlaybackState.PAUSED) {
            resume();
        }
    }

    public synchronized void stop() {
        stopPlaybackInternal();
        setState(PlaybackState.STOPPED);
        currentMicroseconds = 0;
    }

    private synchronized void stopPlaybackInternal() {
        stopRequested.set(true);
        isPaused = false;
        closeStreams();
    }

    private synchronized void closeStreams() {
        if (sourceDataLine != null) {
            try {
                sourceDataLine.stop();
                sourceDataLine.close();
            } catch (Exception ignored) {}
            sourceDataLine = null;
        }
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (Exception ignored) {}
            audioInputStream = null;
        }
    }

    /**
     * Sets volume between 0.0 (silent) and 1.0 (max).
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        applyVolume(this.volume);
    }

    public float getVolume() {
        return volume;
    }

    private void applyVolume(float vol) {
        if (volumeControl != null) {
            try {
                if (vol <= 0.0001f) {
                    volumeControl.setValue(volumeControl.getMinimum());
                } else {
                    // Logarithmic decibel calculation for natural hearing curve
                    float min = volumeControl.getMinimum();
                    float max = volumeControl.getMaximum();
                    float dB = (float) (Math.log10(vol) * 20.0);
                    dB = Math.max(min, Math.min(max, dB));
                    volumeControl.setValue(dB);
                }
            } catch (Exception ignored) {}
        }
    }

    public PlaybackState getState() {
        return state;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public long getCurrentSeconds() {
        return currentMicroseconds / 1_000_000L;
    }

    public long getTotalSeconds() {
        if (currentTrack != null && currentTrack.getDurationSeconds() > 0) {
            return currentTrack.getDurationSeconds();
        }
        return totalMicroseconds / 1_000_000L;
    }

    private void setState(PlaybackState newState) {
        this.state = newState;
        SwingUtilities.invokeLater(() -> {
            for (AudioPlayerListener l : listeners) {
                l.onStateChanged(newState);
            }
        });
    }

    private void notifyTrackStarted(Track track) {
        SwingUtilities.invokeLater(() -> {
            for (AudioPlayerListener l : listeners) {
                l.onTrackStarted(track);
            }
        });
    }

    private void notifyTrackFinished(Track track) {
        SwingUtilities.invokeLater(() -> {
            for (AudioPlayerListener l : listeners) {
                l.onTrackFinished(track);
            }
        });
    }

    private void notifyError(String msg, Throwable t) {
        SwingUtilities.invokeLater(() -> {
            for (AudioPlayerListener l : listeners) {
                l.onError(msg, t);
            }
        });
    }

    private void startProgressMonitor() {
        progressScheduler.scheduleAtFixedRate(() -> {
            if (state == PlaybackState.PLAYING && currentTrack != null) {
                long cur = getCurrentSeconds();
                long tot = getTotalSeconds();
                SwingUtilities.invokeLater(() -> {
                    for (AudioPlayerListener l : listeners) {
                        l.onProgressUpdate(cur, tot);
                    }
                });
            }
        }, 0, 250, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        stopPlaybackInternal();
        progressScheduler.shutdownNow();
        playbackExecutor.shutdownNow();
    }
}
