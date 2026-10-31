package com.musicplayer.ui.components;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Generates beautiful Apple Music inspired gradient album artworks for tracks without covers.
 */
public class ArtworkGenerator {

    private static final Color[][] GRADIENT_PALETTES = {
            {new Color(0xFF, 0x2D, 0x55), new Color(0x58, 0x56, 0xD6)}, // Pink to Purple
            {new Color(0xFF, 0x37, 0x5F), new Color(0xFF, 0x9F, 0x0A)}, // Neon Pink to Orange
            {new Color(0x30, 0xD1, 0x58), new Color(0x0A, 0x84, 0xFF)}, // Emerald to Cyan
            {new Color(0xBF, 0x5A, 0xF2), new Color(0xFF, 0x2D, 0x55)}, // Violet to Electric Red
            {new Color(0x64, 0xD2, 0xFF), new Color(0x00, 0x40, 0xDD)}, // Sky Blue to Deep Indigo
            {new Color(0xFF, 0x45, 0x3A), new Color(0xFF, 0xD6, 0x0A)}  // Coral to Warm Gold
    };

    private ArtworkGenerator() {}

    public static BufferedImage generateCover(String title, String artist, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int hash = Math.abs((title + artist).hashCode());
        Color[] palette = GRADIENT_PALETTES[hash % GRADIENT_PALETTES.length];

        // Diagonal gradient
        GradientPaint gp = new GradientPaint(0, 0, palette[0], size, size, palette[1]);
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Float(0, 0, size, size, size * 0.18f, size * 0.18f));

        // Subtle concentric vinyl groove decoration
        g2.setColor(new Color(255, 255, 255, 25));
        g2.setStroke(new BasicStroke(size * 0.02f));
        int center = size / 2;
        int radius1 = (int) (size * 0.35);
        int radius2 = (int) (size * 0.22);
        g2.drawOval(center - radius1, center - radius1, radius1 * 2, radius1 * 2);
        g2.drawOval(center - radius2, center - radius2, radius2 * 2, radius2 * 2);

        // Music note glyph in center
        g2.setColor(new Color(255, 255, 255, 210));
        int noteSize = (int) (size * 0.38);
        Font font = new Font("SansSerif", Font.BOLD, noteSize);
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();
        String glyph = "\u266B"; // Musical double note
        int glyphWidth = fm.stringWidth(glyph);
        int glyphAscent = fm.getAscent();

        int x = (size - glyphWidth) / 2;
        int y = (size + glyphAscent) / 2 - (int) (size * 0.05);
        g2.drawString(glyph, x, y);

        g2.dispose();
        return image;
    }
}
