package com.smarttask.client.view.renderer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UIRenderer {

    private final TextFlow targetFlow;
    private final ScrollPane scrollPane;
    private Rectangle cursorNode;
    private boolean isBoldMode = false;

    // --- ⚡ OPTIMISATION : MÉMOIRE TAMPON + COMPTEUR SCROLL ---
    private Text currentTextNode = null;
    private String currentStyleClass = "";
    private int scrollCounter = 0;

    // SVG Constants
    private static final String SVG_DELETE = "M6 19c0 1.1.9 2 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    private static final String SVG_WARNING = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
    private static final String SVG_CREATE = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    private static final String SVG_SUCCESS = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";

    public UIRenderer(TextFlow targetFlow, ScrollPane scrollPane) {
        this.targetFlow = targetFlow;
        this.scrollPane = scrollPane;
        initCursor();
    }

    public void clear() {
        targetFlow.getChildren().clear();
        isBoldMode = false;
        currentTextNode = null; // Reset du tampon
        currentStyleClass = "";
    }

    public void animateText(String fullText, Runnable onFinished) {
        if (fullText == null || fullText.isEmpty()) return;

        // Détecter et extraire les tableaux markdown
        if (containsMarkdownTable(fullText)) {
            renderMarkdownWithTables(fullText, onFinished);
            return;
        }

        // ⚡ OPTIMISÉ: Pas d'animation pour les textes longs (>500 chars)
        // Affichage instantané pour éviter les lags
        if (fullText.length() > 500) {
            targetFlow.getChildren().remove(cursorNode);
            appendRichText(fullText);
            targetFlow.getChildren().add(cursorNode);
            scrollToBottom();
            if (onFinished != null) {
                Platform.runLater(onFinished);
            }
            return;
        }

        // Gestion du curseur
        targetFlow.getChildren().remove(cursorNode);
        targetFlow.getChildren().add(cursorNode);
        if (!cursorNode.getStyleClass().contains("cursor-blink")) {
            cursorNode.getStyleClass().add("cursor-blink");
        }

        Timeline timeline = new Timeline();
        // ⚡ OPTIMISÉ : Affichage par mots entiers au lieu de caractères
        String[] chunks = fullText.split("(?=[\\s\\n])|(?<=[\\s\\n])");
        
        Duration delay = Duration.ZERO;
        
        // ⚡ Vitesse augmentée : 15ms -> 10ms pour plus de rapidité
        Duration step = Duration.millis(10); 

        for (String chunk : chunks) {
            timeline.getKeyFrames().add(new KeyFrame(delay, e -> {
                targetFlow.getChildren().remove(cursorNode);
                appendRichText(chunk);
                targetFlow.getChildren().add(cursorNode);
            }));
            delay = delay.add(step);
        }

        timeline.setOnFinished(e -> {
            targetFlow.getChildren().remove(cursorNode);
            if (onFinished != null) onFinished.run();
        });
        timeline.play();
    }

    public void appendRichText(String text) {
        if (text.equals("\n")) {
            targetFlow.getChildren().add(new Text("\n"));
            currentTextNode = null; // Force nouveau nœud après saut de ligne
            return;
        }

        String cleanText = text;
        
        // --- 0. Détection des Titres Markdown ---
        if (text.trim().startsWith("###")) {
            Text heading = new Text(text.replace("###", "").trim() + "\n");
            heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-fill: #2c3e50;");
            targetFlow.getChildren().add(heading);
            currentTextNode = null;
            scrollToBottom();
            return;
        } else if (text.trim().startsWith("##")) {
            Text heading = new Text(text.replace("##", "").trim() + "\n");
            heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #2c3e50;");
            targetFlow.getChildren().add(heading);
            currentTextNode = null;
            scrollToBottom();
            return;
        } else if (text.trim().startsWith("#")) {
            Text heading = new Text(text.replace("#", "").trim() + "\n");
            heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #2c3e50;");
            targetFlow.getChildren().add(heading);
            currentTextNode = null;
            scrollToBottom();
            return;
        }
        
        // --- 1. Liste à puces ---
        if (text.trim().startsWith("• ") || text.trim().startsWith("- ") || text.trim().startsWith("* ")) {
            cleanText = "  " + text.replace("- ", "• ").replace("* ", "• ");
        }
        
        // --- 2. Code Pill (Label distinct) ---
        if (cleanText.contains("`")) {
            String code = cleanText.replace("`", "").trim();
            if (!code.isEmpty()) {
                Label codeLabel = new Label(code);
                codeLabel.getStyleClass().add("ai-code-pill");
                codeLabel.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-family: 'Courier New';");
                targetFlow.getChildren().add(codeLabel);
                currentTextNode = null; // Reset tampon
            }
            scrollToBottom();
            return;
        }

        // --- 3. Détection du Style ---
        String styleClass = "ai-text-normal";
        String upper = cleanText.toUpperCase().trim();

        if (upper.matches("DELETE|DROP|REMOVE|TRUNCATE")) styleClass = "sql-keyword-danger";
        else if (upper.matches("SELECT|INSERT|UPDATE|CREATE")) styleClass = "sql-keyword-safe";
        else if (upper.matches("WHERE|FROM|AND|OR|JOIN|LIMIT")) styleClass = "sql-keyword-logic";
        else {
            if (cleanText.contains("**")) {
                cleanText = cleanText.replace("**", "");
                isBoldMode = !isBoldMode; // Bascule
                styleClass = "ai-text-bold";
            } else {
                styleClass = isBoldMode ? "ai-text-bold" : "ai-text-normal";
            }
        }

        // --- 4. ⚡ OPTIMISATION MAJEURE : RÉUTILISATION DES NŒUDS + BATCH ---
        if (currentTextNode != null && currentStyleClass.equals(styleClass)) {
            // Réutilisation du même node texte pour économiser mémoire et rendering
            currentTextNode.setText(currentTextNode.getText() + cleanText);
        } else {
            Text newText = new Text(cleanText);
            newText.getStyleClass().add(styleClass);
            targetFlow.getChildren().add(newText);
            
            currentTextNode = newText;
            currentStyleClass = styleClass;
        }

        // ⚡ Scroll uniquement tous les 10 appels pour réduire le CPU
        scrollCounter++;
        if (scrollCounter % 10 == 0) {
            scrollToBottom();
        }
    }

    public void addIcon(String type) {
        if (type == null || type.isEmpty()) return;
        
        StackPane container = new StackPane();
        container.getStyleClass().add("chat-icon-container");
        SVGPath svg = new SVGPath();
        svg.getStyleClass().add("chat-icon-svg");

        switch (type.toUpperCase()) {
            case "DELETE": case "REMOVE":
                svg.setContent(SVG_DELETE); svg.getStyleClass().add("icon-danger"); container.getStyleClass().add("container-danger"); break;
            case "UPDATE": case "WARNING":
                svg.setContent(SVG_WARNING); svg.getStyleClass().add("icon-warning"); container.getStyleClass().add("container-warning"); break;
            case "CREATE":
                svg.setContent(SVG_CREATE); svg.getStyleClass().add("icon-success"); container.getStyleClass().add("container-success"); break;
            case "SUCCESS":
                svg.setContent(SVG_SUCCESS); svg.getStyleClass().add("icon-success"); container.getStyleClass().add("container-success"); break;
            default: return;
        }
        
        container.getChildren().add(svg);
        targetFlow.getChildren().add(new HBox(container));
        targetFlow.getChildren().add(new Text("\n"));
        currentTextNode = null; // Reset après icône
    }
    
    public void addFeedback(String msg, boolean success) {
        Label lbl = new Label(" " + msg);
        lbl.getStyleClass().add(success ? "msg-success" : "msg-error");
        HBox box = new HBox(lbl);
        box.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 5px 0 0 0;");
        targetFlow.getChildren().addAll(new Text("\n\n"), box);
        scrollToBottom();
        currentTextNode = null;
    }

    public void addSystemText(String msg) {
        Text t = new Text(msg);
        t.setStyle("-fx-fill: #aaaaaa; -fx-font-style: italic;");
        targetFlow.getChildren().add(t);
        scrollToBottom();
        currentTextNode = null;
    }

    private void scrollToBottom() {
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    private void initCursor() {
        cursorNode = new Rectangle(2, 16);
        cursorNode.getStyleClass().add("cursor-caret");
    }
    
    // ========================== MARKDOWN ADVANCED RENDERING ==========================
    
    /**
     * Détecte si le texte contient un tableau markdown
     */
    private boolean containsMarkdownTable(String text) {
        return text.contains("|") && text.contains("---");
    }
    
    /**
     * Rend le markdown avec support des tableaux
     */
    private void renderMarkdownWithTables(String markdown, Runnable onFinished) {
        targetFlow.getChildren().clear();
        currentTextNode = null;
        
        String[] lines = markdown.split("\n");
        List<String> tableLines = new ArrayList<>();
        boolean inTable = false;
        
        for (String line : lines) {
            if (line.trim().startsWith("|")) {
                inTable = true;
                tableLines.add(line);
            } else {
                if (inTable && !tableLines.isEmpty()) {
                    // Fin du tableau - le rendre
                    renderMarkdownTable(tableLines);
                    tableLines.clear();
                    inTable = false;
                }
                
                // Ligne normale
                if (!line.trim().isEmpty()) {
                    appendRichText(line + "\n");
                } else {
                    appendRichText("\n");
                }
            }
        }
        
        // Dernier tableau si le markdown se termine par un tableau
        if (!tableLines.isEmpty()) {
            renderMarkdownTable(tableLines);
        }
        
        scrollToBottom();
        if (onFinished != null) onFinished.run();
    }
    
    /**
     * Rend un tableau markdown
     */
    private void renderMarkdownTable(List<String> tableLines) {
        if (tableLines.size() < 2) return; // Besoin au minimum header + separator
        
        // Parser le header
        String headerLine = tableLines.get(0);
        String[] headers = parseTableRow(headerLine);
        
        // Parser les lignes de données (sauter la ligne de séparation)
        List<String[]> rows = new ArrayList<>();
        for (int i = 2; i < tableLines.size(); i++) {
            rows.add(parseTableRow(tableLines.get(i)));
        }
        
        // Créer le tableau visuel
        GridPane table = new GridPane();
        table.getStyleClass().add("markdown-table");
        table.setHgap(10);
        table.setVgap(5);
        table.setPadding(new Insets(10));
        table.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8px; -fx-border-color: #e0e0e0; -fx-border-radius: 8px; -fx-border-width: 1px;");
        
        // Ajouter le header
        for (int col = 0; col < headers.length; col++) {
            Label headerLabel = new Label(headers[col].trim());
            headerLabel.getStyleClass().add("table-header");
            headerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 5px 10px; -fx-background-color: #e3f2fd; -fx-background-radius: 4px;");
            table.add(headerLabel, col, 0);
        }
        
        // Ajouter les données
        for (int row = 0; row < rows.size(); row++) {
            String[] rowData = rows.get(row);
            for (int col = 0; col < rowData.length && col < headers.length; col++) {
                String cellText = rowData[col].trim();
                
                // Détection des émojis et styling
                Label cellLabel = new Label(cellText);
                cellLabel.getStyleClass().add("table-cell");
                cellLabel.setStyle("-fx-padding: 5px 10px; -fx-text-fill: #34495e;");
                
                // Coloration selon le contenu
                if (cellText.contains("HIGH") || cellText.contains("URGENT") || cellText.contains("🔴")) {
                    cellLabel.setStyle(cellLabel.getStyle() + " -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else if (cellText.contains("COMPLETED") || cellText.contains("✅")) {
                    cellLabel.setStyle(cellLabel.getStyle() + " -fx-text-fill: #27ae60;");
                } else if (cellText.contains("IN_PROGRESS") || cellText.contains("🔄")) {
                    cellLabel.setStyle(cellLabel.getStyle() + " -fx-text-fill: #3498db;");
                }
                
                table.add(cellLabel, col, row + 1);
            }
        }
        
        // Ajouter le tableau au flow
        VBox tableContainer = new VBox(table);
        tableContainer.setPadding(new Insets(10, 0, 10, 0));
        targetFlow.getChildren().add(tableContainer);
        targetFlow.getChildren().add(new Text("\n"));
        
        currentTextNode = null;
    }
    
    /**
     * Parse une ligne de tableau markdown
     */
    private String[] parseTableRow(String line) {
        // Retirer les | de début et fin
        line = line.trim();
        if (line.startsWith("|")) line = line.substring(1);
        if (line.endsWith("|")) line = line.substring(0, line.length() - 1);
        
        return line.split("\\|");
    }
}