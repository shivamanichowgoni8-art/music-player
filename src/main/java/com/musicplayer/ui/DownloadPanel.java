package com.musicplayer.ui;

import com.musicplayer.downloader.DownloadListener;
import com.musicplayer.downloader.MusicDownloader;
import com.musicplayer.model.DownloadableSong;
import com.musicplayer.model.Track;
import com.musicplayer.ui.components.ModernScrollBarUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel allowing users to download music onto their device and play locally.
 */
public class DownloadPanel extends JPanel implements DownloadListener {

    private final MusicDownloader downloader;
    private final Consumer<Track> onNewTrackDownloaded;

    private JLabel lblFolder;
    private JTextField txtUrl;
    private JTextField txtTitle;
    private JTextField txtArtist;
    private JProgressBar progressBar;
    private JLabel lblDownloadStatus;
    private JPanel catalogGrid;

    private static final Color BG_COLOR = new Color(0x1C, 0x1C, 0x1E);
    private static final Color CARD_BG = new Color(0x24, 0x24, 0x28);
    private static final Color ACCENT_COLOR = new Color(0xFF, 0x2D, 0x55);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(0x8E, 0x8E, 0x93);

    public DownloadPanel(MusicDownloader downloader, Consumer<Track> onNewTrackDownloaded) {
        this.downloader = downloader;
        this.onNewTrackDownloaded = onNewTrackDownloaded;
        this.downloader.addListener(this);

        setBackground(BG_COLOR);
        setLayout(new BorderLayout());

        initContent();
    }

    private void initContent() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(BG_COLOR);
        container.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header Section
        JLabel title = new JLabel("Download Music to Device");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Download songs directly to your local storage to play offline anytime.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        container.add(title);
        container.add(Box.createVerticalStrut(4));
        container.add(subtitle);
        container.add(Box.createVerticalStrut(18));

        // Storage Folder Banner
        JPanel folderPanel = new JPanel(new BorderLayout(10, 0));
        folderPanel.setBackground(CARD_BG);
        folderPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        folderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        folderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblFolder = new JLabel("\uD83D\uDCC1 Saving to: " + downloader.getDownloadFolder().getAbsolutePath());
        lblFolder.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblFolder.setForeground(TEXT_MUTED);

