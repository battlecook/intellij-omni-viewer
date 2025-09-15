# IntelliJ Omni Viewer Plugin

A powerful IntelliJ plugin for viewing and editing various file formats including audio, video, images, CSV, and JSONL files.

## Features

### Audio Viewer (v1.0.0)
- **Play/Pause/Stop Controls**: Full audio playback control
- **Progress Bar**: Visual progress indicator with time display
- **Multiple Format Support**: MP3, WAV, OGG, FLAC, M4A, AAC, WMA
- **Real-time Updates**: Live progress tracking and time display

### Planned Features
- Video Viewer with playback controls
- Image Viewer with zoom and pan
- CSV Viewer with table editing
- JSONL Viewer with syntax highlighting

## Installation

1. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```

2. Install in IntelliJ IDEA:
   - Go to `File` → `Settings` → `Plugins`
   - Click the gear icon → `Install Plugin from Disk`
   - Select the generated `.zip` file from `build/distributions/`

## Development

### Prerequisites
- IntelliJ IDEA 2023.2 or later
- JDK 17 or later
- Gradle 7.0 or later

### Building
```bash
./gradlew buildPlugin
```

### Running in Development
```bash
./gradlew runIde
```

### Testing
```bash
./gradlew test
```

## Usage

1. Open any supported audio file in IntelliJ IDEA
2. The file will automatically open in the Audio Viewer
3. Use the play/pause/stop controls to control playback
4. Monitor progress with the progress bar and time display

## Supported File Types

### Audio Files
- MP3 (.mp3)
- WAV (.wav)
- OGG (.ogg)
- FLAC (.flac)
- M4A (.m4a)
- AAC (.aac)
- WMA (.wma)

## Technical Details

- Built with IntelliJ Platform SDK
- Uses Java Sound API for audio playback
- Swing-based UI components
- Thread-safe audio controls

## License

This project is licensed under the MIT License.
