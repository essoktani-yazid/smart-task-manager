package com.smarttask.client.view.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class AIController {

    // --- ÉLÉMENTS FXML ---
    @FXML private StackPane aiRoot;
    @FXML private HBox searchContainer;
    
    @FXML private VBox homeContainer;
    @FXML private VBox resultContainer;
    
    @FXML private Label typingTitle;
    @FXML private TextArea userInput;
    @FXML private Label userQuestionLabel;
    
    @FXML private TextFlow aiResponseFlow;
    @FXML private ScrollPane responseScroll;
    
    // Zone de Statut (Spinner)
    @FXML private HBox statusContainer;
    @FXML private SVGPath statusIcon;
    @FXML private Label statusLabel;
    
    // Zone de confirmation
    @FXML private HBox confirmationBox;
    @FXML private Button confirmBtn;
    @FXML private Button cancelBtn;

    @FXML private Button sendButton;

    // --- CONSTANTES ---
    private final String TITLE_TEXT = "How can I supercharge your productivity?";
    private static final String AI_SERVICE_URL = "ws://localhost:8000/ai/stream";
    private final String ICON_SEND = "M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z";
    private final String ICON_STOP = "M6 6h12v12H6z";

    // --- OBJETS UTILITAIRES ---
    private WebSocket webSocket;
    private final Gson gson = new Gson();
    
    // --- ÉTAT ---
    private boolean isGenerating = false;
    private boolean isBoldMode = false;
    private String pendingSqlQuery = null;
    private Rectangle cursorNode; // Le curseur clignotant

    @FXML
    public void initialize() {
        // 1. Initialisation de l'interface : Cacher les éléments non nécessaires
        homeContainer.setVisible(true);
        resultContainer.setVisible(false);
        resultContainer.setManaged(false);
        
        if (confirmationBox != null) {
            confirmationBox.setVisible(false);
            confirmationBox.setManaged(false);
        }
        
        if (statusContainer != null) {
            statusContainer.setVisible(false);
            statusContainer.setManaged(false);
        }

        // 2. Configuration des comportements
        setupAutoGrowingTextArea();
        typeTitleAnimation();
        
        // 3. Connexion au Backend
        connectToAiService();
    }

    private void connectToAiService() {
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(AI_SERVICE_URL), new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    System.out.println("✅ Connecté au service IA");
                })
                .exceptionally(ex -> {
                    System.err.println("❌ Erreur WebSocket : " + ex.getMessage());
                    return null;
                });
    }

    // --- GESTION DE L'ENVOI ---

    @FXML
    private void handleSendMessage() {
        String message = userInput.getText();
        if (message == null || message.trim().isEmpty()) return;

        // Si génération en cours, le bouton agit comme STOP
        if (isGenerating) {
            if (webSocket != null) webSocket.sendText("__STOP__", true);
        }

        // Reset UI pour la nouvelle question
        userQuestionLabel.setText(message);
        aiResponseFlow.getChildren().clear();
        userInput.clear();
        userInput.setPrefHeight(40);
        pendingSqlQuery = null;
        
        if (confirmationBox != null) {
            confirmationBox.setVisible(false);
            confirmationBox.setManaged(false);
        }
        
        isBoldMode = false;

        // ACTIVATION DU SPINNER (Status Pill)
        if (statusContainer != null) {
            statusContainer.setVisible(true);
            statusContainer.setManaged(true);
            statusLabel.setText("Thinking...");
            if (!statusIcon.getStyleClass().contains("spinning")) {
                statusIcon.getStyleClass().add("spinning");
            }
        }

        performSearchTransition();
        setGeneratingState(true);

        // Envoi au Backend
        if (webSocket != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("prompt", message);
            webSocket.sendText(gson.toJson(payload), true);
        } else {
            handleFinalResponse("⚠️ Service disconnected. Reconnecting...", true);
            connectToAiService();
            setGeneratingState(false);
        }
    }

    // --- RÉCEPTION ET TRAITEMENT (ROBUSTE) ---

    private void processIncomingJson(String jsonString) {
        System.out.println("DEBUG JSON: " + jsonString);

        try {
            JsonObject response = gson.fromJson(jsonString, JsonObject.class);

            // CAS 1 : Mise à jour de statut (ex: "Calculating impact...")
            if (response.has("status")) {
                String statusText = response.get("status").getAsString();
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText(statusText);
                });
                return; 
            }

            // CAS 2 : Réponse finale
            // Extraction sécurisée pour éviter UnsupportedOperationException sur JsonNull
            String displayMessage = "";
            if (response.has("display_message") && !response.get("display_message").isJsonNull()) {
                displayMessage = response.get("display_message").getAsString();
            }

            boolean requiresConfirmation = false;
            if (response.has("requires_confirmation") && !response.get("requires_confirmation").isJsonNull()) {
                requiresConfirmation = response.get("requires_confirmation").getAsBoolean();
            }

            String sqlQuery = "";
            if (response.has("sql_to_execute") && !response.get("sql_to_execute").isJsonNull()) {
                sqlQuery = response.get("sql_to_execute").getAsString();
            }

            // Variables finales pour le thread UI
            String finalDisplayMessage = displayMessage;
            boolean finalRequiresConfirmation = requiresConfirmation;
            String finalSqlQuery = sqlQuery;

            Platform.runLater(() -> {
                // Arrêt du spinner
                if (statusContainer != null) {
                    statusContainer.setVisible(false);
                    statusContainer.setManaged(false);
                    statusIcon.getStyleClass().remove("spinning");
                }
                
                setGeneratingState(false);

                // Animation Rich Text (Machine à écrire + Code highlighting)
                animateText(finalDisplayMessage, () -> {
                    if (finalRequiresConfirmation) {
                        this.pendingSqlQuery = finalSqlQuery;
                        if (confirmationBox != null) {
                            confirmationBox.setVisible(true);
                            confirmationBox.setManaged(true);
                        }
                        scrollToBottom();
                    }
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> handleFinalResponse("❌ Internal Error: " + e.getMessage(), true));
        }
    }

    // --- ACTIONS DE CONFIRMATION (AVEC FEEDBACK CAPSULES) ---

    @FXML
    private void handleConfirm() {
        if (pendingSqlQuery != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("action", "CONFIRM");
            payload.addProperty("sql", pendingSqlQuery);
            
            if (webSocket != null) webSocket.sendText(gson.toJson(payload), true);

            // Feedback Visuel (Capsule Verte)
            addFeedbackMessage("Action confirmed successfully.", true);
        }
        hideConfirmation();
    }

    @FXML
    private void handleCancel() {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "CANCEL");
        
        if (webSocket != null) webSocket.sendText(gson.toJson(payload), true);

        // Feedback Visuel (Capsule Rouge)
        addFeedbackMessage("Action cancelled by user.", false);
        hideConfirmation();
    }
    
    private void hideConfirmation() {
        if (confirmationBox != null) {
            confirmationBox.setVisible(false);
            confirmationBox.setManaged(false);
        }
        pendingSqlQuery = null;
    }

    /**
     * Ajoute un message stylisé (Capsule/Badge) dans le flux de réponse.
     */
    private void addFeedbackMessage(String message, boolean isSuccess) {
        // Création de l'icône SVG
        SVGPath icon = new SVGPath();
        if (isSuccess) {
            // Check mark
            icon.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z");
            icon.getStyleClass().add("icon-success");
        } else {
            // X mark (ou Warning)
            icon.setContent("M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z");
            icon.getStyleClass().add("icon-error");
        }

        // Création du Label (Capsule)
        Label feedbackLabel = new Label(message);
        feedbackLabel.setGraphic(icon); 
        feedbackLabel.getStyleClass().add(isSuccess ? "msg-success" : "msg-error");
        
        // Ajout d'un saut de ligne avant
        aiResponseFlow.getChildren().add(new Text("\n\n"));
        aiResponseFlow.getChildren().add(feedbackLabel);
        
        scrollToBottom();
    }

    // --- MOTEUR DE RENDU RICH TEXT (CODE & ANIMATION) ---

    private void initCursor() {
        if (cursorNode == null) {
            cursorNode = new Rectangle(2, 16); // Curseur type terminal
            cursorNode.getStyleClass().add("cursor-caret");
        }
    }

    private void animateText(String fullText, Runnable onFinished) {
        initCursor();
        // Reset du curseur
        aiResponseFlow.getChildren().remove(cursorNode);
        aiResponseFlow.getChildren().add(cursorNode);
        if (!cursorNode.getStyleClass().contains("cursor-blink")) {
            cursorNode.getStyleClass().add("cursor-blink");
        }

        Timeline timeline = new Timeline();
        // Regex complexe pour garder les espaces et sauts de ligne lors du split
        String[] words = fullText.split("(?=[\\s\\n])|(?<=[\\s\\n])"); 
        
        Duration delay = Duration.ZERO;
        for (String word : words) {
            KeyFrame kf = new KeyFrame(delay, e -> {
                aiResponseFlow.getChildren().remove(cursorNode);
                appendRichText(word);
                aiResponseFlow.getChildren().add(cursorNode);
            });
            timeline.getKeyFrames().add(kf);
            delay = delay.add(Duration.millis(15)); // Vitesse de frappe
        }
        
        timeline.setOnFinished(e -> {
            aiResponseFlow.getChildren().remove(cursorNode);
            if (onFinished != null) onFinished.run();
        });
        timeline.play();
    }

    private void appendRichText(String chunk) {
        // 1. Retour ligne
        if (chunk.equals("\n")) {
            aiResponseFlow.getChildren().add(new Text("\n"));
            return;
        }

        // 2. Nettoyage Markdown basique
        String text = chunk.replace("- ", "• ").replace("* ", "• ");
        
        // 3. Détection CODE PILL (`table`)
        if (text.contains("`")) {
            String cleanCode = text.replace("`", "").trim();
            if (!cleanCode.isEmpty()) {
                Label codeLabel = new Label(cleanCode);
                codeLabel.getStyleClass().add("ai-code-pill");
                aiResponseFlow.getChildren().add(codeLabel);
                aiResponseFlow.getChildren().add(new Text(" ")); 
            }
            return;
        }

        // 4. Coloration Syntaxique SQL (Safe vs Danger)
        String upper = text.toUpperCase().trim();
        Text textNode = new Text(text);
        
        if (upper.matches("DELETE|DROP|REMOVE|TRUNCATE")) {
            textNode.getStyleClass().add("sql-keyword-danger");
        } 
        else if (upper.matches("SELECT|INSERT|UPDATE|CREATE|READ")) {
            textNode.getStyleClass().add("sql-keyword-safe");
        }
        else if (upper.matches("WHERE|FROM|AND|OR|JOIN|LIMIT|ORDER|BY|SET|VALUES")) {
            textNode.getStyleClass().add("sql-keyword-logic");
        }
        else {
            // 5. Styles Markdown standards (Gras et Titres)
            if (text.contains("**")) {
                textNode.setText(text.replace("**", ""));
                isBoldMode = !isBoldMode; 
                textNode.getStyleClass().add("ai-text-bold");
            } else if (text.startsWith("###")) {
                textNode.setText(text.replace("###", "") + "\n");
                textNode.getStyleClass().add("ai-text-h3");
            } else {
                textNode.getStyleClass().add(isBoldMode ? "ai-text-bold" : "ai-text-normal");
            }
        }

        aiResponseFlow.getChildren().add(textNode);
        scrollToBottom();
    }
    
    private void handleFinalResponse(String text, boolean isError) {
        aiResponseFlow.getChildren().clear(); 
        appendRichText(text); 
    }

    private void scrollToBottom() {
        responseScroll.layout();
        responseScroll.setVvalue(1.0);
    }

    private void setGeneratingState(boolean generating) {
        this.isGenerating = generating;
        Platform.runLater(() -> {
            if (sendButton.getGraphic() instanceof SVGPath) {
                SVGPath icon = (SVGPath) sendButton.getGraphic();
                if (generating) {
                    icon.setContent(ICON_STOP);
                    sendButton.getStyleClass().remove("btn-send-gradient");
                    sendButton.getStyleClass().add("btn-stop-mode");
                } else {
                    icon.setContent(ICON_SEND);
                    sendButton.getStyleClass().remove("btn-stop-mode");
                    sendButton.getStyleClass().add("btn-send-gradient");
                }
            }
        });
    }

    // --- LISTENERS & TRANSITIONS ---

    private class WebSocketListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String text = data.toString();
            if (text.startsWith("{")) processIncomingJson(text);
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("❌ Connection Error");
                setGeneratingState(false);
            });
        }
    }

    private void performSearchTransition() {
        FadeTransition fadeOutHome = new FadeTransition(Duration.millis(300), homeContainer);
        fadeOutHome.setToValue(0);
        fadeOutHome.setOnFinished(e -> {
            homeContainer.setVisible(false);
            homeContainer.setManaged(false);
            resultContainer.setVisible(true);
            resultContainer.setManaged(true);
            resultContainer.setOpacity(0);
            FadeTransition fadeInResult = new FadeTransition(Duration.millis(400), resultContainer);
            fadeInResult.setToValue(1);
            fadeInResult.play();
        });
        fadeOutHome.play();
        double targetY = (aiRoot.getHeight() / 2) - 80; 
        TranslateTransition moveSearchDown = new TranslateTransition(Duration.millis(500), searchContainer);
        moveSearchDown.setToY(targetY);
        moveSearchDown.setInterpolator(Interpolator.EASE_BOTH);
        moveSearchDown.play();
    }
    
    @FXML
    private void resetView() {
        if(isGenerating && webSocket != null) webSocket.sendText("__STOP__", true);
        setGeneratingState(false);
        
        FadeTransition fadeOutResult = new FadeTransition(Duration.millis(300), resultContainer);
        fadeOutResult.setToValue(0);
        fadeOutResult.setOnFinished(e -> {
            resultContainer.setVisible(false);
            resultContainer.setManaged(false);
            homeContainer.setVisible(true);
            homeContainer.setManaged(true);
            homeContainer.setOpacity(0);
            FadeTransition fadeInHome = new FadeTransition(Duration.millis(400), homeContainer);
            fadeInHome.setToValue(1);
            fadeInHome.play();
            typeTitleAnimation();
        });
        fadeOutResult.play();
        TranslateTransition moveSearchUp = new TranslateTransition(Duration.millis(500), searchContainer);
        moveSearchUp.setToY(0);
        moveSearchUp.setInterpolator(Interpolator.EASE_BOTH);
        moveSearchUp.play();
        userInput.clear();
    }
    
    @FXML
    private void handleSuggestion(ActionEvent event) {
        if (event.getSource() instanceof Node) {
            Node node = (Node) event.getSource();
            Object data = node.getUserData();
            if (data != null) {
                userInput.setText(data.toString());
                userInput.requestFocus();
            }
        }
    }
    
    private void setupAutoGrowingTextArea() {
        userInput.setPrefHeight(40);
        userInput.textProperty().addListener((obs, oldVal, newVal) -> {
            Text textNode = new Text(userInput.getText());
            textNode.setFont(userInput.getFont());
            textNode.setWrappingWidth(userInput.getWidth() - 20);
            double height = textNode.getLayoutBounds().getHeight();
            userInput.setPrefHeight(Math.max(40, Math.min(height + 20, 150)));
        });
        userInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                handleSendMessage();
            }
        });
    }
    
    private void typeTitleAnimation() {
        typingTitle.setText("");
        Timeline timeline = new Timeline();
        for (int i = 0; i <= TITLE_TEXT.length(); i++) {
            final int index = i;
            KeyFrame frame = new KeyFrame(Duration.millis(30 * i + 200), e -> typingTitle.setText(TITLE_TEXT.substring(0, index)));
            timeline.getKeyFrames().add(frame);
        }
        timeline.play();
    }
}