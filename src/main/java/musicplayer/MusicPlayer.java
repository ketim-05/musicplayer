package musicplayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javax.swing.*;

public class MusicPlayer extends JFrame implements ActionListener {
    private JButton addButton, playButton, pauseButton, resumeButton, stopButton;
    private JButton nextButton, prevButton, shuffleButton, repeatButton;
    private JLabel currentSongLabel, timeLabel;
    private JSlider progressBar;
    private JProgressBar songProgressBar;
    private JTree folderTree;
    private JList<String> playlist;
    private DefaultListModel<String> playlistModel;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private ArrayList<File> songs;
    private HashMap<String, ArrayList<File>> folderSongs;
    private int currentSongIndex = 0;
    private MediaPlayer mediaPlayer;
    private boolean isPaused = false;
    private boolean isPlaying = false;
    private boolean isRepeat = false;
    private boolean isShuffle = false;
    private File currentFile;
    private String currentFileName;
    private static final JFXPanel fxPanel = new JFXPanel(); // Initialize JavaFX toolkit
    private Duration lastPosition; // Add this field at the class level
    private Map<String, List<File>> savedPlaylists = new HashMap<>();
    private JComboBox<String> playlistComboBox;
    private boolean wasPlayingBeforeSeek = false;
    private JSlider volumeSlider;
    private ArrayList<File> favorites;
    private ArrayList<File> mostPlayed;
    private JList<String> favoritesList;
    private DefaultListModel<String> favoritesModel;

