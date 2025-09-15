package com.omniviewer.audio;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AudioFileType implements FileType {
    
    public static final AudioFileType INSTANCE = new AudioFileType();
    
    @Override
    @NotNull
    @NonNls
    public String getName() {
        return "Audio";
    }
    
    @Override
    @NotNull
    @NlsContexts.Label
    public String getDescription() {
        return "Audio files (MP3, WAV, OGG, FLAC, M4A, AAC, WMA)";
    }
    
    @Override
    @NotNull
    @NonNls
    public String getDefaultExtension() {
        return "mp3";
    }
    
    @Override
    @Nullable
    public Icon getIcon() {
        return null; // Will use default file icon
    }
    
    @Override
    public boolean isBinary() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    @Override
    @Nullable
    @NonNls
    public String getCharset(@NotNull VirtualFile file, byte @NotNull [] content) {
        return null;
    }
}
