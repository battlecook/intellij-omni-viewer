package com.omniviewer.audio;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioEditorComponent extends JPanel {
    
    private final VirtualFile file;
    private Clip audioClip;
    private AudioInputStream audioStream;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    
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
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            statusLabel.setText("Error loading audio: " + e.getMessage());
            playPauseButton.setEnabled(false);
            e.printStackTrace();
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
            } else {
                formatLabel.setText(formatName);
            }
        }
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
                    // Start from beginning
                    audioClip.setFramePosition(0);
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
        }
    }
    
    public void dispose() {
        stop();
        progressTimer.stop();
        
        if (audioClip != null) {
            audioClip.close();
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
