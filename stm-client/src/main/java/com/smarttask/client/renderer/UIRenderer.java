package com.smarttask.client.view.renderer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public class UIRenderer {

    private final TextFlow targetFlow;
    private final ScrollPane scrollPane;
    private Rectangle cursorNode;
    private boolean isBoldMode = false;

    // --- OPTIMISATION : MÉMOIRE TAMPON ---
    private Text currentTextNode = null;
    private String currentStyleClass = "";

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

        // Gestion du curseur
        targetFlow.getChildren().remove(cursorNode);
        targetFlow.getChildren().add(cursorNode);
        if (!cursorNode.getStyleClass().contains("cursor-blink")) {
            cursorNode.getStyleClass().add("cursor-blink");
        }

        Timeline timeline = new Timeline();
        // Optimisation : Split par espace pour moins de keyframes (plus gros morceaux)
        String[] chunks = fullText.split("(?=[\\s\\n])|(?<=[\\s\\n])");
        
        Duration delay = Duration.ZERO;
        
        // Vitesse ajustée : 25ms (plus fluide que 15ms pour le CPU)
        Duration step = Duration.millis(25); 

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

        String cleanText = text.replace("- ", "• ").replace("* ", "• ");
        
        // --- 1. Code Pill (Label distinct) ---
        if (cleanText.contains("`")) {
            String code = cleanText.replace("`", "").trim();
            if (!code.isEmpty()) {
                Label codeLabel = new Label(code);
                codeLabel.getStyleClass().add("ai-code-pill");
                targetFlow.getChildren().add(codeLabel);
                currentTextNode = null; // Reset tampon
            }
            scrollToBottom();
            return;
        }

        // --- 2. Détection du Style ---
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
            } else if (cleanText.startsWith("###")) {
                cleanText = cleanText.replace("###", "") + "\n";
                styleClass = "ai-text-h3";
            } else {
                styleClass = isBoldMode ? "ai-text-bold" : "ai-text-normal";
            }
        }

        // --- 3. OPTIMISATION MAJEURE : RÉUTILISATION DES NŒUDS ---
        // Si le style est le même que le mot précédent, on concatène le texte
        // au lieu de créer un objet Text JavaFX (lourd pour le Scene Graph).
        if (currentTextNode != null && currentStyleClass.equals(styleClass)) {
            currentTextNode.setText(currentTextNode.getText() + cleanText);
        } else {
            // Nouveau style ou premier mot -> Nouveau nœud
            Text newText = new Text(cleanText);
            newText.getStyleClass().add(styleClass);
            targetFlow.getChildren().add(newText);
            
            currentTextNode = newText;
            currentStyleClass = styleClass;
        }

        scrollToBottom();
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
}