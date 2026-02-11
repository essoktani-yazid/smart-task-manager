package com.smarttask.client.service;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class AudioService {

    private final AudioStreamPlayer audioPlayer;
    private AudioCaptureService captureService; // Le nouveau service
    private AIService aiService; // Référence pour envoyer les données
    
    private boolean isVocalMode = false;

    public AudioService() {
        this.audioPlayer = new AudioStreamPlayer();
    }
    
    private Label vocalStatusLabel; // Référence pour mise à jour du statut
    
    // On doit injecter AIService pour pouvoir envoyer l'audio
    public void setAIService(AIService aiService) {
        this.aiService = aiService;
        
        // On initialise la capture ici avec callback pour les chunks et détection de silence
        this.captureService = new AudioCaptureService(
            // Callback 1: Envoi des chunks audio au serveur
            chunk -> {
                if (this.aiService != null) {
                    this.aiService.sendAudioChunk(chunk);
                }
            },
            // Callback 2: Détection de silence (utilisateur a fini de parler)
            () -> {
                System.out.println("🔇 Silence détecté - Fin de phrase détectée");
                // Mise à jour visuelle du statut
                if (vocalStatusLabel != null) {
                    updateVocalStatus(vocalStatusLabel, "PROCESSING");
                }
            }
        );
    }
    
    /**
     * Permet d'injecter la référence au label de statut pour les mises à jour
     */
    public void setVocalStatusLabel(Label label) {
        this.vocalStatusLabel = label;
    }

    public void start() {
        audioPlayer.start();
    }

    public void stop() {
        audioPlayer.stop();
        if (captureService != null) captureService.stopCapture();
    }

    public void playChunk(byte[] data) {
        audioPlayer.enqueueAudio(data);
    }

    public boolean toggleVocalMode() {
        isVocalMode = !isVocalMode;
        
        if (isVocalMode) {
            // Démarrer la vraie capture
            if (captureService != null) captureService.startCapture();
        } else {
            // Arrêter la capture
            if (captureService != null) captureService.stopCapture();
        }
        
        return isVocalMode;
    }

    public void resumeCapture() {
        if (captureService != null) {
            captureService.startCapture();
        }
    }

    /**
     * Met à jour l'overlay visuel du mode vocal avec animations améliorées
     */
    public void updateOverlay(boolean active, VBox overlay, Label statusLabel, Circle pulseCircle) {
        if (overlay == null) return;
        Platform.runLater(() -> {
            overlay.setVisible(active);
            overlay.setManaged(active);
            if (active) {
                if (statusLabel != null) {
                    statusLabel.setText("🎤 Listening...");
                    statusLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");
                }
                if (pulseCircle != null) {
                    pulseCircle.getStyleClass().add("pulse-animation");
                }
            } else {
                if (pulseCircle != null) {
                    pulseCircle.getStyleClass().remove("pulse-animation");
                }
                stop(); 
            }
        });
    }
    
    /**
     * Met à jour le statut vocal (Listening, Processing, Speaking, etc.)
     */
    public void updateVocalStatus(Label statusLabel, String status) {
        if (statusLabel == null) return;
        Platform.runLater(() -> {
            switch (status) {
                case "LISTENING":
                    statusLabel.setText("🎤 Listening...");
                    statusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    break;
                case "PROCESSING":
                    statusLabel.setText("🧠 Processing...");
                    statusLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                    break;
                case "SPEAKING":
                    statusLabel.setText("🔊 Speaking...");
                    statusLabel.setStyle("-fx-text-fill: #8b5cf6; -fx-font-weight: bold;");
                    break;
                case "SILENCE":
                    statusLabel.setText("🔇 Silence detected");
                    statusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: normal;");
                    break;
                default:
                    statusLabel.setText(status);
            }
        });
    }
}