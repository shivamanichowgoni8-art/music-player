package com.musicplayer.ui;

import com.musicplayer.model.PlaybackQueue;
import com.musicplayer.model.Track;
import com.musicplayer.player.AudioPlayer;
import com.musicplayer.player.AudioPlayerListener;
import com.musicplayer.player.PlaybackState;
import com.musicplayer.ui.components.AppleSliderUI;
import com.musicplayer.ui.components.ArtworkGenerator;
import com.musicplayer.util.FormatUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Apple Music persistent bottom playback bar.
 * Left: Track info & artwork.
 * Center: Transport controls, seek bar, time labels.
 * Right: Volume slider, mute button, queue toggle.
 */
public class PlaybackBar extends JPanel implements AudioPlayerListener {

    private final AudioPlayer player;
    private final PlaybackQueue queue;
    private final Runnable onQueueToggle;
    private final Runnable onTrackChangedCallback;

    // UI elements
    private JLabel lblArtwork;
    private JLabel lblTitle;
    private JLabel lblArtist;
    private JButton btnFavorite;

    private JButton btnShuffle;
    private JButton btnPrev;
    private JButton btnPlayPause;
    private JButton btnNext;
    private JButton btnRepeat;

    private JSlider seekSlider;
    private JLabel lblCurrentTime;
    private JLabel lblTotalTime;
    private boolean isUserSeeking = false;

    private JButton btnMute;
    private JSlider volumeSlider;
    private float previousVolume = 0.85f;

    private static final Color BG_COLOR = new Color(0x24, 0x24, 0x26);
    private static final Color BORDER_COLOR = new Color(0x38, 0x38, 0x3A);
    private static final Color ACCENT_COLOR = new Color(0xFF, 0x2D, 0x55);
    private static final Color TEXT_MUTED = new Color(0x8E, 0x8E, 0x93);

    public PlaybackBar(AudioPlayer player, PlaybackQueue queue, Runnable onQueueToggle, Runnable onTrackChangedCallback) {
        this.player = player;
        this.queue = queue;
        this.onQueueToggle = onQueueToggle;
        this.onTrackChangedCallback = onTrackChangedCallback;

        this.player.addListener(this);

        setPreferredSize(new Dimension(1000, 84));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(15, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(8, 20, 8, 20)
        ));

        initLeftPanel();
        initCenterPanel();
        initRightPanel();

