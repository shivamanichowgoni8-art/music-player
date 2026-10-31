package com.musicplayer.ui;

import com.musicplayer.downloader.MusicDownloader;
import com.musicplayer.model.PlaybackQueue;
import com.musicplayer.model.Track;
import com.musicplayer.player.AudioPlayer;
import com.musicplayer.util.SampleAudioHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window for Apple Music Desktop Player.
 */
public class MusicPlayerGUI extends JFrame {

    private final AudioPlayer player;
    private final PlaybackQueue queue;
    private final MusicDownloader downloader;

    private SidebarPanel sidebarPanel;
    private MediaLibraryPanel mediaLibraryPanel;
    private DownloadPanel downloadPanel;
    private QueueDrawerPanel queueDrawerPanel;
    private PlaybackBar playbackBar;

    private JPanel mainContentCardPanel;
    private CardLayout mainCardLayout;
    private boolean isQueueVisible = false;

    private final List<Track> loadedTracks = new ArrayList<>();
    private File currentMusicFolder;

    private static final Color BG_DARK = new Color(0x1C, 0x1C, 0x1E);

    public MusicPlayerGUI() {
        super("Apple Music");

        // Initialize core engines
        this.player = new AudioPlayer();
        this.queue = new PlaybackQueue();

        File defaultMusicFolder = new File(System.getProperty("user.home"), "Music");
        if (!defaultMusicFolder.exists()) {
            defaultMusicFolder = new File("downloaded_music");
        }
        this.downloader = new MusicDownloader(new File("downloaded_music"));

        initWindow();
        initUI();

        // Load initial or demo tracks
        loadDefaultTracks(defaultMusicFolder);
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                player.shutdown();
                downloader.shutdown();
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Center card views (Library / Downloader)
        mainCardLayout = new CardLayout();
        mainContentCardPanel = new JPanel(mainCardLayout);
        mainContentCardPanel.setBackground(BG_DARK);

        mediaLibraryPanel = new MediaLibraryPanel(player, queue, this::playSelectedTrack, this::onSongDeleted);
        downloadPanel = new DownloadPanel(downloader, this::onSongDownloaded);

        mainContentCardPanel.add(mediaLibraryPanel, "LIBRARY");
        mainContentCardPanel.add(downloadPanel, "DOWNLOADS");

        // Right Queue Drawer
        queueDrawerPanel = new QueueDrawerPanel(queue, player, this::playSelectedTrack);
        queueDrawerPanel.setVisible(false);

        // Center Split / Container
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(BG_DARK);
        centerContainer.add(mainContentCardPanel, BorderLayout.CENTER);
        centerContainer.add(queueDrawerPanel, BorderLayout.EAST);

        // Left Sidebar
        sidebarPanel = new SidebarPanel(this::onNavigationSelected, this::chooseMusicFolder);

        // Bottom Playback Bar
        playbackBar = new PlaybackBar(player, queue, this::toggleQueueDrawer, () -> {
            mediaLibraryPanel.repaint();
            queueDrawerPanel.refreshQueue();
        });

        add(sidebarPanel, BorderLayout.WEST);
        add(centerContainer, BorderLayout.CENTER);
        add(playbackBar, BorderLayout.SOUTH);
    }

    private void onNavigationSelected(String viewName) {
        if ("Download Music".equals(viewName)) {
            mainCardLayout.show(mainContentCardPanel, "DOWNLOADS");
        } else if ("Up Next Queue".equals(viewName)) {
            toggleQueueDrawer();
        } else {
            mainCardLayout.show(mainContentCardPanel, "LIBRARY");
            filterLibraryBySection(viewName);
        }
    }

    private void filterLibraryBySection(String section) {
        if ("Favorites".equals(section)) {
            List<Track> favs = new ArrayList<>();
            for (Track t : loadedTracks) {
                if (t.isFavorite()) favs.add(t);
            }
            mediaLibraryPanel.setTracks(favs);
        } else {
            mediaLibraryPanel.setTracks(loadedTracks);
        }
    }

