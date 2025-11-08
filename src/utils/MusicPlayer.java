package utils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

/**
 * MusicPlayer - chỉ hỗ trợ WAV.
 * dùng javax.sound.sampled.Clip để phát loop và hiệu ứng.
 * - playLoop: dừng loop hiện tại (nếu có) rồi play file mới lặp vô hạn.
 * - playOnce: phát một clip riêng cho hiệu ứng, gọi onComplete khi kết thúc (nếu không null).
 */
public class MusicPlayer {
    // Clip dùng cho nhạc nền lặp (chỉ 1 clip cùng lúc)
    private static volatile Clip loopClip = null;
    // Executor để tránh blocking EDT khi mở file
    private static final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MusicPlayer-IO");
        t.setDaemon(true);
        return t;
    });

    public static void init() {
        // tất cả file đã là WAV, không cần khởi tạo gì thêm
    }

    public static void playLoop(String filePath) {
        if (filePath == null) return;
        stop();
        exec.submit(() -> {
            try {
                File f = new File(filePath);
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
                    DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
                    Clip clip = (Clip) AudioSystem.getLine(info);
                    clip.open(ais);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    loopClip = clip;
                }
            } catch (Throwable t) {
                System.err.println("MusicPlayer: playLoop failed: " + t.getMessage());
            }
        });
    }

    public static void playOnce(String filePath, Runnable onComplete) {
        if (filePath == null) {
            if (onComplete != null) try { onComplete.run(); } catch (Throwable ignored) {}
            return;
        }
        exec.submit(() -> {
            Clip clip = null;
            try {
                File f = new File(filePath);
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
                    DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
                    clip = (Clip) AudioSystem.getLine(info);
                    final Clip finalClip = clip;
                    clip.open(ais);
                    clip.addLineListener(ev -> {
                        javax.sound.sampled.LineEvent.Type t = ev.getType();
                        if (t == javax.sound.sampled.LineEvent.Type.STOP || t == javax.sound.sampled.LineEvent.Type.CLOSE) {
                            try {
                                if (finalClip.isOpen()) finalClip.close();
                            } catch (Throwable ignored) {}
                            if (onComplete != null) {
                                try { onComplete.run(); } catch (Throwable ignored) {}
                            }
                        }
                    });
                    clip.start();
                }
            } catch (Throwable t) {
                System.err.println("MusicPlayer: playOnce failed: " + t.getMessage());
                if (clip != null) try { clip.close(); } catch (Throwable ignored) {}
                if (onComplete != null) try { onComplete.run(); } catch (Throwable ignored) {}
            }
        });
    }

    public static void playOnce(String filePath) { playOnce(filePath, null); }

    public static void stop() {
        // Stop loop clip if present
        try {
            Clip c = loopClip;
            if (c != null) {
                try {
                    c.stop();
                } catch (Throwable ignored) {}
                try { c.close(); } catch (Throwable ignored) {}
            }
        } finally {
            loopClip = null;
        }
    }

    // Optional: shutdown executor when app exits (not strictly necessary here)
    public static void shutdown() {
        try {
            stop();
            exec.shutdownNow();
        } catch (Throwable ignored) {}
    }
}

