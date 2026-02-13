package com.smarttask.client.view.controller;

import com.google.gson.Gson;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.User;
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
import com.smarttask.client.config.AppConfig;

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
    private final String ICON_SEND = "M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z";
    private final String ICON_STOP = "M6 6h12v12H6z";

    // --- CONSTANTES SVG (MATERIAL DESIGN) ---
    private static final String SVG_DELETE = "M6 19c0 1.1.9 2 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"; // Poubelle
    private static final String SVG_WARNING = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"; // Triangle Attention
    private static final String SVG_CREATE = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"; // Plus
    private static final String SVG_SUCCESS = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"; // Check

    // --- OBJETS UTILITAIRES ---
    private WebSocket webSocket;
    private final Gson gson = GsonUtils.getGson();
    
    // --- ÉTAT ---
    private boolean isGenerating = false;
    private boolean isBoldMode = false;
    private String pendingSqlQuery = null;
    private Rectangle cursorNode; // Le curseur clignotant
    
    // Drapeau pour gérer le flux de confirmation (Nettoyage écran)
    private boolean waitingForConfirmationResult = false; 

    private User currentUser;

    @FXML
    public void initialize() {
        this.currentUser = SessionManager.getInstance().getCurrentUser();
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
                .buildAsync(URI.create(AppConfig.AI_WS_URL), new WebSocketListener())
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
        waitingForConfirmationResult = false; // Reset du state
        
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
            payload.addProperty("userId", currentUser.getId());
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

            // [NOUVEAU] Récupération du type d'opération pour l'icône
            String opType = "";
            if (response.has("operation_type") && !response.get("operation_type").isJsonNull()) {
                opType = response.get("operation_type").getAsString();
            }

            // Variables finales pour le thread UI
            String finalDisplayMessage = displayMessage;
            boolean finalRequiresConfirmation = requiresConfirmation;
            String finalSqlQuery = sqlQuery;
            String finalOpType = opType;

            Platform.runLater(() -> {
                // Arrêt du spinner
                if (statusContainer != null) {
                    statusContainer.setVisible(false);
                    statusContainer.setManaged(false);
                    statusIcon.getStyleClass().remove("spinning");
                }
                
                setGeneratingState(false);

                // --- GESTION DU NETTOYAGE APRES CONFIRMATION ---
                if (waitingForConfirmationResult) {
                    // On efface tout le texte précédent pour afficher le résultat propre
                    aiResponseFlow.getChildren().clear();
                    waitingForConfirmationResult = false; 
                    
                    // Si on revient d'une confirmation sans type précis, on affiche SUCCESS par défaut
                    if (finalOpType.isEmpty()) {
                         aiResponseFlow.getChildren().add(createStyledIcon("SUCCESS"));
                    }
                }

                // --- INJECTION DE L'ICÔNE SVG ---
                if (!finalOpType.isEmpty()) {
                    Node iconNode = createStyledIcon(finalOpType);
                    if (iconNode != null) {
                        aiResponseFlow.getChildren().add(iconNode);
                        aiResponseFlow.getChildren().add(new Text("\n")); // Petit saut de ligne
                    }
                }

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

    // --- ACTIONS DE CONFIRMATION ---

    @FXML
    private void handleConfirm() {
        if (pendingSqlQuery != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("action", "CONFIRM");
            payload.addProperty("sql", pendingSqlQuery);
            
            if (webSocket != null) webSocket.sendText(gson.toJson(payload), true);

            // --- FEEDBACK D'ATTENTE ---
            // 1. Ajouter le message "Executing..." au bas du texte actuel
            Text executingText = new Text("\n\n⚙️ Executing operation...");
            executingText.setStyle("-fx-fill: #aaaaaa; -fx-font-style: italic;");
            aiResponseFlow.getChildren().add(executingText);
            scrollToBottom();

            // 2. Activer le mode "attente" pour déclencher le clear() au prochain message
            waitingForConfirmationResult = true;
        }
        hideConfirmation();
    }

    @FXML
    private void handleCancel() {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "CANCEL");
        
        if (webSocket != null) webSocket.sendText(gson.toJson(payload), true);

        // Feedback simple pour l'annulation
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
     * Crée une icône SVG stylisée selon le type d'opération.
     */
    private Node createStyledIcon(String type) {
        StackPane container = new StackPane();
        // CHANGEMENT ICI : "chat-icon-container" au lieu de "card-icon-container"
        container.getStyleClass().add("chat-icon-container");

        SVGPath svg = new SVGPath();
        // CHANGEMENT ICI : "chat-icon-svg" au lieu de "card-svg-path"
        svg.getStyleClass().add("chat-icon-svg"); 

        switch (type.toUpperCase()) {
            case "DELETE":
            case "REMOVE":
                svg.setContent(SVG_DELETE);
                svg.getStyleClass().add("icon-danger");
                container.getStyleClass().add("container-danger");
                break;
            case "UPDATE":
            case "WARNING":
                svg.setContent(SVG_WARNING);
                svg.getStyleClass().add("icon-warning");
                container.getStyleClass().add("container-warning");
                break;
            case "CREATE":
                svg.setContent(SVG_CREATE);
                svg.getStyleClass().add("icon-success");
                container.getStyleClass().add("container-success");
                break;
            case "SUCCESS":
                svg.setContent(SVG_SUCCESS);
                svg.getStyleClass().add("icon-success");
                container.getStyleClass().add("container-success");
                break;
            default:
                return null;
        }
        
        container.getChildren().add(svg);
        HBox wrapper = new HBox(container);
        wrapper.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 0 0 10 0;");
        return wrapper;
    }

    private void addFeedbackMessage(String message, boolean isSuccess) {
        // Fallback pour les messages simples (ex: Cancel)
        Node icon = createStyledIcon(isSuccess ? "SUCCESS" : "DELETE"); // On réutilise la logique
        
        Label feedbackLabel = new Label(" " + message);
        feedbackLabel.getStyleClass().add(isSuccess ? "msg-success" : "msg-error");
        
        HBox container = new HBox(feedbackLabel);
        container.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 5px 0 0 0;");

        aiResponseFlow.getChildren().add(new Text("\n\n"));
        aiResponseFlow.getChildren().add(container);
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
        aiResponseFlow.getChildren().remove(cursorNode);
        aiResponseFlow.getChildren().add(cursorNode);
        if (!cursorNode.getStyleClass().contains("cursor-blink")) {
            cursorNode.getStyleClass().add("cursor-blink");
        }

        Timeline timeline = new Timeline();
        String[] words = fullText.split("(?=[\\s\\n])|(?<=[\\s\\n])"); 
        
        Duration delay = Duration.ZERO;
        for (String word : words) {
            KeyFrame kf = new KeyFrame(delay, e -> {
                aiResponseFlow.getChildren().remove(cursorNode);
                appendRichText(word);
                aiResponseFlow.getChildren().add(cursorNode);
            });
            timeline.getKeyFrames().add(kf);
            delay = delay.add(Duration.millis(15));
        }
        
        timeline.setOnFinished(e -> {
            aiResponseFlow.getChildren().remove(cursorNode);
            if (onFinished != null) onFinished.run();
        });
        timeline.play();
    }

    private void appendRichText(String chunk) {
        if (chunk.equals("\n")) {
            aiResponseFlow.getChildren().add(new Text("\n"));
            return;
        }

        String text = chunk.replace("- ", "• ").replace("* ", "• ");
        
        // Détection Code Pill
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

        // Coloration Syntaxique SQL
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
        else if (upper.matches("WARNING|ATTENTION")) {
             textNode.getStyleClass().add("sql-keyword-logic"); // Jaune/Orange
        }
        else {
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