package com.omniviewer.jsonl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

public class JsonlEditorComponent extends JPanel {
    
    private final Project project;
    private final VirtualFile file;
    private Editor editor;
    private final Document document;
    private JsonPopupComponent currentPopup;
    private Timer hoverTimer;
    private int lastHoveredLine = -1;
    
    public JsonlEditorComponent(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = file;
        this.document = FileDocumentManager.getInstance().getDocument(file);
        
        setLayout(new BorderLayout());
        
        // Create editor on EDT
        ApplicationManager.getApplication().invokeLater(() -> {
            EditorFactory editorFactory = EditorFactory.getInstance();
            this.editor = editorFactory.createEditor(document, project);
            initializeEditor();
        });
    }
    
    private void initializeEditor() {
        if (editor == null) return;
        
        // Configure editor
        editor.getSettings().setLineNumbersShown(true);
        editor.getSettings().setGutterIconsShown(true);
        editor.getSettings().setWhitespacesShown(false);
        editor.getSettings().setLineMarkerAreaShown(true);
        editor.getSettings().setFoldingOutlineShown(true);
        editor.getSettings().setIndentGuidesShown(true);
        
        // Add mouse listeners for hover functionality
        editor.addEditorMouseMotionListener(new EditorMouseMotionListener() {
            @Override
            public void mouseMoved(@NotNull EditorMouseEvent event) {
                handleMouseMove(event);
            }
            
            @Override
            public void mouseDragged(@NotNull EditorMouseEvent event) {
                // Not needed for hover functionality
            }
        });
        
        editor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(@NotNull EditorMouseEvent event) {
                hideCurrentPopup();
            }
            
            @Override
            public void mouseClicked(@NotNull EditorMouseEvent event) {
                hideCurrentPopup();
            }
            
            @Override
            public void mouseReleased(@NotNull EditorMouseEvent event) {
                // Not needed
            }
            
            @Override
            public void mouseEntered(@NotNull EditorMouseEvent event) {
                // Not needed
            }
            
            @Override
            public void mouseExited(@NotNull EditorMouseEvent event) {
                hideCurrentPopup();
            }
        });
        
        // Add editor to panel
        add(editor.getComponent(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    private void handleMouseMove(@NotNull EditorMouseEvent event) {
        if (editor == null) return;
        
        Point point = event.getMouseEvent().getPoint();
        LogicalPosition logicalPos = editor.xyToLogicalPosition(point);
        int lineNumber = logicalPos.line;
        
        // Debug: Print line number
        System.out.println("Mouse moved to line: " + lineNumber);
        
        // If we're hovering over the same line, don't do anything
        if (lineNumber == lastHoveredLine) {
            return;
        }
        
        // Cancel previous timer
        if (hoverTimer != null) {
            hoverTimer.cancel();
        }
        
        // Hide current popup
        hideCurrentPopup();
        
        // Update lastHoveredLine before starting timer
        lastHoveredLine = lineNumber;
        
        // Start new timer for hover delay
        System.out.println("Starting timer for line: " + lineNumber);
        hoverTimer = new Timer();
        hoverTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timer executed for line: " + lineNumber + ", lastHoveredLine: " + lastHoveredLine);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (lineNumber == lastHoveredLine) {
                        System.out.println("Showing popup for line: " + lineNumber);
                        showJsonPopup(lineNumber);
                    } else {
                        System.out.println("Line changed, not showing popup. Current: " + lineNumber + ", Last: " + lastHoveredLine);
                    }
                });
            }
        }, 500); // 500ms delay before showing popup
    }
    
    private void showJsonPopup(int lineNumber) {
        try {
            String lineContent = document.getText().split("\n")[lineNumber].trim();
            System.out.println("Line content: " + lineContent);
            
            if (lineContent.isEmpty()) {
                System.out.println("Line is empty, skipping popup");
                return;
            }
            
            // Validate if the line contains valid JSON
            if (!isValidJson(lineContent)) {
                System.out.println("Invalid JSON, skipping popup");
                return;
            }
            
            System.out.println("Valid JSON found, showing popup");
            
            // Hide current popup
            hideCurrentPopup();
            
            // Create popup component
            currentPopup = new JsonPopupComponent(lineContent);
            
            // Calculate position for popup - use line position instead of mouse position
            Point lineStart = editor.logicalPositionToXY(new LogicalPosition(lineNumber, 0));
            SwingUtilities.convertPointToScreen(lineStart, editor.getComponent());
            
            // Show popup near the line
            currentPopup.showPopup(editor.getComponent(), lineStart.x + 20, lineStart.y + 20);
            
        } catch (Exception e) {
            System.out.println("Error showing popup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void hideCurrentPopup() {
        if (currentPopup != null) {
            currentPopup.hidePopup();
            currentPopup = null;
        }
        lastHoveredLine = -1;
    }
    
    private boolean isValidJson(String json) {
        try {
            // Simple JSON validation - check for basic structure
            json = json.trim();
            if (json.isEmpty()) {
                return false;
            }
            
            // Check if it starts and ends with valid JSON delimiters
            if ((json.startsWith("{") && json.endsWith("}")) ||
                (json.startsWith("[") && json.endsWith("]"))) {
                return true;
            }
            
            // Check for primitive values
            if (json.equals("true") || json.equals("false") || json.equals("null") ||
                json.matches("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$") ||
                (json.startsWith("\"") && json.endsWith("\""))) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    public JComponent getPreferredFocusedComponent() {
        return editor != null ? editor.getContentComponent() : this;
    }
    
    public boolean isModified() {
        return document.isWritable() && FileDocumentManager.getInstance().isDocumentUnsaved(document);
    }
    
    public void dispose() {
        hideCurrentPopup();
        if (hoverTimer != null) {
            hoverTimer.cancel();
        }
        if (editor != null) {
            EditorFactory.getInstance().releaseEditor(editor);
        }
    }
}
