package com.musicplayer.ui;

import com.musicplayer.model.PlaybackQueue;
import com.musicplayer.model.Track;
import com.musicplayer.player.AudioPlayer;
import com.musicplayer.ui.components.ArtworkGenerator;
import com.musicplayer.ui.components.ModernScrollBarUI;
import com.musicplayer.util.FormatUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main panel presenting the Media Library in List or Grid view with real-time search.
 */
public class MediaLibraryPanel extends JPanel {

    private final AudioPlayer player;
    private final PlaybackQueue queue;
    private final Consumer<Track> onTrackSelected;
    private final Consumer<Track> onTrackDeleted;

    private List<Track> allTracks = new ArrayList<>();
    private List<Track> filteredTracks = new ArrayList<>();

    private JTextField searchField;
    private JTable trackTable;
    private DefaultTableModel tableModel;
    private JPanel gridContainer;
    private CardLayout cardLayout;
    private JPanel contentContainer;
    private JLabel lblStatus;

    private boolean isGridView = false;

    private static final Color BG_COLOR = new Color(0x1C, 0x1C, 0x1E);
    private static final Color CARD_BG = new Color(0x24, 0x24, 0x28);
    private static final Color ROW_HOVER = new Color(0x2C, 0x2C, 0x30);
    private static final Color ACCENT_COLOR = new Color(0xFF, 0x2D, 0x55);
    private static final Color DANGER_COLOR = new Color(0xFF, 0x45, 0x3A);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(0x8E, 0x8E, 0x93);

    public MediaLibraryPanel(AudioPlayer player, PlaybackQueue queue, Consumer<Track> onTrackSelected) {
        this(player, queue, onTrackSelected, null);
    }

    public MediaLibraryPanel(AudioPlayer player, PlaybackQueue queue, Consumer<Track> onTrackSelected, Consumer<Track> onTrackDeleted) {
        this.player = player;
        this.queue = queue;
        this.onTrackSelected = onTrackSelected;
        this.onTrackDeleted = onTrackDeleted;

        setBackground(BG_COLOR);
        setLayout(new BorderLayout());

        initTopBar();
        initCenterView();
    }

    private void initTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setBackground(BG_COLOR);
        topBar.setBorder(new EmptyBorder(20, 25, 15, 25));

        // Left: Search Field
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBox.setOpaque(false);

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchIcon.setForeground(TEXT_MUTED);

