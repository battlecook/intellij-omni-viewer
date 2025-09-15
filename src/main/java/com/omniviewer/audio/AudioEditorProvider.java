package com.omniviewer.audio;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AudioEditorProvider implements FileEditorProvider, DumbAware {
    
    private static final String EDITOR_TYPE_ID = "AudioEditor";
    
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getFileType() instanceof AudioFileType;
    }
    
    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new AudioEditor(project, file);
    }
    
    @Override
    @NotNull
    public String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }
    
    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
