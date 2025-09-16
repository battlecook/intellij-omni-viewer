package com.omniviewer.jsonl;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JsonPopupComponent extends JPanel {
    
    private final String jsonContent;
    private final JEditorPane editorPane;
    private final JScrollPane scrollPane;
    private JWindow popupWindow;
    
    public JsonPopupComponent(@NotNull String jsonContent) {
        this.jsonContent = jsonContent;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1),
            JBUI.Borders.empty(8)
        ));
        setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
        
        // Create formatted JSON editor pane with syntax highlighting
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        editorPane.setBackground(getBackground());
        editorPane.setBorder(null);
        editorPane.setText(createHighlightedJson(formatJson(jsonContent)));
        editorPane.setCaretPosition(0);
        
        // Add scroll pane
        scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Add close button
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setBorder(null);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> hidePopup());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.NORTH);
        
        // Add mouse listener to hide popup when clicking outside
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == JsonPopupComponent.this) {
                    hidePopup();
                }
            }
        });
    }
    
    public void showPopup(Component parent, int x, int y) {
        System.out.println("JsonPopupComponent.showPopup called with x=" + x + ", y=" + y);
        
        if (popupWindow != null) {
            System.out.println("Disposing existing popup window");
            popupWindow.dispose();
        }
        
        try {
            popupWindow = new JWindow(SwingUtilities.getWindowAncestor(parent));
            popupWindow.getContentPane().add(this);
            popupWindow.pack();
            popupWindow.setLocation(x, y);
            popupWindow.setVisible(true);
            System.out.println("Popup window created and shown successfully");
        } catch (Exception e) {
            System.out.println("Error creating popup window: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void hidePopup() {
        if (popupWindow != null) {
            popupWindow.dispose();
            popupWindow = null;
        }
    }
    
    private String formatJson(String json) {
        try {
            // Simple JSON formatting - add indentation
            StringBuilder formatted = new StringBuilder();
            int indentLevel = 0;
            boolean inString = false;
            boolean escapeNext = false;
            
            for (char c : json.toCharArray()) {
                if (escapeNext) {
                    formatted.append(c);
                    escapeNext = false;
                    continue;
                }
                
                if (c == '\\') {
                    escapeNext = true;
                    formatted.append(c);
                    continue;
                }
                
                if (c == '"') {
                    inString = !inString;
                    formatted.append(c);
                    continue;
                }
                
                if (!inString) {
                    switch (c) {
                        case '{':
                        case '[':
                            formatted.append(c).append('\n');
                            indentLevel++;
                            appendIndent(formatted, indentLevel);
                            break;
                        case '}':
                        case ']':
                            formatted.append('\n');
                            indentLevel--;
                            appendIndent(formatted, indentLevel);
                            formatted.append(c);
                            break;
                        case ',':
                            formatted.append(c).append('\n');
                            appendIndent(formatted, indentLevel);
                            break;
                        case ':':
                            formatted.append(c).append(' ');
                            break;
                        default:
                            formatted.append(c);
                            break;
                    }
                } else {
                    formatted.append(c);
                }
            }
            
            return formatted.toString();
        } catch (Exception e) {
            return json; // Return original if formatting fails
        }
    }
    
    private void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
    }
    
    private String createHighlightedJson(String formattedJson) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: monospace; font-size: 12px; margin: 0; padding: 0; background-color: ");
        html.append(getBackgroundColorHex());
        html.append("; color: ");
        html.append(getForegroundColorHex());
        html.append("; }");
        html.append(".json-key { color: #9876aa; }");
        html.append(".json-string { color: #6a8759; }");
        html.append(".json-number { color: #6897bb; }");
        html.append(".json-boolean { color: #cc7832; }");
        html.append(".json-null { color: #808080; font-style: italic; }");
        html.append(".json-bracket { color: #a9b7c6; }");
        html.append(".json-comma { color: #a9b7c6; }");
        html.append(".json-colon { color: #a9b7c6; }");
        html.append("</style></head><body><pre>");
        
        // Parse and highlight the JSON
        String highlighted = highlightJsonSyntax(formattedJson);
        html.append(highlighted);
        html.append("</pre></body></html>");
        
        return html.toString();
    }
    
    private String getBackgroundColorHex() {
        Color bg = EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground();
        return String.format("#%02x%02x%02x", bg.getRed(), bg.getGreen(), bg.getBlue());
    }
    
    private String getForegroundColorHex() {
        Color fg = EditorColorsManager.getInstance().getGlobalScheme().getDefaultForeground();
        return String.format("#%02x%02x%02x", fg.getRed(), fg.getGreen(), fg.getBlue());
    }
    
    private String highlightJsonSyntax(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escapeNext = false;
        boolean inKey = false;
        boolean afterColon = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escapeNext) {
                result.append(escapeHtmlChar(c));
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                result.append(escapeHtmlChar(c));
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                if (inString) {
                    // Check if this is a key (preceded by { or , and followed by :)
                    inKey = isKeyStart(json, i);
                    if (inKey) {
                        result.append("<span class=\"json-key\">").append(escapeHtmlChar(c));
                    } else {
                        result.append("<span class=\"json-string\">").append(escapeHtmlChar(c));
                    }
                } else {
                    result.append(escapeHtmlChar(c)).append("</span>");
                    inKey = false;
                }
                continue;
            }
            
            if (inString) {
                result.append(escapeHtmlChar(c));
                continue;
            }
            
            // Handle non-string characters
            switch (c) {
                case '{':
                case '}':
                case '[':
                case ']':
                    result.append("<span class=\"json-bracket\">").append(escapeHtmlChar(c)).append("</span>");
                    afterColon = false;
                    break;
                case ',':
                    result.append("<span class=\"json-comma\">").append(escapeHtmlChar(c)).append("</span>");
                    afterColon = false;
                    break;
                case ':':
                    result.append("<span class=\"json-colon\">").append(escapeHtmlChar(c)).append("</span>");
                    afterColon = true;
                    break;
                case ' ':
                case '\n':
                case '\t':
                    result.append(escapeHtmlChar(c));
                    break;
                default:
                    if (Character.isDigit(c) || c == '-' || c == '.') {
                        // Number
                        if (afterColon) {
                            result.append("<span class=\"json-number\">").append(escapeHtmlChar(c));
                            // Find the end of the number
                            int j = i + 1;
                            while (j < json.length() && (Character.isDigit(json.charAt(j)) || json.charAt(j) == '.' || json.charAt(j) == 'e' || json.charAt(j) == 'E' || json.charAt(j) == '+' || json.charAt(j) == '-')) {
                                result.append(escapeHtmlChar(json.charAt(j)));
                                j++;
                            }
                            result.append("</span>");
                            i = j - 1; // Adjust loop counter
                        } else {
                            result.append(escapeHtmlChar(c));
                        }
                    } else if (c == 't' || c == 'f' || c == 'n') {
                        // Boolean or null
                        if (afterColon) {
                            String word = extractWord(json, i);
                            if (word.equals("true") || word.equals("false")) {
                                result.append("<span class=\"json-boolean\">").append(escapeHtml(word)).append("</span>");
                                i += word.length() - 1; // Adjust loop counter
                            } else if (word.equals("null")) {
                                result.append("<span class=\"json-null\">").append(escapeHtml(word)).append("</span>");
                                i += word.length() - 1; // Adjust loop counter
                            } else {
                                result.append(escapeHtmlChar(c));
                            }
                        } else {
                            result.append(escapeHtmlChar(c));
                        }
                    } else {
                        result.append(escapeHtmlChar(c));
                    }
                    break;
            }
        }
        
        return result.toString();
    }
    
    private boolean isKeyStart(String json, int pos) {
        // Look backwards to find the last non-whitespace character
        for (int i = pos - 1; i >= 0; i--) {
            char c = json.charAt(i);
            if (c == ' ' || c == '\n' || c == '\t') {
                continue;
            }
            return c == '{' || c == ',';
        }
        return false;
    }
    
    private String extractWord(String json, int start) {
        StringBuilder word = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isLetter(c)) {
                word.append(c);
            } else {
                break;
            }
        }
        return word.toString();
    }
    
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    private String escapeHtmlChar(char c) {
        switch (c) {
            case '&': return "&amp;";
            case '<': return "&lt;";
            case '>': return "&gt;";
            case '"': return "&quot;";
            case '\'': return "&#39;";
            default: return String.valueOf(c);
        }
    }
}