    private void toggleQueueDrawer() {
        isQueueVisible = !isQueueVisible;
        queueDrawerPanel.setVisible(isQueueVisible);
        if (isQueueVisible) {
            queueDrawerPanel.refreshQueue();
        }
        revalidate();
        repaint();
    }

    public void chooseMusicFolder() {
        JFileChooser chooser = new JFileChooser(currentMusicFolder != null ? currentMusicFolder : new File("."));
        chooser.setDialogTitle("Select Music Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();
            scanAndLoadFolder(selectedFolder);
        }
    }

    public void scanAndLoadFolder(File folder) {
        this.currentMusicFolder = folder;
        loadedTracks.clear();

        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && isAudioFile(f)) {
                    loadedTracks.add(new Track(f));
                }
            }
        }

        // Also scan downloaded_music folder if distinct
        File dlFolder = downloader.getDownloadFolder();
        if (dlFolder.exists() && !dlFolder.equals(folder)) {
            File[] dlFiles = dlFolder.listFiles();
            if (dlFiles != null) {
                for (File f : dlFiles) {
                    if (f.isFile() && isAudioFile(f)) {
                        Track t = new Track(f);
                        if (!loadedTracks.contains(t)) {
                            loadedTracks.add(t);
                        }
                    }
                }
            }
        }

        mediaLibraryPanel.setTracks(loadedTracks);
        queue.setTracks(loadedTracks);
        queueDrawerPanel.refreshQueue();

        if (loadedTracks.isEmpty()) {
            createDemoTracksIfEmpty(folder);
        }
    }

    private boolean isAudioFile(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".aiff") || name.endsWith(".au");
    }

    private void onSongDownloaded(Track track) {
        if (!loadedTracks.contains(track)) {
            loadedTracks.add(0, track);
            mediaLibraryPanel.setTracks(loadedTracks);
            queue.addTrack(track);
            queueDrawerPanel.refreshQueue();
        }
    }

    private void onSongDeleted(Track track) {
        loadedTracks.remove(track);
        mediaLibraryPanel.setTracks(loadedTracks);
        queue.removeTrack(queue.getActiveQueue().indexOf(track));
        queueDrawerPanel.refreshQueue();
    }

    private void playSelectedTrack(Track track) {
        player.play(track);
        playbackBar.updateTrackInfo(track);
        queueDrawerPanel.refreshQueue();
    }

    private void loadDefaultTracks(File folder) {
        scanAndLoadFolder(folder);
    }

    private void createDemoTracksIfEmpty(File folder) {
        // Generate 3 harmonic sample tracks for immediate offline enjoyment
        File demoDir = new File(downloader.getDownloadFolder(), "Demo Tracks");
        demoDir.mkdirs();

        try {
            File demo1 = new File(demoDir, "Chill Horizon.wav");
            File demo2 = new File(demoDir, "Velvet Sunset.wav");
            File demo3 = new File(demoDir, "Midnight Melody.wav");

            if (!demo1.exists()) SampleAudioHelper.createSynthesizedTrack(demo1, "Chill Horizon", 18);
            if (!demo2.exists()) SampleAudioHelper.createSynthesizedTrack(demo2, "Velvet Sunset", 22);
            if (!demo3.exists()) SampleAudioHelper.createSynthesizedTrack(demo3, "Midnight Melody", 20);

            Track t1 = new Track("1", "Chill Horizon", "Lofi Sunset", "Ambient Vibes", 18, demo1);
            Track t2 = new Track("2", "Velvet Sunset", "Acoustic Breeze", "Summer Reverie", 22, demo2);
            Track t3 = new Track("3", "Midnight Melody", "Synth Dreamer", "Nightscapes", 20, demo3);

            loadedTracks.add(t1);
            loadedTracks.add(t2);
            loadedTracks.add(t3);

            mediaLibraryPanel.setTracks(loadedTracks);
            queue.setTracks(loadedTracks);
            queueDrawerPanel.refreshQueue();
        } catch (Exception ignored) {}
    }
}
