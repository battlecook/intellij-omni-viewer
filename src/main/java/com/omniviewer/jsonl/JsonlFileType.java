package com.omniviewer.jsonl;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JsonlFileType implements FileType {
    
    public static final JsonlFileType INSTANCE = new JsonlFileType();
    
    @Override
    @NotNull
    @NonNls
    public String getName() {
        return "JSONL";
    }
    
    @Override
    @NotNull
    @NlsContexts.Label
    public String getDescription() {
        return "JSON Lines files (JSONL)";
    }
    
    @Override
    @NotNull
    @NonNls
    public String getDefaultExtension() {
        return "jsonl";
    }
    
    @Override
    @Nullable
    public Icon getIcon() {
        return null; // Will use default file icon
    }
    
    @Override
    public boolean isBinary() {
        return false;
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    @Override
    @Nullable
    @NonNls
    public String getCharset(@NotNull VirtualFile file, byte @NotNull [] content) {
        return "UTF-8";
    }
}
