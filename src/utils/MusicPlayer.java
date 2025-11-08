package utils;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;

/**
 * MusicPlayer chỉ hỗ trợ .wav sử dụng Java Sound API (javax.sound.sampled).
 * API đơn giản: playLoop, playOnce, stop.
 */
public class MusicPlayer {
    // Nhạc nền (nếu có) 
    private static Clip currentClip = null;

    /**
     * Play WAV file in loop (background). Stops previous background if any.
     */
    public static void init() {
        // Tất cả các file đã là WAV
    }

    public static void playLoop(String filePath) {
        if (filePath == null) return;
        try { stop(); } catch (Throwable ignored) {}

        try {
            File f = new File(filePath);
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(ais);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            currentClip = clip;
        } catch (Throwable t) {
            System.err.println("MusicPlayer: cannot play loop via Java Sound: " + t.getMessage());
        }
    }

    /**
     * Chơi một lần.
     * Không dừng nhạc nền (nếu có).
     */
    public static void playOnce(String filePath, Runnable onComplete) {
        if (filePath == null) return;
        try {
            File f = new File(filePath);
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(ais);
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP || ev.getType() == LineEvent.Type.CLOSE) {
                    try { clip.close(); } catch (Throwable ignored) {}
                    if (onComplete != null) try { onComplete.run(); } catch (Throwable ignored) {}
                }
            });
            clip.start();
        } catch (Throwable t) {
            System.err.println("MusicPlayer: cannot play once via Java Sound: " + t.getMessage());
        }
    }

    /**
     * Dừng nhạc nền (nếu có).
     */
    public static void stop() {
        if (currentClip == null) return;
        try {
            currentClip.stop();
            currentClip.close();
        } catch (Throwable ignored) {
        } finally {
            currentClip = null;
        }
    }
}

