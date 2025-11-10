package function;

/**
 * Quản lý số mạng của người chơi (in-memory).
 *
 * Lý do: số mạng đã được lưu trong metadata của file save. Không cần thêm I/O riêng.
 * Vẫn giữ API cũ để tương thích với GamePanel/SaveController.
 */
public final class LifeManager {
    private static final int DEFAULT_LIVES = 3;
    private static int currentLives = DEFAULT_LIVES;

    private LifeManager() {}

    public static int resetLives() {
        currentLives = DEFAULT_LIVES;
        return currentLives;
    }

    public static int loadLives() {
        return Math.max(0, currentLives);
    }

    public static int decrementLife() {
        currentLives = Math.max(0, loadLives() - 1);
        return currentLives;
    }

    public static void setLives(int lives) {
        currentLives = Math.max(0, lives);
    }

    public static int getDefaultLives() {
        return DEFAULT_LIVES;
    }
}
