package com.smarttask.client.service;

import com.google.gson.JsonObject;

public interface AIResponseListener {
    void onMessageReceived(JsonObject json);
    void onAudioChunkReceived(byte[] audioData);
    void onAudioEnd();
    void onError(String message);
    void onConnectionStatus(boolean isConnected);
}