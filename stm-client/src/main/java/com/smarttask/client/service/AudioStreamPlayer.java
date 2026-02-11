package com.smarttask.client.service;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioStreamPlayer {

    // File d'attente thread-safe (Tampon)
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private volatile boolean isPlaying = false;
    private SourceDataLine line;

    // Configuration EXACTE correspondant au Python
    private final AudioFormat format = new AudioFormat(
            24000,  // Sample Rate
            16,     // Sample Size (bits)
            1,      // Channels (Mono)
            true,   // Signed
            false   // Big Endian (False = Little Endian, standard PC)
    );

    public void start() {
        if (isPlaying) {
            System.out.println("⚠️ Audio player déjà actif");
            return;
        }
        isPlaying = true;
        
        // On lance le lecteur dans un thread séparé pour ne pas bloquer l'UI
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
        audioQueue.offer(audioData);
    }

    private void playbackLoop() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            
            // Vérifier que le système audio est disponible
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("❌ Format audio non supporté par le système");
                isPlaying = false;
                return;
            }
            
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            System.out.println("🔊 Ligne audio ouverte - Prêt à jouer");

            int chunkCount = 0;
            while (isPlaying) {
                try {
                    // Prend le prochain morceau (attend s'il n'y en a pas)
                    byte[] chunk = audioQueue.take();
                    
                    // Signal d'arrêt (chunk vide optionnel)
                    if (chunk.length == 0) {
                        System.out.println("⚠️ Chunk vide reçu, ignoré");
                        continue;
                    }

                    // Écriture directe sur la carte son
                    line.write(chunk, 0, chunk.length);
                    chunkCount++;
                    
                    if (chunkCount % 10 == 0) {
                        System.out.println("🔊 " + chunkCount + " chunks audio joués");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("⚠️ Playback interrompu");
                    break;
                }
            }
            
            System.out.println("✅ Playback terminé - Total chunks: " + chunkCount);
            
        } catch (LineUnavailableException e) {
            System.err.println("❌ Impossible d'ouvrir la ligne audio: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue dans playback: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (line != null) {
                try {
                    line.drain(); // Attendre que tout soit joué
                    line.close();
                    System.out.println("🔇 Ligne audio fermée");
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur lors de la fermeture: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        isPlaying = false;
        audioQueue.clear();
    }
}