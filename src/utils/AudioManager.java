package utils;

/**
 * AudioManager: quản lý phát âm thanh trong trò chơi.
 */
public class AudioManager {
    public static void init() {
        MusicPlayer.init();
    }

    public static void playLoop(String path) {
        if (path == null) return;
        MusicPlayer.playLoop(path); 
    }

    /**
     * Chơi một lần.
     */
    public static void playOnce(String path, Runnable onComplete) {
        if (path == null) return;
        MusicPlayer.playOnce(path, onComplete); 
    }

    public static void playOnce(String path) {
        playOnce(path, null);
    }

    public static void stop() {
        MusicPlayer.stop();
    }

    @Deprecated
    public void playSound(String name) { playOnce(name); }
    @Deprecated
    public void stopAll() { stop(); }
    @Deprecated
    public void setVolume(float v) { /* chưa hỗ trợ */ }
}
