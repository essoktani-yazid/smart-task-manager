package com.smarttask.client.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.smarttask.client.config.AppConfig;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.model.User;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Base64;
import java.util.concurrent.CompletionStage;

public class AIService {

    private WebSocket webSocket;
    private final Gson gson = GsonUtils.getGson();
    private final AIResponseListener listener;
    private boolean isGenerating = false;

    public AIService(AIResponseListener listener) {
        this.listener = listener;
    }

    public void connect() {
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(AppConfig.AI_WS_URL), new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    listener.onConnectionStatus(true);
                })
                .exceptionally(ex -> {
                    listener.onError("WebSocket Error: " + ex.getMessage());
                    listener.onConnectionStatus(false);
                    return null;
                });
    }

    public void sendMessage(String prompt, User user) {
        if (webSocket == null) {
            listener.onError("Service disconnected.");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("prompt", prompt);
        
        // Gestion propre du typage Gson
        if (user != null) {
            payload.addProperty("userId", user.getId());
        } else {
            payload.addProperty("userId", 1);
        }

        webSocket.sendText(gson.toJson(payload), true);
        isGenerating = true;
    }

    public void sendAction(String action, String sql) {
        if (webSocket == null) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("action", action);
        if (sql != null) payload.addProperty("sql", sql);
        
        webSocket.sendText(gson.toJson(payload), true);
    }

    public void stopGeneration() {
        if (webSocket != null && isGenerating) {
            webSocket.sendText("__STOP__", true);
        }
        isGenerating = false;
    }

    public boolean isGenerating() {
        return isGenerating;
    }

    public void setGenerating(boolean generating) {
        this.isGenerating = generating;
    }

    // --- OPTIMISATION THREADING ---
    private class WebSocketListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String text = data.toString();
            
            // 1. TRAITEMENT LOURD EN ARRIÈRE-PLAN (Pas de lag UI)
            try {
                // Parsing JSON hors du thread UI
                JsonObject json = gson.fromJson(text, JsonObject.class);

                if (json.has("type")) {
                    String type = json.get("type").getAsString();
                    
                    // --- AUDIO (Zéro contact avec l'UI nécessaire) ---
                    if ("AUDIO_CHUNK".equals(type)) {
                        String base64Data = json.get("data").getAsString();
                        // Décodage Base64 hors du thread UI
                        byte[] pcmData = Base64.getDecoder().decode(base64Data);
                        
                        // On envoie direct au service audio (Thread-Safe)
                        // PAS DE Platform.runLater ICI ! C'est ce qui causait le lag.
                        listener.onAudioChunkReceived(pcmData); 
                        return null; 
                    }
                    
                    if ("AUDIO_END".equals(type)) {
                        listener.onAudioEnd();
                        return null;
                    }
                }

                // --- UI UPDATES (Seulement ici on appelle JavaFX) ---
                Platform.runLater(() -> {
                    listener.onMessageReceived(json);
                });

            } catch (Exception e) {
                Platform.runLater(() -> listener.onError("Error: " + e.getMessage()));
            }
            
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            Platform.runLater(() -> listener.onError("Connection Lost"));
        }
    }
}