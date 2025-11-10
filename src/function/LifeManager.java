package function;

/**
 * Quản lý số mạng của người chơi (in-memory).
 *
 */
public final class LifeManager {
    private static final int DEFAULT_LIVES = 3;

    private static int currentLives = DEFAULT_LIVES;

    private static volatile long lastLifeLostAtMs = 0L;

    private static final int LIFE_LOST_MESSAGE_DURATION_MS = 2000;

    private static final String[] LIFE_LOST_MESSAGES = new String[] {
        "Giận quá mất khôn.",
        "Đứng dậy coi.",
        "Mạnh mỗi cái miệng.",
        "Yếu quá, để anh lo."
    };

    private static volatile String lastLifeLostMessage = null;

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
        currentLives = lives;
        lastLifeLostAtMs = System.currentTimeMillis();
        try {
            int idx = (int) (Math.random() * LIFE_LOST_MESSAGES.length);
            if (idx < 0) idx = 0;
            if (idx >= LIFE_LOST_MESSAGES.length) idx = LIFE_LOST_MESSAGES.length - 1;
            lastLifeLostMessage = LIFE_LOST_MESSAGES[idx];
        } catch (Throwable t) {
            lastLifeLostMessage = LIFE_LOST_MESSAGES[0];
        }
        return lives;
    }

    public static long getLastLifeLostAtMs() {
        return lastLifeLostAtMs;
    }

    public static int getLifeLostMessageDurationMs() {
        return LIFE_LOST_MESSAGE_DURATION_MS;
    }

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