        searchField = new JTextField(18) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x2C, 0x2C, 0x2E));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchField.setPreferredSize(new Dimension(240, 34));
        searchField.setBackground(new Color(0x2C, 0x2C, 0x2E));
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(ACCENT_COLOR);
        searchField.setBorder(new EmptyBorder(4, 12, 4, 12));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Search songs, artists, albums...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        searchBox.add(searchIcon);
        searchBox.add(searchField);
        topBar.add(searchBox, BorderLayout.WEST);

        // Right: View Switcher and Track Stats
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightBox.setOpaque(false);

        lblStatus = new JLabel("0 songs");
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblStatus.setForeground(TEXT_MUTED);

        JButton btnPlayAll = createActionButton("\u25B6 Play All", e -> {
            if (!filteredTracks.isEmpty()) {
                queue.setTracks(filteredTracks);
                Track first = queue.playIndex(0);
                if (first != null) onTrackSelected.accept(first);
            }
        });

        JButton btnShufflePlay = createActionButton("\u21C4 Shuffle", e -> {
            if (!filteredTracks.isEmpty()) {
                queue.setTracks(filteredTracks);
                queue.setShuffle(true);
                Track first = queue.playIndex(0);
                if (first != null) onTrackSelected.accept(first);
            }
        });

        JButton btnToggleList = createIconButton("\u2630", 14, e -> switchToView(false));
        btnToggleList.setToolTipText("List View");
        JButton btnToggleGrid = createIconButton("\u25A4", 14, e -> switchToView(true));
        btnToggleGrid.setToolTipText("Grid View");

        rightBox.add(lblStatus);
        rightBox.add(btnPlayAll);
        rightBox.add(btnShufflePlay);
        rightBox.add(btnToggleList);
        rightBox.add(btnToggleGrid);

        topBar.add(rightBox, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);
    }

    private void initCenterView() {
        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);
        contentContainer.setBackground(BG_COLOR);

        // 1. List View
        initListView();

        // 2. Grid View
        initGridView();

        add(contentContainer, BorderLayout.CENTER);
    }

    private void initListView() {
        String[] columns = {"#", "TITLE", "ARTIST", "ALBUM", "TIME"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        trackTable = new JTable(tableModel);
        trackTable.setBackground(BG_COLOR);
        trackTable.setForeground(TEXT_PRIMARY);
        trackTable.setSelectionBackground(ROW_HOVER);
        trackTable.setSelectionForeground(TEXT_PRIMARY);
        trackTable.setRowHeight(42);
        trackTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        trackTable.setShowGrid(false);
        trackTable.setIntercellSpacing(new Dimension(0, 0));
        trackTable.setFillsViewportHeight(true);

        // Header styling
        trackTable.getTableHeader().setBackground(BG_COLOR);
        trackTable.getTableHeader().setForeground(TEXT_MUTED);
        trackTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        trackTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x33, 0x33, 0x36)));
        trackTable.getTableHeader().setPreferredSize(new Dimension(100, 32));

        // Column widths
        trackTable.getColumnModel().getColumn(0).setMaxWidth(45);
        trackTable.getColumnModel().getColumn(4).setMaxWidth(80);

        // Cell renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setBackground(BG_COLOR);
        centerRenderer.setForeground(TEXT_MUTED);
        trackTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer timeRenderer = new DefaultTableCellRenderer();
        timeRenderer.setHorizontalAlignment(JLabel.RIGHT);
        timeRenderer.setBackground(BG_COLOR);
        timeRenderer.setForeground(TEXT_MUTED);
        trackTable.getColumnModel().getColumn(4).setCellRenderer(timeRenderer);

        // Click to play
        trackTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = trackTable.getSelectedRow();
                if (row >= 0 && row < filteredTracks.size()) {
                    Track track = filteredTracks.get(row);
                    queue.setTracks(filteredTracks);
                    queue.playTrack(track);
                    onTrackSelected.accept(track);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = trackTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < filteredTracks.size()) {
                        trackTable.setRowSelectionInterval(row, row);
                        showContextMenu(e.getComponent(), e.getX(), e.getY(), filteredTracks.get(row));
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(trackTable);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(new EmptyBorder(0, 20, 0, 20));
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        contentContainer.add(scrollPane, "LIST");
    }

    private void initGridView() {
        gridContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        gridContainer.setBackground(BG_COLOR);

        JScrollPane scrollPane = new JScrollPane(gridContainer);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(new EmptyBorder(0, 20, 0, 20));
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        contentContainer.add(scrollPane, "GRID");
    }

    private void showContextMenu(Component comp, int x, int y, Track track) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(0x28, 0x28, 0x2C));
        menu.setBorder(BorderFactory.createLineBorder(new Color(0x3A, 0x3A, 0x3E)));

        JMenuItem itemPlay = new JMenuItem("\u25B6 Play Now");
        itemPlay.setForeground(TEXT_PRIMARY);
        itemPlay.addActionListener(e -> {
            queue.setTracks(filteredTracks);
            queue.playTrack(track);
            onTrackSelected.accept(track);
        });

        JMenuItem itemNext = new JMenuItem("\u2935 Play Next");
        itemNext.setForeground(TEXT_PRIMARY);
        itemNext.addActionListener(e -> queue.playNext(track));

        JMenuItem itemQueue = new JMenuItem("+ Add to Queue");
        itemQueue.setForeground(TEXT_PRIMARY);
        itemQueue.addActionListener(e -> queue.addTrack(track));

        JMenuItem itemDelete = new JMenuItem("\uD83D\uDDD1 Delete Song");
        itemDelete.setForeground(DANGER_COLOR);
        itemDelete.addActionListener(e -> confirmAndDeleteTrack(track));

        menu.add(itemPlay);
        menu.add(itemNext);
        menu.add(itemQueue);
        menu.addSeparator();
        menu.add(itemDelete);
        menu.show(comp, x, y);
    }

    public void confirmAndDeleteTrack(Track track) {
        if (track == null) return;
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete \"" + track.getTitle() + "\" from your library and device storage?",
                "Delete Song",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            // If playing this track, stop
            if (player.getCurrentTrack() != null && player.getCurrentTrack().equals(track)) {
                player.stop();
            }

            // Remove file from disk
            if (track.getFile() != null && track.getFile().exists()) {
                try {
                    track.getFile().delete();
                } catch (Exception ignored) {}
            }

            // Remove from local memory
            allTracks.remove(track);
            filteredTracks.remove(track);
            queue.getActiveQueue(); // triggers refresh

            // Notify parent
            if (onTrackDeleted != null) {
                onTrackDeleted.accept(track);
            }

            applyFilter();
            JOptionPane.showMessageDialog(this, "Deleted \"" + track.getTitle() + "\" successfully.", "Song Deleted", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void switchToView(boolean grid) {
        this.isGridView = grid;
        cardLayout.show(contentContainer, grid ? "GRID" : "LIST");
        if (grid) renderGrid();
    }

    public void setTracks(List<Track> tracks) {
        this.allTracks = new ArrayList<>(tracks != null ? tracks : List.of());
        applyFilter();
    }

    public void applyFilter() {
        String query = searchField.getText().trim().toLowerCase();
        filteredTracks.clear();

        if (query.isEmpty()) {
            filteredTracks.addAll(allTracks);
        } else {
            for (Track t : allTracks) {
                if (t.getTitle().toLowerCase().contains(query)
                        || t.getArtist().toLowerCase().contains(query)
                        || t.getAlbum().toLowerCase().contains(query)) {
                    filteredTracks.add(t);
                }
            }
        }

        renderList();
        if (isGridView) renderGrid();

        long totalSecs = 0;
        for (Track t : filteredTracks) totalSecs += t.getDurationSeconds();
        lblStatus.setText(filteredTracks.size() + " songs, " + FormatUtils.formatDuration(totalSecs));
    }

    private void renderList() {
        tableModel.setRowCount(0);
        for (int i = 0; i < filteredTracks.size(); i++) {
            Track t = filteredTracks.get(i);
            tableModel.addRow(new Object[]{
                    String.valueOf(i + 1),
                    t.getTitle(),
                    t.getArtist(),
                    t.getAlbum(),
                    t.getFormattedDuration()
            });
        }
    }

    private void renderGrid() {
        gridContainer.removeAll();
        for (Track track : filteredTracks) {
            JPanel card = createTrackCard(track);
            gridContainer.add(card);
        }
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private JPanel createTrackCard(Track track) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(160, 210));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Artwork
        BufferedImage art = track.getAlbumArt();
        if (art == null) art = ArtworkGenerator.generateCover(track.getTitle(), track.getArtist(), 140);
        JLabel lblArt = new JLabel(new ImageIcon(art.getScaledInstance(140, 140, Image.SCALE_SMOOTH)));
        lblArt.setPreferredSize(new Dimension(140, 140));

        // Details
        JPanel details = new JPanel(new GridLayout(2, 1, 0, 2));
        details.setOpaque(false);

        JLabel title = new JLabel(track.getTitle());
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setForeground(TEXT_PRIMARY);

        JLabel artist = new JLabel(track.getArtist());
        artist.setFont(new Font("SansSerif", Font.PLAIN, 11));
        artist.setForeground(TEXT_MUTED);

        details.add(title);
        details.add(artist);

        card.add(lblArt, BorderLayout.CENTER);
        card.add(details, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                queue.setTracks(filteredTracks);
                queue.playTrack(track);
                onTrackSelected.accept(track);
            }
        });

        return card;
    }

    private JButton createActionButton(String text, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x2C, 0x2C, 0x2E));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }

    private JButton createIconButton(String text, int size, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, size));
        btn.setForeground(TEXT_MUTED);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }
}
