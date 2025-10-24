package utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;

// Reflection-based MusicPlayer that tries to use JavaFX MediaPlayer if available.
// This avoids compile-time dependency on JavaFX so the project can still compile
// when JavaFX is not present. If JavaFX is absent at runtime, calls will fail
// gracefully.
public class MusicPlayer {
    // opaque reference to MediaPlayer (javafx.scene.media.MediaPlayer) if available
    private static Object player = null;

    public static void init() {
        try {
            // Initialize JavaFX runtime by creating a JFXPanel if the class exists
            Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
            jfxPanelClass.getConstructor().newInstance();
        } catch (ClassNotFoundException cnf) {
            // JavaFX not on classpath; caller should handle absence gracefully
            System.err.println("MusicPlayer: JavaFX not available on classpath.");
        } catch (Throwable t) {
            System.err.println("MusicPlayer: Failed to initialize JavaFX runtime: " + t.getMessage());
        }
    }

    public static void playLoop(String filePath) {
        if (filePath == null) return;
        // Ensure we don't leave previous players running when starting a new loop
        try { stop(); } catch (Throwable _t) {}
        try {
            // If the file is a WAV/AIFF/ AU, use Java Sound (no external deps)
            String lower = filePath.toLowerCase();
            if (lower.endsWith(".wav") || lower.endsWith(".aiff") || lower.endsWith(".aif") || lower.endsWith(".au")) {
                try {
                    java.io.File f = new java.io.File(filePath);
                    javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(f);
                    javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(javax.sound.sampled.Clip.class, ais.getFormat());
                    javax.sound.sampled.Clip clip = (javax.sound.sampled.Clip) javax.sound.sampled.AudioSystem.getLine(info);
                    clip.open(ais);
                    clip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
                    player = clip; // reuse opaque player field
                    return;
                } catch (Exception e) {
                    System.err.println("MusicPlayer: failed to play WAV via Java Sound: " + e.getMessage());
                    // fallthrough to try JavaFX if present
                }
            }

            String uri = Paths.get(filePath).toUri().toString();

            // Load Media and MediaPlayer classes via reflection
            Class<?> mediaClass = Class.forName("javafx.scene.media.Media");
            Constructor<?> mediaCtor = mediaClass.getConstructor(String.class);
            Object media = mediaCtor.newInstance(uri);

            Class<?> mediaPlayerClass = Class.forName("javafx.scene.media.MediaPlayer");
            Constructor<?> playerCtor = mediaPlayerClass.getConstructor(mediaClass);
            player = playerCtor.newInstance(media);

            // setCycleCount(MediaPlayer.INDEFINITE)
            try {
                Field indefiniteField = mediaPlayerClass.getField("INDEFINITE");
                Object indefiniteVal = indefiniteField.get(null);
                Method setCycleCount = mediaPlayerClass.getMethod("setCycleCount", int.class);
                // Many JavaFX versions define INDEFINITE as an int constant
                if (indefiniteVal instanceof Integer) {
                    setCycleCount.invoke(player, (Integer) indefiniteVal);
                }
            } catch (NoSuchFieldException nsf) {
                // ignore if unavailable
            }

            // start playback
            Method playMethod = mediaPlayerClass.getMethod("play");
            playMethod.invoke(player);
        } catch (ClassNotFoundException cnf) {
            System.err.println("MusicPlayer: JavaFX Media classes not found; cannot play MP3.");
        } catch (Throwable t) {
            System.err.println("MusicPlayer: Failed to play music '" + filePath + "' - " + t.getMessage());
        }
    }

    /**
     * Play a single sound once (non-blocking). Calls onComplete.run() when playback finishes (if non-null).
     * This will not replace the global background `player` so it can play concurrently.
     */
    public static void playOnce(String filePath, Runnable onComplete) {
        if (filePath == null) return;
        String lower = filePath.toLowerCase();
        // Prefer Java Sound for WAV-like files
        if (lower.endsWith(".wav") || lower.endsWith(".aiff") || lower.endsWith(".aif") || lower.endsWith(".au")) {
            try {
                java.io.File f = new java.io.File(filePath);
                javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(f);
                javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(javax.sound.sampled.Clip.class, ais.getFormat());
                javax.sound.sampled.Clip clip = (javax.sound.sampled.Clip) javax.sound.sampled.AudioSystem.getLine(info);
                clip.open(ais);
                clip.addLineListener(ev -> {
                    if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP || ev.getType() == javax.sound.sampled.LineEvent.Type.CLOSE) {
                        try { clip.close(); } catch (Throwable t) {}
                        if (onComplete != null) {
                            try { onComplete.run(); } catch (Throwable t) {}
                        }
                    }
                });
                clip.start();
                return;
            } catch (Exception e) {
                System.err.println("MusicPlayer: failed to play once via Java Sound: " + e.getMessage());
                // fallthrough to try JavaFX if present
            }
        }

        // Try JavaFX MediaPlayer via reflection for other formats
        try {
            String uri = Paths.get(filePath).toUri().toString();
            Class<?> mediaClass = Class.forName("javafx.scene.media.Media");
            Constructor<?> mediaCtor = mediaClass.getConstructor(String.class);
            Object media = mediaCtor.newInstance(uri);

            Class<?> mediaPlayerClass = Class.forName("javafx.scene.media.MediaPlayer");
            Constructor<?> playerCtor = mediaPlayerClass.getConstructor(mediaClass);
            Object one = playerCtor.newInstance(media);
            // setOnEndOfMedia -> call onComplete
            try {
                Method setOnEnd = mediaPlayerClass.getMethod("setOnEndOfMedia", Runnable.class);
                setOnEnd.invoke(one, (Runnable) () -> {
                    try {
                        Method dispose = mediaPlayerClass.getMethod("dispose");
                        dispose.invoke(one);
                    } catch (Throwable t) {}
                    if (onComplete != null) try { onComplete.run(); } catch (Throwable t) {}
                });
            } catch (NoSuchMethodException ns) {
                // ignore
            }
            Method playMethod = mediaPlayerClass.getMethod("play");
            playMethod.invoke(one);
        } catch (ClassNotFoundException cnf) {
            System.err.println("MusicPlayer: JavaFX Media classes not found; cannot play sound: " + filePath);
        } catch (Throwable t) {
            System.err.println("MusicPlayer: Failed to play once '" + filePath + "' - " + t.getMessage());
        }
    }

    public static void stop() {
        if (player == null) return;
        try {
            // If the player is a javax.sound.sampled.Clip, stop and close it
            if (player instanceof javax.sound.sampled.Clip) {
                try {
                    javax.sound.sampled.Clip clip = (javax.sound.sampled.Clip) player;
                    clip.stop();
                    clip.close();
                } catch (Throwable t) {
                    // ignore
                } finally {
                    player = null;
                    return;
                }
            }

            Class<?> mediaPlayerClass = Class.forName("javafx.scene.media.MediaPlayer");
            Method stopMethod = mediaPlayerClass.getMethod("stop");
            stopMethod.invoke(player);
            Method disposeMethod = null;
            try {
                disposeMethod = mediaPlayerClass.getMethod("dispose");
            } catch (NoSuchMethodException ns) {
                // some versions may not have dispose
            }
            if (disposeMethod != null) disposeMethod.invoke(player);
        } catch (Throwable t) {
            // ignore errors on stop
        } finally {
            player = null;
        }
    }
}

