package function;

/**
 * Quản lý số mạng của người chơi (in-memory).
 *
 * Lý do: số mạng đã được lưu trong metadata của file save. Không cần thêm I/O riêng.
 * Vẫn giữ API cũ để tương thích với GamePanel/SaveController.
 */
public final class LifeManager {
    private static final int DEFAULT_LIVES = 3;

    private LifeManager() {}

    public static int resetLives() {
        currentLives = DEFAULT_LIVES;
        return currentLives;
    }

    public static int loadLives() {
        return Math.max(0, currentLives);
    }

    public static int decrementLife() {
        int lives = loadLives();
        lives = Math.max(0, lives - 1);
        writeLives(lives);
        return lives;
    }

    /**
     * Returns the timestamp (ms since epoch) when a life was last lost, or 0 if never.
     */
    public static long getLastLifeLostAtMs() {
        return lastLifeLostAtMs;
    }

    /**
     * Returns how long (ms) the life-lost message should be displayed.
     */
    public static int getLifeLostMessageDurationMs() {
        return LIFE_LOST_MESSAGE_DURATION_MS;
    }

    /**
     * Returns the last selected life-lost message (may be null).
     */
    public static String getLastLifeLostMessage() {
        return lastLifeLostMessage;
    }

    public static void setLives(int lives) {
        currentLives = Math.max(0, lives);
    }

    public static int getDefaultLives() {
        return DEFAULT_LIVES;
    }
}
