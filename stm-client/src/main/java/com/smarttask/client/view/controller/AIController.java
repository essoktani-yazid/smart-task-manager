package com.smarttask.client.view.controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class AIController {

    @FXML private VBox centerContainer;
    @FXML private HBox cardsContainer;
    @FXML private Label typingTitle;
    @FXML private TextArea userInput;
    @FXML private Circle blob1, blob2, blob3;

    // Titre en anglais comme demandé
    private final String TARGET_TEXT = "How can I supercharge your productivity?";

    @FXML
    public void initialize() {
        // 1. Animation des blobs d'arrière-plan
        startFloatingAnimation(blob1, 4);
        startFloatingAnimation(blob2, 5);
        startFloatingAnimation(blob3, 6);

        // 2. Animation d'entrée de la page
        playEntranceAnimation();

        // 3. Effet machine à écrire
        typeTextAnimation();

        // 4. Configuration de la TextArea
        setupAutoGrowingTextArea();
    }

    private void playEntranceAnimation() {
        // Le conteneur apparaît et monte légèrement
        centerContainer.setOpacity(0);
        centerContainer.setTranslateY(30);
        
        FadeTransition fade = new FadeTransition(Duration.millis(1000), centerContainer);
        fade.setFromValue(0); fade.setToValue(1);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(1000), centerContainer);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        // Animation en cascade pour les cartes
        animateCardsEntry();

        new ParallelTransition(fade, slide).play();
    }

    private void animateCardsEntry() {
        int delay = 0;
        for (Node card : cardsContainer.getChildren()) {
            card.setOpacity(0);
            card.setTranslateY(20);

            FadeTransition ft = new FadeTransition(Duration.millis(600), card);
            ft.setToValue(1);
            
            TranslateTransition tt = new TranslateTransition(Duration.millis(600), card);
            tt.setToY(0);
            tt.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition cardAnim = new ParallelTransition(ft, tt);
            cardAnim.setDelay(Duration.millis(800 + delay)); // Décalage progressif
            cardAnim.play();
            
            delay += 150;
        }
    }

    private void setupAutoGrowingTextArea() {
        // Hauteur initiale (40px correspond au padding CSS 10+10+20px texte)
        userInput.setPrefHeight(40); 

        // Listener pour ajuster la hauteur quand le texte change
        userInput.textProperty().addListener((obs, oldVal, newVal) -> adjustHeight());

        // Gestion Entrée (Envoyer) vs Shift+Entrée (Saut de ligne)
        userInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    // Laisser le comportement par défaut (nouvelle ligne)
                } else {
                    event.consume(); // Bloquer la nouvelle ligne
                    handleSendMessage();
                }
            }
        });
    }

    private void adjustHeight() {
        Text textNode = new Text(userInput.getText());
        textNode.setFont(userInput.getFont());
        textNode.setWrappingWidth(userInput.getWidth() - 20); // Marges internes

        double height = textNode.getLayoutBounds().getHeight();
        // Min 40px, Max 150px (scroll après)
        double newHeight = Math.max(40, Math.min(height + 20, 150));

        userInput.setPrefHeight(newHeight);
    }

    private void typeTextAnimation() {
        Timeline timeline = new Timeline();
        for (int i = 0; i <= TARGET_TEXT.length(); i++) {
            final int index = i;
            KeyFrame frame = new KeyFrame(Duration.millis(50 * i + 300), e -> {
                typingTitle.setText(TARGET_TEXT.substring(0, index));
            });
            timeline.getKeyFrames().add(frame);
        }
        timeline.play();
    }

    private void startFloatingAnimation(Circle blob, double durationSeconds) {
        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(durationSeconds), blob);
        floatAnim.setByY(40);
        floatAnim.setByX(20);
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.setInterpolator(Interpolator.EASE_BOTH);
        floatAnim.play();
    }

    @FXML
    private void handleSendMessage() {
        String message = userInput.getText();
        if (message == null || message.trim().isEmpty()) return;

        System.out.println("Processing AI Request: " + message);
        
        userInput.clear();
        userInput.setPrefHeight(40); // Reset hauteur
    }

    @FXML
    private void handleSuggestion(ActionEvent event) {
        if (event.getSource() instanceof Node) {
            Node node = (Node) event.getSource();
            Object data = node.getUserData();
            if (data != null) {
                userInput.setText(data.toString());
                userInput.requestFocus();
                userInput.positionCaret(userInput.getText().length());
            }
        }
    }
}