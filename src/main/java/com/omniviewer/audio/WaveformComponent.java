package com.omniviewer.audio;

import com.intellij.util.ui.JBUI;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
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
    private long audioDurationMicroseconds = 0; // Total audio duration in microseconds
    
    // Colors
    private static final Color WAVEFORM_COLOR = new Color(100, 50, 150); // Dark purple like in the image
    private static final Color PROGRESS_COLOR = new Color(255, 100, 100);
    private static final Color BACKGROUND_COLOR = new Color(60, 60, 60); // Dark gray background
    private static final Color HOVER_COLOR = new Color(255, 200, 100);
    private static final Color TIMELINE_COLOR = Color.WHITE; // White timeline markers
    private static final Color TIMELINE_TEXT_COLOR = Color.WHITE; // White text
    
    public WaveformComponent() {
        setPreferredSize(new Dimension(300, 80)); // Increased height to accommodate timeline
        setMinimumSize(new Dimension(200, 60));
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
            // Calculate duration from the audio stream
            if (audioStream != null) {
                AudioFormat format = audioStream.getFormat();
                long frameLength = audioStream.getFrameLength();
                if (frameLength != AudioSystem.NOT_SPECIFIED) {
                    audioDurationMicroseconds = (long) ((frameLength * 1_000_000.0) / format.getFrameRate());
                }
            }
            repaint();
        } catch (IOException e) {
            e.printStackTrace();
            waveformData = new ArrayList<>();
        }
    }
    
    public void setAudioDuration(long durationMicroseconds) {
        this.audioDurationMicroseconds = durationMicroseconds;
        repaint();
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
        
        // Draw timeline at the top
        drawTimeline(g2d, width, height);
        
        // Draw waveform (offset down to make room for timeline)
        drawWaveform(g2d, width, height);
        
        // Draw progress overlay
        drawProgressOverlay(g2d, width, height);
        
        // Draw hover indicator
        if (isHovering && hoverPosition >= 0) {
            drawHoverIndicator(g2d, width, height);
        }
        
        g2d.dispose();
    }
    
    private void drawTimeline(Graphics2D g2d, int width, int height) {
        if (audioDurationMicroseconds <= 0) return;
        
        g2d.setColor(TIMELINE_COLOR);
        g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 9f));
        FontMetrics fm = g2d.getFontMetrics();
        
        // Timeline area is at the top 25 pixels
        int timelineHeight = 25;
        int timelineY = 2;
        
        // Calculate optimal time interval based on duration and available width
        long totalSeconds = audioDurationMicroseconds / 1_000_000;
        long intervalMicroseconds = calculateOptimalInterval(totalSeconds, width, fm);
        
        // Draw timeline markers
        for (long time = 0; time <= audioDurationMicroseconds; time += intervalMicroseconds) {
            float progress = (float) time / audioDurationMicroseconds;
            int x = (int) (progress * width);
            
            // Draw vertical line - thin white line like in the image
            g2d.setColor(TIMELINE_COLOR);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawLine(x, timelineY, x, timelineY + timelineHeight);
            
            // Draw time label - white text like in the image
            String timeLabel = formatTime(time);
            int textWidth = fm.stringWidth(timeLabel);
            g2d.setColor(TIMELINE_TEXT_COLOR);
            g2d.drawString(timeLabel, x - textWidth / 2, timelineY + 12);
        }
    }
    
    private long calculateOptimalInterval(long totalSeconds, int width, FontMetrics fm) {
        // Target: approximately 8-12 markers across the timeline for good readability
        int targetMarkers = 10;
        long baseIntervalSeconds = totalSeconds / targetMarkers;
        
        // Round to nice intervals for better readability
        long intervalSeconds;
        
        if (baseIntervalSeconds <= 1) {
            // For very short audio, use 0.1, 0.2, 0.5, or 1 second intervals
            if (baseIntervalSeconds <= 0.1) {
                intervalSeconds = 1; // 1 second minimum
            } else if (baseIntervalSeconds <= 0.2) {
                intervalSeconds = 1;
            } else if (baseIntervalSeconds <= 0.5) {
                intervalSeconds = 1;
            } else {
                intervalSeconds = 1;
            }
        } else if (baseIntervalSeconds <= 5) {
            // Round to 1, 2, or 5 seconds
            if (baseIntervalSeconds <= 2) {
                intervalSeconds = 1;
            } else if (baseIntervalSeconds <= 3) {
                intervalSeconds = 2;
            } else {
                intervalSeconds = 5;
            }
        } else if (baseIntervalSeconds <= 30) {
            // Round to 5, 10, 15, or 30 seconds
            if (baseIntervalSeconds <= 7) {
                intervalSeconds = 5;
            } else if (baseIntervalSeconds <= 12) {
                intervalSeconds = 10;
            } else if (baseIntervalSeconds <= 20) {
                intervalSeconds = 15;
            } else {
                intervalSeconds = 30;
            }
        } else if (baseIntervalSeconds <= 300) { // 5 minutes
            // Round to 30 seconds, 1, 2, or 5 minutes
            if (baseIntervalSeconds <= 60) {
                intervalSeconds = 30;
            } else if (baseIntervalSeconds <= 90) {
                intervalSeconds = 60;
            } else if (baseIntervalSeconds <= 180) {
                intervalSeconds = 120;
            } else {
                intervalSeconds = 300;
            }
        } else {
            // For very long audio, use 5, 10, 15, or 30 minute intervals
            long baseIntervalMinutes = baseIntervalSeconds / 60;
            if (baseIntervalMinutes <= 7) {
                intervalSeconds = 300; // 5 minutes
            } else if (baseIntervalMinutes <= 12) {
                intervalSeconds = 600; // 10 minutes
            } else if (baseIntervalMinutes <= 20) {
                intervalSeconds = 900; // 15 minutes
            } else {
                intervalSeconds = 1800; // 30 minutes
            }
        }
        
        // Ensure we don't have too many markers (minimum 50 pixels between markers)
        int estimatedMarkers = (int) (totalSeconds / intervalSeconds);
        int minPixelsPerMarker = 50;
        int maxMarkers = width / minPixelsPerMarker;
        
        if (estimatedMarkers > maxMarkers) {
            // Increase interval to reduce number of markers
            intervalSeconds = (long) Math.ceil((double) totalSeconds / maxMarkers);
            // Round up to next nice interval
            if (intervalSeconds <= 5) {
                intervalSeconds = 5;
            } else if (intervalSeconds <= 10) {
                intervalSeconds = 10;
            } else if (intervalSeconds <= 30) {
                intervalSeconds = 30;
            } else if (intervalSeconds <= 60) {
                intervalSeconds = 60;
            } else if (intervalSeconds <= 300) {
                intervalSeconds = 300;
            } else if (intervalSeconds <= 600) {
                intervalSeconds = 600;
            } else {
                intervalSeconds = ((intervalSeconds / 300) + 1) * 300; // Round up to next 5-minute mark
            }
        }
        
        return intervalSeconds * 1_000_000; // Convert to microseconds
    }
    
    private String formatTime(long microseconds) {
        long totalSeconds = microseconds / 1_000_000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long milliseconds = (microseconds % 1_000_000) / 1_000;
        
        // Format like in the image: 0:00.000, 0:00.500, 0:01.000
        return String.format("%d:%02d.%03d", minutes, seconds, milliseconds);
    }
    
    private void drawWaveform(Graphics2D g2d, int width, int height) {
        if (waveformData.isEmpty()) return;
        
        g2d.setColor(WAVEFORM_COLOR);
        
        // Offset waveform down to make room for timeline
        int timelineHeight = 25;
        int waveformHeight = height - timelineHeight;
        int centerY = timelineHeight + waveformHeight / 2;
        
        int dataPoints = waveformData.size();
        float xStep = (float) width / dataPoints;
        
        // Draw waveform as vertical bars like in the image
        for (int i = 0; i < dataPoints; i++) {
            float amplitude = waveformData.get(i);
            int barHeight = (int) (amplitude * (waveformHeight - 10)); // Leave some margin
            
            int x = (int) (i * xStep);
            int y = centerY - barHeight / 2;
            
            // Draw as a thin vertical line/bar
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawLine(x, y, x, y + barHeight);
        }
    }
    
    private void drawProgressOverlay(Graphics2D g2d, int width, int height) {
        int progressX = (int) (progress * width);
        int timelineHeight = 25;
        
        // Draw progress background (darker area) - only over waveform area
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillRect(0, timelineHeight, progressX, height - timelineHeight);
        
        // Draw progress line
        g2d.setColor(PROGRESS_COLOR);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawLine(progressX, timelineHeight, progressX, height);
    }
    
    private void drawHoverIndicator(Graphics2D g2d, int width, int height) {
        int timelineHeight = 25;
        g2d.setColor(HOVER_COLOR);
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
        g2d.drawLine(hoverPosition, timelineHeight, hoverPosition, height);
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
