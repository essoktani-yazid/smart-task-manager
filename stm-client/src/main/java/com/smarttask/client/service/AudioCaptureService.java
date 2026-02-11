package com.smarttask.client.service;

import javafx.application.Platform;
import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioCaptureService {

    private static final float SAMPLE_RATE = 16000.0f;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    // --- ⚡ PARAMÈTRES DÉTECTION SILENCE OPTIMISÉS ---
    private static final double SILENCE_THRESHOLD = 0.012; // Seuil d'amplitude (réduit pour plus de sensibilité)
    private static final long SILENCE_DURATION_MS = 1000;  // ⚡ 1.0s (réduit de 1.2s) pour plus de réactivité
    private static final long MIN_SPEECH_DURATION_MS = 400; // ⚡ 400ms (réduit de 500ms) pour démarrage rapide
    
    private long lastSpeakTime = System.currentTimeMillis();
    private long firstSpeakTime = 0; // Pour tracker la durée de parole
    private boolean isSpeaking = false;

    private final AudioFormat format;
    private TargetDataLine microphone;
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    private Thread captureThread;
    
    private final AudioChunkListener listener;
    private final Runnable onSilenceDetected;

    public interface AudioChunkListener {
        void onAudioData(byte[] data);
    }

    public AudioCaptureService(AudioChunkListener listener, Runnable onSilenceDetected) {
        this.listener = listener;
        this.onSilenceDetected = onSilenceDetected;
        this.format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
    }

    public void startCapture() {
        if (isCapturing.get()) return;
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            
            isCapturing.set(true);
            lastSpeakTime = System.currentTimeMillis();
            isSpeaking = false;
            firstSpeakTime = 0;

            captureThread = new Thread(this::captureLoop, "Mic-Capture-Thread");
            captureThread.setDaemon(true);
            captureThread.start();
            System.out.println("🎤 Microphone ouvert - VAD (Voice Activity Detection) active");
        } catch (LineUnavailableException e) {
            System.err.println("❌ Erreur microphone: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void captureLoop() {
        // Buffer réduit pour diminuer la latence (1024 au lieu de 4096)
        // Plus petit = plus réactif mais plus de packets réseau
        byte[] buffer = new byte[1024];
        while (isCapturing.get()) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                // Analyse du volume pour le silence (VAD - Voice Activity Detection)
                double rms = calculateRMS(buffer);
                long currentTime = System.currentTimeMillis();

                if (rms > SILENCE_THRESHOLD) {
                    // L'utilisateur parle
                    if (!isSpeaking) {
                        // Début de la parole
                        isSpeaking = true;
                        firstSpeakTime = currentTime;
                        System.out.println("🎤 Début de parole détecté");
                    }
                    lastSpeakTime = currentTime;
                } else {
                    // Silence détecté
                    if (isSpeaking && currentTime - lastSpeakTime > SILENCE_DURATION_MS) {
                        // Vérifier que l'utilisateur a parlé suffisamment longtemps
                        long speechDuration = currentTime - firstSpeakTime;
                        if (speechDuration >= MIN_SPEECH_DURATION_MS) {
                            // SILENCE CONFIRMÉ après une vraie parole
                            System.out.println("🔇 Fin de parole détectée (durée: " + speechDuration + "ms)");
                            Platform.runLater(onSilenceDetected);
                        }
                        isSpeaking = false;
                        lastSpeakTime = currentTime; // Reset pour ne pas boucler
                    }
                }

                // Envoi des données audio uniquement si on parle (économie de bande passante)
                if (isSpeaking || currentTime - lastSpeakTime < 500) {
                    byte[] dataToSend = new byte[bytesRead];
                    System.arraycopy(buffer, 0, dataToSend, 0, bytesRead);
                    listener.onAudioData(dataToSend);
                }
            }
        }
    }

    private double calculateRMS(byte[] audioData) {
        long sum = 0;
        for (int i = 0; i < audioData.length; i += 2) {
            int sample = (audioData[i+1] << 8) | (audioData[i] & 0xFF);
            sum += (long) sample * sample;
        }
        return Math.sqrt((double) sum / (audioData.length / 2.0)) / 32768.0;
    }

    public void stopCapture() {
        isCapturing.set(false);
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }
}