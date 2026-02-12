package com.smarttask.client.service;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.Timer;
import java.util.TimerTask;

public class AudioService {

    private final AudioStreamPlayer audioPlayer;
    private AudioCaptureService captureService;
    private AIService aiService;
    
    private boolean isVocalMode = false;
    private Label vocalStatusLabel; // Référence pour mise à jour du statut
    private Timer processingTimeout; // ⚡ Watchdog pour récupération auto
    private static final long PROCESSING_TIMEOUT_MS = 15000; // 15 secondes

    public AudioService() {
        this.audioPlayer = new AudioStreamPlayer();
        
        // --- LOGIQUE DE SYNCHRONISATION ---
        // Ce callback est déclenché par le Player quand line.drain() est fini.
        this.audioPlayer.setOnPlaybackFinished(() -> {
            System.out.println("🔄 [FLOW] onPlaybackFinished -> Réactivation Micro");
            if (isVocalMode) {
                // On doit revenir sur le thread UI pour toucher aux Labels
                Platform.runLater(() -> {
                    updateVocalStatus(vocalStatusLabel, "LISTENING");
                    resumeCapture(); // Le micro s'ouvre MAINTENANT (safe)
                });
            }
        });
    }
    
    public void setAIService(AIService aiService) {
        this.aiService = aiService;
        
        this.captureService = new AudioCaptureService(
            // Callback 1: Envoi des chunks audio
            chunk -> {
                if (this.aiService != null) {
                    this.aiService.sendAudioChunk(chunk);
                }
            },
            // Callback 2: Détection de silence (User a fini de parler)
            () -> {
                System.out.println("🔇 Silence détecté - Fin de phrase");
                
                if (captureService != null) {
                    captureService.stopCapture();
                }
                
                if (vocalStatusLabel != null) {
                    updateVocalStatus(vocalStatusLabel, "PROCESSING");
                }
                
                // ⚡ Démarrer le watchdog de récupération
                startProcessingTimeout();
                
                if (this.aiService != null) {
                    this.aiService.sendAudioEnd();
                }
            }
        );
    }
    
    /**
     * Appelé par le Controller quand le message AUDIO_END est reçu du serveur.
     * Cela signifie que tout l'audio a été téléchargé, mais pas forcément joué.
     */
    public void notifyServerAudioEnd() {
        System.out.println("🔔 [FLOW] notifyServerAudioEnd -> insertion poison pill");
        // ⚡ Annuler le watchdog car on a bien reçu la réponse
        cancelProcessingTimeout();
        // On dit au player : "C'était le dernier paquet, préviens-moi quand tu as fini."
        audioPlayer.finish();
    }

    /**
     * Démarre un watchdog qui auto-récupère si le serveur ne répond pas.
     */
    private void startProcessingTimeout() {
        cancelProcessingTimeout();
        processingTimeout = new Timer("Processing-Timeout", true);
        processingTimeout.schedule(new TimerTask() {
            @Override
            public void run() {
                System.err.println("⏰ [TIMEOUT] Pas de réponse serveur depuis " + PROCESSING_TIMEOUT_MS + "ms - Auto-récupération");
                Platform.runLater(() -> {
                    if (isVocalMode) {
                        updateVocalStatus(vocalStatusLabel, "LISTENING");
                        resumeCapture();
                    }
                });
            }
        }, PROCESSING_TIMEOUT_MS);
    }

    private void cancelProcessingTimeout() {
        if (processingTimeout != null) {
            processingTimeout.cancel();
            processingTimeout = null;
        }
    }

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
            audioPlayer.start();
            // Démarrage initial
            if (captureService != null) captureService.startCapture();
        } else {
            if (captureService != null) captureService.stopCapture();
        }
        
        return isVocalMode;
    }

    public void resumeCapture() {
        if (captureService != null) {
            System.out.println("🎤 Reprise de la capture microphone");
            captureService.startCapture();
        }
    }

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
                    if (!pulseCircle.getStyleClass().contains("pulse-animation")) {
                        pulseCircle.getStyleClass().add("pulse-animation");
                    }
                }
            } else {
                if (pulseCircle != null) {
                    pulseCircle.getStyleClass().remove("pulse-animation");
                }
                stop(); 
            }
        });
    }
    
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