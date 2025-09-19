package com.omniviewer.audio;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import javazoom.jl.player.Player;
import javazoom.jl.decoder.JavaLayerException;
import org.tritonus.share.sampled.TAudioFormat;
import org.tritonus.share.sampled.file.TAudioFileFormat;

public class AudioEditorComponent extends JPanel {
    
    private final VirtualFile file;
    private Clip audioClip;
    private AudioInputStream audioStream;
    private Player mp3Player;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private boolean isMP3File = false;
    
    // UI Components
    private JButton playPauseButton;
    private JButton stopButton;
    private WaveformComponent waveformComponent;
    private JBLabel statusLabel;
    private JBLabel timeLabel;
    private Timer progressTimer;
    
    // Metadata labels
    private JBLabel durationLabel;
    private JBLabel sampleRateLabel;
    private JBLabel channelsLabel;
    private JBLabel bitDepthLabel;
    private JBLabel fileSizeLabel;
    private JBLabel formatLabel;
    
    public AudioEditorComponent(VirtualFile file) {
        this.file = file;
        initializeUI();
        loadAudioFile();
    }
    
    static {
        // Register MP3SPI providers
        try {
            System.out.println("Registering MP3SPI providers...");
            
            // Force load MP3SPI classes to trigger automatic registration
            Class.forName("org.tritonus.share.sampled.file.TAudioFileReader");
            Class.forName("org.tritonus.share.sampled.TAudioFormat");
            
            // Check if MP3 is now supported
            AudioFileFormat.Type[] supportedTypes = AudioSystem.getAudioFileTypes();
            boolean mp3Supported = false;
            for (AudioFileFormat.Type type : supportedTypes) {
                if (type.toString().toLowerCase().contains("mp3")) {
                    mp3Supported = true;
                    break;
                }
            }
            
            if (mp3Supported) {
                System.out.println("MP3SPI providers registered successfully - MP3 support detected");
            } else {
                System.out.println("MP3SPI providers loaded but MP3 support not detected");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to register MP3SPI providers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));
        
        // File info panel (top)
        JPanel fileInfoPanel = new JPanel(new BorderLayout());
        JBLabel fileNameLabel = new JBLabel("File: " + file.getName());
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD));
        fileInfoPanel.add(fileNameLabel, BorderLayout.NORTH);
        
        // Status and time labels
        JPanel infoPanel = new JPanel(new BorderLayout());
        statusLabel = new JBLabel("Ready to play");
        timeLabel = new JBLabel("00:00 / 00:00");
        infoPanel.add(statusLabel, BorderLayout.WEST);
        infoPanel.add(timeLabel, BorderLayout.EAST);
        fileInfoPanel.add(infoPanel, BorderLayout.SOUTH);
        
        // Main content panel (center) - metadata, buttons and waveform together
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        
        // Audio metadata panel
        JPanel metadataPanel = createMetadataPanel();
        mainContentPanel.add(metadataPanel, BorderLayout.NORTH);
        
        // Center panel for buttons and waveform
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        // Control panel - buttons only
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        // Play/Pause button (toggle)
        playPauseButton = new JButton("▶ Play");
        playPauseButton.addActionListener(new PlayPauseActionListener());
        controlPanel.add(playPauseButton);
        
        // Stop button
        stopButton = new JButton("⏹ Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new StopActionListener());
        controlPanel.add(stopButton);
        
        // Waveform component - right below buttons (increased size for timeline)
        waveformComponent = new WaveformComponent();
        waveformComponent.addSeekListener(this::onSeek);
        waveformComponent.setPreferredSize(new Dimension(300, 80));
        waveformComponent.setMinimumSize(new Dimension(200, 40));
        
        // Add buttons and waveform to center panel
        centerPanel.add(controlPanel, BorderLayout.NORTH);
        centerPanel.add(waveformComponent, BorderLayout.CENTER);
        
        // Add center panel to main content panel
        mainContentPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Layout arrangement
        add(fileInfoPanel, BorderLayout.NORTH);
        add(mainContentPanel, BorderLayout.CENTER);
        
        // Initialize progress timer
        progressTimer = new Timer(100, e -> updateProgress());
    }
    
    private JPanel createMetadataPanel() {
        JPanel metadataPanel = new JPanel(new GridLayout(2, 3, 15, 10));
        metadataPanel.setBorder(JBUI.Borders.empty(15));
        metadataPanel.setBackground(new Color(60, 60, 60));
        metadataPanel.setPreferredSize(new Dimension(600, 80));
        metadataPanel.setMinimumSize(new Dimension(400, 60));
        
        // Create metadata labels
        durationLabel = addMetadataItem(metadataPanel, "Duration:", "Loading...");
        sampleRateLabel = addMetadataItem(metadataPanel, "Sample Rate:", "Loading...");
        channelsLabel = addMetadataItem(metadataPanel, "Channels:", "Loading...");
        bitDepthLabel = addMetadataItem(metadataPanel, "Bit Depth:", "Loading...");
        fileSizeLabel = addMetadataItem(metadataPanel, "File Size:", "Loading...");
        formatLabel = addMetadataItem(metadataPanel, "Format:", "Loading...");
        
        return metadataPanel;
    }
    
    private JBLabel addMetadataItem(JPanel panel, String label, String value) {
        JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        itemPanel.setBackground(new Color(60, 60, 60));
        itemPanel.setBorder(JBUI.Borders.empty(5));
        itemPanel.setPreferredSize(new Dimension(180, 25));
        
        JBLabel labelComponent = new JBLabel(label);
        labelComponent.setForeground(new Color(200, 200, 200));
        labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD, 11f));
        
        JBLabel valueComponent = new JBLabel(value);
        valueComponent.setForeground(new Color(255, 255, 255));
        valueComponent.setFont(valueComponent.getFont().deriveFont(11f));
        
        itemPanel.add(labelComponent);
        itemPanel.add(valueComponent);
        
        panel.add(itemPanel);
        
        return valueComponent;
    }
    
    private void loadAudioFile() {
        try {
            // Check file extension and provide helpful error message
            String fileName = file.getName().toLowerCase();
            String supportedFormats = "WAV, AU, AIFF, MP3";
            
            System.out.println("=== AudioEditorComponent.loadAudioFile() ===");
            System.out.println("File name: " + file.getName());
            System.out.println("File name (lowercase): " + fileName);
            System.out.println("File size: " + file.getLength() + " bytes");
            
            if (!fileName.endsWith(".wav") && !fileName.endsWith(".au") && 
                !fileName.endsWith(".aiff") && !fileName.endsWith(".mp3")) {
                System.out.println("Unsupported file format: " + fileName);
                statusLabel.setText("Unsupported format. Supported formats: " + supportedFormats);
                playPauseButton.setEnabled(false);
                updateMetadataWithError("Unsupported format");
                return;
            }
            
            // Special handling for MP3 files
            if (fileName.endsWith(".mp3")) {
                System.out.println("MP3 file detected, calling handleMP3File()");
                handleMP3File();
                return;
            }
            
            System.out.println("Standard audio file detected, processing with AudioSystem");
            
            InputStream inputStream = file.getInputStream();
            audioStream = AudioSystem.getAudioInputStream(inputStream);
            
            // Create a copy of the stream for waveform data extraction
            InputStream waveformInputStream = file.getInputStream();
            AudioInputStream waveformStream = AudioSystem.getAudioInputStream(waveformInputStream);
            
            // Set waveform data first (before opening the clip)
            waveformComponent.setWaveformData(waveformStream);
            
            // Now open the clip with the original stream
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);
            
            // Set audio duration for timeline
            if (audioClip.isOpen()) {
                long duration = audioClip.getMicrosecondLength();
                waveformComponent.setAudioDuration(duration);
            }
            
            statusLabel.setText("Audio loaded successfully");
            playPauseButton.setEnabled(true);
            
            // Update metadata
            updateMetadata();
            
        } catch (UnsupportedAudioFileException e) {
            String errorMsg = "Unsupported audio format. Please use WAV, AU, AIFF, or MP3 files.";
            statusLabel.setText(errorMsg);
            playPauseButton.setEnabled(false);
            updateMetadataWithError("Unsupported format");
            System.err.println("Audio format error for file: " + file.getName());
            System.err.println("Error details: " + e.getMessage());
        } catch (IOException e) {
            String errorMsg = "Error reading file: " + e.getMessage();
            statusLabel.setText(errorMsg);
            playPauseButton.setEnabled(false);
            updateMetadataWithError("File read error");
            System.err.println("IO error for file: " + file.getName());
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            String errorMsg = "Audio line unavailable: " + e.getMessage();
            statusLabel.setText(errorMsg);
            playPauseButton.setEnabled(false);
            updateMetadataWithError("Audio line error");
            System.err.println("Line unavailable error for file: " + file.getName());
            e.printStackTrace();
        }
    }
    
    private void handleMP3File() {
        try {
            System.out.println("=== handleMP3File() called ===");
            isMP3File = true;
            
            // Try to use Java Sound API with MP3SPI first
            System.out.println("Attempting to load MP3 with Java Sound API...");
            try {
                InputStream inputStream = file.getInputStream();
                audioStream = AudioSystem.getAudioInputStream(inputStream);
                
                // Create a copy of the stream for waveform data extraction
                InputStream waveformInputStream = file.getInputStream();
                AudioInputStream waveformStream = AudioSystem.getAudioInputStream(waveformInputStream);
                
                // Set waveform data first (before opening the clip)
                waveformComponent.setWaveformData(waveformStream);
                
                // Now open the clip with the original stream
                audioClip = AudioSystem.getClip();
                audioClip.open(audioStream);
                
                // Set audio duration for timeline
                if (audioClip.isOpen()) {
                    long duration = audioClip.getMicrosecondLength();
                    waveformComponent.setAudioDuration(duration);
                }
                
                System.out.println("MP3 loaded successfully with Java Sound API");
                statusLabel.setText("MP3 file loaded successfully");
                playPauseButton.setEnabled(true);
                
                // Update metadata
                updateMetadata();
                System.out.println("MP3 metadata updated successfully");
                return;
                
            } catch (UnsupportedAudioFileException e) {
                System.out.println("Java Sound API failed for MP3, falling back to JLayer: " + e.getMessage());
            }
            
            // Fallback to JLayer if Java Sound API fails
            System.out.println("Creating InputStream for MP3 file...");
            InputStream mp3InputStream = file.getInputStream();
            System.out.println("InputStream created successfully");
            
            System.out.println("Creating JLayer Player...");
            mp3Player = new Player(mp3InputStream);
            System.out.println("JLayer Player created successfully");
            
            // Generate real waveform data for MP3 (using separate stream)
            System.out.println("Generating real waveform data for MP3...");
            generateMP3WaveformData();
            System.out.println("Real waveform data generated");
            
            statusLabel.setText("MP3 file loaded successfully (JLayer)");
            playPauseButton.setEnabled(true);
            
            System.out.println("MP3 file loaded successfully, updating metadata...");
            // Update metadata with basic file information
            updateMP3Metadata();
            System.out.println("MP3 metadata updated successfully");
            
        } catch (JavaLayerException e) {
            System.err.println("=== JavaLayerException in handleMP3File() ===");
            System.err.println("Error loading MP3 file: " + file.getName());
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Error loading MP3 file: " + e.getMessage());
            playPauseButton.setEnabled(false);
            updateMetadataWithError("MP3 loading error");
        } catch (IOException e) {
            System.err.println("=== IOException in handleMP3File() ===");
            System.err.println("Error reading MP3 file: " + file.getName());
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Error reading MP3 file: " + e.getMessage());
            playPauseButton.setEnabled(false);
            updateMetadataWithError("MP3 file read error");
        } catch (Exception e) {
            System.err.println("=== Unexpected Exception in handleMP3File() ===");
            System.err.println("Error processing MP3 file: " + file.getName());
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Unexpected error processing MP3 file: " + e.getMessage());
            playPauseButton.setEnabled(false);
            updateMetadataWithError("MP3 processing error");
        }
    }
    
    private void updateMP3Metadata() {
        // File Size
        long fileSize = file.getLength();
        fileSizeLabel.setText(formatFileSize(fileSize));
        
        // Format
        formatLabel.setText("MP3");
        
        // Try to get MP3 metadata using improved estimation
        try {
            System.out.println("Attempting to get MP3 metadata...");
            
            // Get more accurate duration estimation
            long estimatedDuration = getMP3DurationWithJLayer();
            durationLabel.setText(formatTime(estimatedDuration * 1000000)); // Convert to microseconds
            
            // Estimate bitrate based on file size and duration
            long estimatedBitrate = (fileSize * 8) / estimatedDuration; // bits per second
            
            // Set more accurate metadata based on file characteristics
            if (estimatedBitrate < 64000) {
                sampleRateLabel.setText("22.05 kHz (est.)");
                channelsLabel.setText("1 (Mono, est.)");
                bitDepthLabel.setText("16 bit (est.)");
            } else if (estimatedBitrate < 128000) {
                sampleRateLabel.setText("44.1 kHz (est.)");
                channelsLabel.setText("1 (Mono, est.)");
                bitDepthLabel.setText("16 bit (est.)");
            } else if (estimatedBitrate < 192000) {
                sampleRateLabel.setText("44.1 kHz (est.)");
                channelsLabel.setText("2 (Stereo, est.)");
                bitDepthLabel.setText("16 bit (est.)");
            } else if (estimatedBitrate < 320000) {
                sampleRateLabel.setText("44.1 kHz (est.)");
                channelsLabel.setText("2 (Stereo, est.)");
                bitDepthLabel.setText("16 bit (est.)");
            } else {
                sampleRateLabel.setText("48 kHz (est.)");
                channelsLabel.setText("2 (Stereo, est.)");
                bitDepthLabel.setText("16 bit (est.)");
            }
            
            System.out.println("MP3 metadata estimated successfully - Duration: " + estimatedDuration + "s, Bitrate: " + estimatedBitrate + " bps");
            
        } catch (Exception e) {
            System.err.println("Error getting MP3 metadata: " + e.getMessage());
            // Fallback to N/A values
            durationLabel.setText("N/A (MP3)");
            sampleRateLabel.setText("N/A (MP3)");
            channelsLabel.setText("N/A (MP3)");
            bitDepthLabel.setText("N/A (MP3)");
        }
    }
    
    private long estimateMP3Duration(long fileSizeBytes) {
        // Rough estimation: MP3 files are typically compressed at ~128kbps
        // 128 kbps = 16 KB/s = 16,000 bytes/s
        // Duration = file size / bitrate
        long estimatedSeconds = fileSizeBytes / 16000;
        return Math.max(estimatedSeconds, 1); // At least 1 second
    }
    
    private void generateMP3WaveformData() {
        try {
            System.out.println("=== generateMP3WaveformData() called ===");
            
            // Try to get more accurate duration using JLayer
            long estimatedDuration = getMP3DurationWithJLayer();
            long durationMicroseconds = estimatedDuration * 1000000;
            
            System.out.println("Estimated MP3 duration: " + estimatedDuration + " seconds");
            
            // Set audio duration for timeline
            waveformComponent.setAudioDuration(durationMicroseconds);
            
            // Extract real waveform data from MP3 using JLayer
            System.out.println("Extracting real waveform data from MP3...");
            extractRealMP3WaveformData();
            
        } catch (Exception e) {
            System.err.println("Error generating MP3 waveform data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void extractRealMP3WaveformData() {
        try {
            // Create a new input stream for waveform extraction
            InputStream waveformStream = file.getInputStream();
            Player waveformPlayer = new Player(waveformStream);
            
            // We need to decode the MP3 to get PCM data for waveform
            // Since JLayer doesn't provide direct access to decoded samples,
            // we'll use a different approach - decode the MP3 and capture the audio data
            
            System.out.println("Decoding MP3 for waveform extraction...");
            
            // Create a custom player that captures audio data
            MP3WaveformExtractor extractor = new MP3WaveformExtractor();
            extractor.extractWaveform(file);
            
            // Set the extracted waveform data
            if (extractor.getWaveformData() != null && !extractor.getWaveformData().isEmpty()) {
                // Create AudioInputStream from extracted data
                AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100.0f,  // sample rate
                    16,        // sample size in bits
                    2,         // channels
                    4,         // frame size
                    44100.0f,  // frame rate
                    false      // little endian
                );
                
                // Convert extracted samples to byte array
                byte[] audioData = extractor.getAudioData();
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                AudioInputStream audioStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
                
                waveformComponent.setWaveformData(audioStream);
                System.out.println("Real MP3 waveform data extracted and set successfully");
            } else {
                System.out.println("Failed to extract waveform data, using fallback");
                generateFallbackWaveform();
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting real MP3 waveform: " + e.getMessage());
            e.printStackTrace();
            generateFallbackWaveform();
        }
    }
    
    private void generateFallbackWaveform() {
        try {
            System.out.println("Generating fallback waveform...");
            
            // Simple fallback - create a basic waveform pattern
            int numSamples = 1000;
            float[] waveform = new float[numSamples];
            
            for (int i = 0; i < numSamples; i++) {
                double time = (double) i / numSamples;
                // Simple pattern that's not random
                waveform[i] = (float) (0.3 + 0.4 * Math.sin(time * Math.PI * 4) + 0.3 * Math.sin(time * Math.PI * 8));
            }
            
            // Create AudioInputStream from fallback data
            AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,  // sample rate
                16,        // sample size in bits
                2,         // channels
                4,         // frame size
                44100.0f,  // frame rate
                false      // little endian
            );
            
            byte[] audioData = new byte[numSamples * 4];
            for (int i = 0; i < numSamples; i++) {
                short sample = (short) (waveform[i] * Short.MAX_VALUE);
                int byteIndex = i * 4;
                audioData[byteIndex] = (byte) (sample & 0xFF);
                audioData[byteIndex + 1] = (byte) ((sample >> 8) & 0xFF);
                audioData[byteIndex + 2] = (byte) (sample & 0xFF);
                audioData[byteIndex + 3] = (byte) ((sample >> 8) & 0xFF);
            }
            
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
            
            waveformComponent.setWaveformData(audioStream);
            System.out.println("Fallback waveform generated");
            
        } catch (Exception e) {
            System.err.println("Error generating fallback waveform: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private long getMP3DurationWithJLayer() {
        try {
            // Try to get more accurate duration using JLayer
            InputStream durationStream = file.getInputStream();
            Player durationPlayer = new Player(durationStream);
            
            // JLayer doesn't provide direct duration access, so we'll use a more sophisticated estimation
            long fileSize = file.getLength();
            
            // More accurate estimation based on common MP3 bitrates
            // Try to estimate based on file size and common bitrates
            long estimatedSeconds;
            
            if (fileSize < 1024 * 1024) { // Less than 1MB
                // Likely low bitrate or short file
                estimatedSeconds = fileSize / 8000; // ~64kbps
            } else if (fileSize < 5 * 1024 * 1024) { // Less than 5MB
                // Medium bitrate
                estimatedSeconds = fileSize / 16000; // ~128kbps
            } else if (fileSize < 10 * 1024 * 1024) { // Less than 10MB
                // Higher bitrate
                estimatedSeconds = fileSize / 20000; // ~160kbps
            } else {
                // Very high bitrate or long file
                estimatedSeconds = fileSize / 24000; // ~192kbps
            }
            
            // Ensure minimum duration
            return Math.max(estimatedSeconds, 1);
            
        } catch (Exception e) {
            System.err.println("Error getting MP3 duration: " + e.getMessage());
            // Fallback to simple estimation
            return estimateMP3Duration(file.getLength());
        }
    }
    
    private void updateMetadata() {
        if (audioClip != null && audioClip.isOpen()) {
            // Duration
            long duration = audioClip.getMicrosecondLength();
            durationLabel.setText(formatTime(duration));
            
            // Audio format information
            AudioFormat format = audioStream.getFormat();
            
            // Sample Rate
            sampleRateLabel.setText(String.format("%.0f Hz", format.getSampleRate()));
            
            // Channels
            int channels = format.getChannels();
            channelsLabel.setText(channels == 1 ? "1 (Mono)" : channels == 2 ? "2 (Stereo)" : String.valueOf(channels));
            
            // Bit Depth
            bitDepthLabel.setText(format.getSampleSizeInBits() + " bit");
            
            // File Size
            long fileSize = file.getLength();
            fileSizeLabel.setText(formatFileSize(fileSize));
            
            // Format
            String formatName = format.getEncoding().toString();
            if (formatName.equals("PCM_SIGNED")) {
                formatLabel.setText("WAVE");
            } else if (formatName.equals("MPEG1L3")) {
                formatLabel.setText("MP3");
            } else {
                formatLabel.setText(formatName);
            }
        }
    }
    
    private void updateMetadataWithError(String errorType) {
        durationLabel.setText("N/A");
        sampleRateLabel.setText("N/A");
        channelsLabel.setText("N/A");
        bitDepthLabel.setText("N/A");
        
        // File Size (this should still work)
        long fileSize = file.getLength();
        fileSizeLabel.setText(formatFileSize(fileSize));
        
        // Format
        formatLabel.setText(errorType);
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.0f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    private void updateProgress() {
        if (audioClip != null && audioClip.isOpen()) {
            long position = audioClip.getMicrosecondPosition();
            long length = audioClip.getMicrosecondLength();
            
            if (length > 0) {
                float progress = (float) position / length;
                waveformComponent.setProgress(progress);
                
                // Update time labels
                String currentTime = formatTime(position);
                String totalTime = formatTime(length);
                timeLabel.setText(currentTime + " / " + totalTime);
                
                // Check if audio has finished playing
                if (isPlaying.get() && !audioClip.isRunning() && position >= length - 1000000) { // 1 second tolerance
                    // Audio finished, reset to beginning and stop
                    audioClip.stop();
                    audioClip.setFramePosition(0);
                    isPlaying.set(false);
                    isPaused.set(false);
                    
                    playPauseButton.setText("▶ Play");
                    playPauseButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    statusLabel.setText("Finished");
                    waveformComponent.setProgress(0.0f);
                    timeLabel.setText("00:00 / " + totalTime);
                    progressTimer.stop();
                }
            }
        }
    }
    
    private String formatTime(long microseconds) {
        long seconds = microseconds / 1_000_000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    private class PlayPauseActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isMP3File) {
                handleMP3PlayPause();
            } else {
                handleStandardAudioPlayPause();
            }
        }
        
        private void handleMP3PlayPause() {
            System.out.println("=== handleMP3PlayPause() called ===");
            System.out.println("mp3Player is null: " + (mp3Player == null));
            System.out.println("isPlaying: " + isPlaying.get());
            System.out.println("isPaused: " + isPaused.get());
            
            if (mp3Player == null) {
                System.out.println("MP3 player is null, returning");
                return;
            }
            
            if (isPlaying.get()) {
                // Currently playing, so pause
                System.out.println("Pausing MP3 playback...");
                mp3Player.close();
                isPlaying.set(false);
                isPaused.set(true);
                
                playPauseButton.setText("▶ Play");
                stopButton.setEnabled(true);
                statusLabel.setText("Paused");
                progressTimer.stop();
                System.out.println("MP3 playback paused");
            } else {
                // Not playing, so play
                System.out.println("Starting MP3 playback...");
                try {
                    // Always create a fresh player for each playback to avoid stream issues
                    System.out.println("Creating fresh MP3 player for playback...");
                    InputStream mp3InputStream = file.getInputStream();
                    mp3Player = new Player(mp3InputStream);
                    isPaused.set(false);
                    System.out.println("Fresh MP3 player created for playback");
                    
                    // Start playing in a separate thread
                    System.out.println("Starting MP3 playback thread...");
                    new Thread(() -> {
                        try {
                            System.out.println("MP3 playback thread started, calling mp3Player.play()...");
                            mp3Player.play();
                            System.out.println("MP3 playback completed");
                            // When playback finishes
                            SwingUtilities.invokeLater(() -> {
                                isPlaying.set(false);
                                isPaused.set(false);
                                playPauseButton.setText("▶ Play");
                                stopButton.setEnabled(false);
                                statusLabel.setText("Finished");
                                progressTimer.stop();
                                System.out.println("MP3 playback finished, UI updated");
                            });
                        } catch (JavaLayerException ex) {
                            System.err.println("=== JavaLayerException in MP3 playback thread ===");
                            System.err.println("MP3 playback error: " + ex.getMessage());
                            ex.printStackTrace();
                            SwingUtilities.invokeLater(() -> {
                                statusLabel.setText("MP3 playback error: " + ex.getMessage());
                                isPlaying.set(false);
                                isPaused.set(false);
                                playPauseButton.setText("▶ Play");
                                stopButton.setEnabled(false);
                            });
                        }
                    }).start();
                    
                    isPlaying.set(true);
                    playPauseButton.setText("⏸ Pause");
                    stopButton.setEnabled(true);
                    statusLabel.setText("Playing MP3...");
                    progressTimer.start();
                    System.out.println("MP3 playback started successfully");
                    
                } catch (Exception ex) {
                    System.err.println("=== Exception in handleMP3PlayPause() ===");
                    System.err.println("Error starting MP3 playback: " + ex.getMessage());
                    ex.printStackTrace();
                    statusLabel.setText("Error starting MP3 playback: " + ex.getMessage());
                    isPlaying.set(false);
                    isPaused.set(false);
                    playPauseButton.setText("▶ Play");
                    stopButton.setEnabled(false);
                }
            }
        }
        
        private void handleStandardAudioPlayPause() {
            if (audioClip == null) return;
            
            if (isPlaying.get()) {
                // Currently playing, so pause
                audioClip.stop();
                isPlaying.set(false);
                isPaused.set(true);
                
                playPauseButton.setText("▶ Play");
                stopButton.setEnabled(true);
                statusLabel.setText("Paused");
                progressTimer.stop();
            } else {
                // Not playing, so play
                if (isPaused.get()) {
                    // Resume from pause
                    audioClip.start();
                    isPaused.set(false);
                } else {
                    // Start from current position (don't reset to beginning)
                    // The position is already set by the seek functionality
                    audioClip.start();
                }
                
                isPlaying.set(true);
                playPauseButton.setText("⏸ Pause");
                stopButton.setEnabled(true);
                statusLabel.setText("Playing...");
                progressTimer.start();
            }
        }
    }
    
    private class StopActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isMP3File) {
                handleMP3Stop();
            } else {
                handleStandardAudioStop();
            }
        }
        
        private void handleMP3Stop() {
            if (mp3Player != null) {
                mp3Player.close();
            }
            isPlaying.set(false);
            isPaused.set(false);
            
            playPauseButton.setText("▶ Play");
            playPauseButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Stopped");
            progressTimer.stop();
        }
        
        private void handleStandardAudioStop() {
            if (audioClip == null) return;
            
            audioClip.stop();
            audioClip.setFramePosition(0);
            isPlaying.set(false);
            isPaused.set(false);
            
            playPauseButton.setText("▶ Play");
            playPauseButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Stopped");
            waveformComponent.setProgress(0.0f);
            timeLabel.setText("00:00 / " + formatTime(audioClip.getMicrosecondLength()));
            progressTimer.stop();
        }
    }
    
    public void stop() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
        }
        progressTimer.stop();
    }
    
    private void onSeek(float progress) {
        if (audioClip != null && audioClip.isOpen()) {
            long totalFrames = audioClip.getFrameLength();
            long targetFrame = (long) (totalFrames * progress);
            audioClip.setFramePosition((int) targetFrame);
            
            // Update the progress display immediately
            long position = audioClip.getMicrosecondPosition();
            long length = audioClip.getMicrosecondLength();
            if (length > 0) {
                String currentTime = formatTime(position);
                String totalTime = formatTime(length);
                timeLabel.setText(currentTime + " / " + totalTime);
            }
        }
    }
    
    public void dispose() {
        stop();
        progressTimer.stop();
        
        if (audioClip != null) {
            audioClip.close();
        }
        
        if (mp3Player != null) {
            mp3Player.close();
        }
        
        try {
            if (audioStream != null) {
                audioStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
