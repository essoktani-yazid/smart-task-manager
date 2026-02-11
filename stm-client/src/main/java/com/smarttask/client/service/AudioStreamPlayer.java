package com.smarttask.client.service;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioStreamPlayer {

    // File d'attente thread-safe (Tampon)
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private volatile boolean isPlaying = false;
    private SourceDataLine line;
    private Runnable onPlaybackFinished; // Callback pour signaler la fin physique du son

    // Configuration EXACTE correspondant au Python (Edge TTS -> Pydub -> PCM)
    private final AudioFormat format = new AudioFormat(
            24000,  // Sample Rate
            16,     // Sample Size (bits)
            1,      // Channels (Mono)
            true,   // Signed
            false   // Big Endian (False = Little Endian, standard PC)
    );

    /**
     * Définit l'action à exécuter quand tout l'audio a fini de jouer.
     */
    public void setOnPlaybackFinished(Runnable callback) {
        this.onPlaybackFinished = callback;
    }

    public void start() {
        if (isPlaying) {
            return;
        }
        isPlaying = true;
        
        Thread playerThread = new Thread(this::playbackLoop, "Audio-Player-Thread");
        playerThread.setDaemon(true);
        playerThread.setUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("❌ Erreur critique dans Audio Player: " + throwable.getMessage());
            throwable.printStackTrace();
            isPlaying = false;
        });
        playerThread.start();
        System.out.println("✅ Audio player démarré");
    }

    // Méthode appelée par le WebSocket quand un chunk arrive
    public void enqueueAudio(byte[] audioData) {
        if (audioData != null && audioData.length > 0) {
            audioQueue.offer(audioData);
            int size = audioQueue.size();
            if (size <= 5 || size % 20 == 0) {
                System.out.println("📥 [FLOW] Chunk en file (queue=" + size + ", +" + audioData.length + " bytes)");
            }
        }
    }

    /**
     * Appelé quand le serveur a fini d'envoyer les données.
     * Insère un marqueur de fin dans la file.
     */
    public void finish() {
        int queueSize = audioQueue.size();
        System.out.println("🏁 [FLOW] finish() -> poison pill (queue=" + queueSize + ")");
        if (queueSize == 0) {
            System.err.println("⚠️ [FLOW] Queue vide: micro va se réactiver sans avoir joué d'audio");
        }
        audioQueue.offer(new byte[0]);
    }

    private void playbackLoop() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("❌ Format audio non supporté par le système");
                isPlaying = false;
                return;
            }
            
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            System.out.println("🔊 Ligne audio ouverte - Prêt à jouer");

            while (isPlaying) {
                try {
                    // Prend le prochain morceau (bloquant si vide)
                    byte[] chunk = audioQueue.take();
                    
                    // DÉTECTION DU MARQUEUR DE FIN
                    if (chunk.length == 0) {
                        System.out.println("🏁 [FLOW] Poison pill -> drainage -> onPlaybackFinished");
                        
                        // Signaler au service qu'on peut rouvrir le micro
                        if (onPlaybackFinished != null) {
                            onPlaybackFinished.run();
                        }
                        continue;
                    }

                    // Aligner au frame size (2 bytes pour 16-bit mono)
                    // Sinon IllegalArgumentException: "non-integral number of frames"
                    int frameSize = 2; // 16-bit mono = 2 bytes par frame
                    int alignedLength = (chunk.length / frameSize) * frameSize;
                    if (alignedLength > 0) {
                        line.write(chunk, 0, alignedLength);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
        } catch (LineUnavailableException e) {
            System.err.println("❌ Impossible d'ouvrir la ligne audio: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue dans playback: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (line != null) {
                try {
                    line.drain();
                    line.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    public void stop() {
        isPlaying = false;
        audioQueue.clear();
        // Pour débloquer le thread s'il attend sur take()
        audioQueue.offer(new byte[0]); 
    }
}