package com.musicplayer.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Apple Music styled left navigation sidebar.
 */
public class SidebarPanel extends JPanel {

    private static final Color BG_COLOR = new Color(0x18, 0x18, 0x1A);
    private static final Color BORDER_COLOR = new Color(0x2C, 0x2C, 0x2E);
    private static final Color ACCENT_COLOR = new Color(0xFF, 0x2D, 0x55);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(0x8E, 0x8E, 0x93);
    private static final Color ACTIVE_BG = new Color(0x28, 0x28, 0x2C);

    private final Consumer<String> onNavigationSelected;
    private final Runnable onOpenFolderClicked;
    private final Map<String, JPanel> navButtons = new HashMap<>();
    private String currentView = "Songs";

    public SidebarPanel(Consumer<String> onNavigationSelected, Runnable onOpenFolderClicked) {
        this.onNavigationSelected = onNavigationSelected;
        this.onOpenFolderClicked = onOpenFolderClicked;

        setPreferredSize(new Dimension(220, 600));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));

        initContent();
    }

    private void initContent() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(24, 16, 20, 16));

        // Header: Apple Music Icon and Title
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setOpaque(false);
        brandPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoIcon = new JLabel("\u266B");
        logoIcon.setFont(new Font("SansSerif", Font.BOLD, 22));
        logoIcon.setForeground(ACCENT_COLOR);

        JLabel brandTitle = new JLabel("Music");
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        brandTitle.setForeground(TEXT_PRIMARY);

        brandPanel.add(logoIcon);
        brandPanel.add(brandTitle);
        container.add(brandPanel);
        container.add(Box.createVerticalStrut(28));

        // Section: Library
        addSectionHeader(container, "LIBRARY");
        addNavItem(container, "Songs", "\u266C");
        addNavItem(container, "Albums", "\u25A3");
        addNavItem(container, "Artists", "\uD83D\uDC64");

        container.add(Box.createVerticalStrut(20));

        // Section: Playlists
        addSectionHeader(container, "PLAYLISTS");
        addNavItem(container, "Recently Added", "\uD83D\uDD53");
        addNavItem(container, "Favorites", "\u2665");

        container.add(Box.createVerticalStrut(20));

        // Section: Store / Downloads
        addSectionHeader(container, "DOWNLOADS");
        addNavItem(container, "Download Music", "\u2913");
        addNavItem(container, "Up Next Queue", "\u2630");

        add(container, BorderLayout.NORTH);

        // Bottom panel: Open Folder button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(16, 16, 20, 16));

        JButton btnOpenFolder = new JButton("\uD83D\uDCC2  Open Folder...") {
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
        btnOpenFolder.setForeground(TEXT_PRIMARY);
        btnOpenFolder.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnOpenFolder.setContentAreaFilled(false);
        btnOpenFolder.setBorderPainted(false);
        btnOpenFolder.setFocusPainted(false);
        btnOpenFolder.setPreferredSize(new Dimension(180, 36));
        btnOpenFolder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOpenFolder.addActionListener(e -> onOpenFolderClicked.run());

        bottomPanel.add(btnOpenFolder, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Select default
        setSelectedView("Songs");
    }

    private void addSectionHeader(JPanel container, String title) {
        JLabel header = new JLabel(title);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setForeground(TEXT_MUTED);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(new EmptyBorder(0, 8, 8, 0));
        container.add(header);
    }

    private void addNavItem(JPanel container, String label, String iconGlyph) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                if (label.equals(currentView)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(ACTIVE_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setMaximumSize(new Dimension(190, 36));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(iconGlyph);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 14));
        icon.setForeground(label.equals(currentView) ? ACCENT_COLOR : TEXT_MUTED);

        JLabel text = new JLabel(label);
        text.setFont(new Font("SansSerif", Font.PLAIN, 13));
        text.setForeground(label.equals(currentView) ? TEXT_PRIMARY : TEXT_MUTED);

        item.add(icon);
        item.add(text);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setSelectedView(label);
                onNavigationSelected.accept(label);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!label.equals(currentView)) {
                    text.setForeground(TEXT_PRIMARY);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!label.equals(currentView)) {
                    text.setForeground(TEXT_MUTED);
                }
            }
        });

        navButtons.put(label, item);
        container.add(item);
        container.add(Box.createVerticalStrut(4));
    }

    public void setSelectedView(String viewName) {
        this.currentView = viewName;
        for (Map.Entry<String, JPanel> entry : navButtons.entrySet()) {
            JPanel panel = entry.getValue();
            boolean active = entry.getKey().equals(viewName);
            Component[] comps = panel.getComponents();
            if (comps.length >= 2) {
                comps[0].setForeground(active ? ACCENT_COLOR : TEXT_MUTED);
                comps[1].setForeground(active ? TEXT_PRIMARY : TEXT_MUTED);
            }
            panel.repaint();
        }
    }
}
