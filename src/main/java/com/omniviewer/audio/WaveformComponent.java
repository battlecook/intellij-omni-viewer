package com.omniviewer.audio;

import com.intellij.util.ui.JBUI;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WaveformComponent extends JPanel {
    
    private List<Float> waveformData;
    private float progress = 0.0f;
    private int hoverPosition = -1;
    private boolean isHovering = false;
    
    // Colors
    private static final Color WAVEFORM_COLOR = new Color(100, 150, 255);
    private static final Color PROGRESS_COLOR = new Color(255, 100, 100);
    private static final Color BACKGROUND_COLOR = new Color(240, 240, 240);
    private static final Color HOVER_COLOR = new Color(255, 200, 100);
    
    public WaveformComponent() {
        setPreferredSize(new Dimension(300, 60));
        setMinimumSize(new Dimension(200, 40));
        setBorder(JBUI.Borders.empty(10));
        
        // Add mouse listener for seeking
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (waveformData != null && !waveformData.isEmpty()) {
                    float newProgress = (float) e.getX() / getWidth();
                    newProgress = Math.max(0.0f, Math.min(1.0f, newProgress));
                    setProgress(newProgress);
                    fireSeekEvent(newProgress);
                }
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (waveformData != null && !waveformData.isEmpty()) {
                    hoverPosition = e.getX();
                    isHovering = true;
                    repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                hoverPosition = -1;
                repaint();
            }
        });
    }
    
    public void setWaveformData(AudioInputStream audioStream) {
        try {
            waveformData = extractWaveformData(audioStream);
            repaint();
        } catch (IOException e) {
            e.printStackTrace();
            waveformData = new ArrayList<>();
        }
    }
    
    public void setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        repaint();
    }
    
    public float getProgress() {
        return progress;
    }
    
    private List<Float> extractWaveformData(AudioInputStream audioStream) throws IOException {
        List<Float> data = new ArrayList<>();
        AudioFormat format = audioStream.getFormat();
        
        // Calculate how many samples we need for visualization
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
        
        // We want about 1000 points for the waveform
        int targetPoints = 1000;
        long totalFrames = audioStream.getFrameLength();
        int framesPerPoint = (int) (totalFrames / targetPoints);
        
        if (framesPerPoint < 1) framesPerPoint = 1;
        
        byte[] buffer = new byte[framesPerPoint * channels * sampleSizeInBytes];
        int bytesRead;
        
        while ((bytesRead = audioStream.read(buffer)) > 0) {
            float sample = 0.0f;
            int samplesProcessed = 0;
            
            // Process the buffer
            for (int i = 0; i < bytesRead; i += sampleSizeInBytes * channels) {
                if (i + sampleSizeInBytes <= bytesRead) {
                    float channelSample = 0.0f;
                    
                    long sampleValue = 0;
                    if (format.isBigEndian()) {
                        for (int j = 0; j < sampleSizeInBytes; j++) {
                            sampleValue = (sampleValue << 8) | (buffer[i + j] & 0xFF);
                        }
                    } else {
                        for (int j = sampleSizeInBytes - 1; j >= 0; j--) {
                            sampleValue = (sampleValue << 8) | (buffer[i + j] & 0xFF);
                        }
                    }
                    
                    // Convert to signed value
                    if (sampleSizeInBytes == 1) {
                        channelSample = (float) ((byte) sampleValue);
                    } else if (sampleSizeInBytes == 2) {
                        channelSample = (float) ((short) sampleValue);
                    } else if (sampleSizeInBytes == 4) {
                        channelSample = (float) ((int) sampleValue);
                    } else {
                        channelSample = (float) sampleValue;
                    }
                    
                    // Normalize based on sample size
                    if (sampleSizeInBytes == 1) {
                        channelSample = channelSample / 128.0f;
                    } else if (sampleSizeInBytes == 2) {
                        channelSample = channelSample / 32768.0f;
                    } else if (sampleSizeInBytes == 4) {
                        channelSample = channelSample / 2147483648.0f;
                    }
                    
                    sample += Math.abs(channelSample);
                    samplesProcessed++;
                }
            }
            
            if (samplesProcessed > 0) {
                data.add(sample / samplesProcessed);
            }
        }
        
        return data;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Clear background
        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRect(0, 0, width, height);
        
        if (waveformData == null || waveformData.isEmpty()) {
            // Draw placeholder text
            g2d.setColor(Color.GRAY);
            g2d.setFont(g2d.getFont().deriveFont(Font.ITALIC, 12f));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "No waveform data available";
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            g2d.drawString(text, (width - textWidth) / 2, (height + textHeight) / 2);
            g2d.dispose();
            return;
        }
        
        // Draw waveform
        drawWaveform(g2d, width, height);
        
        // Draw progress overlay
        drawProgressOverlay(g2d, width, height);
        
        // Draw hover indicator
        if (isHovering && hoverPosition >= 0) {
            drawHoverIndicator(g2d, width, height);
        }
        
        g2d.dispose();
    }
    
    private void drawWaveform(Graphics2D g2d, int width, int height) {
        if (waveformData.isEmpty()) return;
        
        g2d.setColor(WAVEFORM_COLOR);
        g2d.setStroke(new BasicStroke(1.0f));
        
        int centerY = height / 2;
        int dataPoints = waveformData.size();
        float xStep = (float) width / dataPoints;
        
        for (int i = 0; i < dataPoints; i++) {
            float amplitude = waveformData.get(i);
            int barHeight = (int) (amplitude * (height - 20)); // Leave some margin
            
            int x = (int) (i * xStep);
            int y = centerY - barHeight / 2;
            
            g2d.drawLine(x, y, x, y + barHeight);
        }
    }
    
    private void drawProgressOverlay(Graphics2D g2d, int width, int height) {
        int progressX = (int) (progress * width);
        
        // Draw progress background (darker area)
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillRect(0, 0, progressX, height);
        
        // Draw progress line
        g2d.setColor(PROGRESS_COLOR);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawLine(progressX, 0, progressX, height);
    }
    
    private void drawHoverIndicator(Graphics2D g2d, int width, int height) {
        g2d.setColor(HOVER_COLOR);
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
        g2d.drawLine(hoverPosition, 0, hoverPosition, height);
    }
    
    // Event handling for seeking
    private void fireSeekEvent(float progress) {
        // This will be handled by the parent component
        SwingUtilities.invokeLater(() -> {
            for (SeekListener listener : seekListeners) {
                listener.onSeek(progress);
            }
        });
    }
    
    // Seek listener interface
    public interface SeekListener {
        void onSeek(float progress);
    }
    
    private final List<SeekListener> seekListeners = new ArrayList<>();
    
    public void addSeekListener(SeekListener listener) {
        seekListeners.add(listener);
    }
    
    public void removeSeekListener(SeekListener listener) {
        seekListeners.remove(listener);
    }
}
