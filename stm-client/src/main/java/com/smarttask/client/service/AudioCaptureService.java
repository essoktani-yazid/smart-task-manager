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

    // --- ⚡ PARAMÈTRES DÉTECTION SILENCE ---
    private static final double SILENCE_THRESHOLD = 0.005; // Seuil réduit pour plus de sensibilité
    private static final long SILENCE_DURATION_MS = 1500;  // 1.5s de silence après parole → fin de phrase
    private static final long MIN_SPEECH_DURATION_MS = 300; // Durée minimum de parole
    private static final long MAX_LISTENING_TIMEOUT_MS = 10000; // 10s max sans parole → auto-récupération
    
    private long lastSpeakTime = System.currentTimeMillis();
    private long firstSpeakTime = 0;
    private long captureStartTime = 0; // Pour le timeout global
    private boolean isSpeaking = false;
    private boolean hasSentAudio = false; // Tracker si on a envoyé de l'audio
    private int debugCounter = 0; // Pour limiter le logging

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
            captureStartTime = System.currentTimeMillis();
            isSpeaking = false;
            firstSpeakTime = 0;
            hasSentAudio = false;
            debugCounter = 0;

            captureThread = new Thread(this::captureLoop, "Mic-Capture-Thread");
            captureThread.setDaemon(true);
            captureThread.start();
            System.out.println("🎤 Microphone ouvert - Capture audio active (threshold=" + SILENCE_THRESHOLD + ")");
        } catch (LineUnavailableException e) {
            System.err.println("❌ Erreur microphone: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void captureLoop() {
        byte[] buffer = new byte[2048]; // Buffer légèrement plus grand pour meilleure précision RMS
        while (isCapturing.get()) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                double rms = calculateRMS(buffer, bytesRead);
                long currentTime = System.currentTimeMillis();

                // Debug: afficher le RMS toutes les ~50 lectures (~1.6s)
                debugCounter++;
                if (debugCounter % 50 == 1) {
                    System.out.println("🔍 [VAD] RMS=" + String.format("%.6f", rms) 
                        + " threshold=" + SILENCE_THRESHOLD 
                        + " isSpeaking=" + isSpeaking
                        + " elapsed=" + (currentTime - captureStartTime) + "ms");
                }

                if (rms > SILENCE_THRESHOLD) {
                    // L'utilisateur parle
                    if (!isSpeaking) {
                        isSpeaking = true;
                        firstSpeakTime = currentTime;
                        System.out.println("🎤 Début de parole détecté (RMS=" + String.format("%.4f", rms) + ")");
                    }
                    lastSpeakTime = currentTime;
                } else {
                    // Silence détecté
                    if (isSpeaking && currentTime - lastSpeakTime > SILENCE_DURATION_MS) {
                        long speechDuration = lastSpeakTime - firstSpeakTime;
                        if (speechDuration >= MIN_SPEECH_DURATION_MS) {
                            System.out.println("🔇 Fin de parole détectée (durée: " + speechDuration + "ms)");
                            Platform.runLater(onSilenceDetected);
                            return; // Sortir de la boucle après détection
                        }
                        isSpeaking = false;
                        lastSpeakTime = currentTime;
                    }
                }

                // ⚡ TOUJOURS envoyer l'audio au serveur (Vosk fait sa propre détection)
                byte[] dataToSend = new byte[bytesRead];
                System.arraycopy(buffer, 0, dataToSend, 0, bytesRead);
                listener.onAudioData(dataToSend);
                hasSentAudio = true;

                // ⏰ Timeout global : si aucune parole après MAX_LISTENING_TIMEOUT_MS
                if (!isSpeaking && currentTime - captureStartTime > MAX_LISTENING_TIMEOUT_MS) {
                    System.out.println("⏰ [TIMEOUT] Aucune parole détectée après " + MAX_LISTENING_TIMEOUT_MS + "ms - envoi AUDIO_END");
                    Platform.runLater(onSilenceDetected);
                    return;
                }
            }
        }
    }

    private double calculateRMS(byte[] audioData, int length) {
        long sum = 0;
        int sampleCount = length / 2;
        for (int i = 0; i < length - 1; i += 2) {
            // Correctly read signed 16-bit little-endian sample
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += (long) sample * sample;
        }
        return Math.sqrt((double) sum / sampleCount) / 32768.0;
    }

    public void stopCapture() {
        isCapturing.set(false);
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }
}