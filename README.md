# Modern Music Player

A sleek and modern music player built with Java Swing that supports MP3 and WAV files.

## Features

- Modern blue-grey UI with cyan accents
- Play, pause, and stop functionality
- Next and previous track controls
- Shuffle and repeat modes
- Progress bar with time display
- Playlist management
- Support for MP3 and WAV files

## Requirements

- Java 11 or higher
- Maven

## Building and Running

1. Clone this repository
2. Navigate to the project directory
3. Build the project:
   ```
   mvn clean package
   ```
4. Run the application:
   ```
   java -jar target/musicplayer-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

## Usage

1. Click the "Add Songs" button to add MP3 or WAV files to your playlist
2. Select a song from the playlist to play it
3. Use the control buttons to:
   - Play/Pause/Stop the current song
   - Skip to next or previous song
   - Toggle shuffle mode (red when active)
   - Toggle repeat mode (red when active)
4. The progress bar shows the current position in the song
5. The time display shows the current and total time of the song

## Controls

- ▶ - Play
- ⏸ - Pause
- ⏹ - Stop
- ⏭ - Next track
- ⏮ - Previous track
- 🔀 - Toggle shuffle
- 🔁 - Toggle repeat
"# musicplayer" 
