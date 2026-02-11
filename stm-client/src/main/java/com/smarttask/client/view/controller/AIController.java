package com.smarttask.client.view.controller;

import com.google.gson.JsonObject;
import com.smarttask.client.service.AIResponseListener;
import com.smarttask.client.service.AIService;
import com.smarttask.client.service.AudioService;
import com.smarttask.client.view.renderer.UIRenderer;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.User;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public class AIController implements AIResponseListener {

    // --- FXML UI ---
    @FXML private StackPane aiRoot;
    @FXML private VBox homeContainer, resultContainer;
    @FXML private Label typingTitle, userQuestionLabel, statusLabel;
    @FXML private HBox cardsContainer, statusContainer, confirmationBox, searchContainer;
    @FXML private TextFlow aiResponseFlow;
    @FXML private ScrollPane responseScroll;
    @FXML private TextArea userInput;
    @FXML private Button sendButton, micButton;
    @FXML private SVGPath micIcon, statusIcon;
    @FXML private VBox vocalOverlay;
    @FXML private Label vocalStatusLabel;
    @FXML private Circle vocalPulseCircle;

    private boolean isVocalMode = false;

    // --- SERVICES & LOGIC ---
    private AIService aiService;
    private AudioService audioService;
    private UIRenderer renderer;
    private User currentUser;
    
    private String pendingSqlQuery = null;
    private boolean waitingForConfirmationResult = false;
    private final String TITLE_TEXT = "How can I supercharge your productivity?";

    @FXML
    public void initialize() {
        this.aiService = new AIService(this);
        this.audioService = new AudioService();
        this.audioService.setAIService(this.aiService);
        this.audioService.setVocalStatusLabel(vocalStatusLabel); // Injection pour feedback visuel
        this.renderer = new UIRenderer(aiResponseFlow, responseScroll);
        this.currentUser = SessionManager.getInstance().getCurrentUser();

        audioService.start();
        aiService.connect();

        homeContainer.setVisible(true);
        resultContainer.setVisible(false);
        resultContainer.setManaged(false);
        setupAutoGrowingTextArea();
        typeTitleAnimation();
        
        System.out.println("✅ AI Controller initialisé - Mode vocal prêt");
    }

    @Override
    public void onMessageReceived(JsonObject response) {
        updateStatus(false, null);
        
        try {
            if (response.has("status")) {
                updateStatus(true, response.get("status").getAsString());
                return;
            }

            String displayMessage = getSafeString(response, "display_message");
            String sqlQuery = getSafeString(response, "sql_to_execute");
            String opType = getSafeString(response, "operation_type");
            boolean requiresConfirm = response.has("requires_confirmation") && response.get("requires_confirmation").getAsBoolean();

            if (waitingForConfirmationResult) {
                renderer.clear();
                waitingForConfirmationResult = false;
                if (opType.isEmpty()) renderer.addIcon("SUCCESS");
            }

            renderer.addIcon(opType);
            
            renderer.animateText(displayMessage, () -> {
                // FIN DE L'ANIMATION : On remet le bouton SEND
                updateSendButton(false);
                
                if (requiresConfirm) {
                    this.pendingSqlQuery = sqlQuery;
                    toggleConfirmationBox(true);
                }
            });

        } catch (Exception e) {
            onError("UI Error: " + e.getMessage());
        }
    }

    @Override
    public void onAudioChunkReceived(byte[] audioData) {
        // Appelé depuis le Thread WebSocket, OK car AudioService gère sa file d'attente
        audioService.playChunk(audioData);
        
        // Feedback visuel: AI est en train de parler
        if (isVocalMode) {
            audioService.updateVocalStatus(vocalStatusLabel, "SPEAKING");
        }
    }

    @Override
    public void onAudioEnd() {
        // Log détaillé dans AIService (compteur chunks)
        if (isVocalMode) {
            // Petit délai pour laisser le temps aux derniers chunks audio d'arriver
            // (évite de repasser en mode écoute avant que l'audio soit bien en file)
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                javafx.application.Platform.runLater(() -> {
                    if (isVocalMode) {
                        audioService.notifyServerAudioEnd();
                    }
                });
            }, "AudioEnd-Delay").start();
        }
    }

    @Override
    public void onError(String message) {
        updateStatus(false, null);
        updateSendButton(false);
        renderer.addFeedback("Error: " + message, false);
    }

    @Override
    public void onConnectionStatus(boolean isConnected) {
        if (!isConnected) updateStatus(true, "Offline");
    }

    @FXML
    private void handleSendMessage() {
        String msg = userInput.getText();
        if (msg == null || msg.trim().isEmpty()) return;

        if (aiService.isGenerating()) {
            aiService.stopGeneration();
            audioService.stop();
            updateSendButton(false);
            return;
        }

        userQuestionLabel.setText(msg);
        renderer.clear();
        userInput.clear();
        userInput.setPrefHeight(40);
        pendingSqlQuery = null;
        toggleConfirmationBox(false);

        updateStatus(true, "Thinking...");
        performSearchTransition();
        updateSendButton(true);

        aiService.sendMessage(msg, currentUser);
    }

    @FXML
    private void handleMicToggle() {
        isVocalMode = !isVocalMode;
        boolean active = audioService.toggleVocalMode();
        
        if (active) {
            System.out.println("🎤 Mode vocal ACTIVÉ");
            micButton.getStyleClass().add("btn-mic-active");
            micIcon.setFill(Color.web("#ef4444"));
            audioService.updateVocalStatus(vocalStatusLabel, "LISTENING");
        } else {
            System.out.println("🎤 Mode vocal DÉSACTIVÉ");
            micButton.getStyleClass().remove("btn-mic-active");
            micIcon.setFill(Color.web("#6b7280"));
        }
        
        audioService.updateOverlay(active, vocalOverlay, vocalStatusLabel, vocalPulseCircle);
    }

    @FXML
    private void handleConfirm() {
        if (pendingSqlQuery != null) {
            aiService.sendAction("CONFIRM", pendingSqlQuery);
            renderer.addSystemText("\n\n⚙️ Executing operation...");
            waitingForConfirmationResult = true;
            updateSendButton(true);
        }
        toggleConfirmationBox(false);
    }

    @FXML
    private void handleCancel() {
        aiService.sendAction("CANCEL", null);
        renderer.addFeedback("Cancelled by user.", false);
        toggleConfirmationBox(false);
    }

    @FXML
    private void resetView() {
        aiService.stopGeneration();
        audioService.stop();
        updateSendButton(false);
        
        // Reset Barre de recherche
        TranslateTransition moveSearchUp = new TranslateTransition(Duration.millis(500), searchContainer);
        moveSearchUp.setToY(0);
        moveSearchUp.setInterpolator(Interpolator.EASE_BOTH);
        moveSearchUp.play();

        // ⚡ OPTIMISÉ: Animations plus rapides (300ms -> 150ms)
        FadeTransition fadeOutResult = new FadeTransition(Duration.millis(150), resultContainer);
        fadeOutResult.setFromValue(1.0);
        fadeOutResult.setToValue(0.0);
        fadeOutResult.setOnFinished(e -> {
            resultContainer.setVisible(false);
            resultContainer.setManaged(false);
            homeContainer.setVisible(true);
            homeContainer.setManaged(true);
            homeContainer.setOpacity(0);
            FadeTransition fadeInHome = new FadeTransition(Duration.millis(200), homeContainer);
            fadeInHome.setToValue(1);
            fadeInHome.play();
            typeTitleAnimation();
        });
        fadeOutResult.play();
        
        userInput.clear();
    }
    
    @FXML
    private void handleSuggestion(ActionEvent event) {
        if (event.getSource() instanceof Node) {
            Object data = ((Node) event.getSource()).getUserData();
            if (data != null) {
                userInput.setText(data.toString());
                userInput.requestFocus();
            }
        }
    }

    private void updateStatus(boolean show, String text) {
        Platform.runLater(() -> {
            if (statusContainer == null) return;
            statusContainer.setVisible(show);
            statusContainer.setManaged(show);
            if (show) {
                statusLabel.setText(text);
                if (!statusIcon.getStyleClass().contains("spinning")) statusIcon.getStyleClass().add("spinning");
            } else {
                statusIcon.getStyleClass().remove("spinning");
            }
        });
    }

    private void updateSendButton(boolean generating) {
        Platform.runLater(() -> {
            aiService.setGenerating(generating);
            if (!(sendButton.getGraphic() instanceof SVGPath)) return;
            SVGPath icon = (SVGPath) sendButton.getGraphic();
            if (generating) {
                icon.setContent("M6 6h12v12H6z"); 
                sendButton.getStyleClass().replaceAll(s -> s.equals("btn-send-gradient") ? "btn-stop-mode" : s);
            } else {
                icon.setContent("M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z");
                sendButton.getStyleClass().replaceAll(s -> s.equals("btn-stop-mode") ? "btn-send-gradient" : s);
            }
        });
    }

    private void toggleConfirmationBox(boolean show) {
        if (confirmationBox == null) return;
        confirmationBox.setVisible(show);
        confirmationBox.setManaged(show);
    }

    private void performSearchTransition() {
        if (!homeContainer.isVisible() && resultContainer.isVisible()) return;

        // ⚡ OPTIMISÉ: Animations plus rapides (300ms -> 150ms, 500ms -> 300ms)
        FadeTransition fadeOutHome = new FadeTransition(Duration.millis(150), homeContainer);
        fadeOutHome.setToValue(0);
        fadeOutHome.setOnFinished(e -> {
            homeContainer.setVisible(false);
            homeContainer.setManaged(false);
            resultContainer.setVisible(true);
            resultContainer.setManaged(true);
            resultContainer.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), resultContainer);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOutHome.play();
        
        double parentHeight = aiRoot.getHeight();
        if (parentHeight == 0) parentHeight = 600;
        double targetY = (parentHeight / 2) - 80;

        TranslateTransition moveSearchDown = new TranslateTransition(Duration.millis(300), searchContainer);
        moveSearchDown.setToY(targetY);
        moveSearchDown.setInterpolator(Interpolator.EASE_BOTH);
        moveSearchDown.play();
    }

    private void setupAutoGrowingTextArea() {
        userInput.textProperty().addListener((obs, oldVal, newVal) -> {
            Text textNode = new Text(userInput.getText());
            textNode.setFont(userInput.getFont());
            textNode.setWrappingWidth(userInput.getWidth() - 20);
            userInput.setPrefHeight(Math.max(40, Math.min(textNode.getLayoutBounds().getHeight() + 20, 150)));
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
            final int idx = i;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(30 * i + 200), e -> typingTitle.setText(TITLE_TEXT.substring(0, idx))));
        }
        timeline.play();
    }

    private String getSafeString(JsonObject json, String key) {
        return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : "";
    }
}