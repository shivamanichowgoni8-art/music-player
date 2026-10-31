package com.musicplayer;

import com.musicplayer.ui.MusicPlayerGUI;

import javax.swing.*;

/**
 * Application Entry Point.
 */
public class Main {

    public static void main(String[] args) {
        // Attempt FlatLaf Dark theme if available on classpath
        try {
            Class<?> flatDarkLaf = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            UIManager.setLookAndFeel((LookAndFeel) flatDarkLaf.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            // Fall back to system Look and Feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Launch UI on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MusicPlayerGUI gui = new MusicPlayerGUI();
            gui.setVisible(true);
        });
    }
}
