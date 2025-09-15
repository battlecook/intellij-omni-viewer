package com.omniviewer.audio;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class AudioFileTypeFactory extends FileTypeFactory {
    
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        consumer.consume(AudioFileType.INSTANCE, "mp3;wav;ogg;flac;m4a;aac;wma");
    }
}
