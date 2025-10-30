package utils;

/**
 * AudioManager: quản lý phát âm thanh trong trò chơi.
 */
public class AudioManager {
    public static void init() {
        try { MusicPlayer.init(); } catch (Throwable t) { /* ignore */ }
    }

    public static void playLoop(String path) {
        if (path == null) return;
        try { MusicPlayer.playLoop(path); } catch (Throwable t) { /* ignore */ }
    }

    /**
     * Play a one-off sound. onComplete may be null.
     */
    public static void playOnce(String path, Runnable onComplete) {
        if (path == null) return;
        try { MusicPlayer.playOnce(path, onComplete); } catch (Throwable t) { if (onComplete != null) try { onComplete.run(); } catch (Throwable _t) {} }
    }

    public static void playOnce(String path) {
        playOnce(path, null);
    }

    public static void stop() {
        try { MusicPlayer.stop(); } catch (Throwable t) { /* ignore */ }
    }

    @Deprecated
    public void playSound(String name) { playOnce(name); }
    @Deprecated
    public void stopAll() { stop(); }
    @Deprecated
    public void setVolume(float v) { /* no-op currently */ }
}
