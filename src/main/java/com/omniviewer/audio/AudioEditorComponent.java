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
    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JProgressBar progressBar;
    private JBLabel statusLabel;
    private JBLabel timeLabel;
    private Timer progressTimer;
    
    public AudioEditorComponent(VirtualFile file) {
        this.file = file;
        initializeUI();
        loadAudioFile();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));
        
        // Create control panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        // Play button
        playButton = new JButton("▶ Play");
        playButton.addActionListener(new PlayActionListener());
        controlPanel.add(playButton);
        
        // Pause button
        pauseButton = new JButton("⏸ Pause");
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(new PauseActionListener());
        controlPanel.add(pauseButton);
        
        // Stop button
        stopButton = new JButton("⏹ Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new StopActionListener());
        controlPanel.add(stopButton);
        
        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(300, 20));
        controlPanel.add(progressBar);
        
        // Status and time labels
        JPanel infoPanel = new JPanel(new BorderLayout());
        statusLabel = new JBLabel("Ready to play");
        timeLabel = new JBLabel("00:00 / 00:00");
        infoPanel.add(statusLabel, BorderLayout.WEST);
        infoPanel.add(timeLabel, BorderLayout.EAST);
        
        // File info panel
        JPanel fileInfoPanel = new JPanel(new BorderLayout());
        JBLabel fileNameLabel = new JBLabel("File: " + file.getName());
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD));
        fileInfoPanel.add(fileNameLabel, BorderLayout.NORTH);
        fileInfoPanel.add(infoPanel, BorderLayout.SOUTH);
        
        add(fileInfoPanel, BorderLayout.NORTH);
        add(controlPanel, BorderLayout.CENTER);
        
        // Initialize progress timer
        progressTimer = new Timer(100, e -> updateProgress());
    }
    
    private void loadAudioFile() {
        try {
            InputStream inputStream = file.getInputStream();
            audioStream = AudioSystem.getAudioInputStream(inputStream);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);
            
            statusLabel.setText("Audio loaded successfully");
            playButton.setEnabled(true);
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            statusLabel.setText("Error loading audio: " + e.getMessage());
            playButton.setEnabled(false);
            e.printStackTrace();
        }
    }
    
    private void updateProgress() {
        if (audioClip != null && audioClip.isOpen()) {
            long position = audioClip.getMicrosecondPosition();
            long length = audioClip.getMicrosecondLength();
            
            if (length > 0) {
                int progress = (int) ((position * 100) / length);
                progressBar.setValue(progress);
                
                // Update time labels
                String currentTime = formatTime(position);
                String totalTime = formatTime(length);
                timeLabel.setText(currentTime + " / " + totalTime);
            }
        }
    }
    
    private String formatTime(long microseconds) {
        long seconds = microseconds / 1_000_000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    private class PlayActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (audioClip == null) return;
            
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
            playButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
            statusLabel.setText("Playing...");
            progressTimer.start();
        }
    }
    
    private class PauseActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (audioClip == null || !isPlaying.get()) return;
            
            audioClip.stop();
            isPlaying.set(false);
            isPaused.set(true);
            
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            statusLabel.setText("Paused");
            progressTimer.stop();
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
            
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            statusLabel.setText("Stopped");
            progressBar.setValue(0);
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
