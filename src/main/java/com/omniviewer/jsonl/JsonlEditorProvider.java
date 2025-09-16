package com.omniviewer.jsonl;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class JsonlEditorProvider implements FileEditorProvider, DumbAware {
    
    private static final String EDITOR_TYPE_ID = "jsonl-editor";
    
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getFileType() instanceof JsonlFileType;
    }
    
    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new JsonlEditor(project, file);
    }
    
    @Override
    @NotNull
    public String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }
    
    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.NONE;
    }
}
