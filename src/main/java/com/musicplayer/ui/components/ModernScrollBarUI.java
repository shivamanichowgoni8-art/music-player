package com.musicplayer.ui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Minimalist translucent dark scrollbar matching Apple Music design.
 */
public class ModernScrollBarUI extends BasicScrollBarUI {

    private static final Color THUMB_COLOR = new Color(255, 255, 255, 60);
    private static final Color THUMB_HOVER = new Color(255, 255, 255, 110);
    private static final Color TRACK_COLOR = new Color(0, 0, 0, 0); // Transparent

    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = THUMB_COLOR;
        this.trackColor = TRACK_COLOR;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(0, 0));
        btn.setMinimumSize(new Dimension(0, 0));
        btn.setMaximumSize(new Dimension(0, 0));
        return btn;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Invisible transparent track
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(isThumbRollover() ? THUMB_HOVER : THUMB_COLOR);
        int arc = 6;
        int padding = 3;
        g2.fill(new RoundRectangle2D.Float(
                thumbBounds.x + padding,
                thumbBounds.y + padding,
                Math.max(4, thumbBounds.width - padding * 2),
                Math.max(12, thumbBounds.height - padding * 2),
                arc, arc
        ));

        g2.dispose();
    }
}
