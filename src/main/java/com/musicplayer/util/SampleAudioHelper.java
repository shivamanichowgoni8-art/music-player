package com.musicplayer.util;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Generates harmonic musical audio waveforms (.wav) for offline demonstration and testing.
 */
public class SampleAudioHelper {

    private SampleAudioHelper() {}

    /**
     * Synthesizes a pleasant ambient / chill melodic chord progression into a standard WAV file.
     */
    public static void createSynthesizedTrack(File outputFile, String songTitle, int durationSeconds) throws Exception {
        float sampleRate = 44100;
        int channels = 2;
        int sampleSizeInBits = 16;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, true, false);

        int totalFrames = (int) (sampleRate * durationSeconds);
        byte[] buffer = new byte[totalFrames * channels * 2];

        // Musical frequencies: C major / A minor pentatonic chords (A3, C4, E4, G4, D4)
        double[] chordNotes = {220.0, 261.63, 329.63, 392.0, 293.66, 349.23, 440.0, 523.25};
        double hashOffset = Math.abs(songTitle.hashCode()) % chordNotes.length;

        int byteIndex = 0;
        for (int frame = 0; frame < totalFrames; frame++) {
            double time = frame / sampleRate;

            // Arpeggio note selection based on beat
            int beat = (int) (time * 2.0); // 120 bpm eighth notes
            double baseFreq = chordNotes[(int) ((beat + hashOffset) % chordNotes.length)];
            double chordHarmonic = chordNotes[(int) ((beat + hashOffset + 2) % chordNotes.length)];

            // Envelope to give notes a soft pluck / bell attack and decay
            double noteTime = (time * 2.0) % 1.0;
            double envelope = Math.exp(-3.0 * noteTime);

            // Synthesize primary harmonic and overtone
            double wave = Math.sin(2.0 * Math.PI * baseFreq * time) * 0.6
                        + Math.sin(2.0 * Math.PI * chordHarmonic * time) * 0.3
                        + Math.sin(2.0 * Math.PI * (baseFreq * 0.5) * time) * 0.2; // warm sub-bass

            wave *= envelope * 0.45;

            // Fade in and fade out
            if (time < 1.0) {
                wave *= time;
            } else if (time > durationSeconds - 1.5) {
                wave *= Math.max(0.0, (durationSeconds - time) / 1.5);
            }

            short sampleVal = (short) (wave * 32767.0);

            // Left channel (little-endian)
            buffer[byteIndex++] = (byte) (sampleVal & 0xFF);
            buffer[byteIndex++] = (byte) ((sampleVal >> 8) & 0xFF);

            // Right channel with subtle stereo chorus phase
            short rightVal = (short) (wave * 32767.0 * 0.95);
            buffer[byteIndex++] = (byte) (rightVal & 0xFF);
            buffer[byteIndex++] = (byte) ((rightVal >> 8) & 0xFF);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        AudioInputStream ais = new AudioInputStream(bais, format, totalFrames);

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
    }
}