    public MusicPlayer() {
        setTitle("Modern Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        songs = new ArrayList<>();
        folderSongs = new HashMap<>();
        playlistModel = new DefaultListModel<>();
        rootNode = new DefaultMutableTreeNode("Music Library");
        treeModel = new DefaultTreeModel(rootNode);
        favorites = new ArrayList<>();
        mostPlayed = new ArrayList<>();
        favoritesModel = new DefaultListModel<>();
        favoritesList = new JList<>(favoritesModel);

        songProgressBar = new JProgressBar(0, 100);
        songProgressBar.setStringPainted(true); // Show percentage
        songProgressBar.setForeground(new Color(0, 204, 204)); // Cyan
        songProgressBar.setBackground(new Color(200, 200, 200)); // Light Gray
        songProgressBar.setBorderPainted(false);
        songProgressBar.setPreferredSize(new Dimension(getWidth(), 15));

        volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 50);
        volumeSlider.setMajorTickSpacing(20);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setBackground(new Color(240, 248, 255)); // Light Cyan
        volumeSlider.addChangeListener(e -> {
            int volume = volumeSlider.getValue();
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume / 100.0);
            }
        });

        initComponents();
    }

    private void initComponents() {
        // Add Menu Bar
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File Menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem openFile = new JMenuItem("Open File");
        JMenuItem openFolder = new JMenuItem("Open Folder");
        JMenuItem clearPlaylist = new JMenuItem("Clear Playlist");
        JMenuItem exit = new JMenuItem("Exit");

        fileMenu.add(openFile);
        fileMenu.add(openFolder);
        fileMenu.addSeparator();
        fileMenu.add(clearPlaylist);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        // Add Playlist Menu
        JMenu playlistMenu = new JMenu("Playlist");
        menuBar.add(playlistMenu);

        JMenuItem createPlaylist = new JMenuItem("Create Playlist");
        JMenuItem savePlaylist = new JMenuItem("Save Current Playlist");
        JMenuItem deletePlaylist = new JMenuItem("Delete Playlist");
        JMenuItem addToPlaylist = new JMenuItem("Add Songs to Playlist");

        playlistMenu.add(createPlaylist);
        playlistMenu.add(savePlaylist);
        playlistMenu.add(addToPlaylist);
        playlistMenu.addSeparator();
        playlistMenu.add(deletePlaylist);

        // Playback Menu
        JMenu playbackMenu = new JMenu("Playback");
        menuBar.add(playbackMenu);

        JMenuItem play = new JMenuItem("Play");
        JMenuItem pause = new JMenuItem("Pause");
        JMenuItem stop = new JMenuItem("Stop");
        JMenuItem next = new JMenuItem("Next");
        JMenuItem previous = new JMenuItem("Previous");
        JCheckBoxMenuItem shuffle = new JCheckBoxMenuItem("Shuffle");
        JCheckBoxMenuItem repeat = new JCheckBoxMenuItem("Repeat");

        playbackMenu.add(play);
        playbackMenu.add(pause);
        playbackMenu.add(stop);
        playbackMenu.addSeparator();
        playbackMenu.add(next);
        playbackMenu.add(previous);
        playbackMenu.addSeparator();
        playbackMenu.add(shuffle);
        playbackMenu.add(repeat);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem about = new JMenuItem("About");
        JMenuItem shortcuts = new JMenuItem("Shortcuts");

        helpMenu.add(shortcuts);
        helpMenu.add(about);

        // Add action listeners
        openFile.addActionListener(e -> addButton.doClick());

        openFolder.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File folder = fileChooser.getSelectedFile();
                addMusicFromFolder(folder);
            }
        });

        clearPlaylist.addActionListener(e -> {
            playlistModel.clear();
            songs.clear();
            if (mediaPlayer != null) {
                stopMusic();
            }
            currentSongIndex = -1;
            currentFile = null;
            currentFileName = "";
            currentSongLabel.setText("No song selected");
        });

        exit.addActionListener(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            System.exit(0);
        });

        // Playback menu actions
        play.addActionListener(e -> playButton.doClick());
        pause.addActionListener(e -> pauseButton.doClick());
        stop.addActionListener(e -> stopButton.doClick());
        next.addActionListener(e -> nextButton.doClick());
        previous.addActionListener(e -> prevButton.doClick());

        shuffle.addActionListener(e -> {
            shuffleButton.doClick();
            shuffle.setSelected(isShuffle);
        });

        repeat.addActionListener(e -> {
            repeatButton.doClick();
            repeat.setSelected(isRepeat);
        });

        // Help menu actions
        shortcuts.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Keyboard Shortcuts:\n\n" +
                            "Space - Play/Pause\n" +
                            "Ctrl + O - Open File\n" +
                            "Ctrl + F - Open Folder\n" +
                            "Ctrl + Right - Next Track\n" +
                            "Ctrl + Left - Previous Track\n" +
                            "Ctrl + S - Toggle Shuffle\n" +
                            "Ctrl + R - Toggle Repeat\n" +
                            "Ctrl + Q - Exit",
                    "Keyboard Shortcuts",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        about.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Music Player\nVersion 1.0\n\n" +
                            "A modern Java music player with advanced playback features.\n" +
                            "Created by Group 3 Students\n\n" +
                            "Supports: MP3, WAV, M4A, AAC",
                    "About Music Player",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        // Add
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // Space for play/pause
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "playPause");
        actionMap.put("playPause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    pauseButton.doClick();
                } else {
                    playButton.doClick();
                }
            }
        });

        // Ctrl + O for open file
        inputMap.put(KeyStroke.getKeyStroke("control O"), "openFile");
        actionMap.put("openFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile.doClick();
            }
        });

        // Ctrl + F for open folder
        inputMap.put(KeyStroke.getKeyStroke("control F"), "openFolder");
        actionMap.put("openFolder", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFolder.doClick();
            }
        });


        // Space - Play/Pause
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "playPause");
        actionMap.put("playPause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    pauseMusic(); // Directly call the method
                } else {
                    playMusic();
                }
            }
        });

        // Ctrl + Right - Next Song
        inputMap.put(KeyStroke.getKeyStroke("control RIGHT"), "nextSong");
        actionMap.put("nextSong", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextButton.doClick();
            }
        });

        // Ctrl + Left - Previous Song
        inputMap.put(KeyStroke.getKeyStroke("control LEFT"), "prevSong");
        actionMap.put("prevSong", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prevButton.doClick();
            }
        });

        // Ctrl + S - Toggle Shuffle
        inputMap.put(KeyStroke.getKeyStroke("control S"), "toggleShuffle");
        actionMap.put("toggleShuffle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shuffleButton.doClick();
            }
        });

        // Ctrl + R - Toggle Repeat
        inputMap.put(KeyStroke.getKeyStroke("control R"), "toggleRepeat");
        actionMap.put("toggleRepeat", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repeatButton.doClick();
            }
        });

        // Ctrl + Q - Quit Application
        inputMap.put(KeyStroke.getKeyStroke("control Q"), "quitApp");
        actionMap.put("quitApp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                System.exit(0);
            }
        });

        // Initialize folder tree
        folderTree = new JTree(treeModel);
        folderTree.setBackground(new Color(240, 248, 255)); // Light Cyan
        folderTree.setForeground(Color.BLACK);
        folderTree.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Initialize playlist with drag and drop
        playlist = new JList<>(playlistModel);
        playlist.setBackground(new Color(240, 248, 255)); // Light Cyan
        playlist.setForeground(Color.BLACK);
        playlist.setBorder(new EmptyBorder(10, 10, 10, 10));
        playlist.setDragEnabled(true);
        playlist.setDropMode(DropMode.INSERT);

        // Enable drag and drop for the playlist
        playlist.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                Transferable transferable = support.getTransferable();
                try {
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        if (isAudioFile(file)) {
                            songs.add(file);
                            playlistModel.addElement(file.getName());
                            System.out.println("Added file: " + file.getAbsolutePath());
                        }
                    }
                    if (!songs.isEmpty() && playlist.getSelectedIndex() == -1) {
                        playlist.setSelectedIndex(0);
                        currentFile = songs.get(0);
                        currentFileName = currentFile.getName();
                        currentSongLabel.setText("Selected: " + currentFileName);
                    }
                    return true;
                } catch (UnsupportedFlavorException | IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "Error adding files: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });

        // Add mouse listener for playlist
        playlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = playlist.locationToIndex(e.getPoint());
                if (index != -1) {
                    if (index == currentSongIndex && isPlaying) {
                        stopMusic();
                    } else {
                        // Clear last position if selecting a different song
                        if (index != currentSongIndex) {
                            lastPosition = null;
                        }
                        currentSongIndex = index;
                        currentFile = songs.get(currentSongIndex);
                        currentFileName = currentFile.getName();
                        currentSongLabel.setText("Selected: " + currentFileName);
                        playMusic();
                    }
                }
            }
        });

        // Add to Playlist action
        addToPlaylist.addActionListener(e -> {
            String currentPlaylist = (String) playlistComboBox.getSelectedItem();
            if (currentPlaylist != null && !currentPlaylist.equals("Default Playlist")) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Music Files", "mp3", "wav", "m4a", "aac", "wma", "ogg"));

                int result = fileChooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File[] selectedFiles = fileChooser.getSelectedFiles();
                    for (File file : selectedFiles) {
                        if (!songs.contains(file)) {
                            songs.add(file);
                            playlistModel.addElement(file.getName());
                        }
                    }
                    // Update the saved playlist
                    savedPlaylists.put(currentPlaylist, new ArrayList<>(songs));
                }

            } else {
                JOptionPane.showMessageDialog(this,
                        "Please create and select a playlist first!",
                        "No Playlist Selected",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        // Update the createPlaylist action to add a right-click context menu
        playlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPlaylistContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPlaylistContextMenu(e);
                }
            }
        });

        // Initialize buttons
        Color buttonColor = new Color(30, 144, 255); // Dodger Blue
        addButton = createButton("Add Music", buttonColor);
        playButton = createButton("Play", buttonColor);
        pauseButton = createButton("Pause", buttonColor);
        resumeButton = createButton("Resume", buttonColor);
        stopButton = createButton("Stop", buttonColor);
        nextButton = createButton("Next", buttonColor);
        prevButton = createButton("Previous", buttonColor);
        shuffleButton = createButton("Shuffle", buttonColor);
        repeatButton = createButton("Repeat", buttonColor);

        // Add action listeners
        addButton.addActionListener(this);
        playButton.addActionListener(this);
        pauseButton.addActionListener(this);
        resumeButton.addActionListener(this);
        stopButton.addActionListener(this);
        nextButton.addActionListener(this);
        prevButton.addActionListener(this);
        shuffleButton.addActionListener(this);
        repeatButton.addActionListener(this);

        // Set initial button states
        pauseButton.setEnabled(false);
        resumeButton.setEnabled(false);
        stopButton.setEnabled(false);

        // Labels and Progress Bar
        currentSongLabel = new JLabel("No song selected");
        currentSongLabel.setForeground(Color.BLACK);
        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setForeground(Color.BLACK);

        // Control Panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(new Color(240, 248, 255)); // Light Cyan
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // First row - Labels
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        controlPanel.add(currentSongLabel, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        controlPanel.add(timeLabel, gbc);

        // Progress Panel (contains both progress bar and slider)
        JPanel progressPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        progressPanel.setBackground(new Color(240, 248, 255)); // Light Cyan

        // Add progress bar
        progressPanel.add(songProgressBar);

        // Add slider
        progressBar = new JSlider(0, 100, 0);
        progressBar.setPreferredSize(new Dimension(400, 20));
        progressBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (mediaPlayer != null) {
                    wasPlayingBeforeSeek = isPlaying;
                    if (isPlaying) {
                        mediaPlayer.pause();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (mediaPlayer != null) {
                    double percent = e.getX() / (double) progressBar.getWidth();
                    mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(percent));
                    if (wasPlayingBeforeSeek) {
                        mediaPlayer.play();
                        isPlaying = true;
                    }
                }
            }
        });
        progressPanel.add(progressBar);

        // Add progress panel to control panel
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 5;
        controlPanel.add(progressPanel, gbc);

        // Third row - Control buttons
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        buttonPanel.setBackground(new Color(240, 248, 255)); // Light Cyan
        buttonPanel.add(prevButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(resumeButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(shuffleButton);
        buttonPanel.add(repeatButton);
        controlPanel.add(buttonPanel, gbc);

        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        controlPanel.add(volumeSlider, gbc);

        JScrollPane favoritesScrollPane = new JScrollPane(favoritesList);
        favoritesScrollPane.setPreferredSize(new Dimension(200, 300));
        controlPanel.add(favoritesScrollPane, gbc);

        // Add components to main panel
        JPanel playlistPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        playlistPanel.setBackground(new Color(240, 248, 255)); // Light Cyan

        playlistComboBox = new JComboBox<>();
        playlistComboBox.setPreferredSize(new Dimension(200, 25));
        playlistComboBox.addItem("Default Playlist");

        // Create Add Songs button
        JButton addSongsButton = createButton("Add Songs", new Color(30, 144, 255)); // Dodger Blue
        addSongsButton.setPreferredSize(new Dimension(150, 25)); // Increased width from 100 to 150

        // Add Label styling
        JLabel playlistLabel = new JLabel("Current Playlist: ");
        playlistLabel.setForeground(Color.BLACK);

        // Add components to playlist panel with more spacing
        playlistPanel.setBorder(new EmptyBorder(5, 10, 5, 10)); // Add some padding
        playlistPanel.add(playlistLabel);
        playlistPanel.add(Box.createHorizontalStrut(5)); // Add spacing
        playlistPanel.add(playlistComboBox);
        playlistPanel.add(Box.createHorizontalStrut(10)); // Add spacing
        playlistPanel.add(addSongsButton);

        // Add Songs button action
        addSongsButton.addActionListener(e -> {
            String currentPlaylist = (String) playlistComboBox.getSelectedItem();
            if (currentPlaylist != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Music Files", "mp3", "wav", "m4a", "aac", "wma", "ogg"));

                int result = fileChooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File[] selectedFiles = fileChooser.getSelectedFiles();
                    for (File file : selectedFiles) {
                        if (!songs.contains(file)) {
                            songs.add(file);
                            playlistModel.addElement(file.getName());
                        }
                    }
                    if (!currentPlaylist.equals("Default Playlist")) {
                        savedPlaylists.put(currentPlaylist, new ArrayList<>(songs));
                    }
                }
            }
        });

        // Playlist Menu Actions
        createPlaylist.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this,
                    "Enter playlist name:",
                    "Create New Playlist",
                    JOptionPane.PLAIN_MESSAGE);

            if (name != null && !name.trim().isEmpty()) {
                if (!savedPlaylists.containsKey(name)) {
                    savedPlaylists.put(name, new ArrayList<>());
                    playlistComboBox.addItem(name);
                    playlistComboBox.setSelectedItem(name);
                    // Clear current playlist
                    playlistModel.clear();
                    songs.clear();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "A playlist with this name already exists!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        savePlaylist.addActionListener(e -> {
            String currentPlaylist = (String) playlistComboBox.getSelectedItem();
            if (currentPlaylist != null && !currentPlaylist.equals("Default Playlist")) {
                savedPlaylists.put(currentPlaylist, new ArrayList<>(songs));
                JOptionPane.showMessageDialog(this,
                        "Playlist saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        deletePlaylist.addActionListener(e -> {
            String currentPlaylist = (String) playlistComboBox.getSelectedItem();
            if (currentPlaylist != null && !currentPlaylist.equals("Default Playlist")) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete this playlist?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    savedPlaylists.remove(currentPlaylist);
                    playlistComboBox.removeItem(currentPlaylist);
                    playlistComboBox.setSelectedItem("Default Playlist");
                    playlistModel.clear();
                    songs.clear();
                }
            }
        });

        // Add ComboBox listener
        playlistComboBox.addActionListener(e -> {
            String selectedPlaylist = (String) playlistComboBox.getSelectedItem();
            if (selectedPlaylist != null) {
                // Store current state
                File currentlyPlayingFile = currentFile;
                boolean wasPlaying = isPlaying;

                // Load the new playlist
                if (selectedPlaylist.equals("Default Playlist")) {
                    playlistModel.clear();
                    songs.clear();
                } else {
                    List<File> playlistSongs = savedPlaylists.get(selectedPlaylist);
                    if (playlistSongs != null) {
                        playlistModel.clear();
                        songs.clear();
                        for (File song : playlistSongs) {
                            if (song.exists()) {
                                songs.add(song);
                                playlistModel.addElement(song.getName());
                            }
                        }
                    }
                }

                // If a song was playing, check if it exists in the new playlist
                if (wasPlaying && currentlyPlayingFile != null) {
                    boolean foundSong = false;
                    for (int i = 0; i < songs.size(); i++) {
                        if (songs.get(i).getAbsolutePath().equals(currentlyPlayingFile.getAbsolutePath())) {
                            currentSongIndex = i;
                            playlist.setSelectedIndex(i);
                            foundSong = true;
                            break;
                        }
                    }

                    // If the song is not found in the new playlist, do not stop it
                    if (!foundSong) {
                        // Optionally, you can choose to keep playing the song or handle it differently
                        // For example, you can keep the current song playing and just update the
                        // playlist view
                        // currentSongLabel.setText("Now Playing: " + currentlyPlayingFile.getName());
                    }
                }
            }
        });

        // Add all components to the frame
        setLayout(new BorderLayout());
        add(playlistPanel, BorderLayout.NORTH);
        add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(folderTree), new JScrollPane(playlist)), BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Initialize JavaFX Platform
        Platform.setImplicitExit(false);
    }

    private void playMusic() {
        if (currentFile != null) {
            try {
                Media media = new Media(currentFile.toURI().toString());
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                mediaPlayer = new MediaPlayer(media);

                // Update progress bar as song plays
                mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                    if (mediaPlayer != null) {
                        Duration total = mediaPlayer.getTotalDuration();
                        if (total != null && newValue != null) {
                            double progress = (newValue.toMillis() / total.toMillis()) * 100;
                            progressBar.setValue((int) progress);
                            songProgressBar.setValue((int) progress);
                            Platform.runLater(() -> {
                                timeLabel.setText(formatDuration(newValue) + " / " + formatDuration(total));
                            });
                        }
                    }
                });

                mediaPlayer.play();
                isPlaying = true;
                isPaused = false;
                currentSongLabel.setText("Now Playing: " + currentFile.getName());
                playButton.setEnabled(false);
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
                resumeButton.setEnabled(false);

                // Add end of media handler
                mediaPlayer.setOnEndOfMedia(() -> {
                    if (isRepeat) {
                        // Restart the same song
                        Platform.runLater(() -> {
                            mediaPlayer.seek(Duration.ZERO);
                            mediaPlayer.play();
                        });
                    } else {
                        // Move to next song if available
                        Platform.runLater(() -> {
                            if (!songs.isEmpty()) {
                                if (isShuffle) {
                                    // Random selection for shuffle mode
                                    int newIndex;
                                    do {
                                        newIndex = (int) (Math.random() * songs.size());
                                    } while (newIndex == currentSongIndex && songs.size() > 1);
                                    currentSongIndex = newIndex;
                                } else {
                                    // Normal sequential play
                                    currentSongIndex = (currentSongIndex + 1) % songs.size();
                                }
                                playlist.setSelectedIndex(currentSongIndex);
                                currentFile = songs.get(currentSongIndex);
                                currentFileName = currentFile.getName();
                                playMusic();
                            }
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error playing the file: " + e.getMessage());
            }
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && isPlaying) {
            Platform.runLater(() -> {
                mediaPlayer.pause();
                isPaused = true;
                isPlaying = false;
                pauseButton.setEnabled(false);
                resumeButton.setEnabled(true);
                currentSongLabel.setText("Paused: " + currentFileName);
            });
        }
    }

    private void resumeMusic() {
        if (mediaPlayer != null && isPaused) {
            Platform.runLater(() -> {
                mediaPlayer.play();
                isPlaying = true;
                isPaused = false;
                resumeButton.setEnabled(false);
                pauseButton.setEnabled(true);
                currentSongLabel.setText("Now Playing: " + currentFileName);
            });
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> {
                // Save the current position before stopping
                lastPosition = mediaPlayer.getCurrentTime();
                mediaPlayer.stop();
                isPlaying = false;
                isPaused = false;
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                resumeButton.setEnabled(false);
                stopButton.setEnabled(false);
                progressBar.setValue(0);
                songProgressBar.setValue(0);
                timeLabel.setText("00:00 / 00:00");
                currentSongLabel.setText("Stopped");
            });
        }
    }

    private void updateTimeLabel(Duration current, Duration total) {
        SwingUtilities.invokeLater(() -> {
            String currentTime = formatDuration(current);
            String totalTime = formatDuration(total);
            timeLabel.setText(currentTime + " / " + totalTime);
        });
    }

    private String formatDuration(Duration duration) {
        int seconds = (int) Math.floor(duration.toSeconds());
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source.equals(addButton)) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            fileChooser.setDialogTitle("Select Music Files or Folder");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(
                    new FileNameExtensionFilter("Audio Files", "mp3", "wav", "m4a", "aac", "wma", "ogg"));

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                for (File file : selectedFiles) {
                    if (file.isDirectory()) {
                        addMusicFromDirectory(file);
                    } else if (isAudioFile(file)) {
                        addSongToPlaylist(file);
                    }
                }

                if (!songs.isEmpty() && playlist.getSelectedIndex() == -1) {
                    playlist.setSelectedIndex(0);
                    currentFile = songs.get(0);
                    currentFileName = currentFile.getName();
                    currentSongLabel.setText("Selected: " + currentFileName);
                }
            }
        } else if (source.equals(playButton)) {
            if (currentFile != null && !isPlaying) {
                playMusic();
            }
        } else if (source.equals(pauseButton)) {
            pauseMusic();
        } else if (source.equals(resumeButton)) {
            resumeMusic();
        } else if (source.equals(stopButton)) {
            stopMusic();
        } else if (source.equals(nextButton)) {
            if (!songs.isEmpty()) {
                if (isShuffle) {
                    // Random selection for shuffle mode
                    int newIndex;
                    do {
                        newIndex = (int) (Math.random() * songs.size());
                    } while (newIndex == currentSongIndex && songs.size() > 1);
                    currentSongIndex = newIndex;
                } else {
                    // Normal sequential play
                    currentSongIndex = (currentSongIndex + 1) % songs.size();
                }
                playlist.setSelectedIndex(currentSongIndex);
                currentFile = songs.get(currentSongIndex);
                currentFileName = currentFile.getName();
                playMusic();
            }
        } else if (source.equals(prevButton)) {
            if (!songs.isEmpty()) {
                if (isShuffle) {
                    // Random selection for shuffle mode
                    int newIndex;
                    do {
                        newIndex = (int) (Math.random() * songs.size());
                    } while (newIndex == currentSongIndex && songs.size() > 1);
                    currentSongIndex = newIndex;
                } else {
                    // Normal sequential play
                    currentSongIndex = (currentSongIndex - 1 + songs.size()) % songs.size();
                }
                playlist.setSelectedIndex(currentSongIndex);
                currentFile = songs.get(currentSongIndex);
                currentFileName = currentFile.getName();
                playMusic();
            }
        } else if (source.equals(shuffleButton)) {
            isShuffle = !isShuffle;
            shuffleButton.setText(isShuffle ? "Shuffle On" : "Shuffle Off");
            if (isShuffle) {
                shuffleButton.setBackground(new Color(255, 99, 71)); // Tomato red for ON state
            } else {
                shuffleButton.setBackground(new Color(30, 144, 255)); // Dodger Blue for OFF state
            }
        } else if (source.equals(repeatButton)) {
            isRepeat = !isRepeat;
            repeatButton.setText(isRepeat ? "Repeat On" : "Repeat Off");
            if (isRepeat) {
                repeatButton.setBackground(new Color(255, 99, 71)); // Tomato red for ON state
            } else {
                repeatButton.setBackground(new Color(30, 144, 255)); // Dodger Blue for OFF state
            }
        }
    }

    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        return button;
    }

    private void addMusicFromDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addMusicFromDirectory(file);
                } else if (isAudioFile(file)) {
                    addSongToPlaylist(file);
                }
            }
        }
    }

    private void addSongToPlaylist(File file) {
        if (!songs.contains(file)) {
            songs.add(file);
            playlistModel.addElement(file.getName());
            System.out.println("Added file: " + file.getAbsolutePath());

            // Add to folder structure
            String folderPath = file.getParentFile().getAbsolutePath();
            folderSongs.computeIfAbsent(folderPath, k -> new ArrayList<>()).add(file);

            // Update tree structure
            String parentPath = file.getParentFile().getAbsolutePath();
            DefaultMutableTreeNode parentNode = findOrCreateNode(rootNode, parentPath);
            treeModel.reload();
        }
    }

    private DefaultMutableTreeNode findOrCreateNode(DefaultMutableTreeNode root, String path) {
        String[] parts = path.split("\\\\");
        DefaultMutableTreeNode current = root;

        for (String part : parts) {
            DefaultMutableTreeNode node = findChildNode(current, part);
            if (node == null) {
                node = new DefaultMutableTreeNode(part);
                current.add(node);
            }
            current = node;
        }

        return current;
    }

    private DefaultMutableTreeNode findChildNode(DefaultMutableTreeNode parent, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (node.getUserObject().toString().equals(name)) {
                return node;
            }
        }
        return null;
    }

    private void addMusicFromFolder(File folder) {
        if (folder == null || !folder.isDirectory()) {
            return;
        }

        // Create a node for this folder
        DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folder.getName());
        rootNode.add(folderNode);

        // Get all files in the folder and subfolders
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursively add music from subfolders
                    addMusicFromFolder(file);
                } else {
                    // Check if the file is a music file
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") ||
                            fileName.endsWith(".m4a") || fileName.endsWith(".aac") ||
                            fileName.endsWith(".wma") || fileName.endsWith(".ogg")) {

                        // Add to playlist and tree
                        songs.add(file);
                        playlistModel.addElement(file.getName());
                        DefaultMutableTreeNode musicNode = new DefaultMutableTreeNode(file.getName());
                        folderNode.add(musicNode);
                    }
                }
            }
        }

        // Update the tree
        treeModel.reload();
        // Expand the root node
        for (int i = 0; i < folderTree.getRowCount(); i++) {
            folderTree.expandRow(i);
        }
    }

    public static void main(String[] args) {
        // Initialize JavaFX Platform
        Platform.setImplicitExit(false);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MusicPlayer().setVisible(true);
        });
    }

    private boolean isAudioFile(File file) {
        if (!file.isFile())
            return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".wav") ||
                name.endsWith(".m4a") || name.endsWith(".aac") ||
                name.endsWith(".wma") || name.endsWith(".ogg");
    }

    private void showPlaylistContextMenu(MouseEvent e) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem addSongs = new JMenuItem("Add Songs");
        JMenuItem removeSong = new JMenuItem("Remove Selected Song");
        JMenuItem moveUp = new JMenuItem("Move Up");
        JMenuItem moveDown = new JMenuItem("Move Down");
        JMenuItem addToFavorites = new JMenuItem("Add to Favorites");
        JMenuItem addToMostPlayed = new JMenuItem("Add to Most Played");
        JMenuItem showFavorites = new JMenuItem("Show Favorites");

        // Add Songs action
        addSongs.addActionListener(event -> {
            String currentPlaylist = (String) playlistComboBox.getSelectedItem();
            if (currentPlaylist != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Music Files", "mp3", "wav", "m4a", "aac", "wma", "ogg"));

                int result = fileChooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File[] selectedFiles = fileChooser.getSelectedFiles();
                    for (File file : selectedFiles) {
                        if (!songs.contains(file)) {
                            songs.add(file);
                            playlistModel.addElement(file.getName());
                        }
                    }
                    if (!currentPlaylist.equals("Default Playlist")) {
                        savedPlaylists.put(currentPlaylist, new ArrayList<>(songs));
                    }
                }
            }
        });

        // Remove Song action
        removeSong.addActionListener(event -> {
            int selectedIndex = playlist.getSelectedIndex();
            if (selectedIndex != -1) {
                songs.remove(selectedIndex);
                playlistModel.remove(selectedIndex);
                String currentPlaylist = (String) playlistComboBox.getSelectedItem();
                if (!currentPlaylist.equals("Default Playlist")) {
                    savedPlaylists.put(currentPlaylist, new ArrayList<>(songs));
                }
            }
        });

        // Move Up action
        moveUp.addActionListener(event -> {
            int selectedIndex = playlist.getSelectedIndex();
            if (selectedIndex > 0) {
                // Swap in the model
                String temp = playlistModel.getElementAt(selectedIndex);
                playlistModel.setElementAt(playlistModel.getElementAt(selectedIndex - 1), selectedIndex);
                playlistModel.setElementAt(temp, selectedIndex - 1);

                // Swap in the songs list
                File tempFile = songs.get(selectedIndex);
                songs.set(selectedIndex, songs.get(selectedIndex - 1));
                songs.set(selectedIndex - 1, tempFile);

                // Update selection
                playlist.setSelectedIndex(selectedIndex - 1);

                // Update saved playlist
                String currentPlaylist = (String) playlistComboBox.getSelectedItem();
                if (!currentPlaylist.equals("Default Playlist")) {
                    savedPlaylists.put(currentPlaylist, new ArrayList<>(songs));
                }
            }
        });

        // Move Down action
        moveDown.addActionListener(event -> {
            int selectedIndex = playlist.getSelectedIndex();
            if (selectedIndex != -1 && selectedIndex < playlistModel.getSize() - 1) {
                // Swap in the model
                String temp = playlistModel.getElementAt(selectedIndex);
                playlistModel.setElementAt(playlistModel.getElementAt(selectedIndex + 1), selectedIndex);
                playlistModel.setElementAt(temp, selectedIndex + 1);

                // Swap in the songs list
                File tempFile = songs.get(selectedIndex);
                songs.set(selectedIndex, songs.get(selectedIndex + 1));
                songs.set(selectedIndex + 1, tempFile);

                // Update selection
                playlist.setSelectedIndex(selectedIndex + 1);

                // Update saved playlist
                String currentPlaylist = (String) playlistComboBox.getSelectedItem();
                if (!currentPlaylist.equals("Default Playlist")) {
                    savedPlaylists.put(currentPlaylist, new ArrayList<>(songs));
                }
            }
        });

        // Add to Favorites action
        addToFavorites.addActionListener(event -> {
            int selectedIndex = playlist.getSelectedIndex();
            if (selectedIndex != -1) {
                File selectedSong = songs.get(selectedIndex);
                if (!favorites.contains(selectedSong)) {
                    favorites.add(selectedSong);
                    favoritesModel.addElement(selectedSong.getName());
                    System.out.println("Added to Favorites: " + selectedSong.getName());
                }
            }
        });

        // Add to Most Played action
        addToMostPlayed.addActionListener(event -> {
            int selectedIndex = playlist.getSelectedIndex();
            if (selectedIndex != -1) {
                File selectedSong = songs.get(selectedIndex);
                if (!mostPlayed.contains(selectedSong)) {
                    mostPlayed.add(selectedSong);
                    System.out.println("Added to Most Played: " + selectedSong.getName());
                }
            }
        });

        // Show Favorites action
        showFavorites.addActionListener(event -> {
            System.out.println("Show Favorites clicked"); // Debugging output
            JOptionPane.showMessageDialog(this, favoritesList, "Favorite Songs", JOptionPane.PLAIN_MESSAGE);
        });

        contextMenu.add(addSongs);
        contextMenu.add(removeSong);
        contextMenu.addSeparator();
        contextMenu.add(moveUp);
        contextMenu.add(moveDown);
        contextMenu.addSeparator();
        contextMenu.add(addToFavorites);
        contextMenu.add(addToMostPlayed);
        contextMenu.addSeparator();
        contextMenu.add(showFavorites);

        contextMenu.show(playlist, e.getX(), e.getY());
    }
}
