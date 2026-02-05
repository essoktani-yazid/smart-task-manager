package com.smarttask.client.view.controller;

import com.google.gson.Gson;
import com.smarttask.client.util.GsonUtils;
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
    
    // Zone de confirmation (Cachée par défaut)
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
    private final Gson gson = GsonUtils.getGson();
    
    // --- ÉTAT ---
    private Text statusNode; // Le texte qui affiche "Thinking..." ou "Reading DB..."
    private boolean isGenerating = false;
    private boolean isBoldMode = false;
    private String pendingSqlQuery = null; // Stocke la requête en attente de validation

    @FXML
    public void initialize() {
        // 1. Initialisation de l'interface
        homeContainer.setVisible(true);
        resultContainer.setVisible(false);
        resultContainer.setManaged(false);
        
        if (confirmationBox != null) {
            confirmationBox.setVisible(false);
            confirmationBox.setManaged(false);
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

        // Si on est déjà en train de générer, le bouton sert de STOP
        if (isGenerating) {
            if (webSocket != null) {
                webSocket.sendText("__STOP__", true);
            }
            // On ne retourne pas, on permet à l'utilisateur d'enchaîner une nouvelle question directement
        }

        // Reset de l'UI pour la nouvelle question
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

        // Affichage du statut initial
        statusNode = new Text("Thinking...");
        statusNode.getStyleClass().add("ai-text-italic");
        aiResponseFlow.getChildren().add(statusNode);

        // Transition visuelle
        performSearchTransition();
        setGeneratingState(true);

        // Envoi au Backend
        if (webSocket != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("prompt", message);
            webSocket.sendText(gson.toJson(payload), true);
        } else {
            // Tentative de reconnexion si perdu
            handleFinalResponse("⚠️ Service disconnected. Reconnecting...", true);
            connectToAiService();
            setGeneratingState(false);
        }
    }

    // --- RÉCEPTION ET TRAITEMENT DES MESSAGES ---

    private void processIncomingJson(String jsonString) {
        System.out.println("DEBUG JSON: " + jsonString);

        try {
            JsonObject response = gson.fromJson(jsonString, JsonObject.class);

            // CAS 1 : Mise à jour de statut (ex: "Analyzing intent...")
            if (response.has("status")) {
                String statusText = response.get("status").getAsString();
                Platform.runLater(() -> {
                    if (statusNode != null) {
                        statusNode.setText("🤖 " + statusText);
                    }
                });
                return; // On s'arrête là, ce n'est pas encore la réponse finale
            }

            // CAS 2 : Réponse finale ou demande de confirmation
            String displayMessage = response.has("display_message") ? response.get("display_message").getAsString() : "";
            boolean requiresConfirmation = response.has("requires_confirmation") && response.get("requires_confirmation").getAsBoolean();
            String sqlQuery = response.has("sql_to_execute") ? response.get("sql_to_execute").getAsString() : "";

            Platform.runLater(() -> {
                // On supprime le texte de statut
                aiResponseFlow.getChildren().remove(statusNode);
                statusNode = null;
                
                // La génération est finie (ou en pause pour confirmation)
                setGeneratingState(false);

                // Animation type "Machine à écrire"
                animateText(displayMessage, () -> {
                    // Si une confirmation est requise, on affiche les boutons APRÈS le texte
                    if (requiresConfirmation) {
                        this.pendingSqlQuery = sqlQuery;
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
        }
    }

    // --- ACTIONS DE CONFIRMATION ---

    @FXML
    private void handleConfirm() {
        if (pendingSqlQuery != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("action", "CONFIRM");
            payload.addProperty("sql", pendingSqlQuery);
            
            if (webSocket != null) {
                webSocket.sendText(gson.toJson(payload), true);
            }

            // Feedback immédiat
            Text successText = new Text("\n\n✅ Confirmation envoyée.");
            successText.getStyleClass().add("ai-text-success");
            aiResponseFlow.getChildren().add(successText);
        }
        hideConfirmation();
    }

    @FXML
    private void handleCancel() {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "CANCEL");
        
        if (webSocket != null) {
            webSocket.sendText(gson.toJson(payload), true);
        }

        Text cancelText = new Text("\n\n❌ Opération annulée.");
        cancelText.getStyleClass().add("ai-text-error");
        aiResponseFlow.getChildren().add(cancelText);
        
        hideConfirmation();
    }
    
    private void hideConfirmation() {
        if (confirmationBox != null) {
            confirmationBox.setVisible(false);
            confirmationBox.setManaged(false);
        }
        pendingSqlQuery = null;
    }

    // --- UTILITAIRES D'AFFICHAGE (MARKDOWN & ANIMATION) ---

    private void animateText(String fullText, Runnable onFinished) {
        Timeline timeline = new Timeline();
        // Découpe par espace pour afficher mot par mot
        String[] words = fullText.split("(?<=\\s)"); 
        
        Duration delay = Duration.ZERO;
        for (String word : words) {
            KeyFrame kf = new KeyFrame(delay, e -> appendMarkdown(word, false));
            timeline.getKeyFrames().add(kf);
            delay = delay.add(Duration.millis(15)); // Vitesse de frappe (ms)
        }
        
        timeline.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        timeline.play();
    }

    private void appendMarkdown(String chunk, boolean isError) {
        if (isError) {
            Text errorText = new Text(chunk);
            errorText.getStyleClass().add("ai-text-error");
            aiResponseFlow.getChildren().add(errorText);
            scrollToBottom();
            return;
        }

        // Remplacement basique des puces Markdown
        String processedChunk = chunk.replace("\n- ", "\n • ").replace("\n* ", "\n • ");
        if (processedChunk.startsWith("- ")) processedChunk = " • " + processedChunk.substring(2);

        // Détection du gras (**text**)
        String[] parts = processedChunk.split("(\\*\\*)", -1);

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                // Si on tombe sur un délimiteur vide, on bascule le mode gras
                if (i < parts.length - 1) isBoldMode = !isBoldMode;
                continue;
            }

            Text textNode = new Text();
            // Détection des titres (### Titre)
            if (part.contains("###") || part.trim().startsWith("###")) {
                textNode.setText(part.replace("###", "").trim() + "\n");
                textNode.getStyleClass().add("ai-text-h3");
            } else {
                textNode.setText(part);
                textNode.getStyleClass().add(isBoldMode ? "ai-text-bold" : "ai-text-normal");
            }
            aiResponseFlow.getChildren().add(textNode);
            
            // Bascule après chaque segment
            if (i < parts.length - 1) isBoldMode = !isBoldMode;
        }
        scrollToBottom();
    }
    
    private void handleFinalResponse(String text, boolean isError) {
        aiResponseFlow.getChildren().clear(); 
        appendMarkdown(text, isError);
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
            
            // On ignore les messages vides ou spéciaux
            if (text.startsWith("{")) {
                processIncomingJson(text);
            }
            // Note: On ne gère pas <END> ici car le JSON complet suffit
            
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            Platform.runLater(() -> {
                if (statusNode != null) statusNode.setText("❌ Erreur de connexion");
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