        JButton btnChangeFolder = new JButton("Change Folder") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x3A, 0x3A, 0x3C));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnChangeFolder.setForeground(TEXT_PRIMARY);
        btnChangeFolder.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnChangeFolder.setContentAreaFilled(false);
        btnChangeFolder.setBorderPainted(false);
        btnChangeFolder.setFocusPainted(false);
        btnChangeFolder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnChangeFolder.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(downloader.getDownloadFolder());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                lblFolder.setText("\uD83D\uDCC1 Saving to: " + chooser.getSelectedFile().getAbsolutePath());
            }
        });

        folderPanel.add(lblFolder, BorderLayout.CENTER);
        folderPanel.add(btnChangeFolder, BorderLayout.EAST);
        container.add(folderPanel);
        container.add(Box.createVerticalStrut(24));

        // Custom URL Downloader Form
        JLabel formHeader = new JLabel("DOWNLOAD VIA URL");
        formHeader.setFont(new Font("SansSerif", Font.BOLD, 11));
        formHeader.setForeground(TEXT_MUTED);
        formHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(formHeader);
        container.add(Box.createVerticalStrut(8));

        JPanel urlPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        urlPanel.setOpaque(false);
        urlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        urlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUrl = createTextField("Direct MP3 / Audio URL");
        txtTitle = createTextField("Song Title (optional)");
        txtArtist = createTextField("Artist Name (optional)");

        JButton btnDownloadUrl = new JButton("\u2913 Download") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnDownloadUrl.setForeground(TEXT_PRIMARY);
        btnDownloadUrl.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnDownloadUrl.setContentAreaFilled(false);
        btnDownloadUrl.setBorderPainted(false);
        btnDownloadUrl.setFocusPainted(false);
        btnDownloadUrl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDownloadUrl.addActionListener(e -> {
            String url = txtUrl.getText().trim();
            if (url.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter an audio URL.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String titleVal = txtTitle.getText().trim();
            if (titleVal.isEmpty()) titleVal = "Online Song " + System.currentTimeMillis() % 1000;
            String artistVal = txtArtist.getText().trim();
            if (artistVal.isEmpty()) artistVal = "Web Download";

            downloader.downloadSong(titleVal, artistVal, url);
            txtUrl.setText("");
        });

        urlPanel.add(txtUrl);
        urlPanel.add(txtTitle);
        urlPanel.add(txtArtist);
        urlPanel.add(btnDownloadUrl);

        container.add(urlPanel);
        container.add(Box.createVerticalStrut(14));

        // Progress Section
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 6));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progressBar.setForeground(ACCENT_COLOR);
        progressBar.setBackground(new Color(0x3A, 0x3A, 0x3C));
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblDownloadStatus = new JLabel("");
        lblDownloadStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDownloadStatus.setForeground(TEXT_MUTED);
        lblDownloadStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        container.add(progressBar);
        container.add(Box.createVerticalStrut(4));
        container.add(lblDownloadStatus);
        container.add(Box.createVerticalStrut(24));

        // Curated Catalog Section
        JLabel catalogHeader = new JLabel("CURATED CATALOG (1-CLICK DOWNLOAD)");
        catalogHeader.setFont(new Font("SansSerif", Font.BOLD, 11));
        catalogHeader.setForeground(TEXT_MUTED);
        catalogHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(catalogHeader);
        container.add(Box.createVerticalStrut(12));

        catalogGrid = new JPanel(new GridLayout(0, 2, 14, 14));
        catalogGrid.setOpaque(false);
        catalogGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<DownloadableSong> catalog = downloader.getCuratedCatalog();
        for (DownloadableSong song : catalog) {
            catalogGrid.add(createCatalogItem(song));
        }

        container.add(catalogGrid);

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createCatalogItem(DownloadableSong song) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
        info.setOpaque(false);

        JLabel title = new JLabel(song.getTitle());
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(TEXT_PRIMARY);

        JLabel meta = new JLabel(song.getArtist() + " \u2022 " + song.getGenre() + " \u2022 " + song.getDuration());
        meta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        meta.setForeground(TEXT_MUTED);

        info.add(title);
        info.add(meta);

        JButton btnGet = new JButton("\u2913 Get") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x3A, 0x3A, 0x3C));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnGet.setForeground(ACCENT_COLOR);
        btnGet.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnGet.setContentAreaFilled(false);
        btnGet.setBorderPainted(false);
        btnGet.setFocusPainted(false);
        btnGet.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGet.setPreferredSize(new Dimension(68, 30));
        btnGet.addActionListener(e -> downloader.downloadSong(song.getTitle(), song.getArtist(), song.getDownloadUrl()));

        card.add(info, BorderLayout.CENTER);
        card.add(btnGet, BorderLayout.EAST);
        return card;
    }

    private JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setBackground(new Color(0x2C, 0x2C, 0x2E));
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_COLOR);
        tf.setBorder(new EmptyBorder(4, 10, 4, 10));
        tf.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        return tf;
    }

    // DownloadListener callbacks
    @Override
    public void onDownloadStarted(String songTitle, String url) {
        progressBar.setVisible(true);
        progressBar.setValue(0);
        lblDownloadStatus.setText("Downloading: " + songTitle + "...");
    }

    @Override
    public void onDownloadProgress(String songTitle, int percentage, long bytesRead, long totalBytes) {
        if (percentage >= 0) {
            progressBar.setValue(percentage);
            lblDownloadStatus.setText("Downloading: " + songTitle + " (" + percentage + "%)");
        } else {
            progressBar.setIndeterminate(true);
            lblDownloadStatus.setText("Downloading: " + songTitle + "...");
        }
    }

    @Override
    public void onDownloadCompleted(String songTitle, File localFile) {
        progressBar.setVisible(false);
        lblDownloadStatus.setText("\u2714 Download complete: " + localFile.getName() + " (Saved to device)");

        // Add to player media library
        Track newTrack = new Track(localFile);
        if (onNewTrackDownloaded != null) {
            onNewTrackDownloaded.accept(newTrack);
        }
    }

    @Override
    public void onDownloadFailed(String songTitle, String errorMessage) {
        progressBar.setVisible(false);
        lblDownloadStatus.setText("\u26A0 Failed to download: " + songTitle + " (" + errorMessage + ")");
    }
}
