package com.musicplayer.ui;

import com.musicplayer.model.PlaybackQueue;
import com.musicplayer.model.Track;
import com.musicplayer.player.AudioPlayer;
import com.musicplayer.ui.components.ModernScrollBarUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Slide-in / dockable Up Next Queue panel.
 */
public class QueueDrawerPanel extends JPanel {

    private final PlaybackQueue queue;
    private final AudioPlayer player;
    private final Consumer<Track> onTrackSelected;

    private JPanel listContainer;
    private JLabel lblQueueCount;

    private static final Color BG_COLOR = new Color(0x1E, 0x1E, 0x22);
    private static final Color CARD_BG = new Color(0x28, 0x28, 0x2C);
    private static final Color BORDER_COLOR = new Color(0x33, 0x33, 0x36);
    private static final Color ACCENT_COLOR = new Color(0xFF, 0x2D, 0x55);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(0x8E, 0x8E, 0x93);

    public QueueDrawerPanel(PlaybackQueue queue, AudioPlayer player, Consumer<Track> onTrackSelected) {
        this.queue = queue;
        this.player = player;
        this.onTrackSelected = onTrackSelected;

        setPreferredSize(new Dimension(280, 600));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COLOR));

        initHeader();
        initQueueList();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 16, 12, 16));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);

        JLabel lblTitle = new JLabel("Up Next");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitle.setForeground(TEXT_PRIMARY);

        lblQueueCount = new JLabel("0 tracks");
        lblQueueCount.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblQueueCount.setForeground(TEXT_MUTED);

        titleBox.add(lblTitle);
        titleBox.add(lblQueueCount);

        JButton btnClear = new JButton("Clear") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x2C, 0x2C, 0x30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnClear.setForeground(TEXT_MUTED);
        btnClear.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnClear.setContentAreaFilled(false);
        btnClear.setBorderPainted(false);
        btnClear.setFocusPainted(false);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> {
            queue.clear();
            refreshQueue();
        });

        header.add(titleBox, BorderLayout.WEST);
        header.add(btnClear, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    private void initQueueList() {
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(BG_COLOR);
        listContainer.setBorder(new EmptyBorder(6, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshQueue() {
        listContainer.removeAll();
        List<Track> tracks = queue.getActiveQueue();
        int currentIdx = queue.getCurrentIndex();

        lblQueueCount.setText(tracks.size() + " tracks");

        if (tracks.isEmpty()) {
            JLabel emptyLbl = new JLabel("Queue is empty");
            emptyLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            emptyLbl.setForeground(TEXT_MUTED);
            emptyLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLbl.setBorder(new EmptyBorder(30, 0, 0, 0));
            listContainer.add(emptyLbl);
        } else {
            for (int i = 0; i < tracks.size(); i++) {
                Track t = tracks.get(i);
                boolean isCurrent = (i == currentIdx);
                listContainer.add(createQueueItem(t, i, isCurrent));
                listContainer.add(Box.createVerticalStrut(4));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createQueueItem(Track track, int index, boolean isCurrent) {
        JPanel item = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isCurrent ? new Color(0x32, 0x22, 0x2A) : CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                if (isCurrent) {
                    g2.setColor(ACCENT_COLOR);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        item.setPreferredSize(new Dimension(250, 44));
        item.setBorder(new EmptyBorder(4, 8, 4, 8));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(isCurrent ? "\u25B6" : String.valueOf(index + 1));
        icon.setFont(new Font("SansSerif", Font.BOLD, 11));
        icon.setForeground(isCurrent ? ACCENT_COLOR : TEXT_MUTED);
        icon.setPreferredSize(new Dimension(22, 36));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        textPanel.setOpaque(false);

        JLabel title = new JLabel(track.getTitle());
        title.setFont(new Font("SansSerif", Font.PLAIN, 12));
        title.setForeground(isCurrent ? ACCENT_COLOR : TEXT_PRIMARY);

        JLabel artist = new JLabel(track.getArtist());
        artist.setFont(new Font("SansSerif", Font.PLAIN, 10));
        artist.setForeground(TEXT_MUTED);

        textPanel.add(title);
        textPanel.add(artist);

        JButton btnRemove = new JButton("\u00D7");
        btnRemove.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRemove.setForeground(TEXT_MUTED);
        btnRemove.setContentAreaFilled(false);
        btnRemove.setBorderPainted(false);
        btnRemove.setFocusPainted(false);
        btnRemove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRemove.addActionListener(e -> {
            queue.removeTrack(index);
            refreshQueue();
        });

        item.add(icon, BorderLayout.WEST);
        item.add(textPanel, BorderLayout.CENTER);
        item.add(btnRemove, BorderLayout.EAST);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Track selected = queue.playIndex(index);
                if (selected != null) {
                    onTrackSelected.accept(selected);
                    refreshQueue();
                }
            }
        });

        return item;
    }
}
