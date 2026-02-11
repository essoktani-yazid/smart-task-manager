package com.smarttask.client.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.smarttask.client.config.AppConfig;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.model.User;
import javafx.application.Platform;

import java.nio.ByteBuffer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

public class AIService {

    private WebSocket webSocket;
    private final Gson gson = GsonUtils.getGson();
    private final AIResponseListener listener;
    private boolean isGenerating = false;
    private final AtomicInteger binaryChunkCount = new AtomicInteger(0);  // [FLOW] Compteur chunks reçus

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

    public void sendAudioChunk(byte[] audioData) {
        if (webSocket == null) {
            System.err.println("⚠️ Impossible d'envoyer audio: WebSocket non connecté");
            return;
        }
        
        if (audioData.length == 0) {
            System.err.println("⚠️ Chunk audio vide, ignoré");
            return;
        }
        
        try {
            // sendBinary est la méthode native de Java 11 HttpClient pour les octets
            System.out.println("🌐 [AIService] Envoi WebSocket Binary: " + audioData.length + " bytes");
            webSocket.sendBinary(java.nio.ByteBuffer.wrap(audioData), true);
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi audio chunk: " + e.getMessage());
            e.printStackTrace();
            listener.onError("Audio transmission error");
        }
    }

    public void sendAudioEnd() {
        if (webSocket == null) {
            System.err.println("⚠️ Impossible d'envoyer signal fin audio: WebSocket non connecté");
            return;
        }
        
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("action", "AUDIO_END");
            
            String jsonMessage = gson.toJson(payload);
            System.out.println("🔚 [AIService] Envoi signal fin audio");
            webSocket.sendText(jsonMessage, true);
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi signal fin audio: " + e.getMessage());
            e.printStackTrace();
        }
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
                    
                    // --- AUDIO_END (audio TTS terminé côté serveur) ---
                    if ("AUDIO_END".equals(type)) {
                        int total = binaryChunkCount.get();
                        System.out.println("⬇️ [FLOW] AUDIO_END reçu | Chunks binaires reçus avant: " + total);
                        if (total == 0) {
                            System.err.println("⚠️ [FLOW] AUCUN chunk audio reçu avant AUDIO_END - mic va se réactiver sans lecture");
                        }
                        binaryChunkCount.set(0);  // Reset pour prochaine réponse
                        listener.onAudioEnd();
                        webSocket.request(1); // ⚡ CRITICAL: demander le prochain message
                        return null;
                    }
                }

                // --- UI UPDATES (Seulement ici on appelle JavaFX) ---
                if (json.has("display_message")) {
                    System.out.println("⬇️ [FLOW] Message texte reçu (display_message)");
                }
                Platform.runLater(() -> {
                    listener.onMessageReceived(json);
                });

            } catch (Exception e) {
                Platform.runLater(() -> listener.onError("Error: " + e.getMessage()));
            }
            
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            // Audio TTS reçu du serveur (format binaire PCM)
            try {
                int size = data.remaining();
                if (size <= 0) {
                    System.err.println("⚠️ [FLOW] onBinary: chunk vide ignoré");
                    return WebSocket.Listener.super.onBinary(webSocket, data, last);
                }
                byte[] pcmData = new byte[size];
                data.get(pcmData);
                binaryChunkCount.incrementAndGet();
                if (binaryChunkCount.get() <= 3 || binaryChunkCount.get() % 20 == 0) {
                    System.out.println("⬇️ [FLOW] BINARY reçu #" + binaryChunkCount.get() + " (" + size + " bytes)");
                }
                listener.onAudioChunkReceived(pcmData);
            } catch (Exception e) {
                System.err.println("❌ [FLOW] Erreur onBinary: " + e.getMessage());
                e.printStackTrace();
            }
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("❌ Erreur WebSocket: " + error.getMessage());
            error.printStackTrace();
            Platform.runLater(() -> {
                listener.onError("Connection Lost: " + error.getMessage());
                listener.onConnectionStatus(false);
            });
        }
        
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("🔌 WebSocket fermé - Code: " + statusCode + ", Raison: " + reason);
            Platform.runLater(() -> {
                listener.onConnectionStatus(false);
            });
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
    }
}