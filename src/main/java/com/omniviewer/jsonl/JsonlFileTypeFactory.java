package com.omniviewer.jsonl;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class JsonlFileTypeFactory extends FileTypeFactory {
    
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        consumer.consume(JsonlFileType.INSTANCE, "jsonl");
        consumer.consume(JsonlFileType.INSTANCE, "ndjson");
        consumer.consume(JsonlFileType.INSTANCE, "jsonlines");
    }
}
