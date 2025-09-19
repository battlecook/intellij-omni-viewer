package com.omniviewer.audio;

import com.intellij.openapi.vfs.VirtualFile;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.Obuffer;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MP3WaveformExtractor {
    
    private List<Float> waveformData;
    private byte[] audioData;
    
    public void extractWaveform(VirtualFile file) {
        try {
            System.out.println("=== MP3WaveformExtractor.extractWaveform() called ===");
            
            waveformData = new ArrayList<>();
            ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
            
            // Create a fresh input stream for waveform extraction
            InputStream inputStream = file.getInputStream();
            Bitstream bitstream = new Bitstream(inputStream);
            Decoder decoder = new Decoder();
            
            int sampleCount = 0;
            int maxSamples = 2000; // Limit samples for performance
            
            System.out.println("Starting MP3 decoding for waveform extraction...");
            
            while (true) {
                try {
                    Header header = bitstream.readFrame();
                    if (header == null) {
                        System.out.println("End of MP3 stream reached");
                        break;
                    }
                    
                    Obuffer obuffer = decoder.decodeFrame(header, bitstream);
                    if (obuffer == null) {
                        System.out.println("Failed to decode frame");
                        break;
                    }
                    
                    // Check if it's a SampleBuffer
                    if (!(obuffer instanceof SampleBuffer)) {
                        System.out.println("Unexpected buffer type: " + obuffer.getClass().getName());
                        continue;
                    }
                    
                    SampleBuffer sampleBuffer = (SampleBuffer) obuffer;
                    
                    // Extract samples for waveform
                    short[] samples = sampleBuffer.getBuffer();
                    int channels = sampleBuffer.getChannelCount();
                    
                    // Process samples for waveform data
                    for (int i = 0; i < samples.length && sampleCount < maxSamples; i += channels) {
                        float amplitude = 0.0f;
                        
                        // Average across channels
                        for (int ch = 0; ch < channels && (i + ch) < samples.length; ch++) {
                            amplitude += Math.abs(samples[i + ch]);
                        }
                        amplitude /= channels;
                        
                        // Normalize to 0-1 range
                        amplitude = amplitude / Short.MAX_VALUE;
                        waveformData.add(amplitude);
                        
                        sampleCount++;
                    }
                    
                    // Also collect raw audio data for AudioInputStream
                    byte[] frameData = new byte[samples.length * 2]; // 16-bit samples
                    for (int i = 0; i < samples.length; i++) {
                        frameData[i * 2] = (byte) (samples[i] & 0xFF);
                        frameData[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
                    }
                    audioBuffer.write(frameData);
                    
                    bitstream.closeFrame();
                    
                } catch (Exception e) {
                    System.err.println("Error processing MP3 frame: " + e.getMessage());
                    break;
                }
            }
            
            bitstream.close();
            inputStream.close();
            
            audioData = audioBuffer.toByteArray();
            
            System.out.println("MP3 waveform extraction completed:");
            System.out.println("- Waveform samples: " + waveformData.size());
            System.out.println("- Audio data size: " + audioData.length + " bytes");
            
        } catch (Exception e) {
            System.err.println("Error extracting MP3 waveform: " + e.getMessage());
            e.printStackTrace();
            waveformData = new ArrayList<>();
            audioData = new byte[0];
        }
    }
    
    public List<Float> getWaveformData() {
        return waveformData;
    }
    
    public byte[] getAudioData() {
        return audioData;
    }
}
