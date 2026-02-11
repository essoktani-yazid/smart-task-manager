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
        if (isPlaying) return;
        isPlaying = true;
        
        // On lance le lecteur dans un thread séparé pour ne pas bloquer l'UI
        Thread playerThread = new Thread(this::playbackLoop, "Audio-Player-Thread");
        playerThread.setDaemon(true);
        playerThread.start();
    }

    // Méthode appelée par le WebSocket quand un chunk arrive
    public void enqueueAudio(byte[] audioData) {
        audioQueue.offer(audioData);
    }

    private void playbackLoop() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            while (isPlaying) {
                // Prend le prochain morceau (attend s'il n'y en a pas)
                byte[] chunk = audioQueue.take();
                
                // Signal d'arrêt (chunk vide optionnel)
                if (chunk.length == 0) continue; 

                // Écriture directe sur la carte son
                line.write(chunk, 0, chunk.length);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            if (line != null) {
                line.drain();
                line.close();
            }
        }
    }

    public void stop() {
        isPlaying = false;
        audioQueue.clear();
    }
}