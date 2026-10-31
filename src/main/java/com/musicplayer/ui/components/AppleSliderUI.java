package com.musicplayer.ui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom Apple Music styled JSlider UI with vibrant pink accent (#FF2D55) and sleek rounded thumb.
 */
public class AppleSliderUI extends BasicSliderUI {

    private static final Color ACCENT_COLOR = new Color(0xFF, 0x2D, 0x55);
    private static final Color TRACK_BACKGROUND = new Color(0x3A, 0x3A, 0x3C);
    private static final Color THUMB_COLOR = Color.WHITE;

    private boolean isHovered = false;

    public AppleSliderUI(JSlider b) {
        super(b);
        b.setOpaque(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                b.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                b.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Click anywhere on track to seek directly to that spot
                if (slider.getOrientation() == JSlider.HORIZONTAL) {
                    int pos = e.getX();
                    double percent = (double) pos / (double) slider.getWidth();
                    int val = (int) Math.round(slider.getMinimum() + percent * (slider.getMaximum() - slider.getMinimum()));
                    slider.setValue(val);
                }
            }
        });
    }

    @Override
    protected Dimension getThumbSize() {
        int diameter = isHovered || isDragging() ? 12 : 8;
        return new Dimension(diameter, diameter);
    }

    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle trackBounds = trackRect;
        int trackHeight = 4;
        int cy = trackBounds.y + (trackBounds.height - trackHeight) / 2;

        // Background track
        g2.setColor(TRACK_BACKGROUND);
        g2.fill(new RoundRectangle2D.Float(trackBounds.x, cy, trackBounds.width, trackHeight, trackHeight, trackHeight));

        // Filled progress track
        int thumbMidX = thumbRect.x + thumbRect.width / 2;
        int fillWidth = Math.max(0, thumbMidX - trackBounds.x);
        g2.setColor(ACCENT_COLOR);
        g2.fill(new RoundRectangle2D.Float(trackBounds.x, cy, fillWidth, trackHeight, trackHeight, trackHeight));

        g2.dispose();
    }

    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int diameter = isHovered || isDragging() ? 12 : 8;
        int cx = thumbRect.x + (thumbRect.width - diameter) / 2;
        int cy = thumbRect.y + (thumbRect.height - diameter) / 2;

        // Subtle drop shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(cx, cy + 1, diameter, diameter);

        // Thumb circle
        g2.setColor(THUMB_COLOR);
        g2.fillOval(cx, cy, diameter, diameter);

        g2.dispose();
    }

    @Override
    public void paintFocus(Graphics g) {
        // No focus rectangle for clean Apple design
    }
}
