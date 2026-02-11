package com.smarttask.client.service;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class AudioService {

    private final AudioStreamPlayer audioPlayer;
    private boolean isVocalMode = false;

    public AudioService() {
        this.audioPlayer = new AudioStreamPlayer();
    }

    public void start() {
        audioPlayer.start();
    }

    public void stop() {
        audioPlayer.stop();
    }

    public void playChunk(byte[] data) {
        audioPlayer.enqueueAudio(data);
    }

    public boolean toggleVocalMode() {
        isVocalMode = !isVocalMode;
        return isVocalMode;
    }

    public boolean isVocalMode() {
        return isVocalMode;
    }

    // Gestion de l'UI Overlay Vocal
    public void updateOverlay(boolean active, VBox overlay, Label statusLabel, Circle pulseCircle) {
        if (overlay == null) return;
        
        Platform.runLater(() -> {
            overlay.setVisible(active);
            overlay.setManaged(active);
            
            if (active) {
                if (statusLabel != null) statusLabel.setText("Listening...");
                if (pulseCircle != null) pulseCircle.getStyleClass().add("pulse-animation");
                // TODO: Démarrer ici le vrai service VOSK/STT
                simulateListening(); 
            } else {
                if (pulseCircle != null) pulseCircle.getStyleClass().remove("pulse-animation");
                stop(); // Arrêter l'audio si on coupe le micro
            }
        });
    }

    private void simulateListening() {
        System.out.println("🎤 Micro ON (Service)");
    }
}