package com.omniviewer.jsonl;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class JsonlEditor extends UserDataHolderBase implements FileEditor {
    
    private final Project project;
    private final VirtualFile file;
    private final JsonlEditorComponent component;
    
    public JsonlEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = file;
        this.component = new JsonlEditorComponent(project, file);
    }
    
    @Override
    @NotNull
    public JComponent getComponent() {
        return component;
    }
    
    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return component.getPreferredFocusedComponent();
    }
    
    @Override
    @NotNull
    public String getName() {
        return "JSONL Viewer";
    }
    
    @Override
    public void setState(@NotNull FileEditorState state) {
        // No state to set for JSONL editor
    }
    
    @Override
    public boolean isModified() {
        return component.isModified();
    }
    
    @Override
    public boolean isValid() {
        return file.isValid();
    }
    
    @Override
    public void selectNotify() {
        // Called when the editor becomes active
    }
    
    @Override
    public void deselectNotify() {
        // Called when the editor becomes inactive
    }
    
    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // No properties to listen to
    }
    
    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // No properties to listen to
    }
    
    @Override
    public void dispose() {
        component.dispose();
    }
    
    @Override
    @Nullable
    public FileEditorLocation getCurrentLocation() {
        return null;
    }
    
    @Override
    @NotNull
    public VirtualFile getFile() {
        return file;
    }
}
