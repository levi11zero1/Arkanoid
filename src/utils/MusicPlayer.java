package utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;

public class MusicPlayer {
    // Tham chiếu đến player hiện tại (có thể là javax.sound.sampled.Clip hoặc một instance của JavaFX MediaPlayer)
    private static Object currentPlayer = null;

    // ------------ Utility helpers ------------
    // Kiểm tra xem file có phải là dạng âm thanh được Java Sound hỗ trợ trực tiếp không
    private static boolean isNativeSoundFile(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".wav") ;
    }

    // ------------ Khởi tạo runtime JavaFX (tùy chọn) ------------
    /**
     * Thử khởi tạo JavaFX runtime (nếu JavaFX có trên classpath) bằng cách tạo 1 JFXPanel.
     * Việc này giúp JavaFX MediaPlayer hoạt động trong ứng dụng Swing.
     */
    public static void init() {
        try {
            Class<?> jfxPanel = Class.forName("javafx.embed.swing.JFXPanel");
            jfxPanel.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            // JavaFX không có -> không sao, chỉ in thông báo
            System.err.println("MusicPlayer: JavaFX không có trên classpath. Chỉ hỗ trợ WAV/AU bằng Java Sound.");
        } catch (Throwable t) {
            System.err.println("MusicPlayer: Lỗi khi khởi tạo JavaFX: " + t.getMessage());
        }
    }

    // ------------ Phát nhạc lặp (background) ------------
    /**
     * Phát file âm thanh lặp liên tục (dùng cho nhạc nền). Nếu trước đó có player đang
     * chạy thì sẽ dừng nó trước khi khởi tạo player mới.
     */
    public static void playLoop(String filePath) {
        if (filePath == null) return;
        // Dừng player đang chạy (nếu có)
        try { stop(); } catch (Throwable ignored) {}

        // Nếu file là WAV/AIFF/AU, ưu tiên dùng Java Sound (không cần JavaFX)
        if (isNativeSoundFile(filePath)) {
            if (tryPlayLoopWithJavaSound(filePath)) return;
            // nếu thất bại thì tiếp tục thử JavaFX
        }

        // Thử dùng JavaFX MediaPlayer (reflection) cho các định dạng khác như MP3
        tryPlayLoopWithJavaFX(filePath);
    }

    // Dùng javax.sound.sampled.Clip để phát lặp (nếu định dạng hỗ trợ)
    private static boolean tryPlayLoopWithJavaSound(String filePath) {
        try {
            java.io.File f = new java.io.File(filePath);
            javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(f);
            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(javax.sound.sampled.Clip.class, ais.getFormat());
            javax.sound.sampled.Clip clip = (javax.sound.sampled.Clip) javax.sound.sampled.AudioSystem.getLine(info);
            clip.open(ais);
            clip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
            currentPlayer = clip;
            return true;
        } catch (Throwable t) {
            System.err.println("MusicPlayer: Không thể phát vòng bằng Java Sound: " + t.getMessage());
            return false;
        }
    }

    // Dùng JavaFX MediaPlayer (nếu có) để phát lặp
    private static void tryPlayLoopWithJavaFX(String filePath) {
        try {
            String uri = Paths.get(filePath).toUri().toString();
            Class<?> mediaClass = Class.forName("javafx.scene.media.Media");
            Constructor<?> mediaCtor = mediaClass.getConstructor(String.class);
            Object media = mediaCtor.newInstance(uri);

            Class<?> mediaPlayerClass = Class.forName("javafx.scene.media.MediaPlayer");
            Constructor<?> playerCtor = mediaPlayerClass.getConstructor(mediaClass);
            Object fxPlayer = playerCtor.newInstance(media);

            // setCycleCount(MediaPlayer.INDEFINITE) nếu có hằng số này
            try {
                Field indField = mediaPlayerClass.getField("INDEFINITE");
                Object indVal = indField.get(null);
                Method setCycle = mediaPlayerClass.getMethod("setCycleCount", int.class);
                if (indVal instanceof Integer) setCycle.invoke(fxPlayer, (Integer) indVal);
            } catch (NoSuchFieldException nsf) {
                // bỏ qua nếu không tồn tại
            }

            Method play = mediaPlayerClass.getMethod("play");
            play.invoke(fxPlayer);
            currentPlayer = fxPlayer;
        } catch (ClassNotFoundException cnf) {
            System.err.println("MusicPlayer: Không có lớp JavaFX Media. Không thể phát: " + filePath);
        } catch (Throwable t) {
            System.err.println("MusicPlayer: Lỗi khi phát (JavaFX) '" + filePath + "' - " + t.getMessage());
        }
    }

    // ------------ Phát một lần (sound effect) ------------
    /**
     * Phát một file một lần. Hàm onComplete.run() sẽ được gọi khi phát xong (nếu không null).
     * Không thay thế `currentPlayer` để cho phép hiệu ứng và nhạc nền chạy đồng thời.
     */
    public static void playOnce(String filePath, Runnable onComplete) {
        if (filePath == null) return;

        // Nếu là file native được Java Sound hỗ trợ, dùng Clip và lắng nghe LineEvent
        if (isNativeSoundFile(filePath)) {
            if (tryPlayOnceWithJavaSound(filePath, onComplete)) return;
            // nếu thất bại -> thử JavaFX
        }

        // Thử JavaFX (reflection) cho các định dạng khác
        tryPlayOnceWithJavaFX(filePath, onComplete);
    }

    private static boolean tryPlayOnceWithJavaSound(String filePath, Runnable onComplete) {
        try {
            java.io.File f = new java.io.File(filePath);
            javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(f);
            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(javax.sound.sampled.Clip.class, ais.getFormat());
            javax.sound.sampled.Clip clip = (javax.sound.sampled.Clip) javax.sound.sampled.AudioSystem.getLine(info);
            clip.open(ais);
            clip.addLineListener(ev -> {
                if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP || ev.getType() == javax.sound.sampled.LineEvent.Type.CLOSE) {
                    try { clip.close(); } catch (Throwable ignored) {}
                    if (onComplete != null) try { onComplete.run(); } catch (Throwable ignored) {}
                }
            });
            clip.start();
            return true;
        } catch (Throwable t) {
            System.err.println("MusicPlayer: Không thể phát một lần bằng Java Sound: " + t.getMessage());
            return false;
        }
    }

    private static void tryPlayOnceWithJavaFX(String filePath, Runnable onComplete) {
        try {
            String uri = Paths.get(filePath).toUri().toString();
            Class<?> mediaClass = Class.forName("javafx.scene.media.Media");
            Constructor<?> mediaCtor = mediaClass.getConstructor(String.class);
            Object media = mediaCtor.newInstance(uri);

            Class<?> mediaPlayerClass = Class.forName("javafx.scene.media.MediaPlayer");
            Constructor<?> playerCtor = mediaPlayerClass.getConstructor(mediaClass);
            Object fxPlayer = playerCtor.newInstance(media);

            // setOnEndOfMedia -> dispose và gọi onComplete
            try {
                Method setOnEnd = mediaPlayerClass.getMethod("setOnEndOfMedia", Runnable.class);
                setOnEnd.invoke(fxPlayer, (Runnable) () -> {
                    try {
                        Method dispose = mediaPlayerClass.getMethod("dispose");
                        dispose.invoke(fxPlayer);
                    } catch (Throwable ignored) {}
                    if (onComplete != null) try { onComplete.run(); } catch (Throwable ignored) {}
                });
            } catch (NoSuchMethodException ns) {
                // một số phiên bản không có method này -> bỏ qua
            }

            Method play = mediaPlayerClass.getMethod("play");
            play.invoke(fxPlayer);
        } catch (ClassNotFoundException cnf) {
            System.err.println("MusicPlayer: JavaFX không tìm thấy, không thể phát: " + filePath);
        } catch (Throwable t) {
            System.err.println("MusicPlayer: Lỗi khi phát một lần (JavaFX) '" + filePath + "' - " + t.getMessage());
        }
    }

    // ------------ Dừng phát ------------
    /**
     * Dừng và giải phóng player hiện tại (nếu có).
     */
    public static void stop() {
        if (currentPlayer == null) return;
        try {
            // Nếu currentPlayer là Clip (Java Sound)
            if (currentPlayer instanceof javax.sound.sampled.Clip) {
                try {
                    javax.sound.sampled.Clip clip = (javax.sound.sampled.Clip) currentPlayer;
                    clip.stop();
                    clip.close();
                } catch (Throwable ignored) {}
                currentPlayer = null;
                return;
            }

            // Nếu là JavaFX MediaPlayer (sử dụng reflection để gọi stop/dispose)
            try {
                Class<?> mediaPlayerClass = Class.forName("javafx.scene.media.MediaPlayer");
                Method stop = mediaPlayerClass.getMethod("stop");
                stop.invoke(currentPlayer);
                try {
                    Method dispose = mediaPlayerClass.getMethod("dispose");
                    dispose.invoke(currentPlayer);
                } catch (NoSuchMethodException ns) {
                    // một số phiên bản không có dispose
                }
            } catch (ClassNotFoundException cnf) {
                // không phải JavaFX -> bỏ qua
            }
        } catch (Throwable ignored) {
            // bỏ qua mọi lỗi khi dừng
        } finally {
            currentPlayer = null;
        }
    }
}

