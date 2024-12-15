import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.Arrays;

public class MusicPlayer extends PlaybackListener {
    private static final Object playSignal = new Object(); // For thread synchronization
    private MusicPlayerGUI musicPlayerGUI;                 // GUI reference
    private Song currentSong;                              // Currently loaded song
    private AdvancedPlayer advancedPlayer;                 // For playback
    private boolean isPaused;                              // Playback state
    private boolean songFinished;                          // Track if song is finished
    private int currentFrame;                              // Current frame of the song
    private int currentTimeInMilli;                        // Playback time in milliseconds
    private File currentDirectory;                         // Directory where the music files are located
    private File[] audioFiles;                             // List of audio files in the directory

    // Constructor
    public MusicPlayer(MusicPlayerGUI musicPlayerGUI) {
        this.musicPlayerGUI = musicPlayerGUI;
        this.currentDirectory = new File("src/assets");
        loadAudioFiles();
    }

    // Getters and Setters
    public Song getCurrentSong() {
        return currentSong;
    }

    public void setCurrentFrame(int frame) {
        this.currentFrame = frame;
    }

    public void setCurrentTimeInMilli(int timeInMilli) {
        this.currentTimeInMilli = timeInMilli;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public boolean isPlaying() {
        return advancedPlayer != null && !isPaused && !songFinished;
    }

    // Load and play a song
    public void loadSong(Song song) {
        currentSong = song;

        // Stop the current song if playing
        if (advancedPlayer != null) {
            stopSong();
        }

        if (currentSong != null) {
            currentFrame = 0;
            currentTimeInMilli = 0;
            musicPlayerGUI.setPlaybackSliderValue(0);
            musicPlayerGUI.updateSongTitleAndArtist(currentSong);
            playCurrentSong();
        }
    }

    private void startMusicThread() {
        new Thread(() -> {
            try {
                // If the song is paused, wait for a signal to continue playing
                if (isPaused) {
                    synchronized (playSignal) {
                        isPaused = false;  // Set the paused flag to false when the song is resumed
                        playSignal.notify();  // Notify the waiting thread to continue
                    }
                    advancedPlayer.play(currentFrame, Integer.MAX_VALUE);  // Resume from the current frame
                } else {
                    advancedPlayer.play();  // Start playing from the beginning
                }
            } catch (Exception e) {
                System.err.println("Error in playback thread: " + e.getMessage());
            }
        }).start();
    }

    private void startPlaybackSliderThread() {
        new Thread(() -> {
            while (!isPaused && !songFinished) {
                try {
                    // Increment playback time in milliseconds
                    currentTimeInMilli++;

                    // Calculate the corresponding frame based on playback time and frame rate
                    double frameRate = currentSong.getFrameRatePerMilliseconds();
                    int calculatedFrame = (int) ((double) currentTimeInMilli * frameRate);

                    // Update the GUI slider with the calculated frame
                    musicPlayerGUI.setPlaybackSliderValue(calculatedFrame);

                    // Sleep for a short time to avoid maxing out CPU usage
                    Thread.sleep(1);
                } catch (Exception e) {
                    System.err.println("Error in slider thread: " + e.getMessage());
                }
            }
        }).start();
    }


    public void playCurrentSong() {
        if (currentSong == null) return;

        try {
            FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            advancedPlayer = new AdvancedPlayer(bufferedInputStream);
            advancedPlayer.setPlayBackListener(this);

            startMusicThread();
            startPlaybackSliderThread();

        } catch (Exception e) {
            System.err.println("Error playing song: " + e.getMessage());
        }
    }

    public void pauseSong() {
        if (advancedPlayer != null) {
            isPaused = true;
            stopSong();
        }
    }

    public void stopSong() {
        if (advancedPlayer != null) {
            advancedPlayer.close();
            advancedPlayer = null;
        }
    }

    // Method to load audio files in the current directory
    private void loadAudioFiles() {
        // Fetch all files in the directory
        audioFiles = currentDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".wav"));
        if (audioFiles != null && audioFiles.length > 0) {
            // Sort files alphabetically to play in order
            Arrays.sort(audioFiles);
        }
    }

    // Next song functionality
    public void nextSong() {
        if (audioFiles == null || audioFiles.length == 0) {
            System.out.println("No songs found in the directory.");
            return;
        }

        // Find the current song in the list
        for (int i = 0; i < audioFiles.length; i++) {
            if (audioFiles[i].getAbsolutePath().equals(currentSong.getFilePath())) {
                // Play the next song in the list, if available
                if (i < audioFiles.length - 1) {
                    File nextFile = audioFiles[i + 1];
                    Song nextSong = new Song(nextFile.getAbsolutePath()); // Correcting instantiation
                    loadSong(nextSong);
                } else {
                    System.out.println("No more songs in the folder.");
                }
                return;
            }
        }
        System.out.println("Current song not found in the list.");
    }

    // Previous song functionality
    public void prevSong() {
        if (audioFiles == null || audioFiles.length == 0) {
            System.out.println("No songs found in the directory.");
            return;
        }

        // Find the current song in the list
        for (int i = 0; i < audioFiles.length; i++) {
            if (audioFiles[i].getAbsolutePath().equals(currentSong.getFilePath())) {
                // Play the previous song in the list, if available
                if (i > 0) {
                    File prevFile = audioFiles[i - 1];
                    Song prevSong = new Song(prevFile.getAbsolutePath()); // Correcting instantiation
                    loadSong(prevSong);
                } else {
                    System.out.println("No previous song in the folder.");
                }
                return;
            }
        }

    }
}