        updateTrackInfo(null);
    }

    private void initLeftPanel() {
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(280, 68));

        lblArtwork = new JLabel();
        lblArtwork.setPreferredSize(new Dimension(48, 48));
        lblArtwork.setIcon(new ImageIcon(ArtworkGenerator.generateCover("Music", "", 48)));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        textPanel.setPreferredSize(new Dimension(170, 48));

        lblTitle = new JLabel("Not Playing");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitle.setForeground(Color.WHITE);

        lblArtist = new JLabel("Select a song to start");
        lblArtist.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblArtist.setForeground(TEXT_MUTED);

        textPanel.add(lblTitle);
        textPanel.add(lblArtist);

        btnFavorite = createIconButton("\u2661", 16, e -> {
            Track t = player.getCurrentTrack();
            if (t != null) {
                t.setFavorite(!t.isFavorite());
                btnFavorite.setText(t.isFavorite() ? "\u2665" : "\u2661");
                btnFavorite.setForeground(t.isFavorite() ? ACCENT_COLOR : TEXT_MUTED);
            }
        });

        leftPanel.add(lblArtwork);
        leftPanel.add(textPanel);
        leftPanel.add(btnFavorite);

        add(leftPanel, BorderLayout.WEST);
    }

    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 4));
        centerPanel.setOpaque(false);

        // Top row: Transport buttons
        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        controlsRow.setOpaque(false);

        btnShuffle = createIconButton("\u21C4", 15, e -> {
            queue.setShuffle(!queue.isShuffle());
            btnShuffle.setForeground(queue.isShuffle() ? ACCENT_COLOR : TEXT_MUTED);
        });

        btnPrev = createIconButton("\u23EE", 18, e -> {
            Track prev = queue.previous();
            if (prev != null) {
                player.play(prev);
                if (onTrackChangedCallback != null) onTrackChangedCallback.run();
            }
        });

        btnPlayPause = new JButton("\u25B6") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_COLOR);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnPlayPause.setPreferredSize(new Dimension(36, 36));
        btnPlayPause.setContentAreaFilled(false);
        btnPlayPause.setBorderPainted(false);
        btnPlayPause.setFocusPainted(false);
        btnPlayPause.setForeground(Color.WHITE);
        btnPlayPause.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnPlayPause.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPlayPause.addActionListener(e -> {
            if (player.getCurrentTrack() == null) {
                Track next = queue.next();
                if (next != null) {
                    player.play(next);
                    if (onTrackChangedCallback != null) onTrackChangedCallback.run();
                }
            } else {
                player.togglePlayPause();
            }
        });

        btnNext = createIconButton("\u23ED", 18, e -> {
            Track next = queue.next();
            if (next != null) {
                player.play(next);
                if (onTrackChangedCallback != null) onTrackChangedCallback.run();
            }
        });

        btnRepeat = createIconButton("\uD83D\uDD01", 14, e -> {
            queue.cycleRepeatMode();
            updateRepeatButtonVisual();
        });

        controlsRow.add(btnShuffle);
        controlsRow.add(btnPrev);
        controlsRow.add(btnPlayPause);
        controlsRow.add(btnNext);
        controlsRow.add(btnRepeat);

        // Bottom row: Seek bar and timestamps
        JPanel seekRow = new JPanel(new BorderLayout(8, 0));
        seekRow.setOpaque(false);

        lblCurrentTime = new JLabel("0:00");
        lblCurrentTime.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblCurrentTime.setForeground(TEXT_MUTED);

        seekSlider = new JSlider(0, 100, 0);
        seekSlider.setUI(new AppleSliderUI(seekSlider));
        seekSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isUserSeeking = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isUserSeeking = false;
            }
        });

        lblTotalTime = new JLabel("0:00");
        lblTotalTime.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTotalTime.setForeground(TEXT_MUTED);

        seekRow.add(lblCurrentTime, BorderLayout.WEST);
        seekRow.add(seekSlider, BorderLayout.CENTER);
        seekRow.add(lblTotalTime, BorderLayout.EAST);

        centerPanel.add(controlsRow, BorderLayout.NORTH);
        centerPanel.add(seekRow, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void initRightPanel() {
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 18));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(260, 68));

        JButton btnQueue = createIconButton("\u2630", 16, e -> {
            if (onQueueToggle != null) onQueueToggle.run();
        });
        btnQueue.setToolTipText("Up Next Queue");

        btnMute = createIconButton("\uD83D\uDD0A", 16, e -> {
            if (player.getVolume() > 0.001f) {
                previousVolume = player.getVolume();
                player.setVolume(0.0f);
                volumeSlider.setValue(0);
                btnMute.setText("\uD83D\uDD07");
            } else {
                float restore = previousVolume > 0.05f ? previousVolume : 0.85f;
                player.setVolume(restore);
                volumeSlider.setValue((int) (restore * 100));
                btnMute.setText("\uD83D\uDD0A");
            }
        });

        volumeSlider = new JSlider(0, 100, (int) (player.getVolume() * 100));
        volumeSlider.setPreferredSize(new Dimension(100, 16));
        volumeSlider.setUI(new AppleSliderUI(volumeSlider));
        volumeSlider.addChangeListener(e -> {
            float vol = volumeSlider.getValue() / 100.0f;
            player.setVolume(vol);
            btnMute.setText(vol <= 0.001f ? "\uD83D\uDD07" : "\uD83D\uDD0A");
        });

        rightPanel.add(btnQueue);
        rightPanel.add(btnMute);
        rightPanel.add(volumeSlider);

        add(rightPanel, BorderLayout.EAST);
    }

    private JButton createIconButton(String text, int fontSize, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        btn.setForeground(TEXT_MUTED);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn == btnShuffle && queue.isShuffle()) {
                    btn.setForeground(ACCENT_COLOR);
                } else if (btn == btnRepeat && queue.getRepeatMode() != PlaybackQueue.RepeatMode.OFF) {
                    btn.setForeground(ACCENT_COLOR);
                } else {
                    btn.setForeground(TEXT_MUTED);
                }
            }
        });
        return btn;
    }

    public void updateTrackInfo(Track track) {
        if (track != null) {
            lblTitle.setText(track.getTitle());
            lblArtist.setText(track.getArtist());
            BufferedImage art = track.getAlbumArt();
            if (art == null) {
                art = ArtworkGenerator.generateCover(track.getTitle(), track.getArtist(), 48);
            }
            lblArtwork.setIcon(new ImageIcon(art.getScaledInstance(48, 48, Image.SCALE_SMOOTH)));
            lblTotalTime.setText(track.getFormattedDuration());
            seekSlider.setMaximum((int) track.getDurationSeconds());
            seekSlider.setValue(0);
            btnFavorite.setText(track.isFavorite() ? "\u2665" : "\u2661");
            btnFavorite.setForeground(track.isFavorite() ? ACCENT_COLOR : TEXT_MUTED);
        } else {
            lblTitle.setText("Not Playing");
            lblArtist.setText("Select a song to start");
            lblArtwork.setIcon(new ImageIcon(ArtworkGenerator.generateCover("Music", "", 48)));
            lblCurrentTime.setText("0:00");
            lblTotalTime.setText("0:00");
            seekSlider.setValue(0);
        }
    }

    private void updateRepeatButtonVisual() {
        switch (queue.getRepeatMode()) {
            case OFF:
                btnRepeat.setText("\uD83D\uDD01");
                btnRepeat.setForeground(TEXT_MUTED);
                btnRepeat.setToolTipText("Repeat: Off");
                break;
            case REPEAT_ALL:
                btnRepeat.setText("\uD83D\uDD01");
                btnRepeat.setForeground(ACCENT_COLOR);
                btnRepeat.setToolTipText("Repeat: All");
                break;
            case REPEAT_ONE:
                btnRepeat.setText("\uD83D\uDD02"); // Repeat 1
                btnRepeat.setForeground(ACCENT_COLOR);
                btnRepeat.setToolTipText("Repeat: One Song");
                break;
        }
    }

    // AudioPlayerListener callbacks
    @Override
    public void onTrackStarted(Track track) {
        updateTrackInfo(track);
        btnPlayPause.setText("\u23F8"); // Pause icon
    }

    @Override
    public void onProgressUpdate(long currentSeconds, long totalSeconds) {
        if (!isUserSeeking) {
            seekSlider.setMaximum((int) totalSeconds);
            seekSlider.setValue((int) currentSeconds);
            lblCurrentTime.setText(FormatUtils.formatDuration(currentSeconds));
        }
    }

    @Override
    public void onStateChanged(PlaybackState newState) {
        if (newState == PlaybackState.PLAYING) {
            btnPlayPause.setText("\u23F8");
        } else {
            btnPlayPause.setText("\u25B6");
        }
    }

    @Override
    public void onTrackFinished(Track track) {
        // Automatically play next in queue
        Track next = queue.next();
        if (next != null) {
            player.play(next);
            if (onTrackChangedCallback != null) onTrackChangedCallback.run();
        } else {
            updateTrackInfo(null);
        }
    }

    @Override
    public void onError(String message, Throwable exception) {
        JOptionPane.showMessageDialog(this, message, "Playback Error", JOptionPane.ERROR_MESSAGE);
    }
}
